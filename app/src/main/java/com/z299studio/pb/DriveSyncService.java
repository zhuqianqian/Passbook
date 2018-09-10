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
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class DriveSyncService extends SyncService {

    private GoogleSignInClient mGoogleSignClient;
    private DriveId mDriveId;
    private DriveResourceClient mDriveResourceClient;

    private static final String SAVED_DATA="pb-drive-data";
    private static final String LOG_TAG = "PB:DriveSyncService";

    private GoogleSignInAccount mSignInAccount;
    
    @Override
    public SyncService initialize(Activity context) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
                .requestScopes(Drive.SCOPE_APPFOLDER).build();
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
            new Thread(()-> {
                mDriveResourceClient = Drive.getDriveResourceClient(context, mSignInAccount);
                read();
            }).start();
        }
        return this;
    }

    @Override
    public void disconnect() {  }
    
    @Override
    public void read() {
        if (mDriveResourceClient == null) {
            mListener.onSyncFailed(CA.CONNECTION);
        }
        final Task<DriveFolder> appFolderTask = mDriveResourceClient.getAppFolder();
        final Query query = new Query.Builder().build();
        appFolderTask.continueWithTask(t -> mDriveResourceClient.queryChildren(t.getResult(), query))
                .continueWithTask(t -> {
                    Task<DriveContents> contentsTask = null;
                    for (Metadata metadata : t.getResult()) {
                        if (SAVED_DATA.equalsIgnoreCase(metadata.getTitle())) {
                            mDriveId = metadata.getDriveId();
                            contentsTask= mDriveResourceClient.openFile(mDriveId.asDriveFile(), DriveFile.MODE_READ_ONLY);
                            break;
                        }
                    }
                    if (contentsTask == null) {
                        return mDriveResourceClient.createContents();
                    }
                    return contentsTask;
                })
                .addOnSuccessListener(c -> {
                    try {
                        InputStream fis = c.getInputStream();
                        if (fis.available() < 1) {
                            throw new IOException();
                        }
                        mData = new byte[fis.available()];
                        int totalBytesRead = fis.read(mData);
                        fis.close();
                        if (totalBytesRead < mData.length) {
                            throw new IOException();
                        }
                        mListener.onSyncProgress(CA.DATA_RECEIVED);

                    } catch (IllegalStateException | IOException e) {
                        Log.e(LOG_TAG, "read: ", e.getCause());
                        mListener.onSyncFailed(CA.NO_DATA);
                    }
                })
                .addOnFailureListener(f -> mListener.onSyncFailed(CA.NO_DATA));
    }
    
    @Override
    public void send(byte[] data) {
        if(data != null) {
            mData = data;
            if (mDriveResourceClient == null) {
                mListener.onSyncFailed(CA.AUTH);
                return;
            }
            Task<DriveContents> contentsTask;
            if (mDriveId == null) {
                contentsTask = mDriveResourceClient.getAppFolder()
                        .continueWithTask(t -> {
                            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                    .setTitle(SAVED_DATA)
                                    .setMimeType("application/bin")
                                    .build();
                            DriveFile file = mDriveResourceClient.createFile(t.getResult(), changeSet, null).getResult();
                            mDriveId = file.getDriveId();
                            return mDriveResourceClient.openFile(file, DriveFile.MODE_WRITE_ONLY);
                        });
            } else {
                contentsTask = mDriveResourceClient.openFile(mDriveId.asDriveFile(), DriveFile.MODE_WRITE_ONLY);
            }
            contentsTask
                    .continueWithTask(t -> {
                        DriveContents contents = t.getResult();
                        OutputStream outputStream = contents.getOutputStream();
                        outputStream.write(mData);
                        return mDriveResourceClient.commitContents(contents, null);
                    })
                    .addOnFailureListener(e -> mListener.onSyncFailed(CA.DATA_SENT))
                    .addOnSuccessListener(s -> mListener.onSyncProgress(CA.DATA_SENT));
        }
    }
    
    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data){
        if (requestCode == CA.AUTH) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                mSignInAccount = task.getResult(ApiException.class);
                mDriveResourceClient = Drive.getDriveResourceClient(activity, mSignInAccount);
                read();
            } catch (ApiException e) {
                Log.e(LOG_TAG, "onActivityResult: ",  e);
                mListener.onSyncFailed(CA.CONNECTION);
            }
        }
    }


}
