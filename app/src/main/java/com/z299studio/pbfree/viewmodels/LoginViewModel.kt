package com.z299studio.pbfree.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LoginViewModel: ViewModel() {

    var passwordSet = false

    var authOnly = false

    val buttonReady: MutableLiveData<Boolean> = MutableLiveData()

    var password: String = ""
        set(value) {
            field = value
            buttonReady.value = passwordSet && field.isNotEmpty()
        }


    var confirmPassword = ""
        set(value) {
            field = value
            buttonReady.value = !passwordSet && field.isNotEmpty() && password.isNotEmpty()
        }
}