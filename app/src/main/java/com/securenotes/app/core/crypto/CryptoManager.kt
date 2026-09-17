package com.securenotes.app.core.crypto

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns every secret the app holds:
 *
 *  - the random 256-bit database passphrase used by SQLCipher,
 *  - the salted PBKDF2 hashes of the app PIN and the vault PIN.
 *
 * All of it lives in an [EncryptedSharedPreferences] file whose master key is
 * held by the Android Keystore, so the raw values never touch plain storage.
 */
@Singleton
class CryptoManager @Inject constructor(
    private val context: Context,
) {

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    /** Lazily generated, then reused for the lifetime of the install. */
    fun databasePassphrase(): CharArray {
        val existing = prefs.getString(KEY_DB_PASSPHRASE, null)
        if (existing != null) return existing.toCharArray()

        val random = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val encoded = Base64.encodeToString(random, Base64.NO_WRAP)
        prefs.edit().putString(KEY_DB_PASSPHRASE, encoded).apply()
        return encoded.toCharArray()
    }

    // ---------------------------------------------------------------- PIN ---

    fun setAppPin(pin: String) = storePin(KEY_APP_PIN, KEY_APP_SALT, pin)

    fun verifyAppPin(pin: String): Boolean = verifyPin(KEY_APP_PIN, KEY_APP_SALT, pin)

    fun hasAppPin(): Boolean = prefs.contains(KEY_APP_PIN)

    fun clearAppPin() {
        prefs.edit().remove(KEY_APP_PIN).remove(KEY_APP_SALT).apply()
    }

    fun setVaultPin(pin: String) = storePin(KEY_VAULT_PIN, KEY_VAULT_SALT, pin)

    fun verifyVaultPin(pin: String): Boolean = verifyPin(KEY_VAULT_PIN, KEY_VAULT_SALT, pin)

    fun hasVaultPin(): Boolean = prefs.contains(KEY_VAULT_PIN)

    fun clearVaultPin() {
        prefs.edit().remove(KEY_VAULT_PIN).remove(KEY_VAULT_SALT).apply()
    }

    // ------------------------------------------------------------ internal ---

    private fun storePin(pinKey: String, saltKey: String, pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = pbkdf2(pin.toCharArray(), salt)
        prefs.edit()
            .putString(saltKey, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(pinKey, Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()
    }

    private fun verifyPin(pinKey: String, saltKey: String, pin: String): Boolean {
        val storedHash = prefs.getString(pinKey, null) ?: return false
        val storedSalt = prefs.getString(saltKey, null) ?: return false
        val salt = Base64.decode(storedSalt, Base64.NO_WRAP)
        val candidate = pbkdf2(pin.toCharArray(), salt)
        val expected = Base64.decode(storedHash, Base64.NO_WRAP)
        return constantTimeEquals(candidate, expected)
    }

    private fun pbkdf2(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS)
        return SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).encoded
    }

    /** Length-safe comparison so we never leak timing information. */
    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }

    private companion object {
        const val SECURE_PREFS = "securenotes_secure_prefs"
        const val KEY_DB_PASSPHRASE = "db_passphrase"
        const val KEY_APP_PIN = "app_pin_hash"
        const val KEY_APP_SALT = "app_pin_salt"
        const val KEY_VAULT_PIN = "vault_pin_hash"
        const val KEY_VAULT_SALT = "vault_pin_salt"
        const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        const val ITERATIONS = 120_000
        const val KEY_LENGTH_BITS = 256
    }
}
