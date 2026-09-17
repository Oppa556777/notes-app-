package com.securenotes.app.data.sync

import com.securenotes.app.data.preferences.SettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SyncState {
    data object Idle : SyncState
    data object Syncing : SyncState
    data class Success(val at: Long) : SyncState
    data class Failed(val reason: String) : SyncState
}

/**
 * Architecture stub for end-to-end encrypted cloud sync.
 *
 * The real implementation would: derive a sync key from the user's password,
 * encrypt each note locally, push ciphertext deltas to the server, and merge
 * remote changes with a last-write-wins + conflict-copy strategy. Everything
 * below is wired so the UI is complete; only the transport is mocked.
 */
@Singleton
class SyncManager @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    private val _state = MutableStateFlow<SyncState>(SyncState.Idle)
    val state: StateFlow<SyncState> = _state.asStateFlow()

    suspend fun syncNow() {
        _state.value = SyncState.Syncing
        delay(1_200) // Simulated round-trip.
        val now = System.currentTimeMillis()
        settingsRepository.setLastSync(now)
        _state.value = SyncState.Success(now)
    }
}
