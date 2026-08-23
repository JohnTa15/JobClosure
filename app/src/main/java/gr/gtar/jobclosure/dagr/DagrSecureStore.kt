package gr.gtar.jobclosure.dagr

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** A DAGR pilot account. Blank means "not set up yet". */
data class DagrAccount(val username: String = "", val password: String = "") {
    val isConfigured: Boolean get() = username.isNotBlank() && password.isNotBlank()
}

/**
 * Holds the DAGR sign-in encrypted under a key that lives in the Android Keystore.
 *
 * This is a civil aviation authority login, so it does not go in DataStore alongside the map API
 * key and the home address. The AES key never leaves the Keystore - hardware-backed on devices that
 * have a TEE or StrongBox - which makes the stored blob useless on its own to anything that reads
 * the app's files off a rooted device or out of a backup.
 *
 * Decryption is allowed to fail and is treated as "nothing stored": a Keystore key does not survive
 * a restore onto another device, and returning garbage or crashing there would be worse than simply
 * asking for the password again.
 */
class DagrSecureStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): DagrAccount = DagrAccount(
        username = decrypt(prefs.getString(KEY_USERNAME, null)),
        password = decrypt(prefs.getString(KEY_PASSWORD, null)),
    )

    fun save(account: DagrAccount) {
        prefs.edit()
            .putString(KEY_USERNAME, encrypt(account.username))
            .putString(KEY_PASSWORD, encrypt(account.password))
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun encrypt(plain: String): String? {
        if (plain.isEmpty()) return null
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            // The IV is generated per encryption and is not secret, so it simply rides in front of
            // the ciphertext - GCM needs the exact same one back to decrypt.
            Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
        } catch (e: GeneralSecurityException) {
            null
        }
    }

    private fun decrypt(stored: String?): String {
        if (stored.isNullOrEmpty()) return ""
        return try {
            val bytes = Base64.decode(stored, Base64.NO_WRAP)
            if (bytes.size <= IV_LENGTH) return ""
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey(),
                GCMParameterSpec(TAG_LENGTH_BITS, bytes, 0, IV_LENGTH),
            )
            String(cipher.doFinal(bytes, IV_LENGTH, bytes.size - IV_LENGTH), Charsets.UTF_8)
        } catch (e: GeneralSecurityException) {
            ""
        } catch (e: IllegalArgumentException) {
            ""
        }
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "jobclosure.dagr.v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val PREFS_NAME = "dagr_account"
        const val KEY_USERNAME = "username"
        const val KEY_PASSWORD = "password"
        const val IV_LENGTH = 12
        const val TAG_LENGTH_BITS = 128
    }
}
