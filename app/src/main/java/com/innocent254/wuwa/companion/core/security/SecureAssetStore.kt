package com.innocent254.wuwa.companion.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores offline assets as AES-GCM encrypted blobs inside app-private,
 * no-backup storage. Decrypted bytes are exposed only as a stream.
 *
 * This blocks ordinary file-manager browsing. It cannot prevent extraction
 * from a rooted/debugged/instrumented device.
 */
class SecureAssetStore(context: Context) {
    private val directory = File(context.noBackupFilesDir, "secure_assets").apply { mkdirs() }

    fun contains(assetId: String): Boolean = fileFor(assetId).isFile

    fun put(assetId: String, source: InputStream) {
        val target = fileFor(assetId)
        val staging = File(target.parentFile, "${target.name}.tmp")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())

        BufferedOutputStream(staging.outputStream()).use { rawOutput ->
            rawOutput.write(FORMAT_VERSION)
            rawOutput.write(cipher.iv.size)
            rawOutput.write(cipher.iv)
            CipherOutputStream(rawOutput, cipher).use { encryptedOutput ->
                source.copyTo(encryptedOutput)
            }
        }

        Files.move(
            staging.toPath(),
            target.toPath(),
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING,
        )
    }

    fun open(assetId: String): InputStream {
        val rawInput = BufferedInputStream(fileFor(assetId).inputStream())
        val version = rawInput.read()
        require(version == FORMAT_VERSION) { "Unsupported secure asset format." }

        val ivLength = rawInput.read()
        require(ivLength in 12..32) { "Invalid IV length." }
        val iv = rawInput.readNBytes(ivLength)
        require(iv.size == ivLength) { "Truncated encrypted asset." }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(GCM_TAG_BITS, iv),
        )
        return CipherInputStream(rawInput, cipher)
    }

    fun delete(assetId: String): Boolean = fileFor(assetId).delete()

    private fun fileFor(assetId: String): File {
        val opaqueName = MessageDigest.getInstance("SHA-256")
            .digest(assetId.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(directory, "$opaqueName.bin")
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE,
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "wuwa_secure_assets_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val FORMAT_VERSION = 1
    }
}
