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

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements ItemFragmentListener,
        NavigationDrawerFragment.NavigationDrawerCallbacks,
        SearchView.OnQueryTextListener, SyncService.SyncListener {


    private Application mApp;
    private NavigationDrawerFragment mNavigationDrawer;
    private MainListFragment mMainList;
    private int mStatusColor;
    private View mRootView;
    private int mStatusColorDetail;
    private int mCategoryId;
    private ArrayList<AccountManager.Account> mAllAccounts = null;
    private ArrayList<AccountManager.Account> mSearchedAccounts= null;
    private String mLastKey = "";
    private String mTitle;

    private Runnable mTintStatusBar = new Runnable() {
        @Override
        public void run() {
            mRootView.setBackgroundColor(mStatusColorDetail);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if(savedInstanceState!=null && AccountManager.getInstance() == null) {
            super.onCreate(savedInstanceState);
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        if(savedInstanceState==null) {
            MainListFragment.clearCache();
        }
        mApp = Application.getInstance(this);
        this.setTheme(C.THEMES[Application.Options.mTheme]);
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            View v = findViewById(R.id.panel_main);
            mRootView = v.getRootView();
            mStatusColor = C.ThemedColors[C.colorPrimary];
            mRootView.setBackgroundColor(mStatusColor);
        }
        setupToolbar();
        mNavigationDrawer = (NavigationDrawerFragment)getSupportFragmentManager()
                .findFragmentById(R.id.navigation_drawer);
        DrawerLayout drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        mNavigationDrawer.setUp(R.id.navigation_drawer, drawerLayout);
        mMainList = (MainListFragment)getSupportFragmentManager()
                .findFragmentById(R.id.panel_main);
        if(savedInstanceState != null) {
            DeleteCategory dialog = (DeleteCategory)getSupportFragmentManager()
                    .findFragmentByTag("delete_category");
            if(dialog!=null) {
                dialog.setListener(new DeleteCategory.OnDeleteConfirmListener() {
                    @Override
                    public void onConfirmed(int category, boolean alsoDelAccounts) {
                        deleteCategory(category, alsoDelAccounts);
                    }
                });
            }
            mTitle = savedInstanceState.getString("pb_title");
        }
        else {
            mTitle = getString(R.string.all_accounts);
        }
        getSupportActionBar().setTitle(mTitle);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("pb_title", mTitle);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if(Application.getInstance().needAuth()) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }
        if(mApp.queryChange(Application.THEME)) {
            mApp.handleChange(Application.THEME);
            startActivity(new Intent(this, MainActivity.class));
            finish();
            overridePendingTransition(0,0);
            return;
        }
        if(mApp.queryChange(Application.DATA_OTHER)) {
            Application.getSortedCategoryNames();
        }
        if(mApp.queryChange(Application.DATA_ALL)) {
            Application.getSortedCategoryNames();
            MainListFragment.clearCache();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(Application.Options.mSync != C.Sync.NONE) {
            SyncService.getInstance(this, Application.Options.mSync)
                    .initialize().setListener(this)
                    .connect(mApp.getLocalVersion());
        }
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        if(Application.Options.mSync != C.Sync.NONE) {
            SyncService.getInstance().disconnect();
        }
    }
    
    @Override 
    protected void onPause() {
        super.onPause();
        mApp.handleChange(Application.DATA_OTHER);
        mApp.onPause();
    }
    
    private void setupToolbar() {
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        float elevation = getResources().getDimension(R.dimen.toolbar_elevation) + 0.5f;
        ViewCompat.setElevation(toolbar, elevation);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(getResources().getBoolean(R.bool.hide_drawer) ?
            R.menu.menu_home_small : R.menu.menu_home_large, menu);
        for(int i = 0; i < menu.size(); ++i) {
            menu.getItem(i).getIcon().setColorFilter(
                    C.ThemedColors[C.colorTextNormal], PorterDuff.Mode.SRC_ATOP);
        }
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(
                menu.findItem(R.id.action_search));
        searchView.setQueryHint(getString(R.string.search));
        searchView.setOnQueryTextListener(this);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_search:
                if(mAllAccounts == null) {
                    mAllAccounts = AccountManager.getInstance().getAllAccounts(true);
                }
                break;
            case R.id.action_delete_category:
                new DeleteCategory()
                        .setCategory(mCategoryId)
                        .setListener(new DeleteCategory.OnDeleteConfirmListener() {
                            @Override
                            public void onConfirmed(int category, boolean alsoDelAccounts) {
                                deleteCategory(category, alsoDelAccounts);
                            }
                        })
                        .show(getSupportFragmentManager(), "delete_category");
                break;
            case R.id.action_edit_category:
                mMainList.editCategory();
                break;
            case R.id.action_help:
                Uri uri = Uri.parse(getResources().getString(R.string.link_ap_help));
                Intent rateIntent = new Intent(Intent.ACTION_VIEW, uri);
                try {  startActivity(rateIntent); } 
                catch (ActivityNotFoundException e) {
                    Log.w("PwdBook", "Activity not found when launching help");                    
                }
                break;
            case R.id.action_about:
                ActionDialog.create(ActionDialog.ACTION_ABOUT)
                        .show(getSupportFragmentManager(), "action_dialog");
                break;
            case R.id.action_settings:
                startActivity(new Intent(this, Settings.class));
                mApp.ignoreNextPause();
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onQueryTextSubmit(String s) {  return false;   }

    @Override
    public boolean onQueryTextChange(String s) { 
        ArrayList<AccountManager.Account> result = new ArrayList<>();
        ArrayList<AccountManager.Account> pool;
        s = s.toLowerCase();
        if(s.isEmpty()) {
            result = mAllAccounts;
        }
        else {
            if (s.startsWith(mLastKey)) {
                pool = mSearchedAccounts;
            } else {
                pool = mAllAccounts;
            }
            for (AccountManager.Account a : pool) {
                if (a.mProfile.toLowerCase().contains(s)) {
                    result.add(a);
                }
            }
        }
        mSearchedAccounts = result;
        mLastKey = s;
        mMainList.setSearch(result);
        return false; 
    }

    @Override
    public void onNavigationDrawerItemSelected(int type, int id) {
        if(type == NavigationDrawerAdapter.NavMenuItem.MENU_SELECTION) {
            mMainList.selectCategory(id, false);
            mCategoryId = id;
            mTitle = id == AccountManager.ALL_CATEGORY_ID ? getString(R.string.all_accounts)
                     : AccountManager.getInstance().getCategory(mCategoryId).mName;
            getSupportActionBar().setTitle(mTitle);
        }
        else {
            switch(id) {
                case R.string.settings:
                    startActivity(new Intent(this, Settings.class));
                    break;
                case R.string.help:
                    Uri uri = Uri.parse(getResources().getString(R.string.link_ap_help));
                    Intent rateIntent = new Intent(Intent.ACTION_VIEW, uri);
                    try {  startActivity(rateIntent); }
                    catch (ActivityNotFoundException e) {
                        Log.w("PwdBook", "Activity not found when launching help");
                    }
                    break;
                case R.string.about:
                    ActionDialog.create(ActionDialog.ACTION_ABOUT)
                            .show(getSupportFragmentManager(), "action_dialog");
                    break;
            }
        }
    }

    @Override
    public void onSelect(int id) {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_bottom, 0, 0, R.anim.slide_out_bottom)
                .replace(R.id.detail_panel, DetailFragment.create(id), "detail")
                .addToBackStack("detail")
                .commit();
    }

    @Override
    public void onBackPressed() {
        setStatusBarColor(0, 0, true);
        Fragment edit = getSupportFragmentManager().findFragmentByTag("edit");
        if(edit != null) {
            getSupportFragmentManager().popBackStack("edit",
                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
            return;
        }
        Fragment detail = getSupportFragmentManager().findFragmentByTag("detail");
        if(detail!=null) {
            getSupportFragmentManager().popBackStack("detail",
                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
            return;
        }
        super.onBackPressed();
    }

    public void setStatusBarColor(int color, int delay, boolean restore) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (delay > 0) {
                mStatusColorDetail = color;
                mRootView.postDelayed(mTintStatusBar, delay);
            } else {
                mRootView.setBackgroundColor(restore ? mStatusColor : color);
            }
        }
    }

    @Override
    public void onEdit(int categoryId, int accountId) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if(accountId < 0) {
            ft.setCustomAnimations(R.anim.slide_in_bottom, 0, 0, R.anim.slide_out_bottom);
        }
        ft.replace(R.id.detail_panel, EditFragment.create(categoryId, accountId), "edit")
                .addToBackStack("edit")
                .commit();
    }

    @Override
    public void onDelete(int accountId) {
        mMainList.onDelete(accountId);
    }

    @Override
    public void onSave(int category) {
        mNavigationDrawer.increaseCounterInMenu(AccountManager.ALL_CATEGORY_ID, 1);
        mNavigationDrawer.increaseCounterInMenu(category, 1);
        mMainList.updateData(AccountManager.ALL_CATEGORY_ID);
        mMainList.updateData(category);
    }

    @Override
    public void onSaveChanged(int account, int category, int oldCategory, boolean nameChanged) {
        if(category!=oldCategory) {
            mNavigationDrawer.increaseCounterInMenu(category, 1);
            mNavigationDrawer.increaseCounterInMenu(oldCategory, -1);
            mMainList.updateData(category);
            mMainList.updateData(oldCategory);
        }
        if(nameChanged) {
            mMainList.updateData(AccountManager.ALL_CATEGORY_ID);
            mMainList.updateData(category);
        }
    }
    
    @Override
    public void onDeleted(int categoryId, int count) {
        if(categoryId == AccountManager.ALL_CATEGORY_ID) {
            mNavigationDrawer.increaseCounterInMenu(AccountManager.ALL_CATEGORY_ID, -count);
            mNavigationDrawer.refreshCategoryCounters();
            for(int id : Application.getSortedCategoryIds()) {
                MainListFragment.resetAdapter(id);
            }
        }
        else {
            mNavigationDrawer.increaseCounterInMenu(AccountManager.ALL_CATEGORY_ID, -count);
            mNavigationDrawer.increaseCounterInMenu(categoryId, -count);
            MainListFragment.resetAdapter(AccountManager.ALL_CATEGORY_ID);
        }
    }
    
    @Override
    public void onCategorySaved() {
        Application.reset();
        mNavigationDrawer.remove(-1);
        MainListFragment.resetAdapter(AccountManager.DEFAULT_CATEGORY_ID);
        MainListFragment.resetAdapter(AccountManager.DEFAULT_CATEGORY_ID);
    }
    
    @Override
    public void onLockDrawer(boolean lock) {
        if(mNavigationDrawer!=null) {
            mNavigationDrawer.lockDrawer(lock);
        }
    }
    
    private void deleteCategory(int category, boolean alsoDelAccounts) {
        AccountManager.getInstance().removeCategory(category, alsoDelAccounts);
        int countAccounts = mNavigationDrawer.getCount(category);
        if(countAccounts > 0) {
            if(alsoDelAccounts) {
                MainListFragment.resetAdapter(AccountManager.ALL_CATEGORY_ID);
                mNavigationDrawer.increaseCounterInMenu(AccountManager.ALL_CATEGORY_ID,
                        -countAccounts);
            }
            else {
                MainListFragment.resetAdapter(AccountManager.DEFAULT_CATEGORY_ID);
                mNavigationDrawer.increaseCounterInMenu(AccountManager.DEFAULT_CATEGORY_ID,
                        countAccounts);
            }
        }
        MainListFragment.resetAdapter(category);
        mNavigationDrawer.remove(category);
        mNavigationDrawer.select(AccountManager.ALL_CATEGORY_ID);
        Application.showToast(this, R.string.category_deleted, Toast.LENGTH_SHORT);
        Application.reset();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        SyncService.getInstance().onActivityResult(requestCode, resultCode, data);
    }
    
    @Override
    public void onSyncFailed(int errorCode) {
        Application.showToast(this, R.string.sync_failed, Toast.LENGTH_SHORT);
        if(errorCode == SyncService.CA.NO_DATA) {
            SyncService.getInstance().send(mApp.getData());
        }
    }
    
    @Override
    public void onSyncProgress(int actionCode) {
        if(actionCode == SyncService.CA.AUTH) {
            mApp.ignoreNextPause();
        }
        else if(actionCode == SyncService.CA.DATA_RECEIVED) {
            mApp.onSyncSucceed();
            byte[] data = SyncService.getInstance().requestData();
            Application.FileHeader fh = Application.FileHeader.parse(data);
            if(fh.valid && fh.revision > mApp.getLocalVersion()) {
                Application.showToast(this, R.string.sync_success_local, Toast.LENGTH_SHORT);
                Application.Options.mSyncVersion = fh.revision;
                mApp.saveData(data);
                mApp.onVersionUpdated(fh.revision);    
                Application.reset();
                Application.getSortedCategoryNames();
                MainListFragment.clearCache();
            }
            else if(fh.revision < mApp.getLocalVersion()){
                SyncService.getInstance().send(mApp.getData());
            }
            if(fh.revision != Application.Options.mSyncVersion) {
                mApp.onVersionUpdated(fh.revision);
            }
        }
        else if(actionCode == SyncService.CA.DATA_SENT) {
            mApp.onSyncSucceed();
            Application.showToast(this, R.string.sync_success_server, Toast.LENGTH_SHORT);
            mApp.onVersionUpdated(mApp.getLocalVersion());
        }
    }
}
