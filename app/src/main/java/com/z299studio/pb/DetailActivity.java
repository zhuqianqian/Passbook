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

import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class DetailActivity extends ActionBarActivity{

    private static int[] COLORS = {R.color.pb_0, R.color.pb_1, R.color.pb_2, R.color.pb_3,
            R.color.pb_4, R.color.pb_5, R.color.pb_6, R.color.pb_7,
            R.color.pb_8, R.color.pb_9, R.color.pb_a, R.color.pb_b,
            R.color.pb_c, R.color.pb_d, R.color.pb_e, R.color.pb_f};
    private Toolbar mToolbar;
    private int mAccountId;
    private int mAction;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if(savedInstanceState!=null && AccountManager.getInstance() == null) {
            super.onCreate(savedInstanceState);
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        this.setTheme(C.THEMES[Application.Options.mTheme]);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        int color;
        if(savedInstanceState!=null) {
            mAccountId = savedInstanceState.getInt(C.ACCOUNT);
            mAction = savedInstanceState.getInt(C.ACTION);
        } else {
            Bundle extras = getIntent().getExtras();
            mAction = extras.getInt(C.ACTION);
            mAccountId = extras.getInt(C.ACCOUNT);
        }
        if(mAction == C.ACTION_VIEW) {
            color = getResources().getColor(COLORS[AccountManager.getInstance()
                    .getAccountById(mAccountId).getCategoryId()&0x0f]);
            getSupportFragmentManager().beginTransaction().replace(R.id.detail_panel,
                    DetailFragment.create(mAccountId), null).commit();
        }
        else {
            int[] accentColor = {R.attr.colorAccent};
            TypedArray ta = obtainStyledAttributes(accentColor);
            color = ta.getColor(0, 0);
            ta.recycle();
        }
        setupToolbar(color);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            View v = findViewById(R.id.activity_root);
            v.setBackgroundColor(color);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(C.ACTION, mAction);
        outState.putInt(C.ACCOUNT, mAccountId);
        super.onSaveInstanceState(outState);
    }

    private void setupToolbar(int color) {
        mToolbar = (Toolbar)findViewById(R.id.toolbar);
        mToolbar.setBackgroundColor(color);
        float elevation = getResources().getDimension(R.dimen.toolbar_elevation) + 0.5f;
        ViewCompat.setElevation(mToolbar, elevation);
    }
}
