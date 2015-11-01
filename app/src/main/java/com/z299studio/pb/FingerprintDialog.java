/*
* Copyright 2015 Qianqian Zhu <zhuqianqian.299@gmail.com> All rights reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.z299studio.pb;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidParameterSpecException;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

@TargetApi(Build.VERSION_CODES.M)
public class FingerprintDialog extends DialogFragment implements View.OnClickListener,
        FingerprintUiHelper.Callback{
    
    public interface FingerprintListener{
        void onCanceled(boolean isFirstTime);
        void onConfirmed(boolean isFirstTime, byte[] password);
    }

    private static final String KEY_FILE = "PBFPK";
    private static final String IV_FILE = "PBFPI";

    private FingerprintListener mListener;
    private boolean mIsFirstTime;

    private Cipher mCipher;
    private FingerprintManager.CryptoObject mCryptoObject;
    FingerprintUiHelper mFingerprintUiHelper;

    private static final String KEY_NAME = "Passbook_KEY";
    
    public static FingerprintDialog build(boolean isFirstTime) {
        FingerprintDialog dialog = new FingerprintDialog();
        dialog.mIsFirstTime = isFirstTime ;
        return dialog;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }
    
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (FingerprintListener) context;
        }catch (ClassCastException e) {
            Log.e("Pb:FingerprintDialog",
                    "Activity must implement OnOptionSelected interface");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dialog_fingerprint, container, false);
        if(!mIsFirstTime) {
            ((TextView)rootView.findViewById(R.id.fp_desc)).setText(R.string.fp_confirm);
        }
        rootView.findViewById(R.id.cancel).setOnClickListener(this);
        initCipher(mIsFirstTime ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE);
        FingerprintManager fpManager = (FingerprintManager)getContext().
                getSystemService(Context.FINGERPRINT_SERVICE);
        mFingerprintUiHelper = FingerprintUiHelper.build(fpManager,
                (ImageView)rootView.findViewById(R.id.fp_icon),
                (TextView)rootView.findViewById(R.id.fp_area), this);
        return rootView;
    }

    @Override
    public void onClick(View view) {
        if(view.getId() ==  R.id.cancel) {
            dismiss();
            mListener.onCanceled(mIsFirstTime);
            if(mIsFirstTime) {
                Application.Options.mEnableFingerprint = C.fpDisabled;
                Application.getInstance().mSP.edit().putInt(C.Keys.ENABLE_FP, C.fpDisabled).apply();
            }
        }
    }

    private void initCipher(int mode) {
        try {
            byte[] iv;
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
                File file = new File(getContext().getFilesDir()+"/"+IV_FILE);
                int fileSize = (int)file.length();
                iv = new byte[fileSize];
                FileInputStream fis = getContext().openFileInput(IV_FILE);
                fis.read(iv, 0, fileSize);
                fis.close();
                ivParams = new IvParameterSpec(iv);
                mCipher.init(mode, key, ivParams);
            }
            mCryptoObject = new FingerprintManager.CryptoObject(mCipher);
        }
        catch(KeyStoreException | CertificateException | UnrecoverableKeyException | IOException
                | NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException
                | InvalidAlgorithmParameterException | NoSuchPaddingException e) {
            Log.e("Pb:FingerprintDialog", "Runtime error in initCipher.");
            Log.e("Pb:FingerprintDialog", e.toString());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mFingerprintUiHelper.startListening(mCryptoObject);
    }

    @Override
    public void onPause() {
        super.onPause();
        mFingerprintUiHelper.stopListening();
    }

    @Override
    public void onAuthenticated() {
        byte[] data;
        try {
            if (mIsFirstTime) {
                data = mCryptoObject.getCipher().doFinal(
                       Application.getInstance().getPassword().getBytes());
                FileOutputStream fos = getContext().openFileOutput(KEY_FILE, Context.MODE_PRIVATE);
                fos.write(data);
                fos.close();
                IvParameterSpec ivParams = mCipher.getParameters()
                        .getParameterSpec(IvParameterSpec.class);
                data = ivParams.getIV();
                fos = getContext().openFileOutput(IV_FILE, Context.MODE_PRIVATE);
                fos.write(data);
                fos.close();
                Application.Options.mEnableFingerprint = C.fpEnabled;
                Application.getInstance().mSP.edit().putInt(C.Keys.ENABLE_FP, C.fpEnabled).apply();
                mListener.onConfirmed(true, null);
            }
            else {
                File file = new File(getContext().getFilesDir()+"/"+KEY_FILE);
                int fileSize = (int)file.length();
                data = new byte[fileSize];
                FileInputStream fis = getContext().openFileInput(KEY_FILE);
                fis.read(data, 0, fileSize);
                fis.close();
                mListener.onConfirmed(false, mCryptoObject.getCipher().doFinal(data));
            }

        }
        catch (BadPaddingException | IllegalBlockSizeException | IOException |
                InvalidParameterSpecException e) {
            Log.e("Pb:FingerprintDialog", "Runtime error during encryption/decryption.");
            Log.e("Pb:FingerprintDialog", e.toString());
        }
        dismiss();
    }

    @Override
    public void onError() {
    }
}
