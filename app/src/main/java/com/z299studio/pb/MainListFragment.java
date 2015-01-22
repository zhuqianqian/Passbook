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
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;

import java.util.Hashtable;

public class MainListFragment extends Fragment
implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener,
    MainListAdapter.OnListItemCheckListener, Animation.AnimationListener,
    View.OnClickListener{

    private ItemFragmentListener mListener;
    private ListView mListView;
    private MainListAdapter mAdapter;
    private int mCategoryId;
    private boolean mSelectionMode;
    private ActionMode mActionMode;
    private int [] mToBeRemoved;
    private int mRemoveCount;
    private boolean mActionModeDestroyed = false;
    private ImageButton mFab;
    private Animation mFabIn, mFabOut;

    private static class AdapterHolder {
        public MainListAdapter mAdapter;
        public boolean mUpToDate;
    }
    private static Hashtable<Integer, AdapterHolder> cachedAdapters = new Hashtable<>();

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        
        private boolean mFromMenu;
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_remove, menu);
            showFab(false);
            mActionModeDestroyed = false;
            mFromMenu = false;
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            menu.getItem(0).getIcon().setColorFilter(C.ThemedColors[C.colorTextNormal],
                    PorterDuff.Mode.SRC_ATOP);
            return false; // Return false if nothing is done
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_delete:
                    mRemoveCount = mAdapter.getSelected(mToBeRemoved);
                    mFromMenu = true;
                    reset();
                    return true;
                default:
                    mFromMenu = true;
                    reset();
                    return true;
            }
        }

        private void reset() {
            int begin = mListView.getFirstVisiblePosition();
            int end = mListView.getLastVisiblePosition();
            if(mAdapter.cancelSelection(mListView, begin,end) < 1 && mFromMenu) {
                mActionMode.finish();
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            showFab(true);
            mActionModeDestroyed = true;
            if(!mFromMenu) {
                reset();
            }
            mActionMode = null;
        }

    };

    private static MainListAdapter getAdapter( int category_id) {
        AdapterHolder ah = cachedAdapters.get(category_id);
        if(ah!=null && ah.mUpToDate) {
            return ah.mAdapter;
        }
        return null;
    }

    private static void cacheAdapter(int categoryId, MainListAdapter adapter) {
        AdapterHolder ah = cachedAdapters.get(categoryId);
        if(ah==null) {
            ah = new AdapterHolder();
        }
        ah.mUpToDate = true;
        ah.mAdapter = adapter;
        cachedAdapters.put(categoryId, ah);
    }

    public MainListFragment() {   }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null) {
            mCategoryId = savedInstanceState.getInt("category_id");
            mSelectionMode = savedInstanceState.getBoolean("selection_mode");
        }
        else {
            mCategoryId = AccountManager.ALL_CATEGORY_ID;
            mSelectionMode = false;
        }
        mFabIn = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_in_bottom);
        mFabOut = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_out_bottom);
        mFabIn.setAnimationListener(this);
        mFabOut.setAnimationListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("category_id", mCategoryId);
        outState.putBoolean("selection_mode", mSelectionMode);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        mListView = (ListView)rootView.findViewById(android.R.id.list);
        if((mAdapter = getAdapter(mCategoryId)) == null) {
            mAdapter = new MainListAdapter(getActivity(),
                    AccountManager.getInstance().getAccountsByCategory(mCategoryId),
                    Application.getThemedIcons(), R.drawable.pb_unknown);
            mListView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mAdapter.enableAnimation(false);
                }
            }, 100);
            cacheAdapter(mCategoryId, mAdapter);
        }
        mAdapter.setListener(this);
        mListView.setAdapter(mAdapter);
        mListView.setEmptyView(rootView.findViewById(android.R.id.empty));
        mListView.setOnItemClickListener(this);
        mListView.setOnItemLongClickListener(this);
        mToBeRemoved = new int[mAdapter.getCount()];
        mFab = (ImageButton)rootView.findViewById(R.id.fab);
        mFab.setOnClickListener(this);
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            LayerDrawable background = (LayerDrawable) mFab.getBackground();
            background.getDrawable(1).setColorFilter(C.ThemedColors[C.colorAccent],
                    PorterDuff.Mode.SRC_ATOP);
        }
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (ItemFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement ItemSelectionInterface.");
        }
    }

    @Override
    public void onAnimationStart(Animation animation) { }

    @Override
    public void onAnimationEnd(Animation animation) {
        if(animation == mFabIn) {
            mFab.setVisibility(View.VISIBLE);
            mActionModeDestroyed = false;
            if(mRemoveCount > 0) {
                showDeleteSnackbar((ActionBarActivity) getActivity(), mRemoveCount);
                animateDeletion();
            }
        }
        else if(animation == mFabOut) {
            mFab.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onAnimationRepeat(Animation animation) { }

    public void selectCategory(int category_id, boolean forceUpdate) {
        if(mCategoryId != category_id || forceUpdate) {
            mCategoryId = category_id;
            if((mAdapter = getAdapter(mCategoryId)) == null) {
                mAdapter = new MainListAdapter(getActivity(),
                        AccountManager.getInstance().getAccountsByCategory(mCategoryId),
                        Application.getThemedIcons(), R.drawable.pb_unknown);
                mListView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.enableAnimation(false);
                    }
                }, 100);
                cacheAdapter(mCategoryId, mAdapter);
            }
            mAdapter.setListener(this);
            mListView.setAdapter(mAdapter);
            mAdapter.notifyDataSetChanged();
            mToBeRemoved = new int[mAdapter.getCount()];
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
        if(mSelectionMode) {
            onItemLongClick(parent, view, pos, id);
            return;
        }
        if(mListener != null) {
            mListener.onSelect((int)id);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
        mAdapter.onLongClick(view, pos);
        return true;
    }

    @Override
    public void onCheck(int count, int position, boolean isChecked) {
        if(count == 0) {
            mSelectionMode = false;
        }
        else if(count == 1) {
            mSelectionMode = true;
        }
        if(!mActionModeDestroyed) {
            if(count > 0) {
                if(mActionMode == null) {
                    mActionMode = ((MainActivity)getActivity()).startSupportActionMode(mActionModeCallback);
                }
            }
            else if(mActionMode != null) {
                mActionMode.finish();
            }
        }
    }
    
    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.fab) {
            if(mListener!=null) {
                mListener.onEdit(mCategoryId, -1);
            }
        }
    }
    
    public void onDelete(int accountId) {
        int firstVisiblePos = mListView.getFirstVisiblePosition();
        int removePos = mAdapter.getItemPosition(accountId, firstVisiblePos);
        View v = mListView.getChildAt(removePos - firstVisiblePos);
        mAdapter.animateDeletion(v, removePos);
        showDeleteSnackbar((ActionBarActivity) getActivity(), 1);
    }
    
    public void animateDeletion() {
        int firstVisiblePosition = mListView.getFirstVisiblePosition();
        int end = mRemoveCount - 1;
        View v;
        mAdapter.markDeletion(mToBeRemoved, mRemoveCount, true);
        for(int i = end; i >= 0; --i) {
            v = mListView.getChildAt(mToBeRemoved[i] - firstVisiblePosition);
            if(v!=null) {
                mAdapter.animateDeletion(v, mToBeRemoved[i]);
            }
        }
    }
    
    protected void showFab(boolean show) {
        mFab.clearAnimation();
        mFab.startAnimation(show? mFabIn : mFabOut);
    }
    
    public void updateData(int categoryId) {
        AdapterHolder ah = cachedAdapters.get(categoryId);
        if(ah!=null) {
            ah.mUpToDate = false;
        }
        if(categoryId == mCategoryId) {
            selectCategory(categoryId, true);
        }
    }
    
    public void showDeleteSnackbar(ActionBarActivity activity, int count) {
        new Snackbar()
                .setText(getResources().getQuantityString(R.plurals.info_deleted, count, count))
                .setActionText(getString(R.string.undo))
                .setActionListener(new Snackbar.OnActionListener() {
                    @Override
                    public void onAction() {

                    }
                })
                .setDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {

                    }
                })
                .show(activity);
    }
}
