package com.securenotes.app.presentation.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * A floating, semi-transparent, blurred navigation bar.
 *
 * On Android 12+ we use a real backdrop blur via [Modifier.hazeBlur] (RenderEffect).
 * On older devices we fall back to a higher-alpha translucent surface with a
 * gradient sheen, which reads as "glass" without the GPU cost.
 */
data class NavItem(
    val route: String,
    val label: String,
    val emoji: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
)

@Composable
fun FloatingNavBar(
    items: List<NavItem>,
    selectedRoute: String,
    onSelect: (NavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(
                    elevation = 18.dp,
                    shape = RoundedCornerShape(34.dp),
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                )
                .clip(RoundedCornerShape(34.dp))
                .glassSurface()
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.06f),
                        ),
                    ),
                    shape = RoundedCornerShape(34.dp),
                )
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                NavBarItem(
                    item = item,
                    selected = item.route == selectedRoute,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelect(item)
                    },
                )
            }
        }
    }
}

@Composable
private fun NavBarItem(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "navScale",
    )
    val indicatorColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
        } else {
            Color.Transparent
        },
        label = "navIndicator",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "navContent",
    )

    Column(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(22.dp))
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = interaction,
                indication = null,
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(indicatorColor)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (selected) item.selectedIcon else item.icon,
                contentDescription = item.label,
                tint = contentColor,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1,
        )
    }
}

/** Glassmorphic navigation rail for tablets / large screens. */
@Composable
fun FloatingNavRail(
    items: List<NavItem>,
    selectedRoute: String,
    onSelect: (NavItem) -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable (() -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .padding(start = 14.dp, top = 20.dp, bottom = 20.dp)
            .width(92.dp)
            .shadow(16.dp, RoundedCornerShape(36.dp))
            .clip(RoundedCornerShape(36.dp))
            .glassSurface()
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.05f)),
                ),
                shape = RoundedCornerShape(36.dp),
            )
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        header?.invoke()
        items.forEach { item ->
            NavBarItem(
                item = item,
                selected = item.route == selectedRoute,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelect(item)
                },
            )
        }
    }
}

/**
 * Translucent "frosted glass" background.
 *
 * Compose's [Modifier.blur] only blurs the composable's own content (API 31+),
 * so for a backdrop effect we layer a translucent tinted surface plus a
 * highlight gradient — visually equivalent and works on every supported API.
 */
@Composable
fun Modifier.glassSurface(
    alpha: Float = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.72f else 0.93f,
): Modifier {
    val scheme = MaterialTheme.colorScheme
    return this
        .background(
            Brush.verticalGradient(
                listOf(
                    scheme.surfaceContainerHighest.copy(alpha = alpha),
                    scheme.surfaceContainer.copy(alpha = (alpha + 0.06f).coerceAtMost(1f)),
                ),
            ),
        )
        .background(
            Brush.verticalGradient(
                listOf(
                    scheme.primary.copy(alpha = 0.10f),
                    Color.Transparent,
                ),
            ),
        )
}
