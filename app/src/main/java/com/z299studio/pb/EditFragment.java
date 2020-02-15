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

import android.content.ClipData;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.core.view.ViewCompat;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.DragEvent;
import android.view.LayoutInflater;
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
import android.widget.Spinner;

import com.z299studio.pb.AccountManager.Account.Entry;

import java.util.ArrayList;

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
        EditText mNameField;
        EditText mValueField;
        Spinner mTypeField;
        ImageButton mAutoPwd;
        View mEntryLayout;
        View mEntryContainer;
        Entry mEntryItem;
    }

    private class TextWatcherEx implements TextWatcher {
        private EditText mHost;
        TextWatcherEx(EditText host) {
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

    private class DragEventListener implements View.OnDragListener{
        private int mIndex;
        private EntryHolder mDragged;
        private int mItemHeight;
        private int mAdjustScrollY, mScrollHeight;

        void startDrag(View v) {
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
            mDeleteView.setX(mDragged.mAutoPwd.getX());
            float y = mDragged.mEntryLayout.getY() + (mDragged.mEntryLayout.getMeasuredHeight()
                    - mDeleteView.getMeasuredHeight()) / 2;
            mDeleteView.setY(y);
            mDeleteView.setVisibility(View.VISIBLE);
        }

        @Override
        public boolean onDrag(View v, DragEvent event) {
            final int action = event.getAction();
            switch(action) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return true;

                case DragEvent.ACTION_DRAG_ENTERED:
                    if(v==mDeleteView) {
                        mDeleteView.setColorFilter(C.ThemedColors[C.colorAccent]);
                        mDeleteView.setScaleX(1.2f);
                        mDeleteView.setScaleY(1.2f);
                    }
                    else {
                        mDeleteView.setColorFilter(C.ThemedColors[C.colorTextNormal]);
                        mDeleteView.setScaleX(1.0f);
                        mDeleteView.setScaleY(1.0f);
                        EntryHolder eh = (EntryHolder) v.getTag();
                        if (mDragged.mEntryLayout != eh.mEntryLayout) {
                            int index = mEntries.indexOf(eh);
                            mDeleteView.animate().setDuration(300).y(v.getY() +
                                    (mItemHeight - mDeleteView.getMeasuredHeight())/2);
                            eh = mEntries.get(mIndex);
                            mEntries.remove(mIndex);
                            mEntries.add(index, eh);
                            mContainer.removeViewAt(mIndex);
                            mContainer.addView(eh.mEntryLayout,index);
                            mIndex = index;
                        }
                    }
                    return true;

                case DragEvent.ACTION_DRAG_ENDED:
                    mDeleteView.setVisibility(View.INVISIBLE);
                    if(v!=mDeleteView) {
                        mDragged.mEntryContainer.setVisibility(View.VISIBLE);
                        mDragged.mEntryLayout.setAlpha(1.0f);
                    }
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
                    if(v==mDeleteView) {
                        delete(mDragged);
                    }
                    return true;

            }
            return false;
        }
    }

    private ItemFragmentListener mListener;
    private LinearLayout mContainer;
    private View mHeader;
    private Spinner mCategorySpinner;
    private EditText mNameEditText;
    private PbScrollView mScroll;
    private Toolbar mToolbar;
    private View mToolbarContainer;
    private ImageView mDeleteView;
    private Application mApp;
    private boolean mReady = false;
    private boolean mSavable;
    private boolean mNameOk;
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
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (ItemFragmentListener)context;
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mApp = Application.getInstance();
        if(savedInstanceState == null) {
            if (getArguments() == null) {
                return;
            }
            int categoryId = getArguments().getInt(C.CATEGORY, AccountManager.DEFAULT_CATEGORY_ID);
            mPosition = 0;
            int[] allIds = mApp.getSortedCategoryIds();
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
        mListener.onLockDrawer(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(mApp == null || mApp.getAccountManager() == null) {
            return null;
        }
        View rootView = inflater.inflate(R.layout.fragment_edit, container, false);
        mContainer = rootView.findViewById(android.R.id.list);
        View footer = inflater.inflate(R.layout.add_field, container, false);
        footer.setOnClickListener(this);
        mNameEditText = rootView.findViewById(android.R.id.title);
        mScroll = rootView.findViewById(R.id.scroll);
        mNameEditText.addTextChangedListener(this);
        mToolbarContainer = rootView.findViewById(R.id.toolbar_container);
        if(mToolbarContainer!=null) {
            mHeader = rootView.findViewById(R.id.header);
            mScroll.setPbScrollListener(this);
        }
        setupToolbar(rootView);
        mCategorySpinner = rootView.findViewById(R.id.category);
        if(mAccountId >= 0) {
            mDummyAccount = mApp.getAccountManager().getAccountById(mAccountId).clone();
            mName = mDummyAccount.getAccountName();
        }
        else {
            mDummyAccount = getEntryList();
            mName = "";
        }
        int spinnerLayout = android.os.Build.VERSION.SDK_INT>=android.os.Build.VERSION_CODES.JELLY_BEAN ?
                android.R.layout.simple_spinner_dropdown_item : R.layout.spinner_dropdown;
        if (getActivity() != null) {
            mTypeAdapter = ArrayAdapter.createFromResource(getActivity(),
                    R.array.field_types, android.R.layout.simple_spinner_dropdown_item);
            mTypeAdapter.setDropDownViewResource(spinnerLayout);
        }
        mEntries = new ArrayList<> ();
        mDeleteView = (ImageView)inflater.inflate(R.layout.delete_field, container, false);
        int pos = 0;
        for(Entry e : mDummyAccount.getEntryList()) {
            onAddField(e, pos++);
        }
        mContainer.addView(footer);
        mContainer.addView(mDeleteView);
        mDeleteView.setOnDragListener(mDragListener);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item , mApp.getSortedCategoryNames());
        spinnerAdapter.setDropDownViewResource(spinnerLayout);
        mCategorySpinner.setAdapter(spinnerAdapter);
        mCategorySpinner.setOnItemSelectedListener(this);
        View top = rootView.findViewById(R.id.top_frame);
        if(top!=null) {
            top.setOnClickListener(this);
        }
        return rootView;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
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
        if (getActivity() == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(mNameEditText.getWindowToken(), 0);
        }
    }
    
    @Override
    public void onDetach() {
        super.onDetach();
        mListener.onLockDrawer(false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.auto_gen:
                EntryHolder eh = (EntryHolder) v.getTag();
                requestPassword(eh.mValueField, eh.mEntryItem.mType);
                break;
            case R.id.close:
            case R.id.top_frame:
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
                break;
            default:
                onAddField(mDummyAccount.newEntry("", "", 1), mEntries.size());
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        ClipData not_used_clip = ClipData.newPlainText("", "");
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            v.startDrag(not_used_clip, new View.DragShadowBuilder(v), v, 0);
        }
        else {
            v.startDragAndDrop(not_used_clip, new View.DragShadowBuilder(v), v, 0);
        }
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
        mNameOk = !s.toString().isEmpty();
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
        mToolbar = rootView.findViewById(R.id.toolbar);
        if(mToolbarContainer!=null) {
            mToolbarContainer.setBackgroundColor(C.ThemedColors[C.colorPrimary]);
        }
        mToolbar.inflateMenu(R.menu.menu_edit);
        mToolbar.getMenu().getItem(0).getIcon().setColorFilter(
                C.ThemedColors[C.colorTextNormal], PorterDuff.Mode.SRC_ATOP);
        mToolbar.setOnMenuItemClickListener(this);
        if(rootView.findViewById(R.id.frame_box) == null) {
            MainActivity ma = (MainActivity) getActivity();
            if (ma != null) {
                ma.setStatusBarColor(0, 0, true);
            }
        }
        ImageButton close = rootView.findViewById(R.id.close);
        close.setOnClickListener(this);
        mElevation = (int) (getResources().getDimension(R.dimen.toolbar_elevation) + 0.5f);
    }

    private void onAddField(Entry e, int index) {
        EntryHolder eh = new EntryHolder();
        if (getActivity() == null) {
            return;
        }
        eh.mEntryLayout = getActivity().getLayoutInflater()
                .inflate(R.layout.account_edit_item, mContainer, false);
        eh.mEntryContainer = eh.mEntryLayout.findViewById(R.id.field_container);
        eh.mNameField = eh.mEntryLayout.findViewById(R.id.field_name);
        eh.mValueField = eh.mEntryLayout.findViewById(R.id.field_value);
        eh.mTypeField = eh.mEntryLayout.findViewById(R.id.field_type);
        eh.mAutoPwd = eh.mEntryLayout.findViewById(R.id.auto_gen);
        eh.mEntryItem = e;
        eh.mTypeField.setAdapter(mTypeAdapter);
        eh.mTypeField.setTag(eh);
        eh.mNameField.setTag(eh);
        eh.mValueField.setTag(eh);
        eh.mAutoPwd.setTag(eh);
        eh.mEntryContainer.setTag(eh);
        eh.mEntryLayout.setTag(eh);
        mContainer.addView(eh.mEntryLayout, index);
        mEntries.add(eh);
        eh.mTypeField.setOnItemSelectedListener(this);
        eh.mAutoPwd.setOnClickListener(this);
        eh.mNameField.addTextChangedListener(new TextWatcherEx(eh.mNameField));
        eh.mValueField.addTextChangedListener(new TextWatcherEx(eh.mValueField));
        eh.mEntryContainer.setOnLongClickListener(this);
        eh.mEntryLayout.setOnDragListener(mDragListener);
    }

    private void requestPassword(EditText view, int type) {
        if (getFragmentManager() == null) {
            return;
        }
        PasswordGenerator.build(type, view).show(getFragmentManager(), "generate");
    }

    private AccountManager.Account getEntryList() {
        int cateId = mApp.getSortedCategoryIds()[mPosition];
        AccountManager.Account result = mApp.getAccountManager().getTemplate(cateId);
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
        AccountManager.Account account = mApp.getAccountManager().newAccount(id);
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

    private void delete(EntryHolder entry) {
        mContainer.removeView(entry.mEntryLayout);
        mEntries.remove(entry);
        boolean savable = true;
        for(EntryHolder eh : mEntries) {
            if(eh.mEntryItem.mName.isEmpty() || eh.mEntryItem.mValue.isEmpty()) {
                savable = false;
                break;
            }
        }
        mSavable = mEntries.size() > 0 && savable;
        changeSaveStatus();
    }

    private void changeSaveStatus() {
        boolean enable = mSavable && mNameOk;
        mToolbar.getMenu().getItem(0).getIcon().setAlpha(enable ? 255 : 138);
        mToolbar.getMenu().getItem(0).setEnabled(enable);
    }

    private AccountManager.Account getAccount() {
        AccountManager.Account account  = mApp.getAccountManager().newAccount(mPosition);
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
        int categoryId = mApp.getSortedCategoryIds()[mPosition];
        account.setCategory(categoryId);
        if(mAccountId < 0) {
            mApp.getAccountManager().addAccount(categoryId, account);
            if(mListener!=null) {
                mListener.onSave(categoryId);
            }
        }
        else {
            mApp.getAccountManager().setAccount(account);
            if(mListener!=null) {
                mListener.onSaveChanged(mAccountId, categoryId, mOldCategoryId,
                        name.equals(account.getAccountName()));
            }
        }
        if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }
}
