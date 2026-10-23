package com.libreta.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.libreta.app.data.HomeWidget
import com.libreta.app.model.Block
import com.libreta.app.model.BlockType
import com.libreta.app.ui.components.BrutalButton
import com.libreta.app.ui.components.BrutalCard
import com.libreta.app.ui.components.BrutalShape
import com.libreta.app.ui.components.BrutalSheet
import com.libreta.app.ui.components.BtnStyle
import com.libreta.app.ui.components.CrayonCheckbox
import com.libreta.app.ui.components.DoodleArt
import com.libreta.app.ui.components.Doodle
import com.libreta.app.ui.components.Marquee
import com.libreta.app.ui.components.NoteCard
import com.libreta.app.ui.components.Pressable
import com.libreta.app.ui.components.SectionHeader
import com.libreta.app.ui.components.Sticker
import com.libreta.app.ui.components.hardShadow
import com.libreta.app.ui.components.rememberApp
import com.libreta.app.ui.components.settingsState
import com.libreta.app.ui.theme.Dims
import com.libreta.app.ui.theme.LocalPalette
import com.libreta.app.ui.theme.T
import com.libreta.app.ui.theme.crayon
import com.libreta.app.util.Fmt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(nav: Nav) {
    val app = rememberApp()
    val repo = app.repository
    val p = LocalPalette.current
    val settings by settingsState()
    val notes by repo.noteItems.collectAsStateWithLifecycle(emptyList())
    val tasks by repo.tasks.collectAsStateWithLifecycle(emptyList())
    val notebooks by repo.notebooks.collectAsStateWithLifecycle(emptyList())
    val tags by repo.tags.collectAsStateWithLifecycle(emptyList())
    val searches by repo.savedSearches.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    var customize by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val pendingToday = tasks.count { !it.done && it.dueAt != null && it.dueAt < Fmt.startOfDay(now) + 86_400_000L }

    Box(Modifier.fillMaxSize().background(p.canvas)) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 120.dp)) {
        item {
            Column(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = Dims.gutter, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(Fmt.dayHeader(now).uppercase(), style = T.micro, color = p.inkSoft)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            (Fmt.greeting() + if (settings.userName.isNotBlank()) ",\n${settings.userName}" else "").uppercase(),
                            style = T.display, color = p.ink,
                        )
                    }
                    DoodleArt(Doodle.NOTEBOOK, size = 86.dp, modifier = Modifier.rotate(6f))
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pressable(Modifier.weight(1f), onClick = { nav.openSearch(null) }) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Search, null, tint = p.inkSoft, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Buscar en ${notes.size} notas…", style = T.body, color = p.inkFaint)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Pressable(Modifier.size(46.dp), color = p.surface, onClick = { nav.go("settings") }) {
                        Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Settings, "Ajustes", tint = p.ink) }
                    }
                }
            }
        }
        item {
            Marquee("${notes.size} NOTAS  ·  ${tasks.count { !it.done }} TAREAS PENDIENTES  ·  $pendingToday PARA HOY  ·  ESCRIBÍ  ·  CAPTURÁ  ·  RECORDÁ")
        }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = Dims.gutter, vertical = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val actions = listOf(
                    Triple("Nota", Icons.AutoMirrored.Filled.NoteAdd) { nav.newNote(null, null, null) },
                    Triple("Tarea", Icons.Filled.TaskAlt) { nav.go("tasks?new=1") },
                    Triple("Audio", Icons.Filled.Mic) { nav.newNote("audio", null, null) },
                    Triple("Foto", Icons.Filled.PhotoCamera) { nav.newNote("camera", null, null) },
                    Triple("Escanear", Icons.Filled.DocumentScanner) { nav.newNote("scan", null, null) },
                    Triple("Boceto", Icons.Filled.Brush) { nav.newNote("sketch", null, null) },
                    Triple("Plantilla", Icons.Filled.Dashboard) { nav.newFromTemplate() },
                )
                items(actions.size) { i ->
                    val (label, icon, action) = actions[i]
                    QuickTile(label, icon, crayon(i), action)
                }
            }
        }
        settings.homeWidgets.forEach { w ->
            item(key = w.name) {
                Column(Modifier.padding(horizontal = Dims.gutter).padding(bottom = 22.dp)) {
                    when (w) {
                        HomeWidget.SCRATCH -> ScratchPad(settings.scratchpad, onSave = { scope.launch { repo.prefs.setScratchpad(it) } }) { text ->
                            scope.launch {
                                val lines = text.split('\n')
                                val id = repo.createNote(title = lines.first().take(60), blocks = lines.drop(1).map { Block(text = it) }.ifEmpty { listOf(Block()) })
                                repo.prefs.setScratchpad("")
                                nav.openNote(id)
                            }
                        }
                        HomeWidget.PINNED -> {
                            val pinned = notes.firstOrNull { it.note.isPinned }
                            SectionHeader("Nota fijada")
                            if (pinned != null) NoteCard(pinned, 1, onClick = { nav.openNote(pinned.note.id) })
                            else EmptyHint("Fijá una nota desde su menú ⋮ → \"Fijar en el inicio\".")
                        }
                        HomeWidget.RECENT -> {
                            SectionHeader("Recientes", action = "Ver todas") { nav.openNotes(null, null, null) }
                            if (notes.isEmpty()) EmptyHint("Tus notas aparecerán acá.")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(start = 6.dp, end = 4.dp, bottom = 6.dp)) {
                                items(notes.take(12), key = { it.note.id }) { n ->
                                    NoteCard(n, 2, Modifier.width(170.dp).heightIn(min = 190.dp), onClick = { nav.openNote(n.note.id) })
                                }
                            }
                        }
                        HomeWidget.TASKS -> {
                            SectionHeader("Mis tareas", action = "Ver todas") { nav.go("tasks") }
                            val pending = tasks.filter { !it.done }.take(5)
                            if (pending.isEmpty()) EmptyHint("Nada pendiente. ✦ Disfrutá el momento.")
                            else BrutalCard(Modifier.fillMaxWidth(), padding = PaddingValues(vertical = 4.dp)) {
                                pending.forEachIndexed { i, t ->
                                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        CrayonCheckbox(false, { scope.launch { repo.saveTask(t.copy(done = true)) } }, round = true, color = p.primary)
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f).clickable { if (t.noteId != null) nav.openNote(t.noteId) else nav.go("tasks") }) {
                                            Text(t.title, style = T.body, color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            t.dueAt?.let { d ->
                                                Text(Fmt.due(d), style = T.caption, color = if (d < System.currentTimeMillis()) p.danger else p.inkSoft)
                                            }
                                        }
                                        if (t.flagged) Icon(Icons.Filled.Flag, null, tint = p.orange, modifier = Modifier.size(18.dp))
                                    }
                                    if (i < pending.lastIndex) com.libreta.app.ui.components.Divider(Modifier.padding(horizontal = 14.dp), dashed = true)
                                }
                            }
                        }
                        HomeWidget.SHORTCUTS -> {
                            SectionHeader("Atajos")
                            val sNotes = notes.filter { it.note.isShortcut }
                            val sNbs = notebooks.filter { it.isShortcut }
                            val sTags = tags.filter { it.isShortcut }
                            if (sNotes.isEmpty() && sNbs.isEmpty() && sTags.isEmpty() && searches.isEmpty()) EmptyHint("Marcá notas con ★, o libretas y etiquetas como atajo.")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                sNotes.forEach { n -> ShortcutChip("★ " + n.note.title.ifBlank { "Sin título" }, p.canary) { nav.openNote(n.note.id) } }
                                sNbs.forEach { nb -> ShortcutChip("${nb.emoji ?: "▤"} ${nb.name}", crayon(nb.color)) { nav.openNotes(nb.id, null, null) } }
                                sTags.forEach { t -> ShortcutChip("# ${t.name}", crayon(t.color)) { nav.openNotes(null, t.id, null) } }
                                searches.forEach { s -> ShortcutChip("⌕ ${s.name}", p.iceWash) { nav.openSearch(s.query) } }
                            }
                        }
                        HomeWidget.REMINDERS -> {
                            val rem = notes.filter { it.note.reminderAt != null && !it.note.reminderDone }.sortedBy { it.note.reminderAt }
                            SectionHeader("Recordatorios", action = if (rem.isNotEmpty()) "Ver" else null) { nav.openNotes(null, null, "reminders") }
                            if (rem.isEmpty()) EmptyHint("Sin recordatorios activos.")
                            rem.take(4).forEach { n ->
                                Row(
                                    Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(BrutalShape).background(p.surface).border(1.5.dp, p.line, BrutalShape)
                                        .clickable { nav.openNote(n.note.id) }.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Filled.Alarm, null, tint = if ((n.note.reminderAt ?: 0) < System.currentTimeMillis()) p.danger else p.ink, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text(n.note.title.ifBlank { "Sin título" }, style = T.body, color = p.ink, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(Fmt.due(n.note.reminderAt!!), style = T.caption, color = p.inkSoft)
                                }
                            }
                        }
                        HomeWidget.NOTEBOOKS -> {
                            SectionHeader("Libretas", action = "Todas") { nav.go("notebooks") }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(start = 6.dp, bottom = 6.dp)) {
                                items(notebooks, key = { it.id }) { nb ->
                                    val count = notes.count { it.note.notebookId == nb.id }
                                    BookCover(nb.name, nb.emoji, nb.color, count) { nav.openNotes(nb.id, null, null) }
                                }
                            }
                        }
                        HomeWidget.TAGS -> {
                            SectionHeader("Etiquetas", action = "Todas") { nav.go("tags") }
                            if (tags.isEmpty()) EmptyHint("Sin etiquetas todavía.")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                tags.forEachIndexed { i, t ->
                                    Sticker(t.name, crayon(t.color), rotation = listOf(-2f, 1.5f, -0.5f, 2f)[i % 4], onClick = { nav.openNotes(null, t.id, null) })
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Box(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) {
                BrutalButton("Personalizar inicio", { customize = true }, icon = Icons.Filled.Tune, style = BtnStyle.Secondary, small = true)
            }
        }
    }

    // franja bajo la barra de estado para que el contenido no se superponga al desplazar
    Box(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(p.canvas))
    }

    if (customize) {
        BrutalSheet({ customize = false }, "Personalizar inicio") {
            val enabled = settings.homeWidgets
            val order = enabled + HomeWidget.entries.filter { it !in enabled }
            order.forEach { w ->
                val on = w in enabled
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(w.label, style = T.body, color = p.ink, modifier = Modifier.weight(1f))
                    if (on) {
                        Icon(Icons.Filled.KeyboardArrowUp, "Subir", tint = p.ink, modifier = Modifier.size(30.dp).clip(BrutalShape).clickable {
                            val l = enabled.toMutableList(); val i = l.indexOf(w)
                            if (i > 0) { l.removeAt(i); l.add(i - 1, w); scope.launch { repo.prefs.setHomeWidgets(l) } }
                        })
                        Icon(Icons.Filled.KeyboardArrowDown, "Bajar", tint = p.ink, modifier = Modifier.size(30.dp).clip(BrutalShape).clickable {
                            val l = enabled.toMutableList(); val i = l.indexOf(w)
                            if (i < l.lastIndex) { l.removeAt(i); l.add(i + 1, w); scope.launch { repo.prefs.setHomeWidgets(l) } }
                        })
                    }
                    Switch(
                        on, { v -> scope.launch { repo.prefs.setHomeWidgets(if (v) enabled + w else enabled - w) } },
                        colors = SwitchDefaults.colors(checkedTrackColor = p.primary, checkedThumbColor = p.surface, uncheckedTrackColor = p.surfaceAlt, uncheckedBorderColor = p.line, checkedBorderColor = p.line, uncheckedThumbColor = p.ink),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    val p = LocalPalette.current
    Text(
        text, style = T.bodySm, color = p.inkSoft,
        modifier = Modifier.fillMaxWidth().clip(BrutalShape).border(1.5.dp, p.inkFaint, BrutalShape).padding(14.dp),
    )
}

@Composable
private fun QuickTile(label: String, icon: ImageVector, border: Color, onClick: () -> Unit) {
    val p = LocalPalette.current
    Pressable(Modifier.width(84.dp), color = p.surface, borderColor = border, border = 2.5.dp, shadow = 4.dp, onClick = onClick) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(36.dp).clip(BrutalShape).background(border).border(1.5.dp, p.line, BrutalShape),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = Color(0xFF383838), modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.height(8.dp))
            Text(label.uppercase(), style = T.micro, color = p.ink, maxLines = 1)
        }
    }
}

