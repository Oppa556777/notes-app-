@file:OptIn(ExperimentalLayoutApi::class)

package com.securenotes.app.presentation.screens.editor

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.securenotes.app.domain.model.NoteColor
import com.securenotes.app.presentation.components.LinkPreviewCard
import com.securenotes.app.presentation.components.MarkdownView
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onBack: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val notebooks by viewModel.notebooks.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()

    val snackbarHost = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current

    var preview by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<EditorSheet?>(null) }
    var linkDialog by remember { mutableStateOf(false) }

    // "Show preview for this link?" prompt when auto-previews are disabled.
    LaunchedEffect(state.pendingLink) {
        val url = state.pendingLink ?: return@LaunchedEffect
        val result = snackbarHost.showSnackbar(
            message = "Show preview for this link? 🔗",
            actionLabel = "Show",
            withDismissAction = true,
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.fetchPreview(url)
        } else {
            viewModel.dismissPendingLink()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = {
                    BasicTextField(
                        value = state.title,
                        onValueChange = viewModel::onTitleChange,
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { inner ->
                            if (state.title.isEmpty()) {
                                Text(
                                    "Untitled note 📝",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            inner()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.save(); onBack() }) {
                        Icon(Icons.Rounded.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { preview = !preview }) {
                        Icon(
                            if (preview) Icons.Rounded.Edit else Icons.Rounded.Visibility,
                            contentDescription = if (preview) "Edit" else "Preview",
                        )
                    }
                    IconButton(onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.togglePin()
                    }) {
                        Icon(
                            Icons.Rounded.PushPin,
                            "Pin",
                            tint = if (state.isPinned) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Rounded.MoreVert, "More")
                        }
                        EditorMenu(
                            expanded = menuOpen,
                            isFavorite = state.isFavorite,
                            isInVault = state.isInVault,
                            onDismiss = { menuOpen = false },
                            onSheet = { sheet = it; menuOpen = false },
                            onFavorite = { viewModel.toggleFavorite(); menuOpen = false },
                            onVault = { viewModel.toggleVault(); menuOpen = false },
                            onDelete = {
                                menuOpen = false
                                viewModel.delete(onBack)
                            },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        bottomBar = {
            if (!preview) {
                FormatToolbar(
                    onFormat = viewModel::applyFormat,
                    onLink = { linkDialog = true },
                )
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            // Chips summarising organisation
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                notebooks.firstOrNull { it.id == state.notebookId }?.let {
                    AssistChip(
                        onClick = { sheet = EditorSheet.NOTEBOOK },
                        label = { Text("${it.emoji} ${it.name}") },
                    )
                }
                tags.filter { it.id in state.tagIds }.forEach {
                    AssistChip(onClick = { sheet = EditorSheet.TAGS }, label = { Text("#${it.name}") })
                }
                if (state.color != NoteColor.NONE) {
                    AssistChip(
                        onClick = { sheet = EditorSheet.COLOR },
                        label = { Text("${state.color.emoji} ${state.color.label}") },
                    )
                }
                if (state.reminderAt != null) {
                    AssistChip(
                        onClick = { sheet = EditorSheet.REMINDER },
                        label = { Text("⏰ Reminder set") },
                    )
                }
            }

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                if (preview) {
                    MarkdownView(
                        markdown = state.content,
                        onToggleCheckbox = viewModel::toggleChecklistLine,
                    )
                } else {
                    BasicTextField(
                        value = state.content,
                        onValueChange = viewModel::onContentChange,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp),
                        decorationBox = { inner ->
                            if (state.content.isEmpty()) {
                                Text(
                                    "Start writing… ✍️\n\nTip: use the toolbar for checklists ☑️, code 💻, tables 📊 and math ∑",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            inner()
                        },
                    )
                }

                // Link preview cards
                val visiblePreviews = state.linkPreviews.filterNot { it.hidden }
                if (visiblePreviews.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "🔗 Links",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    visiblePreviews.forEach { link ->
                        LinkPreviewCard(
                            preview = link,
                            modifier = Modifier.padding(bottom = 10.dp),
                            onHide = { viewModel.hidePreview(link.url) },
                            onRefresh = { viewModel.fetchPreview(link.url, forceRefresh = true) },
                        )
                    }
                }

                Spacer(Modifier.height(90.dp))
            }
        }
    }

    // ------------------------------------------------------------- sheets ---

    sheet?.let { current ->
        ModalBottomSheet(
            onDismissRequest = { sheet = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            when (current) {
                EditorSheet.NOTEBOOK -> NotebookPicker(
                    notebooks = notebooks,
                    selectedId = state.notebookId,
                    onSelect = { viewModel.setNotebook(it); sheet = null },
                )

                EditorSheet.TAGS -> TagPicker(
                    tags = tags,
                    selectedIds = state.tagIds,
                    onToggle = viewModel::toggleTag,
                    onCreate = viewModel::createAndAddTag,
                )

                EditorSheet.COLOR -> ColorPicker(
                    selected = state.color,
                    onSelect = { viewModel.setColor(it); sheet = null },
                )

                EditorSheet.REMINDER -> ReminderPicker(
                    hasReminder = state.reminderAt != null,
                    onPick = { viewModel.setReminder(it); sheet = null },
                )
            }
            Spacer(Modifier.height(28.dp))
        }
    }

    if (linkDialog) {
        LinkDialog(
            onDismiss = { linkDialog = false },
            onInsert = { url, label, showPreview ->
                viewModel.insertLink(url, label, showPreview)
                linkDialog = false
            },
        )
    }
}

enum class EditorSheet { NOTEBOOK, TAGS, COLOR, REMINDER }

@Composable
private fun EditorMenu(
    expanded: Boolean,
    isFavorite: Boolean,
    isInVault: Boolean,
    onDismiss: () -> Unit,
    onSheet: (EditorSheet) -> Unit,
    onFavorite: () -> Unit,
    onVault: () -> Unit,
    onDelete: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("📒 Add to notebook") },
            onClick = { onSheet(EditorSheet.NOTEBOOK) },
        )
        DropdownMenuItem(
            text = { Text("🏷️ Manage tags") },
            onClick = { onSheet(EditorSheet.TAGS) },
        )
        DropdownMenuItem(
            text = { Text("🎨 Assign colour") },
            onClick = { onSheet(EditorSheet.COLOR) },
        )
        DropdownMenuItem(
            text = { Text(if (isFavorite) "⭐ Remove favourite" else "⭐ Add to favourites") },
            leadingIcon = { Icon(Icons.Rounded.Star, null) },
            onClick = onFavorite,
        )
        DropdownMenuItem(
            text = { Text(if (isInVault) "🔓 Remove from Vault" else "🔒 Move to Vault") },
            onClick = onVault,
        )
        DropdownMenuItem(
            text = { Text("⏰ Add reminder") },
            onClick = { onSheet(EditorSheet.REMINDER) },
        )
        DropdownMenuItem(
            text = { Text("🗑️ Delete") },
            leadingIcon = { Icon(Icons.Rounded.Delete, null) },
            onClick = onDelete,
        )
    }
}

/** Scrollable rounded icon toolbar. */
@Composable
private fun FormatToolbar(
    onFormat: (EditorFormat) -> Unit,
    onLink: () -> Unit,
) {
    val actions = listOf<Triple<String, String, () -> Unit>>(
        Triple("H", "Heading") { onFormat(EditorFormat.HEADING) },
        Triple("B", "Bold") { onFormat(EditorFormat.BOLD) },
        Triple("I", "Italic") { onFormat(EditorFormat.ITALIC) },
        Triple("U", "Underline") { onFormat(EditorFormat.UNDERLINE) },
        Triple("•", "Bullet list") { onFormat(EditorFormat.BULLET) },
        Triple("1.", "Numbered list") { onFormat(EditorFormat.NUMBERED) },
        Triple("☑️", "Checklist") { onFormat(EditorFormat.CHECKLIST) },
        Triple("💻", "Code block") { onFormat(EditorFormat.CODE) },
        Triple("📊", "Table") { onFormat(EditorFormat.TABLE) },
        Triple("∑", "Math") { onFormat(EditorFormat.MATH) },
        Triple("❝", "Quote") { onFormat(EditorFormat.QUOTE) },
        Triple("➖", "Divider") { onFormat(EditorFormat.DIVIDER) },
        Triple("🔗", "Link", onLink),
    )

    Row(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        actions.forEach { (label, description, action) ->
            IconButton(onClick = action, modifier = Modifier.size(44.dp)) {
                Text(
                    text = label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun NotebookPicker(
    notebooks: List<com.securenotes.app.domain.model.Notebook>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text("📒 Add to notebook", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        FilterChip(
            selected = selectedId == null,
            onClick = { onSelect(null) },
            label = { Text("No notebook") },
        )
        Spacer(Modifier.height(8.dp))
        notebooks.forEach { notebook ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${notebook.emoji}  ${notebook.name}", Modifier.weight(1f))
                if (notebook.id == selectedId) Icon(Icons.Rounded.Check, null)
                TextButton(onClick = { onSelect(notebook.id) }) { Text("Select") }
            }
        }
        if (notebooks.isEmpty()) {
            Text(
                "No notebooks yet 🌱 Create one from the Notebooks tab.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TagPicker(
    tags: List<com.securenotes.app.domain.model.Tag>,
    selectedIds: List<String>,
    onToggle: (String) -> Unit,
    onCreate: (String) -> Unit,
) {
    var newTag by remember { mutableStateOf("") }

    Column(Modifier.padding(horizontal = 20.dp)) {
        Text("🏷️ Manage tags", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tags.forEach { tag ->
                FilterChip(
                    selected = tag.id in selectedIds,
                    onClick = { onToggle(tag.id) },
                    label = { Text("#${tag.name}") },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newTag,
                onValueChange = { newTag = it },
                label = { Text("New tag") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.size(8.dp))
            Button(
                onClick = {
                    if (newTag.isNotBlank()) {
                        onCreate(newTag)
                        newTag = ""
                    }
                },
            ) { Text("Add") }
        }
    }
}

@Composable
private fun ColorPicker(selected: NoteColor, onSelect: (NoteColor) -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text("🎨 Assign colour", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NoteColor.entries.forEach { color ->
                FilterChip(
                    selected = color == selected,
                    onClick = { onSelect(color) },
                    label = { Text("${color.emoji} ${color.label}") },
                )
            }
        }
    }
}

@Composable
private fun ReminderPicker(hasReminder: Boolean, onPick: (Long?) -> Unit) {
    val options = listOf(
        "⏰ In 1 hour" to 3_600_000L,
        "🌇 This evening (18:00)" to millisUntilHour(18),
        "🌅 Tomorrow morning (09:00)" to millisUntilHour(9, tomorrow = true),
        "📅 Next week" to 7 * 86_400_000L,
    )

    Column(Modifier.padding(horizontal = 20.dp)) {
        Text("⏰ Add reminder", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        options.forEach { (label, offset) ->
            TextButton(
                onClick = { onPick(System.currentTimeMillis() + offset) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(label, Modifier.fillMaxWidth())
            }
        }
        if (hasReminder) {
            TextButton(onClick = { onPick(null) }, modifier = Modifier.fillMaxWidth()) {
                Text("🚫 Remove reminder", Modifier.fillMaxWidth())
            }
        }
    }
}

private fun millisUntilHour(hour: Int, tomorrow: Boolean = false): Long {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        if (tomorrow || before(now)) add(Calendar.DAY_OF_YEAR, 1)
    }
    return (target.timeInMillis - now.timeInMillis).coerceAtLeast(60_000L)
}

@Composable
private fun LinkDialog(
    onDismiss: () -> Unit,
    onInsert: (String, String, Boolean) -> Unit,
) {
    var url by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var showPreview by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Link, null) },
        title = { Text("🔗 Insert link") },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    placeholder = { Text("https://example.com") },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label (optional)") },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = showPreview, onCheckedChange = { showPreview = it })
                    Text("Show preview card")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (url.isNotBlank()) onInsert(url.trim(), label.trim(), showPreview) },
            ) { Text("Insert") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
