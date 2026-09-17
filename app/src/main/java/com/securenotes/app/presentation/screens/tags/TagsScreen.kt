@file:OptIn(ExperimentalLayoutApi::class)

package com.securenotes.app.presentation.screens.tags

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.securenotes.app.presentation.components.EmptyState
import com.securenotes.app.presentation.screens.notebooks.OrganizationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(
    onOpenTag: (String) -> Unit,
    viewModel: OrganizationViewModel = hiltViewModel(),
) {
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Tags 🏷️",
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
            ) { Icon(Icons.Rounded.Add, "New tag") }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            if (tags.isEmpty()) {
                EmptyState(
                    emoji = "🏷️",
                    title = "No tags yet 🏷️",
                    subtitle = "Tags are a flexible way to cross-link notes across notebooks.",
                )
            } else {
                FlowRow(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(PaddingValues(16.dp, 4.dp, 16.dp, 150.dp)),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tags.forEach { tag ->
                        InputChip(
                            selected = false,
                            onClick = { onOpenTag(tag.id) },
                            label = { Text("${tag.emoji} ${tag.name}") },
                            trailingIcon = {
                                Icon(
                                    Icons.Rounded.Close,
                                    "Delete tag",
                                    modifier = Modifier.padding(2.dp),
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("🏷️ New tag") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Tag name") },
                        singleLine = true,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Tags are lowercase and unique.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) viewModel.createTag(name, "🏷️")
                    showDialog = false
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } },
        )
    }
}
