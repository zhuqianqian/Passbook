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

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class ActionDialog extends DialogFragment implements View.OnClickListener,
        AdapterView.OnItemSelectedListener, TextWatcher{
    
    public static final int REQ_CODE_FILE_SELECTION = 0x299;
    
    public interface ActionDialogListener {
        void onConfirm(String text, int type, int operation, int option);
    }
    
    public static final int ACTION_AUTHENTICATE = 0;
    public static final int ACTION_EXPORT = 1;
    public static final int ACTION_IMPORT = 2;
    public static final int ACTION_RESET_PWD = 3;
    public static final int ACTION_CREDITS = 4;
    public static final int ACTION_LICENSE = 5;
    public static final int ACTION_AUTHENTICATE2 = 6;

    private Handler mHandler = new Handler();
    private final Runnable mUpdateUi = new Runnable() {
        @Override
        public void run() {
            if(mText != null) {
                mOkButton.setEnabled(true);
                mSelectButton.setText(mText);
                mOkButton.setAlpha(1.0f);
            }
        }
    };
    
    private int mDlgType;
    private String mText;
    private Button mOkButton;
    private int mFileType;
    private EditText[] mPasswordEdits = new EditText[3];
    private ActionDialogListener mListener;
    private int mOption;
    private Button mSelectButton;
    
    public static ActionDialog create(int type) {
        ActionDialog dialog = new ActionDialog();
        dialog.mDlgType = type;
        return dialog;
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (ActionDialogListener)activity;
        }
        catch (ClassCastException e) {   }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState!=null) {
            mDlgType = savedInstanceState.getInt("dialog_type");
            mText = savedInstanceState.getString("dialog_text");
            mFileType = savedInstanceState.getInt("file_type");
            mOption = savedInstanceState.getInt("import_option");
        }        
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("dialog_type", mDlgType);
        outState.putString("dialog_text", mText);
        outState.putInt("file_type", mFileType);
        outState.putInt("import_option", mOption);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(Application.getInstance() == null
                || Application.getInstance().getAccountManager() == null) {
            return null;
        }
        View rootView;
        int []layouts =  {R.layout.dialog_authenticate, R.layout.dialog_export,
            R.layout.dialog_import, R.layout.dialog_reset_pwd, R.layout.dialog_credits,
            R.layout.dialog_license, R.layout.dialog_authenticate};
        rootView = inflater.inflate(layouts[mDlgType], container, false);
        mOkButton = (Button)rootView.findViewById(R.id.ok);
        if(mOkButton != null) {
            mOkButton.setOnClickListener(this);
        }
        View cancel = rootView.findViewById(R.id.cancel);
        if(cancel!=null) {
            cancel.setOnClickListener(this);
        }
        if(mDlgType != ACTION_EXPORT && mOkButton != null) {
            mOkButton.setEnabled(false);
            mOkButton.setAlpha(0.4f);
        }
        switch(mDlgType) {
        case ACTION_EXPORT:
        case ACTION_IMPORT:
            Spinner spinner = (Spinner) rootView.findViewById(R.id.spinner);
            ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                    getActivity(), mDlgType == ACTION_EXPORT ? R.array.file_types :
                            R.array.file_types_import, android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(spinnerAdapter);
            spinner.setSelection(mFileType);
            spinner.setOnItemSelectedListener(this);
            if(mDlgType == ACTION_IMPORT) {
                int ids[] = {R.id.ignore, R.id.keepall, R.id.overwrite};
                for (int i = 0; i < ids.length; ++i) {
                    RadioButton rb = (RadioButton) rootView.findViewById(ids[i]);
                    rb.setOnClickListener(this);
                    if (i == mOption) {
                        rb.setChecked(true);
                    }
                }
                mSelectButton = (Button) rootView.findViewById(R.id.select);
                mSelectButton.setOnClickListener(this);
            }
            break;
        case ACTION_AUTHENTICATE2:
            ((TextView)rootView.findViewById(R.id.auth_desc)).setText(R.string.diff_pwd);
            ((Button)rootView.findViewById(R.id.cancel)).setText(R.string.discard);
            // no break to use the logic of authenticate
        case ACTION_AUTHENTICATE:
            mPasswordEdits[1] = (EditText) rootView.findViewById(R.id.et_password);
            mPasswordEdits[1].addTextChangedListener(this);
            break;
        
        case ACTION_RESET_PWD:
            int ids[] = {R.id.et_cur_pwd, R.id.et_password, R.id.et_confirm};
            for (int i = 0; i < ids.length; ++i) {
                mPasswordEdits[i] = (EditText) rootView.findViewById(ids[i]);
                mPasswordEdits[i].addTextChangedListener(this);
            }
            break;
        
        case ACTION_CREDITS:
            rootView.findViewById(R.id.cancel).setOnClickListener(this);
            TextView tv = (TextView) rootView.findViewById(R.id.testers);
            tv.setText(R.string.cc_yhc);
            tv = (TextView) rootView.findViewById(R.id.translators);
            String translators = String.format(Locale.getDefault(),
                    "%s: %s\n%s: %s\n%s: %s",
                    getString(R.string.lang_fr), getString(R.string.cc_xcx),
                    getString(R.string.lang_es), getString(R.string.cc_jh),
                    getString(R.string.lang_zhtw), getString(R.string.cc_qqz));
            tv.setText(translators);
            break;
        
        case ACTION_LICENSE:
            WebView wv = (WebView) rootView.findViewById(R.id.licence_page);
            wv.loadUrl("file:///android_asset/licence.html");
            break;
        }
        return rootView;
    }
    
    @Override
    public void onStart() {
        super.onStart();
        if(mDlgType == ACTION_LICENSE) {
            WindowManager.LayoutParams lp = getDialog().getWindow().getAttributes();
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            getDialog().getWindow().setAttributes(lp);
        }
        else if(mDlgType == ACTION_AUTHENTICATE2) {
            getDialog().setCanceledOnTouchOutside(false);
        }
    }
    
    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.ok:
                if(mDlgType == ACTION_RESET_PWD) {
                    resetPassword();
                }
                else {
                    mListener.onConfirm(mText, mFileType, mDlgType, mOption);
                }
                this.dismiss();
                break;
            case R.id.cancel:
                if(mDlgType == ACTION_AUTHENTICATE2) {
                    mListener.onConfirm(null, 0, mDlgType, mOption);
                }
                this.dismiss();
                break;     
            case R.id.ignore:
                mOption = ImportExportTask.OPTION_IGNORE;
                break;
            case R.id.keepall:
                mOption = ImportExportTask.OPTION_KEEPALL;
                break;
            case R.id.overwrite:
                mOption = ImportExportTask.OPTION_OVERWRITE;
                break;
            case R.id.select:
                showFileChooser();
                break;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mFileType = position;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {   }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) { }

    @Override
    public void afterTextChanged(Editable s) {
        if(mDlgType == ACTION_RESET_PWD) {
            if(mPasswordEdits[0].getText().toString().length() > 0 &&
               mPasswordEdits[1].getText().toString().length() > 0 &&
               mPasswordEdits[2].getText().toString().length() > 0)  {
                mOkButton.setEnabled(true);
                mOkButton.setAlpha(1.0f);
            }
            else {
                mOkButton.setEnabled(false);
                mOkButton.setAlpha(0.4f);
            }
        }
        else {
            mText = mPasswordEdits[1].getText().toString();
            if(mText.length() > 0) {
                mOkButton.setEnabled(true);
                mOkButton.setAlpha(1.0f);
            }
            else {
                mOkButton.setEnabled(false);
                mOkButton.setAlpha(0.4f);
            }
            
        }
    }

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            getActivity().startActivityForResult(
                    Intent.createChooser(intent, getResources().getString(R.string.select_file)),
                    REQ_CODE_FILE_SELECTION);
        }
        catch (android.content.ActivityNotFoundException ex) {
            Application.showToast(getActivity(), R.string.no_file_explorer, Toast.LENGTH_SHORT);
        }
    }

    public void onFileSelected(Settings context, int resultCode, Intent data) {
        if(resultCode == Settings.RESULT_OK) {
            Uri uri = data.getData();
            if(uri!=null) {
                mText = getFilePath(context, uri);
                if(mText!=null) {
                    mHandler.post(mUpdateUi);
                    return;
                }
            }
        }
        Application.showToast(context, R.string.invalid_file, Toast.LENGTH_LONG);
    }

    private String getFilePath(Activity context, Uri uri){
        String docId;
        Uri contentUri;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT  &&
                DocumentsContract.isDocumentUri(context, uri)){
            if("com.android.externalstorage.documents".equals(uri.getAuthority())) {
                docId = DocumentsContract.getDocumentId(uri);
                final String split[] = docId.split(":");
                if("primary".equalsIgnoreCase(split[0])) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            else if("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                docId = DocumentsContract.getDocumentId(uri);
                contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads//public_downloads"), Long.valueOf(docId));
                return getDataColumn(context, contentUri, null, null);
            }
        }
        else if(uri.getScheme().equalsIgnoreCase("content")) {
            return  getDataColumn(context, uri, null, null);
        }
        else if(uri.getScheme().equalsIgnoreCase("file")){
            return uri.getPath();
        }
        return null;
    }

    private String getDataColumn(Activity context, Uri uri, String selection,
                                 String[] selectionArgs) {

        Cursor cursor = null;
        final String[] projection = {MediaStore.Images.Media.DATA};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(projection[0]);
                return cursor.getString(columnIndex);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    protected void resetPassword() {
        Application app = Application.getInstance();
        String current = mPasswordEdits[0].getText().toString();
        if(app.getPassword().equals(current)) {
            String pwd = mPasswordEdits[1].getText().toString();
            String confirm = mPasswordEdits[2].getText().toString();
            if(pwd.equals(confirm)) {
                app.setPassword(pwd, true);
                app.saveData();
                Application.showToast(getActivity(), R.string.pwd_changed, Toast.LENGTH_SHORT);
                mListener.onConfirm(pwd, 0, mDlgType, 0);
                this.dismiss();
            }
            else {
                mPasswordEdits[1].setText("");
                mPasswordEdits[2].setText("");
                Application.showToast(getActivity(), R.string.pwd_unmatch, Toast.LENGTH_SHORT);
            }
        }
        else {
            mPasswordEdits[0].setText("");
            Application.showToast(getActivity(), R.string.pwd_wrong, Toast.LENGTH_SHORT);
        }
    }
}
