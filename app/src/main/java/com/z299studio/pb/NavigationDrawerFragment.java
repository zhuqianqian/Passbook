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
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

import com.z299studio.pb.NavigationDrawerAdapter.NavMenuItem;

public class NavigationDrawerFragment extends Fragment implements
        AdapterView.OnItemClickListener{

    private static final String SELECTION_KEY = "current_selection";

    public static interface NavigationDrawerCallbacks {
        void onNavigationDrawerItemSelected(int type, int id);
    }

    private NavigationDrawerCallbacks mCallback;
    private DrawerLayout mDrawerLayout;
    private ListView mMenuList;
    private View mFragmentContainerView;
    public ActionBarDrawerToggle mDrawerToggle;
    private NavigationDrawerAdapter mAdapter;
    private int mCurrentSelection;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null) {
            mCurrentSelection = savedInstanceState.getInt(SELECTION_KEY);
        } else {
            mCurrentSelection = 1;
        }
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState) {
        mMenuList = (ListView)inflater.inflate(R.layout.fragment_navigation_drawer, container, false);
        mAdapter = new NavigationDrawerAdapter(getActivity(), buildMenuItems());
        mMenuList.setAdapter(mAdapter);
        mMenuList.setOnItemClickListener(this);
        return mMenuList;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(SELECTION_KEY, mCurrentSelection);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (NavigationDrawerCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement NavigationDrawerCallbacks.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallback = null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        NavMenuItem item = null;
        if(mMenuList!=null) {
            item = (NavMenuItem)mMenuList.getItemAtPosition(position);
            if(item.mType == NavMenuItem.MENU_SELECTION) {
                mCurrentSelection = position;
            }
            mMenuList.setItemChecked(mCurrentSelection,  true);
        }

        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(mFragmentContainerView);
        }
        if (mCallback != null && item !=null) {
            mCallback.onNavigationDrawerItemSelected(item.mType, item.mId);
        }
    }

    public void setUp(int fragmentId, DrawerLayout drawerLayout) {
        mFragmentContainerView = getActivity().findViewById(fragmentId);
        mDrawerLayout = drawerLayout;

        if (mDrawerLayout != null) {
            mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

            ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);

            mDrawerToggle = new ActionBarDrawerToggle(getActivity(), mDrawerLayout,
                    R.string.navigation_drawer_open, R.string.navigation_drawer_close ) {
                @Override
                public void onDrawerClosed(View drawerView) {
                    super.onDrawerClosed(drawerView);
                    if (!isAdded()) {
                        return;
                    }
                    getActivity().supportInvalidateOptionsMenu();
                }

                @Override
                public void onDrawerOpened(View drawerView) {
                    super.onDrawerOpened(drawerView);
                    if (!isAdded()) {
                        return;
                    }
                    getActivity().supportInvalidateOptionsMenu();
                }
            };

            mDrawerLayout.post(new Runnable() {
                @Override
                public void run() {
                    mDrawerToggle.syncState();
                }
            });

            mDrawerLayout.setDrawerListener(mDrawerToggle);
        }
    }

    private ArrayList<NavMenuItem> buildMenuItems() {
        ArrayList<NavMenuItem> result = new ArrayList<NavMenuItem>();
        return result;
    }
}
