package com.libreta.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        NoteEntity::class, NotebookEntity::class, TagEntity::class, NoteTagRef::class,
        TaskEntity::class, NoteVersionEntity::class, TemplateEntity::class, SavedSearchEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class LibretaDatabase : RoomDatabase() {
    abstract fun notes(): NoteDao
    abstract fun notebooks(): NotebookDao
    abstract fun tags(): TagDao
    abstract fun tasks(): TaskDao
    abstract fun versions(): VersionDao
    abstract fun templates(): TemplateDao
    abstract fun searches(): SearchDao

    companion object {
        fun build(context: Context): LibretaDatabase =
            Room.databaseBuilder(context, LibretaDatabase::class.java, "libreta.db").build()
    }
}
