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

import java.util.ArrayList;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class NavigationDrawerAdapter extends BaseAdapter {
    
    public static class NavMenuItem {
        public static final int MENU_SEPARATOR = 0;
        public static final int MENU_SELECTION = 1;
        public static final int MENU_ACTION = 2;
        
        public int mType;
        public int mId;
        public int mIcon;
        public String mTitle;
        public int mCount;

        public NavMenuItem(int icon, String title, int count, int id, int type) {
            mIcon = icon;
            mTitle = title;
            mCount = count;
            mId = id;
            mType = type;
        }

    }

    private ArrayList<NavMenuItem> mItemList;
    private Context mContext;
    private NavItemHolder mSelected;
    private int mSelection;
    private int mTintColor;
    private int mIconColor;
    private ColorStateList mTextColor;

    private static class NavItemHolder {
        public ImageView mIconView;
        public TextView mTitleView;
        public TextView mCounterView;
    }

    @Override
    public int getCount() {
        return mItemList.size();
    }

    @Override
    public Object getItem(int position) {
        return mItemList.get(position);
    }

    @Override
    public boolean isEnabled(int position) {
        return (mItemList.get(position).mType != NavMenuItem.MENU_SEPARATOR);
    }

    @Override
    public long getItemId(int position) {
        return mItemList.get(position).mId;
    }

    @Override
    public int getItemViewType(int position) {
        return mItemList.get(position).mType;
    }

    @Override
    public int getViewTypeCount() {
        return (NavMenuItem.MENU_ACTION + 1);
    }

    public NavigationDrawerAdapter(Context context, ArrayList<NavMenuItem> menuList) {
        this.mItemList = menuList;
        this.mContext = context;
        int[] primaryColors = {R.attr.colorPrimary, R.attr.iconColorNormal, R.attr.textColorNormal};
        TypedArray ta = mContext.obtainStyledAttributes(primaryColors);
        mTintColor = ta.getColor(0, 0);
        mIconColor = ta.getColor(1, 0);
        int colorText = ta.getColor(2, 0);
        mTextColor = new ColorStateList(new int[][]{
                new int[]{android.R.attr.state_activated},
                new int[]{}},
                new int[]{mTintColor, colorText});
        ta.recycle();
    }

    public void selectItem(View view, int position) {
        if(mSelected!=null) {
            mSelected.mIconView.setColorFilter(mIconColor);
        }
        if(view!=null) {
            mSelected = (NavItemHolder)view.getTag();
        }
        mSelection = position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        NavItemHolder holder;
        NavMenuItem menuItem = mItemList.get(position);
        int type = getItemViewType(position);
        if (convertView == null) {
            holder = new NavItemHolder();
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if(type == NavMenuItem.MENU_SEPARATOR) {
                v = inflater.inflate(R.layout.nav_separator, parent, false);
            }
            else {
                v = inflater.inflate(R.layout.nav_menu, parent, false);
            }
            TextView titleView = (TextView) v.findViewById(R.id.menu_text);
            ImageView img = (ImageView) v.findViewById(R.id.menu_icon);
            TextView counter = (TextView) v.findViewById(R.id.menu_indicator);

            holder.mTitleView = titleView;
            holder.mIconView = img;
            holder.mCounterView = counter;

            v.setTag(holder);
        }
        else{
            holder = (NavItemHolder) v.getTag();
        }
        holder.mTitleView.setText(menuItem.mTitle);
        holder.mTitleView.setTextColor(mTextColor);
        if(menuItem.mIcon != 0 && holder.mIconView != null) {
            holder.mIconView.setImageResource(menuItem.mIcon);
            holder.mIconView.setColorFilter(position == mSelection ? mTintColor : mIconColor);
        }
        if(menuItem.mCount > 0) {
            if(holder.mCounterView!=null) {
                holder.mCounterView.setText(String.valueOf(menuItem.mCount));
            }
        }
        else if(menuItem.mType == NavMenuItem.MENU_SELECTION && holder.mCounterView != null){
            holder.mCounterView.setText("");
        }
        return v;
    }

    public void setList(ArrayList<NavMenuItem> menuList) {
        this.mItemList = menuList;
    }

    public int getCounterInMenu(int position) {
        return mItemList.get(position).mCount;
    }

    public NavigationDrawerAdapter updateCounterInMenu(int position, int delta) {
        NavMenuItem nmi = mItemList.get(position);
        nmi.mCount += delta;
        mItemList.set(position, nmi);
        return this;
    }

    public NavigationDrawerAdapter updateCategoryCounter(int position, int value) {
        NavMenuItem nmi = mItemList.get(position);
        nmi.mCount = value;
        return this;
    }

    public void remove(int position) {
        mItemList.remove(position);
        this.notifyDataSetChanged();
    }
}
