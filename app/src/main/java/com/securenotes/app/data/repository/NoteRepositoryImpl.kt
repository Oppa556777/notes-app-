package com.securenotes.app.data.repository

import com.securenotes.app.data.local.Converters
import com.securenotes.app.data.local.dao.NoteDao
import com.securenotes.app.data.local.dao.NotebookDao
import com.securenotes.app.data.local.dao.TagDao
import com.securenotes.app.data.local.entity.NoteEntity
import com.securenotes.app.domain.model.LinkPreview
import com.securenotes.app.domain.model.Note
import com.securenotes.app.domain.model.NoteFilter
import com.securenotes.app.domain.model.NoteSort
import com.securenotes.app.domain.model.NoteWithMeta
import com.securenotes.app.domain.repository.NoteRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val notebookDao: NotebookDao,
    private val tagDao: TagDao,
    private val ioDispatcher: CoroutineDispatcher,
) : NoteRepository {

    private val converters = Converters()

    override fun observeNotes(
        filter: NoteFilter,
        notebookId: String?,
        tagId: String?,
        sort: NoteSort,
    ): Flow<List<NoteWithMeta>> {
        val source: Flow<List<NoteEntity>> = when {
            notebookId != null -> noteDao.observeByNotebook(notebookId)
            tagId != null -> noteDao.observeByTag(tagId)
            else -> when (filter) {
                NoteFilter.ALL -> noteDao.observeActive(inVault = false)
                NoteFilter.VAULT -> noteDao.observeActive(inVault = true)
                NoteFilter.FAVORITES -> noteDao.observeFavorites()
                NoteFilter.PINNED -> noteDao.observePinned()
                NoteFilter.REMINDERS -> noteDao.observeWithReminders()
                NoteFilter.TRASH -> noteDao.observeTrashed()
                NoteFilter.ARCHIVE -> noteDao.observeArchived()
            }
        }
        return decorate(source, sort)
    }

    override fun observeNote(id: String): Flow<Note?> =
        combine(noteDao.observeById(id), noteDao.observeAllCrossRefs()) { entity, refs ->
            entity?.toDomain(refs.filter { it.noteId == id }.map { it.tagId })
        }.flowOn(ioDispatcher)

    override fun search(query: String, includeVault: Boolean): Flow<List<NoteWithMeta>> =
        decorate(noteDao.search(query, includeVault), NoteSort.UPDATED_DESC)

    /** Joins notes with their notebook + tag objects and applies client-side sorting. */
    private fun decorate(source: Flow<List<NoteEntity>>, sort: NoteSort): Flow<List<NoteWithMeta>> =
        combine(
            source,
            notebookDao.observeAll(),
            tagDao.observeAll(),
            noteDao.observeAllCrossRefs(),
        ) { notes, notebooks, tags, refs ->
            val notebooksById = notebooks.associateBy { it.id }
            val tagsById = tags.associateBy { it.id }
            val tagIdsByNote = refs.groupBy({ it.noteId }, { it.tagId })

            notes.map { entity ->
                val tagIds = tagIdsByNote[entity.id].orEmpty()
                NoteWithMeta(
                    note = entity.toDomain(tagIds),
                    notebook = entity.notebookId?.let { notebooksById[it]?.toDomain() },
                    tags = tagIds.mapNotNull { tagsById[it]?.toDomain() },
                )
            }.sortedWith(comparatorFor(sort))
        }.flowOn(ioDispatcher)

    private fun comparatorFor(sort: NoteSort): Comparator<NoteWithMeta> {
        val pinnedFirst = compareByDescending<NoteWithMeta> { it.note.isPinned }
        return when (sort) {
            NoteSort.UPDATED_DESC -> pinnedFirst.thenByDescending { it.note.updatedAt }
            NoteSort.CREATED_DESC -> pinnedFirst.thenByDescending { it.note.createdAt }
            NoteSort.TITLE_ASC -> pinnedFirst.thenBy { it.note.title.lowercase() }
            NoteSort.COLOR -> pinnedFirst.thenBy { it.note.color.ordinal }
        }
    }

    override suspend fun getNote(id: String): Note? = withContext(ioDispatcher) {
        noteDao.getById(id)?.toDomain(noteDao.tagIdsFor(id))
    }

    override suspend fun upsert(note: Note): String = withContext(ioDispatcher) {
        val id = note.id.ifBlank { UUID.randomUUID().toString() }
        val toSave = note.copy(id = id, updatedAt = System.currentTimeMillis())
        noteDao.upsert(NoteEntity.fromDomain(toSave))
        noteDao.replaceTags(id, note.tags)
        id
    }

    override suspend fun setPinned(id: String, pinned: Boolean) = withContext(ioDispatcher) {
        noteDao.setPinned(id, pinned, now())
    }

    override suspend fun setFavorite(id: String, favorite: Boolean) = withContext(ioDispatcher) {
        noteDao.setFavorite(id, favorite, now())
    }

    override suspend fun setColor(id: String, colorName: String) = withContext(ioDispatcher) {
        noteDao.setColor(id, colorName, now())
    }

    override suspend fun setNotebook(id: String, notebookId: String?) = withContext(ioDispatcher) {
        noteDao.setNotebook(id, notebookId, now())
    }

    override suspend fun setTags(id: String, tagIds: List<String>) = withContext(ioDispatcher) {
        noteDao.replaceTags(id, tagIds)
    }

    override suspend fun setReminder(id: String, at: Long?) = withContext(ioDispatcher) {
        noteDao.setReminder(id, at, now())
    }

    override suspend fun moveToVault(id: String, inVault: Boolean) = withContext(ioDispatcher) {
        noteDao.setInVault(id, inVault, now())
    }

    override suspend fun trash(id: String) = withContext(ioDispatcher) {
        noteDao.setTrashed(id, true, now())
    }

    override suspend fun restore(id: String) = withContext(ioDispatcher) {
        noteDao.setTrashed(id, false, now())
    }

    override suspend fun deleteForever(id: String) = withContext(ioDispatcher) {
        noteDao.deleteForever(id)
    }

    override suspend fun emptyTrash() = withContext(ioDispatcher) { noteDao.emptyTrash() }

    override suspend fun updateLinkPreviews(id: String, previews: List<LinkPreview>) =
        withContext(ioDispatcher) {
            noteDao.updateLinkPreviewsJson(id, converters.linkPreviewsToJson(previews), now())
        }

    override suspend fun countInNotebook(notebookId: String): Int = withContext(ioDispatcher) {
        noteDao.countInNotebook(notebookId)
    }

    private fun now() = System.currentTimeMillis()
}
