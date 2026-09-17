@file:OptIn(ExperimentalFoundationApi::class)

package com.securenotes.app.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.securenotes.app.domain.model.LinkPreview

/**
 * Compact, extra-rounded card rendering the metadata of a link found in a note.
 * Tap opens a Chrome Custom Tab; long-press exposes copy / hide / share.
 */
@Composable
fun LinkPreviewCard(
    preview: LinkPreview,
    modifier: Modifier = Modifier,
    onHide: () -> Unit = {},
    onRefresh: () -> Unit = {},
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }

    Box(modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { openLink(context, preview.url) },
                    onLongClick = { menuOpen = true },
                ),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column {
                preview.imageUrl?.let { image ->
                    AsyncImage(
                        model = image,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(132.dp),
                    )
                }

                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (preview.faviconUrl != null) {
                        AsyncImage(
                            model = preview.faviconUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                    } else {
                        Icon(
                            Icons.Rounded.Link,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Spacer(Modifier.size(10.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            text = preview.title ?: preview.url,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        preview.description?.let { description ->
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            text = "🔗 ${preview.domain}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("Copy link") },
                leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                onClick = {
                    copyToClipboard(context, preview.url)
                    menuOpen = false
                },
            )
            DropdownMenuItem(
                text = { Text("Refresh preview") },
                leadingIcon = { Icon(Icons.Rounded.Refresh, null) },
                onClick = { onRefresh(); menuOpen = false },
            )
            DropdownMenuItem(
                text = { Text("Remove preview") },
                leadingIcon = { Icon(Icons.Rounded.VisibilityOff, null) },
                onClick = { onHide(); menuOpen = false },
            )
            DropdownMenuItem(
                text = { Text("Share") },
                leadingIcon = { Icon(Icons.Rounded.Share, null) },
                onClick = { shareLink(context, preview.url); menuOpen = false },
            )
        }
    }
}

fun openLink(context: Context, url: String) {
    runCatching {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setUrlBarHidingEnabled(true)
            .build()
            .launchUrl(context, Uri.parse(url))
    }.onFailure {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }
}

fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
        as? android.content.ClipboardManager
    clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("SecureNotes", text))
}

fun shareLink(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Share link")) }
}
