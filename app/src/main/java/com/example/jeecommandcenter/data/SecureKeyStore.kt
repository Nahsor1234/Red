package com.example.jeecommandcenter.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureKeyStore(context: Context) {

    private val prefs = context.getSharedPreferences("jee_secure_keys", Context.MODE_PRIVATE)
    private val alias = "jee_ai_api_key"

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

        val existing = runCatching { store.getKey(alias, null) }.getOrNull()
        if (existing is SecretKey) return existing

        if (runCatching { store.containsAlias(alias) }.getOrDefault(false)) {
            runCatching { store.deleteEntry(alias) }
        }

        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(256)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()

        return KeyGenerator
            .getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            .apply { init(spec) }
            .generateKey()
    }

    fun save(value: String) {
        require(value.isNotBlank()) { "API key cannot be blank." }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))

        prefs.edit()
            .putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString("ciphertext", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
    }

    fun read(): String? {
        val iv = prefs.getString("iv", null) ?: return null
        val ciphertext = prefs.getString("ciphertext", null) ?: return null

        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                key(),
                GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))
            )
            String(
                cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP)),
                StandardCharsets.UTF_8
            )
        }.getOrNull()
    }

    fun clear() {
        prefs.edit().clear().apply()
        runCatching {
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                .takeIf { it.containsAlias(alias) }
                ?.deleteEntry(alias)
        }
    }

    fun exists(): Boolean = read() != null
}
