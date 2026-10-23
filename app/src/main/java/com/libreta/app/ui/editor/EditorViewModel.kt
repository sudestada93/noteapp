package com.libreta.app.ui.editor

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.libreta.app.LibretaApp
import com.libreta.app.data.NoteItem
import com.libreta.app.model.Block
import com.libreta.app.model.BlockType
import com.libreta.app.model.Inline
import com.libreta.app.model.newId
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

const val ZW = "​"

class EditorBlock(b: Block) {
    val id: String = b.id
    var type by mutableStateOf(b.type)
    var field by mutableStateOf(TextFieldValue(ZW + b.text, TextRange(1)))
    var checked by mutableStateOf(b.checked)
    var indent by mutableIntStateOf(b.indent)
    var dueAt by mutableStateOf(b.dueAt)
    var reminderAt by mutableStateOf(b.reminderAt)
    var flagged by mutableStateOf(b.flagged)
    var color by mutableIntStateOf(b.color)
    var path by mutableStateOf(b.path)
    var name by mutableStateOf(b.name)
    val mime = b.mime
    val size = b.size
    val durationMs = b.durationMs
    val waveform = b.waveform
    val sketch = b.sketch
    var linkId by mutableStateOf(b.linkId)
    val rows: SnapshotStateList<SnapshotStateList<String>> =
        mutableStateListOf<SnapshotStateList<String>>().apply { b.rows.forEach { add(mutableStateListOf<String>().apply { addAll(it) }) } }
    val focusRequester = FocusRequester()

    val text: String get() = this.field.text.replace(ZW, "")

    fun setText(t: String, cursor: Int = t.length) {
        field = TextFieldValue(ZW + t, TextRange(cursor + 1))
    }

    fun toBlock(): Block = Block(
        id = id, type = type, text = text, checked = checked, indent = indent, path = path, name = name,
        mime = mime, size = size, durationMs = durationMs, waveform = waveform,
        rows = rows.map { it.toList() }, linkId = linkId, color = color, dueAt = dueAt,
        reminderAt = reminderAt, flagged = flagged, sketch = sketch,
    )
}

private data class Snap(val title: String, val blocks: List<Block>)

data class SlashCommand(val key: String, val label: String, val hint: String)

val SlashCommands = listOf(
    SlashCommand("text", "Texto", "párrafo normal"),
    SlashCommand("h1", "Título 1", "encabezado grande"),
    SlashCommand("h2", "Título 2", "encabezado mediano"),
    SlashCommand("h3", "Título 3", "encabezado chico"),
    SlashCommand("bullet", "Viñetas", "lista con puntos"),
    SlashCommand("numbered", "Numerada", "lista 1. 2. 3."),
    SlashCommand("check", "Casillas", "lista de verificación"),
    SlashCommand("task", "Tarea", "con fecha, bandera y recordatorio"),
    SlashCommand("quote", "Cita", "texto citado"),
    SlashCommand("code", "Código", "bloque monoespaciado"),
    SlashCommand("callout", "Destacado", "caja de color"),
    SlashCommand("divider", "Separador", "línea divisoria"),
    SlashCommand("table", "Tabla", "filas y columnas"),
    SlashCommand("image", "Imagen", "desde la galería"),
    SlashCommand("camera", "Cámara", "sacar una foto"),
    SlashCommand("scan", "Escanear", "documento con recorte"),
    SlashCommand("sketch", "Boceto", "dibujar a mano"),
    SlashCommand("audio", "Audio", "grabar nota de voz"),
    SlashCommand("file", "Archivo", "adjuntar PDF u otro"),
    SlashCommand("link", "Enlace a nota", "vincular otra nota"),
    SlashCommand("date", "Fecha de hoy", "insertar fecha"),
)

class EditorViewModel(app: Application, saved: SavedStateHandle) : AndroidViewModel(app) {
    private val libreta = app as LibretaApp
    val repo = libreta.repository
    val noteId: String = checkNotNull(saved["id"])

    var loaded by mutableStateOf(false)
        private set
    var title by mutableStateOf(TextFieldValue(""))
        private set
    val blocks = mutableStateListOf<EditorBlock>()
    var focusedId by mutableStateOf<String?>(null)
    var focusSignal by mutableStateOf<Pair<String, Int>?>(null)
        private set
    private var focusCounter = 0
    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set
    var find by mutableStateOf("")
    var findOpen by mutableStateOf(false)
    var lastSavedAt by mutableStateOf(0L)
        private set

