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

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class ScreensaverActivity extends BaseScreenOnActivity {
    static final boolean DEBUG = BuildConfig.DEBUG;
    private final static String TAG = Utils.class.getName();
    private static final String ACTION_CHARGING = "ca.mlaflamme.clocktime.CHARGING";
    private static final String ACTION_NOT_CHARGING = "ca.mlaflamme.clocktime.NOT_CHARGING";
    private static final String PARAM_DAYDREAM_MODE = "screensaver_mode";

    // This value must match android:defaultValue of
    // android:key="screensaver_clock_style" in preferences_1_1.xml  static final String DEFAULT_CLOCK_STYLE = "digital";

    private View mSaverView;
    private final Handler mHandler = new Handler();
    private final ScreensaverMoveSaverRunnable mMoveSaverRunnable;
    boolean mDaydreamMode = false;

    public ScreensaverActivity() {
        mMoveSaverRunnable = new ScreensaverMoveSaverRunnable(mHandler);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle b = getIntent().getExtras();

        if (b != null)
            mDaydreamMode = b.getBoolean(PARAM_DAYDREAM_MODE);

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
    public boolean onTouchEvent(MotionEvent event){
        finish();
        return true;
    }

    private void setClockStyle() {

        String style = Utils.getClockStyle(this);
        Utils.setAnalogOrDigitalView(this.getWindow(), style, false);

        mSaverView = findViewById(R.id.main_clock);
        mSaverView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity( Utils.getAlarmPackage(v.getContext()));
            }
        });

        // Change Date Size
        TextView mDateView = (TextView) findViewById(R.id.date);
        Utils.resizeTextView(mDateView, ScreensaverSettingsActivity.KEY_DATE_SIZE, getString(R.string.default_date_clock_size));
        Utils.setTextViewFont(mDateView, ScreensaverSettingsActivity.KEY_DATE_FONT, getString(R.string.default_date_clock_font));

        // Change Alarm Size
        TextView mAlarmView = (TextView) findViewById(R.id.nextAlarm);
        Utils.resizeTextView(mAlarmView, ScreensaverSettingsActivity.KEY_ALARM_SIZE, getString(R.string.default_alarm_clock_size));
        Utils.setTextViewFont(mAlarmView, ScreensaverSettingsActivity.KEY_ALARM_FONT, getString(R.string.default_alarm_clock_font));

        Utils.resizeContent((ViewGroup) mSaverView);

        Utils.setBrightness(getWindow(), mSaverView, mMoveSaverRunnable);

    }

    private void layoutClockSaver() {
        setClockStyle();
        View contentView = (View) mSaverView.getParent();
        contentView.forceLayout();
        mSaverView.forceLayout();
        mSaverView.setAlpha(0);

        mMoveSaverRunnable.registerViews(contentView, mSaverView);

        Utils.hideSystemUiAndRetry(contentView);
    }

    @Override
    protected void updateViews() {

        if (mDaydreamMode && !getIsPluggedIn()) {
            Log.d(TAG, "Finishing because unplugged");
            finish();
        }

        mMoveSaverRunnable.handleUpdate();
    }

    @Override
    protected int getAdditionalFlags() {
        return WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
    }

}
