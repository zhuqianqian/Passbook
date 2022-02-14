package com.z299studio.pbfree.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PasswordViewModel : ViewModel() {

    var current: String = ""
        set(value) {
            field = value
            this.enableOk.value = this.newPass.isNotEmpty() && this.confirm.isNotEmpty() && value.isNotEmpty()
        }

    var newPass: String = ""
        set(value) {
            field = value
            this.enableOk.value = this.current.isNotEmpty() && this.confirm.isNotEmpty() && value.isNotEmpty()
        }

    var confirm: String = ""
        set(value) {
            field = value
            this.enableOk.value = this.current.isNotEmpty() && this.newPass.isNotEmpty() && value.isNotEmpty()
        }

    val enableOk = MutableLiveData(false)
}