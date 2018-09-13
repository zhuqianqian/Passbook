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
import android.content.Intent;
import android.os.Handler;
import android.support.annotation.NonNull;

import com.google.android.gms.common.ConnectionResult;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * Abstract class for synchronization, as other sync service may be used in 
 * upcoming versions (like Dropbox?).
 */
abstract class SyncService {

    static final int REQ_RESOLUTION = 299;
    
    static class CA {
        static final int DATA_SENT = 1;
        static final int DATA_RECEIVED = 2;
        static final int CONNECTION = 3;
        static final int AUTH = 4;
        static final int NO_DATA = 5;
    }
    
    interface SyncListener {
        void onSyncFailed(int errorCode);
        void onSyncProgress(int actionCode);
    }
    
    private static SyncService __instance;

    protected ExecutorService mExecutorService = Executors.newSingleThreadExecutor();

    protected SyncListener mListener;
    int mLocalVersion;
    byte[] mData;
    
    public static SyncService getInstance(int server) {
        if(__instance!=null) {
            __instance.disconnect();
        }
        switch(server) {
        case C.Sync.GDRIVE:
            __instance = new DriveSyncService();
            break;
        case C.Sync.GPGS:
            __instance = new GameSyncService();
            break;
        }
        return __instance;
    }
    
    public static SyncService getInstance() {
        return __instance;
    }
    
    public abstract SyncService initialize(Activity context);
    
    public abstract SyncService connect(Activity context, int localVersion);
    
    public abstract void disconnect();
    
    public abstract void read();
    
    public abstract void send(byte[] data);
    
    byte[] requestData() {
        return mData;
    }
    
    public SyncService setListener(SyncListener l) {
        this.mListener = l;
        return this;
    }
    
    public abstract void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data);

}
