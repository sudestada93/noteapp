package com.libreta.app.ui.editor

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.outlined.BorderColor
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.libreta.app.data.NoteVersionEntity
import com.libreta.app.model.Block
import com.libreta.app.model.BlockCodec
import com.libreta.app.model.BlockType
import com.libreta.app.ui.components.BrutalButton
import com.libreta.app.ui.components.BrutalDialog
import com.libreta.app.ui.components.BrutalSheet
import com.libreta.app.ui.components.BrutalShape
import com.libreta.app.ui.components.BrutalTextField
import com.libreta.app.ui.components.BtnStyle
import com.libreta.app.ui.components.ColorDots
import com.libreta.app.ui.components.ConfirmDialog
import com.libreta.app.ui.components.DateTimePickerDialog
import com.libreta.app.ui.components.Doodle
import com.libreta.app.ui.components.EmptyState
import com.libreta.app.ui.components.NotePickerSheet
import com.libreta.app.ui.components.NotebookPickerSheet
import com.libreta.app.ui.components.NotebookSpine
import com.libreta.app.ui.components.PinPad
import com.libreta.app.ui.components.SheetItem
import com.libreta.app.ui.components.Sticker
import com.libreta.app.ui.components.TagPickerSheet
import com.libreta.app.ui.components.TextInputDialog
import com.libreta.app.ui.components.hardShadow
import com.libreta.app.ui.components.rememberApp
import com.libreta.app.ui.components.settingsState
import com.libreta.app.ui.theme.Crayons
import com.libreta.app.ui.theme.Dims
import com.libreta.app.ui.theme.LocalPalette
import com.libreta.app.ui.theme.Mono
import com.libreta.app.ui.theme.T
import com.libreta.app.ui.theme.crayon
import com.libreta.app.util.Biometric
import com.libreta.app.util.Exporter
import com.libreta.app.util.Fmt
import com.libreta.app.util.Ocr
import com.libreta.app.util.findActivity
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

