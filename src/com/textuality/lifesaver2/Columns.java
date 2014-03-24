/*
 * Copyright 2010 Google Inc.
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

package com.textuality.lifesaver2;

import java.util.Hashtable;
import java.util.Map;
import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.telephony.PhoneNumberUtils;

/*
 * The system provides phone-call and SMS data via content-provider Cursors.  The data is 
 * interchanged over the network in JSON.  This takes care of shuffling the records back 
 * and forth, with a certain amount of type-sensitivity.
 */
public class Columns {

    private String[] mNames;

    private enum Type {
        STRING, INT, LONG, FLOAT, DOUBLE
    }

    private Type[] mTypes;
    private int[] mColumns = null;
    private String mKey1 = null, mKey2 = null;
    private int mKey1Index = -1, mKey2Index;
    private TimeZone mTZ = TimeZone.getDefault();
    private NameFinder mNameFinder;

    public Columns(Context context, String[] names, Class<?>[] classes, String key1, String key2) {
        this.mNames = names;
        mTypes = new Type[names.length];
        mKey1 = key1;
        mKey2 = key2;
        mNameFinder = new NameFinder(context);
        for (int i = 0; i < names.length; i++) {

            if (classes[i] == String.class)
                mTypes[i] = Type.STRING;
            else if (classes[i] == Integer.TYPE || classes[i] == Integer.class)
                mTypes[i] = Type.INT;
            else if (classes[i] == Long.TYPE || classes[i] == Long.class)
                mTypes[i] = Type.LONG;
            else if (classes[i] == Float.TYPE || classes[i] == Float.class)
                mTypes[i] = Type.FLOAT;
            else if (classes[i] == Double.TYPE || classes[i] == Double.class)
                mTypes[i] = Type.DOUBLE;
        }
    }

    private void setColumns(Cursor cursor) {
        if (mColumns != null)
            return;
        mColumns = new int[mNames.length];
        for (int i = 0; i < mNames.length; i++)
            mColumns[i] = cursor.getColumnIndex(mNames[i]);
    }

    public String jsonToKey(JSONObject json) {
        return json.opt(mKey1).toString() + "/" + json.opt(mKey2).toString();
    }

    public String cursorToKey(Cursor c) {
        if (mKey1Index == -1) {
            mKey1Index = c.getColumnIndex(mKey1);
            mKey2Index = c.getColumnIndex(mKey2);
        }
        return c.getString(mKey1Index) + "/" + c.getString(mKey2Index);
    }
    
    public static Uri callsProvider() {
        return CallLog.Calls.CONTENT_URI;
    }
    public static Uri messagesProvider() {
        return Uri.parse("content://sms");
    }

    public boolean hasField(Cursor c, String name) {
        int i = c.getColumnIndex(name);
        return ((i != -1) && !c.isNull(i));
    }

    public static Map<String, Boolean> loadKeys(Context context, Uri provider, Columns columns) {
        Cursor cursor = context.getContentResolver().query(provider, null, null, null, null);
        Boolean exists = new Boolean(true);
        Map<String, Boolean> map = new Hashtable<String, Boolean>();
        while (cursor.moveToNext()) {
            map.put(columns.cursorToKey(cursor), exists);
        }
        cursor.close();
        return map;
    }

    public JSONObject cursorToJSON(Cursor cursor) {
        setColumns(cursor);
        JSONObject json = new JSONObject();
        try {
            for (int i = 0; i < mNames.length; i++) {
                int col = mColumns[i];
                if (cursor.isNull(col))
                    continue;
                switch (mTypes[i]) {
                case STRING:
                    String str = cursor.getString(col);
                    if (mNames[i].equals("number")) {
                        json.put("name", mNameFinder.find(str));
                    } else if (mNames[i].equals("address")) {
                        str = PhoneNumberUtils.formatNumber(str);
                        str = PhoneNumberUtils.stripSeparators(str);
                        json.put("name", mNameFinder.find(str));
                    } 
                    json.put(mNames[i], str);
                    break;
                case INT:
                    json.put(mNames[i], cursor.getInt(col));
                    break;
                case LONG:
                    long val = cursor.getLong(col);
                    json.put(mNames[i], val);
                    if (mNames[i].equals("date")) {
                        json.put("tzoffset", mTZ.getOffset(val));
                    }
                    break;
                case FLOAT:
                    json.put(mNames[i], cursor.getFloat(col));
                    break;
                case DOUBLE:
                    json.put(mNames[i], cursor.getDouble(col));
                    break;
                }
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return json;
    }

    public ContentValues jsonToContentValues(JSONObject j) {
        ContentValues cv = new ContentValues();
        for (int i = 0; i < mNames.length; i++) {
            switch (mTypes[i]) {
            case STRING:
                j2cvString(j, cv, mNames[i]);
                break;
            case INT:
                j2cvInt(j, cv, mNames[i]);
                break;
            case LONG:
                j2cvLong(j, cv, mNames[i]);
                break;
            case FLOAT:
                j2cvFloat(j, cv, mNames[i]);
                break;
            case DOUBLE:
                j2cvDouble(j, cv, mNames[i]);
                break;
            }
        }

        return cv;
    }

    private static void j2cvInt(JSONObject j, ContentValues cv, String key) {
        try {
            int v = j.getInt(key);
            cv.put(key, v);
        } catch (JSONException e) {
        }
    }

    private static void j2cvLong(JSONObject j, ContentValues cv, String key) {
        try {
            long v = j.getLong(key);
            cv.put(key, v);
        } catch (JSONException e) {
        }
    }

    private static void j2cvString(JSONObject j, ContentValues cv, String key) {
        try {
            String v = j.getString(key);
            cv.put(key, v);
        } catch (JSONException e) {
        }
    }

    private static void j2cvFloat(JSONObject j, ContentValues cv, String key) {
        try {
            float v = (float) j.getDouble(key);
            cv.put(key, v);
        } catch (JSONException e) {
        }
    }

    private static void j2cvDouble(JSONObject j, ContentValues cv, String key) {
        try {
            double v = j.getDouble(key);
            cv.put(key, v);
        } catch (JSONException e) {
        }
    }

}
