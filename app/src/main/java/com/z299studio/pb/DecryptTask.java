package com.z299studio.pb;

import android.os.AsyncTask;
import android.util.Log;

import java.security.GeneralSecurityException;

class DecryptTask extends AsyncTask<String, Void, Boolean> {

    interface OnTaskFinishListener {
        void preExecute();
        void onFinished(boolean isSuccessful, AccountManager manager, String pwd,
                        byte[] data, Application.FileHeader header, Crypto crypto);
    }
    private byte[] mData;
    private Application.FileHeader mHeader;
    private OnTaskFinishListener mListener;
    private AccountManager mMgr;
    private Crypto mCrypto;
    private String mPassword;

    DecryptTask(byte[] data, Application.FileHeader header, OnTaskFinishListener listener) {
        super();
        mData = data;
        mHeader = header;
        mListener = listener;
    }

    @Override
    protected void onPreExecute() {
        if(mListener!=null) {
            mListener.preExecute();
        }
    }
    @Override
    protected Boolean doInBackground(String... params) {
        try {
            if(mData!=null) {
                mCrypto = new Crypto();
                int total = mHeader.keyLength + mHeader.ivLength;
                mPassword = params[0];
                mCrypto.setPassword(params[0], mData, mHeader.size, total);
                total += mHeader.size;
                byte[] textData = new byte[mData.length - total];
                System.arraycopy(mData, total, textData, 0, textData.length);
                byte[] text = mCrypto.decrypt(textData);
                mMgr = new AccountManager(new String(text));
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        catch(GeneralSecurityException e) {
            Log.e("Passbook", "GeneralSecurityException caught.");
            return Boolean.FALSE;
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if(mListener != null) {
            mListener.onFinished(result, mMgr, mPassword, mData, mHeader, mCrypto);
        }
    }
}