@Composable
fun EditorScreen(start: String?, onBack: () -> Unit, onOpenNote: (String) -> Unit) {
    val vm: EditorViewModel = viewModel()
    val app = rememberApp()
    val repo = app.repository
    val p = LocalPalette.current
    val settings by settingsState()
    val item by vm.item.collectAsStateWithLifecycle()
    val allNotes by repo.noteItems.collectAsStateWithLifecycle(emptyList())
    val notebooks by repo.notebooks.collectAsStateWithLifecycle(emptyList())
    val tags by repo.tags.collectAsStateWithLifecycle(emptyList())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var unlocked by rememberSaveable { mutableStateOf(false) }

    // Guardar al pasar a segundo plano
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_STOP) vm.save() }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    // ---- Estado de hojas y diálogos ----
    var sheet by remember { mutableStateOf<String?>(null) }
    var showSketch by remember { mutableStateOf(false) }
    var showRecorder by remember { mutableStateOf(false) }
    var viewer by remember { mutableStateOf<File?>(null) }
    var taskDueFor by remember { mutableStateOf<EditorBlock?>(null) }
    var customDateFor by remember { mutableStateOf<String?>(null) }
    var confirmTrash by remember { mutableStateOf(false) }
    var restoreVersion by remember { mutableStateOf<NoteVersionEntity?>(null) }

    fun toast(s: String) = Toast.makeText(context, s, Toast.LENGTH_SHORT).show()

    // ---- Lanzadores ----
    val pickImages = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(12)) { uris ->
        if (uris.isNotEmpty()) scope.launch { vm.insertBlocks(uris.mapNotNull { repo.importAttachment(it, BlockType.IMAGE) }) }
    }
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { repo.importAttachment(uri)?.let { vm.insertBlocks(listOf(it)) } ?: toast("No se pudo adjuntar") }
    }
    var pendingPhoto by remember { mutableStateOf<Pair<String, File>?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val pp = pendingPhoto
        if (ok && pp != null) vm.insertBlocks(listOf(Block(type = BlockType.IMAGE, path = pp.first, name = "Foto.jpg", mime = "image/jpeg", size = pp.second.length())))
        else pp?.second?.delete()
        pendingPhoto = null
    }
    fun launchCamera() {
        val pair = repo.newAttachmentFile("jpg")
        pendingPhoto = pair
        try { camera.launch(fileUri(context, pair.second)) } catch (e: Exception) { toast("No hay cámara disponible") }
    }
    val scanLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val result = GmsDocumentScanningResult.fromActivityResultIntent(res.data)
            scope.launch {
                val blocks = mutableListOf<Block>()
                result?.pages?.forEach { page -> repo.importAttachment(page.imageUri, BlockType.IMAGE)?.let { blocks += it.copy(name = "Escaneo.jpg") } }
                result?.pdf?.let { pdf ->
                    repo.importAttachment(pdf.uri, BlockType.FILE)?.let { blocks += it.copy(name = "Escaneo ${Fmt.short(System.currentTimeMillis())}.pdf", mime = "application/pdf") }
                }
                vm.insertBlocks(blocks)
            }
        }
    }
    fun launchScan() {
        val activity = context.findActivity() ?: return
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(20)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG, GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
        GmsDocumentScanning.getClient(options).getStartScanIntent(activity)
            .addOnSuccessListener { scanLauncher.launch(IntentSenderRequest.Builder(it).build()) }
            .addOnFailureListener { toast("Escáner no disponible: uso la cámara"); launchCamera() }
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showRecorder = true else toast("Se necesita permiso de micrófono")
    }
    fun startAudio() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) showRecorder = true
        else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    fun ensureNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val exportMd = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(BlockCodec.toMarkdown(vm.title.text, vm.blocks.map { b -> b.toBlock() }).toByteArray()) }
                toast("Exportado")
            }.onFailure { toast("No se pudo exportar") }
        }
    }

    fun command(key: String) {
        when (key) {
            "image" -> pickImages.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            "camera" -> launchCamera()
            "scan" -> launchScan()
            "sketch" -> showSketch = true
            "audio" -> startAudio()
            "file" -> pickFile.launch(arrayOf("*/*"))
            "link" -> sheet = "notelink"
            "weblink" -> sheet = "weblink"
            "table" -> vm.insertTable()
            "divider" -> vm.insertBlocks(listOf(Block(type = BlockType.DIVIDER)))
        }
    }

    var startHandled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(vm.loaded) {
        if (vm.loaded && !startHandled) {
            startHandled = true
            if (start != null) command(start)
            else if (vm.isEmpty()) vm.blocks.firstOrNull()?.let { vm.focus(it, 0) }
        }
    }

    val note = item?.note
    val locked = note?.isLocked == true && settings.lockEnabled && !unlocked

    val env = EditorEnv(
        fontIndex = settings.editorFont,
        fontSize = settings.editorFontSize,
        onOpenImage = { b -> b.path?.let { viewer = repo.fileFor(it) } },
        onOcr = { b ->
            b.path?.let { repo.fileFor(it) }?.let { f ->
            toast("Reconociendo texto…")
            Ocr.recognize(context, f) { text ->
                if (text.isNullOrBlank()) toast("No se encontró texto")
                else {
                    vm.focusedId = b.id
                    vm.insertBlocks(listOf(Block(type = BlockType.CALLOUT, text = "Texto extraído de la imagen", color = 0)) + text.split('\n').filter { it.isNotBlank() }.map { Block(text = it) })
                }
            }
            }
        },
        onOpenNote = onOpenNote,
        onTaskDue = { taskDueFor = it },
        onSlash = { b, cmd -> vm.applySlash(b, cmd)?.let { command(it) } },
        notes = allNotes,
    )

    Column(Modifier.fillMaxSize().background(p.canvas)) {
        // ---------- Barra superior ----------
        Column(Modifier.fillMaxWidth().background(p.surface).windowInsetsPadding(WindowInsets.statusBars)) {
            Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                TopIcon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") { onBack() }
                Column(Modifier.weight(1f).padding(start = 4.dp)) {
                    Text(item?.notebook?.name?.uppercase() ?: "NOTA", style = T.micro, color = p.inkSoft, maxLines = 1)
                    Text(
                        if (vm.lastSavedAt > 0) "✓ Guardado ${Fmt.time(vm.lastSavedAt)}" else note?.let { "Editada " + Fmt.relative(it.updatedAt) } ?: "",
                        style = T.caption, color = p.inkFaint, maxLines = 1,
                    )
                }
                TopIcon(Icons.AutoMirrored.Filled.Undo, "Deshacer", enabled = vm.canUndo) { vm.undo() }
                TopIcon(Icons.AutoMirrored.Filled.Redo, "Rehacer", enabled = vm.canRedo) { vm.redo() }
                TopIcon(if (note?.isShortcut == true) Icons.Filled.Star else Icons.Filled.StarBorder, "Atajo", tint = if (note?.isShortcut == true) p.orange else p.ink) {
                    vm.meta { it.copy(isShortcut = !it.isShortcut) }
                }
                TopIcon(Icons.Filled.MoreVert, "Más") { sheet = "menu" }
            }
            Box(Modifier.fillMaxWidth().height(Dims.border).background(p.line))
            if (note != null && note.color >= 0) Box(Modifier.fillMaxWidth().height(6.dp).background(crayon(note.color)))
        }
        AnimatedVisibility(vm.findOpen) {
            Row(Modifier.fillMaxWidth().background(p.canary).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                BrutalTextField(vm.find, { vm.find = it }, placeholder = "Buscar en la nota", leading = Icons.Filled.FindInPage, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                val count = if (vm.find.isBlank()) 0 else vm.blocks.sumOf { b -> Regex(Regex.escape(vm.find), RegexOption.IGNORE_CASE).findAll(b.field.text).count() }
                Text("$count", style = T.label, color = Color(0xFF383838))
                Spacer(Modifier.width(6.dp))
                TopIcon(Icons.Filled.Close, "Cerrar", tint = Color(0xFF383838)) { vm.find = ""; vm.findOpen = false }
            }
        }

        if (!vm.loaded) {
            Box(Modifier.weight(1f))
        } else if (locked) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                PinPad(
                    "Nota bloqueada", "Ingresá tu PIN",
                    onBiometric = if (settings.biometric && Biometric.available(context)) ({ Biometric.prompt(context, "Abrir nota") { unlocked = true } }) else null,
                ) { pin -> repo.prefs.checkPin(pin, settings).also { if (it) unlocked = true } }
            }
            LaunchedEffect(Unit) { if (settings.biometric && Biometric.available(context)) Biometric.prompt(context, "Abrir nota") { unlocked = true } }
        } else {
            // ---------- Cuerpo ----------
            val scroll = rememberScrollState()
            val paper = settings.paperStyle
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scroll)
                    .drawBehind {
                        val step = 28.dp.toPx()
                        when (paper) {
                            1 -> {
                                var y = step * 3
                                while (y < size.height) { drawLine(p.paperLine, Offset(0f, y), Offset(size.width, y), 1.dp.toPx()); y += step }
                                drawLine(p.danger.copy(alpha = 0.35f), Offset(14.dp.toPx(), 0f), Offset(14.dp.toPx(), size.height), 1.dp.toPx())
                            }
                            2 -> {
                                val g = 22.dp.toPx()
                                var y = 0f
                                while (y < size.height) { drawLine(p.paperLine, Offset(0f, y), Offset(size.width, y), 1f); y += g }
                                var x = 0f
                                while (x < size.width) { drawLine(p.paperLine, Offset(x, 0f), Offset(x, size.height), 1f); x += g }
                            }
                            3 -> {
                                val g = 20.dp.toPx()
                                var y = g
                                while (y < size.height) {
                                    var x = g
                                    while (x < size.width) { drawCircle(p.inkFaint.copy(alpha = 0.35f), 1.3f, Offset(x, y)); x += g }
                                    y += g
                                }
                            }
                        }
                    }
                    .padding(start = 24.dp, end = 18.dp, top = 18.dp),
            ) {
                // Título
                BasicTextField(
                    value = vm.title,
                    onValueChange = { vm.setTitle(it) },
                    textStyle = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 27.sp, lineHeight = 33.sp, color = p.ink, letterSpacing = 0.3.sp),
                    cursorBrush = SolidColor(p.ink),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        Box {
                            if (vm.title.text.isEmpty()) Text("Título", style = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 27.sp, color = p.inkFaint))
                            inner()
                        }
                    },
                )
                Spacer(Modifier.height(12.dp))
                // Metadatos
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.clip(BrutalShape).background(p.surface).border(1.5.dp, p.line, BrutalShape).clickable { sheet = "notebook" }.padding(end = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NotebookSpine(item?.notebook, size = 26)
                        Spacer(Modifier.width(8.dp))
                        Text(item?.notebook?.name ?: "—", style = T.caption.copy(fontWeight = FontWeight.SemiBold), color = p.ink, maxLines = 1)
                    }
                    item?.tags?.forEachIndexed { i, t ->
                        Sticker(t.name, crayon(t.color), rotation = if (i % 2 == 0) -1.5f else 1.2f, onClick = { sheet = "tags" })
                    }
                    Row(
                        Modifier.clip(BrutalShape).border(1.5.dp, p.inkFaint, BrutalShape).clickable { sheet = "tags" }.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Label, null, tint = p.inkSoft, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("ETIQUETA", style = T.micro, color = p.inkSoft)
                    }
                    if (note?.reminderAt != null) {
                        val done = note.reminderDone
                        Row(
                            Modifier.clip(BrutalShape).background(if (done) p.surfaceAlt else p.canary).border(1.5.dp, p.line, BrutalShape)
                                .clickable { sheet = "reminder" }.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Alarm, null, tint = Color(0xFF383838), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text((if (done) "✓ " else "") + Fmt.due(note.reminderAt), style = T.micro, color = if (done) p.inkSoft else Color(0xFF383838))
                        }
                    }
                    note?.sourceUrl?.let { url ->
                        Row(
                            Modifier.clip(BrutalShape).background(p.iceWash).border(1.5.dp, p.line, BrutalShape)
                                .clickable { openUrl(context, url) }.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Link, null, tint = p.ink, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(url.removePrefix("https://").removePrefix("http://").take(28), style = T.micro, color = p.ink, maxLines = 1)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                vm.blocks.forEachIndexed { i, b ->
                    androidx.compose.runtime.key(b.id) { BlockView(b, i, vm, env) }
                }
                // Zona para tocar y seguir escribiendo
                Box(
                    Modifier.fillMaxWidth().height(220.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        val last = vm.blocks.lastOrNull()
                        if (last != null && last.type == BlockType.PARAGRAPH && last.text.isEmpty()) vm.focus(last, 0)
                        else vm.insertBlocks(listOf(Block()))
                    },
                )
            }
            // ---------- Barra de formato ----------
            FormatToolbar(vm, onInsert = { sheet = "insert" }, onBlockType = { sheet = "blocktype" }, onCommand = { command(it) }, onLink = { sheet = "weblink" })
        }
    }

    // ---------- Hojas ----------
    when (sheet) {
        "menu" -> NoteMenuSheet(
            isPinned = note?.isPinned == true, isLocked = note?.isLocked == true,
            onDismiss = { sheet = null },
            onAction = { a ->
                sheet = null
                val blocks = vm.blocks.map { it.toBlock() }
                when (a) {
                    "reminder" -> sheet = "reminder"
                    "notebook" -> sheet = "notebook"
                    "tags" -> sheet = "tags"
                    "find" -> vm.findOpen = true
                    "pin" -> vm.meta { it.copy(isPinned = !it.isPinned) }
                    "color" -> sheet = "color"
                    "duplicate" -> scope.launch { vm.save(); repo.duplicate(vm.noteId)?.let { toast("Nota duplicada"); onOpenNote(it) } }
                    "share" -> Exporter.shareText(context, vm.title.text, blocks)
                    "pdf" -> Exporter.print(context, vm.title.text, blocks, repo)
                    "md" -> exportMd.launch((vm.title.text.ifBlank { "nota" }) + ".md")
                    "history" -> sheet = "history"
                    "info" -> sheet = "info"
                    "template" -> sheet = "template"
                    "lock" -> if (!settings.lockEnabled) toast("Primero configurá un PIN en Ajustes → Seguridad") else vm.meta { it.copy(isLocked = !it.isLocked) }.also { unlocked = true }
                    "trash" -> confirmTrash = true
                }
            },
        )
        "insert" -> InsertSheet(onDismiss = { sheet = null }) { sheet = null; command(it) }
        "blocktype" -> BlockTypeSheet(onDismiss = { sheet = null }) { sheet = null; vm.convertFocused(it) }
        "notebook" -> NotebookPickerSheet(notebooks, note?.notebookId, { sheet = null }, onCreate = { name ->
            scope.launch { val id = repo.createNotebook(name, (0..7).random(), null, null); vm.meta { it.copy(notebookId = id) } }
            sheet = null
        }) { nb -> vm.meta { it.copy(notebookId = nb.id, updatedAt = System.currentTimeMillis()) }; sheet = null }
        "tags" -> TagPickerSheet(
            tags, item?.tags?.map { it.id }?.toSet() ?: emptySet(), { sheet = null },
            onToggle = { t ->
                scope.launch { if (item?.tags?.any { it.id == t.id } == true) repo.removeTag(vm.noteId, t.id) else repo.addTag(vm.noteId, t.id) }
            },
            onCreate = { name -> scope.launch { val t = repo.createTag(name); repo.addTag(vm.noteId, t.id) } },
        )
        "reminder" -> ReminderSheet(
            current = note?.reminderAt, done = note?.reminderDone == true, onDismiss = { sheet = null },
            onPick = { at -> ensureNotifications(); scope.launch { repo.setReminder(vm.noteId, at) }; sheet = null },
            onCustom = { sheet = null; customDateFor = "note" },
            onClear = { scope.launch { repo.setReminder(vm.noteId, null) }; sheet = null },
            onDone = { scope.launch { repo.completeReminder(vm.noteId, !(note?.reminderDone ?: false)) }; sheet = null },
        )
        "color" -> BrutalSheet({ sheet = null }, "Color de la nota") {
            ColorDots(note?.color ?: -1, { c -> vm.meta { it.copy(color = c) }; sheet = null }, Crayons, allowNone = true)
            Spacer(Modifier.height(8.dp))
        }
        "notelink" -> NotePickerSheet(allNotes, vm.noteId, { sheet = null }) { n ->
            sheet = null
            vm.insertBlocks(listOf(Block(type = BlockType.NOTE_LINK, linkId = n.note.id, name = n.note.title.ifBlank { "Sin título" })))
        }
        "weblink" -> LinkDialog(vm.selectedText(), onDismiss = { sheet = null }) { label, url -> sheet = null; vm.insertLink(label, url) }
        "info" -> InfoSheet(vm, item, allNotes, onDismiss = { sheet = null }, onOpenNote = { sheet = null; onOpenNote(it) })
        "history" -> HistorySheet(repo.observeVersions(vm.noteId), onDismiss = { sheet = null }) { restoreVersion = it; sheet = null }
        "template" -> TextInputDialog("Guardar como plantilla", vm.title.text, "Nombre de la plantilla", onDismiss = { sheet = null }) { name ->
            sheet = null
            scope.launch { repo.saveTemplate(name, vm.title.text, vm.blocks.map { it.toBlock() }); toast("Plantilla guardada") }
        }
    }

    taskDueFor?.let { b ->
        BrutalSheet({ taskDueFor = null }, "Fecha de la tarea") {
            QuickDates { at -> vm.setTaskDue(b, at, b.reminderAt != null); taskDueFor = null }
            SheetItem(Icons.Filled.Alarm, "Elegir fecha y hora…") { customDateFor = "task:" + b.id; taskDueFor = null }
            SheetItem(Icons.Filled.Alarm, if (b.reminderAt != null) "Quitar aviso" else "Avisarme a esa hora", subtitle = "Notificación en el teléfono") {
                if (b.dueAt != null) { ensureNotifications(); vm.setTaskDue(b, b.dueAt, b.reminderAt == null) }
                else toast("Primero elegí una fecha")
                taskDueFor = null
            }
            if (b.dueAt != null) SheetItem(Icons.Filled.Delete, "Quitar fecha", tint = p.danger) { vm.setTaskDue(b, null, false); taskDueFor = null }
        }
    }
    customDateFor?.let { target ->
        val initial = if (target == "note") note?.reminderAt else vm.blocks.firstOrNull { "task:" + it.id == target }?.dueAt
        DateTimePickerDialog(initial, { customDateFor = null }) { at ->
            if (target == "note") { ensureNotifications(); scope.launch { repo.setReminder(vm.noteId, at) } }
            else vm.blocks.firstOrNull { "task:" + it.id == target }?.let { vm.setTaskDue(it, at, it.reminderAt != null) }
            customDateFor = null
        }
    }
    if (showSketch) SketchDialog(repo, { showSketch = false }) { showSketch = false; vm.insertBlocks(listOf(it)) }
    if (showRecorder) RecorderDialog(repo, { showRecorder = false }) { showRecorder = false; vm.insertBlocks(listOf(it)) }
    viewer?.let { ImageViewerDialog(it) { viewer = null } }
    if (confirmTrash) ConfirmDialog("Mover a la papelera", "La nota quedará en la papelera y podrás restaurarla.", "Mover", { confirmTrash = false }) {
        confirmTrash = false
        vm.save()
        scope.launch { repo.trash(vm.noteId) }
        onBack()
    }
    restoreVersion?.let { v ->
        ConfirmDialog("Restaurar versión", "Se reemplazará el contenido actual por la versión del ${Fmt.full(v.savedAt)}. La versión actual queda en el historial.", "Restaurar", { restoreVersion = null }, danger = false) {
            restoreVersion = null
            scope.launch { repo.snapshotVersion(vm.noteId) }
            vm.restoreFromVersion(v.content, v.title)
        }
    }
}

