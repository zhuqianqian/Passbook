package com.z299studio.pbfree.tool

import org.apache.commons.codec.DecoderException
import org.apache.commons.codec.binary.Hex
import java.security.GeneralSecurityException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * An AES crypto utility.
 */
class AesHelper
    /**
     * Constructor for creating an AES Utility with given iteration count and
     * keyLength in bits. Using this constructor to create one instance for
     * encryption.
     * @param iteration the iteration count for deriving key.
     * @param keyLength key length in bits.
     */
    (val iteration: Int , val keyLength: Int) {
    private var salt: ByteArray? = null
    private var iv: ByteArray? = null
    private var derivedKey: SecretKey? = null

    /**
     * Construct an AES utility, often for decryption, when the salt and iv are
     * available.
     * @param iteration iteration count for deriving key.
     * @param keyLengthInBits key length in bits.
     * @param saltAndIvData an byte array that holds salt and iv data.
     * @param offset The offset where the iv data starts from the `saltAndIvData`
     * @param total The total length of valid data in the `saltAndIvData`
     * byte array.
     */
    @Deprecated(
        """Only use it for dealing with previous version of data."""
    )
    constructor(
        iteration: Int,
        keyLengthInBits: Int,
        saltAndIvData: ByteArray,
        offset: Int,
        total: Int
    ) : this(iteration, keyLengthInBits) {
        salt = ByteArray(keyLength / 8)
        iv = ByteArray(total - salt!!.size)
        System.arraycopy(saltAndIvData, offset, salt!!, 0, salt!!.size)
        System.arraycopy(saltAndIvData, offset + salt!!.size, iv!!, 0, iv!!.size)
    }

    /**
     * Constructs and AES utility, with given salt and hex encoded in Hex
     * string.
     * @param iteration iteration count for deriving key.
     * @param keyLengthInBits key length in bits.
     * @param saltHex salt data encoded in Hex string.
     * @param ivHex iv data encoded in Hex string.
     */
    constructor(iteration: Int, keyLengthInBits: Int, saltHex: String?, ivHex: String?) : this(
        iteration,
        keyLengthInBits
    ) {
        try {
            salt = Hex.decodeHex(saltHex?.toCharArray())
            iv = Hex.decodeHex(ivHex?.toCharArray())
        } catch (ignored: DecoderException) {
        }
    }

    /**
     * Encrypt `data` with the given `password`.
     * @param password a string password.
     * @param data data to be encrypted.
     * @return encrypted data.
     */
    fun encrypt(password: String, data: ByteArray?): ByteArray {
        if (salt == null) {
            salt = ByteArray(keyLength / 8)
            random.nextBytes(salt)
        }
        val cipherData: ByteArray
        deriveKey(password)
        try {
            val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
            iv = ByteArray(cipher.blockSize)
            random.nextBytes(iv)
            val ivParams = IvParameterSpec(iv)
            cipher.init(Cipher.ENCRYPT_MODE, derivedKey, ivParams)
            cipherData = cipher.doFinal(data)
        } catch (e: GeneralSecurityException) {
            throw RuntimeException(e)
        }
        return cipherData
    }

    /**
     * Decrypt cipher `data` with the given `password`
     * @param password a string password.
     * @param data cipher data to be decrypted.
     * @return Decrypted plain data.
     * @throws GeneralSecurityException When password is wrong.
     */
    @Throws(GeneralSecurityException::class)
    fun decrypt(password: String, data: ByteArray?): ByteArray {
        val plainData: ByteArray
        deriveKey(password)
        val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
        val ivParams = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, derivedKey, ivParams)
        plainData = cipher.doFinal(data)
        return plainData
    }

    private fun deriveKey(password: String) {
        val keyBytes: ByteArray
        val keySpec: KeySpec = PBEKeySpec(
            password.toCharArray(), salt,
            iteration, keyLength
        )
        val keyFactory: SecretKeyFactory
        try {
            keyFactory = SecretKeyFactory
                .getInstance(KEY_DERIVATION_ALGORITHM)
            keyBytes = keyFactory.generateSecret(keySpec).encoded
            derivedKey = SecretKeySpec(keyBytes, "AES")
        } catch (e: NoSuchAlgorithmException) {
            println(e.message)
        } catch (e: InvalidKeySpecException) {
            println(e.message)
        }
    }

    val saltHex: String
        get() {
            checkNotNull(salt) { "salt has not been initialized" }
            return String(Hex.encodeHex(salt))
        }
    val ivHex: String
        get() {
            checkNotNull(iv) { "iv has not been initialized" }
            return String(Hex.encodeHex(iv))
        }


    companion object {
        private const val KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA1"
        private const val CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding"
        private val random = SecureRandom()
    }
}