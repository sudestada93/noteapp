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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.TaskAlt
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.libreta.app.data.HomeWidget
import com.libreta.app.model.Block
import com.libreta.app.ui.components.ArchGauge
import com.libreta.app.ui.components.ArchShape
import com.libreta.app.ui.components.Art
import com.libreta.app.ui.components.BrutalButton
import com.libreta.app.ui.components.BrutalSheet
import com.libreta.app.ui.components.BtnStyle
import com.libreta.app.ui.components.CrayonCheckbox
import com.libreta.app.ui.components.Engraving
import com.libreta.app.ui.components.NoteCard
import com.libreta.app.ui.components.SectionHeader
import com.libreta.app.ui.components.Sticker
import com.libreta.app.ui.components.hairlineBottom
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
    val g = Dims.gutter

    Box(Modifier.fillMaxSize().background(p.canvas)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 140.dp)) {
            // ---------- Portada ----------
            item {
                Column(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Spacer(Modifier.weight(1f))
                        Box(Modifier.size(44.dp).clip(CircleShape).clickable { nav.go("settings") }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Settings, "Ajustes", tint = p.inkSoft, modifier = Modifier.size(21.dp))
                        }
                    }
                    Box(
                        Modifier.padding(horizontal = 56.dp).fillMaxWidth().height(232.dp).border(1.dp, p.rule.copy(alpha = 0.55f), ArchShape),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 22.dp)) {
                            Engraving(Art.INKWELL, Modifier.height(122.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("L I B R E T A", style = T.label.copy(letterSpacing = T.label.letterSpacing * 1.6f), color = p.ink)
                        }
                    }
                    Box(Modifier.padding(horizontal = 56.dp).fillMaxWidth().height(1.dp).background(p.rule.copy(alpha = 0.55f)))
                    Spacer(Modifier.height(26.dp))
                    Text(Fmt.dayHeader(now).uppercase(), style = T.micro, color = p.inkFaint)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        buildAnnotatedString {
                            append(Fmt.greeting())
                            if (settings.userName.isNotBlank()) {
                                append(",\n")
                                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(settings.userName) }
                            }
                        },
                        style = T.display, color = p.ink, textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(26.dp))
                    Row(
                        Modifier.padding(horizontal = g).fillMaxWidth().clickable { nav.openSearch(null) }.hairlineBottom(p.line).padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Search, null, tint = p.inkFaint, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Buscar en ${notes.size} notas", style = T.subheading.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), color = p.inkFaint)
                    }
                }
            }
            // ---------- Accesos ----------
            item {
                val actions: List<Triple<String, ImageVector, () -> Unit>> = listOf(
                    Triple("Nota", Icons.AutoMirrored.Outlined.NoteAdd) { nav.newNote(null, null, null) },
                    Triple("Tarea", Icons.Outlined.TaskAlt) { nav.go("tasks?new=1") },
                    Triple("Audio", Icons.Outlined.Mic) { nav.newNote("audio", null, null) },
                    Triple("Foto", Icons.Outlined.CameraAlt) { nav.newNote("camera", null, null) },
                    Triple("Escanear", Icons.Outlined.DocumentScanner) { nav.newNote("scan", null, null) },
                    Triple("Boceto", Icons.Outlined.Brush) { nav.newNote("sketch", null, null) },
                    Triple("Plantilla", Icons.Outlined.Dashboard) { nav.newFromTemplate() },
                )
                LazyRow(contentPadding = PaddingValues(horizontal = g, vertical = 26.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    items(actions.size) { i ->
                        val (label, icon, action) = actions[i]
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(58.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = action)) {
                            Box(Modifier.size(54.dp).border(1.dp, p.line, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(icon, null, tint = p.ink, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(label.uppercase(), style = T.micro.copy(letterSpacing = T.micro.letterSpacing * 0.7f), color = p.inkSoft, maxLines = 1)
                        }
                    }
                }
            }
            settings.homeWidgets.forEach { w ->
                item(key = w.name) {
                    Column(Modifier.padding(horizontal = g).padding(bottom = 34.dp)) {
                        HomeSection(w, nav, notes, tasks, notebooks, tags, searches, settings.scratchpad, scope, repo)
                    }
                }
            }
            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    BrutalButton("Personalizar inicio", { customize = true }, style = BtnStyle.Ghost)
                }
            }
        }
        Box(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(p.canvas))
    }

    if (customize) {
        BrutalSheet({ customize = false }, "Personalizar inicio") {
            val enabled = settings.homeWidgets
            val order = enabled + HomeWidget.entries.filter { it !in enabled }
            order.forEach { w ->
                val on = w in enabled
                Row(Modifier.fillMaxWidth().hairlineBottom(p.line).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(w.label, style = T.subheading, color = if (on) p.ink else p.inkFaint, modifier = Modifier.weight(1f))
                    if (on) {
                        Icon(Icons.Filled.KeyboardArrowUp, "Subir", tint = p.inkSoft, modifier = Modifier.size(32.dp).clip(CircleShape).clickable {
                            val l = enabled.toMutableList(); val i = l.indexOf(w)
                            if (i > 0) { l.removeAt(i); l.add(i - 1, w); scope.launch { repo.prefs.setHomeWidgets(l) } }
                        })
                        Icon(Icons.Filled.KeyboardArrowDown, "Bajar", tint = p.inkSoft, modifier = Modifier.size(32.dp).clip(CircleShape).clickable {
                            val l = enabled.toMutableList(); val i = l.indexOf(w)
                            if (i < l.lastIndex) { l.removeAt(i); l.add(i + 1, w); scope.launch { repo.prefs.setHomeWidgets(l) } }
                        })
                        Spacer(Modifier.width(6.dp))
                    }
                    Switch(
                        on, { v -> scope.launch { repo.prefs.setHomeWidgets(if (v) enabled + w else enabled - w) } },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = p.primary, checkedThumbColor = p.onPrimary, checkedBorderColor = p.primary,
                            uncheckedTrackColor = Color.Transparent, uncheckedBorderColor = p.line, uncheckedThumbColor = p.inkFaint,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeSection(
    w: HomeWidget, nav: Nav,
    notes: List<com.libreta.app.data.NoteItem>, tasks: List<com.libreta.app.data.TaskEntity>,
    notebooks: List<com.libreta.app.data.NotebookEntity>, tags: List<com.libreta.app.data.TagEntity>,
    searches: List<com.libreta.app.data.SavedSearchEntity>, scratch: String,
    scope: kotlinx.coroutines.CoroutineScope, repo: com.libreta.app.data.Repository,
) {
    val p = LocalPalette.current
    val now = System.currentTimeMillis()
    when (w) {
        HomeWidget.SCRATCH -> ScratchPad(scratch, onSave = { scope.launch { repo.prefs.setScratchpad(it) } }) { text ->
            scope.launch {
                val lines = text.split('\n')
                val id = repo.createNote(title = lines.first().take(60), blocks = lines.drop(1).map { Block(text = it) }.ifEmpty { listOf(Block()) })
                repo.prefs.setScratchpad("")
                nav.openNote(id)
            }
        }
        HomeWidget.PINNED -> {
            val pinned = notes.firstOrNull { it.note.isPinned } ?: return
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(p.violet).clickable { nav.openNote(pinned.note.id) }.padding(26.dp),
            ) {
                Text("NOTA FIJADA", style = T.micro, color = p.onViolet.copy(alpha = 0.6f))
                Spacer(Modifier.height(12.dp))
                Text(pinned.note.title.ifBlank { "Sin título" }, style = T.headingLg, color = p.onViolet, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(10.dp))
                Text(pinned.note.preview, style = T.bodySm, color = p.onViolet.copy(alpha = 0.78f), maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("ABRIR", style = T.label, color = p.onViolet)
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.width(44.dp).height(1.dp).background(p.onViolet))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = p.onViolet, modifier = Modifier.size(14.dp))
                }
            }
        }
        HomeWidget.RECENT -> {
            SectionHeader("Recientes", action = "Ver todas") { nav.openNotes(null, null, null) }
            if (notes.isEmpty()) Hint("Tus notas aparecerán acá.")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(notes.filter { !it.note.isPinned }.take(10), key = { it.note.id }) { n ->
                    NoteCard(n, 2, Modifier.width(172.dp).heightIn(min = 200.dp), onClick = { nav.openNote(n.note.id) })
                }
            }
        }
        HomeWidget.TASKS -> {
            SectionHeader("Tareas", action = "Ver todas") { nav.go("tasks") }
            val pending = tasks.filter { !it.done }.take(5)
            if (pending.isEmpty()) Hint("Nada pendiente. Un buen momento para leer.")
            pending.forEach { t ->
                Row(Modifier.fillMaxWidth().hairlineBottom(p.line).padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    CrayonCheckbox(false, { scope.launch { repo.saveTask(t.copy(done = true)) } }, round = true)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f).clickable { if (t.noteId != null) nav.openNote(t.noteId) else nav.go("tasks") }) {
                        Text(t.title, style = T.subheading.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        t.dueAt?.let { d -> Text(Fmt.due(d), style = T.caption, color = if (d < now) p.danger else p.inkFaint) }
                    }
                    if (t.flagged) Icon(Icons.Outlined.Flag, null, tint = p.danger, modifier = Modifier.size(17.dp))
                }
            }
        }
        HomeWidget.ACTIVITY -> {
            SectionHeader("Actividad")
            val dayStart = Fmt.startOfDay(now)
            val todayTasks = tasks.filter { (it.dueAt != null && it.dueAt < dayStart + 86_400_000L && it.dueAt >= dayStart) || (it.done && (it.completedAt ?: 0) >= dayStart) }
            val doneToday = todayTasks.count { it.done }
            val weekNotes = notes.count { it.note.updatedAt >= now - 7 * 86_400_000L }
            val words = notes.filter { it.note.updatedAt >= now - 7 * 86_400_000L }.sumOf { it.note.wordCount }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ArchGauge(
                    "$doneToday", "Tareas hoy", if (todayTasks.isEmpty()) 0f else doneToday / todayTasks.size.toFloat(),
                    Modifier.weight(1f).height(190.dp), color = p.canary, caption = "de ${todayTasks.size}",
                )
                ArchGauge(
                    "$weekNotes", "Notas · 7 días", (weekNotes / 7f).coerceAtMost(1f),
                    Modifier.weight(1f).height(190.dp), color = p.ink, caption = "$words palabras",
                )
            }
        }
        HomeWidget.SHORTCUTS -> {
            val sNotes = notes.filter { it.note.isShortcut }
            val sNbs = notebooks.filter { it.isShortcut }
            val sTags = tags.filter { it.isShortcut }
            SectionHeader("Atajos")
            if (sNotes.isEmpty() && sNbs.isEmpty() && sTags.isEmpty() && searches.isEmpty()) Hint("Marcá notas con la estrella, o libretas y etiquetas como atajo.")
            sNotes.forEach { n -> LinkRow(Icons.Outlined.StarOutline, n.note.title.ifBlank { "Sin título" }, n.notebook?.name) { nav.openNote(n.note.id) } }
            sNbs.forEach { nb -> LinkRow(null, nb.name, "Libreta", dot = crayon(nb.color)) { nav.openNotes(nb.id, null, null) } }
            sTags.forEach { t -> LinkRow(null, "#" + t.name, "Etiqueta", dot = crayon(t.color)) { nav.openNotes(null, t.id, null) } }
            searches.forEach { s -> LinkRow(Icons.Outlined.Search, s.name, "Búsqueda") { nav.openSearch(s.query) } }
        }
        HomeWidget.REMINDERS -> {
            val rem = notes.filter { it.note.reminderAt != null && !it.note.reminderDone }.sortedBy { it.note.reminderAt }
            if (rem.isEmpty()) return
            SectionHeader("Recordatorios", action = "Ver") { nav.openNotes(null, null, "reminders") }
            rem.take(4).forEach { n ->
                LinkRow(Icons.Outlined.NotificationsNone, n.note.title.ifBlank { "Sin título" }, Fmt.due(n.note.reminderAt!!)) { nav.openNote(n.note.id) }
            }
        }
        HomeWidget.NOTEBOOKS -> {
            SectionHeader("Estante", action = "Libretas") { nav.go("notebooks") }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                items(notebooks, key = { it.id }) { nb ->
                    BookCover(nb.name, nb.emoji, nb.color, notes.count { it.note.notebookId == nb.id }) { nav.openNotes(nb.id, null, null) }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.5.dp).background(p.rule.copy(alpha = 0.7f)))
        }
        HomeWidget.TAGS -> {
            if (tags.isEmpty()) return
            SectionHeader("Etiquetas", action = "Todas") { nav.go("tags") }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.forEach { t -> Sticker(t.name, crayon(t.color), onClick = { nav.openNotes(null, t.id, null) }) }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = T.bodySm.copy(fontStyle = FontStyle.Italic), color = LocalPalette.current.inkFaint, modifier = Modifier.padding(vertical = 6.dp))
}

