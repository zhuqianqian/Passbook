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

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Locale;

public class AccountManager {
    
    class EntryType {
        public static final int SINGLETEXT = 1;
        public static final int PASSWORD = 2;
        public static final int WEBADDR = 3;
        public static final int EMAILADDR = 4;
        public static final int PIN = 5;
    }

    private static final String FIELD_DELIMITER = "\t";
    private static final String ITEM_DELIMITER = "\0";
    private static final String ENTRY_DELIMITER = "\0\0";
    public static final int DEFAULT_CATEGORY_ID = 0;
    public static final int ALL_CATEGORY_ID = -1;

    public class Category {
        int mId;
        int mImgCode;
        String mName;
        public Category(int id, int imgCode, String name) {
            mId = id;
            mImgCode = imgCode;
            mName = name;
        }
    }

    public class Account {
        
        class Entry {
            int mType;
            String mName;
            String mValue;

            Entry(int type, String name, String value) {
                mType = type;
                mName = name;
                mValue = value;
            }
        }
        
        private int mCategoryId;
        public String mProfile;
        private ArrayList<Entry> mEntries;
        public int mId;
        
        public Account(int categoryId) {
            mCategoryId = categoryId;
            mEntries = new ArrayList<> ();
        }
        
        public Account(String accountData) {
            mEntries = new ArrayList<>();
            setAccountData(accountData);            
        }
        
        String getAccountName() {
            return mProfile;
        }
        
        public int getCategoryId() {
            return mCategoryId;
        }
        
        public Account clone() {
            Account account = new Account(this.mCategoryId);
            account.mProfile = this.mProfile;
            account.mId = this.mId;
            for(Entry entry : this.mEntries) {
                account.mEntries.add(new Entry(entry.mType, entry.mName, entry.mValue));
            }
            return account;
        }
        
        void setAccountData(String accountData) {
            String[] entrylist = accountData.split(ITEM_DELIMITER);
            String[] items;
            items = entrylist[0].split(FIELD_DELIMITER, 2);
            mCategoryId = Integer.parseInt(items[0]);
            mProfile = items[1];
            for(int i = 1; i < entrylist.length; ++i) {
                items = entrylist[i].split(FIELD_DELIMITER, 3);
                Entry entry = new Entry(Integer.parseInt(items[0]), items[1], items[2]);
                mEntries.add(entry);
            }
        }
        
        ArrayList<Entry> getEntryList() {
            return mEntries;
        }
        
        public void setAccount(Account account) {
            mEntries.clear();
            this.mProfile = account.mProfile;
            for(Entry e : account.mEntries) {
                this.mEntries.add(new Entry(e.mType, e.mName, e.mValue));
            }
        }

        int addEntry(Entry entry) {
            if(mEntries == null) {
                mEntries = new ArrayList<> ();
            }
            mEntries.add(entry);
            return 0;
        }

        int addEntry(int type, String name, String value) {
            Entry entry = new Entry(type, name, value);
            mEntries.add(entry);
            return 0;
        }
        
        public ArrayList<Entry> getAccount() {
            return mEntries; 
        }
        
        public void setCategory(int newCategoryId) {
            mCategoryId = newCategoryId;
        }
        
        public void setName(String name) {
            mProfile = name;
        }

        void clearEntries() {
            mEntries.clear();
        }
        
        byte[] getBytes() {
            String ret;
            ret = String.format(Locale.ENGLISH, "%d%s%s\0", mCategoryId, FIELD_DELIMITER, mProfile);
            for(Entry e : mEntries) {
                ret += String.format(Locale.ENGLISH,"%d%s%s%s%s\0", e.mType, FIELD_DELIMITER, e.mName, FIELD_DELIMITER, e.mValue);
            }
            ret += "\0";
            return ret.getBytes();
        }
        
        Entry newEntry(String name, String value, int type) {
            return new Entry(type, name, value);
        }
        
        String[] getStringList(AccountManager am) {
            String[] result = new String[mEntries.size()+1];
            int i = 1;
            result[0] = String.format("%s\t%s", am.getCategory(this.mCategoryId).mName, mProfile);
            for(Entry e : mEntries) {
                result[i++] = String.format(Locale.ENGLISH,"%d\t%s\t%s", e.mType, e.mName, e.mValue);
            }
            return result;
        }
    }

    private Hashtable<Integer, Category> mCategories;
    private ArrayList<Account> mAccounts;
    private Hashtable<Integer, ArrayList<Integer>> mMap;
    private boolean mChanged = false;
    private int mNullCount = 0;
    
    private static final Collator COLLATOR = Collator.getInstance(Locale.getDefault());
    private static final Comparator<Account> ACC_COMPARATOR = new Comparator<Account>() {
        @Override
        public int compare(Account arg0, Account arg1) {
            return COLLATOR.compare(arg0.mProfile, arg1.mProfile);
        }
    };

