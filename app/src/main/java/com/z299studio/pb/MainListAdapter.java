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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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

    private static final long TIME_INTERVAL = 50;

    private ArrayList<AccountManager.Account> mEntries;
    private ArrayList<Integer> mIcons;
    private ArrayList<Boolean> mChecked;
    private ArrayList<Boolean> mDeleted;
    private int mCheckCount = 0;
    private Context mContext;
    private OnListItemCheckListener mListener;
    private int mDefaultDrawableId;
    private boolean mAnimationEnabled;
    private int mLastPosition;
    private long mLastTimestamp;
    private long mAdjustment;

    public MainListAdapter(Context context, ArrayList<AccountManager.Account> accounts,
                           int[] drawableResIds, int defaultDrawableResId) {
        mContext = context;
        mAnimationEnabled = true;
        mDefaultDrawableId = defaultDrawableResId;
        setList(accounts, drawableResIds);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder holder;
        AccountManager.Account account = mEntries.get(position);
        boolean status = mChecked.get(position);
        if (convertView == null) {
            v = inflate(parent);
        }
        else{
            holder = (ViewHolder) v.getTag();
            if(holder.mInflate) {
                v = inflate(parent);
            }
        }
//        if(mDeleted.get(position)) {
//            final View deletedView = v;
//            v.post(new Runnable() {
//                public void run() {
//                    animateDelete(deletedView, position);
//                }
//            });
//        }
        holder = (ViewHolder) v.getTag();
        holder.mTextView.setText(account.getAccountName());
//        int srcId = status ? R.drawable.checkmark : mIcons.get(position);
//        int bkgId = status ? mIconBkgAccent : mIconBkgNormal;
        holder.mIconView.setImageResource(mIcons.get(position));
//        holder.mIconView.setBackgroundResource(bkgId);
        holder.mIconView.setTag(position);

//        holder.mIconView.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                v.clearAnimation();
//                v.setAnimation(mAnimation1);
//                v.startAnimation(mAnimation1);
//                AnimationListener listener = getAnimListener((ImageButton) v, Integer.parseInt(v.getTag().toString()),
//                        mAnimation1, mAnimation2);
//                mAnimation1.setAnimationListener(listener);
//                mAnimation2.setAnimationListener(listener);
//            }
//        });
        if(mAnimationEnabled) {
            long timestamp = System.currentTimeMillis();
            long delta = timestamp - mLastTimestamp;
            mLastTimestamp = timestamp;

            Animation animation = AnimationUtils.loadAnimation(mContext,
                    position < mLastPosition ? R.anim.down_from_top : R.anim.up_from_bottom);
            if(delta > TIME_INTERVAL) {
                animation.setStartOffset(0);
                mAdjustment = 0;
            }
            else{
                mAdjustment += TIME_INTERVAL - delta;
                if(mAdjustment > 100) {
                    mAdjustment = 100;
                }
                animation.setStartOffset(mAdjustment);
            }
            v.startAnimation(animation);
        }

        mLastPosition = position;
        return v;
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

    private void setList(ArrayList<AccountManager.Account> accounts, int[] drawableResIds) {
        mEntries = accounts;
        mIcons = new ArrayList<Integer>();
        mChecked = new ArrayList<Boolean>();
        mDeleted = new ArrayList<Boolean>();
        int id;
        AccountManager am = AccountManager.getInstance();
        for(AccountManager.Account a : accounts) {
            id = am.getCategory(a.getCategoryId()).mImgCode;
            if(id == -1) {
                mIcons.add(mDefaultDrawableId);
            }else {
                mIcons.add(drawableResIds[id]);
            }
            mChecked.add(Boolean.FALSE);
            mDeleted.add(Boolean.FALSE);
        }
    }

    private View inflate(ViewGroup parent) {
        View v;
        ViewHolder holder = new ViewHolder();
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v = inflater.inflate(R.layout.account_list_item, parent, false);
        TextView titleView = (TextView) v.findViewById(R.id.item_name);
        ImageButton img = (ImageButton) v.findViewById(R.id.item_icon);

        holder.mIconView = img;
        holder.mTextView = titleView;
        holder.mInflate = false;
        v.setTag(holder);
        return v;
    }
}
