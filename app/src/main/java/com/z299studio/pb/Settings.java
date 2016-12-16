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

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.text.DateFormat;
import java.util.Date;

public class Settings extends AppCompatActivity implements AdapterView.OnItemClickListener,
        SettingListDialog.OnOptionSelected, ImportExportTask.TaskListener,
        ActionDialog.ActionDialogListener, SyncService.SyncListener,
        FingerprintDialog.FingerprintListener, DecryptTask.OnTaskFinishListener{
    
    private static final String TAG_DIALOG = "action_dialog";
    private static final int PERMISSION_REQUEST = 1;

    private String mText;
    private int mActionType;
    private int mOperation;
    private int mOption;
    private byte[] mData;

    private interface ActionListener {
        void onAction(SettingItem sender);
    }
    
    private class SettingItem {
        static final int TYPE_CATEGORY = 0;
        static final int TYPE_SWITCH = 1;
        static final int TYPE_SELECTION = 2;
        static final int TYPE_ACTION = 3;
        int mType;
        int mId;
        String mTitle;
        String mDescription;
        
        SettingItem(int id, String title, String description) {
            mId = id;
            mTitle = title;
            mDescription = description;
            mType = TYPE_CATEGORY;
        }
        public void onClick(View view) {}
        public Object getValue() {return null;}
    }
    
    private class SettingItemSwitch extends SettingItem {
        private boolean mValue;
        
        SettingItemSwitch(int id, String title, String description) {
            super(id, title, description);
            mType = TYPE_SWITCH;
        }
        @Override
        public void onClick(View view) {
            mValue = !mValue;            
        }
        
        @Override
        public Object getValue() {
            return mValue;            
        }        
        
        public SettingItemSwitch setValue(boolean initial) {
            mValue = initial;
            return this;
        }
    }
    
    private class SettingItemAction extends  SettingItem {
        private ActionListener mListener;
        
        SettingItemAction(int id, String title, String description) {
            super(id, title, description);
            mType = TYPE_ACTION; 
        }
        
        public SettingItemAction setListener(ActionListener l) {
            mListener = l;
            return  this;
        }
        @Override
        public void onClick(View view) {
            if(mListener!=null) {
                mListener.onAction(this);
            }
        }      
    }
    
    private class SettingItemSelection extends  SettingItem {
        private String[] mOptions;
        private int mSelection;

        SettingItemSelection(int id, String title, String description) {
            super(id, title, description);
            mType = TYPE_SELECTION; 
        }
        
        public SettingItemSelection setOptions(String[] options) {
            mOptions = options;
            return this;
        }
        
        @Override
        public void onClick(View view) {
            SettingListDialog.build(mTitle, mOptions, mSelection)
                    .show(getSupportFragmentManager(), TAG_DIALOG);
        }
        
        @Override
        public Object getValue() {
            return mSelection;            
        }
        
        public SettingItemSelection setValue(int initial) {
            mSelection = initial;
            mDescription = mOptions[mSelection];
            return this;
        }
        
        public String getText() {
            return mOptions[mSelection];
        }
        
    }
    
    private class SettingItemAdapter extends BaseAdapter {

        private Context mContext;
        private SettingItem[] mItems;
        
        @Override
        public int getCount() {
            return mItems.length;
        }

        @Override
        public Object getItem(int position) {
            return mItems[position];
        }

        @Override
        public long getItemId(int position) {
            return mItems[position].mId;
        }
        
        @Override
        public boolean isEnabled(int position) {
            return mItems[position].mType != SettingItem.TYPE_CATEGORY;
        }
        
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final SettingItem item = mItems[position];
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            View view;
            switch (item.mType) {
                default:
                case SettingItem.TYPE_CATEGORY:
                    view = inflater.inflate(R.layout.list_item_title, parent, false);
                    break;
                case SettingItem.TYPE_SWITCH:
                    view = inflater.inflate(R.layout.list_item_switch, parent, false);
                    break;
                case SettingItem.TYPE_SELECTION:
                case SettingItem.TYPE_ACTION:
                    view = inflater.inflate(R.layout.list_item_selection, parent, false);
                    break;
            }
            TextView description = (TextView)view.findViewById(R.id.description);
            ((TextView)view.findViewById(R.id.title)).setText(item.mTitle);
            if(item.mDescription!=null) {
                description.setText(item.mDescription);
                view.setTag(description);
            }
            else {
                if(description!=null) {
                    description.setVisibility(View.GONE);
                }
            }
            if(item.mType == SettingItem.TYPE_SWITCH) {
                SwitchCompat sc = (SwitchCompat)view.findViewById(R.id.switch_ctrl);
                sc.setChecked((boolean) item.getValue());
                view.setTag(sc);
                sc.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onItemClick(null, null, position, item.mId);
                    }
                });
            }            
            return view;
        }
        
        SettingItemAdapter(Context context, SettingItem[] items) {
            mContext = context;
            mItems = items;           
        }
        
        void updateDescription(String text, int position, ListView listView) {
            int firstVisible = listView.getFirstVisiblePosition();
            View view = listView.getChildAt(position - firstVisible);
            if(view != null) {
                TextView description = (TextView)view.getTag();
                description.setText(text);
            }
        }
    }

    private ActionListener mActionListener = new ActionListener() {
        @Override
        public void onAction(SettingItem sender) {
            int type = 0;
            switch(sender.mId) {
                case R.string.export_data:
                    type = ActionDialog.ACTION_EXPORT;
                    break;
                case R.string.import_data:
                    type = ActionDialog.ACTION_IMPORT;
                    break;
                case R.string.change_pwd:
                    type = ActionDialog.ACTION_RESET_PWD;
                    break;
                case R.string.licence:
                    type = ActionDialog.ACTION_LICENSE;
                    break;
                case R.string.credits:
                    type = ActionDialog.ACTION_CREDITS;
                    break;
                case R.string.last_sync:
                    if(Application.Options.mSync != C.Sync.NONE) {
                        SyncService.getInstance(Application.Options.mSync)
                                .initialize(Settings.this).setListener(Settings.this)
                                .connect(Application.getInstance().getLocalVersion());
                    }
                    return;
                case R.string.guide:
                    Intent intent = new Intent(Settings.this, TourActivity.class);
                    intent.putExtra(C.ACTIVITY, C.Activity.SETTINGS);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    return;
            }
            ActionDialog.create(type).show(getSupportFragmentManager(), TAG_DIALOG);
        }
    };
    
    private SettingItemAdapter mAdapter;
    private boolean mShowOtherInitial;
    private int mRequestingPosition;
    private ListView mListView;
    
    private SettingItemAdapter initSettings() {
        int itemSize = 19;
        if(Application.Options.mFpStatus != C.Fingerprint.UNKNOWN) {
            itemSize += 1;
        }
        SettingItem[] items = new SettingItem[itemSize];
        int index = 0;
        String desc;
        items[index++] = new SettingItem(0, getString(R.string.general), null);
        items[index++] = new SettingItemAction(R.string.import_data,
                getString(R.string.import_data), null).setListener(mActionListener);
        items[index++] = new SettingItemAction(R.string.export_data,
                getString(R.string.export_data), null).setListener(mActionListener);
        items[index++] = new SettingItemSwitch(R.string.show_ungrouped, 
                getString(R.string.show_ungrouped), null)
                .setValue(Application.Options.mShowOther);
        items[index++] = new SettingItemSelection(R.string.theme, getString(R.string.theme), null)
                .setOptions(getResources().getStringArray(R.array.theme_names))
                .setValue(Application.Options.mTheme);
        items[index++] = new SettingItemAction(R.string.guide, getString(R.string.guide), null)
                .setListener(mActionListener);
        items[index++] = new SettingItem(0, getString(R.string.sync), null);
        items[index++] = new SettingItemSelection(R.string.sync_server, 
                getString(R.string.sync_server), null)
                .setOptions(getResources().getStringArray(R.array.sync_methods))
                .setValue(Application.Options.mSync);
        if(Application.Options.mSyncTime.after(new Date(0L))) {
            DateFormat df = DateFormat.getDateTimeInstance();
            desc = df.format(Application.Options.mSyncTime);
        }
        else{
            desc = getString(R.string.never);
        }
        items[index++] = new SettingItemAction(R.string.last_sync,
                getString(R.string.last_sync), desc).setListener(mActionListener);
        items[index++] = new SettingItemSwitch(R.string.sync_msg, getString(R.string.sync_msg), 
                null).setValue(Application.Options.mSyncMsg);
        items[index++] = new SettingItem(0, getString(R.string.security), null);
        int lock_options[] = {1000, 5*60 * 1000, 30*60 * 1000, 0};
        int selection = 0;
        int saved = Application.Options.mAutoLock;
        for(int i = 0; i < lock_options.length; ++i) {
            if(lock_options[i] == saved) {
                selection = i;
            }
        }
        items[index++] = new SettingItemSelection(R.string.auto_lock, getString(R.string.auto_lock),
                null).setOptions(getResources().getStringArray(R.array.lock_options))
                .setValue(selection);
        items[index++] = new SettingItemSwitch(R.string.show_password,
                getString(R.string.show_password), null)
                .setValue(Application.Options.mAlwaysShowPwd);
        items[index++] = new SettingItemSwitch(R.string.warn_copy, getString(R.string.warn_copy),
                null).setValue(Application.Options.mWarnCopyPwd);
        items[index++] = new SettingItemAction(R.string.change_pwd, getString(R.string.change_pwd),
                null).setListener(mActionListener);
        if(Application.Options.mFpStatus != C.Fingerprint.UNKNOWN) {
            items[index++] = new SettingItemSwitch(R.string.fp_title, getString(R.string.fp_title),
                    getString(R.string.fp_desc))
                    .setValue(Application.Options.mFpStatus == C.Fingerprint.ENABLED);
        }

        items[index++] = new SettingItem(0, getString(R.string.about), null);
        items[index++] = new SettingItemAction(R.string.licence, getString(R.string.licence),
                null).setListener(mActionListener);
        items[index++] = new SettingItemAction(R.string.credits, getString(R.string.credits),
                null).setListener(mActionListener);
        String versionName;
        try {
            versionName = this.getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            versionName = "2.2.0";
        }
        desc = getString(R.string.version, versionName);
        items[index] = new SettingItemAction(R.string.build, getString(R.string.build), desc);
        mAdapter = new SettingItemAdapter(this, items);
        return mAdapter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if(Application.getInstance() == null || Application.getInstance().getAccountManager() == null) {
            super.onCreate(savedInstanceState);
            startActivity(new Intent(this, HomeActivity.class));
            this.finish();
            return;
        }
        if(savedInstanceState != null) {
            mRequestingPosition = savedInstanceState.getInt("requested_position");
        }
        setTheme(C.THEMES[Application.Options.mTheme]);
        if(Application.getInstance().queryChange(Application.THEME)) {
            int[] primaryColors = {R.attr.colorPrimary, R.attr.colorPrimaryDark,
                    R.attr.colorAccent, R.attr.textColorNormal, R.attr.iconColorNormal};
            TypedArray ta = obtainStyledAttributes(primaryColors);
            for(int i = 0; i < C.ThemedColors.length; ++i) {
                C.ThemedColors[i] = ta.getColor(i, 0);
            }
            ta.recycle();
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            View v = findViewById(R.id.activity_root);
            v.setBackgroundColor(C.ThemedColors[C.colorPrimary]);
        }
        mShowOtherInitial = Application.Options.mShowOther;
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        ActionBar actionBar = getSupportActionBar();
        if(actionBar!=null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        mListView = (ListView)findViewById(R.id.list);
        mListView.setAdapter(initSettings());
        mListView.setOnItemClickListener(this);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if(Application.getInstance().needAuth()) {
            Intent homeIntent = new Intent(this, HomeActivity.class);
            homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(homeIntent);
            finish();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        Application.getInstance().onPause(this);
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("requested_position", mRequestingPosition);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if(mShowOtherInitial != Application.Options.mShowOther) {
            Application.getInstance().notifyChange(Application.DATA_OTHER);
        }
        super.onBackPressed();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return true;
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, Intent data) {
        switch (requestCode) {
            case SyncService.REQ_RESOLUTION:
                SyncService.getInstance().onActivityResult(requestCode, resultCode, data);
                break;
            case ActionDialog.REQ_CODE_FILE_SELECTION:
                ActionDialog dialog = (ActionDialog)getSupportFragmentManager()
                        .findFragmentByTag(TAG_DIALOG);
                dialog.onFileSelected(this, resultCode, data);
                Application.getInstance().ignoreNextPause();
                break;
        }
    }
    
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        SettingItem item = (SettingItem)mAdapter.getItem(position);
        item.onClick(view);
        mRequestingPosition = position;
        if(item.mType == SettingItem.TYPE_SWITCH) {
            boolean value = (boolean)item.getValue();
            if(view!=null) {
                SwitchCompat sc = (SwitchCompat) view.getTag();
                sc.setChecked(value);
            }            
            handleSwitchOption(item.mId, value);
        }
    }

    @Override
    public void onSelected(int selection) {
        SettingItemSelection item = (SettingItemSelection)mAdapter.getItem(mRequestingPosition);
        item.setValue(selection);
        mAdapter.updateDescription(item.getText(), mRequestingPosition, mListView);
        SharedPreferences.Editor editor = Application.getInstance().mSP.edit();
        switch (item.mId) {
            case R.string.theme:
                Application.getInstance().notifyChange(Application.THEME);
                Application.Options.mTheme = (int)item.getValue();
                editor.putInt(C.Keys.THEME, Application.Options.mTheme);
                startActivity(new Intent(this, Settings.class));
                finish();
                overridePendingTransition(0,0);
                break;
            case R.string.sync_server:
                Application.Options.mSync = (int)item.getValue();
                if(Application.Options.mSync == C.Sync.NONE) {
                    editor.putInt(C.Sync.SERVER, Application.Options.mSync);
                }
                else {
                    SyncService.getInstance(Application.Options.mSync).initialize(this)
                            .setListener(this).connect(Application.getInstance().getLocalVersion());
                    mRequestingPosition += 1;  // Bad design
                }
                break;
            case R.string.auto_lock:
                int lock_options[] = {1000, 5*60*1000, 30 * 60 * 1000, 0};
                Application.Options.mAutoLock = lock_options[(int)item.getValue()];
                editor.putInt(C.Keys.AUTO_LOCK_TIME, Application.Options.mAutoLock);
                break;
        }
        editor.apply();
    }

    @Override
    public void onFinish(boolean authenticate, int operation, String result) {
        if(result== null) {
            if(authenticate) {
                ActionDialog.create(ActionDialog.ACTION_AUTHENTICATE)
                        .show(getSupportFragmentManager(), TAG_DIALOG);
                return;
            }
            Application.showToast(Settings.this, operation == ActionDialog.ACTION_EXPORT ?
                    R.string.export_failed : R.string.import_failed, Toast.LENGTH_LONG);
        }
        else {
            if(operation==ActionDialog.ACTION_EXPORT) {
                Application.showToast(Settings.this,
                        getResources().getString(R.string.export_success, result), 
                        Toast.LENGTH_LONG);
            }
            else {
                if(Application.getInstance().getAccountManager().saveRequired()) {
                    Application.getInstance().saveData(this);
                    Application.getInstance().notifyChange(Application.DATA_ALL);
                }
                Application.showToast(Settings.this, R.string.import_success, Toast.LENGTH_LONG);
            }
        }
    }

    @Override
    public byte[] saveData() {
        if(Application.getInstance().getAccountManager().saveRequired()) {
            Application.getInstance().saveData(this);
        }
        return Application.getInstance().getData(this);
    }

    @Override
    public void onConfirm(String text, int type, int operation, int option) {

        if(operation == ActionDialog.ACTION_AUTHENTICATE) {
            new ImportExportTask(this, text).execute();
        }
        else if(operation == ActionDialog.ACTION_RESET_PWD && Application.Options.mFpStatus ==
                C.Fingerprint.ENABLED) {
            FingerprintDialog.build(true).show(getSupportFragmentManager(), "dialog_fp");
        }
        else if(operation == ActionDialog.ACTION_AUTHENTICATE2) {
            Application.FileHeader header = Application.FileHeader.parse(mData);
            if(text!=null) {
                new DecryptTask(mData, header, this ).execute(text);
            }
            else {
                Application.getInstance().increaseVersion(this,header.revision);
                SyncService.getInstance().send(Application.getInstance().getData(this));
            }
        }
        else {
            if(operation == ActionDialog.ACTION_IMPORT ||
                    operation == ActionDialog.ACTION_EXPORT) {
                getPermission2Do(text, type, operation, option);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    void getPermission2Do(String text, int type, int operation, int option){
        String permission = operation == ActionDialog.ACTION_IMPORT ?
                Manifest.permission.READ_EXTERNAL_STORAGE :
                Manifest.permission.WRITE_EXTERNAL_STORAGE;

        if (ActivityCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission},
                    PERMISSION_REQUEST);
            mActionType = type;
            mText = text;
            mOperation = operation;
            mOption = option;
        }
        else {
            new ImportExportTask(this, text, Application.getInstance().getPassword(),
                    type, operation, option).execute();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResult){
        if(requestCode == PERMISSION_REQUEST) {
            if(grantResult[0] == PackageManager.PERMISSION_GRANTED) {
                new ImportExportTask(this, mText, Application.getInstance().getPassword(),
                        mActionType, mOperation, mOption).execute();
            }
        }
    }

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
    public void onSyncFailed(int errorCode) { }
    
    @Override
    public void onSyncProgress(int actionCode) {
        String time = null;
        Application app = Application.getInstance();
        app.mSP.edit().putInt(C.Sync.SERVER, Application.Options.mSync).apply();
        if(actionCode == SyncService.CA.AUTH) {
            app.ignoreNextPause();
        }
        else if(actionCode == SyncService.CA.DATA_RECEIVED) {
            time = app.onSyncSucceed();
            byte[] data = SyncService.getInstance().requestData();
            Application.FileHeader fh = Application.FileHeader.parse(data);
            if(fh.valid && fh.revision > app.getLocalVersion()) {
                new DecryptTask(data, fh, this).execute(app.getPassword());
            }
            else if(fh.revision < app.getLocalVersion()){
                SyncService.getInstance().send(app.getData(this));
            }
            if(fh.revision != Application.Options.mSyncVersion) {
                app.onVersionUpdated(fh.revision);
            }
        }
        else if(actionCode == SyncService.CA.DATA_SENT) {
            time = app.onSyncSucceed();
            Application.showToast(this, R.string.sync_success_server, Toast.LENGTH_SHORT);
            app.onVersionUpdated(app.getLocalVersion());
        }
        if(time!=null) {
            mAdapter.updateDescription(time, mRequestingPosition, mListView);
        }
    }

    @Override
    public void preExecute() {  }

    @Override
    public void onFinished(boolean isSuccessful, AccountManager manager, String password,
                           byte[] data, Application.FileHeader header, Crypto crypto) {
        if(isSuccessful) {
            Application app = Application.getInstance();
            Application.showToast(this, R.string.sync_success_local, Toast.LENGTH_SHORT);
            Application.Options.mSyncVersion = header.revision;
            app.setAccountManager(manager, -1, getString(R.string.def_category));
            app.setCrypto(crypto);
            app.saveData(this,data, header);
            app.onVersionUpdated(header.revision);
            app.notifyChange(Application.DATA_ALL);
            if( !app.getPassword().equals(password)) {
                app.setPassword(password, false);
                if(Application.Options.mFpStatus == C.Fingerprint.ENABLED) {
                    FingerprintDialog.build(true).show(getSupportFragmentManager(), "dialog_fp");
                }
            }
        }
        else {
            mData = data;
            ActionDialog.create(ActionDialog.ACTION_AUTHENTICATE2).show(
                    getSupportFragmentManager(), "dialog_auth2");
        }
    }

    private void handleSwitchOption(int id, boolean value) {
        SharedPreferences.Editor editor = Application.getInstance().mSP.edit();
        switch(id) {
            case R.string.show_ungrouped:
                Application.Options.mShowOther = value;
                editor.putBoolean(C.Keys.SHOW_OTHER, value);
                break;
            case R.string.sync_msg:
                Application.Options.mSyncMsg = value;
                editor.putBoolean(C.Sync.MSG, value);
                break;
            case R.string.show_password:
                Application.Options.mAlwaysShowPwd = value;
                editor.putBoolean(C.Keys.SHOW_PWD, value);
                break;
            case R.string.warn_copy:
                Application.Options.mWarnCopyPwd = value;
                editor.putBoolean(C.Keys.WARN_COPY, value);
                break;
            case R.string.fp_title:
                if(value) {
                    FingerprintDialog.build(true).show(getSupportFragmentManager(), "dialog_fp");
                }
                else {
                    Application.getInstance().clearFpData();
                }
        }
        editor.apply();
    }

    @Override
    public void onCanceled(boolean isFirstTime) { }

    @Override
    public void onConfirmed(boolean isFirstTime, byte[] password) { }

}
