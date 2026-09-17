package com.securenotes.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securenotes.app.core.crypto.CryptoManager
import com.securenotes.app.core.security.AppLockManager
import com.securenotes.app.data.preferences.SettingsRepository
import com.securenotes.app.domain.model.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the app shell should be showing right now. */
sealed interface AppGate {
    data object Loading : AppGate
    data object Onboarding : AppGate
    data object SetupPin : AppGate
    data object Locked : AppGate
    data object Ready : AppGate
}

data class MainUiState(
    val settings: AppSettings = AppSettings(),
    val gate: AppGate = AppGate.Loading,
    val vaultUnlocked: Boolean = false,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val cryptoManager: CryptoManager,
    val appLockManager: AppLockManager,
) : ViewModel() {

    /** Bumped whenever a PIN is created or setup is skipped, to re-evaluate the gate. */
    private val securityRevision = MutableStateFlow(0)

    val uiState: StateFlow<MainUiState> = combine(
        settingsRepository.settings,
        appLockManager.isUnlocked,
        appLockManager.isVaultUnlocked,
        securityRevision,
    ) { settings, unlocked, vaultUnlocked, _ ->
        val hasPin = cryptoManager.hasAppPin()
        val gate = when {
            !settings.onboardingComplete -> AppGate.Onboarding
            // Straight after onboarding we invite the user to create a PIN.
            !settings.securitySetupDone -> AppGate.SetupPin
            settings.appLockEnabled && !hasPin -> AppGate.SetupPin
            settings.appLockEnabled && !unlocked -> AppGate.Locked
            else -> AppGate.Ready
        }
        MainUiState(settings = settings, gate = gate, vaultUnlocked = vaultUnlocked)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(),
    )

    fun completeOnboarding() = viewModelScope.launch {
        settingsRepository.setOnboardingComplete(true)
    }

    fun setAppPin(pin: String) = viewModelScope.launch {
        cryptoManager.setAppPin(pin)
        settingsRepository.setAppLockEnabled(true)
        settingsRepository.setSecuritySetupDone(true)
        appLockManager.unlockApp()
        securityRevision.value += 1
    }

    fun skipPinSetup() = viewModelScope.launch {
        settingsRepository.setAppLockEnabled(false)
        settingsRepository.setSecuritySetupDone(true)
        appLockManager.unlockApp()
        securityRevision.value += 1
    }

    fun verifyPin(pin: String): Boolean = appLockManager.verifyAppPin(pin)

    fun unlockWithBiometrics() = appLockManager.unlockApp()

    fun onBackground() = appLockManager.onMovedToBackground()

    fun onForeground() {
        val state = uiState.value.settings
        appLockManager.onMovedToForeground(state.appLockTimeout, state.appLockEnabled)
    }

    // ------------------------------------------------------------- vault ---

    fun hasVaultPin(): Boolean = cryptoManager.hasVaultPin()

    fun setVaultPin(pin: String) = viewModelScope.launch {
        cryptoManager.setVaultPin(pin)
        settingsRepository.setVaultEnabled(true)
        appLockManager.unlockVault()
        securityRevision.value += 1
    }

    fun verifyVaultPin(pin: String): Boolean = appLockManager.verifyVaultPin(pin)

    fun lockVault() = appLockManager.lockVault()
}