    public AccountManager(String data) {
        mCategories = new  Hashtable<> ();
        mMap = new Hashtable<>();
        mAccounts = new ArrayList<>();
        if(data!=null) {
            setData(data);
        }
    }
    
    void setDefaultCategory(int imgCode, String name) {
        if(mCategories!=null) {
            mCategories.put(DEFAULT_CATEGORY_ID,
                    new Category(DEFAULT_CATEGORY_ID, imgCode, name));
            ArrayList<Integer> intList = mMap.get(0);
            if(intList==null) {
                intList = new ArrayList<>();
                mMap.put(0, intList);
            }
        }
    }
    
    public void setData(String data) {
        String [] accountList = data.split(ENTRY_DELIMITER);
        String [] tmp = accountList[0].split(ITEM_DELIMITER);
        String [] details;
        Integer key;
        ArrayList<Integer> intList;
        int i, pos, begin = 1;
        
        for(i = 0; i < tmp.length; ++i) {
            details = tmp[i].split(FIELD_DELIMITER, 3);
            if(details.length < 3) {
                begin = 0;
                break;
            }
            mCategories.put(Integer.valueOf(details[0]), 
                    new Category(Integer.parseInt(details[0]), Integer.parseInt(details[1]), details[2]));
        }
        Account account;
        for(i = begin, pos = 0; i < accountList.length; ++i, ++pos) {
            tmp = accountList[i].split(FIELD_DELIMITER, 2);
            if(tmp.length < 2) {
                break;
            }
            key = Integer.parseInt(tmp[0]);
            intList = mMap.get(key);
            if(intList == null) {
                intList = new ArrayList<> ();
                intList.add(pos);
                mMap.put(key, intList);
            }
            else {
                intList.add(pos);
            }
            account = new Account(accountList[i]);
            account.mId = pos;
            mAccounts.add(account);
        }
    }
    
    ArrayList<Category> getCategoryList(boolean includeDefault, boolean sort) {
        
        @SuppressWarnings("unchecked")
        Hashtable<Integer, Category> categories = (Hashtable<Integer, Category>) mCategories.clone();
        if(!includeDefault) {
            categories.remove(0);
        }
        ArrayList<Category> result = new ArrayList<>(categories.values());
        if(sort) {
            Collections.sort(result, new Comparator<Category>() {
                @Override
                public int compare(Category arg0, Category arg1) {
                    return COLLATOR.compare(arg0.mName, arg1.mName);
                }
            });
        }
        return result; 
    }
    
    int getAccountsCountByCategory(int id) {
        int size=0;
        if(id == ALL_CATEGORY_ID) {
            return (mAccounts.size() - mNullCount);
        }
        ArrayList<Integer> intList = mMap.get(id);
        if(intList==null) {
            return 0;
        }
        if(mNullCount > 0) {
            Account a;
            for(Integer i : intList) {
                a = mAccounts.get(i);
                if(a!=null) {
                    size++;
                }
            }
        }
        else {
            size = intList.size();
        }
        return size;
    }
    
    ArrayList<Account> getAccountsByCategory(int categoryId) {
        if(categoryId == ALL_CATEGORY_ID) {
            return getAllAccounts(true);
        }
        ArrayList<Integer> intList = mMap.get(categoryId);
        ArrayList<Account> accounts = new ArrayList<>();
        
        Account a;
        if(intList == null) {
            return null;
        }
        for(Integer i : intList) {
            a = mAccounts.get(i);
            if(a!=null) {
                accounts.add(a);
            }
        }
        Collections.sort(accounts, ACC_COMPARATOR);
        return accounts;
    }
    
    ArrayList<Account> getAllAccounts(boolean sort) {
        ArrayList<Account> copyOfAccounts = new ArrayList<>();
        for(Account account : mAccounts) {
            if(account!=null) {
                copyOfAccounts.add(account);
            }
        }
        if(sort) {
            Collections.sort(copyOfAccounts, ACC_COMPARATOR);
        }
        return copyOfAccounts;
    }
    
    private byte[] getCategoryBytes() {
        ArrayList<Category> categories = getCategoryList(false, false);
        String ret = "";
        if(categories.size() > 0) {
            for(Category c : categories) {
                ret += String.format(Locale.ENGLISH, "%d%s%d%s%s\0", c.mId, FIELD_DELIMITER,c.mImgCode, FIELD_DELIMITER, c.mName);
            }
            ret += ITEM_DELIMITER;
        }
        return ret.getBytes();
    }
    
    private ArrayList<byte[]> getAccountBytesList() {
        ArrayList<byte[]> bytesList = new ArrayList<>();
        ArrayList<Account> accounts = getAllAccounts(false);
        for(Account a : accounts) {
            if(a!=null) {
                bytesList.add(a.getBytes());
            }
        }
        return bytesList;
    }
    
