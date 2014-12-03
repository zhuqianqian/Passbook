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
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;

public class MainListAdapter extends BaseAdapter {

    public interface OnListItemCheckListener{
        public void onCheck(int count);
    }

    private static class ViewHolder {
        public ImageButton mIconView;
        public TextView mTextView;
        public boolean mInflate;
    }

    private ArrayList<AccountManager.Account> mEntries;
    private ArrayList<Integer> mIcons;
    private ArrayList<Boolean> mChecked;
    private ArrayList<Boolean> mDeleted;
    private int mCheckCount = 0;
    private Context mContext;
    private OnListItemCheckListener mListener;
    private int mDefaultDrawableId;

    public MainListAdapter(Context context, ArrayList<AccountManager.Account> accounts,
                           int[] drawableResIds, int defaultDrawableResId) {
        mDefaultDrawableId = defaultDrawableResId;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        return null;
    }

    @Override
    public int getCount() {  return mEntries.size();  }

    @Override
    public Object getItem(int position) { return mEntries.get(position); }

    @Override
    public long getItemId(int position) { return position;  }

    public void addList(ArrayList<AccountManager.Account> itemList, boolean reset) {
        if(reset) {
            setList(itemList, null);
            notifyDataSetChanged();
        }
        else {
            for(AccountManager.Account a : itemList) {
                mEntries.add(a);
                mChecked.add(Boolean.FALSE);
                mDeleted.add(Boolean.FALSE);
                mIcons.add(mDefaultDrawableId);
            }
        }
    }

    private void setList(ArrayList<AccountManager.Account> accounts, int[] icons) {

    }
}
