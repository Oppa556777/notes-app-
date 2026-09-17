@file:OptIn(ExperimentalFoundationApi::class)

package com.securenotes.app.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.securenotes.app.domain.model.NoteColor
import com.securenotes.app.domain.model.NoteWithMeta
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NoteCard(
    item: NoteWithMeta,
    compact: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val note = item.note
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardScale",
    )

    val tint = if (note.color == NoteColor.NONE) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        Color(note.color.argb.toInt()).copy(alpha = 0.16f)
            .compositeOverSurface(MaterialTheme.colorScheme.surfaceContainerLow)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = tint),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, pressedElevation = 4.dp),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            // Colour strip
            if (note.color != NoteColor.NONE) {
                Box(
                    Modifier
                        .width(6.dp)
                        .fillMaxHeight()
                        .background(Color(note.color.argb.toInt())),
                )
            }

            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = note.title.ifBlank { "Untitled note 📝" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    NoteBadges(item)
                }

                if (!compact && note.preview.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = note.preview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // First link domain, shown as a subtle hint.
                note.linkPreviews.firstOrNull { !it.hidden }?.let { link ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "🔗 ${link.domain}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    item.notebook?.let { notebook ->
                        MiniChip("${notebook.emoji} ${notebook.name}")
                    }
                    item.tags.take(2).forEach { tag ->
                        MiniChip("#${tag.name}")
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = note.updatedAt.asRelativeDate(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteBadges(item: NoteWithMeta) {
    val note = item.note
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (note.isPinned) Badge(Icons.Rounded.PushPin)
        if (note.isFavorite) Badge(Icons.Rounded.Star)
        if (note.reminderAt != null) Badge(Icons.Rounded.Notifications)
        if (note.hasAttachments) Badge(Icons.Rounded.AttachFile)
        if (note.isInVault) Badge(Icons.Rounded.Lock)
    }
}

@Composable
private fun Badge(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(15.dp),
        tint = MaterialTheme.colorScheme.primary,
    )
}

@Composable
fun MiniChip(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Blends a translucent tint over an opaque surface colour. */
private fun Color.compositeOverSurface(surface: Color): Color = Color(
    red = red * alpha + surface.red * (1 - alpha),
    green = green * alpha + surface.green * (1 - alpha),
    blue = blue * alpha + surface.blue * (1 - alpha),
    alpha = 1f,
)

fun Long.asRelativeDate(): String {
    val diff = System.currentTimeMillis() - this
    return when {
        diff < 60_000 -> "just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(this))
    }
}
