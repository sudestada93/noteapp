package com.libreta.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.libreta.app.ui.components.BrutalShape
import com.libreta.app.ui.components.LibretaTopBar
import com.libreta.app.ui.components.Marquee
import com.libreta.app.ui.components.Pressable
import com.libreta.app.ui.components.rememberApp
import com.libreta.app.ui.theme.LocalPalette
import com.libreta.app.ui.theme.T
import com.libreta.app.ui.theme.crayon

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
        MoreItem("Libretas", "${notebooks.size} libretas", Icons.Filled.FolderCopy) { nav.go("notebooks") },
        MoreItem("Etiquetas", "${tags.size} etiquetas", Icons.AutoMirrored.Filled.Label) { nav.go("tags") },
        MoreItem("Atajos", "${notes.count { it.note.isShortcut }} notas", Icons.Filled.Star) { nav.openNotes(null, null, "shortcuts") },
        MoreItem("Recordatorios", "${notes.count { it.note.reminderAt != null && !it.note.reminderDone }} activos", Icons.Filled.Alarm) { nav.openNotes(null, null, "reminders") },
        MoreItem("Calendario", "notas y tareas por día", Icons.Filled.CalendarMonth) { nav.go("calendar") },
        MoreItem("Buscar", "en todas las notas", Icons.Filled.Search) { nav.openSearch(null) },
        MoreItem("Plantillas", "empezá con estructura", Icons.Filled.Dashboard) { nav.newFromTemplate() },
        MoreItem("Papelera", "${trashed.size} notas", Icons.Filled.Delete) { nav.go("trash") },
        MoreItem("Ajustes", "tema, seguridad, copias", Icons.Filled.Settings) { nav.go("settings") },
    )
    Column(Modifier.fillMaxSize().background(p.canvas)) {
        LibretaTopBar("Más")
        Marquee("ORGANIZÁ TU MUNDO EN LIBRETAS, PILAS Y ETIQUETAS", color = p.primary)
        LazyVerticalGrid(
            GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 20.dp, end = 16.dp, top = 20.dp, bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            itemsIndexed(items) { i, it ->
                Pressable(Modifier.fillMaxWidth(), borderColor = crayon(i), border = 2.5.dp, onClick = it.action) {
                    Column(Modifier.padding(14.dp)) {
                        Box(
                            Modifier.size(40.dp).clip(BrutalShape).background(crayon(i)).border(1.5.dp, p.line, BrutalShape),
                            contentAlignment = Alignment.Center,
                        ) { Icon(it.icon, null, tint = Color(0xFF383838)) }
                        Spacer(Modifier.height(14.dp))
                        Text(it.label.uppercase(), style = T.label, color = p.ink)
                        Text(it.sub, style = T.caption, color = p.inkSoft, maxLines = 1)
                    }
                }
            }
            item(span = { GridItemSpan(2) }) {
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.Center) {
                    Text("LIBRETA · hecho con tinta y papel crema", style = T.micro, color = p.inkFaint)
                }
            }
        }
    }
}
