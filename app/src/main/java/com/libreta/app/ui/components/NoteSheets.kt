package com.libreta.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.libreta.app.data.NoteItem
import com.libreta.app.data.NotebookEntity
import com.libreta.app.data.TagEntity
import com.libreta.app.ui.theme.LocalPalette
import com.libreta.app.ui.theme.T
import com.libreta.app.ui.theme.crayon
import com.libreta.app.util.Fmt

@Composable
fun NotebookSpine(nb: NotebookEntity?, modifier: Modifier = Modifier, size: Int = 34) {
    val p = LocalPalette.current
    Box(
        modifier.size(size.dp).clip(BrutalShape).background(crayon(nb?.color ?: 0)).border(1.5.dp, p.line, BrutalShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(nb?.emoji ?: (nb?.name?.take(1)?.uppercase() ?: "?"), style = T.label, color = androidx.compose.ui.graphics.Color(0xFF383838))
    }
}

@Composable
fun NotebookPickerSheet(
    notebooks: List<NotebookEntity>,
    selectedId: String?,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
    onPick: (NotebookEntity) -> Unit,
) {
    val p = LocalPalette.current
    var creating by remember { mutableStateOf(false) }
    BrutalSheet(onDismiss, "Mover a libreta") {
        notebooks.groupBy { it.stack }.toSortedMap(nullsFirst<String>()).forEach { (stack, list) ->
            if (stack != null) Text("▤ ${stack.uppercase()}", style = T.micro, color = p.inkSoft, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
            list.forEach { nb ->
                Row(
                    Modifier.fillMaxWidth().clip(BrutalShape).clickable { onPick(nb) }
                        .background(if (nb.id == selectedId) p.iceWash else androidx.compose.ui.graphics.Color.Transparent)
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NotebookSpine(nb)
                    Spacer(Modifier.width(12.dp))
                    Text(nb.name, style = T.body, color = p.ink, modifier = Modifier.weight(1f))
                    if (nb.id == selectedId) Icon(Icons.Filled.Check, null, tint = p.ink)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        BrutalButton("Nueva libreta", { creating = true }, icon = Icons.Filled.Add, style = BtnStyle.Secondary)
    }
    if (creating) TextInputDialog("Nueva libreta", placeholder = "Nombre", confirmText = "Crear", onDismiss = { creating = false }) {
        creating = false; onCreate(it)
    }
}

@Composable
fun TagPickerSheet(
    tags: List<TagEntity>,
    selected: Set<String>,
    onDismiss: () -> Unit,
    onToggle: (TagEntity) -> Unit,
    onCreate: (String) -> Unit,
) {
    val p = LocalPalette.current
    var q by remember { mutableStateOf("") }
    BrutalSheet(onDismiss, "Etiquetas") {
        BrutalTextField(
            q, { q = it }, placeholder = "Buscar o crear etiqueta", leading = Icons.Filled.Search, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (q.isNotBlank()) { onCreate(q.trim()); q = "" } }),
        )
        Spacer(Modifier.height(14.dp))
        val filtered = tags.filter { q.isBlank() || it.name.contains(q, true) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            filtered.forEachIndexed { i, t ->
                Sticker(t.name, crayon(t.color), selected = t.id in selected, rotation = if (i % 3 == 0) -1.5f else if (i % 3 == 1) 1f else 0f, onClick = { onToggle(t) })
            }
        }
        if (q.isNotBlank() && tags.none { it.name.equals(q.trim(), true) }) {
            Spacer(Modifier.height(14.dp))
            BrutalButton("Crear \"${q.trim()}\"", { onCreate(q.trim()); q = "" }, icon = Icons.Filled.Add, small = true)
        }
        if (tags.isEmpty() && q.isBlank()) Text("Todavía no hay etiquetas. Escribí una arriba.", style = T.bodySm, color = p.inkSoft)
    }
}

@Composable
fun NotePickerSheet(notes: List<NoteItem>, excludeId: String?, onDismiss: () -> Unit, onPick: (NoteItem) -> Unit) {
    val p = LocalPalette.current
    var q by remember { mutableStateOf("") }
    BrutalSheet(onDismiss, "Enlazar una nota") {
        BrutalTextField(q, { q = it }, placeholder = "Buscar nota", leading = Icons.Filled.Search, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        notes.filter { it.note.id != excludeId && (q.isBlank() || it.note.title.contains(q, true) || it.note.plainText.contains(q, true)) }
            .take(40).forEach { n ->
                Row(
                    Modifier.fillMaxWidth().clip(BrutalShape).clickable { onPick(n) }.padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NotebookSpine(n.notebook, size = 26)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(n.note.title.ifBlank { "Sin título" }, style = T.body.copy(fontWeight = FontWeight.SemiBold), color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(Fmt.relative(n.note.updatedAt), style = T.caption, color = p.inkSoft)
                    }
                }
            }
    }
}
