package com.libreta.app.data

import android.content.Context
import android.net.Uri
import com.libreta.app.model.BlockCodec
import com.libreta.app.reminders.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAt: Long,
    val notes: List<NoteEntity>,
    val notebooks: List<NotebookEntity>,
    val tags: List<TagEntity>,
    val refs: List<NoteTagRef>,
    val tasks: List<TaskEntity>,
    val templates: List<TemplateEntity>,
    val searches: List<SavedSearchEntity>,
)

/** Copia de seguridad completa (.zip con JSON + adjuntos). */
object Backup {
    suspend fun export(context: Context, repo: Repository, uri: Uri): Int = withContext(Dispatchers.IO) {
        val db = repo.db
        val data = BackupData(
            exportedAt = System.currentTimeMillis(),
            notes = db.notes().all(), notebooks = db.notebooks().all(), tags = db.tags().all(),
            refs = db.tags().allRefs(), tasks = db.tasks().all(), templates = db.templates().all(), searches = db.searches().all(),
        )
        context.contentResolver.openOutputStream(uri)?.use { os ->
            ZipOutputStream(os.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry("libreta.json"))
                zip.write(BlockCodec.json.encodeToString(data).toByteArray())
                zip.closeEntry()
                repo.attachmentsDir.listFiles()?.forEach { f ->
                    zip.putNextEntry(ZipEntry("attachments/" + f.name))
                    f.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
        repo.prefs.setLastBackup(System.currentTimeMillis())
        data.notes.size
    }

    suspend fun import(context: Context, repo: Repository, uri: Uri): Int = withContext(Dispatchers.IO) {
        var data: BackupData? = null
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input.buffered()).use { zip ->
                var e = zip.nextEntry
                while (e != null) {
                    when {
                        e.name == "libreta.json" -> data = BlockCodec.json.decodeFromString(BackupData.serializer(), zip.readBytes().decodeToString())
                        e.name.startsWith("attachments/") && !e.isDirectory -> {
                            val name = e.name.substringAfterLast('/')
                            if (name.isNotBlank() && !name.contains("..")) repo.fileFor(name).outputStream().use { zip.copyTo(it) }
                        }
                    }
                    zip.closeEntry()
                    e = zip.nextEntry
                }
            }
        }
        val d = data ?: return@withContext 0
        val db = repo.db
        d.notebooks.forEach { db.notebooks().upsert(it) }
        d.tags.forEach { db.tags().upsert(it) }
        db.notes().upsertAll(d.notes)
        d.refs.forEach { db.tags().addRef(it) }
        d.tasks.forEach { db.tasks().upsert(it); ReminderScheduler.syncTask(context, it) }
        d.templates.forEach { db.templates().upsert(it) }
        d.searches.forEach { db.searches().upsert(it) }
        d.notes.forEach { ReminderScheduler.syncNote(context, it) }
        d.notes.size
    }
}
