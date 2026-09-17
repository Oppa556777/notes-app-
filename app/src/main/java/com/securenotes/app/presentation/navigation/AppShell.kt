package com.securenotes.app.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.MaterialTheme
import com.securenotes.app.presentation.MainViewModel
import com.securenotes.app.presentation.components.FloatingNavBar
import com.securenotes.app.presentation.components.FloatingNavRail
import com.securenotes.app.presentation.components.NavItem
import com.securenotes.app.presentation.screens.editor.EditorScreen
import com.securenotes.app.presentation.screens.notebooks.NotebooksScreen
import com.securenotes.app.presentation.screens.notebooks.OrganizationViewModel
import com.securenotes.app.presentation.screens.notes.NotesScreen
import com.securenotes.app.presentation.screens.search.SearchScreen
import com.securenotes.app.presentation.screens.settings.SettingsScreen
import com.securenotes.app.presentation.screens.tags.TagsScreen
import com.securenotes.app.presentation.screens.vault.VaultScreen
import kotlinx.coroutines.launch

object Routes {
    const val NOTES = "notes"
    const val NOTEBOOKS = "notebooks"
    const val TAGS = "tags"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val VAULT = "vault"
    const val EDITOR = "editor/{noteId}?vault={vault}"

    fun editor(noteId: String, vault: Boolean = false) = "editor/$noteId?vault=$vault"
}

private val navItems = listOf(
    NavItem(Routes.NOTES, "Notes", "📒", Icons.Rounded.Home),
    NavItem(Routes.NOTEBOOKS, "Books", "📚", Icons.Rounded.MenuBook),
    NavItem(Routes.TAGS, "Tags", "🏷️", Icons.Rounded.Label),
    NavItem(Routes.SEARCH, "Search", "🔍", Icons.Rounded.Search),
    NavItem(Routes.SETTINGS, "Settings", "⚙️", Icons.Rounded.Settings),
)

@Composable
fun AppShell(
    mainViewModel: MainViewModel,
    widthSizeClass: WindowWidthSizeClass,
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val organizationViewModel: OrganizationViewModel = hiltViewModel()
    val notebooks by organizationViewModel.notebooks.collectAsStateWithLifecycle()
    val tags by organizationViewModel.tags.collectAsStateWithLifecycle()
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route.orEmpty()
    val showNav = currentRoute in navItems.map { it.route } || currentRoute == Routes.VAULT
    val expanded = widthSizeClass != WindowWidthSizeClass.Compact

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                notebooks = notebooks,
                tags = tags,
                onFilter = { closeDrawer(scope, drawerState); navController.navigateTab(Routes.NOTES) },
                onNotebook = { closeDrawer(scope, drawerState); navController.navigateTab(Routes.NOTEBOOKS) },
                onTag = { closeDrawer(scope, drawerState); navController.navigateTab(Routes.TAGS) },
                onColor = { closeDrawer(scope, drawerState); navController.navigateTab(Routes.NOTES) },
                onVault = { closeDrawer(scope, drawerState); navController.navigateTab(Routes.VAULT) },
            )
        },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f),
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                ),
        ) {
            Row(Modifier.fillMaxSize()) {
                // Tablets get a glass rail on the left.
                if (expanded && showNav) {
                    FloatingNavRail(
                        items = navItems,
                        selectedRoute = currentRoute,
                        onSelect = { navController.navigateTab(it.route) },
                    )
                }

                AppNavHost(
                    navController = navController,
                    mainViewModel = mainViewModel,
                    vaultUnlocked = uiState.vaultUnlocked,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Phones get the floating glass bar at the bottom.
            if (!expanded) {
                AnimatedVisibility(
                    visible = showNav,
                    enter = fadeIn() + scaleIn(initialScale = 0.9f),
                    exit = fadeOut() + scaleOut(targetScale = 0.9f),
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    FloatingNavBar(
                        items = navItems,
                        selectedRoute = currentRoute,
                        onSelect = { navController.navigateTab(it.route) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    mainViewModel: MainViewModel,
    vaultUnlocked: Boolean,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.NOTES,
        modifier = modifier,
        enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 12 } },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(180)) + slideOutHorizontally(tween(260)) { it / 12 } },
    ) {
        composable(Routes.NOTES) {
            NotesScreen(
                onOpenNote = { navController.navigate(Routes.editor(it)) },
                onNewNote = { navController.navigate(Routes.editor("new")) },
                onOpenSearch = { navController.navigateTab(Routes.SEARCH) },
                onOpenDrawer = onOpenDrawer,
            )
        }

        composable(Routes.NOTEBOOKS) {
            NotebooksScreen(onOpenNotebook = { navController.navigateTab(Routes.NOTES) })
        }

        composable(Routes.TAGS) {
            TagsScreen(onOpenTag = { navController.navigateTab(Routes.NOTES) })
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                onOpenNote = { navController.navigate(Routes.editor(it)) },
                onBack = { navController.navigateTab(Routes.NOTES) },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onChangePin = { navController.navigateTab(Routes.NOTES) },
                onSetupVault = { navController.navigateTab(Routes.VAULT) },
            )
        }

        composable(Routes.VAULT) {
            VaultScreen(
                unlocked = vaultUnlocked,
                hasVaultPin = mainViewModel.hasVaultPin(),
                onVerifyPin = mainViewModel::verifyVaultPin,
                onCreatePin = { mainViewModel.setVaultPin(it) },
                onOpenNote = { navController.navigate(Routes.editor(it, vault = true)) },
                onNewNote = { navController.navigate(Routes.editor("new", vault = true)) },
                onOpenSearch = { navController.navigateTab(Routes.SEARCH) },
                onOpenDrawer = onOpenDrawer,
            )
        }

        composable(
            route = Routes.EDITOR,
            arguments = listOf(
                navArgument("noteId") { type = NavType.StringType },
                navArgument("vault") {
                    type = NavType.StringType
                    defaultValue = "false"
                },
            ),
        ) {
            EditorScreen(onBack = { navController.popBackStack() })
        }
    }
}

/** Single-top tab navigation that keeps the back stack shallow. */
private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun closeDrawer(
    scope: kotlinx.coroutines.CoroutineScope,
    drawerState: androidx.compose.material3.DrawerState,
) {
    scope.launch { drawerState.close() }
}
