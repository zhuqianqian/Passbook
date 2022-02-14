package com.z299studio.pbfree.data

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.OutputStream
import java.security.GeneralSecurityException
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class AccountRepository {

    var password: String = ""
    private var entries: MutableList<Entry> = ArrayList()
    private var categories: MutableList<Category> = ArrayList()
    private val categoryNames = HashSet<String>()
    private val typedNamesMap = HashMap<ValueType, List<String>>()

    fun setPassword(context: Context, password: String) {
        this.password = password
        saveData(context)
    }
    fun auth(password: String) {
        if (this.password.isEmpty() || this.password != password) {
            throw GeneralSecurityException()
        }
    }

    fun hasData(context: Context) : Boolean {
        return try {
            val file = File(context.filesDir, FILE_NAME)
            file.length() > 0
        } catch (ex: IOException) {
            false
        }
    }

    fun saveData(file: OutputStream, password: String?) {
        try {
            val converter = DataProcessor.getConverter()
            APP_INFO.saveTime = System.currentTimeMillis()
            val passphrase = if (password.isNullOrEmpty()) { this.password} else { password }
            file.write(converter.toByteArray(APP_INFO, passphrase, categories,  entries))
        } catch (ex : Exception) {
            Log.w("Passbook", "AccountRepository.saveData(file, password?) failed with ${ex.message}")
        }
    }

    fun saveData(context: Context) {
        try {
            val outputStream = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE)
            APP_INFO.dataVersion++
            saveData(outputStream, this.password)
            outputStream.close()
        } catch (error: IOException) {
            Log.e("Passbook", "AccountRepository.saveData(context) failed with ${error.message}")
        }
    }

    fun saveData(context: Context, data: ByteArray) {
        try {
            val outputStream = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE)
            outputStream.write(data)
            outputStream.close()
        } catch (error: IOException) {
            Log.e("Passbook", "AccountRepository.saveData(context, data) failed with ${error.message}")
        }
    }
    
    fun load(context: Context, password: String) : Boolean {
        return try {
            val inputStream = context.openFileInput(FILE_NAME)
            val file = File(context.filesDir, FILE_NAME)
            val content = ByteArray(file.length().toInt())
            inputStream.read(content)
            val decrypted = DataProcessor.getDataParser(content).parse(password, content)
            this.entries.clear()
            this.categories.clear()
            this.entries.addAll(decrypted.first)
            this.categories.addAll(decrypted.second)
            this.password = password
            APP_INFO.dataVersion = decrypted.third.dataVersion
            APP_INFO.saveTime = decrypted.third.saveTime
            true
        } catch (ex : FileNotFoundException) {
            Log.w("Pb.AccountRepository", "error: File Not Found")
            false
        } catch (ex : IOException) {
            Log.e("Pb.AccountRepository", "error: IOException", ex)
            false
        }
    }

    fun getAllEntries() : List<Entry> {
        return this.entries
    }

    fun getOne(id: Int) : Entry {
        return this.entries[id]
    }

    fun delete(id: Int) {
        this.entries.removeAt(id)
    }

    fun add(entry: Entry) : Int {
        this.entries.add(entry)
        return this.entries.size - 1
    }

    fun set(id: Int, entry: Entry) {
        this.entries[id] = entry
    }

    fun getAllCategories() : List<Category> {
        return this.categories
    }

    fun deleteCategory(name: String) {
        this.getAllEntries().filter { name == it.category }
            .forEach { it.category = null }
        this.categories.removeAll { name == it.name }
    }

    fun addCategory(category: Category): Boolean {
        if (categoryNames.isEmpty() && categories.isNotEmpty()) {
            categoryNames.addAll(categories.map { it.name })
        }
        if (categoryNames.contains(category.name)) {
            return false
        }
        return this.categories.add(category)
    }

    fun getFieldKeys(type: ValueType?): List<String> {
        if (type == null) {
            return ArrayList()
        }
        return typedNamesMap.getOrPut(type) {
            this.getAllEntries().flatMap { entry ->
                entry.values.filter { it.type == type }.map { it.key }
            }.groupingBy { it }.eachCount().entries.sortedBy { it.value }
                .map { it.key }.toSortedSet().toList()
        }
    }

    fun setData(result: Triple<List<Entry>, List<Category>, AppInfo>) {
        APP_INFO.dataVersion = result.third.dataVersion
        this.entries.clear()
        this.entries.addAll(result.first)
        this.categories.clear()
        this.categories.addAll(result.second)
    }

    companion object {
        const val FILE_NAME = "data"

        val APP_INFO = AppInfo("pb-android","3.0")

        fun get(): AccountRepository = repository
        private val repository = AccountRepository()
    }
}