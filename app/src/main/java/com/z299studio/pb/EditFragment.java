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

import android.content.ClipData;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
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
import java.util.Collections;

public class EditFragment extends Fragment implements View.OnClickListener,
        AdapterView.OnItemSelectedListener, TextWatcher, View.OnLongClickListener{

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
        public ImageButton mControl;
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

    private class DragEventListener implements View.OnDragListener {
        private int mIndex, mStart;
        private EntryHolder mDragged;

        public void startDrag(View v) {
            mDragged = (EntryHolder)v.getTag();
            mStart = mEntries.indexOf(mDragged);
            mDragged.mEntryContainer.setVisibility(View.INVISIBLE);
            mDragged.mEntryLayout.setAlpha(0.0f);
        }

        @Override
        public boolean onDrag(View v, DragEvent event) {
            final int action = event.getAction();

            switch(action) {
                case DragEvent.ACTION_DRAG_STARTED:
                    Log.d("Dragging (start)", String.format("view=%s", v.toString()));
                    return true;

                case DragEvent.ACTION_DRAG_ENTERED:
                    Log.d("Dragging (enter)", String.format("view=%s", v.toString()));
                    EntryHolder eh = (EntryHolder) v.getTag();
                    if(mDragged.mEntryLayout != eh.mEntryLayout) {
                        mIndex = mEntries.indexOf(eh);
                        mDragged.mEntryLayout.animate().setDuration(250).y(v.getY());
                        v.animate().setDuration(250).y(mDragged.mEntryLayout.getY());
                    }
                    return true;

                case DragEvent.ACTION_DRAG_ENDED:
                    Log.d("Dragging (end)", String.format("view=%s", v.toString()));
                    return true;

                case DragEvent.ACTION_DROP:
                    mDragged.mEntryContainer.setVisibility(View.VISIBLE);
                    mDragged.mEntryLayout.setAlpha(1.0f);
                    Collections.swap(mEntries, mStart, mIndex);
                    return true;

            }
            return false;
        }
    }

    private ItemFragmentListener mListener;
    private LinearLayout mContainer;
    private ImageButton mSaveFab;
    private EditText mPasswordView;
    private Spinner mCategorySpinner;
    private EditText mNameEditText;
    private boolean mReady = false;
    private boolean mSavable;
    private boolean mNameOk;
    private int mOldCategoryId;
    private int mPosition;
    private int mAccountId;
    private String mName;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(savedInstanceState!=null && AccountManager.getInstance()==null) {
            return null;
        }
        processArgs(savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_edit, container, false);
        mContainer = (LinearLayout)rootView.findViewById(android.R.id.list);
        View footer = inflater.inflate(R.layout.add_field, container, false);
        footer.setOnClickListener(this);
        mNameEditText = (EditText)rootView.findViewById(android.R.id.title);
        mSaveFab = (ImageButton)rootView.findViewById(R.id.fab);
        mSaveFab.setEnabled(false);
        mSaveFab.setOnClickListener(this);
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            LayerDrawable drawable = (LayerDrawable)mSaveFab.getBackground();
            drawable.getDrawable(1).setColorFilter(C.ThemedColors[C.colorAccent],
                    PorterDuff.Mode.SRC_ATOP);
            ImageView addView = (ImageView)footer.findViewById(R.id.plus_sign);
            drawable = (LayerDrawable)addView.getBackground();
            drawable.getDrawable(1).setColorFilter(C.ThemedColors[C.colorAccent],
                    PorterDuff.Mode.SRC_ATOP);
        }
        mNameEditText.addTextChangedListener(this);
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
    public void onSaveInstanceState(Bundle outState) {
       // outState.putString("account_name", mName);
        outState.putBoolean("edit_fragment_ready", mReady);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.auto_gen) {
            EntryHolder eh = (EntryHolder) v.getTag();
            requestPassword(eh.mValueField, eh.mEntryItem.mType);
        }
        else if(v.getId() == R.id.fab) {
            save();
        }
        else {
            onAddField(mDummyAccount.newEntry("", "", 1), mEntries.size());
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

    private void processArgs(Bundle savedInstanceState) {
        int categoryId = getArguments().getInt(C.CATEGORY, AccountManager.DEFAULT_CATEGORY_ID);
        mPosition = 0;
        int[] allIds = Application.getSortedCategoryIds();
        for(int i = 0; i < allIds.length; ++i) {
            if(categoryId == allIds[i]) {
                mPosition = i;
                break;
            }
        }
        mAccountId = getArguments().getInt(C.ACCOUNT, -1);
        mOldCategoryId = categoryId;
        mSavable = false;
        mNameOk = false;
        if(savedInstanceState!=null) {
            mReady = savedInstanceState.getBoolean("edit_fragment_ready", false);
        } else {
            mReady = false;
        }
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
     //   eh.mControl = (ImageButton)eh.mEntryLayout.findViewById(R.id.field_delete);
        eh.mEntryItem = e;
        eh.mTypeField.setAdapter(mTypeAdapter);
        eh.mTypeField.setTag(eh);
        eh.mNameField.setTag(eh);
        eh.mValueField.setTag(eh);
        eh.mAutoPwd.setTag(eh);
        eh.mEntryContainer.setTag(eh);
        eh.mEntryLayout.setTag(eh);
    //    eh.mControl.setTag(eh);
        mContainer.addView(eh.mEntryLayout, index);
        mEntries.add(eh);
        eh.mTypeField.setOnItemSelectedListener(this);
    //    eh.mControl.setOnClickListener(this);
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
        if(mSavable && mNameOk) {
            mSaveFab.setEnabled(true);
        }
        else {
            mSaveFab.setEnabled(false);
        }
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
