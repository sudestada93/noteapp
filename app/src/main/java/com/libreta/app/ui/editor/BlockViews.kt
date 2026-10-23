package com.libreta.app.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.OutlinedFlag
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.libreta.app.data.NoteItem
import com.libreta.app.model.BlockType
import com.libreta.app.ui.components.BrutalShape
import com.libreta.app.ui.components.CrayonCheckbox
import com.libreta.app.ui.components.hardShadow
import com.libreta.app.ui.theme.Dims
import com.libreta.app.ui.theme.LocalPalette
import com.libreta.app.ui.theme.Mono
import com.libreta.app.ui.theme.Palette
import com.libreta.app.ui.theme.T
import com.libreta.app.ui.theme.crayon
import com.libreta.app.ui.theme.crayonWash
import com.libreta.app.ui.theme.editorFamily
import com.libreta.app.util.Fmt
import kotlin.math.sin

data class EditorEnv(
    val fontIndex: Int,
    val fontSize: Float,
    val onOpenImage: (EditorBlock) -> Unit,
    val onOcr: (EditorBlock) -> Unit,
    val onOpenNote: (String) -> Unit,
    val onTaskDue: (EditorBlock) -> Unit,
    val onSlash: (EditorBlock, SlashCommand) -> Unit,
    val notes: List<NoteItem>,
)

fun textStyleFor(type: BlockType, env: EditorEnv, p: Palette, checked: Boolean): TextStyle {
    val fam = editorFamily(env.fontIndex)
    val fs = env.fontSize
    val base = TextStyle(fontFamily = fam, fontSize = fs.sp, lineHeight = (fs * 1.55f).sp, color = p.ink)
    return when (type) {
        BlockType.H1 -> TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = (fs + 10).sp, lineHeight = (fs + 16).sp, color = p.ink, letterSpacing = 0.2.sp)
        BlockType.H2 -> TextStyle(fontFamily = Mono, fontWeight = FontWeight.SemiBold, fontSize = (fs + 5).sp, lineHeight = (fs + 11).sp, color = p.ink)
        BlockType.H3 -> TextStyle(fontFamily = Mono, fontWeight = FontWeight.SemiBold, fontSize = (fs + 1).sp, lineHeight = (fs + 7).sp, color = p.ink, letterSpacing = 0.6.sp)
        BlockType.CODE -> TextStyle(fontFamily = Mono, fontSize = (fs - 2).sp, lineHeight = (fs * 1.45f).sp, color = Color(0xFFF4EFEA))
        BlockType.QUOTE -> base.copy(fontStyle = FontStyle.Italic, color = p.ink)
        BlockType.CHECK, BlockType.TASK -> if (checked) base.copy(color = p.inkFaint, textDecoration = TextDecoration.LineThrough) else base
        else -> base
    }
}

@Composable
fun BlockView(block: EditorBlock, index: Int, vm: EditorViewModel, env: EditorEnv) {
    val p = LocalPalette.current
    val indentPad = (block.indent * 22).dp
    Box(Modifier.fillMaxWidth().padding(start = indentPad)) {
        when (block.type) {
            BlockType.DIVIDER -> DividerBlock(block, vm)
            BlockType.IMAGE -> ImageBlock(block, vm, env)
            BlockType.FILE -> FileBlock(block, vm)
            BlockType.AUDIO -> AudioBlock(block, vm)
            BlockType.TABLE -> TableBlock(block, vm, env)
            BlockType.NOTE_LINK -> NoteLinkBlock(block, vm, env)
            else -> TextBlock(block, index, vm, env, p)
        }
    }
}

