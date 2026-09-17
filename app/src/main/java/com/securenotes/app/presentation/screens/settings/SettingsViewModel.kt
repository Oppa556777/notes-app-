package com.securenotes.app.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securenotes.app.core.crypto.CryptoManager
import com.securenotes.app.core.security.AppLockManager
import com.securenotes.app.data.preferences.SettingsRepository
import com.securenotes.app.data.sync.SyncManager
import com.securenotes.app.data.sync.SyncState
import com.securenotes.app.domain.model.AccentColor
import com.securenotes.app.domain.model.AppLockTimeout
import com.securenotes.app.domain.model.AppSettings
import com.securenotes.app.domain.model.FontScale
import com.securenotes.app.domain.model.ThemeMode
import com.securenotes.app.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val cryptoManager: CryptoManager,
    private val appLockManager: AppLockManager,
    private val noteRepository: NoteRepository,
    private val syncManager: SyncManager,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val syncState: StateFlow<SyncState> = syncManager.state

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        settingsRepository.setThemeMode(mode)
    }

    fun setDynamicColor(value: Boolean) = viewModelScope.launch {
        settingsRepository.setDynamicColor(value)
    }

    fun setAccent(accent: AccentColor) = viewModelScope.launch {
        settingsRepository.setAccentColor(accent)
    }

    fun setCustomAccent(argb: Long) = viewModelScope.launch {
        settingsRepository.setCustomAccent(argb)
    }

    fun setFontScale(scale: FontScale) = viewModelScope.launch {
        settingsRepository.setFontScale(scale)
    }

    fun setCompactList(value: Boolean) = viewModelScope.launch {
        settingsRepository.setCompactList(value)
    }

    fun setBiometric(value: Boolean) = viewModelScope.launch {
        settingsRepository.setBiometricEnabled(value)
    }

    fun setLockTimeout(value: AppLockTimeout) = viewModelScope.launch {
        settingsRepository.setLockTimeout(value)
    }

    fun setAppLock(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setAppLockEnabled(enabled)
        if (!enabled) cryptoManager.clearAppPin()
    }

    fun changeAppPin(pin: String) = viewModelScope.launch {
        cryptoManager.setAppPin(pin)
        settingsRepository.setAppLockEnabled(true)
    }

    fun hasVaultPin(): Boolean = cryptoManager.hasVaultPin()

    fun setVaultPin(pin: String) = viewModelScope.launch {
        cryptoManager.setVaultPin(pin)
        settingsRepository.setVaultEnabled(true)
    }

    fun disableVault() = viewModelScope.launch {
        cryptoManager.clearVaultPin()
        settingsRepository.setVaultEnabled(false)
        appLockManager.lockVault()
    }

    fun setAutoLinkPreviews(value: Boolean) = viewModelScope.launch {
        settingsRepository.setAutoLinkPreviews(value)
    }

    fun setSpellCheck(value: Boolean) = viewModelScope.launch {
        settingsRepository.setSpellCheck(value)
    }

    fun setMarkdownAssist(value: Boolean) = viewModelScope.launch {
        settingsRepository.setMarkdownAssist(value)
    }

    fun setSyncEnabled(value: Boolean) = viewModelScope.launch {
        settingsRepository.setSyncEnabled(value)
    }

    fun syncNow() = viewModelScope.launch { syncManager.syncNow() }

    fun emptyTrash() = viewModelScope.launch { noteRepository.emptyTrash() }
}
