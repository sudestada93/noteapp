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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.libreta.app.data.NotebookEntity
import com.libreta.app.data.TagEntity
import com.libreta.app.ui.components.BareIcon
import com.libreta.app.ui.components.BrutalButton
import com.libreta.app.ui.components.BrutalDialog
import com.libreta.app.ui.components.BrutalShape
import com.libreta.app.ui.components.BrutalSheet
import com.libreta.app.ui.components.BrutalTextField
import com.libreta.app.ui.components.BtnStyle
import com.libreta.app.ui.components.ColorDots
import com.libreta.app.ui.components.ConfirmDialog
import com.libreta.app.ui.components.Doodle
import com.libreta.app.ui.components.EmptyState
import com.libreta.app.ui.components.LibretaTopBar
import com.libreta.app.ui.components.NotebookSpine
import com.libreta.app.ui.components.Pressable
import com.libreta.app.ui.components.SheetItem
import com.libreta.app.ui.components.Sticker
import com.libreta.app.ui.components.TextInputDialog
import com.libreta.app.ui.components.rememberApp
import com.libreta.app.ui.components.settingsState
import com.libreta.app.ui.theme.Crayons
import com.libreta.app.ui.theme.LocalPalette
import com.libreta.app.ui.theme.T
import com.libreta.app.ui.theme.crayon
import com.libreta.app.util.Fmt
import kotlinx.coroutines.launch

// ===================== LIBRETAS =====================

@Composable
fun NotebooksScreen(nav: Nav) {
    val repo = rememberApp().repository
    val p = LocalPalette.current
    val notebooks by repo.notebooks.collectAsStateWithLifecycle(emptyList())
    val notes by repo.noteItems.collectAsStateWithLifecycle(emptyList())
    val settings by settingsState()
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<NotebookEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<NotebookEntity?>(null) }
    var deleting by remember { mutableStateOf<NotebookEntity?>(null) }

    Column(Modifier.fillMaxSize().background(p.canvas)) {
        LibretaTopBar("Libretas", onBack = nav.back, subtitle = "${notebooks.size} libretas · ${notebooks.mapNotNull { it.stack }.distinct().size} pilas") {
            BareIcon(Icons.Filled.Add, "Nueva", { creating = true })
        }
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val grouped = notebooks.groupBy { it.stack }.toSortedMap(nullsFirst<String>())
            grouped.forEach { (stack, list) ->
                if (stack != null) item(key = "s_$stack") {
                    Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(width = 18.dp, height = 12.dp).background(p.ink))
                        Spacer(Modifier.width(8.dp))
                        Text("PILA · ${stack.uppercase()}", style = T.label, color = p.ink)
                    }
                }
                items(list, key = { it.id }) { nb ->
                    val count = notes.count { it.note.notebookId == nb.id }
                    val last = notes.filter { it.note.notebookId == nb.id }.maxOfOrNull { it.note.updatedAt }
                    Pressable(Modifier.fillMaxWidth().animateItem(), onClick = { nav.openNotes(nb.id, null, null) }, onLongClick = { menuFor = nb }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(width = 16.dp, height = 68.dp).background(crayon(nb.color)))
                            Box(Modifier.size(width = 2.dp, height = 68.dp).background(p.line))
                            Spacer(Modifier.width(12.dp))
                            NotebookSpine(nb, size = 40)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(nb.name, style = T.subheading.copy(fontWeight = FontWeight.Bold), color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (settings.defaultNotebookId == nb.id) {
                                        Spacer(Modifier.width(6.dp))
                                        com.libreta.app.ui.components.Badge("predet.")
                                    }
                                }
                                Text("$count notas" + (last?.let { " · " + Fmt.relative(it) } ?: ""), style = T.caption, color = p.inkSoft)
                            }
                            if (nb.isShortcut) Icon(Icons.Filled.Star, null, tint = p.orange, modifier = Modifier.size(18.dp))
                            BareIcon(Icons.Filled.MoreVert, "Opciones", { menuFor = nb })
                        }
                    }
                }
            }
        }
    }

    menuFor?.let { nb ->
        BrutalSheet({ menuFor = null }, nb.name) {
            SheetItem(Icons.Filled.Edit, "Editar (nombre, color, pila)") { editing = nb; menuFor = null }
            SheetItem(if (nb.isShortcut) Icons.Filled.StarBorder else Icons.Filled.Star, if (nb.isShortcut) "Quitar de atajos" else "Agregar a atajos") {
                scope.launch { repo.saveNotebook(nb.copy(isShortcut = !nb.isShortcut)) }; menuFor = null
            }
            SheetItem(Icons.Filled.Check, "Usar como predeterminada") { scope.launch { repo.prefs.setDefaultNotebook(nb.id) }; menuFor = null }
            SheetItem(Icons.Filled.Add, "Nueva nota aquí") { menuFor = null; nav.newNote(null, nb.id, null) }
            if (notebooks.size > 1) SheetItem(Icons.Filled.Delete, "Eliminar libreta", tint = p.danger) { deleting = nb; menuFor = null }
        }
    }
    if (creating) NotebookEditor(null, notebooks, { creating = false }) { name, color, stack, emoji ->
        scope.launch { repo.createNotebook(name, color, stack, emoji) }; creating = false
    }
    editing?.let { nb ->
        NotebookEditor(nb, notebooks, { editing = null }) { name, color, stack, emoji ->
            scope.launch { repo.saveNotebook(nb.copy(name = name, color = color, stack = stack?.ifBlank { null }, emoji = emoji?.ifBlank { null })) }; editing = null
        }
    }
    deleting?.let { nb ->
        ConfirmDialog("Eliminar libreta", "Las notas de \"${nb.name}\" irán a la papelera.", "Eliminar", { deleting = null }) {
            scope.launch { repo.deleteNotebook(nb.id) }; deleting = null
        }
    }
}

