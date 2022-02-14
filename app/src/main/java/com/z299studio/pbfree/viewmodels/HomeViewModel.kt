package com.z299studio.pbfree.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    var category = MutableLiveData<String?>(null)

    val searchText = MutableLiveData("")

    val empty = MutableLiveData(false)
}