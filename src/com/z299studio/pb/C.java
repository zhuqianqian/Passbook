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

/* 
 * Constants definitions. The keys or values used through out the shared 
 * preferences and instance bundles are enumerated here. For fast input, 
 * use just 'C' as the class name.
 */
public final class C {
    
    public static final int APP_VERSION = 2;
    
    public static final class Keys {
        public static final String AUTO_LOCK = "AutoLock";
        public static final String ENABLE_COPY = "EnableCopy";
        public static final String SHOW_OTHER = "ShowUngrouped";
        public static final String SHOW_PWD = "ShowPassword";
        public static final String THEME = "Theme";
        public static final String TOUR = "AppTour";
        public static final String WARN_COPY = "WarningCopy";
    }
    
    public static final class Sync {
        public static final String SERVER = "SyncServer";
        public static final String MSG = "SyncMessage";
        public static final String VERSION = "LastSyncVersion";
        public static final int NONE = 0;
        public static final int GPGS = 1;
        public static final int GDRIVE = 2;    
    }
    
    public static final class Names {
        public static final String ACTIVITY = "activity";
        public static final String PAGE_NUM = "page";
    }
    
    public static final class Activity {
        public static final int TOUR = 0;
        public static final int HOME = 1;
        public static final int MAIN = 2;
        public static final int SETTINGS = 3;
    }
    
    public static final int THEMES[] = {
        R.style.AppLight_Default, R.style.AppDark_Default
    };
}
