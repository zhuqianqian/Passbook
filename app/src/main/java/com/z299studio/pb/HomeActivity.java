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

import android.Manifest;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.annotation.TargetApi;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class HomeActivity extends AppCompatActivity implements
AnimatorListener, SyncService.SyncListener, FingerprintDialog.FingerprintListener,
        DecryptTask.OnTaskFinishListener{
    protected Application mApp;
    protected EditText mPwdEdit;
    protected TextView mSyncText;
    protected View mButtonContainer;
    private ProgressBar mProgress;
    private static int mStage;
    private static final int SELECT_SYNC = 0;
    private static final int LOADING = 1;
    private static final int SET_PWD = 2;
    private static final int AUTH = 3;
    private FingerprintManager mFingerprintManager;
    private static final int PERMISSION_REQ_CODE_FP = 1;
    
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
            if(!mApp.hasDataFile(this)) {
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
        if(!Application.Options.mTour){
            Intent intent = new Intent(this, TourActivity.class);
            intent.putExtra(C.ACTIVITY, C.Activity.HOME);
            this.startActivity(intent);
            this.finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ){
            mFingerprintManager = (FingerprintManager)getSystemService(FINGERPRINT_SERVICE);
            if(mStage == AUTH && mApp.queryFpStatus() == C.Fingerprint.ENABLED) {
                tryUseFingerprint();
            }
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
        super.onResume();
        popInput();
    }
    
    @Override
    protected void onPause() {
        if(mPwdEdit!=null) {
            mPwdEdit.clearFocus();
            InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mPwdEdit.getWindowToken(), 0);
        }
        super.onPause();
    }
    
    public void onConfirm(View view) {
        String password = mPwdEdit.getText().toString();
        if(password.length() < 1) {
            Application.showToast(this, R.string.pwd_wrong, Toast.LENGTH_SHORT);
            return;
        }
        if(mStage == SET_PWD) {
            EditText et_confirm = (EditText) findViewById(R.id.confirm);
            if(password.equals(et_confirm.getText().toString())) {
                et_confirm.clearFocus();
                ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(et_confirm.getWindowToken(), 0);
                mApp.setPassword(password, true);
                new InitTask().execute();
            }
            else {
                et_confirm.setText("");
                mPwdEdit.setText("");
                Application.showToast(this, R.string.pwd_unmatch, Toast.LENGTH_SHORT);
            }
        }
        else {
            mApp.setPassword(password, false);
            new DecryptTask(mApp.getData(this), mApp.getAppHeaderData(this), this).execute(password);
        }
    }

    @Override @TargetApi(Build.VERSION_CODES.M)
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResult){
        if(requestCode == PERMISSION_REQ_CODE_FP &&
                grantResult[0] == PackageManager.PERMISSION_GRANTED){
            FingerprintDialog.build(Application.Options.mFpStatus == C.Fingerprint.UNKNOWN)
                    .show(getSupportFragmentManager(), "dialog_fp");
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean tryUseFingerprint() {
        boolean isHandled = false;
        if(Application.Options.mFpStatus != C.Fingerprint.DISABLED){
            if(checkSelfPermission(Manifest.permission.USE_FINGERPRINT)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] {Manifest.permission.USE_FINGERPRINT},
                        PERMISSION_REQ_CODE_FP);
                isHandled = true;
            }
            else if(mFingerprintManager.isHardwareDetected() &&
                    mFingerprintManager.hasEnrolledFingerprints()) {
                if(mPwdEdit!=null) {
                    ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                            .hideSoftInputFromWindow(mPwdEdit.getWindowToken(), 0);
                }
                FingerprintDialog.build(Application.Options.mFpStatus == C.Fingerprint.UNKNOWN)
                        .show(getSupportFragmentManager(), "dialog_fp");
                isHandled = true;
            }
        }
        return isHandled;
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
        SyncService ss = SyncService.getInstance(Application.Options.mSync);
        ss.initialize(this).setListener(this).connect(0);
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
        mApp.getAccountManager().setDefaultCategory(-1, getString(R.string.def_category));
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

    @Override
    public void preExecute() {
        Button okButton = (Button)HomeActivity.this.findViewById(R.id.unlock);
        mProgress = (ProgressBar)HomeActivity.this.findViewById(R.id.pb);
        okButton.setEnabled(false);
        mProgress.setVisibility(View.VISIBLE);
        mApp.onStart();
    }

    @Override
    public void onFinished(boolean isSuccessful, AccountManager manager, String password,
                           byte[] data, Application.FileHeader header, Crypto crypto) {
        if(isSuccessful) {
            mApp.setAccountManager(manager, -1, getString(R.string.def_category));
            mApp.setCrypto(crypto);
        }
        if(!isSuccessful) {
            mProgress.setVisibility(View.INVISIBLE);
            mPwdEdit.setText("");
            Application.showToast(HomeActivity.this, R.string.pwd_wrong, Toast.LENGTH_SHORT);
        }
        else if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || Application.Options.mFpStatus == C.Fingerprint.ENABLED
                || !tryUseFingerprint())  {
            startMain();
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
            int layout;
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
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        if (!result.hasResolution()) {
            GoogleApiAvailability.getInstance()
                    .getErrorDialog(this,result.getErrorCode(), 0).show();
            onSyncFailed(SyncService.CA.CONNECTION);
            return;
        }
        try {
            result.startResolutionForResult(this, SyncService.REQ_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            onSyncFailed(SyncService.CA.CONNECTION);
        }
    }

    @Override
    public void onSyncFailed(int errorCode) {
        mStage = SET_PWD;
        if(errorCode== SyncService.CA.AUTH) {
            Application.Options.mSync = C.Sync.NONE;
            mApp.mSP.edit().putInt(C.Sync.SERVER, C.Sync.NONE).apply();
        }
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
            Application.getInstance().onSyncSucceed();
            Application.FileHeader fh = Application.FileHeader.parse(data);
            if(fh.valid) {
                mApp.onVersionUpdated(fh.revision);
                mApp.saveData(this, data, fh);
                mStage = AUTH;
            }
            else {
                mStage = SET_PWD;
            }
            startHome();
        }
    }

    @Override
    public void onCanceled(boolean isFirstTime) {
        if(isFirstTime) {
            startMain();
        }
        else {
            popInput();
        }
    }

    @Override
    public void onConfirmed(boolean isFirstTime, byte[] password) {
        if(isFirstTime) {
            startMain();
        }
        else {
            String pwd = new String(password);
            mApp.setPassword(pwd, false);
            new DecryptTask(mApp.getData(this), mApp.getAppHeaderData(this), this).execute(pwd);
        }
    }

    private class InitTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            Button okButton = (Button)HomeActivity.this.findViewById(R.id.unlock);
            ProgressBar pb = (ProgressBar)HomeActivity.this.findViewById(R.id.pb);
            okButton.setEnabled(false);
            pb.setVisibility(View.VISIBLE);
        }
        @Override
        protected String doInBackground(String... params) {
            mApp.onStart();
            Resources r = getResources();
            String[] defCategories = r.getStringArray(R.array.category_names);
            int i = 0;
            AccountManager am = new AccountManager(null);
            mApp.setAccountManager(am, -1, getString(R.string.def_category));
            for(String s : defCategories) {
                am.addCategory(i++, s);
            }
            String[] defAccountNames = r.getStringArray(R.array.def_account_names);
            String[] defNames = r.getStringArray(R.array.def_field_names);
            String[] defValues = r.getStringArray(R.array.def_values);
            int[] defTypes = r.getIntArray(R.array.def_field_types);
            TypedArray defAccountData = r.obtainTypedArray(R.array.def_account_data);
            int[] dataDetails;
            AccountManager.Account a;
            for(i = 0; i < defAccountNames.length; ++i) {
                dataDetails = r.getIntArray(defAccountData.getResourceId(i, 0));
                a = am.newAccount(dataDetails[0]);
                a.mProfile = defAccountNames[i];
                for (int j = 1; j < dataDetails.length; j += 3){
                    try {
                        a.addEntry(defTypes[dataDetails[j + 2]],
                                defNames[dataDetails[j]],
                                defValues[dataDetails[j + 1]]);
                    }catch(ArrayIndexOutOfBoundsException e) {
                        Log.e("Passbook", "This should really not happen.");
                    }
                }
                am.addAccount(dataDetails[0], a);
            }
            defAccountData.recycle();
            return "OK";
        }

        @Override
        protected void onPostExecute(String result) {
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                    || Application.Options.mFpStatus == C.Fingerprint.ENABLED
                    || !tryUseFingerprint())  {
                startMain();
            }
        }
    }
}
