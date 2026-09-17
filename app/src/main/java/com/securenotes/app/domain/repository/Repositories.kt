package com.securenotes.app.domain.repository

import com.securenotes.app.domain.model.LinkPreview
import com.securenotes.app.domain.model.Note
import com.securenotes.app.domain.model.NoteFilter
import com.securenotes.app.domain.model.NoteSort
import com.securenotes.app.domain.model.NoteWithMeta
import com.securenotes.app.domain.model.Notebook
import com.securenotes.app.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeNotes(
        filter: NoteFilter = NoteFilter.ALL,
        notebookId: String? = null,
        tagId: String? = null,
        sort: NoteSort = NoteSort.UPDATED_DESC,
    ): Flow<List<NoteWithMeta>>

    fun observeNote(id: String): Flow<Note?>

    fun search(query: String, includeVault: Boolean = false): Flow<List<NoteWithMeta>>

    suspend fun getNote(id: String): Note?
    suspend fun upsert(note: Note): String
    suspend fun setPinned(id: String, pinned: Boolean)
    suspend fun setFavorite(id: String, favorite: Boolean)
    suspend fun setColor(id: String, colorName: String)
    suspend fun setNotebook(id: String, notebookId: String?)
    suspend fun setTags(id: String, tagIds: List<String>)
    suspend fun setReminder(id: String, at: Long?)
    suspend fun moveToVault(id: String, inVault: Boolean)
    suspend fun trash(id: String)
    suspend fun restore(id: String)
    suspend fun deleteForever(id: String)
    suspend fun emptyTrash()
    suspend fun updateLinkPreviews(id: String, previews: List<LinkPreview>)
    suspend fun countInNotebook(notebookId: String): Int
}

interface NotebookRepository {
    fun observeAll(): Flow<List<Notebook>>
    suspend fun upsert(notebook: Notebook): String
    suspend fun delete(id: String)
    suspend fun reorder(ids: List<String>)
    suspend fun setHidden(id: String, hidden: Boolean)
}

interface TagRepository {
    fun observeAll(): Flow<List<Tag>>
    suspend fun upsert(tag: Tag): String
    suspend fun delete(id: String)
    suspend fun reorder(ids: List<String>)
    suspend fun setHidden(id: String, hidden: Boolean)
    suspend fun findOrCreate(name: String): Tag
}

interface LinkPreviewRepository {
    /** Fetches (or returns cached) metadata for [url]. */
    suspend fun fetch(url: String, forceRefresh: Boolean = false): Result<LinkPreview>
}
