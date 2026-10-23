package com.libreta.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.libreta.app.model.Block
import com.libreta.app.model.BlockCodec
import com.libreta.app.model.BlockType
import com.libreta.app.model.newId
import com.libreta.app.reminders.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.io.File

data class NoteItem(
    val note: NoteEntity,
    val notebook: NotebookEntity?,
    val tags: List<TagEntity>,
)

class Repository(
    private val context: Context,
    val db: LibretaDatabase,
    val prefs: Prefs,
) {
    private val notes = db.notes()
    private val notebooksDao = db.notebooks()
    private val tagsDao = db.tags()
    private val tasksDao = db.tasks()
    private val versions = db.versions()
    private val templatesDao = db.templates()
    private val searchesDao = db.searches()

    val attachmentsDir: File get() = File(context.filesDir, "attachments").apply { mkdirs() }
    fun fileFor(path: String): File = File(attachmentsDir, path)

    // ---------- Flujos ----------
    val notebooks: Flow<List<NotebookEntity>> = notebooksDao.observeAll()
    val tags: Flow<List<TagEntity>> = tagsDao.observeAll()
    val tasks: Flow<List<TaskEntity>> = tasksDao.observeAll()
    val templates: Flow<List<TemplateEntity>> = templatesDao.observeAll()
    val savedSearches: Flow<List<SavedSearchEntity>> = searchesDao.observeAll()
    val trashed: Flow<List<NoteEntity>> = notes.observeTrashed()

    val noteItems: Flow<List<NoteItem>> = combine(
        notes.observeActive(), notebooksDao.observeAll(), tagsDao.observeAll(), tagsDao.observeRefs(),
    ) { ns, nbs, ts, refs ->
        val nbMap = nbs.associateBy { it.id }
        val tagMap = ts.associateBy { it.id }
        val byNote = refs.groupBy({ it.noteId }, { it.tagId })
        ns.map { n -> NoteItem(n, nbMap[n.notebookId], byNote[n.id].orEmpty().mapNotNull { tagMap[it] }) }
    }

    fun observeNote(id: String): Flow<NoteEntity?> = notes.observe(id)
    fun observeVersions(id: String): Flow<List<NoteVersionEntity>> = versions.observeFor(id)
    suspend fun getNote(id: String) = notes.get(id)
    suspend fun tagIdsFor(noteId: String) = tagsDao.tagIdsFor(noteId)

    // ---------- Libretas ----------
    suspend fun defaultNotebookId(): String {
        val s = prefs.current()
        val all = notebooksDao.all()
        s.defaultNotebookId?.let { id -> if (all.any { it.id == id }) return id }
        if (all.isNotEmpty()) return all.minBy { it.createdAt }.id
        val nb = NotebookEntity(newId(), "Mi libreta", color = 0)
        notebooksDao.upsert(nb)
        prefs.setDefaultNotebook(nb.id)
        return nb.id
    }

    suspend fun saveNotebook(nb: NotebookEntity) = notebooksDao.upsert(nb)

    suspend fun createNotebook(name: String, color: Int, stack: String?, emoji: String?): String {
        val nb = NotebookEntity(newId(), name.trim(), color, stack?.trim()?.ifBlank { null }, emoji?.ifBlank { null })
        notebooksDao.upsert(nb)
        return nb.id
    }

    /** Borra la libreta; sus notas van a la papelera. */
    suspend fun deleteNotebook(id: String) {
        val now = System.currentTimeMillis()
        val fallback = notebooksDao.all().filter { it.id != id }.minByOrNull { it.createdAt }
        if (fallback == null) return
        notes.all().filter { it.notebookId == id }.forEach {
            notes.upsert(it.copy(notebookId = fallback.id, isTrashed = true, trashedAt = now))
        }
        notebooksDao.delete(id)
        if (prefs.current().defaultNotebookId == id) prefs.setDefaultNotebook(fallback.id)
    }

    // ---------- Etiquetas ----------
    suspend fun createTag(name: String, color: Int = (0..7).random()): TagEntity {
        val existing = tagsDao.all().firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
        if (existing != null) return existing
        val t = TagEntity(newId(), name.trim(), color)
        tagsDao.upsert(t)
        return t
    }

    suspend fun saveTag(t: TagEntity) = tagsDao.upsert(t)

    suspend fun deleteTag(id: String) {
        tagsDao.deleteRefsForTag(id)
        tagsDao.delete(id)
    }

    suspend fun addTag(noteId: String, tagId: String) {
        tagsDao.addRef(NoteTagRef(noteId, tagId))
        touch(noteId)
    }

    suspend fun removeTag(noteId: String, tagId: String) {
        tagsDao.removeRef(NoteTagRef(noteId, tagId))
        touch(noteId)
    }

    private suspend fun touch(noteId: String) {
        notes.get(noteId)?.let { notes.upsert(it.copy(updatedAt = System.currentTimeMillis())) }
    }

    // ---------- Notas ----------
    suspend fun createNote(
        notebookId: String? = null,
        title: String = "",
        blocks: List<Block> = listOf(Block()),
        tagIds: List<String> = emptyList(),
        sourceUrl: String? = null,
    ): String {
        val id = newId()
        val nbId = notebookId ?: defaultNotebookId()
        val fresh = blocks.map { it.copy(id = newId()) }
        notes.upsert(buildNote(NoteEntity(id = id, notebookId = nbId, sourceUrl = sourceUrl), title, fresh))
        tagIds.forEach { tagsDao.addRef(NoteTagRef(id, it)) }
        syncTasks(id, fresh)
        return id
    }

    private fun buildNote(base: NoteEntity, title: String, blocks: List<Block>): NoteEntity {
        val plain = BlockCodec.plainText(blocks)
        return base.copy(
            title = title,
            content = BlockCodec.encode(blocks),
            plainText = plain,
            preview = BlockCodec.preview(blocks),
            hasImage = blocks.any { it.type == BlockType.IMAGE },
            hasFile = blocks.any { it.type == BlockType.FILE },
            hasAudio = blocks.any { it.type == BlockType.AUDIO },
            hasChecklist = blocks.any { it.type == BlockType.CHECK },
            hasTask = blocks.any { it.type == BlockType.TASK },
            coverPath = blocks.firstOrNull { it.type == BlockType.IMAGE }?.path,
            wordCount = BlockCodec.wordCount(title + " " + plain),
        )
    }

    /** Guarda el contenido de la nota. Crea una versión en el historial cada ~10 minutos de edición. */
    suspend fun saveContent(id: String, title: String, blocks: List<Block>) {
        val current = notes.get(id) ?: return
        val updated = buildNote(current, title, blocks).copy(updatedAt = System.currentTimeMillis())
        if (updated.content == current.content && updated.title == current.title) return
        val last = versions.latest(id)
        if (current.content != "[]" && (last == null || System.currentTimeMillis() - last.savedAt > 10 * 60_000)) {
            versions.insert(NoteVersionEntity(noteId = id, title = current.title, content = current.content, savedAt = current.updatedAt))
            versions.trim(id, 40)
        }
        notes.upsert(updated)
        syncTasks(id, blocks)
    }

    suspend fun snapshotVersion(id: String) {
        val n = notes.get(id) ?: return
        versions.insert(NoteVersionEntity(noteId = id, title = n.title, content = n.content, savedAt = System.currentTimeMillis()))
    }

    suspend fun restoreVersion(v: NoteVersionEntity) {
        snapshotVersion(v.noteId)
        val n = notes.get(v.noteId) ?: return
        val blocks = BlockCodec.decode(v.content)
        notes.upsert(buildNote(n, v.title, blocks).copy(updatedAt = System.currentTimeMillis()))
        syncTasks(n.id, blocks)
    }

    suspend fun updateNote(id: String, transform: (NoteEntity) -> NoteEntity) {
        val n = notes.get(id) ?: return
        val updated = transform(n)
        notes.upsert(updated)
        if (n.reminderAt != updated.reminderAt || n.reminderDone != updated.reminderDone || n.isTrashed != updated.isTrashed) {
            ReminderScheduler.syncNote(context, updated)
        }
    }

    suspend fun setReminder(id: String, at: Long?) = updateNote(id) { it.copy(reminderAt = at, reminderDone = false) }
    suspend fun completeReminder(id: String, done: Boolean = true) = updateNote(id) { it.copy(reminderDone = done) }

    suspend fun trash(id: String) {
        updateNote(id) { it.copy(isTrashed = true, trashedAt = System.currentTimeMillis(), isPinned = false) }
        tasksDao.forNote(id).forEach { ReminderScheduler.cancelTask(context, it.id) }
    }

    suspend fun restore(id: String) {
        val n = notes.get(id) ?: return
        val nb = if (notebooksDao.get(n.notebookId) != null) n.notebookId else defaultNotebookId()
        updateNote(id) { it.copy(isTrashed = false, trashedAt = null, notebookId = nb) }
    }

    suspend fun deleteForever(id: String) {
        val n = notes.get(id) ?: return
        BlockCodec.decode(n.content).mapNotNull { it.path }.forEach { fileFor(it).delete() }
        tagsDao.deleteRefsForNote(id)
        tasksDao.forNote(id).forEach { ReminderScheduler.cancelTask(context, it.id) }
        tasksDao.deleteForNote(id)
        versions.deleteFor(id)
        ReminderScheduler.cancelNote(context, id)
        notes.delete(id)
    }

    suspend fun emptyTrash() {
        notes.trashedIds().forEach { deleteForever(it) }
    }

    suspend fun purgeOldTrash() {
        val days = prefs.current().trashAutoDays
        if (days <= 0) return
        val limit = System.currentTimeMillis() - days * 86_400_000L
        notes.all().filter { it.isTrashed && (it.trashedAt ?: 0) < limit }.forEach { deleteForever(it.id) }
    }

    suspend fun duplicate(id: String): String? {
        val n = notes.get(id) ?: return null
        val blocks = BlockCodec.decode(n.content).map { b ->
            if (b.path != null) {
                val src = fileFor(b.path)
                val newPath = newId() + "." + src.extension
                if (src.exists()) src.copyTo(fileFor(newPath))
                b.copy(path = newPath)
            } else b
        }
        val newIdValue = createNote(n.notebookId, n.title + " (copia)", blocks, tagsDao.tagIdsFor(id))
        return newIdValue
    }

    suspend fun moveNotes(ids: Collection<String>, notebookId: String) {
        ids.forEach { id -> updateNote(id) { it.copy(notebookId = notebookId, updatedAt = System.currentTimeMillis()) } }
    }

    // ---------- Tareas ----------
    /** Refleja los bloques TASK de una nota en la tabla de tareas. */
    private suspend fun syncTasks(noteId: String, blocks: List<Block>) {
        val existing = tasksDao.forNote(noteId).associateBy { it.id }
        val taskBlocks = blocks.filter { it.type == BlockType.TASK }
        val ids = taskBlocks.map { it.id }.toSet()
        existing.values.filter { it.id !in ids }.forEach {
            tasksDao.delete(it.id); ReminderScheduler.cancelTask(context, it.id)
        }
        taskBlocks.forEach { b ->
            val old = existing[b.id]
            val t = TaskEntity(
                id = b.id, noteId = noteId, title = com.libreta.app.model.Inline.strip(b.text),
                done = b.checked, dueAt = b.dueAt, reminderAt = b.reminderAt, flagged = b.flagged,
                priority = old?.priority ?: 0, createdAt = old?.createdAt ?: System.currentTimeMillis(),
                completedAt = if (b.checked) old?.completedAt ?: System.currentTimeMillis() else null,
            )
            if (t != old) {
                tasksDao.upsert(t)
                ReminderScheduler.syncTask(context, t)
            }
        }
    }

    suspend fun saveTask(task: TaskEntity) {
        val t = task.copy(completedAt = if (task.done) task.completedAt ?: System.currentTimeMillis() else null)
        tasksDao.upsert(t)
        ReminderScheduler.syncTask(context, t)
        val noteId = t.noteId ?: return
        val n = notes.get(noteId) ?: return
        val blocks = BlockCodec.decode(n.content)
        if (blocks.none { it.id == t.id }) return
        val patched = blocks.map { b ->
            if (b.id == t.id) b.copy(
                text = if (com.libreta.app.model.Inline.strip(b.text) == t.title) b.text else t.title,
                checked = t.done, dueAt = t.dueAt, reminderAt = t.reminderAt, flagged = t.flagged,
            ) else b
        }
        notes.upsert(buildNote(n, n.title, patched).copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun createTask(title: String, dueAt: Long? = null, flagged: Boolean = false, noteId: String? = null): TaskEntity {
        val t = TaskEntity(newId(), noteId = noteId, title = title.trim(), dueAt = dueAt, flagged = flagged)
        tasksDao.upsert(t)
        return t
    }

    suspend fun deleteTask(task: TaskEntity) {
        ReminderScheduler.cancelTask(context, task.id)
        tasksDao.delete(task.id)
        val noteId = task.noteId ?: return
        val n = notes.get(noteId) ?: return
        val blocks = BlockCodec.decode(n.content).filterNot { it.id == task.id }
        notes.upsert(buildNote(n, n.title, blocks).copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun getTask(id: String) = tasksDao.get(id)

    // ---------- Plantillas y búsquedas ----------
    suspend fun saveTemplate(name: String, title: String, blocks: List<Block>, emoji: String = "📄") =
        templatesDao.upsert(TemplateEntity(newId(), name, title, BlockCodec.encode(blocks), emoji))

    suspend fun deleteTemplate(id: String) = templatesDao.delete(id)

    suspend fun saveSearch(name: String, query: String) = searchesDao.upsert(SavedSearchEntity(newId(), name, query))
    suspend fun deleteSearch(id: String) = searchesDao.delete(id)

    // ---------- Adjuntos ----------
    suspend fun importAttachment(uri: Uri, forceType: BlockType? = null): Block? = withContext(Dispatchers.IO) {
        try {
            val cr = context.contentResolver
            var name = "archivo"
            var size = 0L
            cr.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val si = c.getColumnIndex(OpenableColumns.SIZE)
                    if (ni >= 0) name = c.getString(ni) ?: name
                    if (si >= 0) size = c.getLong(si)
                }
            }
            val mime = cr.getType(uri) ?: MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(name.substringAfterLast('.', "").lowercase()) ?: "application/octet-stream"
            val ext = name.substringAfterLast('.', "").ifBlank {
                MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "bin"
            }
            val path = newId() + "." + ext
            cr.openInputStream(uri)?.use { input -> fileFor(path).outputStream().use { input.copyTo(it) } }
            if (size == 0L) size = fileFor(path).length()
            val type = forceType ?: when {
                mime.startsWith("image/") -> BlockType.IMAGE
                mime.startsWith("audio/") -> BlockType.AUDIO
                else -> BlockType.FILE
            }
            Block(type = type, path = path, name = name, mime = mime, size = size)
        } catch (e: Exception) {
            null
        }
    }

    fun newAttachmentFile(ext: String): Pair<String, File> {
        val path = newId() + "." + ext
        return path to fileFor(path)
    }

    // ---------- Datos iniciales ----------
    suspend fun ensureSeed() {
        if (notebooksDao.all().isNotEmpty()) return
        val personal = NotebookEntity(newId(), "Personal", color = 0, emoji = "🏠", createdAt = System.currentTimeMillis() - 3000)
        val trabajo = NotebookEntity(newId(), "Trabajo", color = 5, stack = "Proyectos", emoji = "💼", createdAt = System.currentTimeMillis() - 2000)
        val ideas = NotebookEntity(newId(), "Ideas", color = 2, stack = "Proyectos", emoji = "💡", createdAt = System.currentTimeMillis() - 1000)
        listOf(personal, trabajo, ideas).forEach { notebooksDao.upsert(it) }
        prefs.setDefaultNotebook(personal.id)
        val tagImportante = createTag("importante", 0)
        val tagLeer = createTag("para-leer", 3)
        createTag("ideas", 4)

        val welcome = listOf(
            Block(type = BlockType.CALLOUT, text = "Bienvenido a **Libreta** — tu cuaderno de papel marfil y tinta azul."),
            Block(type = BlockType.H2, text = "Lo básico"),
            Block(type = BlockType.PARAGRAPH, text = "Escribí libremente. Usá **negrita**, *cursiva*, ++subrayado++, ~~tachado~~, ==resaltado== y `código`. Los enlaces también funcionan: [libreta](https://example.com)."),
            Block(type = BlockType.CHECK, text = "Tocá un casillero para tacharlo", checked = true),
            Block(type = BlockType.CHECK, text = "Escribí \"/\" en una línea vacía para ver todos los bloques"),
            Block(type = BlockType.TASK, text = "Probá una tarea con fecha y bandera", dueAt = System.currentTimeMillis() + 86_400_000L, flagged = true),
            Block(type = BlockType.H2, text = "Atajos de escritura"),
            Block(type = BlockType.BULLET, text = "`# ` título, `## ` subtítulo, `- ` viñeta, `1. ` lista numerada"),
            Block(type = BlockType.BULLET, text = "`[] ` casilla, `[!] ` tarea, `> ` cita, `!! ` destacado, `---` separador"),
            Block(type = BlockType.QUOTE, text = "La memoria es frágil; la tinta, paciente."),
            Block(type = BlockType.DIVIDER),
            Block(type = BlockType.TABLE, rows = listOf(listOf("Función", "Dónde"), listOf("Escanear documentos", "Botón +"), listOf("Grabar audio", "Barra del editor"), listOf("Bocetos a mano", "Barra del editor"))),
            Block(type = BlockType.PARAGRAPH, text = "Organizá con libretas, pilas y etiquetas. Fijá notas en el inicio, agregá recordatorios y bloqueá la app con PIN o huella."),
        )
        createNote(personal.id, "👋 Empezá acá", welcome, listOf(tagImportante.id)).also { id ->
            updateNote(id) { it.copy(isPinned = true, isShortcut = true, color = 4) }
        }
        createNote(
            trabajo.id, "Reunión de equipo — planificación",
            listOf(
                Block(type = BlockType.H3, text = "Asistentes"),
                Block(type = BlockType.BULLET, text = "Ana, Marcos, Lucía"),
                Block(type = BlockType.H3, text = "Decisiones"),
                Block(type = BlockType.NUMBERED, text = "Lanzar la beta el **viernes**"),
                Block(type = BlockType.NUMBERED, text = "Revisar el ==presupuesto== del trimestre"),
                Block(type = BlockType.H3, text = "Próximos pasos"),
                Block(type = BlockType.TASK, text = "Enviar minuta al equipo", dueAt = System.currentTimeMillis() + 3 * 3_600_000L),
                Block(type = BlockType.TASK, text = "Preparar demo", dueAt = System.currentTimeMillis() + 2 * 86_400_000L),
            ),
        )
        createNote(
            ideas.id, "Libros para este año",
            listOf(
                Block(type = BlockType.CHECK, text = "*Rayuela* — Julio Cortázar", checked = true),
                Block(type = BlockType.CHECK, text = "*Ficciones* — Jorge Luis Borges"),
                Block(type = BlockType.CHECK, text = "*Cien años de soledad* — Gabriel García Márquez"),
            ),
            listOf(tagLeer.id),
        )
        createTask("Comprar cuadernos nuevos", dueAt = System.currentTimeMillis() - 3_600_000L)
        createTask("Llamar a mamá", flagged = true)
    }
}
