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
import android.widget.Toast;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

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

    private FingerprintListener mListener;
    private boolean mIsFirstTime;

    private Cipher mCipher;
    private FingerprintManager.CryptoObject mCryptoObject;
    FingerprintUiHelper mFingerprintUiHelper;

    private static final String KEY_NAME = "Passbook_KEY";
    
    public static FingerprintDialog build(boolean isFirstTime) {
        FingerprintDialog dialog = new FingerprintDialog();
        dialog.mIsFirstTime = isFirstTime ;
        dialog.initCipher(isFirstTime ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE);
        if(dialog.mCryptoObject == null) {
            Application.getInstance().resetFpData();
            return null;
        }
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
                    "Activity must implement FingerprintListener interface");
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
                Application.getInstance().clearFpData();
            }
        }
    }

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
        if(mFingerprintUiHelper!=null) {
            mFingerprintUiHelper.startListening(mCryptoObject);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mFingerprintUiHelper!=null) {
            mFingerprintUiHelper.stopListening();
        }
    }

    @Override
    public void onAuthenticated() {
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
        dismiss();
    }

    @Override
    public void onError() {
    }
}
