package com.libreta.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "notes", indices = [Index("notebookId"), Index("updatedAt")])
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String = "",
    val content: String = "[]",
    val plainText: String = "",
    val preview: String = "",
    val notebookId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isShortcut: Boolean = false,
    val isPinned: Boolean = false,
    val color: Int = -1,
    val reminderAt: Long? = null,
    val reminderDone: Boolean = false,
    val isTrashed: Boolean = false,
    val trashedAt: Long? = null,
    val isLocked: Boolean = false,
    val sourceUrl: String? = null,
    val coverPath: String? = null,
    val hasImage: Boolean = false,
    val hasFile: Boolean = false,
    val hasAudio: Boolean = false,
    val hasChecklist: Boolean = false,
    val hasTask: Boolean = false,
    val wordCount: Int = 0,
)

@Serializable
@Entity(tableName = "notebooks")
data class NotebookEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: Int = 0,
    val stack: String? = null,
    val emoji: String? = null,
    val isShortcut: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: Int = 0,
    val isShortcut: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(tableName = "note_tags", primaryKeys = ["noteId", "tagId"], indices = [Index("tagId")])
data class NoteTagRef(val noteId: String, val tagId: String)

@Serializable
@Entity(tableName = "tasks", indices = [Index("noteId")])
data class TaskEntity(
    @PrimaryKey val id: String,
    val noteId: String? = null,
    val title: String,
    val done: Boolean = false,
    val dueAt: Long? = null,
    val reminderAt: Long? = null,
    val flagged: Boolean = false,
    val priority: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
)

@Serializable
@Entity(tableName = "note_versions", indices = [Index("noteId")])
data class NoteVersionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: String,
    val title: String,
    val content: String,
    val savedAt: Long,
)

@Serializable
@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val title: String,
    val content: String,
    val emoji: String = "📄",
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(tableName = "saved_searches")
data class SavedSearchEntity(
    @PrimaryKey val id: String,
    val name: String,
    val query: String,
    val createdAt: Long = System.currentTimeMillis(),
)
