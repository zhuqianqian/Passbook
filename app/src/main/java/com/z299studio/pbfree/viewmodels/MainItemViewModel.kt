package com.z299studio.pbfree.viewmodels

import androidx.lifecycle.ViewModel
import com.z299studio.pbfree.data.Entry
import com.z299studio.pbfree.data.ValueType
import java.net.URI

data class MainItemViewModel(val index: Int, val entry: Entry) : ViewModel() {

    val iconUrl = entry.values.filter { valueTuple -> valueTuple.type == ValueType.Url }
        .map { valueTuple -> iconUrl(valueTuple.value) }.elementAtOrNull(0)

    val title: String = when {
        entry.key.isNotBlank() -> entry.key
        iconUrl.isNullOrBlank() -> "#"
        else -> {
            val host = URI(iconUrl).host
            if (host.startsWith("www.")) {
                host.substring(4)
            } else {
                host
            }
        }
    }

    val iconText = title.take(1)
    var lastRow = false


    private fun iconUrl(url: String?): String? {
        if (url == null || url.isBlank()) {
            return null
        }
        val iconUrl: String =
            if (url.startsWith("https://") || url.startsWith("http://")) { url }
            else { "https://$url"}
        return if (iconUrl.endsWith("/")) { iconUrl + "favicon.ico" }
        else { "$iconUrl/favicon.ico" }
    }
}