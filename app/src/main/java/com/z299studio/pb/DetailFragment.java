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
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.appcompat.widget.Toolbar;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.z299studio.pb.AccountManager.Account;

import java.util.ArrayList;

public class DetailFragment extends Fragment implements
        AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener,
        View.OnClickListener, Toolbar.OnMenuItemClickListener, 
        ConfirmCopy.OnCopyConfirmListener{

    private static int[] COLORS = {R.color.pb_0, R.color.pb_1, R.color.pb_2, R.color.pb_3,
            R.color.pb_4, R.color.pb_5, R.color.pb_6, R.color.pb_7,
            R.color.pb_8, R.color.pb_9, R.color.pb_a, R.color.pb_b,
            R.color.pb_c, R.color.pb_d, R.color.pb_e, R.color.pb_f};

    private int mAccountId;
    private ListView mList;
    private AccountAdapter mAdapter;
    private int mColor;
    private Account mAccount;
    private ItemFragmentListener mListener;
    private Account.Entry mEntryToCopy;

    public static DetailFragment create(int accountId) {
        DetailFragment df = new DetailFragment();
        Bundle args = new Bundle();
        args.putInt(C.ACCOUNT, accountId);
        df.setArguments(args);
        return df;
    }

    public DetailFragment() {    }

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
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null) {
            mAccountId = savedInstanceState.getInt(C.ACCOUNT);
        }
        else {
            if (getArguments() != null) {
                mAccountId = getArguments().getInt(C.ACCOUNT);
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if(Application.getInstance() == null
                || Application.getInstance().getAccountManager() == null) {
            return null;
        }
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        mList = rootView.findViewById(android.R.id.list);
        mAccount = Application.getInstance().getAccountManager().getAccountById(mAccountId);
        if (getContext() != null) {
            mColor = ContextCompat.getColor(getContext(), COLORS[mAccount.getCategoryId() & 0x0f]);
        }
        setUpList();
        setupToolbar(rootView, mAccount.mProfile);
        View top = rootView.findViewById(R.id.top_frame);
        if(top!=null) {
            top.setOnClickListener(this);
        }
        return rootView;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(C.ACCOUNT, mAccountId);
        super.onSaveInstanceState(outState);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        mListener.onLockDrawer(true);
    }
    
    @Override
    public void onPause() {
        super.onPause();
        mListener.onLockDrawer(false);
    }
    
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
        mAdapter.changeDisplay(view, pos);
    }

    private void setUpList() {
        mAdapter = new AccountAdapter(getActivity(), mAccount);
        mAdapter.setShowPassword(Application.Options.mAlwaysShowPwd);
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(this);
        mList.setOnItemLongClickListener(this);
    }

    private void setupToolbar(View rootView, String title) {
        Toolbar toolbar;
        TextView titleView;
        toolbar = rootView.findViewById(R.id.toolbar);
        View close = rootView.findViewById(R.id.close);
        close.setOnClickListener(this);
        View header = rootView.findViewById(R.id.header);
        ImageButton fab = rootView.findViewById(R.id.fab);

        fab.setOnClickListener(this);
        header.setBackgroundColor(mColor);
        titleView = rootView.findViewById(android.R.id.title);
        titleView.setText(title);
        // Elevation to minus 1 so that fab would not be covered on 5.0
        float elevation = getResources().getDimension(R.dimen.fab_small_elevation) - 0.5f;
        ViewCompat.setElevation(header, elevation);
        if(rootView.findViewById(R.id.frame_box)==null) {
            MainActivity ma = (MainActivity) getActivity();
            if (ma != null) {
                ma.setStatusBarColor(mColor, 200, false);
            }
        }
        toolbar.inflateMenu(R.menu.menu_detail);
        toolbar.getMenu().getItem(0).getIcon().setColorFilter(
                C.ThemedColors[C.colorTextNormal], PorterDuff.Mode.SRC_ATOP);
        toolbar.setOnMenuItemClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.fab) {
            mListener.onEdit(mAccount.getCategoryId(), mAccountId);
        }
        else if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        if(menuItem.getItemId() == R.id.action_delete) {
            if(mListener!=null) {
                mListener.onDelete(mAccountId);
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            }
        }
        return true;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view,
                                   int position, long id) {
        Account.Entry entry = (Account.Entry) mAdapter.getItem(position);
        if(entry.mType == AccountManager.EntryType.PASSWORD ||
                entry.mType == AccountManager.EntryType.PIN) {
            if(Application.Options.mWarnCopyPwd) {
                if (getFragmentManager() != null) {
                    new ConfirmCopy().setListener(this).show(getFragmentManager(), "confirm_copy");
                }
                mEntryToCopy = entry;
            }
            else {
                if(!Application.Options.mEnableCopyPwd) {
                    return true;
                }
                copyToClipboard(entry.mName, entry.mValue);
            }
        }
        else {
            copyToClipboard(entry.mName, entry.mValue);
        }
        return true;
    }

    @Override
    public void onConfirmed(boolean confirmed, boolean remember) {
        if(confirmed) {
            copyToClipboard(mEntryToCopy.mName, mEntryToCopy.mValue);
        }
        if(remember) {
            SharedPreferences.Editor editor = Application.getInstance().mSP.edit();
            Application.Options.mWarnCopyPwd = !Application.Options.mWarnCopyPwd;
            editor.putBoolean(C.Keys.WARN_COPY, Application.Options.mWarnCopyPwd);
            if(!confirmed) {
                Application.Options.mEnableCopyPwd = false;
                editor.putBoolean(C.Keys.ENABLE_COPY, Application.Options.mEnableCopyPwd);
            }
            editor.apply();
        }
    }

    private class AccountAdapter extends BaseAdapter {
        private ArrayList<Account.Entry> mItems;
        private ArrayList<Boolean> mPwdShowed;
        private Context mContext;
        private boolean mShowPwd;

        class ViewHolder {
            TextView mName;
            TextView mValue;
        }

        AccountAdapter(Context context, Account account) {
            mContext = context;
            mItems = account.getEntryList();
        }

        void setShowPassword(boolean showPwd) {
            mShowPwd = showPwd;
            if(!showPwd) {
                mPwdShowed = new ArrayList<>(mItems.size());
                for (int i = 0; i < mItems.size(); ++i) {
                    mPwdShowed.add(Boolean.FALSE);
                }
            }
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            View v = convertView;
            Account.Entry entry = mItems.get(position);
            if(v == null) {
                holder = new ViewHolder();
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                assert inflater != null;
                v = inflater.inflate(R.layout.account_view_item, parent, false);

                holder.mName = v.findViewById(R.id.field_name);
                holder.mValue = v.findViewById(R.id.field_value);
                v.setTag(holder);

            }
            else{
                holder = (ViewHolder) v.getTag();
            }
            holder.mName.setText(entry.mName);
            if(!mShowPwd) {
                boolean showed = mPwdShowed.get(position);
                if(entry.mType == AccountManager.EntryType.PASSWORD ||
                        entry.mType == AccountManager.EntryType.PIN && !showed) {
                    holder.mValue.setTransformationMethod(
                            PasswordTransformationMethod.getInstance());
                }
                else {
                    holder.mValue.setTransformationMethod(
                            SingleLineTransformationMethod.getInstance());
                }
                if(entry.mType == AccountManager.EntryType.WEBADDR) {
                    holder.mValue.setAutoLinkMask(Linkify.WEB_URLS);
                }
            }
            holder.mValue.setText(entry.mValue);
            return v;
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        void changeDisplay(View view, int pos) {
            if(mShowPwd) {
                return;
            }
            Account.Entry entry = mItems.get(pos);
            if(entry.mType == AccountManager.EntryType.PASSWORD ||
                    entry.mType == AccountManager.EntryType.PIN) {
                boolean showed = mPwdShowed.get(pos);
                ViewHolder holder = (ViewHolder)view.getTag();
                if(showed) {
                    holder.mValue.setTransformationMethod(
                            PasswordTransformationMethod.getInstance());
                } else {
                    holder.mValue.setTransformationMethod(
                            SingleLineTransformationMethod.getInstance());
                }
                mPwdShowed.set(pos, !showed);
            }
        }
    }
    
    private void copyToClipboard(String label, String text) {
        if (getActivity() == null) {
            return;
        }
        ClipboardManager clipboardManager = (ClipboardManager)(getActivity().getSystemService(
                Context.CLIPBOARD_SERVICE));
        ClipData clipData = ClipData.newPlainText(label, text);
        if (clipboardManager != null) {
            clipboardManager.setPrimaryClip(clipData);
            Application.showToast(getActivity(), R.string.text_copied, Toast.LENGTH_SHORT);
        }
    }
}
