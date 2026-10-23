package com.libreta.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.libreta.app.data.NoteItem
import com.libreta.app.ui.components.BareIcon
import com.libreta.app.ui.components.BrutalTextField
import com.libreta.app.ui.components.Doodle
import com.libreta.app.ui.components.EmptyState
import com.libreta.app.ui.components.FilterPill
import com.libreta.app.ui.components.Pressable
import com.libreta.app.ui.components.SectionHeader
import com.libreta.app.ui.components.Sticker
import com.libreta.app.ui.components.TextInputDialog
import com.libreta.app.ui.components.rememberApp
import com.libreta.app.ui.components.settingsState
import com.libreta.app.ui.theme.Dims
import com.libreta.app.ui.theme.LocalPalette
import com.libreta.app.ui.theme.T
import com.libreta.app.ui.theme.crayon
import com.libreta.app.util.Fmt
import kotlinx.coroutines.launch
import java.text.Normalizer

private fun norm(s: String): String =
    Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

data class ParsedQuery(val words: List<String>, val tags: List<String>, val notebooks: List<String>, val has: Set<String>, val created: String?)

fun parseQuery(q: String): ParsedQuery {
    val words = mutableListOf<String>()
    val tags = mutableListOf<String>()
    val nbs = mutableListOf<String>()
    val has = mutableSetOf<String>()
    var created: String? = null
    Regex("\"[^\"]+\"|\\S+").findAll(q).map { it.value }.forEach { tok ->
        val t = tok.trim('"')
        when {
            t.startsWith("tag:") || t.startsWith("etiqueta:") -> tags += norm(t.substringAfter(':'))
            t.startsWith("libreta:") -> nbs += norm(t.substringAfter(':'))
            t.startsWith("tiene:") -> has += norm(t.substringAfter(':'))
            t.startsWith("creada:") -> created = norm(t.substringAfter(':'))
            t.startsWith("#") && t.length > 1 -> tags += norm(t.drop(1))
            t.isNotBlank() -> words += norm(t)
        }
    }
    return ParsedQuery(words, tags, nbs, has, created)
}

fun searchNotes(all: List<NoteItem>, q: String): List<NoteItem> {
    val pq = parseQuery(q)
    val now = System.currentTimeMillis()
    return all.filter { item ->
        val n = item.note
        val hay = norm(n.title + " " + (if (n.isLocked) "" else n.plainText) + " " + item.tags.joinToString(" ") { it.name } + " " + (n.sourceUrl ?: ""))
        pq.words.all { hay.contains(it) } &&
            pq.tags.all { t -> item.tags.any { norm(it.name).contains(t) } } &&
            pq.notebooks.all { b -> item.notebook?.let { norm(it.name).contains(b) } == true } &&
            pq.has.all { h ->
                when (h) {
                    "imagen", "imagenes" -> n.hasImage
                    "audio" -> n.hasAudio
                    "archivo", "archivos", "pdf" -> n.hasFile
                    "tarea", "tareas" -> n.hasTask
                    "casilla", "casillas" -> n.hasChecklist
                    "recordatorio" -> n.reminderAt != null
                    else -> true
                }
            } &&
            when (pq.created) {
                "hoy" -> n.createdAt >= Fmt.startOfDay(now)
                "semana" -> n.createdAt >= now - 7 * 86_400_000L
                "mes" -> n.createdAt >= now - 30 * 86_400_000L
                else -> true
            }
    }.sortedByDescending { item ->
        val t = norm(item.note.title)
        pq.words.count { t.contains(it) } * 10_000_000_000_000L + item.note.updatedAt
    }
}

