package com.z299studio.pbfree

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.z299studio.pbfree.databinding.DialogAuthenticateBinding
import com.z299studio.pbfree.viewmodels.AuthSource
import com.z299studio.pbfree.viewmodels.AuthStatus
import com.z299studio.pbfree.viewmodels.AuthenticateViewModel
import com.z299studio.pbfree.widget.PbDialogFragment

class AuthenticateDialog : PbDialogFragment() {

    private lateinit var binding: DialogAuthenticateBinding

    private val authViewModel: AuthenticateViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogAuthenticateBinding.inflate(inflater, container, false)
        binding.auth = authViewModel
        authViewModel.prompt = getString(
            when (authViewModel.source) {
                AuthSource.Export -> R.string.export_provide_password
                AuthSource.Import -> R.string.import_require_password
                AuthSource.Sync -> R.string.sync_require_password
                else -> { R.string.auth_required }
            })
        authViewModel.status.observe(viewLifecycleOwner) {
            when (it) {
                AuthStatus.Accepted -> dismiss()
                AuthStatus.Canceled -> dismiss()
                AuthStatus.Rejected -> binding.passwordEdit.error = getString(R.string.wrong_password)
                else -> {}
            }
        }
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }


}