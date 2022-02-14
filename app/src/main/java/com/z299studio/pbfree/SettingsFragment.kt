package com.z299studio.pbfree

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.snackbar.Snackbar
import com.z299studio.pbfree.data.AccountRepository
import com.z299studio.pbfree.data.DataProcessor
import com.z299studio.pbfree.tool.DriveSyncService
import com.z299studio.pbfree.tool.SyncStatus
import com.z299studio.pbfree.viewmodels.AuthSource
import com.z299studio.pbfree.viewmodels.AuthStatus
import com.z299studio.pbfree.viewmodels.AuthenticateViewModel
import com.z299studio.pbfree.viewmodels.BiometricViewModel
import com.z299studio.pbfree.viewmodels.ImportExportViewModel
import com.z299studio.pbfree.viewmodels.SyncViewModel
import java.io.FileNotFoundException
import java.security.GeneralSecurityException
import javax.crypto.Cipher

class SettingsFragment : PreferenceFragmentCompat() {

    private val authViewModel: AuthenticateViewModel by activityViewModels()
    private val syncViewModel: SyncViewModel by activityViewModels()
    private val biometricViewModel: BiometricViewModel by activityViewModels()
    private val importExport: ImportExportViewModel by viewModels()

    private val requestPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                when(authViewModel.source) {
                    AuthSource.Import -> requestFileLauncher.launch("*/*")
                    AuthSource.Export -> requestFolderLauncher.launch(null)
                    else -> {}
                }
            }
        }
    private val requestFileLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            MainActivity.ignoreNextPause = true
            if (uri != null) {
                try {
                    val length = DocumentFile.fromSingleUri(requireContext(), uri)?.length()
                    val inputStream = requireContext().contentResolver.openInputStream(uri)
                    importExport.fileContent = length?.toInt()?.let { ByteArray(it) }
                    inputStream?.read(importExport.fileContent)
                    authViewModel.status.value = AuthStatus.Ready
                    authViewModel.source = AuthSource.Import
                    findNavController().navigate(R.id.authenticateDialog)
                    inputStream?.close()
                } catch (error: FileNotFoundException) {
                    Toast.makeText(requireContext(), R.string.import_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    private val requestFolderLauncher: ActivityResultLauncher<Uri> =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            MainActivity.ignoreNextPause = true
            if (uri != null) {
                importExport.uri = uri
                authViewModel.status.value = AuthStatus.Ready
                authViewModel.source = AuthSource.Export
                findNavController().navigate(R.id.authenticateDialog)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        subscribeImportExport()
        syncViewModel.status.observe(viewLifecycleOwner) {
            val enableSync = syncViewModel.connected
            findPreference<SwitchPreferenceCompat>(getString(R.string.key_sync))?.isChecked = enableSync
            if (!enableSync) {
                Snackbar.make(view!!, R.string.sync_enable_failure, Snackbar.LENGTH_LONG).show()
            }
            PreferenceManager.getDefaultSharedPreferences(requireContext()).edit()
                .putBoolean(getString(R.string.key_sync), enableSync).apply()
            findPreference<Preference>(getString(R.string.key_sync_time))?.isVisible = enableSync
            if (it == SyncStatus.Done || it == SyncStatus.Failed) {
                val defaultPref = PreferenceManager.getDefaultSharedPreferences(requireContext())
                findPreference<Preference>(getString(R.string.key_sync_time))?.summary =
                    defaultPref.getString(getString(R.string.key_sync_time), "")
            }
        }
        biometricViewModel.status.observe(viewLifecycleOwner) {
            findPreference<SwitchPreferenceCompat>(getString(R.string.pref_biometric))
                ?.apply {
                    val pref = requireContext().getSharedPreferences(MainActivity.BIO_PREF, Context.MODE_PRIVATE)
                    this.isChecked = pref.getBoolean(getString(R.string.pref_biometric), false)
                }
        }
        return view
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        val defaultPref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        findPreference<Preference>(getString(R.string.key_sync_time))?.apply {
            this.isVisible = defaultPref.getBoolean(getString(R.string.key_sync), false)
            this.summary = defaultPref.getString(getString(R.string.key_sync_time), "")
            this.setOnPreferenceClickListener {
                this.summary = getString(R.string.syncing)
                DriveSyncService.get().create(requireActivity(), getString(R.string.app_name), syncViewModel)
                true
            }
        }
        findPreference<SwitchPreferenceCompat>(getString(R.string.key_sync))?.let {
            it.setOnPreferenceChangeListener { _, newValue ->
                val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
                if (newValue == true) {
                    syncViewModel.intent = DriveSyncService.get()
                        .create(requireActivity(), getString(R.string.app_name), syncViewModel)
                } else {
                    it.isChecked = false
                    pref.edit().putBoolean(getString(R.string.key_sync), false).apply()
                    findPreference<Preference>(getString(R.string.key_sync_time))?.apply {
                        this.isVisible = false
                    }
                }
                false
            }
        }
        findPreference<SwitchPreferenceCompat>(getString(R.string.pref_biometric))?.let {
            if (!BiometricViewModel.canBiometricAuth(requireContext())) {
                it.isVisible = false
                return@let
            }
            val pref = requireContext().getSharedPreferences(MainActivity.BIO_PREF, Context.MODE_PRIVATE)
            it.isChecked = pref.getBoolean(getString(R.string.pref_biometric), false)
            it.setOnPreferenceChangeListener { _, newValue ->
                if (newValue == false) {
                    it.isChecked = false
                    pref.edit().putBoolean(getString(R.string.pref_biometric), false).apply()
                } else {
                    BiometricViewModel.startBiometricAuth(this, Cipher.ENCRYPT_MODE, biometricViewModel)
                }
                false
            }
        }
        findPreference<Preference>(getString(R.string.key_about))?.summary = getString(R.string.version, getVersion())
        findPreference<Preference>(getString(R.string.key_credits))?.setOnPreferenceClickListener {
            AlertDialog.Builder(requireContext()).setTitle(R.string.credits)
                .setMessage(R.string.credit_message)
                .setPositiveButton(android.R.string.ok) { _,_ -> }
                .show()
            true
        }
        findPreference<Preference>(getString(R.string.key_reset_password))?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.resetPasswordDialog)
            true
        }
        findPreference<Preference>(getString(R.string.key_import))?.setOnPreferenceClickListener {
            authViewModel.source = AuthSource.Import
            selectFileIfPermitted(Manifest.permission.READ_EXTERNAL_STORAGE)
            true
        }
        findPreference<Preference>(getString(R.string.key_export))?.setOnPreferenceClickListener {
            authViewModel.source = AuthSource.Export
            selectFileIfPermitted(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            true
        }
    }

    private fun selectFileIfPermitted(permission: String) {
        MainActivity.ignoreNextPause = true
        if (ActivityCompat.checkSelfPermission(requireContext(), permission)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(permission)
        } else {
            when (authViewModel.source) {
                AuthSource.Import -> requestFileLauncher.launch("*/*")
                AuthSource.Export -> requestFolderLauncher.launch(null)
                else -> {}
            }
        }
    }

    private fun subscribeImportExport() {
        authViewModel.status.observe(viewLifecycleOwner) { status ->
            when (status) {
                AuthStatus.Confirmed -> {
                    if (authViewModel.source == AuthSource.Import) {
                        if (importExport.fileContent == null)  {
                            authViewModel.status.value = AuthStatus.Canceled
                            return@observe
                        }
                        try {
                            val parser = DataProcessor.getDataParser(importExport.fileContent!!)
                            val result = parser.parse(authViewModel.password, importExport.fileContent!!)
                            val sets = AccountRepository.get().getAllEntries().toMutableSet()
                            result.first.forEach {
                                if (!sets.contains(it)) {
                                    AccountRepository.get().add(it)
                                    sets.add(it)
                                }
                            }
                            result.second.forEach {
                                AccountRepository.get().addCategory(it)
                            }
                            AccountRepository.get().saveData(requireContext())
                            importExport.fileContent = null
                            authViewModel.status.value = AuthStatus.Accepted
                            Toast.makeText(requireContext(), R.string.import_success, Toast.LENGTH_SHORT).show()
                        } catch (error: GeneralSecurityException) {
                            authViewModel.status.value = AuthStatus.Rejected
                        } catch (error: Exception) {
                            Toast.makeText(requireContext(), R.string.import_failed, Toast.LENGTH_SHORT).show()
                        }
                    }
                    else if (authViewModel.source == AuthSource.Export) {
                        if (importExport.uri == null) {
                            authViewModel.status.value = AuthStatus.Canceled
                            return@observe
                        }
                        try {
                            val docFile = DocumentFile.fromTreeUri(requireContext(), importExport.uri!!)
                                ?.createFile("application/json", EXPORT_FILE_NAME)
                            val outputStream = requireContext().contentResolver.openOutputStream(docFile!!.uri)
                            AccountRepository.get().saveData(outputStream!!, authViewModel.password)
                            outputStream.close()
                            Toast.makeText(
                                requireContext(), getString(
                                    R.string.export_success,
                                    "${importExport.uri!!.path}/${EXPORT_FILE_NAME}"
                                ), Toast.LENGTH_SHORT
                            ).show()
                            importExport.uri = null
                            authViewModel.status.value = AuthStatus.Accepted
                        } catch (ex: Exception) {
                            Toast.makeText(requireContext(), R.string.export_failed, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                AuthStatus.Canceled -> {
                    importExport.uri = null
                    importExport.fileContent = null
                }
                else -> {}
            }
        }
    }

    private fun getVersion(): String {
        return try {
            val context = this.requireContext()
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            "3.0"
        }
    }

    companion object {
        const val EXPORT_FILE_NAME = "passbook-data"
    }
}