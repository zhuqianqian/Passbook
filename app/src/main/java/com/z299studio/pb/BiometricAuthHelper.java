package com.z299studio.pb;

import android.annotation.TargetApi;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.Executor;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public final class BiometricAuthHelper extends BiometricPrompt.AuthenticationCallback {

    public interface BiometricListener {
        void onCanceled(boolean isFirstTime);
        void onConfirmed(boolean isFirstTime, byte[] password);
    }

    private BiometricListener mListener;
    private boolean mIsFirstTime;
    private Cipher mCipher;
    private BiometricPrompt.CryptoObject mCryptoObject;
    private BiometricPrompt mBiometricPrompt;

    private static final String KEY_NAME = "Passbook_KEY";

    private String mTitle;
    private String mDesc;
    private String mNegativeButtonText;

    @TargetApi(23)
    private void initCipher(int mode) {
        try {
            IvParameterSpec ivParams;
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            SecretKey key;
            mCipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);

            if(mode == Cipher.ENCRYPT_MODE) {
                KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,
                        "AndroidKeyStore");
                keyGenerator.init(new KeyGenParameterSpec.Builder(KEY_NAME,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                        .setUserAuthenticationRequired(true)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                        .build());
                mCipher.init(mode, keyGenerator.generateKey());
            }
            else {
                key = (SecretKey)keyStore.getKey(KEY_NAME, null);
                ivParams = new IvParameterSpec(Application.getInstance().getFpIv());
                mCipher.init(mode, key, ivParams);
            }
            mCryptoObject = new BiometricPrompt.CryptoObject(mCipher);
        }
        catch(KeyStoreException | CertificateException | UnrecoverableKeyException | IOException
                | NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException
                | InvalidAlgorithmParameterException | NoSuchPaddingException e) {
            Log.e("Pb:FingerprintDialog", "Runtime error in initCipher.");
            Log.e("Pb:FingerprintDialog", e.toString());
        }
    }

    public BiometricAuthHelper(boolean isFirstTime, BiometricListener listener, FragmentActivity context) {
        Executor executor = ContextCompat.getMainExecutor(context);
        mBiometricPrompt = new BiometricPrompt(context, executor, this);
        this.mIsFirstTime = isFirstTime;
        mTitle = context.getString(R.string.fp_title);
        initCipher(mIsFirstTime ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE);
        if (mIsFirstTime) {
            mDesc = context.getString(R.string.fp_ask);
            mNegativeButtonText = context.getString(android.R.string.cancel);
        } else {
            mDesc = context.getString(R.string.fp_desc);
            mNegativeButtonText = context.getString(R.string.fp_use_pwd);
        }
        this.mListener = listener;
    }

    public void authenticate() {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(mTitle)
                .setDescription(mDesc)
                // Authenticate without requiring the user to press a "confirm"
                // button after satisfying the biometric check
                .setConfirmationRequired(false)
                .setNegativeButtonText(mNegativeButtonText)
                .build();
        mBiometricPrompt.authenticate(promptInfo, mCryptoObject);
    }

    @Override
    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
        super.onAuthenticationError(errorCode, errString);
        if (errorCode == BiometricPrompt.ERROR_CANCELED && mListener != null) {
            mListener.onCanceled(mIsFirstTime);
        }
    }

    @Override
    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
        super.onAuthenticationSucceeded(result);
        Application app = Application.getInstance();
        try {
            if (mIsFirstTime) {
                app.setFpData(mCryptoObject.getCipher().doFinal(app.getPassword().getBytes()),
                        mCipher.getIV());
                mListener.onConfirmed(true, null);
            }
            else {
                mListener.onConfirmed(false, mCryptoObject.getCipher().doFinal(app.getFpData()));
            }
        }
        catch (BadPaddingException | IllegalBlockSizeException e) {
            Log.e("Pb:FingerprintDialog", "Runtime error during encryption/decryption.");
            Log.e("Pb:FingerprintDialog", e.toString());
        }
    }

    @Override
    public void onAuthenticationFailed() {
        super.onAuthenticationFailed();
    }
}
