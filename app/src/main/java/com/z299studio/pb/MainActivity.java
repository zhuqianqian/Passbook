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

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity implements ItemFragmentListener,
        NavigationDrawerFragment.NavigationDrawerCallbacks{

    private Application mApp;
    private NavigationDrawerFragment mNavigationDrawer;
    private DrawerLayout mDrawerLayout;
    private MainListFragment mMainList;
    private int mStatusColor;
    private View mRootView;
    private int mStatusColorDetail;

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
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        mNavigationDrawer.setUp(R.id.navigation_drawer, mDrawerLayout);
        mMainList = (MainListFragment)getSupportFragmentManager()
                .findFragmentById(R.id.panel_main);
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
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_delete_category:
                break;
            case R.id.action_edit_category:
                mMainList.editCategory();
                break;
            case R.id.action_help:
                break;
            case R.id.action_about:
                break;
            case R.id.action_settings:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onNavigationDrawerItemSelected(int type, int id) {
        if(type == NavigationDrawerAdapter.NavMenuItem.MENU_SELECTION) {
            mMainList.selectCategory(id, false);
        }
    }

    @Override
    public void onSelect(int id) {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_bottom, 0, 0, R.anim.slide_out_bottom)
                .replace(R.id.detail_panel, DetailFragment.create(id))
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setStatusBarColor(0,0,true);
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
        ft.replace(R.id.detail_panel, EditFragment.create(categoryId, accountId))
                .addToBackStack(null)
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
}
