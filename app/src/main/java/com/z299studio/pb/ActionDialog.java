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
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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

public class ActionDialog extends DialogFragment implements View.OnClickListener,
        AdapterView.OnItemSelectedListener, TextWatcher{
    
    public static final int REQ_CODE_FILE_SELECTION = 0x299;
    private static final String TAG_DIALOG = "action_dialog";
    
    public interface ActionDialogListener {
        public void onConfirm(String text, int type, int operation, int option);
    }
    
    public static final int ACTION_AUTHENTICATE = 0;
    public static final int ACTION_EXPORT = 1;
    public static final int ACTION_IMPORT = 2;
    public static final int ACTION_RESET_PWD = 3;
    public static final int ACTION_ABOUT = 4;
    public static final int ACTION_LICENSE = 5;

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
        View rootView;
        int []layouts =  {R.layout.dialog_authenticate, R.layout.dialog_export,
            R.layout.dialog_import, R.layout.dialog_reset_pwd, R.layout.dialog_about,
            R.layout.dialog_license};
        rootView = inflater.inflate(layouts[mDlgType], container, false);
        mOkButton = (Button)rootView.findViewById(R.id.ok);
        if(mOkButton != null) {
            mOkButton.setOnClickListener(this);
        }
        View cancel = rootView.findViewById(R.id.cancel);
        if(cancel!=null) {
            cancel.setOnClickListener(this);
        }
        switch(mDlgType) {
        case ACTION_EXPORT:
            mOkButton.setEnabled(false);
            mOkButton.setAlpha(0.4f);
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
        
        case ACTION_ABOUT:
            rootView.findViewById(R.id.rate).setOnClickListener(this);
            rootView.findViewById(R.id.licence).setOnClickListener(this);
            TextView tv = (TextView) rootView.findViewById(R.id.about);
            Activity context = getActivity();
            String versionName;
            try {
                versionName = context.getPackageManager()
                        .getPackageInfo(context.getPackageName(), 0)
                        .versionName;
            } catch (PackageManager.NameNotFoundException e) {
                versionName = "2.0.0";
            }
            tv.setText(getString(R.string.app_about, getString(R.string.app_name), versionName));
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
    }
    
    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.ok:
                if(mDlgType == ACTION_RESET_PWD) {
                    resetPassword();
                }else {
                    mListener.onConfirm(mText, mFileType, mDlgType, mOption);
                }
            case R.id.cancel:
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
            case R.id.rate:
                this.dismiss();
                Uri uri = Uri.parse("market://details?id=" + getActivity().getPackageName());
                Intent rateIntent = new Intent(Intent.ACTION_VIEW, uri);
                try { startActivity(rateIntent); } 
                catch (ActivityNotFoundException e) { }
                break;
            case R.id.licence:
                this.dismiss();
                ActionDialog.create(ACTION_LICENSE).show(getFragmentManager(), TAG_DIALOG);
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
                if(uri.getScheme().equalsIgnoreCase("file")){
                    mText = uri.getPath();
                    mHandler.post(mUpdateUi);
                    return;
                }
                else if(uri.getScheme().equalsIgnoreCase("content")) {
                    String[] projection = {"_data"};
                    Cursor cursor;
                    try {
                        cursor = context.getContentResolver().query(uri, projection, null, null, null);
                        int column_index = cursor.getColumnIndexOrThrow("_data");
                        if(cursor.moveToFirst()) {
                            mText = cursor.getString(column_index);
                            mHandler.post(mUpdateUi);
                            return;
                        }
                    }
                    catch(Exception e) { }
                }
            }
        }
        Application.showToast(context, R.string.invalid_file, Toast.LENGTH_LONG);
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
