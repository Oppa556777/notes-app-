@file:OptIn(ExperimentalLayoutApi::class)

package com.securenotes.app.presentation.screens.notebooks

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.securenotes.app.presentation.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebooksScreen(
    onOpenNotebook: (String) -> Unit,
    viewModel: OrganizationViewModel = hiltViewModel(),
) {
    val notebooks by viewModel.notebooks.collectAsStateWithLifecycle()
    val counts by viewModel.notebookCounts.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Notebooks 📚",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                modifier = Modifier.padding(bottom = 92.dp),
            ) { Icon(Icons.Rounded.Add, "New notebook") }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            if (notebooks.isEmpty()) {
                EmptyState(
                    emoji = "📚",
                    title = "No notebooks yet 📚",
                    subtitle = "Group related notes into notebooks to keep things tidy.",
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 150.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(notebooks, key = { it.id }) { notebook ->
                    Card(
                        onClick = { onOpenNotebook(notebook.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(notebook.emoji, fontSize = 30.sp)
                            Spacer(Modifier.padding(horizontal = 8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    notebook.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "${counts[notebook.id] ?: 0} notes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { viewModel.deleteNotebook(notebook.id) }) {
                                Icon(Icons.Rounded.Delete, "Delete")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        CreateNotebookDialog(
            onDismiss = { showDialog = false },
            onCreate = { name, emoji ->
                viewModel.createNotebook(name, emoji)
                showDialog = false
            },
        )
    }
}

@Composable
private fun CreateNotebookDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("📒") }
    val emojiOptions = listOf("📒", "📕", "📗", "📘", "📙", "🗂️", "💡", "💼", "🎓", "🏡", "✈️", "🍳")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📒 New notebook") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                )
                Spacer(Modifier.height(14.dp))
                Text("Pick an icon", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    emojiOptions.forEach { option ->
                        TextButton(onClick = { emoji = option }) {
                            Text(
                                option,
                                fontSize = if (option == emoji) 26.sp else 20.sp,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name.trim(), emoji) },
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
