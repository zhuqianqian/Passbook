package com.z299studio.pbfree

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.viewModels
import com.z299studio.pbfree.data.AccountRepository
import com.z299studio.pbfree.databinding.DialogResetPasswordBinding
import com.z299studio.pbfree.viewmodels.PasswordViewModel
import com.z299studio.pbfree.widget.PbDialogFragment
import java.security.GeneralSecurityException

class ResetPasswordDialog : PbDialogFragment() {

    private lateinit var binding: DialogResetPasswordBinding
    private val viewModel: PasswordViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogResetPasswordBinding.inflate(inflater, container, false)
        binding.cancel.setOnClickListener { dismiss() }
        binding.password = viewModel
        binding.confirmPasswordEdit.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.ok.performClick()
                return@setOnEditorActionListener true
            }
            false
        }
        binding.ok.setOnClickListener {
            try {
                AccountRepository.get().auth(viewModel.current)
                when {
                    viewModel.newPass.isEmpty() -> {
                        binding.newPasswordEdit.error = getString(R.string.no_empty)
                    }
                    viewModel.newPass != viewModel.confirm -> {
                        binding.confirmPasswordEdit.error = getString(R.string.password_not_match)
                    }
                    else -> {
                        AccountRepository.get().setPassword(requireContext(), viewModel.newPass)
                        dismiss()
                    }
                }
            } catch (error: GeneralSecurityException) {
                binding.currentEdit.error = getString(R.string.wrong_password)
            }
        }
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

}