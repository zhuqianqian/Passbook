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

/*
 * Abstract class for synchronization, as other sync service may be used in 
 * upcoming versions (like Dropbox?).
 */
public abstract class SyncService {

    public static final int REQ_RESOLUTION = 299;
    
    public static class CA {
        public static final int DATA_SENT = 1;
        public static final int DATA_RECEIVED = 2;
        public static final int CONNECTION = 3;
        public static final int AUTH = 4;
        public static final int NO_DATA = 5;
    }
    
    public interface SyncListener {
        void onSyncFailed(int errorCode);
        void onSyncProgress(int actionCode);
    }
    
    private static SyncService __instance;
    
    protected SyncListener mListener;
    protected Activity mContext;
    protected int mLocalVersion;
    protected byte[] mData;
    protected Handler mHandler = new Handler();
    
    public static SyncService getInstance(Activity context, int server) {
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
        __instance.mContext = context;
        return __instance;
    }
    
    public static SyncService getInstance() {
        return __instance;
    }
    
    public abstract SyncService initialize();
    
    public abstract SyncService connect(int localVersion);
    
    public abstract void disconnect();
    
    public abstract void read();
    
    public abstract void send(byte[] data);
    
    public byte[] requestData() {
        return mData;
    }
    
    public SyncService setListener(SyncListener l) {
        this.mListener = l;
        return this;
    }
    
    public abstract boolean onActivityResult(int requestCode, int resultCode, Intent data);

}
