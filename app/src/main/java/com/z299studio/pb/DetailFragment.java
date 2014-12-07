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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.z299studio.pb.AccountManager.Account;

import java.util.ArrayList;

public class DetailFragment extends Fragment implements AdapterView.OnItemClickListener{
    private int mAccountId;
    private ListView mList;
    private AccountAdapter mAdapter;

    public static DetailFragment create(int accountId) {
        DetailFragment df = new DetailFragment();
        Bundle args = new Bundle();
        args.putInt(C.ACCOUNT, accountId);
        df.setArguments(args);
        return df;
    }

    public DetailFragment() {}

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
        mList = (ListView)inflater.inflate(R.layout.fragment_list, container, false);
        Account account = AccountManager.getInstance().getAccountById(mAccountId);
        mAdapter = new AccountAdapter(getActivity(), account);
        mAdapter.setShowPassword(Application.Options.mAlwaysShowPwd);
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(this);
        return mList;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
        mAdapter.changeDisplay(view, pos);
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