fun openUrl(context: android.content.Context, url: String) {
    runCatching {
        val u = if (url.startsWith("http")) url else "https://$url"
        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(u)))
    }
}

@Composable
private fun TopIcon(icon: ImageVector, desc: String, enabled: Boolean = true, tint: Color = LocalPalette.current.ink, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(BrutalShape).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = if (enabled) tint else LocalPalette.current.inkFaint.copy(alpha = 0.5f), modifier = Modifier.size(22.dp)) }
}

@Composable
private fun FormatToolbar(vm: EditorViewModel, onInsert: () -> Unit, onBlockType: () -> Unit, onCommand: (String) -> Unit, onLink: () -> Unit) {
    val p = LocalPalette.current
    val context = LocalContext.current
    // leer estado para recomponer al mover el cursor
    val focusedBlock = vm.focused
    @Suppress("UNUSED_VARIABLE") val sel = focusedBlock?.field?.selection
    val active = vm.activeKinds()
    val link = vm.linkAtCursor()
    val type = focusedBlock?.type
    Column(
        Modifier.fillMaxWidth().background(p.surface)
            .drawBehind { drawLine(p.line, Offset(0f, 0f), Offset(size.width, 0f), Dims.border.toPx()) }
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars)),
    ) {
        if (link != null) {
            Row(
                Modifier.fillMaxWidth().background(p.iceWash).clickable { openUrl(context, link) }.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = p.ink, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("ABRIR  " + link, style = T.micro, color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(38.dp).hardShadow(p.shadow, 2.dp).clip(BrutalShape).background(p.primary).border(1.5.dp, p.line, BrutalShape).clickable(onClick = onInsert),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Add, "Insertar", tint = p.onPrimary) }
            Spacer(Modifier.width(8.dp))
            ToolBtn(Icons.Filled.Title, "Tipo de bloque", false, onBlockType)
            Sep()
            ToolBtn(Icons.Filled.FormatBold, "Negrita", com.libreta.app.model.Inline.Kind.BOLD in active) { vm.toggleInline("**") }
            ToolBtn(Icons.Filled.FormatItalic, "Cursiva", com.libreta.app.model.Inline.Kind.ITALIC in active) { vm.toggleInline("*") }
            ToolBtn(Icons.Filled.FormatUnderlined, "Subrayado", com.libreta.app.model.Inline.Kind.UNDERLINE in active) { vm.toggleInline("++") }
            ToolBtn(Icons.Filled.FormatStrikethrough, "Tachado", com.libreta.app.model.Inline.Kind.STRIKE in active) { vm.toggleInline("~~") }
            ToolBtn(Icons.Outlined.BorderColor, "Resaltar", com.libreta.app.model.Inline.Kind.HIGHLIGHT in active) { vm.toggleInline("==") }
            ToolBtn(Icons.Filled.Code, "Código", com.libreta.app.model.Inline.Kind.CODE in active) { vm.toggleInline("`") }
            ToolBtn(Icons.Filled.Link, "Enlace", link != null, onLink)
            Sep()
            ToolBtn(Icons.Filled.CheckBox, "Casillas", type == BlockType.CHECK) { vm.convertFocused(BlockType.CHECK) }
            ToolBtn(Icons.Filled.TaskAlt, "Tarea", type == BlockType.TASK) { vm.convertFocused(BlockType.TASK) }
            ToolBtn(Icons.AutoMirrored.Filled.FormatListBulleted, "Viñetas", type == BlockType.BULLET) { vm.convertFocused(BlockType.BULLET) }
            ToolBtn(Icons.Filled.FormatListNumbered, "Numerada", type == BlockType.NUMBERED) { vm.convertFocused(BlockType.NUMBERED) }
            ToolBtn(Icons.AutoMirrored.Filled.FormatIndentIncrease, "Sangría", false) { vm.indent(1) }
            ToolBtn(Icons.AutoMirrored.Filled.FormatIndentDecrease, "Quitar sangría", false) { vm.indent(-1) }
            Sep()
            ToolBtn(Icons.Filled.Image, "Imagen", false) { onCommand("image") }
            ToolBtn(Icons.Filled.PhotoCamera, "Cámara", false) { onCommand("camera") }
            ToolBtn(Icons.Filled.DocumentScanner, "Escanear", false) { onCommand("scan") }
            ToolBtn(Icons.Filled.Mic, "Audio", false) { onCommand("audio") }
            ToolBtn(Icons.Filled.Brush, "Boceto", false) { onCommand("sketch") }
            ToolBtn(Icons.Filled.AttachFile, "Archivo", false) { onCommand("file") }
            ToolBtn(Icons.Filled.TableChart, "Tabla", false) { onCommand("table") }
            Sep()
            ToolBtn(Icons.Filled.FindInPage, "Buscar en nota", vm.findOpen) { vm.findOpen = !vm.findOpen }
        }
    }
}