@Composable
private fun TextBlock(block: EditorBlock, index: Int, vm: EditorViewModel, env: EditorEnv, p: Palette) {
    var focused by remember { mutableStateOf(false) }
    val type = block.type
    LaunchedEffect(vm.focusSignal) {
        val s = vm.focusSignal
        if (s != null && s.first == block.id) runCatching { block.focusRequester.requestFocus() }
    }
    val style = textStyleFor(type, env, p, block.checked)
    val transformation = remember(focused, p, vm.find, type) {
        InlineTransformation(showMarkers = focused, palette = p, find = vm.find, plain = type == BlockType.CODE)
    }
    val field: @Composable (Modifier) -> Unit = { mod ->
        BasicTextField(
            value = block.field,
            onValueChange = { vm.onTextChange(block, it) },
            textStyle = style,
            visualTransformation = transformation,
            cursorBrush = SolidColor(if (type == BlockType.CODE) Color(0xFF6FC2FF) else p.ink),
            modifier = mod
                .focusRequester(block.focusRequester)
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) vm.focusedId = block.id
                },
            decorationBox = { inner ->
                Box {
                    if (block.text.isEmpty() && focused) {
                        Text(
                            when (type) {
                                BlockType.H1, BlockType.H2, BlockType.H3 -> "Encabezado"
                                BlockType.CHECK -> "Elemento"
                                BlockType.TASK -> "Nueva tarea"
                                BlockType.QUOTE -> "Cita"
                                BlockType.CODE -> "código…"
                                BlockType.CALLOUT -> "Algo importante…"
                                else -> "Escribí, o \"/\" para insertar"
                            },
                            style = style.copy(color = p.inkFaint, textDecoration = TextDecoration.None),
                        )
                    }
                    inner()
                }
            },
        )
    }
    Column(Modifier.fillMaxWidth()) {
        when (type) {
            BlockType.BULLET -> Row(Modifier.padding(vertical = 2.dp)) {
                Box(Modifier.width(24.dp).height((env.fontSize * 1.55f).dp), contentAlignment = Alignment.Center) {
                    val shapeMod = if (block.indent % 2 == 0) Modifier.clip(CircleShape).background(p.ink) else Modifier.border(1.5.dp, p.ink, CircleShape)
                    Box(Modifier.size(7.dp).then(shapeMod))
                }
                field(Modifier.weight(1f))
            }
            BlockType.NUMBERED -> Row(Modifier.padding(vertical = 2.dp)) {
                val number = remember(vm.blocks.size, index, block.indent) {
                    var n = 1
                    var i = index - 1
                    while (i >= 0) {
                        val b = vm.blocks.getOrNull(i) ?: break
                        if (b.type == BlockType.NUMBERED && b.indent == block.indent) n++
                        else if (!(b.type.isListLike && b.indent > block.indent)) break
                        i--
                    }
                    n
                }
                Box(Modifier.width(28.dp).height((env.fontSize * 1.55f).dp), contentAlignment = Alignment.CenterStart) {
                    Text(if (block.indent % 2 == 0) "$number." else "${'a' + (number - 1) % 26}.", style = T.bodySm.copy(fontWeight = FontWeight.SemiBold), color = p.ink)
                }
                field(Modifier.weight(1f))
            }
            BlockType.CHECK -> Row(Modifier.padding(vertical = 3.dp)) {
                Box(Modifier.width(30.dp).height((env.fontSize * 1.55f).dp), contentAlignment = Alignment.CenterStart) {
                    CrayonCheckbox(block.checked, { vm.toggleCheck(block) }, size = 19.dp)
                }
                field(Modifier.weight(1f))
            }
            BlockType.TASK -> TaskRow(block, vm, env, field)
            BlockType.QUOTE -> Row(Modifier.padding(vertical = 4.dp).height(IntrinsicSize.Min)) {
                Box(Modifier.width(5.dp).fillMaxHeight().background(p.primary).border(1.dp, p.line))
                Spacer(Modifier.width(14.dp))
                field(Modifier.weight(1f))
            }
            BlockType.CODE -> Box(
                Modifier.padding(vertical = 6.dp).fillMaxWidth().hardShadow(p.shadow, 3.dp)
                    .clip(BrutalShape).background(Color(0xFF2A2927)).border(Dims.border, p.line, BrutalShape),
            ) {
                Column {
                    Row(Modifier.fillMaxWidth().background(Color(0xFF383838)).padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        listOf(Color(0xFFF38E84), Color(0xFFFFDE00), Color(0xFF38C1B0)).forEach {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(it)); Spacer(Modifier.width(5.dp))
                        }
                        Spacer(Modifier.weight(1f))
                        Text("CÓDIGO", style = T.micro, color = Color(0xFFA1A1A1))
                    }
                    Box(Modifier.horizontalScroll(rememberScrollState()).padding(12.dp)) { field(Modifier.widthIn(min = 280.dp).heightIn(min = 20.dp)) }
                }
            }
            BlockType.CALLOUT -> Row(
                Modifier.padding(vertical = 6.dp).fillMaxWidth().hardShadow(p.shadow, 3.dp).clip(BrutalShape)
                    .background(crayonWash(block.color + 1, p.dark)).border(Dims.border, p.line, BrutalShape).padding(12.dp),
            ) {
                var menu by remember { mutableStateOf(false) }
                Box {
                    Text("💡", fontSize = 18.sp, modifier = Modifier.clickable { menu = true }.padding(end = 10.dp))
                    DropdownMenu(menu, { menu = false }) {
                        (0 until 8).forEach { c ->
                            DropdownMenuItem(
                                text = { Text(com.libreta.app.ui.theme.CrayonNames[(c + 1) % 8], style = T.body) },
                                leadingIcon = { Box(Modifier.size(16.dp).background(crayon(c + 1)).border(1.dp, p.line)) },
                                onClick = { vm.setBlockColor(block, c); menu = false },
                            )
                        }
                    }
                }
                field(Modifier.weight(1f))
            }
            BlockType.H1 -> Column(Modifier.padding(top = 14.dp, bottom = 4.dp)) { field(Modifier.fillMaxWidth()) }
            BlockType.H2 -> Column(Modifier.padding(top = 10.dp, bottom = 2.dp)) { field(Modifier.fillMaxWidth()) }
            BlockType.H3 -> Column(Modifier.padding(top = 8.dp, bottom = 2.dp)) { field(Modifier.fillMaxWidth()) }
            else -> field(Modifier.fillMaxWidth().padding(vertical = 2.dp))
        }
        // Menú de comandos "/"
        val raw = block.text
        if (focused && type == BlockType.PARAGRAPH && raw.startsWith("/") && !raw.contains(' ')) {
            val q = raw.drop(1).lowercase()
            val matches = SlashCommands.filter { q.isEmpty() || it.label.lowercase().contains(q) || it.key.contains(q) || it.hint.contains(q) }
            if (matches.isNotEmpty()) SlashMenu(matches) { env.onSlash(block, it) }
        }
    }
}

