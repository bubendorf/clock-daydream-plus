/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

public class ScreensaverActivity extends BaseScreenOnActivity {
    static final boolean DEBUG = BuildConfig.DEBUG;
    static final String TAG = "DeskClock/ScreensaverAc";

    // This value must match android:defaultValue of
    // android:key="screensaver_clock_style" in preferences_1_1.xml  static final String DEFAULT_CLOCK_STYLE = "digital";

    private View mContentView, mSaverView;
    private final Handler mHandler = new Handler();
    private final ScreensaverMoveSaverRunnable mMoveSaverRunnable;

    public ScreensaverActivity() {
        mMoveSaverRunnable = new ScreensaverMoveSaverRunnable(mHandler);
    }

    /**
     * <p>If the charging is over, for the activity to finish.</p>
     */
    public class PowerConnectionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isNotCharging = status == BatteryManager.BATTERY_STATUS_NOT_CHARGING;

            if (isNotCharging)
                finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean useSlideEffect = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                ScreensaverSettingsActivity.KEY_SLIDE_EFFECT, false);
        mMoveSaverRunnable.setSlideEffect(useSlideEffect);
        mMoveSaverRunnable.setNotificationReceiver(getApplicationContext());


        layoutClockSaver();
        mHandler.post(mMoveSaverRunnable);
    }


    @Override
    public void onPause() {
        mHandler.removeCallbacks(mMoveSaverRunnable);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMoveSaverRunnable.unregister();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (DEBUG)
            Log.d(TAG, "Screensaver config changed");
        super.onConfigurationChanged(newConfig);
        mHandler.removeCallbacks(mMoveSaverRunnable);
        layoutClockSaver();
        mHandler.postDelayed(mMoveSaverRunnable, 250);
    }

    @Override
    public void onUserInteraction() {
        finish();
    }

    private void setClockStyle() {
        String style = Utils.getClockStyle(this);
        Utils.setAnalogOrDigitalView(this.getWindow(), style, false);

        mSaverView = findViewById(R.id.main_clock);

        Utils.resizeContent((ViewGroup) mSaverView);

        Utils.setBrightness(getWindow(), mSaverView, mMoveSaverRunnable);

    }

    private void layoutClockSaver() {
        setClockStyle();
        mContentView = (View) mSaverView.getParent();
        mContentView.forceLayout();
        mSaverView.forceLayout();
        mSaverView.setAlpha(0);

        mMoveSaverRunnable.registerViews(mContentView, mSaverView);

        Utils.hideSystemUiAndRetry(mContentView);
        Utils.refreshAlarm(ScreensaverActivity.this, mContentView);
    }

    @Override
    protected void updateViews() {
    }

    @Override
    protected int getAdditionalFlags() {
        return WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
    }

}
