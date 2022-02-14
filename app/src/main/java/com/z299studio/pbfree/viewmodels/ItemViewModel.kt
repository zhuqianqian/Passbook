package com.z299studio.pbfree.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.z299studio.pbfree.MainActivity
import com.z299studio.pbfree.data.AccountRepository

class ItemViewModel: ViewModel() {

    var index: Int = -1

    var title: String = ""

    var category: String? = null

    val categoryIcon = MutableLiveData(MainActivity.getIcon(MainActivity.DEFAULT_ICON))

    val values = MutableLiveData(ArrayList<FieldViewModel>())

    fun from(id: Int) {
        this.index = id
        if (index >= 0) {
            val entry = AccountRepository.get().getOne(id)
            this.title = entry.key
            this.category = entry.category
            this.values.value?.clear()
            this.values.value?.addAll(entry.values.map { FieldViewModel(it) })
        } else {
            reset2CategoryTemplate(null)
        }
    }

    fun reset2CategoryTemplate(category: String?) {
        this.index = -1
        this.title = ""
        this.category = category
        this.values.value?.clear()
        AccountRepository.get().getAllEntries().find {
            it.category == category
        }?.let {  entry ->
            this.values.value?.addAll(entry.values.map { FieldViewModel(it) })
            this.values.value?.forEach { it.value = "" }
        }
    }
}