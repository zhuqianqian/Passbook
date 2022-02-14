package com.z299studio.pbfree.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.z299studio.pbfree.tool.AesHelper
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.binary.Hex
import java.security.MessageDigest
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.set

private data class FileInfo(val app: String, val version: String, val saveTime: Long?,
                             val dataVersion: Int?, val crypto: CryptoInfo?, val entries: String?)

abstract class DataProcessor {

    /**
     * Convert the given [categories] and [entries] to ByteArray for persisting data.
     * The data processor encrypts the data with the given [password] and save the
     * related information in the returned ByteArray. When the data needs being
     * converted back to data objects, get the processor by the ByteArray data and
     * it can parse them back.
     * @param appInfo file info object to save along with data.
     * @param password The password used for encrypting the data.
     * @param categories An optional list of categories names.
     * @param entries A list of entries.
     * @return byte data for persisting.
     */
    abstract fun toByteArray(appInfo: AppInfo,
                             password: String,
                             categories: List<Category>?,
                             entries: List<Entry>) : ByteArray

    /**
     * Parse a Passbook data file content with given `password`
     * @param password The password string to decrypt the encrypted part in the content
     * @param pbData The passbook data file content.
     * @return a [Pair] of data where the [Pair.first] is a list of parsed [Entry] and the
     * and the [Pair.second] a set of parsed category names ([String])
     */
    abstract fun parse(password: String, pbData: ByteArray)
    : Triple<List<Entry>, List<Category>, AppInfo>

    abstract fun fileInfo(pbData: ByteArray): AppInfo

    companion object {

        /**
         * Get a DataParser that is supposed to be capable of parsing the given
         * [pbData].
         * @param pbData data that will be parsed
         * @throws IllegalArgumentException when no proper parse could be found
         * to handle the given data.
         */
        @Throws(IllegalArgumentException::class)
        fun getDataParser(pbData: ByteArray) : DataProcessor {
            if (pbData[0] == 'P'.code.toByte() && pbData[1] == 'B'.code.toByte()) {
                return V1DataProcessor()
            } else if (pbData[0] == '{'.code.toByte()
                && pbData[pbData.size - 1] == '}'.code.toByte()) {
                return V2DataProcessor()
            }
            throw IllegalArgumentException("Cannot find suitable DataParser to parse the given data")
        }

        fun getConverter() : DataProcessor {
            return V2DataProcessor()
        }
    }
}

/**
 * To be removed. Once all users upgrade to the new version, this can be removed.
 */
private class V1DataProcessor : DataProcessor() {
    override fun toByteArray(appInfo: AppInfo, password: String,
                            categories: List<Category>?, entries: List<Entry>): ByteArray {
        TODO("V1DataProcessor should not be used in this version.")
    }

    override fun parse(password: String, pbData: ByteArray)
    : Triple<List<Entry>, List<Category>, AppInfo> {
        val iteration = 100 * pbData[3]
        val keyLength = pbData[4]
        val ivLength = pbData[5]
        val offset = 16 + keyLength + ivLength
        val aesHelper = AesHelper(iteration, 256, pbData, 16, keyLength + ivLength)
        val encryptedData = ByteArray(pbData.size - offset)
        System.arraycopy(pbData, offset, encryptedData, 0, encryptedData.size)
        val categoryAndEntries = deserialize(String(aesHelper.decrypt(password, encryptedData)))
        val appInfo = fileInfo(pbData)
        return Triple(categoryAndEntries.first, categoryAndEntries.second, appInfo)
    }

    override fun fileInfo(pbData: ByteArray): AppInfo {
        return AppInfo("PB", "1.0", pbData[2].toUByte().toInt())
    }