@Composable
fun SearchScreen(initial: String?, nav: Nav) {
    val repo = rememberApp().repository
    val p = LocalPalette.current
    val notes by repo.noteItems.collectAsStateWithLifecycle(emptyList())
    val tags by repo.tags.collectAsStateWithLifecycle(emptyList())
    val notebooks by repo.notebooks.collectAsStateWithLifecycle(emptyList())
    val saved by repo.savedSearches.collectAsStateWithLifecycle(emptyList())
    val settings by settingsState()
    val scope = rememberCoroutineScope()
    var q by remember { mutableStateOf(initial ?: "") }
    var saving by remember { mutableStateOf(false) }
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (initial == null) runCatching { fr.requestFocus() } }

    fun toggleToken(tok: String) {
        q = if (q.split(' ').contains(tok)) q.split(' ').filter { it != tok }.joinToString(" ") else (q.trim() + " " + tok).trim()
    }
    val results = if (q.isBlank()) emptyList() else searchNotes(notes, q)
    val words = parseQuery(q).words

    Column(Modifier.fillMaxSize().background(p.canvas)) {
        Column(Modifier.fillMaxWidth().background(p.canvas).windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 14.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BareIcon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", nav.back)
                Spacer(Modifier.width(6.dp))
                BrutalTextField(
                    q, { q = it }, placeholder = "Buscar notas, #etiquetas, tiene:imagen…", leading = Icons.Filled.Search,
                    modifier = Modifier.weight(1f).focusRequester(fr),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { if (q.isNotBlank()) scope.launch { repo.prefs.addRecentSearch(q.trim()) } }),
                    trailing = { if (q.isNotEmpty()) Icon(Icons.Filled.Close, "Limpiar", tint = p.inkSoft, modifier = Modifier.size(20.dp).clickable { q = "" }) },
                )
            }
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val chips = listOf("tiene:imagen" to "Imágenes", "tiene:audio" to "Audio", "tiene:archivo" to "Archivos", "tiene:tarea" to "Tareas", "tiene:casilla" to "Casillas", "tiene:recordatorio" to "Recordatorio", "creada:hoy" to "Hoy", "creada:semana" to "Esta semana", "creada:mes" to "Este mes")
                items(chips.size) { i ->
                    val (tok, label) = chips[i]
                    FilterPill(label, q.split(' ').contains(tok), { toggleToken(tok) })
                }
            }
        }
        androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(1.dp).background(p.line))
        if (q.isBlank()) {
            LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 120.dp)) {
                if (settings.recentSearches.isNotEmpty()) {
                    item {
                        SectionHeader("Recientes", action = "Borrar") { scope.launch { repo.prefs.clearRecentSearches() } }
                    }
                    items(settings.recentSearches) { r ->
                        Row(Modifier.fillMaxWidth().clickable { q = r }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.History, null, tint = p.inkSoft, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(r, style = T.body, color = p.ink)
                        }
                    }
                }
                if (saved.isNotEmpty()) {
                    item { SectionHeader("Búsquedas guardadas") }
                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            saved.forEach { s -> Sticker(s.name, p.art, onClick = { q = s.query }, onRemove = { scope.launch { repo.deleteSearch(s.id) } }) }
                        }
                    }
                }
                item { Spacer(Modifier.height(10.dp)); SectionHeader("Etiquetas") }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        tags.forEach { t -> Sticker(t.name, crayon(t.color), onClick = { toggleToken("#" + t.name.replace(' ', '-')) }) }
                    }
                }
                item { Spacer(Modifier.height(10.dp)); SectionHeader("Libretas") }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        notebooks.forEach { nb -> Sticker((nb.emoji ?: "") + " " + nb.name, crayon(nb.color), leading = null, onClick = { toggleToken("libreta:" + nb.name.replace(' ', '-').let { if (nb.name.contains(' ')) "\"${nb.name}\"" else it }) }) }
                    }
                }
                item {
                    Text(
                        "Tip: combiná palabras con filtros, ej. \"reunión #importante tiene:tarea\".",
                        style = T.caption, color = p.inkFaint, modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }
        } else if (results.isEmpty()) {
            EmptyState(Doodle.SEARCH, "Sin resultados", "Probá con otras palabras o quitá algún filtro.")
        } else {
            LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${results.size} resultados", style = T.heading, color = p.ink, modifier = Modifier.weight(1f))
                        Row(Modifier.clickable { saving = true }.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.BookmarkAdd, null, tint = p.ink, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("GUARDAR BÚSQUEDA", style = T.micro, color = p.ink)
                        }
                    }
                }
                items(results, key = { it.note.id }) { item ->
                    Pressable(Modifier.fillMaxWidth(), onClick = { scope.launch { repo.prefs.addRecentSearch(q.trim()) }; nav.openNote(item.note.id) }) {
                        Column(Modifier.padding(12.dp)) {
                            Text(highlight(item.note.title.ifBlank { "Sin título" }, words, p.highlight), style = T.headingSm, color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val snippet = snippet(if (item.note.isLocked) "🔒" else item.note.plainText, words)
                            if (snippet.isNotBlank()) Text(highlight(snippet, words, p.highlight), style = T.bodySm, color = p.inkSoft, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            Text((item.notebook?.name ?: "") + " · " + Fmt.relative(item.note.updatedAt), style = T.micro, color = p.inkFaint, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
    if (saving) TextInputDialog("Guardar búsqueda", q.take(30), "Nombre", onDismiss = { saving = false }) {
        scope.launch { repo.saveSearch(it, q.trim()) }; saving = false
    }
}

private fun snippet(text: String, words: List<String>): String {
    if (words.isEmpty()) return text.take(160)
    val n = norm(text)
    val idx = words.map { n.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: return text.take(160)
    val start = (idx - 50).coerceAtLeast(0)
    return (if (start > 0) "…" else "") + text.substring(start, (start + 180).coerceAtMost(text.length)).replace('\n', ' ')
}

private fun highlight(text: String, words: List<String>, color: Color) = buildAnnotatedString {
    append(text)
    val n = norm(text)
    if (n.length == text.length) {
        words.filter { it.isNotBlank() }.forEach { w ->
            var i = n.indexOf(w)
            while (i >= 0) {
                addStyle(SpanStyle(background = color), i, i + w.length)
                i = n.indexOf(w, i + w.length)
            }
        }
    }
}
