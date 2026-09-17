package com.securenotes.app.core.security

import androidx.fragment.app.FragmentActivity
import com.securenotes.app.core.crypto.CryptoManager
import com.securenotes.app.domain.model.AppLockTimeout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks whether the app (and the private vault) are currently unlocked.
 *
 * The state is deliberately in-memory only, so killing the process always
 * re-locks everything.
 */
@Singleton
class AppLockManager @Inject constructor(
    private val cryptoManager: CryptoManager,
) {
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    private var backgroundedAt: Long = 0L

    fun unlockApp() { _isUnlocked.value = true }

    fun lockApp() {
        _isUnlocked.value = false
        _isVaultUnlocked.value = false
    }

    fun unlockVault() { _isVaultUnlocked.value = true }

    fun lockVault() { _isVaultUnlocked.value = false }

    fun verifyAppPin(pin: String): Boolean =
        cryptoManager.verifyAppPin(pin).also { if (it) unlockApp() }

    fun verifyVaultPin(pin: String): Boolean =
        cryptoManager.verifyVaultPin(pin).also { if (it) unlockVault() }

    fun onMovedToBackground() { backgroundedAt = System.currentTimeMillis() }

    /** Re-locks if the app stayed in the background longer than the chosen timeout. */
    fun onMovedToForeground(timeout: AppLockTimeout, lockEnabled: Boolean) {
        if (!lockEnabled || backgroundedAt == 0L) return
        val elapsed = System.currentTimeMillis() - backgroundedAt
        if (elapsed >= timeout.millis) lockApp()
        backgroundedAt = 0L
    }
}

/** Thin wrapper so screens don't need to know about AndroidX Biometric plumbing. */
class BiometricHelper @Inject constructor() {

    fun canAuthenticate(activity: FragmentActivity): Boolean {
        val manager = androidx.biometric.BiometricManager.from(activity)
        val authenticators = androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK or
            androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return manager.canAuthenticate(authenticators) ==
            androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {},
    ) {
        val executor = androidx.core.content.ContextCompat.getMainExecutor(activity)
        val prompt = androidx.biometric.BiometricPrompt(
            activity,
            executor,
            object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: androidx.biometric.BiometricPrompt.AuthenticationResult,
                ) = onSuccess()

                override fun onAuthenticationError(code: Int, message: CharSequence) =
                    onError(message.toString())
            },
        )

        val info = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            )
            .build()

        runCatching { prompt.authenticate(info) }
            .onFailure { onError(it.message ?: "Biometric unavailable") }
    }
}
