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
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Transformation;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

class MainListAdapter extends BaseAdapter {

    interface OnListItemCheckListener{
        void onCheck(int count, int position, boolean isChecked);
    }

    private static class ViewHolder {
        ImageButton mIconView;
        TextView mTextView;
        boolean mInflate;
    }

    private static final long TIME_INTERVAL = 50;
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

    MainListAdapter(Context context, ArrayList<AccountManager.Account> accounts,
                           int[] drawableResIds, int defaultDrawableResId) {
        mContext = context;
        mAnimationEnabled = true;
        mDefaultDrawableId = defaultDrawableResId;
        setList(accounts, drawableResIds);
        prepareResources();
    }

    private void prepareResources() {
        FLIP1 = AnimationUtils.loadAnimation(mContext, R.anim.shrink_to_middle);
        FLIP2 = AnimationUtils.loadAnimation(mContext, R.anim.expand_from_middle);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder holder;
        AccountManager.Account account = mEntries.get(position);
        boolean checked = mChecked.get(position);
        if (convertView == null) {
            view = inflate(parent);
        }
        else {
            holder = (ViewHolder) view.getTag();
            if(holder.mInflate) {
                view = inflate(parent);
            }
        }
        if (mDeleted.get(position)) {
            final View deletedView = view;
            view.post(() -> animateDeletion(deletedView, position));
        }
        holder = (ViewHolder) view.getTag();
        holder.mTextView.setText(account.getAccountName());
        holder.mIconView.setPressed(checked);
        holder.mIconView.setTag(position);
        int srcId = checked ? R.drawable.checkmark : mIcons.get(position);
        String iconUrl = checked ? null : account.getIconUrl();
        Picasso.get().load(iconUrl).placeholder(srcId)
                .transform(new CircleTransform()).fit().into(holder.mIconView);
        final View currentView = view;
        holder.mIconView.setOnClickListener(v -> {
            v.clearAnimation();
            v.setAnimation(FLIP1);
            v.startAnimation(FLIP1);
            AnimationListener listener = getAnimListener(currentView, (ImageButton) v,
                    Integer.parseInt(v.getTag().toString()), FLIP1, FLIP2);
            FLIP1.setAnimationListener(listener);
            FLIP2.setAnimationListener(listener);
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
        view.setActivated(checked);
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

    void addList(ArrayList<AccountManager.Account> itemList, boolean reset) {
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

    void setList(ArrayList<AccountManager.Account> accounts, int[] drawableResIds) {
        mEntries = accounts;
        mIcons = new ArrayList<>();
        mChecked = new ArrayList<>();
        mDeleted = new ArrayList<>();
        int id;
        AccountManager am = Application.getInstance().getAccountManager();
        if(accounts==null) {
            mEntries = new ArrayList<>();
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
        assert inflater != null;
        v = inflater.inflate(R.layout.account_list_item, parent, false);
        holder.mIconView = v.findViewById(R.id.item_icon);
        holder.mTextView = v.findViewById(R.id.item_name);
        holder.mInflate = false;
        v.setTag(holder);
        return v;
    }

    private AnimationListener getAnimListener(final View view, final ImageButton button,
        final int position, final Animation prev, final Animation next) {
        return new AnimationListener() {
            @Override
            public void onAnimationStart(Animation anim) {
                boolean checking = mChecked.get(position);
                AccountManager.Account account = mEntries.get(position);
                int srcId = checking ? mIcons.get(position) : R.drawable.checkmark;
                button.setPressed(!checking);
                if(anim == prev) {
                    String iconUrl = checking ? account.getIconUrl() : null;
                    Picasso.get().load(iconUrl).placeholder(srcId)
                            .fit().transform(new CircleTransform()).into(button);
                    button.clearAnimation();
                    button.setAnimation(next);
                    button.startAnimation(next);
                } else {
                    mChecked.set(position, !checking);
                    checkCount(position);
                    setActionMode(!checking);
                    view.setActivated(!checking);
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
    }


    void onLongClick(View view, int position) {
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
    
    void disableAnimation() {
        mAnimationEnabled = false;
    }

    int getItemPosition(int accountId, int startFrom) {
        int pos;
        for(pos = startFrom; pos < mEntries.size(); ++pos) {
            if(mEntries.get(pos).mId == accountId) {
                break;
            }
        }
        return pos;
    }
    
    int getSelected(int[] mIndices) {
        int j;
        for(int i = j = 0; i < mChecked.size(); ++i) {
            if(mChecked.get(i)) {
                mIndices[j++] = i;
            }
        }
        return j;
    }

    int cancelSelection(ListView host, int begin, int end) {
        int i, pos;
        int totalVisibleMarked = 0;
        for(pos = 0; pos < begin; ++pos) {
            if(mChecked.get(pos)) {
                mChecked.set(pos,  Boolean.FALSE);
                mCheckCount--;
            }
        }
        for(pos = end+1; pos < mChecked.size(); ++pos) {
            if(mChecked.get(pos)) {
                mChecked.set(pos,  Boolean.FALSE);
                mCheckCount--;
            }
        }
        for(i = 0, pos = begin; pos <= end; i++, pos++) {
            if(mChecked.get(pos)) {
                View view = host.getChildAt(i);
                ViewHolder holder = (ViewHolder) view.getTag();
                Animation anim1 = AnimationUtils.loadAnimation(mContext, R.anim.shrink_to_middle);
                Animation anim2 = AnimationUtils.loadAnimation(mContext, R.anim.expand_from_middle);
                holder.mIconView.clearAnimation();
                holder.mIconView.setAnimation(anim1);
                holder.mIconView.startAnimation(anim1);

                AnimationListener listener = getAnimListener(view, 
                        holder.mIconView, pos, anim1, anim2);
                anim1.setAnimationListener(listener);
                anim2.setAnimationListener(listener);
                totalVisibleMarked++;
            }
        }
        return totalVisibleMarked;
    }
    
    void animateDeletion(final View view, final int position) {
        AnimationListener al = new AnimationListener() {
            @Override
            public void onAnimationEnd(Animation anim) {
                ViewHolder vh = (ViewHolder)view.getTag();
                vh.mInflate = true;
                mLastPosition = position;
            }
            @Override public void onAnimationRepeat(Animation animation) {}
            @Override public void onAnimationStart(Animation animation) {}
        };

        final int initialHeight = view.getMeasuredHeight();

        Animation anim = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    view.setVisibility(View.GONE);
                }
                else {
                    view.getLayoutParams().height = initialHeight - (int)(initialHeight * interpolatedTime);
                    view.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        anim.setAnimationListener(al);
        anim.setDuration(400);
        view.startAnimation(anim);
        mDeleted.set(position, true);
    }

    void undoDelete(final View v, int rowHeight) {
        AnimationListener al = new AnimationListener() {
            @Override
            public void onAnimationEnd(Animation anim) {
                ViewHolder vh = (ViewHolder)v.getTag();
                vh.mInflate = true;
            }
            @Override public void onAnimationRepeat(Animation animation) {}
            @Override public void onAnimationStart(Animation animation) {}
        };

        final int targetHeight = rowHeight;//v.getMeasuredHeight();

        Animation anim = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 0) {
                    v.getLayoutParams().height = (int)(targetHeight * interpolatedTime);
                    v.requestLayout();
                    v.setVisibility(View.VISIBLE);
                }
                else {
                    v.getLayoutParams().height = (int)(targetHeight * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };
        anim.setAnimationListener(al);
        anim.setDuration(400);
        v.startAnimation(anim);
    }
    
    void markDeletion(int[] indices, int total, boolean delete) {
        for(int i = 0; i < total; i++) {
            mDeleted.set(indices[i], delete);
        }
    }

    void doDelete(int [] indices, int total) {
        int end = total - 1;
        AccountManager am = Application.getInstance().getAccountManager();
        int pos;
        for(int i = end; i >= 0; --i) {
            pos = indices[i];
            am.removeAccount(mEntries.get(pos));
            mEntries.remove(pos);
            mIcons.remove(pos);
            mChecked.remove(pos);
            mDeleted.remove(pos);
        }
        this.disableAnimation();
        this.notifyDataSetChanged();
    }
    
    void markAll(ListView listView) {
        int firstVisible = listView.getFirstVisiblePosition();
        int lastVisible = listView.getLastVisiblePosition();
        int pos;
        for(pos = 0; pos < firstVisible; ++pos) {
            mChecked.set(pos, true);
        }
        for(pos = lastVisible + 1; pos < mChecked.size(); ++pos) {
            mChecked.set(pos, true);
        }
        mCheckCount += (mChecked.size() - lastVisible - 1);
        for(pos = firstVisible; pos <= lastVisible; ++pos) {
            View v = listView.getChildAt(pos);
            if(v!=null) {
                ViewHolder holder = (ViewHolder) v.getTag();
                Animation anim1 = AnimationUtils.loadAnimation(mContext, R.anim.shrink_to_middle);
                Animation anim2 = AnimationUtils.loadAnimation(mContext, R.anim.expand_from_middle);
                holder.mIconView.clearAnimation();
                holder.mIconView.setAnimation(anim1);
                holder.mIconView.startAnimation(anim1);

                AnimationListener listener = getAnimListener(v, holder.mIconView, pos, anim1, anim2);
                anim1.setAnimationListener(listener);
                anim2.setAnimationListener(listener);
            }
        }
    }
    
    void moveData(int categoryId) {
        AccountManager.Account account;
        boolean marked;
        AccountManager am = Application.getInstance().getAccountManager();
        for(int i = 0; i < mChecked.size(); i++) {
            account = mEntries.get(i);
            marked = mChecked.get(i);
            if(marked && account.getCategoryId() != categoryId) {
                am.moveAccount(categoryId, account);
            }
            else if(!marked && account.getCategoryId()==categoryId) {
                am.moveAccount(AccountManager.DEFAULT_CATEGORY_ID, account);
            }
        }
    }

    public class CircleTransform implements com.squareup.picasso.Transformation {
        @Override
        public Bitmap transform(Bitmap source) {
            int size = Math.min(source.getWidth(), source.getHeight());

            int x = (source.getWidth() - size) / 2;
            int y = (source.getHeight() - size) / 2;

            Bitmap squaredBitmap = Bitmap.createBitmap(source, x, y, size, size);
            if (squaredBitmap != source) {
                source.recycle();
            }

            Bitmap bitmap = Bitmap.createBitmap(size, size, source.getConfig());

            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            BitmapShader shader = new BitmapShader(squaredBitmap,
                    BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP);
            paint.setShader(shader);
            paint.setAntiAlias(true);

            float r = size / 2f;
            canvas.drawCircle(r, r, r, paint);

            squaredBitmap.recycle();
            return bitmap;
        }

        @Override
        public String key() {
            return "circle";
        }
    }
}
