package com.securenotes.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * Lightweight markdown renderer covering everything the editor toolbar can
 * produce: headings, bold/italic/underline, bullets, numbered lists,
 * checklists, code blocks, tables, block quotes, math and dividers.
 *
 * Hand-written (rather than a heavyweight library) so rendering stays fast and
 * the APK stays small.
 */
@Composable
fun MarkdownView(
    markdown: String,
    modifier: Modifier = Modifier,
    onToggleCheckbox: (Int) -> Unit = {},
) {
    val lines = markdown.lines()
    Column(modifier) {
        var index = 0
        while (index < lines.size) {
            val line = lines[index]
            val trimmed = line.trim()

            when {
                // Fenced code block
                trimmed.startsWith("```") -> {
                    val language = trimmed.removePrefix("```").trim()
                    val body = StringBuilder()
                    index++
                    while (index < lines.size && !lines[index].trim().startsWith("```")) {
                        body.appendLine(lines[index])
                        index++
                    }
                    CodeBlock(language, body.toString().trimEnd())
                }

                // Table
                trimmed.startsWith("|") -> {
                    val rows = mutableListOf<String>()
                    while (index < lines.size && lines[index].trim().startsWith("|")) {
                        rows.add(lines[index].trim())
                        index++
                    }
                    index--
                    MarkdownTable(rows)
                }

                // Math block
                trimmed.startsWith("$$") -> {
                    MathBlock(trimmed.removeSurrounding("$$").trim())
                }

                trimmed == "---" || trimmed == "***" -> {
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                }

                trimmed.startsWith("- [ ]") || trimmed.startsWith("- [x]") -> {
                    val checked = trimmed.startsWith("- [x]")
                    val text = trimmed.removePrefix("- [ ]").removePrefix("- [x]").trim()
                    val lineIndex = index
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { onToggleCheckbox(lineIndex) },
                        )
                        Text(
                            text = inlineStyled(text),
                            style = MaterialTheme.typography.bodyLarge,
                            textDecoration = if (checked) TextDecoration.LineThrough else null,
                            color = if (checked) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }

                trimmed.startsWith("#") -> {
                    val level = trimmed.takeWhile { it == '#' }.length
                    val text = trimmed.dropWhile { it == '#' }.trim()
                    Text(
                        text = text,
                        style = when (level) {
                            1 -> MaterialTheme.typography.headlineMedium
                            2 -> MaterialTheme.typography.headlineSmall
                            else -> MaterialTheme.typography.titleLarge
                        },
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
                    )
                }

                trimmed.startsWith("> ") -> {
                    Row(Modifier.padding(vertical = 6.dp)) {
                        Box(
                            Modifier
                                .width(4.dp)
                                .height(22.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.primary),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = inlineStyled(trimmed.removePrefix("> ")),
                            style = MaterialTheme.typography.bodyLarge,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    Row(Modifier.padding(vertical = 2.dp)) {
                        Text("•  ", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = inlineStyled(trimmed.drop(2)),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }

                trimmed.matches(Regex("""^\d+\.\s.*""")) -> {
                    Row(Modifier.padding(vertical = 2.dp)) {
                        Text(
                            text = trimmed.substringBefore(" ") + "  ",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = inlineStyled(trimmed.substringAfter(" ")),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }

                trimmed.isBlank() -> Spacer(Modifier.height(8.dp))

                else -> Text(
                    text = inlineStyled(line),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
            index++
        }
    }
}

/** Parses **bold**, *italic*, __underline__, `code` and [label](url). */
@Composable
private fun inlineStyled(text: String) = buildAnnotatedString {
    val primary = MaterialTheme.colorScheme.primary
    val codeBg = MaterialTheme.colorScheme.surfaceContainerHighest

    val pattern = Regex("""(\*\*.+?\*\*)|(__.+?__)|(\*.+?\*)|(`.+?`)|(\[.+?]\(.+?\))""")
    var cursor = 0

    pattern.findAll(text).forEach { match ->
        if (match.range.first > cursor) append(text.substring(cursor, match.range.first))
        val token = match.value
        when {
            token.startsWith("**") -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(token.removeSurrounding("**"))
            }

            token.startsWith("__") -> withStyle(
                SpanStyle(textDecoration = TextDecoration.Underline),
            ) { append(token.removeSurrounding("__")) }

            token.startsWith("`") -> withStyle(
                SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg),
            ) { append(token.removeSurrounding("`")) }

            token.startsWith("[") -> {
                val label = token.substringAfter("[").substringBefore("]")
                withStyle(
                    SpanStyle(color = primary, textDecoration = TextDecoration.Underline),
                ) { append(label) }
            }

            token.startsWith("*") -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                append(token.removeSurrounding("*"))
            }
        }
        cursor = match.range.last + 1
    }
    if (cursor < text.length) append(text.substring(cursor))
}

@Composable
private fun CodeBlock(language: String, code: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(14.dp),
    ) {
        if (language.isNotBlank()) {
            Text(
                text = "💻 $language",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
        }
        Text(
            text = code,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        )
    }
}

@Composable
private fun MathBlock(expression: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "∑  $expression",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
            ),
        )
    }
}

@Composable
private fun MarkdownTable(rows: List<String>) {
    // Drop the |---|---| separator row.
    val cells = rows
        .filterNot { it.replace("|", "").replace("-", "").replace(":", "").isBlank() }
        .map { row -> row.trim('|').split("|").map { it.trim() } }

    if (cells.isEmpty()) return

    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        cells.forEachIndexed { rowIndex, row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(
                        if (rowIndex == 0) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        } else {
                            androidx.compose.ui.graphics.Color.Transparent
                        },
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { cell ->
                    Text(
                        text = cell,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (rowIndex == 0) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (rowIndex != cells.lastIndex) HorizontalDivider()
        }
    }
}
