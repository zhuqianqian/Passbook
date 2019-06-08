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

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class PasswordGenerator extends DialogFragment implements View.OnClickListener {

    private int mType;
    private EditText mTargetView;
    private TextView mPasswordView;
    private TextView mLengthTitle;
    private int mLength;
    private CheckBox[] mCheckBoxes;

    public static PasswordGenerator build(int type, EditText target) {
        PasswordGenerator pg = new PasswordGenerator();
        pg.mType = type;
        pg.mTargetView = target;
        return pg;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(Application.getInstance() == null
                || Application.getInstance().getAccountManager() == null) {
            return null;
        }
        View rootView = inflater.inflate(R.layout.dialog_pwd_generator, container, false);
        Button ok = rootView.findViewById(R.id.ok);
        ok.setOnClickListener(this);
        mPasswordView = rootView.findViewById(R.id.text_pwd);
        mPasswordView.setOnClickListener(this);
        Button button = rootView.findViewById(R.id.cancel);
        button.setOnClickListener(this);
        button = rootView.findViewById(R.id.refresh);
        button.setOnClickListener(this);
        SeekBar sb = rootView.findViewById(R.id.sb_length);
        tintSeekBar(sb);
        sb.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                mLength = progress + 4;
                mLengthTitle.setText(getResources().getString(R.string.length, mLength));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {	}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {	}
        });
        mLengthTitle = rootView.findViewById(R.id.tv_length);
        mCheckBoxes = new CheckBox[5];
        int ids[] = {R.id.cb_uppercase, R.id.cb_lowercase, R.id.cb_digit,R.id.cb_char, R.id.cb_smart};
        for(int i = 0; i < mCheckBoxes.length; ++i) {
            mCheckBoxes[i] = rootView.findViewById(ids[i]);
        }
        if(mType == AccountManager.EntryType.PIN) {
            mCheckBoxes[0].setChecked(false);
            mCheckBoxes[1].setChecked(false);
            mCheckBoxes[3].setChecked(false);
            mCheckBoxes[4].setChecked(false);
            mLength = 6;
            mPasswordView.setText(Application.generate(false,false, false, true, false, false, 6, 6));
            sb.setProgress(2);
            mLengthTitle.setText(getResources().getString(R.string.length, 6));
        }
        else {
            mPasswordView.setText(Application.generate(true, true, true, true, true, true, 10, 10));
            sb.setProgress(6);
            mLength = 10;
            mLengthTitle.setText(getResources().getString(R.string.length, 10));
        }
        return rootView;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.refresh:
            case R.id.text_pwd:
            mPasswordView.setText(Application.generate(mCheckBoxes[4].isChecked(), mCheckBoxes[0].isChecked(),
                    mCheckBoxes[1].isChecked(), mCheckBoxes[2].isChecked(),
                    mCheckBoxes[3].isChecked(), mCheckBoxes[3].isChecked(),
                    mLength, mLength));
                break;

            case R.id.ok:
            if(mTargetView!=null) {
                mTargetView.setText(mPasswordView.getText());
            }
            case R.id.cancel:
                this.dismiss();
                break;
        }
    }

    private void tintSeekBar(SeekBar sb) {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            if( getContext() == null) {
                return;
            }
            LayerDrawable progress = (LayerDrawable)ContextCompat.getDrawable(getContext(),
                    R.drawable.progress);
            if (progress == null) {
                return;
            }
            progress.getDrawable(0).setColorFilter(C.ThemedColors[C.colorIconNormal],
                    PorterDuff.Mode.SRC_ATOP);
            progress.getDrawable(1).setColorFilter(C.ThemedColors[C.colorAccent],
                    PorterDuff.Mode.SRC_ATOP);
            sb.setProgressDrawable(progress);

            Drawable thumb = ContextCompat.getDrawable(getContext(), R.drawable.thumb);
            if (thumb == null) {
                return;
            }
            thumb.setColorFilter(C.ThemedColors[C.colorAccent], PorterDuff.Mode.SRC_ATOP);
            sb.setThumb(thumb);
        }
    }
}
