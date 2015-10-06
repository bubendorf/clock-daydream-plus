/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.mlaflamme.clocktime;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.service.dreams.DreamService;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

@TargetApi(17)
public class Screensaver extends DreamService {
    static final boolean DEBUG = BuildConfig.DEBUG;
    private final static String TAG = Utils.class.getName();
    private static final String PARAM_DAYDREAM_MODE = "screensaver_mode";
    private View mSaverView;

    private final Handler mHandler = new Handler();

    private final ScreensaverMoveSaverRunnable mMoveSaverRunnable;

    public Screensaver() {
        if (DEBUG)
            Log.d(TAG, "Screensaver allocated");

        mMoveSaverRunnable = new ScreensaverMoveSaverRunnable(mHandler);
    }

    @Override
    public void onCreate() {
        if (DEBUG)
            Log.d(TAG, "Screensaver created");
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mMoveSaverRunnable.unregister();
    }

    private boolean isAutoOrientationForced() {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(ScreensaverSettingsActivity.KEY_ORIENTATION, false);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (DEBUG)
            Log.d(TAG, "Screensaver configuration changed");
        super.onConfigurationChanged(newConfig);
        mHandler.removeCallbacks(mMoveSaverRunnable);
        if (!isAutoOrientationForced()) {
            layoutClockSaver();
        }
    }

    @Override
    public void onAttachedToWindow() {
        if (DEBUG)
            Log.d(TAG, "Screensaver attached to window");
        super.onAttachedToWindow();

        // We want the screen saver to exit upon user interaction.
        setInteractive(false);
        setFullscreen(true);
        if (isAutoOrientationForced()) {

            Intent intent = new Intent(this, ScreensaverActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Bundle b = new Bundle();
            b.putBoolean(PARAM_DAYDREAM_MODE, true);
            intent.putExtras(b);
            startActivity(intent);
            finish();
        } else {
            layoutClockSaver();
        }
    }

    @Override
    public void onDetachedFromWindow() {
        if (DEBUG)
            Log.d(TAG, "Screensaver detached from window");
        super.onDetachedFromWindow();

        mHandler.removeCallbacks(mMoveSaverRunnable);
    }

    private void setClockStyle() {
        String style = Utils.getClockStyle(this);
        Utils.setAnalogOrDigitalView(this.getWindow(), style, false);

        mSaverView = findViewById(R.id.main_clock);

        Utils.resizeContent((ViewGroup) mSaverView);
        Utils.setBrightness(getWindow(), mSaverView, mMoveSaverRunnable, this);
    }

    private void layoutClockSaver() {
        if (getWindow() == null)// fix for a weird fc
            return;
        setClockStyle();
        if (mSaverView == null)// fix for a weird fc
            return;
        View mContentView = (View) mSaverView.getParent();
        mSaverView.setAlpha(0);
        if (Build.VERSION.SDK_INT >= 19) {
            Utils.hideSystemUiAndRetry(mContentView);
        }
        boolean useSlideEffect = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                ScreensaverSettingsActivity.KEY_SLIDE_EFFECT,
                ScreensaverSettingsActivity.KEY_SLIDE_EFFECT_DEFAULT);
        mMoveSaverRunnable.setSlideEffect(useSlideEffect);
        mMoveSaverRunnable.setNotificationReceiver(getApplicationContext());
        mMoveSaverRunnable.registerViews(mContentView, mSaverView);
        mHandler.post(mMoveSaverRunnable);
    }
}