@Composable
private fun ToolBtn(icon: ImageVector, desc: String, active: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(
        Modifier.padding(horizontal = 1.dp).size(38.dp).clip(BrutalShape)
            .then(if (active) Modifier.background(p.canary).border(1.5.dp, p.line, BrutalShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = if (active) Color(0xFF383838) else p.ink, modifier = Modifier.size(21.dp)) }
}

@Composable
private fun Sep() {
    val p = LocalPalette.current
    Box(Modifier.padding(horizontal = 6.dp).size(width = 1.5.dp, height = 24.dp).background(p.inkFaint))
}

@Composable
private fun NoteMenuSheet(isPinned: Boolean, isLocked: Boolean, onDismiss: () -> Unit, onAction: (String) -> Unit) {
    val p = LocalPalette.current
    BrutalSheet(onDismiss, "Nota") {
        SheetItem(Icons.Filled.Alarm, "Recordatorio") { onAction("reminder") }
        SheetItem(Icons.Filled.FolderOpen, "Mover a otra libreta") { onAction("notebook") }
        SheetItem(Icons.AutoMirrored.Filled.Label, "Etiquetas") { onAction("tags") }
        SheetItem(Icons.Filled.PushPin, if (isPinned) "Quitar del inicio" else "Fijar en el inicio") { onAction("pin") }
        SheetItem(Icons.Filled.Palette, "Color de la nota") { onAction("color") }
        SheetItem(Icons.Filled.FindInPage, "Buscar en la nota") { onAction("find") }
        SheetItem(Icons.Filled.ContentCopy, "Duplicar") { onAction("duplicate") }
        SheetItem(Icons.Filled.Share, "Compartir como texto") { onAction("share") }
        SheetItem(Icons.Filled.PictureAsPdf, "Exportar PDF / imprimir") { onAction("pdf") }
        SheetItem(Icons.Filled.Download, "Exportar Markdown (.md)") { onAction("md") }
        SheetItem(Icons.Filled.History, "Historial de versiones") { onAction("history") }
        SheetItem(Icons.Filled.Dashboard, "Guardar como plantilla") { onAction("template") }
        SheetItem(if (isLocked) Icons.Filled.LockOpen else Icons.Filled.Lock, if (isLocked) "Quitar bloqueo" else "Bloquear nota con PIN") { onAction("lock") }
        SheetItem(Icons.Filled.Info, "Información") { onAction("info") }
        SheetItem(Icons.Filled.Delete, "Mover a la papelera", tint = p.danger) { onAction("trash") }
    }
}

private data class InsertItem(val key: String, val label: String, val icon: ImageVector)

@Composable
private fun InsertSheet(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val p = LocalPalette.current
    val items = listOf(
        InsertItem("image", "Imagen", Icons.Filled.Image),
        InsertItem("camera", "Cámara", Icons.Filled.PhotoCamera),
        InsertItem("scan", "Escanear", Icons.Filled.DocumentScanner),
        InsertItem("sketch", "Boceto", Icons.Filled.Brush),
        InsertItem("audio", "Audio", Icons.Filled.Mic),
        InsertItem("file", "Archivo", Icons.Filled.AttachFile),
        InsertItem("table", "Tabla", Icons.Filled.TableChart),
        InsertItem("divider", "Separador", Icons.Filled.HorizontalRule),
        InsertItem("link", "Enlace a nota", Icons.Filled.Link),
        InsertItem("weblink", "Enlace web", Icons.Filled.Public),
    )
    BrutalSheet(onDismiss, "Insertar") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp), maxItemsInEachRow = 4) {
            items.forEachIndexed { i, it ->
                com.libreta.app.ui.components.Pressable(
                    Modifier.weight(1f), color = p.surface, borderColor = crayon(i), border = 2.5.dp, shadow = 3.dp, onClick = { onPick(it.key) },
                ) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(it.icon, null, tint = p.ink)
                        Spacer(Modifier.height(6.dp))
                        Text(it.label.uppercase(), style = T.micro, color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun BlockTypeSheet(onDismiss: () -> Unit, onPick: (BlockType) -> Unit) {
    BrutalSheet(onDismiss, "Convertir bloque en…") {
        SheetItem(Icons.Filled.TextFields, "Texto") { onPick(BlockType.PARAGRAPH) }
        SheetItem(Icons.Filled.Title, "Título 1") { onPick(BlockType.H1) }
        SheetItem(Icons.Filled.Title, "Título 2") { onPick(BlockType.H2) }
        SheetItem(Icons.Filled.Title, "Título 3") { onPick(BlockType.H3) }
        SheetItem(Icons.AutoMirrored.Filled.FormatListBulleted, "Viñetas") { onPick(BlockType.BULLET) }
        SheetItem(Icons.Filled.FormatListNumbered, "Lista numerada") { onPick(BlockType.NUMBERED) }
        SheetItem(Icons.Filled.CheckBox, "Casillas") { onPick(BlockType.CHECK) }
        SheetItem(Icons.Filled.TaskAlt, "Tarea") { onPick(BlockType.TASK) }
        SheetItem(Icons.Filled.FormatQuote, "Cita") { onPick(BlockType.QUOTE) }
        SheetItem(Icons.Filled.Code, "Bloque de código") { onPick(BlockType.CODE) }
        SheetItem(Icons.Filled.Info, "Destacado") { onPick(BlockType.CALLOUT) }
    }
}

@Composable
fun QuickDates(onPick: (Long) -> Unit) {
    val now = Calendar.getInstance()
    fun at(daysAhead: Int, hour: Int): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, daysAhead); set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val laterToday = if (now.get(Calendar.HOUR_OF_DAY) < 18) at(0, 18) else System.currentTimeMillis() + 3_600_000L
    val nextMonday = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 1)
        while (get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
    }.timeInMillis
    SheetItem(Icons.Filled.Alarm, "Más tarde", subtitle = Fmt.due(laterToday)) { onPick(laterToday) }
    SheetItem(Icons.Filled.Alarm, "Mañana", subtitle = Fmt.due(at(1, 9))) { onPick(at(1, 9)) }
    SheetItem(Icons.Filled.Alarm, "Próxima semana", subtitle = Fmt.due(nextMonday)) { onPick(nextMonday) }
}

@Composable
private fun ReminderSheet(current: Long?, done: Boolean, onDismiss: () -> Unit, onPick: (Long) -> Unit, onCustom: () -> Unit, onClear: () -> Unit, onDone: () -> Unit) {
    val p = LocalPalette.current
    BrutalSheet(onDismiss, "Recordatorio") {
        if (current != null) {
            Text(Fmt.full(current), style = T.body.copy(fontWeight = FontWeight.SemiBold), color = p.ink, modifier = Modifier.padding(bottom = 8.dp))
        }
        QuickDates(onPick)
        SheetItem(Icons.Filled.Alarm, "Elegir fecha y hora…", onClick = onCustom)
        if (current != null) {
            SheetItem(Icons.Filled.TaskAlt, if (done) "Marcar como pendiente" else "Marcar como hecho", onClick = onDone)
            SheetItem(Icons.Filled.Delete, "Quitar recordatorio", tint = p.danger, onClick = onClear)
        }
    }
}

@Composable
private fun LinkDialog(selected: String, onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var label by remember { mutableStateOf(selected) }
    var url by remember { mutableStateOf("https://") }
    BrutalDialog("Insertar enlace", onDismiss, "Insertar", { if (url.length > 8) onConfirm(label.ifBlank { url }, url.trim()) }) {
        BrutalTextField(label, { label = it }, placeholder = "Texto", modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        BrutalTextField(url, { url = it }, placeholder = "https://…", modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun InfoSheet(vm: EditorViewModel, item: com.libreta.app.data.NoteItem?, all: List<com.libreta.app.data.NoteItem>, onDismiss: () -> Unit, onOpenNote: (String) -> Unit) {
    val p = LocalPalette.current
    if (item == null) return
    val n = item.note
    val blocks = vm.blocks.map { it.toBlock() }
    val plain = BlockCodec.plainText(blocks)
    val words = BlockCodec.wordCount(vm.title.text + " " + plain)
    val backlinks = all.filter { it.note.id != n.id && it.note.content.contains("\"linkId\":\"${n.id}\"") }
    BrutalSheet(onDismiss, "Información") {
        @Composable
        fun row(k: String, v: String) {
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Text(k.uppercase(), style = T.micro, color = p.inkSoft, modifier = Modifier.width(120.dp))
                Text(v, style = T.bodySm, color = p.ink, modifier = Modifier.weight(1f))
            }
        }
        row("Creada", Fmt.full(n.createdAt))
        row("Modificada", Fmt.full(n.updatedAt))
        row("Libreta", item.notebook?.name ?: "—")
        row("Etiquetas", item.tags.joinToString { it.name }.ifBlank { "—" })
        row("Palabras", "$words")
        row("Caracteres", "${plain.length}")
        row("Lectura", "${(words / 220).coerceAtLeast(1)} min")
        row("Adjuntos", "${blocks.count { it.type == BlockType.IMAGE }} imágenes · ${blocks.count { it.type == BlockType.FILE }} archivos · ${blocks.count { it.type == BlockType.AUDIO }} audios")
        row("Tareas", "${blocks.count { it.type == BlockType.TASK && it.checked }}/${blocks.count { it.type == BlockType.TASK }} completadas")
        n.sourceUrl?.let { row("Fuente", it) }
        if (backlinks.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text("ENLAZADA DESDE", style = T.micro, color = p.inkSoft)
            backlinks.forEach { b ->
                SheetItem(Icons.Filled.Link, b.note.title.ifBlank { "Sin título" }) { onOpenNote(b.note.id) }
            }
        }
    }
}

@Composable
private fun HistorySheet(flow: kotlinx.coroutines.flow.Flow<List<NoteVersionEntity>>, onDismiss: () -> Unit, onPick: (NoteVersionEntity) -> Unit) {
    val p = LocalPalette.current
    val versions by flow.collectAsStateWithLifecycle(emptyList())
    BrutalSheet(onDismiss, "Historial de versiones") {
        if (versions.isEmpty()) {
            EmptyState(Doodle.HISTORY, "Sin versiones aún", "Libreta guarda una versión automáticamente mientras editás (cada 10 minutos de cambios).")
        }
        versions.forEach { v ->
            val preview = BlockCodec.preview(BlockCodec.decode(v.content), 90)
            Row(
                Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(BrutalShape).background(p.surface).border(1.5.dp, p.line, BrutalShape)
                    .clickable { onPick(v) }.padding(12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(Fmt.full(v.savedAt), style = T.caption.copy(fontWeight = FontWeight.SemiBold), color = p.ink)
                    Text(v.title.ifBlank { "Sin título" } + " — " + preview, style = T.caption, color = p.inkSoft, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Text("RESTAURAR", style = T.micro, color = p.ink, modifier = Modifier.align(Alignment.CenterVertically).padding(start = 8.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        BrutalButton("Cerrar", onDismiss, style = BtnStyle.Secondary, small = true)
    }
}
