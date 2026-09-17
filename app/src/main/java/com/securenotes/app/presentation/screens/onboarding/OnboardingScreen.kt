package com.securenotes.app.presentation.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val emoji: String,
    val floatingEmojis: List<String>,
    val title: String,
    val subtitle: String,
)

private val pages = listOf(
    OnboardingPage(
        emoji = "📒",
        floatingEmojis = listOf("🔐", "✨", "🛡️"),
        title = "Your private notes,\nfinally secure 📒✨",
        subtitle = "Everything you write is encrypted on your device with AES-256. Only you can read your notes — not even us.",
    ),
    OnboardingPage(
        emoji = "🗂️",
        floatingEmojis = listOf("🏷️", "🎨", "📚"),
        title = "Notebooks, tags,\ncolours & more 🗂️🎨",
        subtitle = "Organise your thoughts your way with notebooks, nested tags, colour shortcuts, pins and favourites.",
    ),
    OnboardingPage(
        emoji = "🔐",
        floatingEmojis = listOf("🗝️", "🤫", "🔒"),
        title = "An extra secret\nspace 🔐🗝️",
        subtitle = "The Private Vault hides your most sensitive notes behind a second password — separate from your app lock.",
    ),
    OnboardingPage(
        emoji = "🌈",
        floatingEmojis = listOf("📱", "🎨", "💫"),
        title = "Themes, colours\nand emojis 🌈📱",
        subtitle = "Material You dynamic colour, light & dark modes, custom accents and adjustable text size. Make it yours.",
    ),
    OnboardingPage(
        emoji = "🚀",
        floatingEmojis = listOf("✅", "🎉", "🔑"),
        title = "Let's set up your\nsecure space 🚀",
        subtitle = "Choose a PIN to protect your notes. You can add biometric unlock and a vault right after.",
    ),
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
                    ),
                ),
            ),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            // Skip
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                AnimatedVisibility(visible = !isLast) {
                    TextButton(onClick = onFinish) { Text("Skip") }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { index ->
                OnboardingPageContent(pages[index])
            }

            // Dots
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                pages.indices.forEach { index ->
                    val selected = pagerState.currentPage == index
                    val width by animateFloatAsState(
                        targetValue = if (selected) 26f else 8f,
                        animationSpec = tween(300),
                        label = "dot",
                    )
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(width.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                            ),
                    )
                }
            }

            Button(
                onClick = {
                    if (isLast) {
                        onFinish()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .height(58.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(
                    text = if (isLast) "Set up PIN 🔑" else "Next",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(26.dp))
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IllustrationBlob(page)

        Spacer(Modifier.height(44.dp))

        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(420)) + slideInVertically(tween(420)) { it / 4 },
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = page.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 38.sp,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = page.subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Soft gradient "blob" with a big centred emoji and orbiting mini emojis. */
@Composable
private fun IllustrationBlob(page: OnboardingPage) {
    val transition = rememberInfiniteTransition(label = "blob")
    val float by transition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Reverse),
        label = "float",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "pulse",
    )

    Box(
        modifier = Modifier.size(260.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(230.dp)
                .scale(pulse)
                .clip(RoundedCornerShape(84.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            Modifier
                .size(168.dp)
                .clip(RoundedCornerShape(58.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = page.emoji,
                fontSize = 86.sp,
                modifier = Modifier.padding(bottom = float.dp),
            )
        }

        // Orbiting accents
        page.floatingEmojis.forEachIndexed { index, emoji ->
            val angle = when (index) {
                0 -> Alignment.TopEnd
                1 -> Alignment.BottomStart
                else -> Alignment.TopStart
            }
            Box(
                Modifier
                    .align(angle)
                    .padding(10.dp)
                    .size(56.dp)
                    .rotate(float / 2)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = emoji, fontSize = 26.sp)
            }
        }
    }
}