    byte[] getBytes() {
        byte[] categoryBytes = getCategoryBytes();
        ArrayList<byte[]> accountsBytes = getAccountBytesList();
        byte[] retBytes;
        int size = categoryBytes.length;
        for(byte[] b : accountsBytes) {
            size += b.length;
        }

        retBytes = new byte[size];
        System.arraycopy(categoryBytes, 0, retBytes, 0, categoryBytes.length);
        size = categoryBytes.length;
        for(byte[] b : accountsBytes) {
            System.arraycopy(b, 0, retBytes, size, b.length);
            size += b.length;
        }

        return retBytes;
    }
    
    Category getCategory(int categoryId) {
        return mCategories.get(categoryId);
    }
    
    void addAccount(int categoryId, Account account) {
        account.mId = mAccounts.size();
        account.mCategoryId = categoryId;
        mAccounts.add(account);
        
        ArrayList<Integer> intList = mMap.get(categoryId);
        if(intList==null) {
            intList = new ArrayList<>();
            intList.add(account.mId);
            mMap.put(categoryId, intList);
        }
        else {
            intList.add(account.mId);
        }
        mChanged = true;
    }
    
    Account getAccountById(int id) {
        return mAccounts.get(id);
    }
    
    int addCategory(int imgCode, String name) {
        int id = mCategories.size();
        if(id == 0) {
            id = 1;
        }
        while(mCategories.get(id)!=null) {
            id++;
        }
        mCategories.put(id, new Category(id, imgCode, name));
        mChanged = true;
        return id;
    }
    
    void removeCategory(int id, boolean removeAccounts) {
        if(id < 1) {
            return;
        }
        mCategories.remove(id);
        Account account;
        ArrayList<Integer> intListi = mMap.get(id);
        if(intListi!=null) {
            if(removeAccounts) {
                for(Integer i : intListi) {
                    mAccounts.set(i, null);
                    mNullCount++;
                }
            }
            else {
                ArrayList<Integer> intList0 = mMap.get(0);
                for(Integer i : intListi) {
                    account = mAccounts.get(i);
                    account.mCategoryId = 0;
                    intList0.add(i);
                }
            }
        }
        mMap.remove(id);
        mChanged = true;
    }
    
    Account getTemplate(int categoryId) {
        Account result = null;
        Account template;
        
        ArrayList<Integer> intList = mMap.get(categoryId);
        if(intList!=null && intList.size() > 0) {
            template = mAccounts.get(intList.get(0));
            result = new Account(template.mCategoryId);
            result.mProfile = template.mProfile;
            for(Account.Entry entry : template.mEntries) {
                result.addEntry(entry.mType, entry.mName, "");
            }
        }
        return result;
    }
    
    void removeAccount(Account account) {
        mAccounts.set(account.mId, null);
        ArrayList<Integer> intList = mMap.get(account.mCategoryId);
        intList.remove(Integer.valueOf(account.mId));
        mChanged = true;
        mNullCount += 1;
    }
    
    public void setAccount(Account account) {
        Account previous = getAccountById(account.mId);
        if(previous.mCategoryId != account.mCategoryId) {
            ArrayList<Integer> intList = mMap.get(previous.mCategoryId);
            intList.remove(Integer.valueOf(account.mId));
            ArrayList<Integer> target = mMap.get(account.mCategoryId);
            if(target == null) {
                target = new ArrayList<>();
                mMap.put(account.mCategoryId, target);
            }
            target.add(account.mId);            
        }
        mAccounts.set(account.mId, account);
        mChanged = true;        
    }
    
    void moveAccount(int destCategory, Account account) {
        ArrayList<Integer> intList = mMap.get(account.mCategoryId);
        intList.remove(Integer.valueOf(account.mId));
        account.setCategory(destCategory);
        ArrayList<Integer> target = mMap.get(destCategory);
        if(target == null) {
            target = new ArrayList<>();
            mMap.put(destCategory, target);
        }
        target.add(account.mId);
        mChanged = true;
    }
    
    boolean setCategory(int id, String name, int imgCode) {
        Category category = getCategory(id);
        if(category.mName.equals(name) && category.mImgCode == imgCode) {
            return false;
        }
        category.mName = name;
        category.mImgCode = imgCode;
        mChanged = true;
        return true;
    }
    
    Account newAccount(int categoryId) {
        return new Account(categoryId);
    }
    
    boolean saveRequired() {
        return mChanged;
    }
    
    void onSaved() {
        mChanged = false;
    }
    
    String[] getCategoryNames() {
        Collection<Category> categories = mCategories.values();
        String[] result = new String[categories.size()];
        int i = 0;
        for(Category c : categories) {
            result[i++] = c.mName;
        }
        return result;
    }
}