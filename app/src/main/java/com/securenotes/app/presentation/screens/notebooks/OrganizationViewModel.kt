package com.securenotes.app.presentation.screens.notebooks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securenotes.app.domain.model.Notebook
import com.securenotes.app.domain.model.Tag
import com.securenotes.app.domain.repository.NoteRepository
import com.securenotes.app.domain.repository.NotebookRepository
import com.securenotes.app.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/** Shared by the Notebooks and Tags tabs plus the navigation drawer. */
@HiltViewModel
class OrganizationViewModel @Inject constructor(
    private val notebookRepository: NotebookRepository,
    private val tagRepository: TagRepository,
    private val noteRepository: NoteRepository,
) : ViewModel() {

    private val _notebookCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val notebookCounts: StateFlow<Map<String, Int>> = _notebookCounts.asStateFlow()

    val notebooks: StateFlow<List<Notebook>> = notebookRepository.observeAll()
        .onEach { list -> refreshCounts(list) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tags: StateFlow<List<Tag>> = tagRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun refreshCounts(list: List<Notebook>) = viewModelScope.launch {
        _notebookCounts.value = list.associate { it.id to noteRepository.countInNotebook(it.id) }
    }

    fun createNotebook(name: String, emoji: String) = viewModelScope.launch {
        notebookRepository.upsert(
            Notebook(id = UUID.randomUUID().toString(), name = name, emoji = emoji),
        )
    }

    fun deleteNotebook(id: String) = viewModelScope.launch { notebookRepository.delete(id) }

    fun reorderNotebooks(ids: List<String>) = viewModelScope.launch {
        notebookRepository.reorder(ids)
    }

    fun setNotebookHidden(id: String, hidden: Boolean) = viewModelScope.launch {
        notebookRepository.setHidden(id, hidden)
    }

    fun createTag(name: String, emoji: String) = viewModelScope.launch {
        tagRepository.upsert(
            Tag(id = UUID.randomUUID().toString(), name = name.trim().removePrefix("#"), emoji = emoji),
        )
    }

    fun deleteTag(id: String) = viewModelScope.launch { tagRepository.delete(id) }

    fun setTagHidden(id: String, hidden: Boolean) = viewModelScope.launch {
        tagRepository.setHidden(id, hidden)
    }
}
