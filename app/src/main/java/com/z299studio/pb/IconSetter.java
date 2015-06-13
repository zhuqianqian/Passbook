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

import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;

public class IconSetter extends DialogFragment implements View.OnClickListener {

    public interface OnIconChosen {
        void onChosen(int id);
    }

    protected OnIconChosen mListener;
    private int mImg;
    
    public IconSetter () { }

    public IconSetter setListener(OnIconChosen l) {
        mListener = l;
        return this;
    }
    
    public IconSetter setInitImage(int img) {
        mImg = img;
        return this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView;
        if(savedInstanceState!=null) {
            if(AccountManager.getInstance()==null) {
                return null;
            }
            mImg = savedInstanceState.getInt("img_code");
        }
        rootView = inflater.inflate(R.layout.dialog_choose_icon, container);
        Button button = (Button)rootView.findViewById(R.id.ok);
        button.setOnClickListener(this);
        button = (Button)rootView.findViewById(R.id.cancel);
        button.setOnClickListener(this);
        final ImageAdapter imageAdapter = new ImageAdapter(getActivity());
        if(mImg > 0) {
            imageAdapter.checkItem(null, mImg);
        }
        GridView gridView = (GridView) rootView.findViewById(R.id.icon);
        gridView.setAdapter(imageAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                mImg = pos;
                imageAdapter.checkItem(view, pos);
            }
        });
        
        return rootView;
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("img_code", mImg);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.ok:
                this.dismiss();
                if(mListener!=null) {
                    mListener.onChosen(mImg);
                }
                break;
            case R.id.cancel:
                this.dismiss();
                break;
        }
    }

    private class ImageAdapter extends BaseAdapter {

        private Context mContext;
        private int mIds[];
        private int mSize;
        private int mCheckedPos;
        private ImageView mCheckedView;

        public ImageAdapter(Context context) {
            mContext = context;
            mIds= Application.getThemedIcons();
            mSize = (int) (getResources().getDimension(R.dimen.main_list_height) + 0.5f);
        }
        
        public void checkItem(View view, int pos) {
            if(mCheckedView!=null) {
                mCheckedView.setBackgroundResource(R.drawable.oval_button);
            }
            if(view !=null) {
                mCheckedView = (ImageView)view;
                mCheckedView.setBackgroundResource(R.drawable.oval);
                mCheckedView.getBackground().setColorFilter(C.ThemedColors[C.colorAccent],
                        PorterDuff.Mode.SRC_ATOP);
            }
            mCheckedPos = pos;
        }

        @Override
        public int getCount() {
            return mIds.length;
        }

        @Override
        public Object getItem(int position) {
            return mIds[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                imageView = new ImageView(mContext);
                imageView.setLayoutParams(new GridView.LayoutParams(mSize, mSize));
                imageView.setScaleType(ImageView.ScaleType.CENTER);
            } else {
                imageView = (ImageView) convertView;
            }
            if(mCheckedPos != position) {
                imageView.setBackgroundResource(R.drawable.oval_button);
            }
            else {
                mCheckedView = imageView;
                imageView.setBackgroundResource(R.drawable.oval);
                imageView.getBackground().setColorFilter(C.ThemedColors[C.colorAccent],
                        PorterDuff.Mode.SRC_ATOP);
            }
            imageView.setImageResource(mIds[position]);
            imageView.setColorFilter(C.ThemedColors[C.colorTextNormal]);
            return imageView;
        }
    }
}