private val Emojis = listOf("📓", "🏠", "💼", "💡", "✈️", "🍳", "📚", "🎵", "💰", "🧪", "🌱", "🎨", "🏋️", "❤️", "🧠", "🛠️")

@Composable
private fun NotebookEditor(nb: NotebookEntity?, all: List<NotebookEntity>, onDismiss: () -> Unit, onSave: (String, Int, String?, String?) -> Unit) {
    val p = LocalPalette.current
    var name by remember { mutableStateOf(nb?.name ?: "") }
    var color by remember { mutableStateOf(nb?.color ?: (0..7).random()) }
    var stack by remember { mutableStateOf(nb?.stack ?: "") }
    var emoji by remember { mutableStateOf(nb?.emoji ?: "📓") }
    BrutalDialog(if (nb == null) "Nueva libreta" else "Editar libreta", onDismiss, "Guardar", { if (name.isNotBlank()) onSave(name.trim(), color, stack.trim(), emoji) }, confirmEnabled = name.isNotBlank()) {
        BrutalTextField(name, { name = it }, placeholder = "Nombre", modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp))
        Text("COLOR", style = T.micro, color = p.inkSoft)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth()) {
            androidx.compose.foundation.lazy.LazyRow { item { ColorDots(color, { color = it }, Crayons) } }
        }
        Spacer(Modifier.height(14.dp))
        Text("ÍCONO", style = T.micro, color = p.inkSoft)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Emojis.forEach { e ->
                Box(
                    Modifier.size(36.dp).clip(BrutalShape).background(if (e == emoji) p.canary else p.surface)
                        .border(if (e == emoji) 2.5.dp else 1.dp, p.line, BrutalShape).clickable { emoji = e },
                    contentAlignment = Alignment.Center,
                ) { Text(e) }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("PILA (opcional)", style = T.micro, color = p.inkSoft)
        Spacer(Modifier.height(8.dp))
        BrutalTextField(stack, { stack = it }, placeholder = "Ej.: Proyectos", modifier = Modifier.fillMaxWidth())
        val stacks = all.mapNotNull { it.stack }.distinct()
        if (stacks.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                stacks.forEach { s -> Sticker(s, p.ink, leading = null, selected = s == stack, onClick = { stack = if (stack == s) "" else s }) }
            }
        }
    }
}

// ===================== ETIQUETAS =====================

