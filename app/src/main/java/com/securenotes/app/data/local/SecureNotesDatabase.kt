package com.securenotes.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.securenotes.app.data.local.dao.LinkPreviewDao
import com.securenotes.app.data.local.dao.NoteDao
import com.securenotes.app.data.local.dao.NotebookDao
import com.securenotes.app.data.local.dao.TagDao
import com.securenotes.app.data.local.entity.LinkPreviewEntity
import com.securenotes.app.data.local.entity.NoteEntity
import com.securenotes.app.data.local.entity.NoteTagCrossRef
import com.securenotes.app.data.local.entity.NotebookEntity
import com.securenotes.app.data.local.entity.TagEntity

@Database(
    entities = [
        NoteEntity::class,
        NotebookEntity::class,
        TagEntity::class,
        NoteTagCrossRef::class,
        LinkPreviewEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class SecureNotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun notebookDao(): NotebookDao
    abstract fun tagDao(): TagDao
    abstract fun linkPreviewDao(): LinkPreviewDao

    companion object {
        const val NAME = "securenotes-encrypted.db"
    }
}