@Composable
private fun SlashMenu(items: List<SlashCommand>, onPick: (SlashCommand) -> Unit) {
    val p = LocalPalette.current
    Column(
        Modifier.padding(vertical = 6.dp).fillMaxWidth().hardShadow(p.shadow, 4.dp).clip(BrutalShape)
            .background(p.surface).border(Dims.border, p.line, BrutalShape),
    ) {
        Text("INSERTAR BLOQUE", style = T.micro, color = p.inkSoft, modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 4.dp))
        items.take(8).forEachIndexed { i, c ->
            Row(
                Modifier.fillMaxWidth().clickable { onPick(c) }.background(if (i == 0) p.iceWash else Color.Transparent).padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(c.label, style = T.body.copy(fontWeight = FontWeight.SemiBold), color = p.ink, modifier = Modifier.width(130.dp))
                Text(c.hint, style = T.caption, color = p.inkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun TaskRow(block: EditorBlock, vm: EditorViewModel, env: EditorEnv, field: @Composable (Modifier) -> Unit) {
    val p = LocalPalette.current
    val overdue = block.dueAt?.let { it < System.currentTimeMillis() && !block.checked } ?: false
    Row(
        Modifier.padding(vertical = 4.dp).fillMaxWidth().clip(BrutalShape)
            .background(if (block.checked) Color.Transparent else p.surface.copy(alpha = 0.7f))
            .border(1.5.dp, if (block.checked) p.inkFaint else p.line, BrutalShape).padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Box(Modifier.width(32.dp).height((env.fontSize * 1.55f).dp), contentAlignment = Alignment.CenterStart) {
            CrayonCheckbox(block.checked, { vm.toggleCheck(block) }, size = 21.dp, round = true, color = p.primary)
        }
        Column(Modifier.weight(1f)) {
            field(Modifier.fillMaxWidth())
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.clip(BrutalShape).background(if (overdue) p.danger else if (block.dueAt != null) p.iceWash else Color.Transparent)
                        .border(1.dp, if (block.dueAt != null) p.line else p.inkFaint, BrutalShape)
                        .clickable { env.onTaskDue(block) }.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Alarm, null, tint = if (overdue) Color(0xFF383838) else p.inkSoft, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        block.dueAt?.let { Fmt.due(it) + if (block.reminderAt != null) " 🔔" else "" } ?: "FECHA",
                        style = T.micro, color = if (overdue) Color(0xFF383838) else p.inkSoft,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (block.flagged) Icons.Filled.Flag else Icons.Filled.OutlinedFlag, "Bandera",
                    tint = if (block.flagged) p.orange else p.inkFaint,
                    modifier = Modifier.size(20.dp).clickable { vm.toggleFlag(block) },
                )
            }
        }
    }
}

@Composable
private fun BlockMenu(block: EditorBlock, vm: EditorViewModel, extra: @Composable (close: () -> Unit) -> Unit = {}) {
    val p = LocalPalette.current
    var open by remember { mutableStateOf(false) }
    Box {
        Box(
            Modifier.size(30.dp).clip(BrutalShape).background(p.surface).border(1.5.dp, p.line, BrutalShape).clickable { open = true },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.MoreVert, "Opciones", tint = p.ink, modifier = Modifier.size(18.dp)) }
        DropdownMenu(open, { open = false }) {
            extra { open = false }
            DropdownMenuItem({ Text("Subir", style = T.body) }, { vm.moveBlock(block, -1); open = false }, leadingIcon = { Icon(Icons.Filled.KeyboardArrowUp, null) })
            DropdownMenuItem({ Text("Bajar", style = T.body) }, { vm.moveBlock(block, 1); open = false }, leadingIcon = { Icon(Icons.Filled.KeyboardArrowDown, null) })
            DropdownMenuItem({ Text("Duplicar", style = T.body) }, { vm.duplicateBlock(block); open = false }, leadingIcon = { Icon(Icons.Filled.ContentCopy, null) })
            DropdownMenuItem({ Text("Eliminar", style = T.body, color = p.danger) }, { vm.deleteBlock(block); open = false }, leadingIcon = { Icon(Icons.Filled.Delete, null, tint = p.danger) })
        }
    }
}

@Composable
private fun DividerBlock(block: EditorBlock, vm: EditorViewModel) {
    val p = LocalPalette.current
    var open by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().height(28.dp).clickable { open = true }) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(28.dp)) {
            val path = Path()
            val mid = size.height / 2
            path.moveTo(0f, mid)
            var x = 0f
            while (x <= size.width) {
                path.lineTo(x, mid + sin(x / 9f) * 3.5f)
                x += 3f
            }
            drawPath(path, p.ink, style = Stroke(2.dp.toPx()))
        }
        DropdownMenu(open, { open = false }) {
            DropdownMenuItem({ Text("Eliminar separador", style = T.body) }, { vm.deleteBlock(block); open = false }, leadingIcon = { Icon(Icons.Filled.Delete, null) })
        }
    }
}