    val item: StateFlow<NoteItem?> = repo.noteItems.map { list -> list.firstOrNull { it.note.id == noteId } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val undo = ArrayDeque<Snap>()
    private val redo = ArrayDeque<Snap>()
    private var stable: Snap? = null
    private var inBurst = false
    private var burstJob: Job? = null

    init {
        viewModelScope.launch {
            val n = repo.getNote(noteId)
            if (n != null) {
                title = TextFieldValue(n.title, TextRange(n.title.length))
                val list = com.libreta.app.model.BlockCodec.decode(n.content).ifEmpty { listOf(Block()) }
                blocks.addAll(list.map(::EditorBlock))
                ensureTrailingText()
            }
            stable = snapshot()
            loaded = true
        }
    }

    val focused: EditorBlock? get() = blocks.firstOrNull { it.id == focusedId }

    private fun snapshot() = Snap(title.text, blocks.map { it.toBlock() })

    private fun restore(s: Snap) {
        title = TextFieldValue(s.title, TextRange(s.title.length))
        blocks.clear()
        blocks.addAll(s.blocks.map(::EditorBlock))
        ensureTrailingText()
    }

    /** Llamar en cada modificación: agrupa ediciones para deshacer y guarda con retardo. */
    fun changed() {
        if (!inBurst) {
            stable?.let { undo.addLast(it); if (undo.size > 100) undo.removeFirst() }
            redo.clear()
            inBurst = true
            canUndo = true
            canRedo = false
        }
        burstJob?.cancel()
        burstJob = viewModelScope.launch {
            delay(900)
            endBurst()
            save()
        }
    }

    private fun endBurst() {
        inBurst = false
        stable = snapshot()
    }

    fun undo() {
        if (inBurst) { burstJob?.cancel(); endBurst() }
        val prev = undo.removeLastOrNull() ?: return
        redo.addLast(snapshot())
        restore(prev)
        stable = prev
        canUndo = undo.isNotEmpty(); canRedo = true
        save()
    }

    fun redo() {
        val next = redo.removeLastOrNull() ?: return
        undo.addLast(snapshot())
        restore(next)
        stable = next
        canUndo = true; canRedo = redo.isNotEmpty()
        save()
    }

    fun save() {
        if (!loaded) return
        val t = title.text
        val bl = blocks.map { it.toBlock() }
        libreta.appScope.launch {
            repo.saveContent(noteId, t, bl)
            lastSavedAt = System.currentTimeMillis()
        }
    }

    fun isEmpty(): Boolean = title.text.isBlank() && blocks.all { it.type.isText && it.text.isBlank() }

    /** Al salir: guarda o descarta la nota si quedó vacía. */
    private var closed = false

    fun close() {
        if (!loaded || closed) return
        closed = true
        burstJob?.cancel()
        if (isEmpty()) {
            libreta.appScope.launch { repo.deleteForever(noteId) }
        } else save()
    }

    fun updateTitle(v: TextFieldValue) {
        val clean = v.text.replace("\n", "")
        if (clean != v.text) {
            title = v.copy(text = clean)
            blocks.firstOrNull { it.type.isText }?.let { focus(it, 0) }
        } else title = v
        changed()
    }

    // ---------- Foco ----------
    fun focus(b: EditorBlock, cursor: Int? = null) {
        if (cursor != null) {
            val c = (cursor + 1).coerceIn(1, b.field.text.length)
            b.field = b.field.copy(selection = TextRange(c))
        }
        focusedId = b.id
        focusSignal = b.id to ++focusCounter
    }

    private fun ensureTrailingText() {
        val last = blocks.lastOrNull()
        if (last == null || !last.type.isText) blocks.add(EditorBlock(Block()))
    }

    // ---------- Edición de texto ----------
    fun onTextChange(b: EditorBlock, nv: TextFieldValue) {
        val old = b.field
        var v = nv
        if (!v.text.startsWith(ZW)) {
            if (old.text.startsWith(ZW) && v.text == old.text.substring(1)) {
                backspaceAtStart(b)
                return
            }
            val stripped = v.text.replace(ZW, "")
            val shift = 1 - (v.text.length - stripped.length)
            v = TextFieldValue(
                ZW + stripped,
                TextRange((v.selection.start + shift).coerceIn(1, stripped.length + 1), (v.selection.end + shift).coerceIn(1, stripped.length + 1)),
                v.composition?.let { TextRange((it.start + shift).coerceAtLeast(1), (it.end + shift).coerceAtLeast(1)) },
            )
        }
        if (v.selection.min < 1) {
            v = v.copy(selection = TextRange(v.selection.start.coerceAtLeast(1), v.selection.end.coerceAtLeast(1)))
        }
        if (b.type != BlockType.CODE && v.text.contains('\n')) {
            splitOnNewlines(b, v)
            return
        }
        val textChanged = v.text != old.text
        b.field = v
        if (textChanged) {
            if (b.type == BlockType.PARAGRAPH) applyShortcut(b)
            changed()
        }
    }

    private fun continuationType(t: BlockType): BlockType = when (t) {
        BlockType.BULLET, BlockType.NUMBERED, BlockType.CHECK, BlockType.TASK -> t
        else -> BlockType.PARAGRAPH
    }

    private fun splitOnNewlines(b: EditorBlock, v: TextFieldValue) {
        val raw = v.text.replace(ZW, "")
        val cursorRaw = (v.selection.end - 1).coerceIn(0, raw.length)
        val parts = raw.split('\n')
        val idx = blocks.indexOf(b)
        if (idx < 0) return
        // Enter en un ítem de lista vacío: salir de la lista
        if (parts.size == 2 && parts[0].isEmpty() && parts[1].isEmpty() && b.type != BlockType.PARAGRAPH) {
            if (b.indent > 0) b.indent-- else b.type = BlockType.PARAGRAPH
            b.setText("")
            changed()
            return
        }
        // Enter al inicio de un bloque con contenido: insertar línea vacía arriba
        if (parts.size == 2 && parts[0].isEmpty() && cursorRaw <= 1 && parts[1].isNotEmpty()) {
            val above = EditorBlock(Block(type = if (b.type.isListLike) b.type else BlockType.PARAGRAPH, indent = b.indent))
            blocks.add(idx, above)
            b.setText(parts[1], 0)
            focus(b, 0)
            changed()
            return
        }
        b.setText(parts[0])
        var focusTarget: EditorBlock = b
        var focusCursor = parts[0].length
        var consumed = parts[0].length + 1
        val ct = continuationType(b.type)
        for (i in 1 until parts.size) {
            val nb = EditorBlock(Block(type = ct, text = parts[i], indent = b.indent, color = b.color))
            blocks.add(idx + i, nb)
            if (cursorRaw >= consumed && cursorRaw <= consumed + parts[i].length) {
                focusTarget = nb
                focusCursor = cursorRaw - consumed
            }
            consumed += parts[i].length + 1
        }
        focus(focusTarget, focusCursor)
        changed()
    }

    private fun backspaceAtStart(b: EditorBlock) {
        val idx = blocks.indexOf(b)
        if (b.type != BlockType.PARAGRAPH) {
            if (b.indent > 0) b.indent-- else b.type = BlockType.PARAGRAPH
            b.field = b.field.copy(selection = TextRange(1))
            changed()
            return
        }
        if (b.indent > 0) { b.indent--; changed(); return }
        if (idx <= 0) return
        val prev = blocks[idx - 1]
        when {
            prev.type.isText -> {
                val len = prev.text.length
                prev.setText(prev.text + b.text, len)
                blocks.removeAt(idx)
                focus(prev, len)
            }
            prev.type == BlockType.DIVIDER -> blocks.removeAt(idx - 1)
            b.text.isEmpty() -> {
                blocks.removeAt(idx)
                val target = blocks.subList(0, idx).lastOrNull { it.type.isText }
                if (target != null) focus(target, target.text.length)
                ensureTrailingText()
            }
            else -> return
        }
        changed()
    }

    private fun applyShortcut(b: EditorBlock) {
        val t = b.text
        val rules = listOf(
            "# " to BlockType.H1, "## " to BlockType.H2, "### " to BlockType.H3,
            "- " to BlockType.BULLET, "* " to BlockType.BULLET, "• " to BlockType.BULLET,
            "1. " to BlockType.NUMBERED, "[] " to BlockType.CHECK, "[ ] " to BlockType.CHECK,
            "[x] " to BlockType.CHECK, "[!] " to BlockType.TASK, "> " to BlockType.QUOTE,
            "```" to BlockType.CODE, "!! " to BlockType.CALLOUT,
        )
        if (t == "---") {
            b.type = BlockType.DIVIDER
            b.setText("")
            val idx = blocks.indexOf(b)
            val nb = EditorBlock(Block())
            blocks.add(idx + 1, nb)
            focus(nb, 0)
            return
        }
        for ((prefix, type) in rules) {
            if (t.startsWith(prefix)) {
                b.type = type
                if (prefix == "[x] ") b.checked = true
                b.setText(t.removePrefix(prefix), 0)
                return
            }
        }
    }

    // ---------- Operaciones de bloque ----------
    fun toggleCheck(b: EditorBlock) {
        b.checked = !b.checked
        changed()
    }

    fun convert(b: EditorBlock, type: BlockType) {
        if (!b.type.isText) return
        b.type = if (b.type == type && type != BlockType.PARAGRAPH) BlockType.PARAGRAPH else type
        if (b.text.startsWith("/")) b.setText("")
        changed()
        focus(b)
    }

    fun convertFocused(type: BlockType) {
        val b = focused ?: blocks.lastOrNull { it.type.isText } ?: return
        convert(b, type)
    }

    /** Inserta bloques después del bloque con foco (o al final). */
    fun insertBlocks(newBlocks: List<Block>, focusAfter: Boolean = true) {
        if (newBlocks.isEmpty()) return
        val anchor = focused
        var idx = if (anchor != null) blocks.indexOf(anchor) + 1 else blocks.size
        // si el ancla es un párrafo vacío (o un comando "/"), reemplazarlo
        if (anchor != null && anchor.type == BlockType.PARAGRAPH && (anchor.text.isBlank() || anchor.text.startsWith("/"))) {
            idx = blocks.indexOf(anchor)
            blocks.remove(anchor)
        }
        val created = newBlocks.map(::EditorBlock)
        blocks.addAll(idx.coerceIn(0, blocks.size), created)
        val after = idx + created.size
        val next = blocks.getOrNull(after)
        val target = if (next != null && next.type.isText) next else EditorBlock(Block()).also { blocks.add(after, it) }
        if (focusAfter) {
            val lastText = created.lastOrNull { it.type.isText }
            if (lastText != null) focus(lastText, lastText.text.length) else focus(target, 0)
        }
        changed()
    }

    fun deleteBlock(b: EditorBlock) {
        val idx = blocks.indexOf(b)
        if (idx < 0) return
        blocks.removeAt(idx)
        if (blocks.isEmpty()) blocks.add(EditorBlock(Block()))
        ensureTrailingText()
        changed()
    }

    fun moveBlock(b: EditorBlock, delta: Int) {
        val idx = blocks.indexOf(b)
        val to = idx + delta
        if (idx < 0 || to < 0 || to >= blocks.size) return
        blocks.removeAt(idx)
        blocks.add(to, b)
        changed()
    }

    fun duplicateBlock(b: EditorBlock) {
        val idx = blocks.indexOf(b)
        blocks.add(idx + 1, EditorBlock(b.toBlock().copy(id = newId())))
        changed()
    }

    fun indent(delta: Int) {
        val b = focused ?: return
        b.indent = (b.indent + delta).coerceIn(0, 6)
        changed()
    }

    fun setBlockColor(b: EditorBlock, c: Int) { b.color = c; changed() }

    // ---------- Formato en línea ----------
    fun activeKinds(): Set<Inline.Kind> {
        val b = focused ?: return emptySet()
        if (!b.type.isText) return emptySet()
        val c = b.field.selection.start
        return Inline.parse(b.field.text).filter { c >= it.contentStart && c <= it.contentEnd }.map { it.kind }.toSet()
    }

    fun linkAtCursor(): String? {
        val b = focused ?: return null
        val c = b.field.selection.start
        return Inline.parse(b.field.text).firstOrNull { (it.kind == Inline.Kind.LINK || it.kind == Inline.Kind.URL) && c >= it.start && c <= it.end }?.url
    }

    fun toggleInline(marker: String) {
        val b = focused ?: return
        if (!b.type.isText) return
        val v = b.field
        val t = v.text
        val s = v.selection.min.coerceIn(1, t.length)
        val e = v.selection.max.coerceIn(s, t.length)
        val ml = marker.length
        val before = t.substring(0, s)
        val mid = t.substring(s, e)
        val after = t.substring(e)
        val (nt, sel) = when {
            before.endsWith(marker) && after.startsWith(marker) && before.length - ml >= 1 ->
                (before.dropLast(ml) + mid + after.drop(ml)) to TextRange(s - ml, e - ml)
            mid.length >= 2 * ml && mid.startsWith(marker) && mid.endsWith(marker) ->
                (before + mid.substring(ml, mid.length - ml) + after) to TextRange(s, e - 2 * ml)
            else -> {
                // si no hay selección, envolver la palabra bajo el cursor
                if (mid.isEmpty()) {
                    val ws = before.indexOfLast { it.isWhitespace() || it == '​' } + 1
                    val we = after.indexOfFirst { it.isWhitespace() }.let { if (it < 0) after.length else it }
                    if (ws < before.length || we > 0) {
                        val w0 = ws.coerceAtLeast(1)
                        val w1 = s + we
                        val word = t.substring(w0, w1)
                        (t.substring(0, w0) + marker + word + marker + t.substring(w1)) to TextRange(s + ml)
                    } else (before + marker + marker + after) to TextRange(s + ml)
                } else (before + marker + mid + marker + after) to TextRange(s + ml, e + ml)
            }
        }
        b.field = TextFieldValue(nt, sel)
        changed()
    }

    fun insertText(text: String) {
        val b = focused ?: return
        if (!b.type.isText) return
        val v = b.field
        val s = v.selection.min.coerceAtLeast(1)
        val e = v.selection.max.coerceAtLeast(s)
        b.field = TextFieldValue(v.text.substring(0, s) + text + v.text.substring(e), TextRange(s + text.length))
        changed()
    }

    fun insertLink(label: String, url: String) {
        val b = focused
        if (b == null || !b.type.isText) {
            insertBlocks(listOf(Block(text = "[$label]($url)")))
            return
        }
        val v = b.field
        val s = v.selection.min.coerceAtLeast(1)
        val e = v.selection.max.coerceAtLeast(s)
        val md = "[$label]($url)"
        b.field = TextFieldValue(v.text.substring(0, s) + md + v.text.substring(e), TextRange(s + md.length))
        changed()
    }

    fun selectedText(): String {
        val b = focused ?: return ""
        val v = b.field
        return v.text.substring(v.selection.min.coerceIn(0, v.text.length), v.selection.max.coerceIn(0, v.text.length)).replace(ZW, "")
    }

    /** Aplica un comando "/" sobre el bloque con foco. Devuelve la clave si requiere acción de la pantalla. */
    fun applySlash(b: EditorBlock, cmd: SlashCommand): String? {
        focusedId = b.id
        b.setText("")
        return when (cmd.key) {
            "text" -> { convert(b, BlockType.PARAGRAPH); null }
            "h1" -> { convert(b, BlockType.H1); null }
            "h2" -> { convert(b, BlockType.H2); null }
            "h3" -> { convert(b, BlockType.H3); null }
            "bullet" -> { convert(b, BlockType.BULLET); null }
            "numbered" -> { convert(b, BlockType.NUMBERED); null }
            "check" -> { convert(b, BlockType.CHECK); null }
            "task" -> { convert(b, BlockType.TASK); null }
            "quote" -> { convert(b, BlockType.QUOTE); null }
            "code" -> { convert(b, BlockType.CODE); null }
            "callout" -> { convert(b, BlockType.CALLOUT); null }
            "divider" -> { insertBlocks(listOf(Block(type = BlockType.DIVIDER))); null }
            "table" -> { insertTable(); null }
            "date" -> {
                b.setText(com.libreta.app.util.Fmt.dayHeader(System.currentTimeMillis()))
                focus(b, b.text.length); changed(); null
            }
            else -> cmd.key
        }
    }

    fun insertTable() {
        insertBlocks(listOf(Block(type = BlockType.TABLE, rows = listOf(listOf("", "", ""), listOf("", "", ""), listOf("", "", "")))), focusAfter = false)
    }

    fun setTaskDue(b: EditorBlock, due: Long?, remind: Boolean) {
        b.dueAt = due
        b.reminderAt = if (remind) due else null
        changed()
    }

    fun toggleFlag(b: EditorBlock) { b.flagged = !b.flagged; changed() }

    // ---------- Tablas ----------
    fun setCell(b: EditorBlock, r: Int, c: Int, v: String) { b.rows[r][c] = v; changed() }
    fun addRow(b: EditorBlock) { b.rows.add(mutableStateListOf<String>().apply { repeat(b.rows.firstOrNull()?.size ?: 2) { add("") } }); changed() }
    fun addColumn(b: EditorBlock) { b.rows.forEach { it.add("") }; changed() }
    fun removeRow(b: EditorBlock) { if (b.rows.size > 1) { b.rows.removeAt(b.rows.lastIndex); changed() } }
    fun removeColumn(b: EditorBlock) { if ((b.rows.firstOrNull()?.size ?: 0) > 1) { b.rows.forEach { it.removeAt(it.lastIndex) }; changed() } }

    // ---------- Metadatos ----------
    fun meta(transform: (com.libreta.app.data.NoteEntity) -> com.libreta.app.data.NoteEntity) {
        libreta.appScope.launch { repo.updateNote(noteId, transform) }
    }

    fun restoreFromVersion(content: String, t: String) {
        undo.addLast(snapshot())
        canUndo = true
        restore(Snap(t, com.libreta.app.model.BlockCodec.decode(content)))
        stable = snapshot()
        save()
    }

    override fun onCleared() {
        close()
        super.onCleared()
    }
}
