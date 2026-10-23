package com.libreta.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OutlinedFlag
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.libreta.app.data.TaskEntity
import com.libreta.app.ui.components.BrutalButton
import com.libreta.app.ui.components.BrutalShape
import com.libreta.app.ui.components.BrutalSheet
import com.libreta.app.ui.components.BrutalTextField
import com.libreta.app.ui.components.BtnStyle
import com.libreta.app.ui.components.CrayonCheckbox
import com.libreta.app.ui.components.DateTimePickerDialog
import com.libreta.app.ui.components.Doodle
import com.libreta.app.ui.components.EmptyState
import com.libreta.app.ui.components.FilterPill
import com.libreta.app.ui.components.LibretaTopBar
import com.libreta.app.ui.components.SheetItem
import com.libreta.app.ui.components.hardShadow
import com.libreta.app.ui.components.rememberApp
import com.libreta.app.ui.editor.QuickDates
import com.libreta.app.ui.theme.Dims
import com.libreta.app.ui.theme.LocalPalette
import com.libreta.app.ui.theme.T
import com.libreta.app.util.Fmt
import kotlinx.coroutines.launch

private val TaskFilters = listOf("pending" to "Pendientes", "today" to "Hoy", "week" to "7 días", "overdue" to "Vencidas", "flagged" to "Marcadas", "nodate" to "Sin fecha", "done" to "Completadas")

