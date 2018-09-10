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

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
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
import java.util.Hashtable;

import com.z299studio.pb.NavigationDrawerAdapter.NavMenuItem;

public class NavigationDrawerFragment extends Fragment implements
        AdapterView.OnItemClickListener{

    private final String SELECTION_KEY = "current_selection";

    public interface NavigationDrawerCallbacks {
        void onNavigationDrawerItemSelected(int type, int id);
    }

    private NavigationDrawerCallbacks mCallback;
    private DrawerLayout mDrawerLayout;
    private ListView mMenuList;
    private View mFragmentContainerView;
    public ActionBarDrawerToggle mDrawerToggle;
    private NavigationDrawerAdapter mAdapter;
    private int mCategory;
    private Hashtable<Integer, Integer> mCategory2Navigation;
    private boolean mDrawerHidden;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null) {
            mCategory = savedInstanceState.getInt(SELECTION_KEY);
        } else {
            mCategory = AccountManager.ALL_CATEGORY_ID;
        }
        mCategory2Navigation = new Hashtable<>();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(Application.getInstance() == null
                || Application.getInstance().getAccountManager() == null) {
            return null;
        }
        mDrawerHidden = getResources().getBoolean(R.bool.hide_drawer);
        mMenuList = (ListView)inflater.inflate(R.layout.fragment_navigation_drawer,
                container, false);
        mAdapter = new NavigationDrawerAdapter(getActivity(), buildMenuItems());
        mMenuList.setAdapter(mAdapter);
        int position = mCategory2Navigation.get(mCategory);
        mAdapter.selectItem(null, position);
        mMenuList.setOnItemClickListener(this);
        mMenuList.setItemChecked(position, true);
        return mMenuList;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(SELECTION_KEY, mCategory);
        super.onSaveInstanceState(outState);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if(Application.getInstance().queryChange(Application.DATA_OTHER)||
                Application.getInstance().queryChange(Application.DATA_ALL)) {
            mAdapter.setList(buildMenuItems());
            mAdapter.notifyDataSetChanged();
            select(AccountManager.ALL_CATEGORY_ID);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mCallback = (NavigationDrawerCallbacks) context;
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
        if(mDrawerToggle != null) {
            return mDrawerToggle.onOptionsItemSelected(item);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        NavMenuItem item = null;
        if(id == mCategory) {
            return;
        }
        if(mMenuList!=null) {
            item = (NavMenuItem)mMenuList.getItemAtPosition(position);
            if(item.mType == NavMenuItem.MENU_SELECTION) {
                mAdapter.selectItem(view, position);
                mCategory = (int)id;
            }
            mMenuList.setItemChecked(mCategory2Navigation.get(mCategory),  true);
        }

        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(mFragmentContainerView);
        }
        if (mCallback != null && item !=null) {
            mCallback.onNavigationDrawerItemSelected(item.mType, item.mId);
        }
    }

    public void setUp(int fragmentId, DrawerLayout drawerLayout) {
        mFragmentContainerView = getActivity() == null ? null : getActivity().findViewById(fragmentId);
        mDrawerLayout = drawerLayout;

        if (mDrawerLayout != null) {
            mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

            if (getActivity() != null) {

                ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
                if (actionBar != null) {
                    actionBar.setDisplayHomeAsUpEnabled(true);
                }
            }

            mDrawerToggle = new ActionBarDrawerToggle(getActivity(), mDrawerLayout,
                    R.string.navigation_drawer_open, R.string.navigation_drawer_close ) {
                @Override
                public void onDrawerClosed(View drawerView) {
                    super.onDrawerClosed(drawerView);
                    if (!isAdded()) {
                        return;
                    }
                    getActivity().invalidateOptionsMenu();
                }

                @Override
                public void onDrawerOpened(View drawerView) {
                    super.onDrawerOpened(drawerView);
                    if (!isAdded()) {
                        return;
                    }
                    getActivity().invalidateOptionsMenu();
                }
            };

            mDrawerLayout.post(() -> mDrawerToggle.syncState());

            mDrawerLayout.addDrawerListener(mDrawerToggle);
        }
    }

    private ArrayList<NavMenuItem> buildMenuItems() {
        Application app = Application.getInstance();
        Resources r = getResources();
        AccountManager am = app.getAccountManager();
        ArrayList<NavMenuItem> result = new ArrayList<>();
        int icons[] = Application.getThemedIcons();
        String[] categoryNames = app.getSortedCategoryNames();
        int[] categoryIcons = app.getSortedCategoryIcons();
        int[] categoryIds = app.getSortedCategoryIds();

        int pos = 0, i;
        if(mDrawerHidden) {
            result.add(new NavMenuItem(0, r.getString(R.string.app_name),
                    0, 0, NavMenuItem.MENU_HEADER));
            pos++;
        }
        result.add(new NavMenuItem(R.drawable.pb_all, r.getString(R.string.all_accounts),
                am.getAccountsCountByCategory(AccountManager.ALL_CATEGORY_ID),
                AccountManager.ALL_CATEGORY_ID, NavMenuItem.MENU_SELECTION));
        mCategory2Navigation.put(AccountManager.ALL_CATEGORY_ID, pos++);
        if(Application.Options.mShowOther) {
            result.add(new NavMenuItem(R.drawable.pb_unknown, r.getString(R.string.def_category),
                    am.getAccountsCountByCategory(AccountManager.DEFAULT_CATEGORY_ID),
                    AccountManager.DEFAULT_CATEGORY_ID, NavMenuItem.MENU_SELECTION));
            mCategory2Navigation.put(AccountManager.DEFAULT_CATEGORY_ID, pos++);
        }
        for(i = 1; i < categoryIcons.length; ++i) {
            result.add(new NavMenuItem(icons[categoryIcons[i]], categoryNames[i],
                    am.getAccountsCountByCategory(categoryIds[i]), categoryIds[i],
                    NavMenuItem.MENU_SELECTION));
            mCategory2Navigation.put(categoryIds[i], pos++);
        }
        if(mDrawerHidden) {
            result.add(new NavMenuItem(0, null, 0, 0,
                    NavMenuItem.MENU_SEPARATOR));
            int stringIds[] = {R.string.help, R.string.rate, R.string.settings};
            int iconIds[] = {R.drawable.ic_action_help, R.drawable.ic_rate_review,
                    R.drawable.ic_action_settings};
            for(i = 0; i < stringIds.length; ++i) {
                result.add(new NavMenuItem(iconIds[i], r.getString(stringIds[i]),
                        0, stringIds[i], NavMenuItem.MENU_ACTION));
            }
        }
        return result;
    }

    public void remove(int category) {
        if(category < 0) {
            mAdapter.setList(buildMenuItems());
            mAdapter.notifyDataSetChanged();
        }
        else {
            Integer pos = mCategory2Navigation.get(category);
            if(pos!=null) {
                mAdapter.remove(pos);
                for(int p = pos; p < mAdapter.getCount(); ++p) {
                    NavMenuItem nmi = (NavMenuItem) mAdapter.getItem(p);
                    if(nmi.mType == NavMenuItem.MENU_SELECTION) {
                        mCategory2Navigation.put(nmi.mId, p);
                    }
                }
            }
        }
    }
    
    public void select(int category) {
        Integer pos = mCategory2Navigation.get(category);
        if(pos!=null) {
            onItemClick(mMenuList, null, pos, category);
        }        
    }

    public void increaseCounterInMenu(int category, int delta) {
        Integer pos = mCategory2Navigation.get(category);
        if(pos != null) {
            int firstVisiblePosition = mMenuList.getFirstVisiblePosition();
            View view = mMenuList.getChildAt(pos - firstVisiblePosition);
            mAdapter.increaseCounterInMenu(view, pos, delta);
        }
    }
    
    public void refreshCategoryCounters() {
        int[] cateIds = Application.getInstance().getSortedCategoryIds();
        AccountManager am = Application.getInstance().getAccountManager();
        Integer pos;
        for(int id : cateIds) {
            pos = mCategory2Navigation.get(id);
            if(pos!=null) {
                mAdapter.updateCategoryCounter(null, pos, am.getAccountsCountByCategory(id));
            }
        }
        pos = mCategory2Navigation.get(AccountManager.ALL_CATEGORY_ID);
        if(pos!=null) {
            mAdapter.updateCategoryCounter(null, pos,
                    am.getAccountsCountByCategory(AccountManager.ALL_CATEGORY_ID));
        }
        mAdapter.notifyDataSetChanged();
    }

    public int getCount(int categoryId){
        Integer pos = mCategory2Navigation.get(categoryId);
        if(pos!=null) {
            return  mAdapter.getCounterInMenu(pos);
        }
        return 0;
    }
    
    public void lockDrawer(boolean lock) {
        if(mDrawerLayout!=null) {
            mDrawerLayout.setDrawerLockMode(lock ? 
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED : DrawerLayout.LOCK_MODE_UNLOCKED);
        }
        else {
            mMenuList.setEnabled(!lock);
        }
    }

}
