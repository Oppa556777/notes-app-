package com.securenotes.app.presentation.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.securenotes.app.presentation.components.EmptyState
import com.securenotes.app.presentation.components.NoteCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onOpenNote: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = query,
                    onQueryChange = viewModel::onQueryChange,
                    onSearch = {},
                    expanded = false,
                    onExpandedChange = {},
                    placeholder = { Text("Search notes, tags, notebooks 🔍") },
                    leadingIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Rounded.ArrowBack, "Back")
                        }
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onQueryChange("") }) {
                                Icon(Icons.Rounded.Close, "Clear")
                            }
                        } else {
                            Icon(Icons.Rounded.Search, null)
                        }
                    },
                )
            },
            expanded = false,
            onExpandedChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        ) {}

        Box(Modifier.fillMaxSize()) {
            when {
                query.isBlank() -> EmptyState(
                    emoji = "🔍",
                    title = "Search everything",
                    subtitle = "Find any note by its title or content — all searched locally on your device.",
                )

                results.isEmpty() -> EmptyState(
                    emoji = "🫥",
                    title = "No matches",
                    subtitle = "Nothing found for “$query”. Try a different word.",
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 150.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(results, key = { it.note.id }) { item ->
                        NoteCard(
                            item = item,
                            compact = false,
                            onClick = { onOpenNote(item.note.id) },
                        )
                    }
                }
            }
        }
    }
}
