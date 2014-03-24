package com.textuality.lifesaver2;

import java.util.HashMap;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

public class NameFinder {

    private HashMap<String, String> mCache = new HashMap<String, String>();
    private static final String[] mProjection = { ContactsContract.PhoneLookup.DISPLAY_NAME };
    private Context mContext;

    public NameFinder(Context context) {
        mContext = context;
    }

    public String find(String number) {
        String name = mCache.get(number);
        if (name == null) {
            name = "";
            try {
                Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                        Uri.encode(number));
                Cursor cursor = mContext.getContentResolver().query(uri, mProjection, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst())
                        name = cursor.getString(0);
                    cursor.close();
                    mCache.put(number, name);
                }
            } catch (Exception e) {
                mCache.put(number, ""); // oh well
            }
        }
        return name;
    }

}
