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
import android.content.IntentSender.SendIntentException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.games.snapshot.Snapshots;

import java.io.IOException;

public class GameSyncService extends SyncService implements 
ConnectionCallbacks, OnConnectionFailedListener {

    private static final String SAVED_DATA="Passbook-Saved-Data";
    private static final int MAX_SNAPSHOT_RESOLVE_RETRIES = 3;
    
    private GoogleApiClient mGoogleApiClient;
    
    @Override
    public SyncService initialize() {
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
        .addApi(Games.API)
        .addScope(Games.SCOPE_GAMES)
        .addApi(Drive.API)
        .addScope(Drive.SCOPE_APPFOLDER)
        .addConnectionCallbacks(this)
        .addOnConnectionFailedListener(this)
        .build();
        return this;
    }

    @Override
    public SyncService connect(int localVersion) {
        mLocalVersion = localVersion;
        mGoogleApiClient.connect();
        return this;
    }

    @Override
    public void  disconnect() {
        mGoogleApiClient.disconnect();        
    }
    
    @Override
    public void read() {
        AsyncTask<Void, Void, Integer> task = new AsyncTask<Void, Void, Integer>() {    
            @Override
            protected Integer doInBackground(Void... params) {
                int status = 0;
                try{
                Snapshots.OpenSnapshotResult result = Games.Snapshots.open(
                        mGoogleApiClient, SAVED_DATA, true).await();
                status = result.getStatus().getStatusCode();
                if (status == GamesStatusCodes.STATUS_OK) {
                    Snapshot snapshot = result.getSnapshot();
                    try {
                        mData = snapshot.getSnapshotContents().readFully();
                        if (mData != null && mData.length > Application.FileHeader.HEADER_SIZE) {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mListener.onSyncProgress(CA.DATA_RECEIVED);
                                }
                            });
                        } else {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mListener.onSyncFailed(CA.NO_DATA);
                                }
                            });
                        }
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                }
                else{
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mListener.onSyncFailed(CA.DATA_RECEIVED);
                        }});
                }
                }
                catch(IllegalStateException e) {
                    Log.w("PwdBook:GameSyncService", "IllegalStateException during read()");
                }
                return status;
            }
            
            @Override
            protected void onPostExecute(Integer status){
            }
        };
        task.execute();
    }
    
    @Override
    public void send(final byte[] data) {
        if(data==null) {
            return;
        }
        if(mGoogleApiClient.isConnected()) {
             AsyncTask<Void, Void, Snapshots.OpenSnapshotResult> task = 
                     new AsyncTask<Void, Void, Snapshots.OpenSnapshotResult>() {
                 @Override
                 protected Snapshots.OpenSnapshotResult doInBackground(Void... params) {
                     return Games.Snapshots.open(
                             mGoogleApiClient, SAVED_DATA, true).await();
                 }
                 
                 @Override
                 protected void onPostExecute(Snapshots.OpenSnapshotResult result) {
                     try{
                         Snapshot toWrite = processSnapshotOpenResult(result, 0);
                         if(toWrite!=null) {
                             toWrite.getSnapshotContents().writeBytes(data);
                             SnapshotMetadataChange metadataChange = new SnapshotMetadataChange.Builder()
                                     .setDescription(SAVED_DATA)
                                      .build();
                             Games.Snapshots.commitAndClose(mGoogleApiClient, toWrite, metadataChange);
                             mHandler.post(new Runnable() {
                                 @Override
                                 public void run() {
                                     mListener.onSyncProgress(CA.DATA_SENT);
                                }});                         
                             }
                     } catch(IllegalStateException e) {
                         mHandler.post(new Runnable() {
                             @Override
                             public void run() {
                                 mListener.onSyncFailed(CA.DATA_SENT);
                             }
                         });
                     }
                 }
             };
                 
             task.execute();
        }        
    }
    
    private Snapshot processSnapshotOpenResult(Snapshots.OpenSnapshotResult result, int retryCount) {
        Snapshot mResolvedSnapshot;
        retryCount++;
        int status = result.getStatus().getStatusCode();

        if (status == GamesStatusCodes.STATUS_OK) {
            return result.getSnapshot();
        }
        else if (status == GamesStatusCodes.STATUS_SNAPSHOT_CONTENTS_UNAVAILABLE) {
            return result.getSnapshot();
        }
        else if (status == GamesStatusCodes.STATUS_SNAPSHOT_CONFLICT) {
            Snapshot snapshot = result.getSnapshot();
            Snapshot conflictSnapshot = result.getConflictingSnapshot();
            mResolvedSnapshot = snapshot;
            if (snapshot.getMetadata().getLastModifiedTimestamp() < conflictSnapshot
                    .getMetadata().getLastModifiedTimestamp()) {
                mResolvedSnapshot = conflictSnapshot;
            }
            Snapshots.OpenSnapshotResult resolveResult = Games.Snapshots
                    .resolveConflict(mGoogleApiClient, result.getConflictId(),
                            mResolvedSnapshot).await();
            if (retryCount < MAX_SNAPSHOT_RESOLVE_RETRIES) {
                return processSnapshotOpenResult(resolveResult, retryCount);
            }
        }
        return null;
    }
    
    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQ_RESOLUTION) {
            if (resultCode == Activity.RESULT_OK) {
                mGoogleApiClient.connect();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onSyncProgress(CA.AUTH);
                    }});
            }
            else {
                mHandler.post(new Runnable(){
                    @Override
                    public void run() {
                        mListener.onSyncFailed(CA.AUTH);
                    }});
            }
            return true;
        }
        return false;
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (!result.hasResolution()) {
            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), mContext, 0).show();
            mListener.onSyncFailed(CA.CONNECTION);
            return;
        }
        try {
            result.startResolutionForResult(mContext, REQ_RESOLUTION);
        } catch (SendIntentException e) {
            mListener.onSyncFailed(CA.CONNECTION);
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        read();
    }

    @Override
    public void onConnectionSuspended(int cause) {}
}
