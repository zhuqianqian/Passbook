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
import android.widget.Button;
import android.widget.CheckBox;

public class ConfirmCopy extends DialogFragment implements View.OnClickListener{

    public interface OnCopyConfirmListener{
        void onConfirmed(boolean confirmed, boolean remember);
    }

    OnCopyConfirmListener mListener;
    private CheckBox mCheckBox;

    public ConfirmCopy() { }
    
    public ConfirmCopy setListener(OnCopyConfirmListener l) {
        mListener = l;
        return this;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView;
        if(Application.getInstance()== null
                || Application.getInstance().getAccountManager() == null) {
            return null;
        }
        rootView = inflater.inflate(R.layout.dialog_confirm_copy, container);
        Button button = rootView.findViewById(R.id.ok);
        button.setOnClickListener(this);
        button = rootView.findViewById(R.id.cancel);
        button.setOnClickListener(this);
        mCheckBox = rootView.findViewById(R.id.checkbox);
        return rootView;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch(id) {
            case R.id.ok:
            case R.id.cancel:
                if(mListener != null) {
                    mListener.onConfirmed(id == R.id.ok, mCheckBox.isChecked());
                }
                this.dismiss();
                break;
        }
    }
}
