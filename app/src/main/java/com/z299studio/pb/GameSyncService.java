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

import android.app.Activity;
import android.content.Intent;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.SnapshotsClient;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.tasks.Task;

class GameSyncService extends SyncService {

    private GoogleSignInClient mGoogleSignClient;
    private GoogleSignInAccount mSignInAccount;

    private SnapshotsClient mSnapshotClient;

    private static final String SAVED_DATA="Passbook-Saved-Data";

    @Override
    public SyncService initialize(Activity context) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(Drive.SCOPE_APPFOLDER)
                //.requestScopes(Games.SCOPE_GAMES)
                .build();
        mGoogleSignClient = GoogleSignIn.getClient(context, gso);
        mSignInAccount = GoogleSignIn.getLastSignedInAccount(context);
        return this;
    }

    @Override
    public SyncService connect(Activity context, int localVersion) {
        mLocalVersion = localVersion;
        if (mSignInAccount == null) {
            Intent intent = mGoogleSignClient.getSignInIntent();
            context.startActivityForResult(intent, CA.AUTH);
        } else {
            mGoogleSignClient.silentSignIn().addOnCompleteListener(context, task -> {
                if (task.isSuccessful()) {
                    mSignInAccount = task.getResult();
                    mSnapshotClient = Games.getSnapshotsClient(context, mSignInAccount);
                    mExecutorService.submit(this::read);
                } else {
                    context.startActivityForResult(mGoogleSignClient.getSignInIntent(), CA.AUTH);
                }
            });
        }
        return this;
    }

    @Override
    public void  disconnect() {
    }
    
    @Override
    public void read() {
        if (mSnapshotClient == null) {
            mListener.onSyncFailed(CA.CONNECTION);
        }
        mSnapshotClient.open(SAVED_DATA, true)
                .continueWith((task) -> {
                    Snapshot snapshot = task.getResult().getData();
                    if (snapshot != null) {
                        return snapshot.getSnapshotContents().readFully();
                    }
                    return null;
                })
                .addOnFailureListener(t -> mListener.onSyncFailed(CA.NO_DATA))
                .addOnCompleteListener(t -> {
                    mData = t.getResult();
                    if (mData.length < 1) {
                        mListener.onSyncFailed(CA.NO_DATA);
                    } else {
                        mListener.onSyncProgress(CA.DATA_RECEIVED);
                    }
                });
    }
    
    @Override
    public void send(final byte[] data) {
        if(data == null || mSignInAccount == null) {
            return;
        }
        mData = data;
        mSnapshotClient.open(SAVED_DATA, true)
                .continueWithTask(task -> {
                    Snapshot snapshot = task.getResult().getData();
                    if (snapshot != null) {
                        snapshot.getSnapshotContents().writeBytes(mData);
                        SnapshotMetadataChange metadataChange = new SnapshotMetadataChange.Builder().build();
                        return mSnapshotClient.commitAndClose(snapshot, metadataChange);
                    }
                    return null;
                })
                .addOnFailureListener(t -> mListener.onSyncFailed(CA.DATA_SENT))
                .addOnCompleteListener(t -> mListener.onSyncProgress(CA.DATA_SENT));
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if(requestCode == CA.AUTH) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                mSignInAccount = task.getResult(ApiException.class);
                mSnapshotClient = Games.getSnapshotsClient(activity, mSignInAccount);
                mListener.onSyncProgress(CA.AUTH);
                read();
            } catch (ApiException e) {
                mListener.onSyncFailed(CA.CONNECTION);
            }
        }
    }

}