    private fun deserialize(text: String) : Pair<List<Entry>, List<Category>> {
        val categories = ArrayList<Category>()
        val accounts = text.split("\u0000\u0000")
        val rawCategories = accounts[0].split("\u0000")
        var details: List<String>
        val categoryMap: MutableMap<Int, String> = HashMap()
        var accountStart = 1
        for (category in rawCategories) {
            details = category.split("\t", ignoreCase = false, limit = 3)
            if (details.size < 3) {
                accountStart = 0
                break
            }
            categoryMap[Integer.valueOf(details[0])] = details[2]
            categories.add( Category(details[2], details[1].toInt()))
        }
        val typeMap = mapOf(1 to ValueType.Text, 2 to ValueType.Password, 3 to ValueType.Url,
            4 to ValueType.Email, 5 to ValueType.Pin)
        val entries = ArrayList<Entry>(accounts.size)
        for (i in accountStart until accounts.size) {
            details = accounts[i].split("\t", ignoreCase = false, limit = 2)
            if (details.size >= 2) {
                val category = categoryMap[details[0].toInt()]
                val rawEntries = accounts[i].split("\u0000")
                val name = rawEntries[0].split("\t", ignoreCase = false, limit = 2)[1]
                val entry = Entry(name, category)
                entries.add(entry)
                for (j in 1 until rawEntries.size) {
                    val items = rawEntries[j].split("\t", ignoreCase = false, limit = 3)
                    typeMap[items[0].toInt()]?.let {
                        entry.addValue( items[1], items[2], it)
                    } ?: run {
                        entry.addValue(items[1], items[2], ValueType.Text)
                    }
                }
            }
        }
        return Pair(entries, categories)
    }
}

private class V2DataProcessor : DataProcessor() {
    override fun toByteArray(appInfo: AppInfo, password: String,
                             categories: List<Category>?, entries: List<Entry>): ByteArray {
        val aesHelper = AesHelper(2000, 256)
        val cryptoInfo: CryptoInfo
        val encryptedText: String = if (categories == null) {
            String(Base64.encodeBase64(
                aesHelper.encrypt(derivePassword(appInfo.saveTime, password),
                    Gson().toJson(entries).toByteArray())))
        } else {
            val data = CategoriesAndEntries(categories, entries)
            String(Base64.encodeBase64(
                aesHelper.encrypt(derivePassword(appInfo.saveTime, password),
                    Gson().toJson(data).toByteArray())))
        }
        val params = AesCipherParams(aesHelper.keyLength, aesHelper.iteration, aesHelper.ivHex, aesHelper.saltHex)
        cryptoInfo = CryptoInfo("aes", params)
        val data = FileInfo(appInfo.app, appInfo.version, appInfo.saveTime, appInfo.dataVersion, cryptoInfo, encryptedText)
        return Gson().toJson(data).toByteArray()
    }

    override fun parse(password: String, pbData: ByteArray)
    : Triple<List<Entry>, List<Category>, AppInfo> {
        val fileJson = Gson().fromJson(String(pbData), FileInfo::class.java)
        if (fileJson.crypto == null) {
            throw java.lang.IllegalArgumentException("Data does not contain crypto information")
        }
        val appInfo = AppInfo(fileJson.app, fileJson.version,fileJson.dataVersion ?: 0, fileJson.saveTime ?: 0)
        val params = fileJson.crypto.params
        val aesHelper = AesHelper(params.iteration, params.keySize, params.salt, params.iv)
        val encryptedText = Base64().decode(fileJson.entries?.toByteArray(Charsets.UTF_8))
        val plaintext = String(aesHelper.decrypt(derivePassword(fileJson.saveTime, password), encryptedText))
        val categories: List<Category>
        val entries: List<Entry>
        if (plaintext.startsWith("[")) {
            entries = Gson().fromJson(plaintext, object : TypeToken<List<Entry>>(){}.type)
            categories = entries.mapNotNull { entry -> entry.category }
                .toSet().map { Category(it, -1) }.toList()
        } else {
            val data = Gson().fromJson(plaintext, CategoriesAndEntries::class.java)
            entries = data.entries
            categories = data.categories
        }
        return Triple(entries, categories, appInfo)
    }

    override fun fileInfo(pbData: ByteArray): AppInfo {
        val fileJson = Gson().fromJson(String(pbData), FileInfo::class.java)
        return AppInfo(fileJson.app, fileJson.version,
            fileJson.dataVersion ?: 0, fileJson.saveTime ?: 0)
    }

    private fun derivePassword(time: Long?, rawPassword: String) : String {
        if (time == null) {
            return rawPassword
        }
        val md5 = MessageDigest.getInstance("MD5")
        val sha1 = MessageDigest.getInstance("SHA1")

        return  String(Hex.encodeHex(md5.digest((time + 299).toString().toByteArray()))).substring(7, 15) +
                String(Hex.encodeHex(sha1.digest((time - 299).toString().toByteArray()))).substring(23, 30) +
                String(Hex.encodeHex(md5.digest(rawPassword.toByteArray()))).substring(0, 8).uppercase()
    }

}

