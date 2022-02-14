package com.z299studio.pbfree.tool

import org.apache.commons.codec.binary.Hex
import org.junit.Test

import org.junit.Assert.*
import java.lang.IllegalStateException
import java.security.GeneralSecurityException

class AesHelperTest {
    @Test
    fun testEncrypt() {
        val aesUtil = AesHelper(1000, 256)
        val secret = aesUtil.encrypt("123456", "Hello World".toByteArray())
        assertNotNull(secret)
    }

    @Test
    fun testDecrypt() {
        val aesUtil = AesHelper(1000, 256,
            "a2c1499aaf89c03e8d04b824411588c8d8d96fa818bd498a153ebc141284df73",
            "6af27ae2ee7576096ac3997f39b4682e")
        val plainText = String(aesUtil.decrypt("123456", Hex.decodeHex("0dcc1557d5f812221a4be844cf186033")))
        assertEquals(plainText, "Hello World")
    }

    @Test(expected = GeneralSecurityException::class)
    fun testDecryptFailOnWrongPassword() {
        val aesUtil = AesHelper(1000, 256,
            "a2c1499aaf89c03e8d04b824411588c8d8d96fa818bd498a153ebc141284df73",
            "6af27ae2ee7576096ac3997f39b4682e")
        aesUtil.decrypt("12345", Hex.decodeHex("0dcc1557d5f812221a4be844cf186033"))
    }

    @Test(expected = GeneralSecurityException::class)
    fun testDecryptFailOnWrongSalt() {
        val aesUtil = AesHelper(1000, 256,
            "b2c1499aaf89c03e8d04b824411588c8d8d96fa818bd498a153ebc141284df73",
            "6af27ae2ee7576096ac3997f39b4682e")
        aesUtil.decrypt("123456", Hex.decodeHex("0dcc1557d5f812221a4be844cf186033"))
    }

    @Test
    fun testDecryptFailOnWrongIv() {
        val aesUtil = AesHelper(1000, 256,
            "a2c1499aaf89c03e8d04b824411588c8d8d96fa818bd498a153ebc141284df73",
            "7af27ae2ee7576096ac3997f39b4682e")
        val plainText = String(aesUtil.decrypt("123456", Hex.decodeHex("0dcc1557d5f812221a4be844cf186033")))
        assertNotEquals(plainText, "Hello World")
    }

    @Test(expected = IllegalStateException::class)
    fun testGetSaltAndIvNotReady() {
        val aesUtil = AesHelper(2000, 256)
        println(aesUtil.ivHex)
        println(aesUtil.saltHex)
    }
}