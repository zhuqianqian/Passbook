package com.z299studio.pbfree.viewmodels

import android.content.Intent
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.z299studio.pbfree.data.AccountRepository
import com.z299studio.pbfree.data.DataProcessor
import com.z299studio.pbfree.tool.DriveSyncService
import com.z299studio.pbfree.tool.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.lang.IllegalArgumentException

class SyncViewModel : ViewModel(), DriveSyncService.SyncListener {

    var filePath: File? = null

    var hasLocalData = true

    val status = MutableLiveData(SyncStatus.Ready)

    val message = MutableLiveData("")

    var content: ByteArray? = null

    var connected = false
        private set

    var intent: Intent? = null
        set(value) {
            field = value
            if (value != null) {
                status.value = SyncStatus.Preparing
            }
        }

    fun send() {
        filePath?.let { file ->
            status.postValue(SyncStatus.Sending)
            viewModelScope.launch(Dispatchers.IO) {
                DriveSyncService.get().send(file)
            }
        }
    }

    override fun onError(error: Throwable) {
        when (error::class.java) {
            UserRecoverableAuthIOException::class.java -> {
                this.intent = (error as UserRecoverableAuthIOException).intent
            }
            UserRecoverableAuthException::class.java -> {
                this.intent = (error as UserRecoverableAuthException).intent
            }
            else -> {
                status.postValue(SyncStatus.Failed)
            }
        }
    }

    override fun onConnected() {
        super.onConnected()
        connected = true
        this.status.value = SyncStatus.Loading
        viewModelScope.launch(Dispatchers.IO) {
            DriveSyncService.get().read()
        }
    }

    override fun onReceived(data: ByteArray) {
        if (data.isEmpty()) {
            if (hasLocalData) {
                send()
            } else {
                status.postValue(SyncStatus.Done)
            }
            return
        }
        try {
            val parser = DataProcessor.getDataParser(data)
            val appInfo = parser.fileInfo(data)
            Log.i("SyncViewModel", "received data with $appInfo")
            if (AccountRepository.APP_INFO.dataVersion <= appInfo.dataVersion
                && AccountRepository.APP_INFO.saveTime <= appInfo.saveTime) {
                    // remote version is more up-to-date, use it.
                if (AccountRepository.APP_INFO.dataVersion < appInfo.dataVersion) {
                    content = data
                }
                status.postValue(SyncStatus.Done)
            } else if (AccountRepository.APP_INFO.dataVersion > appInfo.dataVersion &&
                    AccountRepository.APP_INFO.saveTime >= appInfo.saveTime) {
                send()
            } else {
                content = data
                status.postValue(SyncStatus.Resolving)
            }
        } catch (error: IllegalArgumentException) {
            status.value = SyncStatus.Failed
            Log.w("SyncViewModel", "data received but it does not look like Passbook data.", error)
        }
    }

    override fun onDataSent() {
        content = null
        status.postValue(SyncStatus.Done)
    }

    override fun onCancel() {
        status.postValue(SyncStatus.Canceled)
    }
}