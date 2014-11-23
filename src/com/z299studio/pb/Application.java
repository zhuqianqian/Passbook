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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Locale;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Application{
    
    private static final String DATA_FILE = "data";
    
    private static Application __instance;
    
    public SharedPreferences mSP;
    
    public static class Options {
        public static int mTheme;
        public static boolean mTour;        
    }
    
    private static class FileParser {
        public static int version;  // 1 byte
        public static int iterationCount; // 1 byte
        public static int keyLength;  //1 byte
        public static int ivLength;  // 1 byte
        public static int revision;  // 10 bytes
        public static int size;    
        private static final int V1_HEADER_SIZE = 16;
        private static final int REV_SIZE = 10;
            
        public static boolean parse(byte[] buffer) {
            boolean valid = false;
            if(buffer!=null) {
                if(buffer[0] == 0x50 && buffer[1] == 0x42) {
                    int i = 2;
                    valid = true;
                    version = buffer[i++];
                    iterationCount = 100 * (buffer[i++]);
                    keyLength = buffer[i++];
                    ivLength =  buffer[i++];
                    revision = Integer.parseInt(new String(buffer, i, REV_SIZE).trim());
                    i += REV_SIZE;
                    size = i;                
                }
            }
            return valid;
        }    
        
        public static byte[] build(int version, int count, int keyLength, int ivLength, int revision) {
            byte[] header = new byte[V1_HEADER_SIZE];
            int i = 0;
            header[i++] = 0x50; header[i++] = 0x42;
            header[i++] = (byte) version;
            header[i++] = (byte) (count / 100);
            header[i++] = (byte) keyLength;
            header[i++] = (byte) ivLength;
            byte[] revBytes = String.format(Locale.ENGLISH, "%10d", revision).getBytes();
            System.arraycopy(revBytes, 0, header, i, revBytes.length);
            i += REV_SIZE;
            return header;
        }
    }
    
    private Activity mContext;
    private byte[] mBuffer;
    private long mDataSize;
    
    public static Application getInstance(Activity context) {
        if(__instance == null) {
            __instance = new Application(context);
        }
        __instance.mContext = context;
        return __instance;
    }
    
    public static Application getInstance() {
        return __instance;
    }
    
    private Application(Activity context) {
        mContext = context;
        mSP = PreferenceManager.getDefaultSharedPreferences(context);
        Options.mTheme = mSP.getInt(C.Keys.THEME, 0);
        Options.mTour = mSP.getBoolean(C.Keys.TOUR, false);
    }
    
    public void onStart() {
        
    }
    
    public boolean hasDataFile() {
        boolean success = false;
        try {
            File file = new File(mContext.getFilesDir()+"/"+DATA_FILE);
            mDataSize = file.length();
            if(mDataSize > 0) {
                success = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return success;    
    }
    
    public void getData() {
        try {
            mBuffer = new byte[(int) mDataSize];
            FileInputStream fis = mContext.openFileInput(DATA_FILE);
            fis.read(mBuffer, 0, (int) mDataSize);
            FileParser.parse(mBuffer);
            fis.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void saveData() {
        
    }
    
    public void onDataReceived(byte[] data) {
        
    }
}
