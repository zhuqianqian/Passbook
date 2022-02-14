package com.z299studio.pbfree.widget

import android.view.WindowManager
import androidx.fragment.app.DialogFragment

abstract class PbDialogFragment : DialogFragment () {

    override fun onStart() {
        super.onStart()
        isCancelable = false
        dialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
    }
}