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

import android.animation.Animator;
import android.content.ClipData;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;

import com.z299studio.pb.AccountManager.Account.Entry;

import java.util.ArrayList;
import java.util.Collections;

public class EditFragment extends Fragment implements View.OnClickListener,
        AdapterView.OnItemSelectedListener, TextWatcher, View.OnLongClickListener,
        Toolbar.OnMenuItemClickListener, PbScrollView.PbScrollListener{

    private static final int[] INPUT_TYPES = {
            InputType.TYPE_CLASS_TEXT,
            InputType.TYPE_CLASS_TEXT,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_URI,
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_CLASS_NUMBER};

    private class EntryHolder{
        public EditText mNameField;
        public EditText mValueField;
        public Spinner mTypeField;
        public ImageView mControl;
        public ImageButton mAutoPwd;
        public View mEntryLayout;
        public View mEntryContainer;
        public Entry mEntryItem;
    }

    private class TextWatcherEx implements TextWatcher {
        private EditText mHost;
        public TextWatcherEx(EditText host) {
            mHost = host;
        }
        @Override
        public void afterTextChanged(Editable s) {
            if(!mReady) {
                return;
            }
            EntryHolder holder = (EntryHolder) mHost.getTag();
            String text = s.toString();
            if(mHost.getId() == R.id.field_name) {
                holder.mEntryItem.mName = text;
            }
            else {
                holder.mEntryItem.mValue = text;
            }
            boolean savable = false;
            if(!text.isEmpty()) {
                savable = true;
                for(EntryHolder eh : mEntries) {
                    if(eh.mEntryItem.mName.isEmpty() || eh.mEntryItem.mValue.isEmpty()) {
                        savable = false;
                        break;
                    }
                }
            }
            mSavable = savable;
            changeSaveStatus();
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }
    }

    private class DragEventListener implements View.OnDragListener, Animator.AnimatorListener {
        private int mIndex;
        private EntryHolder mDragged;
        private boolean mAnimating;
        private int mItemHeight;
        private int mAdjustScrollY, mScrollHeight;

        public void startDrag(View v) {
            mDragged = (EntryHolder)v.getTag();
            mDragged.mEntryContainer.setVisibility(View.INVISIBLE);
            mDragged.mEntryLayout.setAlpha(0.0f);
            mIndex = mEntries.indexOf(mDragged);
            mItemHeight = mDragged.mEntryLayout.getMeasuredHeight();
            mScrollHeight = mScroll.getMeasuredHeight();
            mAdjustScrollY = mScrollHeight - mItemHeight;
            if(mToolbarContainer!=null) {
                mAdjustScrollY -= mHeader.getMeasuredHeight();
            }
        }

        @Override
        public boolean onDrag(View v, DragEvent event) {
            final int action = event.getAction();

            switch(action) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return true;

                case DragEvent.ACTION_DRAG_ENTERED:
                    EntryHolder eh = (EntryHolder) v.getTag();
                    if (mDragged.mEntryLayout != eh.mEntryLayout && !mAnimating) {
                        int index = mEntries.indexOf(eh);
                        int insert = index;
                        int delta = index > mIndex ? -1 : 1;
                        float y = v.getY();
                        EntryHolder next;
                        while(index != mIndex) {
                            index += delta;
                            next = mEntries.get(index);
                            eh.mEntryLayout.animate().setDuration(250).y(next.mEntryLayout.getY());
                            eh = next;
                        }
                        mDragged.mEntryLayout.animate().setDuration(250).y(y).setListener(this);
                        mAnimating = true;
                        eh = mEntries.get(mIndex);
                        mEntries.remove(mIndex);
                        mEntries.add(insert, eh);
                        mIndex = insert;
                    }
                    return true;

                case DragEvent.ACTION_DRAG_ENDED:
                    mDragged.mEntryContainer.setVisibility(View.VISIBLE);
                    mDragged.mEntryLayout.setAlpha(1.0f);
                    return true;

                case DragEvent.ACTION_DRAG_LOCATION:
                    int yPosition = (int)v.getY();
                    int scrollY = mScroll.getScrollY();
                    if(yPosition < scrollY) {
                        mScroll.smoothScrollBy(0, -mItemHeight);
                    }
                    if(yPosition - scrollY > mAdjustScrollY) {
                        mScroll.smoothScrollBy(0, mItemHeight);
                    }
                    return true;

                case DragEvent.ACTION_DROP:
                    return true;

            }
            return false;
        }

        @Override
        public void onAnimationStart(Animator anim) { }

        @Override
        public void onAnimationEnd(Animator anim) {
            mAnimating = false;
        }

        @Override
        public void onAnimationRepeat(Animator anim) {}

        @Override
        public void onAnimationCancel(Animator anim) {}
    }

    private ItemFragmentListener mListener;
    private LinearLayout mContainer;
    private View mHeader;
    private EditText mPasswordView;
    private Spinner mCategorySpinner;
    private EditText mNameEditText;
    private PbScrollView mScroll;
    private Toolbar mToolbar;
    private Drawable mSaveDrawable;
    private View mToolbarContainer;

    private boolean mReady = false;
    private boolean mSavable;
    private boolean mNameOk;
  //  private boolean mScrollToolbar;
    private int mOldCategoryId;
    private int mPosition;
    private int mAccountId;
    private String mName;
    private int mElevation;
    private AccountManager.Account mDummyAccount;
    private ArrayAdapter<CharSequence> mTypeAdapter;
    private ArrayList<EntryHolder> mEntries;
    private DragEventListener mDragListener = new DragEventListener();

    public static EditFragment create(int category, int accountId) {
        EditFragment fragment = new EditFragment();
        Bundle args = new Bundle();
        args.putInt(C.CATEGORY, category);
        args.putInt(C.ACCOUNT, accountId);
        fragment.setArguments(args);
        return fragment;
    }

    public EditFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if(savedInstanceState == null) {
            int categoryId = getArguments().getInt(C.CATEGORY, AccountManager.DEFAULT_CATEGORY_ID);
            mPosition = 0;
            int[] allIds = Application.getSortedCategoryIds();
            for (int i = 0; i < allIds.length; ++i) {
                if (categoryId == allIds[i]) {
                    mPosition = i;
                    break;
                }
            }
            mAccountId = getArguments().getInt(C.ACCOUNT, -1);
            mOldCategoryId = categoryId;
        }
        else {
            mAccountId = savedInstanceState.getInt(C.ACCOUNT);
            mOldCategoryId = savedInstanceState.getInt(C.CATEGORY);
            mPosition = savedInstanceState.getInt("Category_Position");
        }
        mSavable = false;
        mNameOk = false;
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(savedInstanceState!=null && AccountManager.getInstance()==null) {
            return null;
        }
        View rootView = inflater.inflate(R.layout.fragment_edit, container, false);
        mContainer = (LinearLayout)rootView.findViewById(android.R.id.list);
        View footer = inflater.inflate(R.layout.add_field, container, false);
        footer.setOnClickListener(this);
        mNameEditText = (EditText)rootView.findViewById(android.R.id.title);
        mScroll = (PbScrollView)rootView.findViewById(R.id.scroll);
        mNameEditText.addTextChangedListener(this);
        mToolbarContainer = rootView.findViewById(R.id.toolbar_container);
        if(mToolbarContainer!=null) {
            mHeader = rootView.findViewById(R.id.header);
            mScroll.setPbScrollListener(this);
        }
        setupToolbar(rootView);
        mCategorySpinner = (Spinner)rootView.findViewById(R.id.category);
        if(mAccountId >= 0) {
            mDummyAccount = AccountManager.getInstance().getAccountById(mAccountId).clone();
            mName = mDummyAccount.getAccountName();
        }
        else {
            mDummyAccount = getEntryList();
            mName = "";
        }
        int spinnerLayout = android.os.Build.VERSION.SDK_INT>=android.os.Build.VERSION_CODES.JELLY_BEAN ?
                android.R.layout.simple_spinner_dropdown_item : R.layout.spinner_dropdown;
        mTypeAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.field_types, android.R.layout.simple_spinner_dropdown_item);
        mTypeAdapter.setDropDownViewResource(spinnerLayout);
        mEntries = new ArrayList<> ();

        int pos = 0;
        for(Entry e : mDummyAccount.getEntryList()) {
            onAddField(e, pos++);
        }
        mContainer.addView(footer);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item , Application.getSortedCategoryNames());
        spinnerAdapter.setDropDownViewResource(spinnerLayout);
        mCategorySpinner.setAdapter(spinnerAdapter);
        mCategorySpinner.setOnItemSelectedListener(this);
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(C.CATEGORY, mOldCategoryId);
        outState.putInt(C.ACCOUNT, mAccountId);
        outState.putInt("Category_Position", mPosition);
    }

    @Override
    public void onResume() {
        super.onResume();
        mNameEditText.setText(mName);
        mCategorySpinner.setSelection(mPosition);
        mReady = true;
        for(EntryHolder eh : mEntries) {
            eh.mNameField.setText(eh.mEntryItem.mName);
            eh.mValueField.setText(eh.mEntryItem.mValue);
            eh.mTypeField.setSelection(eh.mEntryItem.mType-1);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mName = mNameEditText.getText().toString();
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mNameEditText.getWindowToken(), 0);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.auto_gen:
                EntryHolder eh = (EntryHolder) v.getTag();
                requestPassword(eh.mValueField, eh.mEntryItem.mType);
                break;
            case R.id.close:
                getActivity().onBackPressed();
                break;
//            case R.id.fab:
//                save();
//                break;
//            case R.id.control:
//                break;
            default:
                onAddField(mDummyAccount.newEntry("", "", 1), mEntries.size());
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        ClipData not_used_clip = ClipData.newPlainText("", "");
        v.startDrag(not_used_clip, new View.DragShadowBuilder(v), v, 0);
        // DragEvent.ACTION_DRAG_STARTED not called in drag event dispatch.
        // Handle it here.
        mDragListener.startDrag(v);
        return true;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        if(parent.getId() == R.id.category) {
            mPosition = pos;
        }
        else {
            EntryHolder eh = (EntryHolder)parent.getTag();
            int type = pos + 1;
            eh.mEntryItem.mType = type;
            eh.mValueField.setInputType(INPUT_TYPES[type]);
            if(type == AccountManager.EntryType.PASSWORD || type == AccountManager.EntryType.PIN) {
                eh.mAutoPwd.setVisibility(View.VISIBLE);
            }
            else {
                eh.mAutoPwd.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}
    @Override
    public void afterTextChanged(Editable s) {
        if(s.toString().isEmpty()) {
            mNameOk = false;
        } else {
            mNameOk = true;
        }
        changeSaveStatus();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count,	int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if(item.getItemId() == R.id.action_save) {
            save();
        }
        return true;
    }

    @Override
    public void onScroll(int l, int t, int oldl, int oldt) {
        int height = mHeader.getMeasuredHeight() - mToolbar.getMeasuredHeight();
        Drawable drawable = mToolbarContainer.getBackground();
        int alpha = 255 *  (Math.min(Math.max(t, 0), height))/ height;
        drawable.setAlpha(alpha);
        if(alpha >= 255) {
            ViewCompat.setElevation(mToolbarContainer, mElevation);
            ViewCompat.setElevation(mHeader, 0);
        }
        else {
            ViewCompat.setElevation(mToolbarContainer, 0);
            ViewCompat.setElevation(mHeader, mElevation);
        }
    }

    private void setupToolbar(View rootView) {
        mToolbar = (Toolbar)rootView.findViewById(R.id.toolbar);
        if(mToolbarContainer!=null) {
            mToolbarContainer.setBackgroundColor(C.ThemedColors[C.colorPrimary]);
        }
        mToolbar.inflateMenu(R.menu.menu_edit);
        Menu menu = mToolbar.getMenu();
        mSaveDrawable = getResources().getDrawable(R.drawable.ic_action_save);
        mSaveDrawable.setColorFilter(C.ThemedColors[C.colorTextNormal], PorterDuff.Mode.SRC_ATOP);
        menu.getItem(0).setIcon(mSaveDrawable);
        mToolbar.setOnMenuItemClickListener(this);
        if(rootView.findViewById(R.id.frame_box) == null) {
            MainActivity ma = (MainActivity) getActivity();
            ma.setStatusBarColor(0, 0, true);
        }
        ImageButton close = (ImageButton)rootView.findViewById(R.id.close);
        close.setOnClickListener(this);
        mElevation = (int) (getResources().getDimension(R.dimen.toolbar_elevation) + 0.5f);
    }

    private void onAddField(Entry e, int index) {
        EntryHolder eh = new EntryHolder();

        eh.mEntryLayout = getActivity().getLayoutInflater()
                .inflate(R.layout.account_edit_item, mContainer, false);
        eh.mEntryContainer = eh.mEntryLayout.findViewById(R.id.field_container);
        eh.mNameField = (EditText)eh.mEntryLayout.findViewById(R.id.field_name);
        eh.mValueField = (EditText)eh.mEntryLayout.findViewById(R.id.field_value);
        eh.mTypeField = (Spinner)eh.mEntryLayout.findViewById(R.id.field_type);
        eh.mAutoPwd = (ImageButton)eh.mEntryLayout.findViewById(R.id.auto_gen);
//        eh.mControl = (ImageView)eh.mEntryLayout.findViewById(R.id.control);
        eh.mEntryItem = e;
        eh.mTypeField.setAdapter(mTypeAdapter);
        eh.mTypeField.setTag(eh);
        eh.mNameField.setTag(eh);
        eh.mValueField.setTag(eh);
        eh.mAutoPwd.setTag(eh);
        eh.mEntryContainer.setTag(eh);
        eh.mEntryLayout.setTag(eh);
//        eh.mControl.setTag(eh);
        mContainer.addView(eh.mEntryLayout, index);
        mEntries.add(eh);
        eh.mTypeField.setOnItemSelectedListener(this);
//        eh.mControl.setOnClickListener(this);
        eh.mAutoPwd.setOnClickListener(this);
        eh.mNameField.addTextChangedListener(new TextWatcherEx(eh.mNameField));
        eh.mValueField.addTextChangedListener(new TextWatcherEx(eh.mValueField));
        eh.mEntryContainer.setOnLongClickListener(this);
        eh.mEntryLayout.setOnDragListener(mDragListener);
    }

    private void requestPassword(EditText view, int type) {

    }

    private AccountManager.Account getEntryList() {
        int cateId = Application.getSortedCategoryIds()[mPosition];
        AccountManager.Account result = AccountManager.getInstance().getTemplate(cateId);
        if(result == null) {
            result = getDefaultTemplate(cateId);
        }
        return result;
    }

    private AccountManager.Account getDefaultTemplate(int id) {
        int intArrayIds[] = {R.array.index_5, R.array.index_0, R.array.index_1, R.array.index_2,
                R.array.index_3, R.array.index_4 };
        Resources r =  getResources();
        String[] defNames = r.getStringArray(R.array.def_field_names);
        int[] defTypes = r.getIntArray(R.array.def_field_types);
        int[] indexArray;
        AccountManager.Account account = AccountManager.getInstance().newAccount(id);
        if(id < intArrayIds.length) {
            indexArray = r.getIntArray(intArrayIds[id]);
        }
        else {
            indexArray = r.getIntArray(intArrayIds[0]);
        }
        for(int i : indexArray) {
            account.addEntry(defTypes[i], defNames[i], "");
        }

        return account;
    }

    private void changeSaveStatus() {
        boolean enable = false;
        if(mSavable && mNameOk) {
            mSaveDrawable.setColorFilter(C.ThemedColors[C.colorTextNormal],
                    PorterDuff.Mode.SRC_ATOP);
            enable = true;
        }
        else {
            mSaveDrawable.setColorFilter(C.ThemedColors[C.colorIconNormal],
                    PorterDuff.Mode.SRC_ATOP);
        }
        mToolbar.getMenu().getItem(0).setEnabled(enable);
    }

    private AccountManager.Account getAccount() {
        AccountManager.Account account  = AccountManager.getInstance().newAccount(mPosition);
        account.mId = mAccountId;
        for(EntryHolder eh : mEntries) {
            account.addEntry(eh.mEntryItem);
        }
        return account;
    }

    private void save() {
        String name = mNameEditText.getText().toString();
        AccountManager.Account account = getAccount();
        account.setName(name);
        int categoryId = Application.getSortedCategoryIds()[mPosition];
        account.setCategory(categoryId);
        if(mAccountId < 0) {
            AccountManager.getInstance().addAccount(categoryId, account);
            if(mListener!=null) {
                mListener.onSave(categoryId);
            }
        }
        else {
            AccountManager.getInstance().setAccount(mAccountId, account);
            if(mListener!=null) {
                mListener.onSaveChanged(mAccountId, categoryId, mOldCategoryId,
                        name.equals(account.getAccountName()));
            }
        }
        getActivity().onBackPressed();
    }
}