@Composable
fun TasksScreen(nav: Nav, focusNew: Boolean) {
    val app = rememberApp()
    val repo = app.repository
    val p = LocalPalette.current
    val tasks by repo.tasks.collectAsStateWithLifecycle(emptyList())
    val notes by repo.noteItems.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    var filter by remember { mutableStateOf("pending") }
    var newTitle by remember { mutableStateOf("") }
    var newDue by remember { mutableStateOf<Long?>(null) }
    var newFlag by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TaskEntity?>(null) }
    var pickDateFor by remember { mutableStateOf<String?>(null) }
    val fr = remember { FocusRequester() }
    LaunchedEffect(focusNew) { if (focusNew) runCatching { fr.requestFocus() } }

    val now = System.currentTimeMillis()
    val todayEnd = Fmt.startOfDay(now) + 86_400_000L
    val filtered = when (filter) {
        "today" -> tasks.filter { !it.done && it.dueAt != null && it.dueAt < todayEnd }
        "week" -> tasks.filter { !it.done && it.dueAt != null && it.dueAt < todayEnd + 6 * 86_400_000L }
        "overdue" -> tasks.filter { !it.done && it.dueAt != null && it.dueAt < now }
        "flagged" -> tasks.filter { !it.done && it.flagged }
        "nodate" -> tasks.filter { !it.done && it.dueAt == null }
        "done" -> tasks.filter { it.done }.sortedByDescending { it.completedAt }
        else -> tasks.filter { !it.done }
    }
    val groups: List<Pair<String, List<TaskEntity>>> = if (filter == "done") listOf("Completadas" to filtered) else {
        val overdue = filtered.filter { it.dueAt != null && it.dueAt < Fmt.startOfDay(now) }
        val today = filtered.filter { it.dueAt != null && it.dueAt >= Fmt.startOfDay(now) && it.dueAt < todayEnd }
        val tomorrow = filtered.filter { it.dueAt != null && it.dueAt >= todayEnd && it.dueAt < todayEnd + 86_400_000L }
        val later = filtered.filter { it.dueAt != null && it.dueAt >= todayEnd + 86_400_000L }
        val none = filtered.filter { it.dueAt == null }
        listOf("Vencidas" to overdue, "Hoy" to today, "Mañana" to tomorrow, "Más adelante" to later, "Sin fecha" to none).filter { it.second.isNotEmpty() }
    }
    fun add() {
        if (newTitle.isBlank()) return
        val t = newTitle; val d = newDue; val f = newFlag
        scope.launch { repo.createTask(t, d, f) }
        newTitle = ""; newDue = null; newFlag = false
    }

    Column(Modifier.fillMaxSize().background(p.canvas)) {
        LibretaTopBar("Tareas", subtitle = "${tasks.count { !it.done }} pendientes · ${tasks.count { it.done }} hechas")
        // Agregar rápido
        Column(Modifier.fillMaxWidth().background(p.canary).padding(16.dp)) {
            BrutalTextField(
                newTitle, { newTitle = it }, placeholder = "Agregar una tarea…", leading = Icons.Filled.Add,
                modifier = Modifier.fillMaxWidth().focusRequester(fr),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { add() }),
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterPill(newDue?.let { Fmt.due(it) } ?: "Fecha", newDue != null, { pickDateFor = "new" }, icon = Icons.Filled.Alarm)
                FilterPill("Bandera", newFlag, { newFlag = !newFlag }, icon = Icons.Filled.Flag)
                Spacer(Modifier.weight(1f))
                BrutalButton("Agregar", { add() }, small = true, enabled = newTitle.isNotBlank())
            }
        }
        Box(Modifier.fillMaxWidth().height(Dims.border).background(p.line))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(TaskFilters.size) { i ->
                val (k, l) = TaskFilters[i]
                FilterPill(l, filter == k, { filter = k })
            }
        }
        if (filtered.isEmpty()) {
            EmptyState(Doodle.TASKS, if (filter == "done") "Nada completado aún" else "Todo al día", "Agregá tareas acá arriba, o escribí \"[!] \" dentro de cualquier nota.")
        } else {
            LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 16.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                groups.forEach { (label, list) ->
                    item(key = "h_$label") {
                        Text(
                            "${label.uppercase()} · ${list.size}", style = T.label,
                            color = if (label == "Vencidas") p.danger else p.ink,
                            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                        )
                    }
                    items(list, key = { it.id }) { t ->
                        val noteTitle = t.noteId?.let { id -> notes.firstOrNull { it.note.id == id }?.note?.title?.ifBlank { "Sin título" } }
                        SwipeTask(
                            t, noteTitle, Modifier.animateItem(),
                            onToggle = { scope.launch { repo.saveTask(t.copy(done = !t.done)) } },
                            onFlag = { scope.launch { repo.saveTask(t.copy(flagged = !t.flagged)) } },
                            onClick = { editing = t },
                            onDelete = { scope.launch { repo.deleteTask(t) } },
                            onOpenNote = { t.noteId?.let(nav.openNote) },
                        )
                    }
                }
            }
        }
    }

    editing?.let { t0 ->
        val t = tasks.firstOrNull { it.id == t0.id } ?: t0
        var title by remember(t.id) { mutableStateOf(t.title) }
        BrutalSheet({ if (title.isNotBlank() && title != t.title) scope.launch { repo.saveTask(t.copy(title = title)) }; editing = null }, "Editar tarea") {
            BrutalTextField(title, { title = it }, placeholder = "Título", modifier = Modifier.fillMaxWidth(), singleLine = false)
            Spacer(Modifier.height(12.dp))
            t.dueAt?.let { Text("Vence: " + Fmt.full(it), style = T.bodySm, color = p.ink) }
            QuickDates { at -> scope.launch { repo.saveTask(t.copy(title = title, dueAt = at, reminderAt = if (t.reminderAt != null) at else null)) } }
            SheetItem(Icons.Filled.Alarm, "Elegir fecha y hora…") { pickDateFor = t.id }
            SheetItem(Icons.Filled.NotificationsActive, if (t.reminderAt != null) "Quitar aviso" else "Avisarme al vencer", subtitle = t.reminderAt?.let { Fmt.due(it) }) {
                scope.launch { repo.saveTask(t.copy(reminderAt = if (t.reminderAt != null) null else t.dueAt)) }
            }
            SheetItem(if (t.flagged) Icons.Filled.Flag else Icons.Filled.OutlinedFlag, if (t.flagged) "Quitar bandera" else "Marcar con bandera", tint = if (t.flagged) p.orange else p.ink) {
                scope.launch { repo.saveTask(t.copy(flagged = !t.flagged)) }
            }
            if (t.dueAt != null) SheetItem(Icons.Filled.Alarm, "Quitar fecha") { scope.launch { repo.saveTask(t.copy(dueAt = null, reminderAt = null)) } }
            if (t.noteId != null) SheetItem(Icons.AutoMirrored.Filled.Notes, "Abrir nota") { editing = null; nav.openNote(t.noteId) }
            SheetItem(Icons.Filled.Delete, "Eliminar tarea", tint = p.danger) { scope.launch { repo.deleteTask(t) }; editing = null }
            Spacer(Modifier.height(8.dp))
            BrutalButton("Listo", {
                if (title.isNotBlank() && title != t.title) scope.launch { repo.saveTask(t.copy(title = title)) }
                editing = null
            }, style = BtnStyle.Primary, small = true)
        }
    }
    pickDateFor?.let { target ->
        val task = tasks.firstOrNull { it.id == target }
        DateTimePickerDialog(if (target == "new") newDue else task?.dueAt, { pickDateFor = null }) { at ->
            if (target == "new") newDue = at
            else task?.let { scope.launch { repo.saveTask(it.copy(dueAt = at, reminderAt = if (it.reminderAt != null) at else null)) } }
            pickDateFor = null
        }
    }
}

