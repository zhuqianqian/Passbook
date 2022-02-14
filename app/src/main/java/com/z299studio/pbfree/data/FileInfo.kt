package com.z299studio.pbfree.data

data class AesCipherParams(val keySize: Int, val iteration: Int, val iv: String, val salt: String)

data class CryptoInfo(val method: String, val params: AesCipherParams)

data class AppInfo(val app: String, val version: String, var dataVersion: Int = 0, var saveTime: Long = 0)

data class CategoriesAndEntries(val categories: List<Category>, val entries: List<Entry>)