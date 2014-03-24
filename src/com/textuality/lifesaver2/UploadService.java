package com.textuality.lifesaver2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;

import com.textuality.aerc.AppEngineClient;
import com.textuality.aerc.Response;

public class UploadService extends IntentService {

    private static final int CHUNK_SIZE = 40;
    private static final String CALLS_KIND = "Calls";
    private static final String MESSAGES_KIND = "Messages";
    private int mCallCount, mMessageCount;

    private Notifier mNotifier;
    

    public UploadService() {
        super("LifeSaver Uploader");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String authToken = intent.getStringExtra("authtoken");
        Cursor calls = getContentResolver().query(Columns.callsProvider(), null,
                null, null, null);
        Cursor messages = getContentResolver().query(Columns.messagesProvider(),
                null, null, null, null);
        mCallCount = calls.getCount();
        mMessageCount = messages.getCount();
        mNotifier = new Notifier(this);
        mNotifier.notifySave(getString(R.string.saving) + 
                mCallCount + getString(R.string.calls_and) +  
                mMessageCount + getString(R.string.messages), false);
        int sentCalls = 0, sentMessages = 0;
        ArrayList<JSONObject> toSend = new ArrayList<JSONObject>(CHUNK_SIZE);
        URL callsURI, messagesURI;
        try {
            callsURI = new URL(LifeSaver.PERSIST_APP_HREF + "calls/");
            messagesURI = new URL(LifeSaver.PERSIST_APP_HREF + "messages/");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        AppEngineClient client = new AppEngineClient(LifeSaver.PERSIST_APP, authToken, this);

        Columns call2JSON = ColumnsFactory.calls(this);
        while (calls.moveToNext()) {
            toSend.add(call2JSON.cursorToJSON(calls));
            if (toSend.size() == CHUNK_SIZE) {
                if (!transmitChunk(toSend, client, callsURI, CALLS_KIND))
                    return;
                toSend.clear();
                sentCalls += CHUNK_SIZE;
                mNotifier.notifySave(getString(R.string.saved) + sentCalls + "/" + mCallCount + 
                        getString(R.string.calls), false);
            }
        }
        if (!transmitChunk(toSend, client, callsURI, CALLS_KIND))
            return;
        sentCalls += toSend.size();
        calls.close();
        mNotifier.notifySave(getString(R.string.saved) + 
                sentCalls + getString(R.string.calls_saving) +  
                mMessageCount + getString(R.string.messages), false);

        // could break loop out into a method but it'd have like 6 arguments
        Columns message2JSON = ColumnsFactory.messages(this);
        toSend.clear();
        while (messages.moveToNext()) {
            if (message2JSON.hasField(messages, "address")) {
                toSend.add(message2JSON.cursorToJSON(messages));
                if (toSend.size() == CHUNK_SIZE) {
                    if (!transmitChunk(toSend, client, messagesURI, MESSAGES_KIND))
                        return;
                    toSend.clear();
                    sentMessages += CHUNK_SIZE;
                    mNotifier.notifySave(getString(R.string.saved) +
                            sentCalls + getString(R.string.calls_and) +
                            sentMessages + "/" + mMessageCount +
                            getString(R.string.messages) + ".", false);
                }
            } else {
                mMessageCount -= 1;
            }
        }
        if (!transmitChunk(toSend, client, messagesURI, MESSAGES_KIND))
            return;
        sentMessages += toSend.size();
        messages.close();
        mNotifier.notifySave(getString(R.string.saved) +
                sentCalls + getString(R.string.calls_and) +  
                mMessageCount + getString(R.string.messages), true);
    }

    private boolean transmitChunk(List<JSONObject> toSend, AppEngineClient client, URL target, String key) {
        ByteArrayOutputStream body = new ByteArrayOutputStream();

        byte[] lineEnd = ",\n".getBytes();
        int count = toSend.size() - 1;
        int sent = 0;
        try {
            body.write(("{ \"" + key + "\" : [\n").getBytes());
            for (JSONObject call : toSend) {
                body.write(call.toString().getBytes());
                if (sent < count)
                    body.write(lineEnd);
                else
                    body.write("\n".getBytes());
                sent++;
            }
            body.write("] }\n".getBytes());
            Response response = client.post(target, null, body.toByteArray());
            if (response == null) {
                error(client.errorMessage());
                return false;
            }
            else if ((response.status / 100) != 2) {
                error("Server problem, status " + response.status );
                return false;
            }

        } catch (IOException e) {
            error(e.getLocalizedMessage());
            return false;
        }
        return true;
    }

    private void error(String message) {
        mNotifier.notifySave(getString(R.string.upload_failed) + message, true);
    }
}
