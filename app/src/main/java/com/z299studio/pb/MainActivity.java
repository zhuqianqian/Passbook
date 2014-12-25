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
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.transition.TransitionInflater;
import android.view.View;

import java.util.ArrayList;

public class MainActivity extends ActionBarActivity implements
        NavigationDrawerFragment.NavigationDrawerCallbacks,
        MainListFragment.ItemSelectionInterface  {

    private Application mApp;
    private Toolbar mToolbar;
    private NavigationDrawerFragment mNavigationDrawer;
    private DrawerLayout mDrawerLayout;
    private MainListFragment mMainList;
    private int mStatusColor;
    private View mRootView;

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
            int[] primaryColors = {R.attr.colorPrimary};
            TypedArray ta = obtainStyledAttributes(primaryColors);
            mStatusColor = ta.getColor(0,0);
            mRootView.setBackgroundColor(mStatusColor);
            ta.recycle();
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
    public void onSelectAccount(Fragment hostFragment, View view, long id) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment fragment = DetailFragment.create((int)id);
//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            View title = view.findViewById(R.id.item_name);
//            title.setTransitionName("title");
//            hostFragment.setSharedElementReturnTransition(
//                    TransitionInflater.from(this).inflateTransition(R.transition.change_bounds));
//            hostFragment.setExitTransition(
//                    TransitionInflater.from(this).inflateTransition(android.R.transition.explode));
//            hostFragment.setReturnTransition(
//                    TransitionInflater.from(this).inflateTransition(android.R.transition.explode));
//
//            fragment.setSharedElementEnterTransition(
//                    TransitionInflater.from(this).inflateTransition(R.transition.change_bounds));
//            fragment.setEnterTransition(
//                    TransitionInflater.from(this).inflateTransition(android.R.transition.explode));
//            ft.addSharedElement(title, "title");
//        }
//        else {
            ft.setCustomAnimations(R.anim.slide_in_right, 0, 0, R.anim.slide_out_right);
//        }
        ft.add(R.id.detail_panel, fragment)
                .addToBackStack(null)
                .commit();
    }

//    @Override
//    public void setStatusBarColor(int color) {
//        mRootView.setBackgroundColor(color);
//    }
//
//    @Override
//    public void onDetach(Fragment fragment) {
//        mRootView.setBackgroundColor(mStatusColor);
//    }

}