@Composable
private fun MediaFrame(content: @Composable () -> Unit) {
    val p = LocalPalette.current
    Box(
        Modifier.padding(vertical = 8.dp).fillMaxWidth().hardShadow(p.shadow, 4.dp).clip(BrutalShape)
            .background(p.surface).border(Dims.border, p.line, BrutalShape),
    ) { content() }
}

@Composable
private fun ImageBlock(block: EditorBlock, vm: EditorViewModel, env: EditorEnv) {
    val context = LocalContext.current
    val file = block.path?.let { vm.repo.fileFor(it) } ?: return
    MediaFrame {
        Box {
            AsyncImage(
                model = file, contentDescription = block.name, contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 520.dp).clickable { env.onOpenImage(block) },
            )
            Box(Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                BlockMenu(block, vm) { close ->
                    DropdownMenuItem({ Text("Extraer texto (OCR)", style = T.body) }, { env.onOcr(block); close() }, leadingIcon = { Icon(Icons.Filled.TextFields, null) })
                    DropdownMenuItem({ Text("Compartir", style = T.body) }, { shareFile(context, file, block.mime); close() }, leadingIcon = { Icon(Icons.Filled.Share, null) })
                }
            }
            if (block.sketch) {
                Text(
                    "BOCETO", style = T.micro, color = Color(0xFF383838),
                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).background(Color(0xFFFFDE00)).border(1.dp, Color(0xFF383838)).padding(horizontal = 6.dp, vertical = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun FileBlock(block: EditorBlock, vm: EditorViewModel) {
    val p = LocalPalette.current
    val context = LocalContext.current
    val file = block.path?.let { vm.repo.fileFor(it) } ?: return
    val isPdf = block.mime == "application/pdf" || block.name?.endsWith(".pdf", true) == true
    MediaFrame {
        Column {
            if (isPdf) PdfThumbnail(file, Modifier.fillMaxWidth().heightIn(max = 300.dp).clickable { openFile(context, file, block.mime) })
            Row(
                Modifier.fillMaxWidth().background(if (isPdf) p.canvas else p.surface).clickable { openFile(context, file, block.mime) }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(42.dp).clip(BrutalShape).background(if (isPdf) p.danger else p.canary).border(1.5.dp, p.line, BrutalShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        (block.name?.substringAfterLast('.', "")?.take(4) ?: "").uppercase().ifBlank { "FILE" },
                        style = T.micro, color = Color(0xFF383838),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(block.name ?: "Archivo", style = T.body.copy(fontWeight = FontWeight.SemiBold), color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(Fmt.size(block.size) + " · tocar para abrir", style = T.caption, color = p.inkSoft)
                }
                BlockMenu(block, vm) { close ->
                    DropdownMenuItem({ Text("Abrir", style = T.body) }, { openFile(context, file, block.mime); close() }, leadingIcon = { Icon(Icons.Filled.OpenInNew, null) })
                    DropdownMenuItem({ Text("Compartir", style = T.body) }, { shareFile(context, file, block.mime); close() }, leadingIcon = { Icon(Icons.Filled.Share, null) })
                }
            }
        }
    }
}

@Composable
private fun AudioBlock(block: EditorBlock, vm: EditorViewModel) {
    val p = LocalPalette.current
    val context = LocalContext.current
    val file = block.path?.let { vm.repo.fileFor(it) } ?: return
    MediaFrame {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🎙", fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Text(block.name ?: "Audio", style = T.bodySm.copy(fontWeight = FontWeight.SemiBold), color = p.ink, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                BlockMenu(block, vm) { close ->
                    DropdownMenuItem({ Text("Compartir", style = T.body) }, { shareFile(context, file, block.mime); close() }, leadingIcon = { Icon(Icons.Filled.Share, null) })
                }
            }
            Spacer(Modifier.height(8.dp))
            AudioPlayer(file, block.durationMs, block.waveform, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun TableBlock(block: EditorBlock, vm: EditorViewModel, env: EditorEnv) {
    val p = LocalPalette.current
    MediaFrame {
        Column {
            Row(Modifier.fillMaxWidth().background(p.canary).padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("TABLA", style = T.micro, color = Color(0xFF383838), modifier = Modifier.weight(1f))
                listOf("+FILA" to { vm.addRow(block) }, "+COL" to { vm.addColumn(block) }, "−FILA" to { vm.removeRow(block) }, "−COL" to { vm.removeColumn(block) }).forEach { (l, a) ->
                    Text(l, style = T.micro, color = Color(0xFF383838), modifier = Modifier.clip(BrutalShape).clickable { a() }.padding(horizontal = 6.dp, vertical = 6.dp))
                }
                BlockMenu(block, vm)
            }
            Box(Modifier.fillMaxWidth().height(Dims.border).background(p.line))
            Column(Modifier.horizontalScroll(rememberScrollState())) {
                block.rows.forEachIndexed { r, row ->
                    Row(Modifier.height(IntrinsicSize.Min)) {
                        row.forEachIndexed { c, cell ->
                            BasicTextField(
                                value = cell,
                                onValueChange = { vm.setCell(block, r, c, it) },
                                textStyle = TextStyle(
                                    fontFamily = if (r == 0) Mono else editorFamily(env.fontIndex),
                                    fontWeight = if (r == 0) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = (env.fontSize - 2).sp, color = p.ink,
                                ),
                                cursorBrush = SolidColor(p.ink),
                                modifier = Modifier.width(140.dp).fillMaxHeight()
                                    .background(if (r == 0) p.iceWash else p.surface)
                                    .drawBehind {
                                        drawLine(p.inkFaint, Offset(size.width, 0f), Offset(size.width, size.height), 1.dp.toPx())
                                        drawLine(p.inkFaint, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                                    }
                                    .padding(10.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteLinkBlock(block: EditorBlock, vm: EditorViewModel, env: EditorEnv) {
    val p = LocalPalette.current
    val target = env.notes.firstOrNull { it.note.id == block.linkId }
    Row(
        Modifier.padding(vertical = 6.dp).fillMaxWidth().hardShadow(p.shadow, 3.dp).clip(BrutalShape).background(p.iceWash)
            .border(Dims.border, p.line, BrutalShape).clickable { block.linkId?.let(env.onOpenNote) }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Link, null, tint = p.ink, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("ENLACE A NOTA", style = T.micro, color = p.inkSoft)
            Text(
                target?.note?.title?.ifBlank { "Sin título" } ?: (block.name ?: "Nota eliminada"),
                style = T.body.copy(fontWeight = FontWeight.SemiBold), color = if (target == null) p.inkFaint else p.ink,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        BlockMenu(block, vm)
    }
}
