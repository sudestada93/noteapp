package com.libreta.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.libreta.app.data.NoteItem
import com.libreta.app.ui.components.BareIcon
import com.libreta.app.ui.components.BrutalButton
import com.libreta.app.ui.components.BrutalSheet
import com.libreta.app.ui.components.ConfirmDialog
import com.libreta.app.ui.components.Doodle
import com.libreta.app.ui.components.EmptyState
import com.libreta.app.ui.components.FilterPill
import com.libreta.app.ui.components.LibretaTopBar
import com.libreta.app.ui.components.NoteCard
import com.libreta.app.ui.components.NotebookPickerSheet
import com.libreta.app.ui.components.SheetItem
import com.libreta.app.ui.components.TagPickerSheet
import com.libreta.app.ui.components.rememberApp
import com.libreta.app.ui.components.settingsState
import com.libreta.app.ui.theme.LocalPalette
import com.libreta.app.ui.theme.T
import kotlinx.coroutines.launch

val NoteFilters = listOf(
    "all" to "Todas", "shortcuts" to "Atajos", "reminders" to "Recordatorios", "tasks" to "Con tareas",
    "images" to "Imágenes", "audio" to "Audio", "files" to "Archivos", "untagged" to "Sin etiquetas", "locked" to "Bloqueadas",
)

fun applyFilter(list: List<NoteItem>, filter: String?): List<NoteItem> = when (filter) {
    "shortcuts" -> list.filter { it.note.isShortcut }
    "reminders" -> list.filter { it.note.reminderAt != null }.sortedWith(compareBy({ it.note.reminderDone }, { it.note.reminderAt }))
    "tasks" -> list.filter { it.note.hasTask || it.note.hasChecklist }
    "images" -> list.filter { it.note.hasImage }
    "audio" -> list.filter { it.note.hasAudio }
    "files" -> list.filter { it.note.hasFile }
    "untagged" -> list.filter { it.tags.isEmpty() }
    "locked" -> list.filter { it.note.isLocked }
    else -> list
}

