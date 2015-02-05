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

import java.security.GeneralSecurityException;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class HomeActivity extends ActionBarActivity implements
AnimatorListener, SyncService.SyncListener{
    protected Application mApp;
    protected EditText mPwdEdit;
    protected TextView mSyncText;
    protected View mButtonContainer;
    private static int mStage;
    private static final int SELECT_SYNC = 0;
    private static final int LOADING = 1;
    private static final int SET_PWD = 2;
    private static final int AUTH = 3;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mApp = Application.getInstance(this);
        this.setTheme(C.THEMES[Application.Options.mTheme]);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        int[] primaryColors = {R.attr.colorPrimary, R.attr.colorPrimaryDark,
                R.attr.colorAccent, R.attr.textColorNormal, R.attr.iconColorNormal};
        TypedArray ta = obtainStyledAttributes(primaryColors);
        for(int i = 0; i < C.ThemedColors.length; ++i) {
            C.ThemedColors[i] = ta.getColor(i, 0);
        }
        ta.recycle();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            View v = findViewById(R.id.activity_root);
            v.setBackgroundColor(C.ThemedColors[C.colorPrimary]);
        }
        if(savedInstanceState==null) {
            if(!mApp.hasDataFile()) {
                mStage = SELECT_SYNC;
            }
            else {
                mStage = AUTH;
            }
            getSupportFragmentManager().beginTransaction().add(R.id.container,
                HomeFragment.create()).commit();
        }
        else {
            mStage = savedInstanceState.getInt("home_stage");
        }
        if(Application.Options.mTour == false){
            Intent intent = new Intent(this, TourActivity.class);
            intent.putExtra(C.ACTIVITY, C.Activity.HOME);
            this.startActivity(intent);
            this.finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        if(mStage == LOADING) {
            startSync(false);
        }
        else {
            mPwdEdit = (EditText)findViewById(R.id.password);
        }
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("home_stage", mStage);
        super.onSaveInstanceState(outState);
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
        String password = mPwdEdit.getText().toString();
        if(mStage == SET_PWD) {
            EditText et_confirm = (EditText) findViewById(R.id.confirm);
            if(password.equals(et_confirm.getText().toString())) {
                mApp.setPassword(password, true);
                String[] defCategories = getResources().getStringArray(R.array.category_names);
                int i = 0;
                AccountManager am = AccountManager.getInstance(null);
                for(String s : defCategories) {
                    am.addCategory(i++, s);
                }
                startMain();
            }
            else {
                et_confirm.setText("");
                mPwdEdit.setText("");
                Application.showToast(this, R.string.pwd_unmatch, Toast.LENGTH_SHORT);
                return;
            }
        }
        else {
            new DecryptTask().execute(password);
        }
    }
    
    public void onSyncSelected(View view) {
        int id = view.getId();
        switch(id) {
        case R.id.btn_gdrive:
        case R.id.btn_gpg:
            mStage = LOADING;
            Application.Options.mSync = id == R.id.btn_gdrive ? C.Sync.GDRIVE : C.Sync.GPGS;
            startSync(true);
            break;
        case R.id.btn_local:
            Application.Options.mSync = C.Sync.NONE;
            mApp.mSP.edit().putInt(C.Sync.SERVER, Application.Options.mSync).apply();
            mStage = SET_PWD;
            startHome();
            break;
        }
    }
    
    private void startSync(boolean animationOn) {
        Resources r = getResources();
        int strRes[] = {R.string.sync_none, R.string.sync_gpg, R.string.sync_gdrive};
        mButtonContainer = findViewById(R.id.btn_container);
        if(animationOn) {
            mButtonContainer.animate().scaleY(0.0f).setListener(this);
        }
        else {
            mButtonContainer.setVisibility(View.INVISIBLE);
        }
        mSyncText = (TextView)findViewById(R.id.sync_hint);
        mSyncText.setText(r.getString(R.string.contacting, 
                r.getString(strRes[Application.Options.mSync])));
        ProgressBar pb = (ProgressBar)findViewById(R.id.pb);
        if(animationOn) {
            pb.animate().alpha(1.0f).setStartDelay(300);
            mSyncText.animate().alpha(1.0f).setStartDelay(300);
        }
        else {
            pb.setAlpha(1.0f);
            mSyncText.setAlpha(1.0f);
        }
        SyncService ss = SyncService.getInstance(this, Application.Options.mSync);
        ss.initialize().setListener(this).connect(0);
    }
    
    private void startHome() {
        getSupportFragmentManager().beginTransaction()                        
            .setCustomAnimations(R.anim.expand_from_right, R.anim.collapse_to_left)
            .replace(R.id.container, HomeFragment.create(), null)            
            .commit();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mPwdEdit = (EditText)findViewById(R.id.password);
                popInput();
            }
            
        },300);
    }
    
    public void startMain() {
        System.gc();
        Application.reset();
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        mApp.ignoreNextPause();
        AccountManager.getInstance().setDefaultCategory(-1, getString(R.string.def_category));
        startActivity(intent);
        this.finish();
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        SyncService.getInstance().onActivityResult(requestCode, resultCode, data);
    }
    
    private void popInput() {
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
            EditText et_confirm = (EditText)findViewById(R.id.confirm);
            if(et_confirm.getVisibility() == View.VISIBLE) {
                mPwdEdit.setImeOptions(EditorInfo.IME_ACTION_NEXT);
                et_confirm.setOnEditorActionListener(eal);
            }
            else {
                mPwdEdit.setOnEditorActionListener(eal);
            }
        }        
    }
    
    public static class HomeFragment extends Fragment {
        
        public static HomeFragment create() {
            return new HomeFragment();
        }
        
        public HomeFragment() {}
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            int layout = 0;
            if(mStage == LOADING || mStage == SELECT_SYNC) {
                layout = R.layout.fragment_startup;
            }
            else {
                layout = R.layout.fragment_home;
            }
            
            View rootView = inflater.inflate(layout, container, false);
            
            if(layout == R.layout.fragment_home) {
                final Button unlock = (Button)rootView.findViewById(R.id.unlock); 
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    unlock.getBackground().setColorFilter(C.ThemedColors[C.colorAccent],
                            PorterDuff.Mode.SRC_ATOP);
                }
                TextWatcher tw = new TextWatcher() {      
                    @Override
                    public void afterTextChanged(Editable s) {
                        if(s.toString().length() < 1) {
                            unlock.setEnabled(false);
                        }
                        else {
                            unlock.setEnabled(true);
                        }
                    }    
                    @Override
                    public void beforeTextChanged(CharSequence s, int start,
                            int count, int after) {    }
    
                    @Override
                    public void onTextChanged(CharSequence s, int start,
                            int before, int count) { }
                };                
                EditText password = (EditText)rootView.findViewById(R.id.password);
                password.addTextChangedListener(tw);
                if(mStage == AUTH) {
                    EditText confirm = (EditText)rootView.findViewById(R.id.confirm);
                    confirm.setVisibility(View.GONE);
                }
                else if(mStage == SET_PWD) {
                    TextView tv = (TextView)rootView.findViewById(R.id.title);
                    tv.setText(R.string.set_pwd);
                    unlock.setText(R.string.get_started);
                }
            }
            else if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
                int ids[] = {R.id.btn_local, R.id.btn_gdrive, R.id.btn_gpg};
                for(int id : ids) {
                    View button = rootView.findViewById(id);
                    button.getBackground().setColorFilter(C.ThemedColors[C.colorAccent],
                            PorterDuff.Mode.SRC_ATOP);
                }
            }
            return rootView;
        }
    }
    @Override
    public void onAnimationStart(Animator animation) {}
    
    @Override
    public void onAnimationEnd(Animator animation) {
        mButtonContainer.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onAnimationCancel(Animator animation) {}
    @Override
    public void onAnimationRepeat(Animator animation) {}
    @Override
    public void onSyncFailed(int errorCode) {
        mStage = SET_PWD;
        Application.Options.mSync = C.Sync.NONE;
        mApp.mSP.edit().putInt(C.Sync.SERVER, C.Sync.NONE).apply();
        startHome();
    }
    @Override
    public void onSyncProgress(int actionCode) {
        if(actionCode == SyncService.CA.AUTH) {
            Resources r = getResources();
            mSyncText.setText(r.getString(R.string.loading, 
                 Application.Options.mSync == C.Sync.GDRIVE ? 
                     r.getString(R.string.sync_gdrive) : r.getString(R.string.sync_gpg)));
            mApp.mSP.edit().putInt(C.Sync.SERVER, Application.Options.mSync).apply();
        }
        else if(actionCode == SyncService.CA.DATA_RECEIVED) {
            byte[] data = SyncService.getInstance().requestData();
            Application.FileHeader fh = Application.FileHeader.parse(data);
            if(fh.valid) {
                mApp.saveData(data);
                mStage = AUTH;
            }
            else {
                mStage = SET_PWD;
            }
            startHome();
        }
    }
    
    private class DecryptTask extends AsyncTask<String, Void, String> {
        Button mOK;
        ProgressBar mProgress;
        
        @Override
        protected void onPreExecute() {
            mOK = (Button)HomeActivity.this.findViewById(R.id.unlock);
            mProgress = (ProgressBar)HomeActivity.this.findViewById(R.id.pb);
            mOK.setEnabled(false);
            mProgress.setVisibility(View.VISIBLE);
        }
        @Override
        protected String doInBackground(String... params) {
            try{
                mApp.onStart();
                mApp.setPassword(params[0], false);
                mApp.decrypt();
            }
            catch(GeneralSecurityException e) {
                return null;
            }
            return "OK";
        }
        
        @Override
        protected void onPostExecute(String result) {
            if(result== null) {
                mProgress.setVisibility(View.INVISIBLE);
                mPwdEdit.setText("");
                Application.showToast(HomeActivity.this, R.string.pwd_wrong, Toast.LENGTH_SHORT);
            }
            else {
                startMain();
            }
        }
    }
}
