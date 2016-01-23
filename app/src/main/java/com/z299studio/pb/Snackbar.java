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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class Snackbar extends DialogFragment implements View.OnClickListener{

    public interface OnActionListener {
        void onAction();
    }
    
    private int mTimeOut = 5000;
    private String mText;
    private String mAction;
    private DialogInterface.OnDismissListener mDismissListener;
    private OnActionListener mActionListener;
    private final Handler mHandler = new Handler();
    private final Runnable mTask = new Runnable() {
        @Override
        public void run() {
            Dialog dlg = getDialog();
            if(dlg!=null) {
                dlg.dismiss();
            }
        }
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, R.style.Snackbar);
        if(savedInstanceState!=null) {
            mTimeOut = savedInstanceState.getInt("timeout");
            mText = savedInstanceState.getString("text");
            mAction = savedInstanceState.getString("action");
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.snackerbar, container, false);
        TextView v = (TextView) rootView.findViewById(android.R.id.text1);
        v.setText(mText);
        Button action = (Button)rootView.findViewById(R.id.action);
        action.setOnClickListener(this);
        action.setText(mAction);
        if(mTimeOut > 0) {
            mHandler.postDelayed(mTask, mTimeOut);
        }
        return rootView;
    }
    
    @Override
    public void onStart() {
        super.onStart();
        if(getDialog() != null) {
            Window window = getDialog().getWindow();
            window.setWindowAnimations(R.style.SnackbarAnimation);
            Resources res = getResources();
            int height = (int) (res.getDimension(R.dimen.snackbar_height_single) + 0.5f);
            boolean leftAlign = res.getBoolean(R.bool.snackbar_left_align);
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.gravity = Gravity.BOTTOM | Gravity.START ;
            lp.x = 0;
            lp.y = 0;
            lp.height = height ;
            if(!leftAlign) {
                Point windowSize = new Point();
                ((WindowManager)getActivity().getSystemService(Context.WINDOW_SERVICE))
                        .getDefaultDisplay().getSize(windowSize);
                lp.width = windowSize.x;
            }
            else {
                lp.height += (int) (res.getDimension(R.dimen.snackbar_horizontal_margin) + 0.5f);
            }
            window.setAttributes(lp);
        }   
    }

    @Override
    public void onClick(View v) {
        if(mActionListener!=null) {
            mActionListener.onAction();
        }
        dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        mHandler.removeCallbacks(mTask);
        if(mDismissListener!=null) {
            mDismissListener.onDismiss(dialog);
        }
        
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("text", mText);
        outState.putString("action", mAction);
        outState.putInt("timeout", mTimeOut);
        super.onSaveInstanceState(outState);
    }

    public Snackbar setDismissListener(DialogInterface.OnDismissListener listener) {
        mDismissListener = listener;
        return this;
    } 
    
    public Snackbar setActionListener(OnActionListener listener) {
        mActionListener = listener;
        return this;
    }
    
    public Snackbar setText(String text) {
        mText = text;
        return this;
    }
    
    public Snackbar setActionText(String action) {
        mAction = action;
        return this;
    }
    
    public void show(AppCompatActivity activity) {
        this.show(activity.getSupportFragmentManager(), "snackbar");
    }
}
