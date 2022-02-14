package com.z299studio.pbfree

import android.content.Context
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView.OnEditorActionListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.z299studio.pbfree.data.AccountRepository
import com.z299studio.pbfree.databinding.FragmentLoginBinding
import com.z299studio.pbfree.tool.SyncStatus
import com.z299studio.pbfree.viewmodels.AuthStatus
import com.z299studio.pbfree.viewmodels.BiometricViewModel
import com.z299studio.pbfree.viewmodels.LoginViewModel
import com.z299studio.pbfree.viewmodels.SyncViewModel
import java.security.GeneralSecurityException
import javax.crypto.Cipher

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val loginViewModel: LoginViewModel by viewModels()
    private val biometricViewModel: BiometricViewModel by activityViewModels()
    private val syncViewModel: SyncViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        loginViewModel.passwordSet = AccountRepository.get().hasData(requireContext())
        syncViewModel.hasLocalData = loginViewModel.passwordSet
        binding.lifecycleOwner = viewLifecycleOwner
        loginViewModel.authOnly = LoginFragmentArgs.fromBundle(requireArguments()).authOnly
        binding.login = loginViewModel
        binding.passwordEdit.imeOptions = if (loginViewModel.passwordSet) {
            EditorInfo.IME_ACTION_DONE
        } else {
            EditorInfo.IME_ACTION_NEXT
        }
        val unlockOrInitialize = View.OnClickListener {
            unlockOrInitialize()
        }
        binding.unlock.setOnClickListener(unlockOrInitialize)
        val editorActionListener = OnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE) {
                unlockOrInitialize()
                return@OnEditorActionListener true
            }
            return@OnEditorActionListener false
        }
        if (loginViewModel.passwordSet) {
            binding.passwordEdit.setOnEditorActionListener(editorActionListener)
            startBiometricAuth(Cipher.DECRYPT_MODE)
        } else {
            binding.confirmPasswordEdit.setOnEditorActionListener(editorActionListener)
            if (syncViewModel.status.value == SyncStatus.Ready) {
                findNavController().navigate(R.id.nav_sync_ask)
            }
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onAuthenticated() {
        try {
            val pref = requireActivity().getSharedPreferences(MainActivity.BIO_PREF, Context.MODE_PRIVATE)
            biometricViewModel.cryptoObject?.let {
                val encrypted = Base64.decode(pref.getString(getString(R.string.bio_data), ""), Base64.DEFAULT)
                val password = it.cipher?.doFinal(encrypted)
                    ?.let { decrypted -> String(decrypted) } ?: ""
                if (loginViewModel.authOnly) {
                    AccountRepository.get().auth(password)
                } else {
                    AccountRepository.get().load(requireContext(), password)
                }
                findNavController().navigate(LoginFragmentDirections.action2Home(true))
            }
        } catch (error: GeneralSecurityException) {
            // fallback to password
            return
        }
    }

    private fun unlockOrInitialize() {
        if (loginViewModel.password.isEmpty()) {
            binding.passwordEdit.error = getString(R.string.no_empty)
            return
        }
        if (loginViewModel.passwordSet) {
            try {
                if (loginViewModel.authOnly) {
                    AccountRepository.get().auth(loginViewModel.password)
                } else {
                    AccountRepository.get().load(requireContext(), loginViewModel.password)
                }
                startBiometricAuth(Cipher.ENCRYPT_MODE)
            } catch (error: GeneralSecurityException) {
                binding.passwordEdit.error = getString(R.string.wrong_password)
                return
            }
        } else {
            if (loginViewModel.password != loginViewModel.confirmPassword) {
                binding.confirmPasswordEdit.error = getString(R.string.password_not_match)
                return
            }
            AccountRepository.get().setPassword(requireContext(), loginViewModel.password)
            startBiometricAuth(Cipher.ENCRYPT_MODE)
        }
        findNavController().navigate(LoginFragmentDirections.action2Home(true))
    }

    private fun startBiometricAuth(mode: Int) {
        biometricViewModel.status.observe(viewLifecycleOwner) {
            if (it == AuthStatus.Accepted && mode == Cipher.DECRYPT_MODE) {
                onAuthenticated()
            }
        }
        if (biometricViewModel.status.value == AuthStatus.Working) {
            return
        }
        val pref = requireContext().getSharedPreferences(MainActivity.BIO_PREF, Context.MODE_PRIVATE)
        val hasAsked = pref.contains(getString(R.string.pref_biometric))
        if ((hasAsked && mode == Cipher.ENCRYPT_MODE)
            || (mode == Cipher.DECRYPT_MODE && !pref.getBoolean(getString(R.string.pref_biometric), false))) {
            return
        }
        var iv: ByteArray? = null
        if (mode == Cipher.DECRYPT_MODE) {
            val ivStr = pref.getString(getString(R.string.bio_iv), "")
            iv = Base64.decode(ivStr, Base64.DEFAULT)
        }
        BiometricViewModel.startBiometricAuth(this, mode, biometricViewModel, iv)
    }
}