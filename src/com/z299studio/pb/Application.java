package com.z299studio.pb;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

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
			fis.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void saveData() {
		
	}
}
