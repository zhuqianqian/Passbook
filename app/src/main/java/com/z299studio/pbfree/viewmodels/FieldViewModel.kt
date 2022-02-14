package com.z299studio.pbfree.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.z299studio.pbfree.data.ValueTuple
import com.z299studio.pbfree.data.ValueType

class FieldViewModel(): ViewModel() {
    val key = MutableLiveData("")
    var value = ""
    val type = MutableLiveData(ValueType.Text)
    val confirmed = MutableLiveData(false)

    constructor(value: ValueTuple) : this() {
        this.key.value = value.key
        this.value = value.value
        this.type.value = value.type
    }

    fun reset() {
        this.key.value = ""
        this.value = ""
        this.type.value = ValueType.Text
        this.confirmed.value = false
    }
}