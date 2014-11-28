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
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.drive.Drive;

public class DriveSyncService extends SyncService implements 
ConnectionCallbacks, OnConnectionFailedListener {
    
    public static final int REQ_RESOLUTION = 299;
    
    private GoogleApiClient mGoogleApiClient;
    
    @Override
    public SyncService initialize() {
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
            .addApi(Drive.API)
            .addScope(Drive.SCOPE_APPFOLDER)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build();
        return this;
    }

    @Override
    public SyncService connect() {
        mGoogleApiClient.connect();
        return this;
    }

    @Override
    public void disconnect() {
        mGoogleApiClient.disconnect();
    }
    
    @Override
    public void read(int minVersion) {
        super.read(minVersion);
    }
    
    @Override
    public void send(byte[] data) {
        
    }
    
    @Override
    public boolean onActivityResult(final int requestCode, final int resultCode, 
            final Intent data){
        return false;
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        
    }

    @Override
    public void onConnected(Bundle connectionHint) {
            
    }

    @Override
    public void onConnectionSuspended(int cause) {
        
    }
}