@Composable
private fun LinkRow(icon: ImageVector?, title: String, subtitle: String?, dot: Color? = null, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).hairlineBottom(p.line).padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) Icon(icon, null, tint = p.inkSoft, modifier = Modifier.size(18.dp))
        else if (dot != null) Box(Modifier.padding(horizontal = 5.dp).size(8.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(14.dp))
        Text(title, style = T.subheading.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), color = p.ink, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (subtitle != null) Text(subtitle.uppercase(), style = T.micro, color = p.inkFaint, maxLines = 1)
    }
}

/** Tapa de libro en el estante. */
@Composable
fun BookCover(name: String, emoji: String?, color: Int, count: Int, onClick: () -> Unit) {
    val ivory = Color(0xFFFBF8F1)
    Box(
        Modifier.width(96.dp).height(138.dp)
            .clip(RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 8.dp, bottomEnd = 8.dp))
            .background(crayon(color)).clickable(onClick = onClick),
    ) {
        Box(Modifier.width(9.dp).fillMaxHeight().background(Color(0x22000000)))
        Box(Modifier.padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp).fillMaxSize().border(0.8.dp, ivory.copy(alpha = 0.55f), RoundedCornerShape(3.dp)))
        Column(Modifier.padding(start = 22.dp, end = 12.dp, top = 20.dp, bottom = 16.dp).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(name, style = T.headingSm.copy(fontSize = T.headingSm.fontSize * 0.82f), color = ivory, textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.weight(1f))
            Box(Modifier.width(18.dp).height(0.8.dp).background(ivory.copy(alpha = 0.7f)))
            Spacer(Modifier.height(6.dp))
            Text("$count", style = T.micro, color = ivory.copy(alpha = 0.85f))
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
    SectionHeader("Borrador", action = if (text.isNotBlank()) "Convertir en nota" else null) { onConvert(text) }
    val lineH = 28.dp
    BasicTextField(
        text, { text = it },
        textStyle = T.body.copy(color = p.ink, fontSize = T.subheading.fontSize, lineHeight = androidx.compose.ui.unit.TextUnit(28f, androidx.compose.ui.unit.TextUnitType.Sp)),
        cursorBrush = SolidColor(p.ink),
        modifier = Modifier.fillMaxWidth().heightIn(min = lineH * 4)
            .drawBehind {
                val step = lineH.toPx()
                var y = step
                while (y <= size.height + 1) { drawLine(p.line, Offset(0f, y), Offset(size.width, y), 1.dp.toPx()); y += step }
            }
            .padding(top = 3.dp),
        decorationBox = { inner ->
            Box {
                if (text.isEmpty()) Text("Anotá algo al vuelo… se guarda solo.", style = T.subheading.copy(fontStyle = FontStyle.Italic, fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), color = p.inkFaint)
                inner()
            }
        },
    )
}
