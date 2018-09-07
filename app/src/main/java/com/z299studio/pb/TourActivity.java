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

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TourActivity extends FragmentActivity implements AnimatorListener{

    private static final int NUM_PAGES = 3;
    private int mCurrent;
    private ViewPager mPager;
    private LayerDrawable mBackground;
    private TextView mAppText;
    private ImageView[] mIndicators = new ImageView[NUM_PAGES];
    private Button mSkip;
    private Button mNext;
    private boolean mReady;
    private int mFromActivity;
    private LinearLayout mContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState!=null) {
            mCurrent = savedInstanceState.getInt(C.PAGE_NUM, 0);
            mReady = savedInstanceState.getBoolean("ready", false);
            mFromActivity = savedInstanceState.getInt(C.ACTIVITY);
        }
        else {
            mCurrent = 0;
            mReady = false;
            mFromActivity = getIntent().getExtras().getInt(C.ACTIVITY,
                    C.Activity.SETTINGS);
            
        }
        setContentView(R.layout.activity_welcome);
        mPager = findViewById(R.id.pager);
        int ids[] = {R.id.indicator_0, R.id.indicator_1, R.id.indicator_2};
        mBackground= (LayerDrawable) mPager.getBackground();
        mBackground.getDrawable(0).setAlpha(255);
        mBackground.getDrawable(1).setAlpha(0);
        mBackground.getDrawable(2).setAlpha(0);
        mBackground.getDrawable(3).setAlpha(0);
        for(int i = 0; i < NUM_PAGES; ++i) {
            mIndicators[i] = findViewById(ids[i]);
        }
        mSkip = findViewById(R.id.skip);
        mNext = findViewById(R.id.next);
        mContainer = findViewById(R.id.container);
        mAppText = findViewById(R.id.app);
        if(!mReady) {
            mAppText.animate().alpha(1.0f).setDuration(400).setListener(this);
        }
        else {
            mPager.setAlpha(1.0f);
            mContainer.setAlpha(1.0f);
        }
        PagerAdapter pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(pagerAdapter);
        mPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                adjustNavigation(position);
                invalidateOptionsMenu();
            }
        });
        mPager.setPageTransformer(true, new ViewPager.PageTransformer(){
            @Override
            public void transformPage(@NonNull View view, float position) {
                int index = (Integer) view.getTag();
                Drawable currentDrawableInLayerDrawable;
                currentDrawableInLayerDrawable = mBackground.getDrawable(index);
                if(position <= -1 || position >= 1) {
                    currentDrawableInLayerDrawable.setAlpha(0);
                }
                else if( position == 0 ) {
                    currentDrawableInLayerDrawable.setAlpha(255);
                } 
                else { 
                    currentDrawableInLayerDrawable.setAlpha((int)(255 - Math.abs(position*255)));
                }
            }
        });    
        if(mCurrent > 0) {
            mPager.setCurrentItem(mCurrent);
        }
    }    
    
    private void adjustNavigation(int position) {
        for(int i = 0; i < NUM_PAGES; i++) {
            if(i==position) {
                mIndicators[i].setImageResource(R.drawable.dot_fill);
            }
            else {
                mIndicators[i].setImageResource(R.drawable.dot);
            }
        }
        if(position == (NUM_PAGES-1)) {
            mSkip.setVisibility(View.INVISIBLE);
            mNext.setText(R.string.done);
        }
        else if(position == NUM_PAGES) {
            finishTour();
        }
        else {
            mSkip.setVisibility(View.VISIBLE);
            mNext.setText(R.string.next);
        }
        mCurrent = position;
    }
    
    public void onButtonClick(View view) {
        if(mReady) {
            if(view.getId() == R.id.skip) {
                finishTour();
            }
            else {
                if(mCurrent < NUM_PAGES - 1) {
                    mPager.setCurrentItem(mCurrent+1, true);
                }
                else {
                    finishTour();
                }
            }
        }
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(C.PAGE_NUM, mCurrent);
        outState.putInt(C.ACTIVITY, mFromActivity);
        outState.putBoolean("ready", mReady);
    }
    
    private void finishTour() {
        Application.Options.mTour = true;
        Application.getInstance().mSP.edit().putBoolean(C.Keys.TOUR, true).apply();
        if(mFromActivity == C.Activity.HOME){
            Intent intent = new Intent(this, HomeActivity.class);
            this.startActivity(intent);
        }
        this.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    private class ScreenSlidePagerAdapter extends FragmentPagerAdapter {
        ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return ScreenSlidePageFragment.create(position);
        }

        @Override
        public int getCount() {
            return NUM_PAGES+1;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            if(object instanceof ScreenSlidePageFragment){
                ScreenSlidePageFragment ssf = (ScreenSlidePageFragment)object;
                view.setTag(ssf.getPageNumber());
            }
            return super.isViewFromObject(view, object);
        }       
    }

    @Override
    public void onAnimationStart(Animator animation) {}

    @Override
    public void onAnimationEnd(Animator animation) {
        mAppText.animate().alpha(0.0f).setDuration(600);
        mPager.animate().alpha(1.0f).setStartDelay(200).setDuration(500);
        mContainer.animate().alpha(1.0f).setStartDelay(200).setDuration(500);
        mContainer.postDelayed(new Runnable() {
            @Override
            public void run() {
                mReady = true;
            }            
        }, 500);
    }

    @Override
    public void onAnimationCancel(Animator animation) {}

    @Override
    public void onAnimationRepeat(Animator animation) {}
}

