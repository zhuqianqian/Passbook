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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
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
    private int mFabToPush;
    private boolean mIsEditing = false;
    private View mCategoryEditView;
    private int mCategoryIcon;
    private String mCategoryName;
    private boolean mCategorySavable;
    private ImageView mCategoryIconView;

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
            return true;
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

    private ActionMode.Callback mEditCategoryCallback = new ActionMode.Callback(){
        
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            showFab(false);
            MenuInflater inflater = actionMode.getMenuInflater();
            inflater.inflate(R.menu.menu_edit, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            menu.getItem(0).getIcon().setColorFilter(
                    C.ThemedColors[mCategorySavable ? C.colorTextNormal : C.colorIconNormal],
                    PorterDuff.Mode.SRC_ATOP);
            menu.getItem(0).setEnabled(mCategorySavable);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            if(menuItem.getItemId() == R.id.action_save) {
                saveCategory();
            }
            mActionMode.finish();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            mActionMode = null;
            mIsEditing = false;
            mCategoryEditView.setVisibility(View.GONE);
            updateData(mCategoryId);
            showFab(true);
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
    
    public static void clearCache() {
        cachedAdapters.clear();
    }

    public static void resetAdapter(int categoryId) {
        AdapterHolder ah = cachedAdapters.get(categoryId);
        if(ah!=null) {
            ah.mUpToDate = false;
        }
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
        if(!getResources().getBoolean(R.bool.snackbar_left_align)) {
            mFabToPush = (int)(getResources().getDimension(R.dimen.snackbar_height_single) + 0.5f);
        }
        else {
            mFabToPush = 0;
        }
        mCategoryEditView = rootView.findViewById(R.id.category_editor);
        EditText editCategoryName = (EditText)rootView.findViewById(R.id.category_name);
        editCategoryName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                mCategoryName = s.toString();
                mCategorySavable = mCategoryName.length() > 0;
                mActionMode.invalidate();
            }
        });
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
            ((TextView)mListView.getEmptyView()).setText(R.string.no_accounts);
        }
    }
    
    public void setSearch(ArrayList<AccountManager.Account> result) {
        mAdapter = new MainListAdapter(getActivity(), result,
                Application.getThemedIcons(), R.drawable.pb_unknown);
        mAdapter.enableAnimation(false);
        mAdapter.setListener(this);
        mListView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
        mToBeRemoved = new int[mAdapter.getCount()];
        ((TextView)mListView.getEmptyView()).setText(R.string.empty_search);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
        if(mSelectionMode || mIsEditing) {
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
        if(!mActionModeDestroyed && !mIsEditing) {
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
        switch(view.getId()) {
            case R.id.fab:
                if (mListener != null) {
                    mListener.onEdit(mCategoryId, -1);
                }
                break;
            case R.id.category_icon:
                new IconSetter().setInitImage(mCategoryIcon)
                        .setListener(new IconSetter.OnIconChosen() {
                            @Override
                            public void onChosen(int id) {
                                mCategoryIcon = id;
                                mCategoryIconView.setImageResource(
                                        Application.getThemedIcons()[mCategoryIcon]);
                            }
                        })
                        .show(getFragmentManager(), "set_icon");
                break;
        }
    }
    
    public void onDelete(int accountId) {
        int firstVisiblePos = mListView.getFirstVisiblePosition();
        int removePos = mAdapter.getItemPosition(accountId, firstVisiblePos);
        mToBeRemoved[0] = removePos;
        mRemoveCount = 1;
        animateDeletion();
    }
    
    public void animateDeletion() {
        int firstVisiblePosition = mListView.getFirstVisiblePosition();
        int end = mRemoveCount - 1;
        View v;
        int viewHeight = 0;
        mAdapter.markDeletion(mToBeRemoved, mRemoveCount, true);
        for(int i = end; i >= 0; --i) {
            v = mListView.getChildAt(mToBeRemoved[i] - firstVisiblePosition);
            if(v!=null) {
                viewHeight = v.getMeasuredHeight();
                mAdapter.animateDeletion(v, mToBeRemoved[i]);
            }
        }
        showDeleteSnackbar((ActionBarActivity) getActivity(), mRemoveCount, viewHeight);
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
    
    public void showDeleteSnackbar(ActionBarActivity activity, int count, final int rowHeight) {
        new Snackbar()
                .setText(getResources().getQuantityString(R.plurals.info_deleted, count, count))
                .setActionText(getString(R.string.undo))
                .setActionListener(new Snackbar.OnActionListener() {
                    @Override
                    public void onAction() {
                        int firstVisiblePos = mListView.getFirstVisiblePosition();
                        int end = mRemoveCount - 1;
                        View v;
                        for(int i = end; i >= 0; --i){
                            v = mListView.getChildAt(mToBeRemoved[i] - firstVisiblePos);
                            if(v!=null) {
                                mAdapter.undoDelete(v, rowHeight);
                            }
                        }
                        mAdapter.markDeletion(mToBeRemoved, mRemoveCount, false);
                        mRemoveCount = 0;
                    }
                })
                .setDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if(mFabToPush > 0) {
                            mFab.animate().yBy(mFabToPush);
                        }
                        if(mRemoveCount > 0) {
                            mAdapter.doDelete(mToBeRemoved, mRemoveCount);
                            mListener.onDeleted(mCategoryId, mRemoveCount);
                            mRemoveCount = 0;
                        }
                    }
                })
                .show(activity);
        if(mFabToPush > 0) {
            mFab.animate().yBy(-mFabToPush);
        }
    }
    
    public void editCategory() {
        if(mIsEditing) {
            return;
        }
        mIsEditing = true;
        mActionMode = ((MainActivity)getActivity()).startSupportActionMode(mEditCategoryCallback);
        EditText editCategoryName = (EditText)mCategoryEditView.findViewById(R.id.category_name);
        mCategoryIconView = (ImageView)mCategoryEditView.findViewById(R.id.category_icon);
        mCategoryEditView.setVisibility(View.VISIBLE);
        int[] icons = Application.getThemedIcons();
        AccountManager.Category category = AccountManager.getInstance().getCategory(mCategoryId);
        if(mCategoryId > AccountManager.DEFAULT_CATEGORY_ID) {
            editCategoryName.setText(category.mName);
            mCategoryIconView.setImageResource(icons[category.mImgCode]);
            mCategoryIcon = category.mImgCode;
        }
        else {
            editCategoryName.setText("");
            mCategoryIconView.setImageResource(icons[icons.length-1]);
            mCategoryIcon = icons.length-1;
        }
        mCategoryIconView.setOnClickListener(this);
        mCategoryIconView.setColorFilter(C.ThemedColors[C.colorTextNormal]);
        updateListForEditing();
        
    }
    
    private void updateListForEditing() {
        if(mCategoryId != AccountManager.DEFAULT_CATEGORY_ID) {
            ArrayList<AccountManager.Account> accounts = AccountManager.getInstance()
                    .getAccountsByCategory(AccountManager.DEFAULT_CATEGORY_ID);
            if(mCategoryId == AccountManager.ALL_CATEGORY_ID) {
                mAdapter.addList(accounts, true);
            }
            else {
                mAdapter.markAll(mListView);
                mAdapter.addList(accounts, false);
                mAdapter.notifyDataSetChanged();
            }
        }
        
    }
    
    protected void saveCategory() {
        if(mCategoryId <= AccountManager.DEFAULT_CATEGORY_ID) {
            mCategoryId = AccountManager.getInstance().addCategory(mCategoryIcon, mCategoryName);
        }
        else {
            AccountManager.getInstance().setCategory(mCategoryId, mCategoryName, mCategoryIcon);
        }
        Application.showToast(getActivity(), R.string.category_saved, Toast.LENGTH_SHORT);
        mAdapter.moveData(mCategoryId);
        mListener.onCategorySaved();
    }
}
