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
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class MainActivity extends ActionBarActivity implements ItemFragmentListener,
        NavigationDrawerFragment.NavigationDrawerCallbacks,
        MainListFragment.ItemSelectionInterface  {

    private Application mApp;
    private Toolbar mToolbar;
    private NavigationDrawerFragment mNavigationDrawer;
    private DrawerLayout mDrawerLayout;
    private MainListFragment mMainList;
    private int mStatusColor;
    private View mRootView;

    private Runnable mTintStatusBar = new Runnable() {
        @Override
        public void run() {
            mRootView.setBackgroundColor(mStatusColor);
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
        mToolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        float elevation = getResources().getDimension(R.dimen.toolbar_elevation) + 0.5f;
        ViewCompat.setElevation(mToolbar, elevation);
    }

    @Override
    public void onNavigationDrawerItemSelected(int type, int id) {
        if(type == NavigationDrawerAdapter.NavMenuItem.MENU_SELECTION) {
            mMainList.selectCategory(id);
        }
    }

    @Override
    public void onSelectAccount(View view, long id) {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_bottom, 0, 0, R.anim.slide_out_bottom)
                .replace(R.id.detail_panel, DetailFragment.create((int)id))
                .addToBackStack(null)
                .commit();
    }

    public void setStatusBarColor(int color) {
        mRootView.setBackgroundColor(color);
    }

    public void onDetach(Fragment fragment) {
        mRootView.postDelayed(mTintStatusBar, 200);
    }

    @Override
    public void onEdit(int categoryId, int accountId) {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(0, 0, 0, R.anim.slide_out_bottom)
                .replace(R.id.detail_panel, EditFragment.create(categoryId, accountId))
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDelete(int accountId) {

    }

    @Override
    public void onSave(int category) {
        mNavigationDrawer.updateCounterInMenu(AccountManager.ALL_CATEGORY_ID, 1);
        mNavigationDrawer.updateCounterInMenu(category, 1);
        mNavigationDrawer.updateUi();
//           Update mainlist fragment data here (all_category_id)
//           Update mainlist fragment data here (category)
    }

    @Override
    public void onSaveChanged(int account, int category, int oldCategory, boolean nameChanged) {
        if(category!=oldCategory) {
            mNavigationDrawer.updateCounterInMenu(category, 1);
            mNavigationDrawer.updateCounterInMenu(oldCategory, -1);
            mNavigationDrawer.updateUi();
//           Update mainlist fragment data here (category)
//           Update mainlist fragment data here (oldcategory)
        }
        if(nameChanged) {
//           Update mainlist fragment data here (all_category_id)
//           Update mainlist fragment data here (category)
        }
    }
}
