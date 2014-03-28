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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;

import com.textuality.aerc.Authenticator;

public class Restorer extends Activity {

    private WebView mReadout;
    private static final String mHead = "<style>body {color: #fff; background: #000}\n" +
            "a { text-decoration: none; font-weight: bold; color: #f88;}</style>";

    @Override
    protected void onCreate(Bundle mumble) {
        super.onCreate(mumble);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.restore);

        mReadout = (WebView) findViewById(R.id.restoreData);
        mReadout.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        mReadout.setBackgroundColor(0xff000000);
        mReadout.loadDataWithBaseURL(LifeSaver.PERSIST_APP_HREF, 
                "<html><head>" + mHead + "</head><body><p>" +
                        Restorer.this.getString(R.string.preparing_restore) +
                        "</p></body></html>", 
                        "text/html", "utf-8", null);
        
        new PrepareDownload().execute();
    }

    class PrepareDownload extends AsyncTask<Void, Void, String> {

        private Account mAccount;
        private String mErrorMessage = null;

        @Override
        protected String doInBackground(Void... params) {
            (new Notifier(Restorer.this)).notifyRestore(Restorer.this.getString(R.string.restoring), false);

            Account[] accounts = AccountManager.get(Restorer.this).getAccountsByType("com.google");
            if (accounts.length == 0) {
                mErrorMessage = Restorer.this.getString(R.string.no_accounts);
                return null;
            }
            mAccount = accounts[0];
            Authenticator authent = Authenticator.appEngineAuthenticator(Restorer.this, mAccount, LifeSaver.PERSIST_APP);
 
            String authToken = authent.token();
            if (authToken == null)  {
                mErrorMessage = authent.errorMessage();
                return null;
            }
            return authToken;
        }

        @Override
        protected void onPostExecute(String authToken) {
            String body;
            if (authToken == null) {
                body = "<p>" + Restorer.this.getString(R.string.ouch) + 
                        "<b>" + mErrorMessage + "</b></p>";

            } else {
                body = "<p>" + 
                        Restorer.this.getString(R.string.restoration_in_progress) + " " +
                        Restorer.this.getString(R.string.stand_by) +
                        "</p>";
            }

            String page = "<html><head>" + mHead + "</head><body>" + body + "</body></html>";
            mReadout.loadDataWithBaseURL(LifeSaver.PERSIST_APP_HREF, page, "text/html", "utf-8", null);

            if (authToken != null) {
                Intent intent = new Intent(Restorer.this, DownloadService.class);
                intent.putExtra("authtoken", authToken);
                startService(intent);
            }
        }        
    }
}
