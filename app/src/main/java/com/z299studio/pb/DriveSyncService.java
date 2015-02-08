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
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class DriveSyncService extends SyncService implements 
    ConnectionCallbacks, OnConnectionFailedListener {
    
    private GoogleApiClient mGoogleApiClient;
    private DriveId mDriveId;

    private static final String SAVED_DATA="pb-drive-data";
    private static final String LOG_TAG = "PwdBook:DriveSyncService";
    
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
    public SyncService connect(int localVersion) {
        mLocalVersion = localVersion;
        mGoogleApiClient.connect();
        return this;
    }

    @Override
    public void disconnect() {
        mGoogleApiClient.disconnect();
    }
    
    @Override
    public void read() {
        Drive.DriveApi.getFile(mGoogleApiClient, mDriveId)
                .open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null)
                .setResultCallback(contentsResultCallback);
    }
    
    @Override
    public void send(byte[] data) {
        if(data !=null) {
            mData = data;
            if(mGoogleApiClient.isConnected()) {
                if (mDriveId == null) {
                    Drive.DriveApi.newDriveContents(mGoogleApiClient)
                            .setResultCallback(driveContentsCallback);
                } else {
                    Drive.DriveApi.getFile(mGoogleApiClient, mDriveId)
                            .open(mGoogleApiClient, DriveFile.MODE_WRITE_ONLY, null)
                            .setResultCallback(contentsResultCallbackToWrite);
                }
            }
        }
    }
    
    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data){
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
        } catch (IntentSender.SendIntentException e) {
            mListener.onSyncFailed(CA.CONNECTION);
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(LOG_TAG, "Connected");
        DriveFolder folder = Drive.DriveApi.getAppFolder(mGoogleApiClient);
        folder.listChildren(mGoogleApiClient).setResultCallback(childrenRetrievedCallback);
    }

    @Override
    public void onConnectionSuspended(int cause) { }
    
    ResultCallback<DriveApi.MetadataBufferResult> childrenRetrievedCallback = new
            ResultCallback<DriveApi.MetadataBufferResult>() {
        @Override
        public void onResult(DriveApi.MetadataBufferResult result) {
            if(!result.getStatus().isSuccess()) {
                Log.w(LOG_TAG, "Retrieving files received error");
            }
            else {
                for(Metadata data : result.getMetadataBuffer()) {
                    String fileName = data.getTitle();
                    if(fileName != null && fileName.equalsIgnoreCase(SAVED_DATA)) {
                        mDriveId = data.getDriveId();
                        break;
                    }
                }
            }
            if(mDriveId!=null) {
                read();
            }
            else {
                mListener.onSyncFailed(CA.DATA_RECEIVED);
            }
        }
    };
    
    ResultCallback<DriveApi.DriveContentsResult> contentsResultCallback = new
            ResultCallback<DriveApi.DriveContentsResult>() {
        @Override
        public void onResult(DriveApi.DriveContentsResult result) {
            if(result.getStatus().isSuccess()) {
                try {
                    FileDescriptor file = result.getDriveContents()
                            .getParcelFileDescriptor().getFileDescriptor();
                    FileInputStream fis = new FileInputStream(file);
                    mData = new byte[fis.available()];
                    fis.read(mData);
                    fis.close();
                    mListener.onSyncProgress(CA.DATA_RECEIVED);
                }catch(IOException e) {
                    mListener.onSyncFailed(CA.DATA_RECEIVED);
                    Log.w(LOG_TAG, "Reading contents received IOException");
                }
            }
            else {
                mListener.onSyncFailed(CA.DATA_RECEIVED);
                Log.w(LOG_TAG, "Retrieving contents received error");
            }
        }
    };

    ResultCallback<DriveApi.DriveContentsResult> contentsResultCallbackToWrite = new
            ResultCallback<DriveApi.DriveContentsResult>() {
        @Override
        public void onResult(DriveApi.DriveContentsResult result) {
            if(result.getStatus().isSuccess()) {
                try {
                    FileDescriptor file = result.getDriveContents()
                            .getParcelFileDescriptor().getFileDescriptor();
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(mData);
                    fos.close();
                    result.getDriveContents().commit(mGoogleApiClient, null);
                    mListener.onSyncProgress(CA.DATA_SENT);
                }catch(IOException e) {
                    mListener.onSyncFailed(CA.DATA_SENT);
                    Log.w(LOG_TAG, "Writing contents received IOException");
                }
            }
            else {
                mListener.onSyncFailed(CA.DATA_SENT);
                Log.w(LOG_TAG, "Writing contents received error");
            }
        }
    };

    private ResultCallback<DriveApi.DriveContentsResult> driveContentsCallback = new
            ResultCallback<DriveApi.DriveContentsResult>() {
        @Override
        public void onResult(DriveApi.DriveContentsResult result) {
            if (!result.getStatus().isSuccess()) {
                Log.w(LOG_TAG, "Error while trying to create new file contents");
                return;
            }
            final DriveContents driveContents = result.getDriveContents();
            new Thread() {
                @Override
                public void run() {
                    FileDescriptor file = driveContents
                            .getParcelFileDescriptor().getFileDescriptor();
                    FileOutputStream fos = new FileOutputStream(file);
                    try {
                        fos.write(mData);
                        fos.close();
                    } catch (IOException e) {
                        Log.e(LOG_TAG, e.getMessage());
                    }
                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                            .setTitle(SAVED_DATA)
                            .setMimeType("application/bin")
                            .build();
                    Drive.DriveApi.getAppFolder(mGoogleApiClient)
                            .createFile(mGoogleApiClient, changeSet, driveContents)
                            .setResultCallback(fileCreateCallback);
                }
            }.start();
        }
    };

    ResultCallback<DriveFolder.DriveFileResult> fileCreateCallback = new
            ResultCallback<DriveFolder.DriveFileResult>() {
        @Override
        public void onResult(DriveFolder.DriveFileResult result) {
            if (!result.getStatus().isSuccess()) {
                Log.w(LOG_TAG, "Error while trying to create the file");
                return;
            }
            mDriveId = result.getDriveFile().getDriveId();
        }
    };
}