@Composable
fun NotesScreen(nbId: String?, tagId: String?, filterArg: String?, nav: Nav, showBack: Boolean) {
    val app = rememberApp()
    val repo = app.repository
    val p = LocalPalette.current
    val settings by settingsState()
    val all by repo.noteItems.collectAsStateWithLifecycle(emptyList())
    val notebooks by repo.notebooks.collectAsStateWithLifecycle(emptyList())
    val tags by repo.tags.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    var filter by remember { mutableStateOf(filterArg ?: "all") }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var sheet by remember { mutableStateOf<String?>(null) }
    var confirmTrash by remember { mutableStateOf(false) }

    val nb = notebooks.firstOrNull { it.id == nbId }
    val tag = tags.firstOrNull { it.id == tagId }
    val base = all.filter { (nbId == null || it.note.notebookId == nbId) && (tagId == null || it.tags.any { t -> t.id == tagId }) }
    val sorted = when (settings.sort) {
        1 -> base.sortedByDescending { it.note.createdAt }
        2 -> base.sortedBy { it.note.title.lowercase() }
        else -> base.sortedByDescending { it.note.updatedAt }
    }
    val pinnedFirst = sorted.sortedByDescending { it.note.isPinned }
    val list = applyFilter(pinnedFirst, filter)
    val title = nb?.let { (it.emoji?.plus(" ") ?: "") + it.name } ?: tag?.let { "# " + it.name } ?: NoteFilters.firstOrNull { it.first == filter && filter != "all" }?.second ?: "Todas las notas"
    val selecting = selected.isNotEmpty()
    val view = settings.listView

    Column(Modifier.fillMaxSize().background(p.canvas)) {
        if (selecting) {
            LibretaTopBar("${selected.size} seleccionadas", onBack = { selected = emptySet() }) {
                BareIcon(Icons.Filled.SelectAll, "Todas", { selected = list.map { it.note.id }.toSet() })
                BareIcon(Icons.Filled.Star, "Atajo", {
                    scope.launch { selected.forEach { id -> repo.updateNote(id) { it.copy(isShortcut = true) } }; selected = emptySet() }
                })
                BareIcon(Icons.Filled.FolderOpen, "Mover", { sheet = "move" })
                BareIcon(Icons.AutoMirrored.Filled.Label, "Etiquetar", { sheet = "tag" })
                BareIcon(Icons.Filled.Delete, "Papelera", { confirmTrash = true }, tint = p.danger)
            }
        } else {
            LibretaTopBar(title, onBack = if (showBack) nav.back else null, subtitle = "${list.size} notas") {
                BareIcon(Icons.Filled.Search, "Buscar", { nav.openSearch(null) })
                BareIcon(Icons.AutoMirrored.Filled.Sort, "Ordenar", { sheet = "sort" })
                BareIcon(
                    when (view) { 0 -> Icons.Filled.ViewHeadline; 1 -> Icons.Filled.ViewAgenda; 2 -> Icons.Filled.GridView; else -> Icons.Filled.ViewStream },
                    "Vista", { scope.launch { repo.prefs.setListView((view + 1) % 4) } },
                )
            }
        }
        if (nbId == null && tagId == null) {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(NoteFilters.size) { i ->
                    val (k, l) = NoteFilters[i]
                    FilterPill(l, filter == k, { filter = k })
                }
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(nb?.stack?.let { "PILA: ${it.uppercase()}" } ?: "", style = T.micro, color = p.inkSoft, modifier = Modifier.weight(1f))
                BrutalButton("Nueva nota", { nav.newNote(null, nbId, tagId) }, icon = Icons.AutoMirrored.Filled.NoteAdd, small = true)
            }
        }
        if (list.isEmpty()) {
            EmptyState(
                if (filter == "reminders") Doodle.BELL else if (filter == "shortcuts") Doodle.STAR else Doodle.NOTES,
                "Nada por acá", "Creá una nota con el botón + o probá otro filtro.",
            ) { BrutalButton("Nueva nota", { nav.newNote(null, nbId, tagId) }, icon = Icons.AutoMirrored.Filled.NoteAdd) }
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(if (view == 2) 2 else 1),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalItemSpacing = if (view == 3) 8.dp else 14.dp,
            ) {
                items(list, key = { it.note.id }) { item ->
                    NoteCard(
                        item, view, Modifier.animateItem(), selected = item.note.id in selected,
                        onClick = {
                            if (selecting) selected = if (item.note.id in selected) selected - item.note.id else selected + item.note.id
                            else nav.openNote(item.note.id)
                        },
                        onLongClick = { selected = selected + item.note.id },
                    )
                }
                item(span = StaggeredGridItemSpan.FullLine) { Spacer(Modifier.height(8.dp)) }
            }
        }
    }

    when (sheet) {
        "sort" -> BrutalSheet({ sheet = null }, "Ordenar y ver") {
            listOf("Última modificación", "Fecha de creación", "Título (A–Z)").forEachIndexed { i, l ->
                SheetItem(Icons.AutoMirrored.Filled.Sort, l, trailing = { if (settings.sort == i) Text("●", color = p.primary, style = T.label) }) {
                    scope.launch { repo.prefs.setSort(i) }; sheet = null
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("VISTA", style = T.micro, color = p.inkSoft)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Lista", "Tarjetas", "Grilla", "Compacta").forEachIndexed { i, l ->
                    FilterPill(l, view == i, { scope.launch { repo.prefs.setListView(i) } })
                }
            }
        }
        "move" -> NotebookPickerSheet(notebooks, null, { sheet = null }, onCreate = { name ->
            scope.launch { val id = repo.createNotebook(name, (0..7).random(), null, null); repo.moveNotes(selected, id); selected = emptySet() }
            sheet = null
        }) { target -> scope.launch { repo.moveNotes(selected, target.id); selected = emptySet() }; sheet = null }
        "tag" -> TagPickerSheet(tags, emptySet(), { sheet = null; selected = emptySet() }, onToggle = { t ->
            scope.launch { selected.forEach { repo.addTag(it, t.id) } }
        }, onCreate = { name -> scope.launch { val t = repo.createTag(name); selected.forEach { repo.addTag(it, t.id) } } })
    }
    if (confirmTrash) ConfirmDialog("Mover a la papelera", "¿Mover ${selected.size} notas a la papelera?", "Mover", { confirmTrash = false }) {
        confirmTrash = false
        scope.launch { selected.forEach { repo.trash(it) }; selected = emptySet() }
    }
}
