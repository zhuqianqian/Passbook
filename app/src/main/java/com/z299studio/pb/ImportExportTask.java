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

import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.security.GeneralSecurityException;
import java.util.Hashtable;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

class ImportExportTask extends AsyncTask<String, Void, String> {
    
    interface TaskListener {
        void onFinish(boolean authenticate, int operation, String result);
        byte[] saveData();
    }

    static final int OPTION_OVERWRITE = 2;
    static final int OPTION_KEEPALL = 1;
    static final int OPTION_IGNORE = 0;

    private static final int FILE_PB_DATA = 0;
    private static final int FILE_PB_CSV = 1;
    private static final int FILE_AW_CSV = 2;
    
    private boolean mAuthRequired = false;
    private static int mOperation;
    private static int mFileType;
    private static String mFilePath;
    private static int mOption;
    private static String mPassword;
    private TaskListener mListener;

    ImportExportTask (TaskListener l, String filePath, String password,
                             int fileType, int operation, int option) {
        super();
        mListener = l;
        mOption = option;
        mOperation = operation;
        mFileType = fileType;
        mFilePath = filePath;
        mPassword = password;
    }
    
    ImportExportTask(TaskListener l, String password) {
        super();
        mListener = l;
        mPassword = password;
    }

    private String importPbData() {
        String result = null;
        try {
            byte[] buffer;
            File file = new File(mFilePath);
            int size = (int) file.length();
            if(size > 0) {
                buffer = new byte[size];
                FileInputStream fis = new FileInputStream(file);
                fis.read(buffer, 0, size);
                fis.close();
                Application.FileHeader header = Application.FileHeader.parse(buffer);
                if(header.valid) {
                    AccountManager am = Application.decrypt(new Crypto(), mPassword, header, buffer);
                    process(am);
                    result = mFilePath;
                }
            }
        } catch (GeneralSecurityException e) {
            mAuthRequired = true;
            result = null;
        } catch (Exception e) {
            result = null;
        }
        return result;
    }

    private void process(AccountManager am) {
        AccountManager appAm = Application.getInstance().getAccountManager();
        Hashtable<String, Integer> existingCategory = new Hashtable<>();
        Hashtable<String, Integer> existingAccounts = new Hashtable<>();
        List<AccountManager.Category> newCategories = am.getCategoryList(true, false);
        List<AccountManager.Account> newAccounts = am.getAllAccounts(false);
        List<AccountManager.Category> categories = appAm.getCategoryList(true, false);

        for(AccountManager.Category c : categories) {
            existingCategory.put(c.mName, c.mId);
        }
        for(AccountManager.Category c : newCategories) {
            if(existingCategory.get(c.mName) == null) {
                existingCategory.put(c.mName, appAm.addCategory(c.mImgCode, c.mName));
            }
        }
        AccountManager.Category category;
        int categoryId;
        if(mOption != OPTION_KEEPALL) {
            Integer accId;
            List<AccountManager.Account> accounts = appAm.getAllAccounts(false);
            for(AccountManager.Account a : accounts) {
                existingAccounts.put(a.mProfile, a.mId);
            }
            for(AccountManager.Account a: newAccounts) {
                accId = existingAccounts.get(a.mProfile);
                if(accId == null) {
                    category = am.getCategory(a.getCategoryId());
                    if(category!=null) {
                        categoryId = existingCategory.get(category.mName);
                    }
                    else {
                        categoryId = AccountManager.DEFAULT_CATEGORY_ID;
                    }
                    appAm.addAccount(categoryId, a);
                }
                else if(mOption == OPTION_OVERWRITE) {
                    AccountManager.Account account = appAm.getAccountById(accId);
                    account.setAccount(a);
                }
            }
        }
        else {
            for(AccountManager.Account a : newAccounts) {
                category = am.getCategory(a.getCategoryId());
                if(category==null) {
                    categoryId = AccountManager.DEFAULT_CATEGORY_ID;
                }
                else {
                    categoryId = existingCategory.get(category.mName);
                }
                appAm.addAccount(categoryId, a);
            }

        }
    }

    private String importPbCSV() {
        String result;
        try {
            CSVReader csvReader = new CSVReader(new FileReader(mFilePath));
            List<String[]> content = csvReader.readAll();
            csvReader.close();
            AccountManager appAm = Application.getInstance().getAccountManager();
            Hashtable<String, Integer> existingCategory = new Hashtable<>();
            Hashtable<String, Integer> existingAccounts = new Hashtable<>();
            List<AccountManager.Category> categories = appAm.getCategoryList(true, false);
            for(AccountManager.Category c : categories) {
                existingCategory.put(c.mName, c.mId);
            }
            for(String s : content.get(0)) {
                if(existingCategory.get(s) == null) {
                    existingCategory.put(s, appAm.addCategory(0, s));
                }
            }
            String[] line;
            String names[];
            String fields[];
            AccountManager.Account account;
            Integer accId;
            int categoryId;
            if(mOption != OPTION_KEEPALL) {
                List<AccountManager.Account> accounts = appAm.getAllAccounts(false);
                for(AccountManager.Account a : accounts) {
                    existingAccounts.put(a.mProfile, a.mId);
                }
                for(int i = 1; i < content.size(); ++i) {
                    line = content.get(i);
                    names = line[0].split("\t", 2);
                    accId = existingAccounts.get(names[1]);
                    if(accId != null && mOption == OPTION_IGNORE) {
                        continue;
                    }
                    categoryId = existingCategory.get(names[0]);
                    if(accId == null) {
                        account = appAm.newAccount(categoryId);
                    }
                    else {
                        account = appAm.getAccountById(accId);
                        account.setCategory(categoryId);
                    }
                    account.setName(names[1]);
                    account.clearEntries();
                    for(int j = 1; j < line.length; ++j) {
                        fields = line[j].split("\t", 3);
                        account.addEntry(Integer.parseInt(fields[0]), fields[1], fields[2]);
                    }
                    if(accId == null) {
                        appAm.addAccount(categoryId, account);
                    }
                    else {
                        appAm.setAccount(account);
                    }
                }
            }
            else {
                for(int i = 1; i < content.size(); ++i) {
                    line = content.get(i);
                    names = line[0].split("\t", 2);
                    categoryId = existingCategory.get(names[0]);
                    account = appAm.newAccount(categoryId);
                    account.setName(names[1]);
                    for(int j = 1; j < line.length; ++j) {
                        fields = line[j].split("\t", 3);
                        account.addEntry(Integer.parseInt(fields[0]), fields[1], fields[2]);
                    }
                    appAm.addAccount(categoryId, account);
                }
            }
            result = mFilePath;

        } catch (Exception e) {
            result = null;
        }
        return result;
    }

