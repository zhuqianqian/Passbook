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

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public class DetailActivity extends ActionBarActivity{

    private int mAccountId;
    private int mAction;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        if(savedInstanceState!=null) {
            mAccountId = savedInstanceState.getInt(C.ACCOUNT);
            mAction = savedInstanceState.getInt(C.ACTION);
        } else {
            Bundle extras = getIntent().getExtras();
            mAction = extras.getInt(C.ACTION);
            mAccountId = extras.getInt(C.ACCOUNT);
            if(mAction == C.ACTION_VIEW) {
                getSupportFragmentManager().beginTransaction().replace(R.id.detail_panel,
                        DetailFragment.create(mAccountId), null).commit();
            }
        }
    }
}
