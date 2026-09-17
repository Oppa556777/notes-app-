package com.securenotes.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.securenotes.app.domain.model.LinkPreview
import com.securenotes.app.domain.model.Note
import com.securenotes.app.domain.model.NoteColor
import com.securenotes.app.domain.model.Notebook
import com.securenotes.app.domain.model.Tag

@Entity(
    tableName = "notes",
    indices = [
        Index("notebookId"),
        Index("isTrashed"),
        Index("isInVault"),
        Index("updatedAt"),
    ],
)
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val notebookId: String?,
    val color: String,
    val isPinned: Boolean,
    val isFavorite: Boolean,
    val isInVault: Boolean,
    val isTrashed: Boolean,
    val isArchived: Boolean,
    val reminderAt: Long?,
    val linkPreviews: List<LinkPreview>,
    val attachments: List<String>,
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun toDomain(tagIds: List<String> = emptyList()) = Note(
        id = id,
        title = title,
        content = content,
        notebookId = notebookId,
        tags = tagIds,
        color = NoteColor.fromName(color),
        isPinned = isPinned,
        isFavorite = isFavorite,
        isInVault = isInVault,
        isTrashed = isTrashed,
        isArchived = isArchived,
        reminderAt = reminderAt,
        linkPreviews = linkPreviews,
        attachments = attachments,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        fun fromDomain(note: Note) = NoteEntity(
            id = note.id,
            title = note.title,
            content = note.content,
            notebookId = note.notebookId,
            color = note.color.name,
            isPinned = note.isPinned,
            isFavorite = note.isFavorite,
            isInVault = note.isInVault,
            isTrashed = note.isTrashed,
            isArchived = note.isArchived,
            reminderAt = note.reminderAt,
            linkPreviews = note.linkPreviews,
            attachments = note.attachments,
            createdAt = note.createdAt,
            updatedAt = note.updatedAt,
        )
    }
}

@Entity(tableName = "notebooks")
data class NotebookEntity(
    @PrimaryKey val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val colorArgb: Long?,
    val sortOrder: Int,
    val isHidden: Boolean,
    val createdAt: Long,
) {
    fun toDomain() = Notebook(id, name, emoji, description, colorArgb, sortOrder, isHidden, createdAt)

    companion object {
        fun fromDomain(n: Notebook) =
            NotebookEntity(n.id, n.name, n.emoji, n.description, n.colorArgb, n.sortOrder, n.isHidden, n.createdAt)
    }
}

@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val emoji: String,
    val colorArgb: Long?,
    val sortOrder: Int,
    val isHidden: Boolean,
) {
    fun toDomain() = Tag(id, name, emoji, colorArgb, sortOrder, isHidden)

    companion object {
        fun fromDomain(t: Tag) = TagEntity(t.id, t.name, t.emoji, t.colorArgb, t.sortOrder, t.isHidden)
    }
}

/** Join table wiring notes to tags (many-to-many). */
@Entity(
    tableName = "note_tag_cross_ref",
    primaryKeys = ["noteId", "tagId"],
    indices = [Index("tagId")],
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class NoteTagCrossRef(
    val noteId: String,
    val tagId: String,
)

/** Cached link metadata, shared across notes that reference the same URL. */
@Entity(tableName = "link_previews")
data class LinkPreviewEntity(
    @PrimaryKey val url: String,
    val title: String?,
    val description: String?,
    val faviconUrl: String?,
    val imageUrl: String?,
    val domain: String,
    val fetchedAt: Long,
) {
    fun toDomain() = LinkPreview(url, title, description, faviconUrl, imageUrl, domain, fetchedAt)

    companion object {
        fun fromDomain(p: LinkPreview) =
            LinkPreviewEntity(p.url, p.title, p.description, p.faviconUrl, p.imageUrl, p.domain, p.fetchedAt)
    }
}
