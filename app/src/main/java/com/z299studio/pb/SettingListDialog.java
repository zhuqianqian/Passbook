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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

public class SettingListDialog extends DialogFragment implements AdapterView.OnItemClickListener{
    
    public interface OnOptionSelected{
        void onSelected(int selection);
    }

    private OnOptionSelected mListener;
    private String[] mOptions;
    private String mTitle;
    private int mSelection;
    private OptionAdapter mAdapter;
    
    public static SettingListDialog build(String title, String[] options, int selection) {
        SettingListDialog dialog = new SettingListDialog();
        dialog.mOptions = options;
        dialog.mTitle = title;
        dialog.mSelection = selection;
        return dialog;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null) {
            mOptions = savedInstanceState.getStringArray("option_list");
            mTitle = savedInstanceState.getString("option_title");
            mSelection = savedInstanceState.getInt("option_selection");
        }
    }
    
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnOptionSelected) context;
        }catch (ClassCastException e) {
            Log.e("PB:SettingListDialog",
                    "Activity must implement OnOptionSelected interface");
        }
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putStringArray("option_list", mOptions);
        outState.putString("option_title", mTitle);
        outState.putInt("option_selection", mSelection);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(Application.getInstance() == null ||
                Application.getInstance().getAccountManager() == null) {
            return null;
        }
        View rootView = inflater.inflate(R.layout.dialog_setting_list, container, false);
        ListView listView = rootView.findViewById(R.id.list);
        listView.setAdapter(mAdapter = new OptionAdapter());
        ((TextView)rootView.findViewById(R.id.title)).setText(mTitle);
        listView.setOnItemClickListener(this);
        listView.setItemsCanFocus(true);
        return rootView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mAdapter.selectItem(view, position);
        if(mListener!=null) {
            mListener.onSelected(mSelection);
        }
        this.dismiss();
    }

    private class OptionAdapter extends BaseAdapter {

        private class ViewHolder {
            RadioButton mButton;
            TextView mText;
            
        }
        private RadioButton mSelectedView;

        @Override
        public int getCount() {
            return mOptions.length;
        }

        @Override
        public Object getItem(int position) {
            return mOptions[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (getActivity() == null) {
                return null;
            }
            LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            View view;
            ViewHolder vh;
            assert inflater != null;
            if(convertView == null) {
                view = inflater.inflate(R.layout.setting_option, parent, false);
                vh = new ViewHolder();
                vh.mButton = view.findViewById(R.id.radio);
                vh.mText = view.findViewById(android.R.id.text1);
                view.setTag(vh);
            }
            else {
                view = convertView;
                vh = (ViewHolder)view.getTag();
            }
            vh.mText.setText(mOptions[position]);
            if(position == mSelection) {
                vh.mButton.setChecked(true);
            }
            else {
                vh.mButton.setChecked(false);
            }
            final View finalView = view;
            vh.mButton.setOnClickListener(v -> onItemClick(null, finalView, position, position));
            return view;
        }
        
        void selectItem(View view, int position) {
            if(mSelectedView!=null) {
                mSelectedView.setChecked(false);
            }
            if(view!=null) {
                mSelectedView = ((ViewHolder)view.getTag()).mButton;
                mSelectedView.setChecked(true);
            }
            mSelection = position;
        }
    }
    
}
