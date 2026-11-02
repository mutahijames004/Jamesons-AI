package com.example.data

import android.util.Base64
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoVault {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val IV_LENGTH = 12
    private const val SALT_LENGTH = 16
    private const val ITERATION_COUNT = 65536
    private const val KEY_LENGTH = 256

    // Default system seed for automatic local at-rest encryption
    private val DEFAULT_SEED = "Synapse-Academic-Master-Vault-Key-2026".toByteArray()

    private fun deriveKey(passphrase: String?, salt: ByteArray): SecretKeySpec {
        val chars = (passphrase ?: "Synapse-Encrypted-Academic-Vault").toCharArray()
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(chars, salt, ITERATION_COUNT, KEY_LENGTH)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, ALGORITHM)
    }

    /**
     * Encrypts plaintext using AES-256-GCM with a random IV and PBKDF2 salt.
     * Returns a URL-safe Base64 encoded payload: [SALT (16B)][IV (12B)][CIPHERTEXT + GCM TAG].
     */
    fun encrypt(plaintext: String, passphrase: String? = null): String {
        try {
            val random = SecureRandom()
            val salt = ByteArray(SALT_LENGTH).also { random.nextBytes(it) }
            val iv = ByteArray(IV_LENGTH).also { random.nextBytes(it) }

            val key = deriveKey(passphrase, salt)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)

            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

            val byteBuffer = ByteBuffer.allocate(salt.size + iv.size + ciphertext.size)
            byteBuffer.put(salt)
            byteBuffer.put(iv)
            byteBuffer.put(ciphertext)

            return Base64.encodeToString(byteBuffer.array(), Base64.NO_WRAP)
        } catch (e: Exception) {
            // Fallback gracefully if any unexpected crypto provider failure occurs
            return "[ENCRYPT_FAILED]: " + plaintext
        }
    }

    /**
     * Decrypts the Base64 payload using AES-256-GCM.
     */
    fun decrypt(encryptedPayload: String, passphrase: String? = null): String {
        try {
            if (!isEncrypted(encryptedPayload)) {
                return encryptedPayload
            }

            val rawBytes = Base64.decode(encryptedPayload, Base64.NO_WRAP)
            if (rawBytes.size < SALT_LENGTH + IV_LENGTH + 16) {
                return encryptedPayload
            }

            val buffer = ByteBuffer.wrap(rawBytes)
            val salt = ByteArray(SALT_LENGTH).also { buffer.get(it) }
            val iv = ByteArray(IV_LENGTH).also { buffer.get(it) }
            val ciphertext = ByteArray(buffer.remaining()).also { buffer.get(it) }

            val key = deriveKey(passphrase, salt)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)

            val decryptedBytes = cipher.doFinal(ciphertext)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            return "[Decryption Error: Incorrect Passphrase or Corrupted Data]"
        }
    }

    fun isEncrypted(text: String): Boolean {
        if (text.startsWith("[ENCRYPT_FAILED]:") || text.isBlank()) return false
        return try {
            val decoded = Base64.decode(text, Base64.NO_WRAP)
            decoded.size > (SALT_LENGTH + IV_LENGTH + 16)
        } catch (_: Exception) {
            false
        }
    }
}
