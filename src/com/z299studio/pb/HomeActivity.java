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

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
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

public class HomeActivity extends Activity implements 
AnimatorListener, SyncService.SyncListener{

    protected Application mApp;
    protected EditText mPwdEdit;
    protected TextView mSyncText;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mApp = Application.getInstance(this);
        this.setTheme(C.THEMES[Application.Options.mTheme]);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        if(!mApp.hasDataFile()) {
            HomeFragment.mLayout = R.layout.fragment_startup;
        }
        else {
            HomeFragment.mLayout = R.layout.fragment_home;
        }
        getFragmentManager().beginTransaction().add(R.id.container,
            HomeFragment.create()).commit();
        if(Application.Options.mTour == false){
            Intent intent = new Intent(this, TourActivity.class);
            intent.putExtra(C.Names.ACTIVITY, C.Activity.HOME);
            this.startActivity(intent);
            this.finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
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
    
    public void onSyncSelected(View view) {
        switch(view.getId()) {
        case R.id.btn_gdrive:
        case R.id.btn_gpg:
            startSync(view.getId());
            break;
        case R.id.btn_local:
            Application.Options.mSync = C.Sync.NONE;
            mApp.mSP.edit().putInt(C.Sync.SERVER, Application.Options.mSync).commit();
            startHome();
            break;
        }
    }
    
    private void startSync(int id) {
        Resources r = getResources();
        Button options[]= new Button[3];
        int ids[] = {R.id.btn_gdrive, R.id.btn_gpg, R.id.btn_local};
        for(int i = 0; i < options.length; ++i) {
            options[i] = (Button)findViewById(ids[i]);
            options[i].animate().scaleY(0.0f).setListener(this);
        }
        mSyncText = (TextView)findViewById(R.id.sync_hint);
        Application.Options.mSync = id == R.id.btn_gdrive ? C.Sync.GDRIVE : C.Sync.GPGS;
        mSyncText.setText(r.getString(R.string.contacting, 
            id == R.id.btn_gdrive ? r.getString(R.string.sync_gdrive) :
                r.getString(R.string.sync_gpg)));
        ProgressBar pb = (ProgressBar)findViewById(R.id.pb);
        pb.animate().alpha(1.0f).setStartDelay(300);
        mSyncText.animate().alpha(1.0f).setStartDelay(300);
        SyncService ss = SyncService.getInstance(this, Application.Options.mSync);
        ss.initialize().connect(0);
    }
    
    private void startHome() {
        HomeFragment.mLayout = R.layout.fragment_home;
        getFragmentManager().beginTransaction()
            .replace(R.id.container, HomeFragment.create())
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .commit();
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        SyncService.getInstance().onActivityResult(requestCode, resultCode, data);
    }
    
    private void popInput() {
        mPwdEdit = (EditText)findViewById(R.id.password);
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
            EditText et_confirm = (EditText)findViewById(R.id.confirm);
            if(et_confirm !=null) {
                et_confirm.setOnEditorActionListener(eal);
            }
        }        
    }
    
    public static class HomeFragment extends Fragment {
        public static int mLayout;
        
        public static HomeFragment create() {
            return new HomeFragment();
        }
        
        public HomeFragment() {}
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(mLayout, container, false);
            EditText confirm = (EditText)rootView.findViewById(R.id.confirm);
            if(Application.getInstance().hasDataFile()) {
                confirm.setVisibility(View.GONE);
            }
            else {
                
            }
            return rootView;
        }
    }

    @Override
    public void onAnimationStart(Animator animation) {}

    @Override
    public void onAnimationEnd(Animator animation) {
        
        
    }

    @Override
    public void onAnimationCancel(Animator animation) {}

    @Override
    public void onAnimationRepeat(Animator animation) {}

    @Override
    public void onSyncFailed(int errorCode) {
        
    }

    @Override
    public void onSyncProgress(int actionCode) {
        if(actionCode == SyncService.CA.AUTH) {
            Resources r = getResources();
            mSyncText.setText(r.getString(R.string.loading, 
                 Application.Options.mSync == C.Sync.GDRIVE ? 
                     r.getString(R.string.sync_gdrive) : r.getString(R.string.sync_gpg)));
        }
    }
}
