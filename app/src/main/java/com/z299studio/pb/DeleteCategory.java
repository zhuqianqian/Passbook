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

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;

public class DeleteCategory extends DialogFragment implements View.OnClickListener,
        AdapterView.OnItemSelectedListener{

    public interface OnDeleteConfirmListener{
        void onConfirmed(int category, boolean alsoDelAccounts);
    }

    private int mPosition = -1;
    private int mCategory;
    OnDeleteConfirmListener mListener;
    private CheckBox mCheckBox;

    public DeleteCategory() { }

    public DeleteCategory setCategory(int category) {
        mCategory = category;
        return this;
    }
    
    public DeleteCategory setListener(OnDeleteConfirmListener l) {
        mListener = l;
        return this;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView;
        if(Application.getInstance()==null
                || Application.getInstance().getAccountManager() == null) {
            return null;
        }
        if(savedInstanceState != null) {
            mPosition = savedInstanceState.getInt("category");
        }
        rootView = inflater.inflate(R.layout.dialog_delete_category, container);
        Button button = rootView.findViewById(R.id.ok);
        button.setOnClickListener(this);
        button = rootView.findViewById(R.id.cancel);
        button.setOnClickListener(this);
        Spinner spinner = rootView.findViewById(R.id.spinner);
        String[] allNames = Application.getInstance().getSortedCategoryNames();
        String[] deletableNames = new String[allNames.length-1];
        int i, j = 0;
        for(i = 1; i < allNames.length; ++i) {
            deletableNames[j++] = allNames[i];
        }
        if(mPosition < 0) {
            int[] allIds = Application.getInstance().getSortedCategoryIds();
            for (i = 1; i < allIds.length; ++i) {
                if (mCategory == allIds[i]) {
                    mPosition = i;
                    break;
                }
            }
            mPosition -= 1;
            if(mPosition < 0) { mPosition = 0;}
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item , deletableNames);
        spinner.setAdapter(spinnerAdapter);
        spinner.setSelection(mPosition);
        spinner.setOnItemSelectedListener(this);
        mCheckBox = rootView.findViewById(R.id.checkbox);
        return rootView;
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("category", mPosition);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.ok:
                if(mListener != null) {
                    mListener.onConfirmed(mCategory, mCheckBox.isChecked());
                }
                this.dismiss();

                break;
            case R.id.cancel:
                this.dismiss();
                break;
        }
    }
    
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        mPosition = pos;
        mCategory = Application.getInstance().getSortedCategoryIds()[pos+1];
    }
    
    @Override
    public void onNothingSelected(AdapterView<?> parent){}
}
