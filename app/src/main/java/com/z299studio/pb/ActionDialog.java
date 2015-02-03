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

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class ActionDialog extends DialogFragment implements View.OnClickListener,
        AdapterView.OnItemSelectedListener, TextWatcher{
    
    public static final int ACTION_AUTHENTICATE = 0;
    public static final int ACTION_EXPORT = 1;
    public static final int ACTION_IMPORT = 2;
    public static final int ACTION_RESET_PWD = 3;
    
    private int mDlgType;
    private String mText;
    private Button mOkButton;
    private int mFileType;
    private EditText[] mPasswordEdits = new EditText[3];
    
    public static ActionDialog create(int type) {
        ActionDialog dialog = new ActionDialog();
        dialog.mDlgType = type;
        return dialog;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState!=null) {
            mDlgType = savedInstanceState.getInt("dialog_type");
            mText = savedInstanceState.getString("dialog_text");
            mFileType = savedInstanceState.getInt("file_type");
        }        
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("dialog_type", mDlgType);
        outState.putString("dialog_text", mText);
        outState.putInt("file_type", mFileType);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView;
        int []layouts =  {R.layout.dialog_authenticate, R.layout.dialog_export,
            R.layout.dialog_import, R.layout.dialog_reset_pwd};
        rootView = inflater.inflate(layouts[mDlgType], container, false);
        mOkButton = (Button)rootView.findViewById(R.id.ok);
        mOkButton.setOnClickListener(this);
        rootView.findViewById(R.id.cancel).setOnClickListener(this);
        if(mDlgType != ACTION_EXPORT) {
            mOkButton.setEnabled(false);
            mOkButton.setAlpha(0.4f);
        }
        if(mDlgType == ACTION_EXPORT || mDlgType == ACTION_IMPORT) {
            Spinner spinner = (Spinner)rootView.findViewById(R.id.spinner);
            ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                    getActivity(), mDlgType == ACTION_EXPORT ? R.array.file_types :
                    R.array.file_types_import, android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(spinnerAdapter);
            spinner.setSelection(mFileType);
            spinner.setOnItemSelectedListener(this);
        }
        if(mDlgType == ACTION_AUTHENTICATE) {
            mPasswordEdits[1] = (EditText)rootView.findViewById(R.id.et_password);
            mPasswordEdits[1].addTextChangedListener(this);
        }
        if(mDlgType == ACTION_RESET_PWD) {
            int ids[] = {R.id.et_cur_pwd, R.id.et_password, R.id.et_confirm};
            for(int i = 0; i < ids.length; ++i) {
                mPasswordEdits[i] = (EditText)rootView.findViewById(ids[i]);
                mPasswordEdits[i].addTextChangedListener(this);
            }
        }
        return rootView;
    }
    
    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.ok:
                if(mDlgType == ACTION_RESET_PWD) {
                    resetPassword();
                }
            case R.id.cancel:
                this.dismiss();
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
            if(mPasswordEdits[1].getText().toString().length() > 0) {
                mOkButton.setEnabled(true);
                mOkButton.setAlpha(1.0f);
            }
            else {
                mOkButton.setEnabled(false);
                mOkButton.setAlpha(0.4f);
            }
            
        }
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