@Composable
private fun SwipeTask(
    t: TaskEntity, noteTitle: String?, modifier: Modifier,
    onToggle: () -> Unit, onFlag: () -> Unit, onClick: () -> Unit, onDelete: () -> Unit, onOpenNote: () -> Unit,
) {
    val p = LocalPalette.current
    val state = rememberSwipeToDismissBoxState(confirmValueChange = { v ->
        when (v) {
            SwipeToDismissBoxValue.StartToEnd -> { onToggle(); false }
            SwipeToDismissBoxValue.EndToStart -> { onDelete(); true }
            else -> false
        }
    })
    SwipeToDismissBox(
        state = state, modifier = modifier,
        backgroundContent = {
            val dir = state.dismissDirection
            val bg by animateColorAsState(if (dir == SwipeToDismissBoxValue.EndToStart) p.danger else p.success, label = "swipe")
            Box(
                Modifier.fillMaxSize().clip(BrutalShape).background(bg).border(Dims.border, p.line, BrutalShape).padding(horizontal = 20.dp),
                contentAlignment = if (dir == SwipeToDismissBoxValue.EndToStart) Alignment.CenterEnd else Alignment.CenterStart,
            ) { Text(if (dir == SwipeToDismissBoxValue.EndToStart) "ELIMINAR" else if (t.done) "REABRIR" else "COMPLETAR", style = T.label, color = Color(0xFF383838)) }
        },
    ) {
        val overdue = !t.done && t.dueAt != null && t.dueAt < System.currentTimeMillis()
        Row(
            Modifier.fillMaxWidth().hardShadow(p.shadow, if (t.done) 0.dp else 3.dp).clip(BrutalShape)
                .background(if (t.done) p.surfaceAlt else p.surface).border(Dims.borderThin, if (t.done) p.inkFaint else p.line, BrutalShape)
                .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CrayonCheckbox(t.done, onToggle, round = true, color = p.primary, size = 22.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    t.title, style = T.body.copy(fontWeight = FontWeight.Medium, textDecoration = if (t.done) TextDecoration.LineThrough else null),
                    color = if (t.done) p.inkFaint else p.ink, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                    t.dueAt?.let {
                        Text(
                            Fmt.due(it) + if (t.reminderAt != null) " 🔔" else "", style = T.micro,
                            color = if (overdue) Color(0xFF383838) else p.inkSoft,
                            modifier = Modifier.clip(BrutalShape).background(if (overdue) p.danger else Color.Transparent).padding(horizontal = if (overdue) 4.dp else 0.dp, vertical = 1.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    if (noteTitle != null) {
                        Text("↳ $noteTitle", style = T.micro, color = p.inkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.clickable(onClick = onOpenNote))
                    }
                }
            }
            Icon(
                if (t.flagged) Icons.Filled.Flag else Icons.Filled.OutlinedFlag, "Bandera",
                tint = if (t.flagged) p.orange else p.inkFaint, modifier = Modifier.size(22.dp).clickable(onClick = onFlag),
            )
        }
    }
}
