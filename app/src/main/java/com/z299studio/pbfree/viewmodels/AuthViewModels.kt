package com.z299studio.pbfree.viewmodels

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import android.view.View
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.z299studio.pbfree.R
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

enum class AuthSource {
    None,
    Export,
    Import,
    Sync,
}

enum class AuthStatus {
    Ready,
    Confirmed,
    Canceled,
    Accepted,
    Rejected,
    Working,
}

class AuthenticateViewModel : ViewModel() {

    var source = AuthSource.None

    var password = ""

    var prompt = ""

    val status = MutableLiveData(AuthStatus.Ready)

    val onOk = View.OnClickListener {
        status.value = AuthStatus.Confirmed
    }

    val onCancel = View.OnClickListener {
        status.value = AuthStatus.Canceled
    }
}

class BiometricViewModel : ViewModel() {

    val status = MutableLiveData(AuthStatus.Ready)
    var mode = Cipher.DECRYPT_MODE
    private var errorCode: Int = 0
    var cryptoObject: BiometricPrompt.CryptoObject? = null
        private set
    private var biometricPrompt: BiometricPrompt? = null

    private val callback = object: BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            cryptoObject = result.cryptoObject
            status.value = AuthStatus.Accepted
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            Log.w("Passbook", "BiometricViewModel.onAuthenticationError: $errorCode, $errString")
            onError(errorCode)
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            Log.e("Passbook", "BiometricViewModel.onAuthenticationFailed")
            onError(-1)
        }
    }
    fun init(activity: FragmentActivity): BiometricViewModel {
        val executor = ContextCompat.getMainExecutor(activity)
        biometricPrompt = BiometricPrompt(activity, executor, callback)
        return this
    }

    private fun authenticate(mode: Int, promptInfo: BiometricPrompt.PromptInfo, iv: ByteArray?) {
        this.mode = mode
        status.value = AuthStatus.Working
        initCipher(mode, iv)?.let {
            biometricPrompt?.authenticate(promptInfo, it)
        }
    }

    fun finish() {
        cryptoObject = null
        status.value = AuthStatus.Ready
    }

    private fun onError(errorCode: Int) {
        this.errorCode = errorCode
        if (errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
            this.status.value = AuthStatus.Canceled
        } else {
            this.status.value = AuthStatus.Rejected
        }
    }

    companion object {
        private const val KEY_NAME = "Passbook_KEY"

        @TargetApi(23)
        private fun initCipher(mode: Int, iv: ByteArray?): BiometricPrompt.CryptoObject? {
            try {
                val ivParams: IvParameterSpec
                val keyStore = KeyStore.getInstance("AndroidKeyStore")
                keyStore.load(null)
                val key: SecretKey
                val cipher = Cipher.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES + "/"
                            + KeyProperties.BLOCK_MODE_CBC + "/"
                            + KeyProperties.ENCRYPTION_PADDING_PKCS7
                )
                if (mode == Cipher.ENCRYPT_MODE) {
                    val keyGenerator = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES,
                        "AndroidKeyStore"
                    )
                    keyGenerator.init(
                        KeyGenParameterSpec.Builder(
                            KEY_NAME,
                            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                        ).setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                            .setUserAuthenticationRequired(false)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                            .build()
                    )
                    cipher.init(mode, keyGenerator.generateKey())
                } else {
                    key = keyStore.getKey(KEY_NAME, null) as SecretKey
                    ivParams = IvParameterSpec(iv)
                    cipher.init(mode, key, ivParams)
                }
                return BiometricPrompt.CryptoObject(cipher)
            } catch (e: Exception) {
                Log.e("Passbook", "BiometricViewModel::initCipher error: ", e)
            }
            return null
        }

        fun canBiometricAuth(context: Context): Boolean {
            val biometricManager = BiometricManager.from(context)
            return BiometricManager.BIOMETRIC_SUCCESS == biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
                        or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        }

        fun startBiometricAuth(fragment: Fragment, mode: Int, viewModel: BiometricViewModel, iv: ByteArray? = null) {
            if (canBiometricAuth(fragment.requireContext())) {
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setConfirmationRequired(false)
                    .setTitle(fragment.getString(R.string.fp_title))
                if (mode == Cipher.ENCRYPT_MODE) {
                    promptInfo.setDescription(fragment.getString(R.string.fp_ask))
                } else {
                    promptInfo.setDescription(fragment.getString(R.string.fp_desc))
                }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    promptInfo.setNegativeButtonText(fragment.getString(R.string.fp_use_pwd))
                } else {
                    promptInfo.setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG
                            or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                }
                viewModel.authenticate(mode, promptInfo.build(), iv)
            }
        }
    }
}