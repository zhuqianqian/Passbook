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
import android.view.animation.Animation.AnimationListener;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;

public class MainListAdapter extends BaseAdapter {

    public interface OnListItemCheckListener{
        public void onCheck(int count, int position, boolean isChecked);
    }

    private static class ViewHolder {
        public ImageButton mIconView;
        public TextView mTextView;
        public boolean mInflate;
    }

    private static final long TIME_INTERVAL = 50;
    private static int COLORS[];
    private static Animation FLIP1, FLIP2;

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
        prepareResources(context);
    }

    private void prepareResources(Context context) {
        if(COLORS == null) {
            COLORS = new int[] {
//When Android 4.0.x is not supported, the drawables can be created dynamically to setBackground
                    R.drawable.oval_00, R.drawable.oval_01, R.drawable.oval_02, R.drawable.oval_03,
                    R.drawable.oval_04, R.drawable.oval_05, R.drawable.oval_06, R.drawable.oval_07,
                    R.drawable.oval_08, R.drawable.oval_09, R.drawable.oval_0a, R.drawable.oval_0b,
                    R.drawable.oval_0c, R.drawable.oval_0d, R.drawable.oval_0e, R.drawable.oval_0f
            };
//            int[] colors = {R.attr.iconColorNormal, R.attr.textColorNormal};
//            TypedArray ta = mContext.obtainStyledAttributes(colors);
//            context.getResources().getDrawable(R.drawable.oval_selected).setColorFilter(
//                    ta.getColor(1, 0) , PorterDuff.Mode.SRC_ATOP);
//            ta.recycle();
            FLIP1 = AnimationUtils.loadAnimation(mContext, R.anim.shrink_to_middle);
            FLIP2 = AnimationUtils.loadAnimation(mContext, R.anim.expand_from_middle);
        }
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder holder;
        AccountManager.Account account = mEntries.get(position);
        boolean status = mChecked.get(position);
        if (convertView == null) {
            view = inflate(parent);
        }
        else{
            holder = (ViewHolder) view.getTag();
            if(holder.mInflate) {
                view = inflate(parent);
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
        holder = (ViewHolder) view.getTag();
        holder.mTextView.setText(account.getAccountName());
        int srcId = status ? R.drawable.checkmark : mIcons.get(position);
        holder.mIconView.setImageResource(srcId);
        int background = status ? R.drawable.oval_selected :
                COLORS[account.getCategoryId() & 0x0f];
        holder.mIconView.setBackgroundResource(background);
        holder.mIconView.setTag(position);
        final View currentView = view;
        holder.mIconView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.clearAnimation();
                v.setAnimation(FLIP1);
                v.startAnimation(FLIP1);
                AnimationListener listener = getAnimListener(currentView, (ImageButton) v,
                        Integer.parseInt(v.getTag().toString()), FLIP1, FLIP2);
                FLIP1.setAnimationListener(listener);
                FLIP2.setAnimationListener(listener);
            }
        });
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
            view.startAnimation(animation);
        }
        view.setActivated(status);
        mLastPosition = position;
        return view;
    }

    @Override
    public int getCount() {  return mEntries.size();  }

    @Override
    public Object getItem(int position) { return mEntries.get(position); }

    @Override
    public long getItemId(int position) {
        return mEntries.get(position).mId;
    }

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
        if(accounts==null) {
            mEntries = new ArrayList<AccountManager.Account>();
        }
        for(AccountManager.Account a : mEntries) {
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

    private AnimationListener getAnimListener(final View view, final ImageButton button,
        final int position, final Animation prev, final Animation next) {
        AnimationListener listener = new AnimationListener() {
            @Override
            public void onAnimationStart(Animation anim) {
                boolean status = mChecked.get(position);
                int srcId = status ? mIcons.get(position) : R.drawable.checkmark;
                int bkg = status ? COLORS[mEntries.get(position).getCategoryId() & 0x0f]
                        : R.drawable.oval_selected;
                if(anim == prev) {
                    button.setImageResource(srcId);
                    button.setBackgroundResource(bkg);
                    button.clearAnimation();
                    button.setAnimation(next);
                    button.startAnimation(next);
                } else {
                    mChecked.set(position, !status);
                    checkCount(position);
                    setActionMode(!status);
                    view.setActivated(!status);
                }
            }

            private void checkCount(int position) {
                if(mChecked.get(position)) { mCheckCount++; }
                else if(mCheckCount > 0) { mCheckCount--; }
            }

            private void setActionMode(boolean status) {
                mListener.onCheck(mCheckCount, position, status);
            }

            @Override
            public void onAnimationRepeat(Animation anim) {}

            @Override
            public void onAnimationEnd(Animation anim) {}
        };
        return listener;
    }


    public void onLongClick(View view, int position) {
        ViewHolder holder = (ViewHolder) view.getTag();
        holder.mIconView.clearAnimation();
        holder.mIconView.setAnimation(FLIP1);
        holder.mIconView.startAnimation(FLIP1);
        AnimationListener listener = getAnimListener(view, holder.mIconView,
                position, FLIP1, FLIP2);
        FLIP1.setAnimationListener(listener);
        FLIP2.setAnimationListener(listener);
    }

    public void setListener(OnListItemCheckListener l) {
        mListener = l;
    }
}
