package com.z299studio.pbfree.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel

class ImportExportViewModel : ViewModel() {

    var fileContent: ByteArray? = null

    var uri: Uri? = null
}