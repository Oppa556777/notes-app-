package com.securenotes.app.presentation.screens.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securenotes.app.data.preferences.SettingsRepository
import com.securenotes.app.domain.model.NoteFilter
import com.securenotes.app.domain.model.NoteSort
import com.securenotes.app.domain.model.NoteWithMeta
import com.securenotes.app.domain.model.Notebook
import com.securenotes.app.domain.model.Tag
import com.securenotes.app.domain.repository.NoteRepository
import com.securenotes.app.domain.repository.NotebookRepository
import com.securenotes.app.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotesUiState(
    val notes: List<NoteWithMeta> = emptyList(),
    val notebooks: List<Notebook> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val filter: NoteFilter = NoteFilter.ALL,
    val sort: NoteSort = NoteSort.UPDATED_DESC,
    val selectedNotebookId: String? = null,
    val selectedTagId: String? = null,
    val compactList: Boolean = false,
    val loading: Boolean = true,
)

data class NotesQuery(
    val filter: NoteFilter = NoteFilter.ALL,
    val notebookId: String? = null,
    val tagId: String? = null,
    val sort: NoteSort = NoteSort.UPDATED_DESC,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotesViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    notebookRepository: NotebookRepository,
    tagRepository: TagRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val query = MutableStateFlow(NotesQuery())

    private val notesFlow = query.flatMapLatest { q ->
        noteRepository.observeNotes(q.filter, q.notebookId, q.tagId, q.sort)
    }

    val uiState: StateFlow<NotesUiState> = combine(
        notesFlow,
        notebookRepository.observeAll(),
        tagRepository.observeAll(),
        query,
        settingsRepository.settings,
    ) { notes, notebooks, tags, q, settings ->
        NotesUiState(
            notes = notes,
            notebooks = notebooks,
            tags = tags,
            filter = q.filter,
            sort = q.sort,
            selectedNotebookId = q.notebookId,
            selectedTagId = q.tagId,
            compactList = settings.compactList,
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotesUiState())

    fun setFilter(filter: NoteFilter) {
        query.value = query.value.copy(filter = filter, notebookId = null, tagId = null)
    }

    fun setNotebook(id: String?) {
        query.value = query.value.copy(notebookId = id, tagId = null, filter = NoteFilter.ALL)
    }

    fun setTag(id: String?) {
        query.value = query.value.copy(tagId = id, notebookId = null, filter = NoteFilter.ALL)
    }

    fun setSort(sort: NoteSort) {
        query.value = query.value.copy(sort = sort)
    }

    fun togglePin(id: String, pinned: Boolean) = viewModelScope.launch {
        noteRepository.setPinned(id, pinned)
    }

    fun toggleFavorite(id: String, favorite: Boolean) = viewModelScope.launch {
        noteRepository.setFavorite(id, favorite)
    }

    fun trash(id: String) = viewModelScope.launch { noteRepository.trash(id) }

    fun restore(id: String) = viewModelScope.launch { noteRepository.restore(id) }

    fun deleteForever(id: String) = viewModelScope.launch { noteRepository.deleteForever(id) }

    fun emptyTrash() = viewModelScope.launch { noteRepository.emptyTrash() }

    fun moveToVault(id: String, inVault: Boolean) = viewModelScope.launch {
        noteRepository.moveToVault(id, inVault)
    }
}
