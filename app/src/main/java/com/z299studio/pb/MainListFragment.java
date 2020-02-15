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

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.Fragment;
import androidx.appcompat.view.ActionMode;
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

    private CoordinatorLayout mLayoutRoot;
    private ItemFragmentListener mListener;
    private ListView mListView;
    private MainListAdapter mAdapter;
    private int mCategoryId;
    private boolean mSelectionMode;
    private ActionMode mActionMode;
    private int [] mToBeRemoved;
    private int mRemoveCount;
    private boolean mActionModeDestroyed = false;
    private FloatingActionButton mFab;
    private Animation mFabIn, mFabOut;
    private boolean mIsEditing = false;
    private View mCategoryEditView;
    private int mCategoryIcon;
    private String mCategoryName;
    private boolean mCategorySavable;
    private ImageView mCategoryIconView;

    private static class AdapterHolder {
        MainListAdapter mAdapter;
        boolean mUpToDate;
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
            mListener.onLockDrawer(true);
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
            mListener.onLockDrawer(false);
        }

    };

    private ActionMode.Callback mEditCategoryCallback = new ActionMode.Callback(){
        
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            showFab(false);
            MenuInflater inflater = actionMode.getMenuInflater();
            inflater.inflate(R.menu.menu_edit, menu);
            mListener.onLockDrawer(true);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            Drawable saveDrawable = menu.getItem(0).getIcon();
            saveDrawable.setColorFilter(C.ThemedColors[C.colorTextNormal],
                    PorterDuff.Mode.SRC_ATOP);
            saveDrawable.setAlpha(mCategorySavable ? 255 : 138);
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
            mSelectionMode = false;
            mCategoryEditView.setVisibility(View.GONE);
            updateData(mCategoryId);
            showFab(true);
            mListener.onLockDrawer(false);
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
            mIsEditing = savedInstanceState.getBoolean("edit_category");
            mCategoryIcon = savedInstanceState.getInt("category_icon");
            mCategoryName = savedInstanceState.getString("category_name");
            IconSetter dialog = null;
            if (getFragmentManager() != null) {
                dialog = (IconSetter) getFragmentManager().findFragmentByTag("set_icon");
            }
            if(dialog!=null) {
                dialog.setListener(id -> {
                    mCategoryIcon = id;
                    mCategoryIconView.setImageResource(
                            Application.getThemedIcons()[mCategoryIcon]);
                });
            }
        }
        else {
            mCategoryId = AccountManager.ALL_CATEGORY_ID;
            mSelectionMode = mIsEditing = false;
            mCategoryIcon = -1;
            mCategoryName = null;
        }
        mFabIn = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_in_bottom);
        mFabOut = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_out_bottom);
        mFabIn.setAnimationListener(this);
        mFabOut.setAnimationListener(this);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt("category_id", mCategoryId);
        outState.putInt("category_icon", mCategoryIcon);
        outState.putString("category_name", mCategoryName);
        outState.putBoolean("selection_mode", mSelectionMode);
        outState.putBoolean("edit_category", mIsEditing);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(Application.getInstance() == null
                || Application.getInstance().getAccountManager() == null) {
            return null;
        }
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        mListView = rootView.findViewById(android.R.id.list);
        mLayoutRoot = rootView.findViewById(R.id.coordinator_layout);
        if((mAdapter = getAdapter(mCategoryId)) == null) {
            mAdapter = new MainListAdapter(getActivity(),
                    Application.getInstance().getAccountManager().getAccountsByCategory(mCategoryId),
                    Application.getThemedIcons(), R.drawable.pb_unknown);
            mListView.postDelayed(() -> mAdapter.disableAnimation(), 100);
            cacheAdapter(mCategoryId, mAdapter);
        }
        mAdapter.setListener(this);
        mListView.setAdapter(mAdapter);
        mListView.setEmptyView(rootView.findViewById(android.R.id.empty));
        mListView.setOnItemClickListener(this);
        mListView.setOnItemLongClickListener(this);
        mToBeRemoved = new int[mAdapter.getCount()];
        mFab = rootView.findViewById(R.id.fab);
        mFab.setOnClickListener(this);

        mCategoryEditView = rootView.findViewById(R.id.category_editor);
        EditText editCategoryName = rootView.findViewById(R.id.category_name);
        editCategoryName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                if(mActionMode!=null) {
                    mCategoryName = s.toString();
                    mCategorySavable = mCategoryName.length() > 0;
                    mActionMode.invalidate();
                }
            }
        });
        if(mIsEditing) {
            mIsEditing = false;
            editCategory();
        }
        else if(mSelectionMode) {
            if (getActivity() != null) {
                mActionMode = ((MainActivity) getActivity()).startSupportActionMode(mActionModeCallback);
            }
        }
        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (ItemFragmentListener)context;
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
                        Application.getInstance().getAccountManager()
                                .getAccountsByCategory(mCategoryId),
                        Application.getThemedIcons(), R.drawable.pb_unknown);
                mListView.postDelayed(() -> mAdapter.disableAnimation(), 100);
                cacheAdapter(mCategoryId, mAdapter);
            }
            mAdapter.setListener(this);
            mListView.setAdapter(mAdapter);
            mAdapter.notifyDataSetChanged();
            mToBeRemoved = new int[mAdapter.getCount()];
            ((TextView)mListView.getEmptyView()).setText(R.string.no_accounts);
            mCategoryIcon = -1;
            mCategoryName = null;
        }
    }
    
    public void setSearch(ArrayList<AccountManager.Account> result) {
        mAdapter = new MainListAdapter(getActivity(), result,
                Application.getThemedIcons(), R.drawable.pb_unknown);
        mAdapter.disableAnimation();
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
                if(mActionMode == null && getActivity() != null) {
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
                if (getFragmentManager() == null) {
                    return;
                }
                new IconSetter().setInitImage(mCategoryIcon)
                        .setListener(id -> {
                            mCategoryIcon = id;
                            mCategoryIconView.setImageResource(
                                    Application.getThemedIcons()[mCategoryIcon]);
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
        showDeleteSnackbar(mRemoveCount, viewHeight);
    }
    
    protected void showFab(boolean show) {
        mFab.clearAnimation();
        mFab.startAnimation(show ? mFabIn : mFabOut);
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

    public void updateDataImmediately() {
        mAdapter.setList(Application.getInstance().getAccountManager().getAccountsByCategory(mCategoryId),
                Application.getThemedIcons());
        mAdapter.notifyDataSetChanged();
        cacheAdapter(mCategoryId, mAdapter);
    }
    
    public void showDeleteSnackbar(int count, final int rowHeight) {
        Snackbar.make(mLayoutRoot,
                getResources().getQuantityString(R.plurals.info_deleted, count, count), Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, view -> {
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
                })
                .addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        if(mRemoveCount > 0) {
                            mAdapter.doDelete(mToBeRemoved, mRemoveCount);
                            mListener.onDeleted(mCategoryId, mRemoveCount);
                            mRemoveCount = 0;
                        }
                    }
                })
                .show();
    }
    
    public void editCategory() {
        if(mIsEditing) {
            return;
        }
        mIsEditing = true;
        if (getActivity() != null) {
            mActionMode = ((MainActivity) getActivity()).startSupportActionMode(mEditCategoryCallback);
        }
        EditText editCategoryName = mCategoryEditView.findViewById(R.id.category_name);
        mCategoryIconView = mCategoryEditView.findViewById(R.id.category_icon);
        mCategoryEditView.setVisibility(View.VISIBLE);
        int[] icons = Application.getThemedIcons();
        AccountManager.Category category = Application.getInstance()
                .getAccountManager().getCategory(mCategoryId);
        if(mCategoryName == null) {
            mCategoryName = mCategoryId > AccountManager.DEFAULT_CATEGORY_ID ? 
                    category.mName : "";
        }
        if(mCategoryIcon < 0) {
            mCategoryIcon = mCategoryId > AccountManager.DEFAULT_CATEGORY_ID ?
                    category.mImgCode : icons.length-1;
        }
        editCategoryName.setText(mCategoryName);
        mCategoryIconView.setImageResource(icons[mCategoryIcon]);
        mCategoryIconView.setOnClickListener(this);
        mCategoryIconView.setColorFilter(C.ThemedColors[C.colorTextNormal]);
        updateListForEditing();
        
    }
    
    private void updateListForEditing() {
        if(mCategoryId != AccountManager.DEFAULT_CATEGORY_ID) {
            ArrayList<AccountManager.Account> accounts = Application.getInstance()
                    .getAccountManager().getAccountsByCategory(AccountManager.DEFAULT_CATEGORY_ID);
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
            mCategoryId = Application.getInstance()
                    .getAccountManager().addCategory(mCategoryIcon, mCategoryName);
        }
        else {
            Application.getInstance()
                    .getAccountManager().setCategory(mCategoryId, mCategoryName, mCategoryIcon);
        }
        Application.showToast(getActivity(), R.string.category_saved, Toast.LENGTH_SHORT);
        mAdapter.moveData(mCategoryId);
        mListener.onCategorySaved();
    }
}
