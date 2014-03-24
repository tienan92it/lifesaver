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

import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony.Sms;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

public class LifeSaver extends Activity {
    private TextView mSaveText, mRestoreText;
    private ImageView mSaveBuoy, mRestoreBuoy;
    private final static long DURATION = 1000L;
    private Intent mNextStep;

    public static final String TAG = "LIFESAVER2"; 
    public static final String PERSIST_APP_HREF = "https://android-lifesaver.appspot.com/";
    //public static final String PERSIST_APP_HREF = "http://192.168.1.108:8080/";
    public static URL PERSIST_APP;
    public static String defaultSmsApp;
    
    @Override
    public void onCreate(Bundle mumble) {
        super.onCreate(mumble);
        setContentView(R.layout.main);

        mSaveBuoy = (ImageView) findViewById(R.id.topBuoy);
        mRestoreBuoy = (ImageView) findViewById(R.id.bottomBuoy);
        mSaveText = (TextView) findViewById(R.id.topText);
        mRestoreText = (TextView) findViewById(R.id.bottomText);

        findViewById(R.id.mainTop).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mNextStep = new Intent(LifeSaver.this, Saver.class);
                saveAnimation();
            }
        });
        findViewById(R.id.mainBottom).setOnClickListener( new OnClickListener() {
            public void onClick(View v) {
                mNextStep = new Intent(LifeSaver.this, Restorer.class);
                restoreAnimation();
            }
        });

        try {
            PERSIST_APP = new URL(PERSIST_APP_HREF);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        WebView seeRandP = (WebView) findViewById(R.id.mainSeeRandP);
        seeRandP.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        seeRandP.setBackgroundColor(0xff000000);
        seeRandP.loadDataWithBaseURL(LifeSaver.PERSIST_APP_HREF, 
                "<html><head><style> " +
                        "p { color: white; background: black; font-size: 120%;} " +
                        "a { text-decoration: none; font-weight: bold; color: #f88; }" +
                        "</style></head><body>" +
                        "<p><em>Important!</em> See <a href='http://android-lifesaver.appspot.com/retention-and-privacy.html'>Retention "+
                        "and Privacy</a> before using!</p>" +
                        "</body></html>" , 
                        "text/html", "utf-8", null);

        // If KitKat, prompt to become default SMS app if we aren't already.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            this.defaultSmsApp = Sms.getDefaultSmsPackage(this);
            if (!this.defaultSmsApp.equals(this.getPackageName())) {
                Intent intent = new Intent(Sms.Intents.ACTION_CHANGE_DEFAULT);
                intent.putExtra(Sms.Intents.EXTRA_PACKAGE_NAME, this.getPackageName());
                this.startActivity(intent);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // they may have been blanked by the transfer animation
        mSaveBuoy.setVisibility(View.VISIBLE);
        mRestoreBuoy.setVisibility(View.VISIBLE);
        mSaveText.setVisibility(View.VISIBLE);
        mRestoreText.setVisibility(View.VISIBLE);
    }

    private AnimationSet roll(ImageView buoy, boolean left) {
        AnimationSet roll = new AnimationSet(false);
        float degrees = 360F;
        float target = (float) buoy.getWidth();
        boolean landscape = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
        if (left) {
            if (!landscape) {
                degrees = -degrees;
                target = -target;
            }
        } else {
            if (landscape) {
                degrees = -degrees;
                target = -target;
            }
        }
        RotateAnimation spin = new RotateAnimation(0, degrees,
                buoy.getWidth() / 2, buoy.getHeight() / 2);
        spin.setDuration(DURATION);
        TranslateAnimation move = new TranslateAnimation(0F, target, 0F, 0F);
        move.setDuration(DURATION);
        roll.addAnimation(spin);
        roll.addAnimation(move);
        return roll;
    }

    private AlphaAnimation fade() {
        AlphaAnimation a = new AlphaAnimation(1.0F, 0.0F);
        a.setDuration(DURATION);
        return a;
    }

    AnimationListener toNextStep = new AnimationListener() {
        public void onAnimationStart(Animation animation) {
        }
        public void onAnimationRepeat(Animation animation) {
        }
        public void onAnimationEnd(Animation animation) {
            startActivity(mNextStep);

            // so that the next view slides in smoothly
            mSaveBuoy.setVisibility(View.GONE);
            mSaveText.setVisibility(View.GONE);
            mRestoreBuoy.setVisibility(View.GONE);
            mRestoreText.setVisibility(View.GONE);
        }
    };

    private void restoreAnimation() {
        Animation roll = roll(mRestoreBuoy, true);
        Animation fade = fade();
        mSaveBuoy.startAnimation(fade);
        mSaveText.startAnimation(fade);
        mRestoreText.startAnimation(fade);
        mRestoreBuoy.startAnimation(roll);

        fade.setAnimationListener(toNextStep);
    }

    private void saveAnimation() {
        Animation roll = roll(mSaveBuoy, false);
        Animation fade = fade();
        mSaveBuoy.startAnimation(roll);
        mSaveText.startAnimation(fade);
        mRestoreText.startAnimation(fade);
        mRestoreBuoy.startAnimation(fade);

        fade.setAnimationListener(toNextStep);
    }

    public static Intent comeBack(Context context) {
        Intent intent = new Intent(context, LifeSaver.class);
        return intent;
    }
}