@Composable
private fun ShortcutChip(text: String, color: Color, onClick: () -> Unit) {
    val p = LocalPalette.current
    Pressable(color = p.surface, shadow = 3.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(width = 8.dp, height = 36.dp).background(color))
            Box(Modifier.size(width = 1.5.dp, height = 36.dp).background(p.line))
            Text(text, style = T.bodySm.copy(fontWeight = FontWeight.SemiBold), color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 10.dp).widthIn(max = 220.dp))
        }
    }
}


@Composable
fun BookCover(name: String, emoji: String?, color: Int, count: Int, onClick: () -> Unit) {
    val p = LocalPalette.current
    Pressable(Modifier.size(width = 112.dp, height = 148.dp), color = crayon(color), shadow = 5.dp, onClick = onClick) {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.width(14.dp).fillMaxSize().background(Color(0x33000000)))
            Box(Modifier.width(2.dp).fillMaxSize().background(p.line))
            Column(Modifier.padding(10.dp).fillMaxSize()) {
                Text(emoji ?: "▤", style = T.heading)
                Spacer(Modifier.weight(1f))
                Box(Modifier.fillMaxWidth().background(Color.White).border(1.5.dp, Color(0xFF383838)).padding(6.dp)) {
                    Text(name, style = T.caption.copy(fontWeight = FontWeight.Bold), color = Color(0xFF383838), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(6.dp))
                Text("$count NOTAS", style = T.micro, color = Color(0xFF383838))
            }
        }
    }
}

@Composable
private fun ScratchPad(initial: String, onSave: (String) -> Unit, onConvert: (String) -> Unit) {
    val p = LocalPalette.current
    var text by remember(initial.isEmpty()) { mutableStateOf(initial) }
    LaunchedEffect(text) {
        delay(600)
        if (text != initial) onSave(text)
    }
    SectionHeader("Borrador rápido", action = if (text.isNotBlank()) "Convertir en nota" else null) { onConvert(text) }
    Box(
        Modifier.fillMaxWidth().hardShadow(p.shadow, 5.dp).clip(BrutalShape).background(Color(0xFFFFF7B8)).border(Dims.border, p.line, BrutalShape),
    ) {
        BasicTextField(
            text, { text = it },
            textStyle = T.body.copy(color = Color(0xFF383838)),
            cursorBrush = SolidColor(Color(0xFF383838)),
            modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp).padding(14.dp),
            decorationBox = { inner ->
                Box {
                    if (text.isEmpty()) Text("Anotá algo al vuelo… se guarda solo.", style = T.body, color = Color(0xFF8A8478))
                    inner()
                }
            },
        )
    }
}