@Composable
fun TagsScreen(nav: Nav) {
    val repo = rememberApp().repository
    val p = LocalPalette.current
    val tags by repo.tags.collectAsStateWithLifecycle(emptyList())
    val notes by repo.noteItems.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    var creating by remember { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<TagEntity?>(null) }
    var renaming by remember { mutableStateOf<TagEntity?>(null) }
    var deleting by remember { mutableStateOf<TagEntity?>(null) }

    Column(Modifier.fillMaxSize().background(p.canvas)) {
        LibretaTopBar("Etiquetas", onBack = nav.back, subtitle = "${tags.size} etiquetas") {
            BareIcon(Icons.Filled.Add, "Nueva", { creating = true })
        }
        if (tags.isEmpty()) {
            EmptyState(Doodle.TAGS, "Sin etiquetas", "Las etiquetas cruzan libretas: \"urgente\", \"ideas\", \"2026\"…") {
                BrutalButton("Crear etiqueta", { creating = true }, icon = Icons.Filled.Add)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 120.dp)) {
                val byLetter = tags.groupBy { it.name.firstOrNull()?.uppercaseChar() ?: '#' }
                byLetter.forEach { (letter, list) ->
                    item(key = "l_$letter") {
                        Text(letter.toString(), style = T.heading, color = p.inkFaint, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                    }
                    item(key = "g_$letter") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            list.forEachIndexed { i, t ->
                                val count = notes.count { n -> n.tags.any { it.id == t.id } }
                                Pressable(
                                    color = p.surface, shadow = 3.dp,
                                    onClick = { nav.openNotes(null, t.id, null) }, onLongClick = { menuFor = t },
                                    modifier = Modifier.padding(bottom = 2.dp),
                                ) {
                                    Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(12.dp).background(crayon(t.color)).border(1.dp, p.line))
                                        Spacer(Modifier.width(8.dp))
                                        Text(t.name, style = T.body.copy(fontWeight = FontWeight.SemiBold), color = p.ink)
                                        Spacer(Modifier.width(8.dp))
                                        Text("$count", style = T.micro, color = Color(0xFF383838), modifier = Modifier.background(crayon(t.color)).padding(horizontal = 5.dp, vertical = 2.dp))
                                        if (t.isShortcut) { Spacer(Modifier.width(6.dp)); Icon(Icons.Filled.Star, null, tint = p.orange, modifier = Modifier.size(14.dp)) }
                                    }
                                }
                            }
                        }
                    }
                }
                item { Text("Mantené presionada una etiqueta para editarla.", style = T.caption, color = p.inkFaint, modifier = Modifier.padding(top = 20.dp)) }
            }
        }
    }
    menuFor?.let { t ->
        BrutalSheet({ menuFor = null }, "# " + t.name) {
            SheetItem(Icons.Filled.Edit, "Renombrar") { renaming = t; menuFor = null }
            Text("COLOR", style = T.micro, color = p.inkSoft, modifier = Modifier.padding(vertical = 8.dp))
            androidx.compose.foundation.lazy.LazyRow { item { ColorDots(t.color, { c -> scope.launch { repo.saveTag(t.copy(color = c)) }; menuFor = null }, Crayons) } }
            Spacer(Modifier.height(8.dp))
            SheetItem(if (t.isShortcut) Icons.Filled.StarBorder else Icons.Filled.Star, if (t.isShortcut) "Quitar de atajos" else "Agregar a atajos") {
                scope.launch { repo.saveTag(t.copy(isShortcut = !t.isShortcut)) }; menuFor = null
            }
            SheetItem(Icons.Filled.Delete, "Eliminar etiqueta", tint = p.danger) { deleting = t; menuFor = null }
        }
    }
    if (creating) TextInputDialog("Nueva etiqueta", placeholder = "nombre", confirmText = "Crear", onDismiss = { creating = false }) {
        scope.launch { repo.createTag(it) }; creating = false
    }
    renaming?.let { t ->
        TextInputDialog("Renombrar etiqueta", t.name, onDismiss = { renaming = null }) { scope.launch { repo.saveTag(t.copy(name = it)) }; renaming = null }
    }
    deleting?.let { t ->
        ConfirmDialog("Eliminar etiqueta", "Se quitará \"${t.name}\" de todas las notas.", "Eliminar", { deleting = null }) {
            scope.launch { repo.deleteTag(t.id) }; deleting = null
        }
    }
}

// ===================== PAPELERA =====================

@Composable
fun TrashScreen(nav: Nav) {
    val repo = rememberApp().repository
    val p = LocalPalette.current
    val trashed by repo.trashed.collectAsStateWithLifecycle(emptyList())
    val settings by settingsState()
    val scope = rememberCoroutineScope()
    var confirmEmpty by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().background(p.canvas)) {
        LibretaTopBar("Papelera", onBack = nav.back, subtitle = "${trashed.size} notas") {
            if (trashed.isNotEmpty()) BareIcon(Icons.Filled.Delete, "Vaciar", { confirmEmpty = true }, tint = p.danger)
        }
        if (settings.trashAutoDays > 0) {
            Text(
                "Las notas se eliminan definitivamente tras ${settings.trashAutoDays} días.", style = T.caption, color = Color(0xFF383838),
                modifier = Modifier.fillMaxWidth().background(p.canary).padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        if (trashed.isEmpty()) {
            EmptyState(Doodle.TRASH, "Papelera vacía", "Las notas eliminadas esperan acá antes de desaparecer.")
        } else {
            LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(trashed, key = { it.id }) { n ->
                    Pressable(Modifier.fillMaxWidth().animateItem(), color = p.surfaceAlt, shadow = 3.dp) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(n.title.ifBlank { "Sin título" }, style = T.body.copy(fontWeight = FontWeight.SemiBold), color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(n.preview, style = T.caption, color = p.inkSoft, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("Eliminada " + Fmt.relative(n.trashedAt ?: n.updatedAt), style = T.micro, color = p.inkFaint, modifier = Modifier.padding(top = 4.dp))
                            }
                            BareIcon(Icons.Filled.Restore, "Restaurar", { scope.launch { repo.restore(n.id) } })
                            BareIcon(Icons.Filled.Delete, "Eliminar", { deleting = n.id }, tint = p.danger)
                        }
                    }
                }
            }
        }
    }
    if (confirmEmpty) ConfirmDialog("Vaciar papelera", "Se eliminarán ${trashed.size} notas y sus adjuntos para siempre.", "Vaciar", { confirmEmpty = false }) {
        scope.launch { repo.emptyTrash() }; confirmEmpty = false
    }
    deleting?.let { id ->
        ConfirmDialog("Eliminar para siempre", "Esta acción no se puede deshacer.", "Eliminar", { deleting = null }) {
            scope.launch { repo.deleteForever(id) }; deleting = null
        }
    }
}

