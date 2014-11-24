/*
* Copyright 2014 Qianqian Zhu <zhuqianqian.299@gmail.com> All rights reserved.
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
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class HomeActivity extends Activity {

    protected Application mApp;
    protected EditText mPwdEdit;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mApp = Application.getInstance(this);
        this.setTheme(C.THEMES[Application.Options.mTheme]);
        super.onCreate(savedInstanceState);
        if(Application.Options.mTour == false){
            Intent intent = new Intent(this, TourActivity.class);
            intent.putExtra(C.Names.ACTIVITY, C.Activity.HOME);
            this.startActivity(intent);
            this.finish();
            return;
        }
        setContentView(R.layout.activity_home);
        if(mApp.hasDataFile()) {
            HomeFragment.mLayout = R.layout.fragment_startup;
        }
        else {
            HomeFragment.mLayout = R.layout.fragment_home;
        }
        getFragmentManager().beginTransaction().add(R.id.container,
            new HomeFragment()).commit();
    }
    
    @Override
    protected void onResume() {
        super.onStart();
        popInput();
    }
    
    @Override
    protected void onPause() {
        if(mPwdEdit!=null) {
            mPwdEdit.clearFocus();
            InputMethodManager imm = (InputMethodManager)getSystemService(
                      Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mPwdEdit.getWindowToken(), 0);
        }
        super.onPause();
    }
    
    public void onConfirm(View view) {
        
    }
    
    public void onSyncChoosed(View view) {
        
    }
    
    private void popInput() {
        mPwdEdit = (EditText)findViewById(R.id.et_password);
        if(mPwdEdit!=null) {
            mPwdEdit.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mPwdEdit.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(mPwdEdit, InputMethodManager.SHOW_FORCED);
                }                
            }, 100);
            
            OnEditorActionListener eal = new OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView edit, int id, KeyEvent event) {
                    if(id == EditorInfo.IME_ACTION_DONE) {
                        onConfirm(null);
                        return true;
                    }
                    return false;
                }
            };
            mPwdEdit.setOnEditorActionListener(eal);
            EditText et_confirm = (EditText)findViewById(R.id.et_confirm);
            if(et_confirm !=null) {
                et_confirm.setOnEditorActionListener(eal);
            }
        }        
    }
    
    private static class HomeFragment extends Fragment {
        public static int mLayout;
        
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(mLayout, container, false);
            return rootView;
        }
    }
}
