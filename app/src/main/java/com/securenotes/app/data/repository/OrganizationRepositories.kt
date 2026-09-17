package com.securenotes.app.data.repository

import com.securenotes.app.data.local.dao.NotebookDao
import com.securenotes.app.data.local.dao.TagDao
import com.securenotes.app.data.local.entity.NotebookEntity
import com.securenotes.app.data.local.entity.TagEntity
import com.securenotes.app.domain.model.Notebook
import com.securenotes.app.domain.model.Tag
import com.securenotes.app.domain.repository.NotebookRepository
import com.securenotes.app.domain.repository.TagRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotebookRepositoryImpl @Inject constructor(
    private val dao: NotebookDao,
    private val ioDispatcher: CoroutineDispatcher,
) : NotebookRepository {

    override fun observeAll(): Flow<List<Notebook>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }.flowOn(ioDispatcher)

    override suspend fun upsert(notebook: Notebook): String = withContext(ioDispatcher) {
        val id = notebook.id.ifBlank { UUID.randomUUID().toString() }
        dao.upsert(NotebookEntity.fromDomain(notebook.copy(id = id)))
        id
    }

    override suspend fun delete(id: String) = withContext(ioDispatcher) {
        dao.detachNotes(id)
        dao.delete(id)
    }

    override suspend fun reorder(ids: List<String>) = withContext(ioDispatcher) {
        ids.forEachIndexed { index, id -> dao.setOrder(id, index) }
    }

    override suspend fun setHidden(id: String, hidden: Boolean) = withContext(ioDispatcher) {
        dao.setHidden(id, hidden)
    }
}

@Singleton
class TagRepositoryImpl @Inject constructor(
    private val dao: TagDao,
    private val ioDispatcher: CoroutineDispatcher,
) : TagRepository {

    override fun observeAll(): Flow<List<Tag>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }.flowOn(ioDispatcher)

    override suspend fun upsert(tag: Tag): String = withContext(ioDispatcher) {
        val id = tag.id.ifBlank { UUID.randomUUID().toString() }
        dao.upsert(TagEntity.fromDomain(tag.copy(id = id)))
        id
    }

    override suspend fun delete(id: String) = withContext(ioDispatcher) { dao.delete(id) }

    override suspend fun reorder(ids: List<String>) = withContext(ioDispatcher) {
        ids.forEachIndexed { index, id -> dao.setOrder(id, index) }
    }

    override suspend fun setHidden(id: String, hidden: Boolean) = withContext(ioDispatcher) {
        dao.setHidden(id, hidden)
    }

    override suspend fun findOrCreate(name: String): Tag = withContext(ioDispatcher) {
        val clean = name.trim().removePrefix("#").lowercase()
        dao.findByName(clean)?.toDomain() ?: Tag(
            id = UUID.randomUUID().toString(),
            name = clean,
        ).also { dao.upsert(TagEntity.fromDomain(it)) }
    }
}
