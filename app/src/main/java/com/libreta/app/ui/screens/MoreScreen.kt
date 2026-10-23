package com.libreta.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.libreta.app.ui.components.Art
import com.libreta.app.ui.components.Engraving
import com.libreta.app.ui.components.hairlineBottom
import com.libreta.app.ui.components.rememberApp
import com.libreta.app.ui.theme.Dims
import com.libreta.app.ui.theme.LocalPalette
import com.libreta.app.ui.theme.T

private data class MoreItem(val label: String, val sub: String, val icon: ImageVector, val action: () -> Unit)

@Composable
fun MoreScreen(nav: Nav) {
    val repo = rememberApp().repository
    val p = LocalPalette.current
    val notes by repo.noteItems.collectAsStateWithLifecycle(emptyList())
    val notebooks by repo.notebooks.collectAsStateWithLifecycle(emptyList())
    val tags by repo.tags.collectAsStateWithLifecycle(emptyList())
    val trashed by repo.trashed.collectAsStateWithLifecycle(emptyList())
    val items = listOf(
        MoreItem("Libretas", "${notebooks.size} libretas", Icons.Outlined.FolderCopy) { nav.go("notebooks") },
        MoreItem("Etiquetas", "${tags.size} etiquetas", Icons.AutoMirrored.Outlined.Label) { nav.go("tags") },
        MoreItem("Atajos", "${notes.count { it.note.isShortcut }} notas", Icons.Outlined.StarOutline) { nav.openNotes(null, null, "shortcuts") },
        MoreItem("Recordatorios", "${notes.count { it.note.reminderAt != null && !it.note.reminderDone }} activos", Icons.Outlined.NotificationsNone) { nav.openNotes(null, null, "reminders") },
        MoreItem("Calendario", "por día", Icons.Outlined.CalendarMonth) { nav.go("calendar") },
        MoreItem("Buscar", "en todas las notas", Icons.Outlined.Search) { nav.openSearch(null) },
        MoreItem("Plantillas", "con estructura", Icons.Outlined.Dashboard) { nav.newFromTemplate() },
        MoreItem("Papelera", "${trashed.size} notas", Icons.Outlined.Delete) { nav.go("trash") },
        MoreItem("Ajustes", "tema, seguridad, copias", Icons.Outlined.Settings) { nav.go("settings") },
    )
    LazyColumn(Modifier.fillMaxSize().background(p.canvas), contentPadding = PaddingValues(bottom = 140.dp)) {
        item {
            Column(
                Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(top = 28.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Engraving(Art.LAUREL, Modifier.height(86.dp))
                Spacer(Modifier.height(14.dp))
                Text("Índice", style = T.display, color = p.ink, textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text("TODO LO QUE GUARDA TU LIBRETA", style = T.micro, color = p.inkFaint)
            }
        }
        itemsIndexed(items) { i, it ->
            Row(
                Modifier.fillMaxWidth().clickable(onClick = it.action).padding(horizontal = Dims.gutter).hairlineBottom(p.line).padding(vertical = 17.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("%02d".format(i + 1), style = T.micro, color = p.inkFaint, modifier = Modifier.width(34.dp))
                Text(it.label, style = T.heading.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), color = p.ink, modifier = Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Icon(it.icon, null, tint = p.inkFaint, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(it.sub.uppercase(), style = T.micro, color = p.inkFaint)
                }
            }
        }
        item { Box(Modifier.height(10.dp)) }
    }
}
