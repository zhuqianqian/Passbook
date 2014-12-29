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
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.Toolbar;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.z299studio.pb.AccountManager.Account;

import java.util.ArrayList;

public class DetailFragment extends Fragment implements
        AdapterView.OnItemClickListener,
        View.OnClickListener{

    private static int[] COLORS = {R.color.pb_0, R.color.pb_1, R.color.pb_2, R.color.pb_3,
            R.color.pb_4, R.color.pb_5, R.color.pb_6, R.color.pb_7,
            R.color.pb_8, R.color.pb_9, R.color.pb_a, R.color.pb_b,
            R.color.pb_c, R.color.pb_d, R.color.pb_e, R.color.pb_f};

    private int mAccountId;
    private ListView mList;
    private Toolbar mToolbar;
    private TextView mTitleView;
    private AccountAdapter mAdapter;
    private int mColor;
    private Account mAccount;
    private ItemFragmentListener mListener;

    public static DetailFragment create(int accountId) {
        DetailFragment df = new DetailFragment();
        Bundle args = new Bundle();
        args.putInt(C.ACCOUNT, accountId);
        df.setArguments(args);
        return df;
    }

    public DetailFragment() {    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (ItemFragmentListener)activity;
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(savedInstanceState!=null) {
            if(AccountManager.getInstance() == null) {
                return null;
            }
            mAccountId = savedInstanceState.getInt(C.ACCOUNT);
        }
        else {
            mAccountId = getArguments().getInt(C.ACCOUNT);
        }
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        mList = (ListView)rootView.findViewById(android.R.id.list);
        mAccount = AccountManager.getInstance().getAccountById(mAccountId);
        mColor = getResources().getColor(COLORS[mAccount.getCategoryId() & 0x0f]);
        setUpList();
        setupToolbar(rootView, mAccount.mProfile);
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(C.ACCOUNT, mAccountId);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        MainActivity ma = (MainActivity)getActivity();
        ma.onDetach(this);
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
    }

    private void setupToolbar(View rootView, String title) {
        mToolbar = (Toolbar)rootView.findViewById(R.id.toolbar);
        View header = rootView.findViewById(R.id.header);
        ImageButton fab = (ImageButton)rootView.findViewById(R.id.fab);
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            LayerDrawable background = (LayerDrawable) fab.getBackground();
            background.getDrawable(1).setColorFilter(C.ThemedColors[C.colorAccent],
                    PorterDuff.Mode.SRC_ATOP);
        }
        fab.setOnClickListener(this);
        header.setBackgroundColor(mColor);
        mTitleView = (TextView)rootView.findViewById(android.R.id.title);
        mTitleView.setText(title);
        // Elevation to minus 1 so that fab would not be covered on 5.0
        float elevation = getResources().getDimension(R.dimen.fab_small_elevation) - 0.5f;
        ViewCompat.setElevation(header, elevation);
        MainActivity ma = (MainActivity)getActivity();
        ma.setStatusBarColor(mColor);
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.fab) {
            mListener.onEdit(mAccount.getCategoryId(), mAccountId);
        }
    }

    private class AccountAdapter extends BaseAdapter {
        private ArrayList<Account.Entry> mItems;
        private ArrayList<Boolean> mPwdShowed;
        private Context mContext;
        private boolean mShowPwd;

        public class ViewHolder {
            public TextView mName;
            public TextView mValue;
        }

        public AccountAdapter(Context context, Account account) {
            mContext = context;
            mItems = account.getEntryList();
        }

        public void setShowPassword(boolean showPwd) {
            mShowPwd = showPwd;
            if(!showPwd) {
                mPwdShowed = new ArrayList<Boolean>(mItems.size());
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
                v = inflater.inflate(R.layout.account_view_item, parent, false);

                holder.mName = (TextView)v.findViewById(R.id.field_name);
                holder.mValue = (TextView)v.findViewById(R.id.field_value);
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
            }
            holder.mValue.setText(entry.mValue);
            return v;
        }

        public void update(Account account) {
            mItems = account.getEntryList();
            this.notifyDataSetChanged();
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

        public void changeDisplay(View view, int pos) {
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
}
