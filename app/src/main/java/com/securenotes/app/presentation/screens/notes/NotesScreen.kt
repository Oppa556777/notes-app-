package com.securenotes.app.presentation.screens.notes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.securenotes.app.domain.model.NoteFilter
import com.securenotes.app.domain.model.NoteSort
import com.securenotes.app.presentation.components.EmptyState
import com.securenotes.app.presentation.components.NoteCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    title: String = "SecureNotes 📒",
    vaultMode: Boolean = false,
    onOpenNote: (String) -> Unit,
    onNewNote: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenDrawer: () -> Unit,
    viewModel: NotesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Vault mode reuses this screen but scopes the query to vault notes only.
    androidx.compose.runtime.LaunchedEffect(vaultMode) {
        viewModel.setFilter(if (vaultMode) NoteFilter.VAULT else NoteFilter.ALL)
    }
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val listState = rememberLazyListState()

    var sortMenu by remember { mutableStateOf(false) }

    val fabExpanded by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) { Icon(Icons.Rounded.Menu, "Menu") }
                },
                actions = {
                    IconButton(onClick = onOpenSearch) { Icon(Icons.Rounded.Search, "Search") }
                    Box {
                        IconButton(onClick = { sortMenu = true }) {
                            Icon(Icons.Rounded.Sort, "Sort")
                        }
                        DropdownMenu(sortMenu, onDismissRequest = { sortMenu = false }) {
                            NoteSort.entries.forEach { sort ->
                                DropdownMenuItem(
                                    text = { Text(sort.label) },
                                    onClick = { viewModel.setSort(sort); sortMenu = false },
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNewNote()
                },
                expanded = fabExpanded,
                icon = { Icon(Icons.Rounded.Edit, null) },
                text = { Text("New note") },
                modifier = Modifier.padding(bottom = 92.dp),
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            if (state.notes.isEmpty() && !state.loading) {
                EmptyState(
                    emoji = if (vaultMode) "🗝️" else "🌱",
                    title = if (vaultMode) "Your vault is empty 🗝️" else "No notes yet 🌱",
                    subtitle = if (vaultMode) {
                        "Move a sensitive note here from its editor menu to keep it double-locked."
                    } else {
                        "Tap “New note” to capture your first encrypted thought."
                    },
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 4.dp,
                    bottom = 150.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    FilterChipsRow(
                        selected = state.filter,
                        onSelect = viewModel::setFilter,
                        vaultMode = vaultMode,
                    )
                }

                items(state.notes, key = { it.note.id }) { item ->
                    NoteCard(
                        item = item,
                        compact = state.compactList,
                        onClick = { onOpenNote(item.note.id) },
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            val note = item.note
                            viewModel.togglePin(note.id, !note.isPinned)
                            scope.launch {
                                snackbarHost.showSnackbar(
                                    if (!note.isPinned) "Pinned 📌" else "Unpinned",
                                )
                            }
                        },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChipsRow(
    selected: NoteFilter,
    onSelect: (NoteFilter) -> Unit,
    vaultMode: Boolean,
) {
    val filters = if (vaultMode) {
        listOf(NoteFilter.VAULT to "🔒 Vault")
    } else {
        listOf(
            NoteFilter.ALL to "📒 All",
            NoteFilter.FAVORITES to "⭐ Favourites",
            NoteFilter.PINNED to "📌 Pinned",
            NoteFilter.REMINDERS to "⏰ Reminders",
            NoteFilter.ARCHIVE to "🗄️ Archive",
        )
    }

    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        filters.forEach { (filter, label) ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                label = { Text(label) },
            )
        }
    }
}
