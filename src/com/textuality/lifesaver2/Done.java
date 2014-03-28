package com.textuality.lifesaver2;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony.Sms;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.webkit.WebView;

public class Done extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        boolean isRestore = intent.getBooleanExtra("isRestore", false);
        final String greeting = (isRestore) ? this.getString(R.string.done_restoring) : this.getString(R.string.done_saving);
        final String result = greeting + "\n\n" + intent.getStringExtra("result");

        final WebView readout;
        final View bottom;
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (isRestore){
            setContentView(R.layout.restore);
            readout = (WebView) findViewById(R.id.restoreData);
            bottom = findViewById(R.id.restoreBottom);
        } else {
            setContentView(R.layout.save);
            readout = (WebView) findViewById(R.id.saveData);
            bottom = findViewById(R.id.saveBottom);
        }
        bottom.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(Done.this, LifeSaver.class));
            }
        });

        readout.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        readout.setBackgroundColor(0xff000000);
        readout.loadDataWithBaseURL(LifeSaver.PERSIST_APP_HREF, 
                "<html><head>" + Saver.mHead + "</head><body><p>" +
                        result +
                        "</p></body></html>", 
                        "text/html", "utf-8", null);

        fixSmsForKitKat();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void fixSmsForKitKat() {
        // If KitKat, prompt to restore default SMS app.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            final Intent i = new Intent(Sms.Intents.ACTION_CHANGE_DEFAULT);
            i.putExtra(Sms.Intents.EXTRA_PACKAGE_NAME,  LifeSaver.mDefaultSmsApp);
            this.startActivity(i); 
        }
    }
}
