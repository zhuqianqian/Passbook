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

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.Hashtable;

public class MainListFragment extends Fragment implements AdapterView.OnItemClickListener{

    public interface ItemSelectionInterface {
        public void onSelectAccount(long id);
    }

    private ItemSelectionInterface mListener;
    private ListView mListView;
    private MainListAdapter mAdapter;
    private int mCategoryId;

    private static class AdapterHolder {
        public MainListAdapter mAdapter;
        public boolean mUpToDate;
    }
    private static Hashtable<Integer, AdapterHolder> cachedAdapters =
            new Hashtable<Integer, AdapterHolder>();

    private static MainListAdapter getAdapter( int category_id) {
        AdapterHolder ah = cachedAdapters.get(category_id);
        if(ah!=null && ah.mUpToDate) {
            return ah.mAdapter;
        }
        return null;
    }

    private static void cacheAdapter(int category_id, MainListAdapter adapter) {
        AdapterHolder ah = cachedAdapters.get(category_id);
        if(ah==null) {
            ah = new AdapterHolder();
        }
        ah.mUpToDate = true;
        ah.mAdapter = adapter;
        cachedAdapters.put(category_id, ah);
    }

    public MainListFragment() {   }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null) {
            mCategoryId = savedInstanceState.getInt("category_id");
        }
        else {
            mCategoryId = AccountManager.ALL_CATEGORY_ID;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("category_id", mCategoryId);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        mListView = (ListView)rootView.findViewById(android.R.id.list);
        if((mAdapter = getAdapter(mCategoryId)) == null) {
            mAdapter = new MainListAdapter(getActivity(),
                    AccountManager.getInstance().getAccountsByCategory(mCategoryId),
                    Application.getThemedIcons(), R.drawable.pb_unknown);
            cacheAdapter(mCategoryId, mAdapter);
        }
        mListView.setAdapter(mAdapter);
        mListView.setEmptyView(rootView.findViewById(android.R.id.empty));
        mListView.setOnItemClickListener(this);
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (ItemSelectionInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement ItemSelectionInterface.");
        }
    }

    public void updateData() {

    }

    public void selectCategory(int category_id) {
        if(mCategoryId != category_id) {
            mCategoryId = category_id;
            if((mAdapter = getAdapter(mCategoryId)) == null) {
                mAdapter = new MainListAdapter(getActivity(),
                        AccountManager.getInstance().getAccountsByCategory(mCategoryId),
                        Application.getThemedIcons(), R.drawable.pb_unknown);
                cacheAdapter(mCategoryId, mAdapter);
            }
            mListView.setAdapter(mAdapter);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
        if(mListener != null) {
            mListener.onSelectAccount(id);
        }
    }
}
