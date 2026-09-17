package com.securenotes.app.presentation.screens.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securenotes.app.core.reminder.ReminderScheduler
import com.securenotes.app.data.link.LinkDetector
import com.securenotes.app.data.preferences.SettingsRepository
import com.securenotes.app.domain.model.LinkPreview
import com.securenotes.app.domain.model.Note
import com.securenotes.app.domain.model.NoteColor
import com.securenotes.app.domain.model.Notebook
import com.securenotes.app.domain.model.Tag
import com.securenotes.app.domain.repository.LinkPreviewRepository
import com.securenotes.app.domain.repository.NoteRepository
import com.securenotes.app.domain.repository.NotebookRepository
import com.securenotes.app.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class EditorUiState(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val notebookId: String? = null,
    val tagIds: List<String> = emptyList(),
    val color: NoteColor = NoteColor.NONE,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isInVault: Boolean = false,
    val reminderAt: Long? = null,
    val linkPreviews: List<LinkPreview> = emptyList(),
    val pendingLink: String? = null,
    val autoPreviews: Boolean = true,
    val saving: Boolean = false,
    val loaded: Boolean = false,
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val linkPreviewRepository: LinkPreviewRepository,
    private val tagRepository: TagRepository,
    notebookRepository: NotebookRepository,
    private val settingsRepository: SettingsRepository,
    private val reminderScheduler: ReminderScheduler,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val noteId: String = savedStateHandle.get<String>("noteId").orEmpty()
    private val startInVault: Boolean = savedStateHandle.get<String>("vault") == "true"

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    val notebooks: StateFlow<List<Notebook>> = notebookRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tags: StateFlow<List<Tag>> = tagRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var autosaveJob: Job? = null

    init {
        viewModelScope.launch {
            val auto = settingsRepository.settings.first().autoLinkPreviews

            if (noteId.isNotBlank() && noteId != "new") {
                noteRepository.getNote(noteId)?.let { note ->
                    _state.value = EditorUiState(
                        id = note.id,
                        title = note.title,
                        content = note.content,
                        notebookId = note.notebookId,
                        tagIds = note.tags,
                        color = note.color,
                        isPinned = note.isPinned,
                        isFavorite = note.isFavorite,
                        isInVault = note.isInVault,
                        reminderAt = note.reminderAt,
                        linkPreviews = note.linkPreviews,
                        autoPreviews = auto,
                        loaded = true,
                    )
                }
            } else {
                _state.value = EditorUiState(
                    id = UUID.randomUUID().toString(),
                    isInVault = startInVault,
                    autoPreviews = auto,
                    loaded = true,
                )
            }
        }
    }

    // ------------------------------------------------------------ editing ---

    fun onTitleChange(value: String) {
        _state.value = _state.value.copy(title = value)
        scheduleAutosave()
    }

    fun onContentChange(value: String) {
        val previous = _state.value.content
        _state.value = _state.value.copy(content = value)
        detectNewLinks(previous, value)
        scheduleAutosave()
    }

    /** Debounced autosave so typing never blocks on disk I/O. */
    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(600)
            save()
        }
    }

    fun save() = viewModelScope.launch {
        val s = _state.value
        if (!s.loaded) return@launch
        if (s.title.isBlank() && s.content.isBlank()) return@launch

        _state.value = s.copy(saving = true)
        noteRepository.upsert(
            Note(
                id = s.id,
                title = s.title,
                content = s.content,
                notebookId = s.notebookId,
                tags = s.tagIds,
                color = s.color,
                isPinned = s.isPinned,
                isFavorite = s.isFavorite,
                isInVault = s.isInVault,
                reminderAt = s.reminderAt,
                linkPreviews = s.linkPreviews,
            ),
        )
        _state.value = _state.value.copy(saving = false)
    }

    // -------------------------------------------------------------- links ---

    private fun detectNewLinks(previous: String, current: String) {
        val before = LinkDetector.findUrls(previous).toSet()
        val after = LinkDetector.findUrls(current)
        val fresh = after.filterNot { it in before }
        val known = _state.value.linkPreviews.map { it.url }.toSet()
        val candidate = fresh.firstOrNull { it !in known } ?: return

        if (_state.value.autoPreviews) {
            fetchPreview(candidate)
        } else {
            _state.value = _state.value.copy(pendingLink = candidate)
        }
    }

    fun fetchPreview(url: String, forceRefresh: Boolean = false) = viewModelScope.launch {
        _state.value = _state.value.copy(pendingLink = null)
        linkPreviewRepository.fetch(url, forceRefresh).onSuccess { preview ->
            val existing = _state.value.linkPreviews.filterNot { it.url == preview.url }
            _state.value = _state.value.copy(linkPreviews = existing + preview)
            save()
        }
    }

    fun dismissPendingLink() {
        _state.value = _state.value.copy(pendingLink = null)
    }

    fun hidePreview(url: String) {
        _state.value = _state.value.copy(
            linkPreviews = _state.value.linkPreviews.map {
                if (it.url == url) it.copy(hidden = true) else it
            },
        )
        save()
    }

    fun insertLink(url: String, label: String, showPreview: Boolean) {
        val markdown = if (label.isBlank()) url else "[$label]($url)"
        onContentChange(_state.value.content + markdown)
        if (showPreview) fetchPreview(url)
    }

    // ------------------------------------------------------- organisation ---

    fun setNotebook(id: String?) {
        _state.value = _state.value.copy(notebookId = id)
        save()
    }

    fun toggleTag(tagId: String) {
        val current = _state.value.tagIds
        _state.value = _state.value.copy(
            tagIds = if (tagId in current) current - tagId else current + tagId,
        )
        save()
    }

    fun createAndAddTag(name: String) = viewModelScope.launch {
        val tag = tagRepository.findOrCreate(name)
        if (tag.id !in _state.value.tagIds) {
            _state.value = _state.value.copy(tagIds = _state.value.tagIds + tag.id)
            save()
        }
    }

    fun setColor(color: NoteColor) {
        _state.value = _state.value.copy(color = color)
        save()
    }

    fun togglePin() {
        _state.value = _state.value.copy(isPinned = !_state.value.isPinned)
        save()
    }

    fun toggleFavorite() {
        _state.value = _state.value.copy(isFavorite = !_state.value.isFavorite)
        save()
    }

    fun toggleVault() {
        _state.value = _state.value.copy(isInVault = !_state.value.isInVault)
        save()
    }

    fun setReminder(at: Long?) {
        _state.value = _state.value.copy(reminderAt = at)
        val s = _state.value
        if (at != null) {
            reminderScheduler.schedule(s.id, s.title.ifBlank { "Note reminder" }, at)
        } else {
            reminderScheduler.cancel(s.id)
        }
        save()
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        noteRepository.trash(_state.value.id)
        onDone()
    }

    // --------------------------------------------------- markdown helpers ---

    /** Inserts/toggles a markdown construct at the end of the current content. */
    fun applyFormat(format: EditorFormat) {
        val content = _state.value.content
        val needsNewline = content.isNotEmpty() && !content.endsWith("\n")
        val prefix = if (needsNewline) "\n" else ""
        val snippet = when (format) {
            EditorFormat.HEADING -> "$prefix## Heading\n"
            EditorFormat.BOLD -> "**bold**"
            EditorFormat.ITALIC -> "*italic*"
            EditorFormat.UNDERLINE -> "__underline__"
            EditorFormat.BULLET -> "$prefix- Item\n"
            EditorFormat.NUMBERED -> "$prefix1. Item\n"
            EditorFormat.CHECKLIST -> "$prefix- [ ] To do\n"
            EditorFormat.CODE -> "$prefix```kotlin\nval hello = \"world\"\n```\n"
            EditorFormat.TABLE -> "$prefix| Column A | Column B |\n| --- | --- |\n| Value 1 | Value 2 |\n"
            EditorFormat.MATH -> "$prefix\$\$ E = mc^2 \$\$\n"
            EditorFormat.QUOTE -> "$prefix> Quote\n"
            EditorFormat.DIVIDER -> "$prefix\n---\n"
        }
        onContentChange(content + snippet)
    }

    /** Toggles the checkbox on the given line index of the markdown body. */
    fun toggleChecklistLine(lineIndex: Int) {
        val lines = _state.value.content.lines().toMutableList()
        if (lineIndex !in lines.indices) return
        val line = lines[lineIndex]
        lines[lineIndex] = when {
            line.trimStart().startsWith("- [ ]") -> line.replaceFirst("- [ ]", "- [x]")
            line.trimStart().startsWith("- [x]") -> line.replaceFirst("- [x]", "- [ ]")
            else -> line
        }
        onContentChange(lines.joinToString("\n"))
    }
}

enum class EditorFormat {
    HEADING, BOLD, ITALIC, UNDERLINE, BULLET, NUMBERED, CHECKLIST,
    CODE, TABLE, MATH, QUOTE, DIVIDER,
}
