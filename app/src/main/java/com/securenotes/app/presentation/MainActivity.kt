package com.securenotes.app.presentation

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.securenotes.app.core.security.BiometricHelper
import com.securenotes.app.presentation.navigation.AppShell
import com.securenotes.app.presentation.screens.lock.PinScreen
import com.securenotes.app.presentation.screens.onboarding.OnboardingScreen
import com.securenotes.app.presentation.theme.LocalAppSettings
import com.securenotes.app.presentation.theme.SecureNotesTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Single-activity host. [FragmentActivity] is required by AndroidX BiometricPrompt.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val biometricHelper = BiometricHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        var keepSplash = true
        splash.setKeepOnScreenCondition { keepSplash }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    keepSplash = state.gate is AppGate.Loading
                }
            }
        }

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val windowSizeClass = calculateWindowSizeClass(this)

            SecureNotesTheme(settings = state.settings) {
                CompositionLocalProvider(LocalAppSettings provides state.settings) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        AnimatedContent(
                            targetState = state.gate,
                            transitionSpec = {
                                fadeIn(tween(320)) togetherWith fadeOut(tween(220))
                            },
                            label = "appGate",
                        ) { gate ->
                            when (gate) {
                                AppGate.Loading -> Box(Modifier.fillMaxSize())

                                AppGate.Onboarding -> OnboardingScreen(
                                    onFinish = { viewModel.completeOnboarding() },
                                )

                                AppGate.SetupPin -> PinScreen(
                                    emoji = "🔑",
                                    title = "Choose your PIN",
                                    subtitle = "You'll use this 4-digit PIN to unlock SecureNotes.",
                                    onSecondaryAction = { viewModel.skipPinSetup() },
                                    secondaryLabel = "Skip for now",
                                    onPinEntered = { pin ->
                                        viewModel.setAppPin(pin)
                                        true
                                    },
                                    onSuccess = {},
                                )

                                AppGate.Locked -> PinScreen(
                                    emoji = "🔒",
                                    title = "Enter your PIN",
                                    subtitle = "Welcome back — your notes are waiting.",
                                    showBiometric = state.settings.biometricEnabled,
                                    onBiometric = { promptBiometric() },
                                    onPinEntered = viewModel::verifyPin,
                                    onSuccess = {},
                                )

                                AppGate.Ready -> AppShell(
                                    mainViewModel = viewModel,
                                    widthSizeClass = windowSizeClass.widthSizeClass,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun promptBiometric() {
        biometricHelper.authenticate(
            activity = this,
            title = "Unlock SecureNotes 🔓",
            subtitle = "Use your biometrics to open your encrypted notes",
            onSuccess = { viewModel.unlockWithBiometrics() },
        )
    }

    override fun onStop() {
        super.onStop()
        viewModel.onBackground()
    }

    override fun onStart() {
        super.onStart()
        viewModel.onForeground()
    }
}