    private String importAwCSV() {
        if(mFilePath == null) {
            return null;
        }
        String result;
        AccountManager appAm = Application.getInstance().getAccountManager();
        int start = mFilePath.lastIndexOf(File.separator)+1;
        Hashtable<String, Integer> existingAccounts = new Hashtable<>();
        int categoryId = -1;
        try{
            CSVReader csvReader = new CSVReader(new FileReader(mFilePath));
            List<String[]> content = csvReader.readAll();
            csvReader.close();
            String categoryName = mFilePath.substring(start, mFilePath.indexOf('.', start));
            List<AccountManager.Category> categories = appAm.getCategoryList(true, false);
            for(AccountManager.Category c : categories) {
                if(categoryName.equals(c.mName)) {
                    categoryId = c.mId;
                    break;
                }
            }
            if(categoryId == -1) {
                categoryId = appAm.addCategory(0, categoryName);
            }
            AccountManager.Account account;
            String[] line;
            String[] fields = content.get(0);
            if(mOption != OPTION_KEEPALL) {
                List<AccountManager.Account> accounts = appAm.getAllAccounts(false);
                for(AccountManager.Account a : accounts) {
                    existingAccounts.put(a.mProfile, a.mId);
                }
                Integer accountId;
                for(int i = 1; i < content.size(); ++i) {
                    line = content.get(i);
                    accountId = existingAccounts.get(line[0]);
                    if(accountId == null) {
                        account = appAm.newAccount(categoryId);
                        account.setName(line[0]);
                    }
                    else if(mOption == OPTION_IGNORE) {
                        continue;
                    }
                    else {
                        account = appAm.getAccountById(accountId);
                        account.clearEntries();
                    }
                    for(int j = 1; j < line.length; ++j) {
                        if(line[j].length() > 0) {
                            account.addEntry(0, fields[j], line[j]);
                        }
                    }
                    if(accountId != null) {
                        appAm.setAccount(account);
                    }
                    else {
                        appAm.addAccount(categoryId, account);
                    }
                }

            }
            else {
                for(int i = 1; i < content.size(); ++i) {
                    line = content.get(i);
                    account = appAm.newAccount(categoryId);
                    account.setName(line[0]);
                    for(int j = 1; j < line.length; ++j) {
                        if(line[j].length() > 0) {
                            account.addEntry(0, fields[j], line[j]);
                        }
                    }
                    appAm.addAccount(categoryId, account);
                }
            }
            result = mFilePath;
        }catch (Exception e) {
            result = null;
        }
        return result;
    }

    private String exportData() {
        String result;
        byte[] data = mListener.saveData();
        try {
            File file = new File(Environment.getExternalStorageDirectory(), "pbdata");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.close();
            result = file.getPath();
        } catch (Exception ex) {
            result = null;
            ex.printStackTrace();
        }
        return result;
    }

    private String exportCSV() {
        String result;
        try{
            AccountManager am = Application.getInstance().getAccountManager();
            File file = new File(Environment.getExternalStorageDirectory(), "pb.csv");
            FileWriter fw = new FileWriter(file, false);
            CSVWriter csvWriter = new CSVWriter(fw);
            csvWriter.writeNext(am.getCategoryNames());
            List<AccountManager.Account> accounts = am.getAllAccounts(false);
            for(AccountManager.Account a: accounts) {
                csvWriter.writeNext(a.getStringList(am));
            }
            csvWriter.close();
            result = file.getPath();
        } catch(Exception ex) {
            result = null;
        }
        return result;
    }

    @Override
    protected void onPreExecute() {
    }

    @Override
    protected String doInBackground(String... params) {
        if(mOperation==ActionDialog.ACTION_EXPORT) {
            if(mFileType == FILE_PB_DATA) {
                return exportData();
            }
            else if(mFileType == FILE_PB_CSV) {
                return exportCSV();
            }
        }
        else {
            if(mFileType == FILE_PB_DATA) {
                return importPbData();
            }
            else if(mFileType == FILE_PB_CSV) {
                return importPbCSV();
            }
            else if(mFileType == FILE_AW_CSV) {
                return importAwCSV();
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        if(mListener!=null) {
            mListener.onFinish(mAuthRequired, mOperation, result);
        }
    }
}
