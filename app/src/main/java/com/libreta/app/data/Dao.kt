package com.libreta.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isTrashed = 0 ORDER BY updatedAt DESC")
    fun observeActive(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isTrashed = 1 ORDER BY trashedAt DESC")
    fun observeTrashed(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun observe(id: String): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun get(id: String): NoteEntity?

    @Query("SELECT * FROM notes")
    suspend fun all(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE reminderAt IS NOT NULL AND reminderDone = 0 AND isTrashed = 0")
    suspend fun pendingReminders(): List<NoteEntity>

    @Upsert
    suspend fun upsert(note: NoteEntity)

    @Upsert
    suspend fun upsertAll(notes: List<NoteEntity>)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM notes WHERE isTrashed = 1")
    suspend fun emptyTrash()

    @Query("SELECT id FROM notes WHERE isTrashed = 1")
    suspend fun trashedIds(): List<String>

    @Query("UPDATE notes SET notebookId = :to WHERE notebookId = :from")
    suspend fun moveAll(from: String, to: String)
}

@Dao
interface NotebookDao {
    @Query("SELECT * FROM notebooks ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<NotebookEntity>>

    @Query("SELECT * FROM notebooks")
    suspend fun all(): List<NotebookEntity>

    @Query("SELECT * FROM notebooks WHERE id = :id")
    suspend fun get(id: String): NotebookEntity?

    @Upsert
    suspend fun upsert(nb: NotebookEntity)

    @Query("DELETE FROM notebooks WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags")
    suspend fun all(): List<TagEntity>

    @Query("SELECT * FROM note_tags")
    fun observeRefs(): Flow<List<NoteTagRef>>

    @Query("SELECT * FROM note_tags")
    suspend fun allRefs(): List<NoteTagRef>

    @Query("SELECT tagId FROM note_tags WHERE noteId = :noteId")
    suspend fun tagIdsFor(noteId: String): List<String>

    @Upsert
    suspend fun upsert(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM note_tags WHERE tagId = :id")
    suspend fun deleteRefsForTag(id: String)

    @Query("DELETE FROM note_tags WHERE noteId = :noteId")
    suspend fun deleteRefsForNote(noteId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addRef(ref: NoteTagRef)

    @Delete
    suspend fun removeRef(ref: NoteTagRef)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY done ASC, CASE WHEN dueAt IS NULL THEN 1 ELSE 0 END, dueAt ASC, createdAt DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun get(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE noteId = :noteId")
    suspend fun forNote(noteId: String): List<TaskEntity>

    @Query("SELECT * FROM tasks")
    suspend fun all(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE reminderAt IS NOT NULL AND done = 0")
    suspend fun pendingReminders(): List<TaskEntity>

    @Upsert
    suspend fun upsert(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM tasks WHERE noteId = :noteId")
    suspend fun deleteForNote(noteId: String)
}

@Dao
interface VersionDao {
    @Query("SELECT * FROM note_versions WHERE noteId = :noteId ORDER BY savedAt DESC")
    fun observeFor(noteId: String): Flow<List<NoteVersionEntity>>

    @Query("SELECT * FROM note_versions WHERE noteId = :noteId ORDER BY savedAt DESC LIMIT 1")
    suspend fun latest(noteId: String): NoteVersionEntity?

    @Insert
    suspend fun insert(v: NoteVersionEntity)

    @Query("DELETE FROM note_versions WHERE noteId = :noteId AND id NOT IN (SELECT id FROM note_versions WHERE noteId = :noteId ORDER BY savedAt DESC LIMIT :keep)")
    suspend fun trim(noteId: String, keep: Int)

    @Query("DELETE FROM note_versions WHERE noteId = :noteId")
    suspend fun deleteFor(noteId: String)
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates")
    suspend fun all(): List<TemplateEntity>

    @Upsert
    suspend fun upsert(t: TemplateEntity)

    @Query("DELETE FROM templates WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface SearchDao {
    @Query("SELECT * FROM saved_searches ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SavedSearchEntity>>

    @Query("SELECT * FROM saved_searches")
    suspend fun all(): List<SavedSearchEntity>

    @Upsert
    suspend fun upsert(s: SavedSearchEntity)

    @Query("DELETE FROM saved_searches WHERE id = :id")
    suspend fun delete(id: String)
}
