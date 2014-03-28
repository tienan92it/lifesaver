package com.textuality.lifesaver2;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Telephony.Sms;

import com.textuality.aerc.AppEngineClient;
import com.textuality.aerc.Response;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class DownloadService extends IntentService {

    private Notifier mNotifier;
    private static final Map<String, List<String>> MEDIA_TYPES_LIST = new HashMap<String, List<String>>();
    private static final ContentValues DUMMY_TEMPLATE;

    static {
        MEDIA_TYPES_LIST.put("Accept", Arrays.asList("application/json"));
        DUMMY_TEMPLATE = new ContentValues();
        DUMMY_TEMPLATE.put("status", -1);
        DUMMY_TEMPLATE.put("read", "1");
        DUMMY_TEMPLATE.put("type", 2);
        DUMMY_TEMPLATE.put("date", 1330452144403L);
    }

    public DownloadService() {
        super("LifeSaver Downloader");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String authToken = intent.getStringExtra("authtoken");
        mNotifier = new Notifier(this);
        mNotifier.notifyRestore(getString(R.string.restoring), false);

        // 1. set up
        Columns callColumns = ColumnsFactory.calls(this);
        Columns messageColumns = ColumnsFactory.messages(this);

        Map<String, Boolean> loggedCalls = Columns.loadKeys(this, Columns.callsProvider(), callColumns);
        Map<String, Boolean> loggedMessages = Columns.loadKeys(this, Columns.messagesProvider(), messageColumns);

        URL callsURI, messagesURI;
        try {
            callsURI = new URL(LifeSaver.PERSIST_APP_HREF + "calls/");
            messagesURI = new URL(LifeSaver.PERSIST_APP_HREF + "messages/");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        // 2. Fetch calls
        AppEngineClient client = new AppEngineClient(LifeSaver.PERSIST_APP, authToken, this);
        Response response = client.get(callsURI, MEDIA_TYPES_LIST);
        if (response == null) {
            error(client.errorMessage());
        } else if ((response.status / 100) != 2) {
            error(getString(R.string.download_failed) + response.status);
        }

        // 3. load calls we haven't already seen
        Restored callsRestored = restore(response, loggedCalls, callColumns, Columns.callsProvider(), null, "calls");
        mNotifier.notifyRestore(getString(R.string.restored) + callsRestored.stored + "/" + 
                callsRestored.downloaded + 
                getString(R.string.calls), false);

        // 4. Fetch messages
        response = client.get(messagesURI, MEDIA_TYPES_LIST);
        if (response == null) {
            error(client.errorMessage());
        }
        else if ((response.status / 100) != 2) {
            error(getString(R.string.download_failed) + response.status);
        }

        // 5. load messages we haven't already seen
        Uri messagesProvider = Columns.messagesProvider();

        Restored messagesRestored = restore(response, loggedMessages, messageColumns, messagesProvider, "thread_id", "messages");
        mNotifier.notifyRestore(getString(R.string.restored) + callsRestored.stored + "/" + callsRestored.downloaded + 
                getString(R.string.calls) + ", " +
                messagesRestored.stored + "/" + messagesRestored.downloaded + 
                getString(R.string.messages), true);

        // 6. Now, run through all the messages we just restored, add a bogus message for each address, and then delete it.
        //    This forces the MmsSmsProvider to recalculate the timestamp
        ContentValues dummyValues = new ContentValues(DUMMY_TEMPLATE);
        dummyValues.put("body", "LifeSaver dummy message at " + System.currentTimeMillis());
        HashMap<String, String> patchedAddresses = new HashMap<String, String>();
        ContentResolver cr = getContentResolver();
        for (int i = 0; i < messagesRestored.toRestore.length(); i++) {
            JSONObject json = (JSONObject) messagesRestored.toRestore.optJSONObject(i);
            String address = json.optString("address");
            if (address != null && patchedAddresses.get(address) == null) {
                dummyValues.put("address", address);
                Uri dummyUri = cr.insert(messagesProvider, dummyValues);
                cr.delete(dummyUri, null, null);
                patchedAddresses.put(address, address);
            } 
        }

        Intent done = new Intent(this, Done.class);
        done.putExtra("isRestore", true);
        done.putExtra("result", getString(R.string.restored) + callsRestored.stored + "/" + callsRestored.downloaded + 
                getString(R.string.calls) + ", " +
                messagesRestored.stored + "/" + messagesRestored.downloaded + 
                getString(R.string.messages) + ".");
        done.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(done);
        
        stopSelf();
    }

    private Restored restore(Response response, Map<String, Boolean> logged, Columns columns, 
            Uri provider, String zeroField, String callsOrMessages) {
        String body = new String(response.body);
        Restored restored = new Restored();
        ContentResolver cr = getContentResolver();
        try {  
            JSONObject bodyJSON = new JSONObject(body);
            JSONArray toRestore = restored.toRestore = bodyJSON.getJSONArray(callsOrMessages);

            for (int i = 0; i < toRestore.length(); i++) {
                restored.downloaded++;
                JSONObject json = (JSONObject) toRestore.get(i);
                String key = columns.jsonToKey(json);
                if (logged.get(key) == null) {
                    ContentValues cv = columns.jsonToContentValues(json);
                    if (zeroField != null)
                        cv.put(zeroField, 0);
                    cr.insert(provider, cv);
                    restored.stored++;
                }
            }
        } catch (JSONException e) {
            error("JSON exception " + e.getLocalizedMessage());
        }
        return restored;
    }

    private void error(String message) {
        mNotifier.notifyRestore(getString(R.string.ouch) + message, true);


        // If KitKat, prompt to restore default SMS app.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Intent i = new Intent(Sms.Intents.ACTION_CHANGE_DEFAULT);
            i.putExtra(Sms.Intents.EXTRA_PACKAGE_NAME, LifeSaver.mDefaultSmsApp);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.startActivity(i);
        }

        stopSelf();
    }

    private class Restored {
        public int downloaded = 0;
        public int stored = 0;
        public JSONArray toRestore = null;
    }
}
