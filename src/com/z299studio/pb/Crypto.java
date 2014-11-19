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

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class Crypto {
	
    private static final String PBKDF2_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA1";
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static int KEY_LENGTH = 256;
    public static final int SALT_LENGTH = KEY_LENGTH / 8;
    private static SecureRandom random = new SecureRandom();
    
    private byte [] mSalt = null;
    private byte [] mIv = null;
    private SecretKey mKey = null;
    private int mIterationCount;
    
    private static Crypto sInstance;
    
    public static Crypto getInstance() {
    	if(sInstance == null ) {
    		sInstance = new Crypto();
    	}
    	return sInstance;
    }
    public Crypto () {
    	mIterationCount = 800;
    }
    
    public void setIteration(int iteration) {
    	mIterationCount = iteration;
    }
    
    // Reset or set the password first time should call this.
    public void resetPassword(String password) {
    	mSalt = new byte[SALT_LENGTH];
    	random.nextBytes(mSalt);
    	deriveKey(password);
    }
    
    private void deriveKey(String password) {
    	byte[] keyBytes = null;
    	KeySpec keySpec = new PBEKeySpec(password.toCharArray(), mSalt,
                mIterationCount, KEY_LENGTH);
        SecretKeyFactory keyFactory;
		try {
			keyFactory = SecretKeyFactory
			        .getInstance(PBKDF2_DERIVATION_ALGORITHM);
			keyBytes = keyFactory.generateSecret(keySpec).getEncoded();
            mKey = new SecretKeySpec(keyBytes, "AES"); 
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		}        
    }
    
    public byte[] encrypt(byte [] data) {
    	byte cipherData[] = null;
    	if(mKey == null) {
    		return null;
    	}
    	try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
	        mIv = new byte[cipher.getBlockSize()];
	        random.nextBytes(mIv);
            IvParameterSpec ivParams = new IvParameterSpec(mIv);
            cipher.init(Cipher.ENCRYPT_MODE, mKey, ivParams);
            cipherData = cipher.doFinal(data);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        } 
    	return cipherData;
    }
    
    public byte[] decrypt(byte [] data) throws GeneralSecurityException {
    	byte plainData[] = null;
    	if(mKey == null ) {
    		return null;
    	}
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        IvParameterSpec ivParams = new IvParameterSpec(mIv);
        cipher.init(Cipher.DECRYPT_MODE, mKey, ivParams);
        plainData = cipher.doFinal(data);
        
    	return plainData;
    }
    
    public byte[] getSaltAndIvBytes() {
    	byte[] saltAndIv = new byte[mSalt.length + mIv.length];
    	System.arraycopy(mSalt, 0, saltAndIv, 0, mSalt.length);
    	System.arraycopy(mIv, 0, saltAndIv, mSalt.length, mIv.length);
    	return saltAndIv;
    }
    
    public void setPassword(String password, byte[] saltAndIvData, int offset, int total) {
    	mSalt = new byte[SALT_LENGTH];
    	mIv = new byte[total - SALT_LENGTH];
    	System.arraycopy(saltAndIvData, offset, mSalt, 0, SALT_LENGTH);
    	System.arraycopy(saltAndIvData, offset+SALT_LENGTH, mIv, 0, mIv.length);
    	deriveKey(password);
    }
}