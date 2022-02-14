package com.z299studio.pbfree

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.z299studio.pbfree.databinding.DialogSyncBinding
import com.z299studio.pbfree.tool.DriveSyncService
import com.z299studio.pbfree.tool.SyncStatus
import com.z299studio.pbfree.viewmodels.SyncViewModel

class CloudSyncDialog : BottomSheetDialogFragment() {

    private lateinit var binding: DialogSyncBinding
    private val syncViewModel: SyncViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        isCancelable = false
        binding = DialogSyncBinding.inflate(inflater, container, false)
        binding.ok.setOnClickListener {
            when (syncViewModel.status.value) {
                SyncStatus.Ready -> {
                    syncViewModel.intent = DriveSyncService.get()
                        .create(requireActivity(), getString(R.string.app_name), syncViewModel)
                }
                SyncStatus.Failed -> {
                    cancelSync()
                }
                else -> {
                    if (syncViewModel.content?.isNotEmpty() == true) {
                        restartActivity()
                    } else {
                        dismiss()
                    }
                }
            }
        }
        binding.cancel.setOnClickListener {cancelSync()  }
        syncViewModel.status.observe(viewLifecycleOwner) {
            Log.i("CloudSync", "status changed to $it")
            when(it) {
                SyncStatus.Preparing -> syncViewModel.message.value = getString(R.string.sync_prepare)
                SyncStatus.Loading -> syncViewModel.message.value = getString(R.string.sync_wait)
                SyncStatus.Ready -> syncViewModel.message.value = getString(R.string.sync_ask)
                SyncStatus.Failed -> syncViewModel.message.value = getString(R.string.sync_fail)
                SyncStatus.Done -> {
                    syncViewModel.message.value = if (syncViewModel.content?.isNotEmpty() == true) {
                        getString(R.string.sync_done)
                    } else {
                        getString(R.string.sync_no_data)
                    }
                    PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .edit().putBoolean(getString(R.string.key_sync), true)
                        .apply()
                }
                else -> { }
            }
        }
        binding.sync = syncViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    private fun cancelSync() {
        val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        pref.edit().putBoolean(getString(R.string.key_sync), false).apply()
        syncViewModel.status.value = SyncStatus.Canceled
        dismiss()
    }

    private fun restartActivity() {
        startActivity(Intent(requireContext(), MainActivity::class.java))
        requireActivity().finish()
    }

}