package com.securenotes.app.domain.model

/** A colour shortcut that can be assigned to a note. */
enum class NoteColor(val emoji: String, val label: String, val argb: Long) {
    NONE("⚪", "None", 0x00000000),
    RED("🔴", "Red", 0xFFE57373),
    ORANGE("🟠", "Orange", 0xFFFFB74D),
    YELLOW("🟡", "Yellow", 0xFFFFD54F),
    GREEN("🟢", "Green", 0xFF81C784),
    TEAL("🩵", "Teal", 0xFF4DB6AC),
    BLUE("🔵", "Blue", 0xFF64B5F6),
    PURPLE("🟣", "Purple", 0xFFBA68C8),
    PINK("🌸", "Pink", 0xFFF06292);

    companion object {
        fun fromName(value: String?): NoteColor =
            entries.firstOrNull { it.name == value } ?: NONE
    }
}

/** Preview metadata for a hyperlink found inside a note. */
data class LinkPreview(
    val url: String,
    val title: String? = null,
    val description: String? = null,
    val faviconUrl: String? = null,
    val imageUrl: String? = null,
    val domain: String = "",
    val fetchedAt: Long = 0L,
    val hidden: Boolean = false,
)

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val notebookId: String? = null,
    val tags: List<String> = emptyList(),
    val color: NoteColor = NoteColor.NONE,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isInVault: Boolean = false,
    val isTrashed: Boolean = false,
    val isArchived: Boolean = false,
    val reminderAt: Long? = null,
    val linkPreviews: List<LinkPreview> = emptyList(),
    val attachments: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    /** First non-empty line of the body, used for list previews. */
    val preview: String
        get() = content.lineSequence()
            .map { it.trim().removePrefix("#").removePrefix("- [ ]").removePrefix("- [x]").trim() }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()

    val hasLinks: Boolean get() = linkPreviews.any { !it.hidden }
    val hasAttachments: Boolean get() = attachments.isNotEmpty()
}

data class Notebook(
    val id: String,
    val name: String,
    val emoji: String = "📒",
    val description: String = "",
    val colorArgb: Long? = null,
    val sortOrder: Int = 0,
    val isHidden: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

data class Tag(
    val id: String,
    val name: String,
    val emoji: String = "🏷️",
    val colorArgb: Long? = null,
    val sortOrder: Int = 0,
    val isHidden: Boolean = false,
)

/** Aggregate used by list screens so tag/notebook names resolve in one pass. */
data class NoteWithMeta(
    val note: Note,
    val notebook: Notebook? = null,
    val tags: List<Tag> = emptyList(),
)

enum class NoteSort(val label: String) {
    UPDATED_DESC("Recently edited"),
    CREATED_DESC("Recently created"),
    TITLE_ASC("Title A–Z"),
    COLOR("Colour"),
}

enum class NoteFilter { ALL, FAVORITES, PINNED, REMINDERS, VAULT, TRASH, ARCHIVE }
