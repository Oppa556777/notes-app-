package com.securenotes.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.securenotes.app.data.local.entity.LinkPreviewEntity
import com.securenotes.app.data.local.entity.NoteEntity
import com.securenotes.app.data.local.entity.NoteTagCrossRef
import com.securenotes.app.data.local.entity.NotebookEntity
import com.securenotes.app.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query(
        """
        SELECT * FROM notes
        WHERE isTrashed = 0 AND isArchived = 0 AND isInVault = :inVault
        ORDER BY isPinned DESC, updatedAt DESC
        """,
    )
    fun observeActive(inVault: Boolean): Flow<List<NoteEntity>>

    @Query(
        """
        SELECT * FROM notes
        WHERE isTrashed = 0 AND isArchived = 0 AND isInVault = 0 AND isFavorite = 1
        ORDER BY isPinned DESC, updatedAt DESC
        """,
    )
    fun observeFavorites(): Flow<List<NoteEntity>>

    @Query(
        """
        SELECT * FROM notes
        WHERE isTrashed = 0 AND isArchived = 0 AND isInVault = 0 AND isPinned = 1
        ORDER BY updatedAt DESC
        """,
    )
    fun observePinned(): Flow<List<NoteEntity>>

    @Query(
        """
        SELECT * FROM notes
        WHERE isTrashed = 0 AND isInVault = 0 AND reminderAt IS NOT NULL
        ORDER BY reminderAt ASC
        """,
    )
    fun observeWithReminders(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isTrashed = 1 ORDER BY updatedAt DESC")
    fun observeTrashed(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isArchived = 1 AND isTrashed = 0 ORDER BY updatedAt DESC")
    fun observeArchived(): Flow<List<NoteEntity>>

    @Query(
        """
        SELECT * FROM notes
        WHERE notebookId = :notebookId AND isTrashed = 0 AND isArchived = 0
        ORDER BY isPinned DESC, updatedAt DESC
        """,
    )
    fun observeByNotebook(notebookId: String): Flow<List<NoteEntity>>

    @Query(
        """
        SELECT n.* FROM notes n
        INNER JOIN note_tag_cross_ref r ON n.id = r.noteId
        WHERE r.tagId = :tagId AND n.isTrashed = 0 AND n.isArchived = 0
        ORDER BY n.isPinned DESC, n.updatedAt DESC
        """,
    )
    fun observeByTag(tagId: String): Flow<List<NoteEntity>>

    @Query(
        """
        SELECT * FROM notes
        WHERE isTrashed = 0
          AND (isInVault = 0 OR :includeVault = 1)
          AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%')
        ORDER BY isPinned DESC, updatedAt DESC
        """,
    )
    fun search(query: String, includeVault: Boolean): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun observeById(id: String): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: String): NoteEntity?

    @Upsert
    suspend fun upsert(note: NoteEntity)

    @Query("UPDATE notes SET isPinned = :pinned, updatedAt = :now WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean, now: Long)

    @Query("UPDATE notes SET isFavorite = :favorite, updatedAt = :now WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean, now: Long)

    @Query("UPDATE notes SET color = :color, updatedAt = :now WHERE id = :id")
    suspend fun setColor(id: String, color: String, now: Long)

    @Query("UPDATE notes SET notebookId = :notebookId, updatedAt = :now WHERE id = :id")
    suspend fun setNotebook(id: String, notebookId: String?, now: Long)

    @Query("UPDATE notes SET reminderAt = :at, updatedAt = :now WHERE id = :id")
    suspend fun setReminder(id: String, at: Long?, now: Long)

    @Query("UPDATE notes SET isInVault = :inVault, updatedAt = :now WHERE id = :id")
    suspend fun setInVault(id: String, inVault: Boolean, now: Long)

    @Query("UPDATE notes SET isTrashed = :trashed, updatedAt = :now WHERE id = :id")
    suspend fun setTrashed(id: String, trashed: Boolean, now: Long)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteForever(id: String)

    @Query("DELETE FROM notes WHERE isTrashed = 1")
    suspend fun emptyTrash()

    @Query("SELECT COUNT(*) FROM notes WHERE notebookId = :notebookId AND isTrashed = 0")
    suspend fun countInNotebook(notebookId: String): Int

    @Query("UPDATE notes SET linkPreviews = :json, updatedAt = :now WHERE id = :id")
    suspend fun updateLinkPreviewsJson(id: String, json: String, now: Long)

    // -------------------------------------------------------------- tags ---

    @Query("SELECT tagId FROM note_tag_cross_ref WHERE noteId = :noteId")
    suspend fun tagIdsFor(noteId: String): List<String>

    @Query("SELECT * FROM note_tag_cross_ref")
    fun observeAllCrossRefs(): Flow<List<NoteTagCrossRef>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(ref: NoteTagCrossRef)

    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId")
    suspend fun clearTagsFor(noteId: String)

    @Transaction
    suspend fun replaceTags(noteId: String, tagIds: List<String>) {
        clearTagsFor(noteId)
        tagIds.forEach { insertCrossRef(NoteTagCrossRef(noteId, it)) }
    }
}

@Dao
interface NotebookDao {
    @Query("SELECT * FROM notebooks ORDER BY sortOrder ASC, createdAt ASC")
    fun observeAll(): Flow<List<NotebookEntity>>

    @Upsert
    suspend fun upsert(notebook: NotebookEntity)

    @Query("DELETE FROM notebooks WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE notebooks SET sortOrder = :order WHERE id = :id")
    suspend fun setOrder(id: String, order: Int)

    @Query("UPDATE notebooks SET isHidden = :hidden WHERE id = :id")
    suspend fun setHidden(id: String, hidden: Boolean)

    @Query("UPDATE notes SET notebookId = NULL WHERE notebookId = :id")
    suspend fun detachNotes(id: String)
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY sortOrder ASC, name ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Upsert
    suspend fun upsert(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE tags SET sortOrder = :order WHERE id = :id")
    suspend fun setOrder(id: String, order: Int)

    @Query("UPDATE tags SET isHidden = :hidden WHERE id = :id")
    suspend fun setHidden(id: String, hidden: Boolean)

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): TagEntity?
}

@Dao
interface LinkPreviewDao {
    @Query("SELECT * FROM link_previews WHERE url = :url")
    suspend fun get(url: String): LinkPreviewEntity?

    @Upsert
    suspend fun upsert(preview: LinkPreviewEntity)
}
