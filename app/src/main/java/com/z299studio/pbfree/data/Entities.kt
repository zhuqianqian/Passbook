package com.z299studio.pbfree.data

import com.google.gson.annotations.SerializedName

enum class ValueType {

    @SerializedName("text")
    Text,

    @SerializedName("password")
    Password,

    @SerializedName("url")
    Url,

    @SerializedName("email")
    Email,

    @SerializedName("pin")
    Pin
}

data class ValueTuple(val key: String, val value: String, val type: ValueType)

data class Entry(val key: String, var category: String?, val values: MutableList<ValueTuple> = ArrayList()) {
    fun addValue(key: String, value: String, type: ValueType) {
        this.values.add(ValueTuple(key, value, type))
    }

    fun matches(query: String?): Boolean{
        return query.isNullOrEmpty() ||
                this.key.contains(query, true) ||
                this.values.any {
                    it.value.contains(query, true)
                }
    }
}

data class Category(val name: String, val icon: Int)
