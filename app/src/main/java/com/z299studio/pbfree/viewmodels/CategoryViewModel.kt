package com.z299studio.pbfree.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.z299studio.pbfree.MainActivity
import com.z299studio.pbfree.data.Category

class CategoryViewModel() : ViewModel() {

    constructor(category: Category) : this() {
        this.name.value = category.name
        this.icon = category.icon
        this.iconRes.value = MainActivity.getIcon(category.icon)
    }

    fun toCategory(): Category {
        return Category(this.name.value ?: "", this.icon)
    }

    val name = MutableLiveData("")
    var icon = MainActivity.DEFAULT_ICON
        set(value) {
            field = if (value < 0) { MainActivity.DEFAULT_ICON } else { value }
            iconRes.value = MainActivity.getIcon(field)
        }

    val iconRes = MutableLiveData(MainActivity.getIcon(icon))
}