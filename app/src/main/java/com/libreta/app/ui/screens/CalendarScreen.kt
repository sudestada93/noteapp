package com.libreta.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.libreta.app.ui.components.BareIcon
import com.libreta.app.ui.components.BrutalShape
import com.libreta.app.ui.components.CrayonCheckbox
import com.libreta.app.ui.components.LibretaTopBar
import com.libreta.app.ui.components.hardShadow
import com.libreta.app.ui.components.hairlineBottom
import com.libreta.app.ui.components.rememberApp
import com.libreta.app.ui.theme.Dims
import com.libreta.app.ui.theme.LocalPalette
import com.libreta.app.ui.theme.T
import com.libreta.app.util.Fmt
import kotlinx.coroutines.launch
import java.util.Calendar

private data class DayEntry(val kind: Int, val title: String, val time: Long, val noteId: String?, val taskId: String?, val done: Boolean)

@Composable
fun CalendarScreen(nav: Nav) {
    val repo = rememberApp().repository
    val p = LocalPalette.current
    val notes by repo.noteItems.collectAsStateWithLifecycle(emptyList())
    val tasks by repo.tasks.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    var month by remember { mutableLongStateOf(Fmt.startOfDay(System.currentTimeMillis())) }
    var selected by remember { mutableLongStateOf(Fmt.startOfDay(System.currentTimeMillis())) }

    // kind: 0 nota editada/creada, 1 recordatorio, 2 tarea
    val entries = remember(notes, tasks) {
        val l = mutableListOf<DayEntry>()
        notes.forEach { n ->
            l += DayEntry(0, n.note.title.ifBlank { "Sin título" }, n.note.updatedAt, n.note.id, null, false)
            n.note.reminderAt?.let { l += DayEntry(1, n.note.title.ifBlank { "Sin título" }, it, n.note.id, null, n.note.reminderDone) }
        }
        tasks.forEach { t -> t.dueAt?.let { l += DayEntry(2, t.title, it, t.noteId, t.id, t.done) } }
        l
    }
    val byDay = remember(entries) { entries.groupBy { Fmt.startOfDay(it.time) } }

    val cal = Calendar.getInstance().apply { timeInMillis = month; set(Calendar.DAY_OF_MONTH, 1) }
    val firstDow = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // lunes = 0
    val days = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val monthStart = cal.timeInMillis
    val today = Fmt.startOfDay(System.currentTimeMillis())

    Column(Modifier.fillMaxSize().background(p.canvas)) {
        LibretaTopBar("Calendario", onBack = nav.back) {
            BareIcon(Icons.Filled.Today, "Hoy", { month = today; selected = today })
        }
        Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BareIcon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Anterior", {
                    month = Calendar.getInstance().apply { timeInMillis = monthStart; add(Calendar.MONTH, -1) }.timeInMillis
                })
                Text(Fmt.monthYear(monthStart), style = T.headingLg, color = p.ink, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                BareIcon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Siguiente", {
                    month = Calendar.getInstance().apply { timeInMillis = monthStart; add(Calendar.MONTH, 1) }.timeInMillis
                })
            }
            Spacer(Modifier.height(10.dp))
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    listOf("L", "M", "M", "J", "V", "S", "D").forEach {
                        Text(it, style = T.micro, color = p.inkFaint, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                }
                val cells = firstDow + days
                val rows = (cells + 6) / 7
                for (r in 0 until rows) {
                    Row(Modifier.fillMaxWidth()) {
                        for (c in 0 until 7) {
                            val idx = r * 7 + c
                            val day = idx - firstDow + 1
                            Box(Modifier.weight(1f).aspectRatio(1f)) {
                                if (day in 1..days) {
                                    val ts = Calendar.getInstance().apply { timeInMillis = monthStart; set(Calendar.DAY_OF_MONTH, day) }.timeInMillis
                                    val d = Fmt.startOfDay(ts)
                                    val list = byDay[d].orEmpty()
                                    val isSel = d == selected
                                    val isToday = d == today
                                    Column(
                                        Modifier.fillMaxSize().padding(4.dp).clip(CircleShape)
                                            .background(if (isSel) p.ink else Color.Transparent)
                                            .then(if (isToday && !isSel) Modifier.border(1.dp, p.canary, CircleShape) else Modifier)
                                            .clickable { selected = d },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Text("$day", style = T.headingSm.copy(fontSize = T.headingSm.fontSize * 0.85f), color = if (isSel) p.canvas else p.ink)
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            if (list.any { it.kind == 0 }) Dot(p.art)
                                            if (list.any { it.kind == 1 }) Dot(p.canary)
                                            if (list.any { it.kind == 2 }) Dot(p.success)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Legend(p.art, "Notas"); Legend(p.canary, "Recordatorios"); Legend(p.success, "Tareas")
            }
        }
        Text(Fmt.dayHeader(selected), style = T.heading, color = p.ink, modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp))
        val dayList = byDay[selected].orEmpty().sortedBy { it.time }
        LazyColumn(contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 4.dp, bottom = 120.dp)) {
            if (dayList.isEmpty()) item { Text("Nada este día.", style = T.bodySm, color = p.inkSoft) }
            items(dayList) { e ->
                val icon: ImageVector = when (e.kind) { 0 -> Icons.Filled.EditNote; 1 -> Icons.Filled.Alarm; else -> Icons.Filled.TaskAlt }
                Row(
                    Modifier.fillMaxWidth().clickable { e.noteId?.let(nav.openNote) ?: nav.go("tasks") }.hairlineBottom(p.line).padding(vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (e.kind == 2 && e.taskId != null) {
                        CrayonCheckbox(e.done, {
                            scope.launch { tasks.firstOrNull { it.id == e.taskId }?.let { repo.saveTask(it.copy(done = !it.done)) } }
                        }, round = true)
                    } else Icon(icon, null, tint = p.ink, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(e.title, style = T.body, color = if (e.done) p.inkFaint else p.ink, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(Fmt.time(e.time), style = T.caption, color = p.inkSoft)
                }
            }
        }
    }
}

@Composable
private fun Dot(c: Color) = Box(Modifier.size(5.dp).clip(CircleShape).background(c))

@Composable
private fun Legend(c: Color, label: String) {
    val p = LocalPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(c))
        Spacer(Modifier.width(5.dp))
        Text(label.uppercase(), style = T.micro, color = p.inkSoft)
    }
}
