@file:OptIn(ExperimentalLayoutApi::class)

package com.securenotes.app.presentation.navigation

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securenotes.app.domain.model.NoteColor
import com.securenotes.app.domain.model.NoteFilter
import com.securenotes.app.domain.model.Notebook
import com.securenotes.app.domain.model.Tag

@Composable
fun AppDrawer(
    notebooks: List<Notebook>,
    tags: List<Tag>,
    onFilter: (NoteFilter) -> Unit,
    onNotebook: (String) -> Unit,
    onTag: (String) -> Unit,
    onColor: (NoteColor) -> Unit,
    onVault: () -> Unit,
) {
    ModalDrawerSheet(
        drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            Spacer(Modifier.height(26.dp))
            Text(
                "SecureNotes 📒",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Text(
                "Private by default 🛡️",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(18.dp))

            DrawerSection("⚡ Shortcuts")
            DrawerRow("⭐  Favourites", Icons.Rounded.Star) { onFilter(NoteFilter.FAVORITES) }
            DrawerRow("📌  Pinned", Icons.Rounded.PushPin) { onFilter(NoteFilter.PINNED) }
            DrawerRow("⏰  Reminders", Icons.Rounded.Notifications) { onFilter(NoteFilter.REMINDERS) }
            DrawerRow("🗝️  Private Vault", Icons.Rounded.Lock) { onVault() }
            DrawerRow("🗄️  Archive", Icons.Rounded.Archive) { onFilter(NoteFilter.ARCHIVE) }
            DrawerRow("🗑️  Trash", Icons.Rounded.Delete) { onFilter(NoteFilter.TRASH) }

            HorizontalDivider(Modifier.padding(vertical = 10.dp))

            DrawerSection("📚 Notebooks")
            if (notebooks.isEmpty()) {
                EmptyHint("No notebooks yet 🌱")
            } else {
                notebooks.filterNot { it.isHidden }.forEach { notebook ->
                    DrawerRowPlain("${notebook.emoji}  ${notebook.name}") { onNotebook(notebook.id) }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 10.dp))

            DrawerSection("🏷️ Tags")
            if (tags.isEmpty()) {
                EmptyHint("No tags yet 🌱")
            } else {
                FlowRow(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tags.filterNot { it.isHidden }.forEach { tag ->
                        AssistChip(
                            onClick = { onTag(tag.id) },
                            label = { Text("#${tag.name}") },
                        )
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 10.dp))

            DrawerSection("🎨 Colours")
            FlowRow(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NoteColor.entries.filter { it != NoteColor.NONE }.forEach { color ->
                    AssistChip(
                        onClick = { onColor(color) },
                        label = { Text("${color.emoji} ${color.label}") },
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 6.dp),
    )
}

@Composable
private fun DrawerRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    NavigationDrawerItem(
        label = { Text(label) },
        icon = { Icon(icon, null) },
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
    )
}

@Composable
private fun DrawerRowPlain(label: String, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label) },
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
    )
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
    )
}
