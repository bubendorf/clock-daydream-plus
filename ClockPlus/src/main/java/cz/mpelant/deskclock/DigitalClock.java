/*
 * Copyright (C) 2008 The Android Open Source Project
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

package cz.mpelant.deskclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.graphics.Typeface;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Displays the time
 */
public class DigitalClock extends LinearLayout {

    private final static String HOURS_24 = "kk";
    private final static String HOURS = "h";
    private final static String MINUTES = ":mm";
    private final String mAmString, mPmString;

    private Calendar mCalendar;
    private String mHoursFormat;
    private TextView mTimeDisplayHours, mTimeDisplayMinutes;
    private TextView mAmPm;
    private ContentObserver mFormatChangeObserver;
    private boolean mLive = true;
    private boolean mAttached;


    /* called by system on minute ticks */
    private final Handler mHandler = new Handler();
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mLive && intent.getAction().equals(
                    Intent.ACTION_TIMEZONE_CHANGED)) {
                mCalendar = Calendar.getInstance();
            }
            // Post a runnable to avoid blocking the broadcast.
            mHandler.post(new Runnable() {
                public void run() {
                    updateTime();
                }
            });
        }
    };

    private void setShowAmPm(boolean show) {
        mAmPm.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void setIsMorning(boolean isMorning) {
        mAmPm.setText(isMorning ? mAmString : mPmString);
    }

    private class FormatChangeObserver extends ContentObserver {
        public FormatChangeObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            setDateFormat();
            updateTime();
        }
    }

    public DigitalClock(Context context) {
        this(context, null);
    }

    public DigitalClock(Context context, AttributeSet attrs) {
        super(context, attrs);

        String[] ampm = new DateFormatSymbols().getAmPmStrings();
        mAmString = ampm[0];
        mPmString = ampm[1];
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mTimeDisplayHours = (TextView) findViewById(R.id.timeDisplayHours);
        mTimeDisplayMinutes = (TextView) findViewById(R.id.timeDisplayMinutes);
        mAmPm = (TextView) findViewById(R.id.am_pm);
        mCalendar = Calendar.getInstance();

        setDateFormat();
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

//        if (Log.LOGV) Log.v("onAttachedToWindow " + this);

        if (mAttached) return;
        mAttached = true;

        if (mLive) {
            /* monitor time ticks, time changed, timezone */
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            getContext().registerReceiver(mIntentReceiver, filter);
        }

        /* monitor 12/24-hour display preference */
        mFormatChangeObserver = new FormatChangeObserver();
        getContext().getContentResolver().registerContentObserver(
                Settings.System.CONTENT_URI, true, mFormatChangeObserver);

        updateTime();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (!mAttached) return;
        mAttached = false;

        if (mLive) {
            getContext().unregisterReceiver(mIntentReceiver);
        }
        getContext().getContentResolver().unregisterContentObserver(
                mFormatChangeObserver);
    }

    private void updateTime() {
        if (mLive) {
            mCalendar.setTimeInMillis(System.currentTimeMillis());
        }

        StringBuilder fullTimeStr = new StringBuilder();
        CharSequence newTime = DateFormat.format(mHoursFormat, mCalendar);
        mTimeDisplayHours.setText(newTime);
//        mTimeDisplayHoursThin.setText(newTime);
        fullTimeStr.append(newTime);
        newTime = DateFormat.format(MINUTES, mCalendar);
        fullTimeStr.append(newTime);
        mTimeDisplayMinutes.setText(newTime);

        boolean isMorning = mCalendar.get(Calendar.AM_PM) == 0;
        setIsMorning(isMorning);
        if (!Alarms.get24HourMode(getContext())) {
            fullTimeStr.append(mAmPm.getText());
        }

        // Update accessibility string.
        setContentDescription(fullTimeStr);
    }

    private void setDateFormat() {
        mHoursFormat = Alarms.get24HourMode(getContext()) ? HOURS_24 : HOURS;

        setShowAmPm(!Alarms.get24HourMode(getContext()));
    }



}
