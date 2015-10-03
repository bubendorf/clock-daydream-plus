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

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.service.dreams.DreamService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ca.mlaflamme.clocktime.notification.IconNotFoundException;
import ca.mlaflamme.clocktime.notification.NotificationInfo;
import ca.mlaflamme.clocktime.preference.SeekBarPreference;

public class Utils {
    private final static String TAG = Utils.class.getName();

    private final static String PARAM_LANGUAGE_CODE = "hl";

    /**
     * Help URL query parameter key for the app version.
     */
    private final static String PARAM_VERSION = "version";

    /**
     * Cached version code to prevent repeated calls to the package manager.
     */
    private static String sCachedVersionCode = null;

    /**
     * Intent to be used for checking if a clock's date has changed. Must be every fifteen
     * minutes because not all time zones are hour-locked.
     **/
    public static final String ACTION_ON_QUARTER_HOUR = "ca.mlaflamme.clocktime.ON_QUARTER_HOUR";

    /** Types that may be used for clock displays. **/
    public static final String CLOCK_TYPE_DIGITAL2 = "digital2";
    public static final String CLOCK_TYPE_DIGITAL = "digital";
    public static final String CLOCK_TYPE_ANALOG = "analog";

    public static final String CLOCK_SIZE_SMALL = "small";
    public static final String CLOCK_SIZE_MEDIUM = "medium";
    public static final String CLOCK_SIZE_LARGE = "large";
    public static final String CLOCK_SIZE_XLARGE = "xlarge";
    public static final String CLOCK_SIZE_2XLARGE = "2xlarge";

    /**
     * time format constants
     */
    public final static String HOURS_24 = "kk";
    public final static String HOURS = "h";
    public final static String MINUTES = ":mm";

    /**
     * Adds two query parameters into the Uri, namely the language code and the version code
     * of the app's package as gotten via the context.
     * 
     * @return the uri with added query parameters
     */
    private static Uri uriWithAddedParameters(Context context, Uri baseUri) {
        Uri.Builder builder = baseUri.buildUpon();

        // Add in the preferred language
        builder.appendQueryParameter(PARAM_LANGUAGE_CODE, Locale.getDefault().toString());

        // Add in the package version code
        if (sCachedVersionCode == null) {
            // There is no cached version code, so try to get it from the package manager.
            try {
                // cache the version code
                PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                sCachedVersionCode = Integer.toString(info.versionCode);

                // append the version code to the uri
                builder.appendQueryParameter(PARAM_VERSION, sCachedVersionCode);
            } catch (NameNotFoundException e) {
                // Cannot find the package name, so don't add in the version parameter
                // This shouldn't happen.
                Log.wtf("Invalid package name for context " + e);
            }
        } else {
            builder.appendQueryParameter(PARAM_VERSION, sCachedVersionCode);
        }

        // Build the full uri and return it
        return builder.build();
    }

    public static long getTimeNow() {
        return SystemClock.elapsedRealtime();
    }

    /**
     * Calculate the amount by which the radius of a CircleTimerView should be offset by the any
     * of the extra painted objects.
     */
    public static float calculateRadiusOffset(float strokeSize, float diamondStrokeSize, float markerStrokeSize) {
        return Math.max(strokeSize, Math.max(diamondStrokeSize, markerStrokeSize));
    }

    /**
     * The pressed color used throughout the app. If this method is changed, it will not have
     * any effect on the button press states, and those must be changed separately.
     **/
    public static int getPressedColorId() {
        return R.color.clock_red;
    }

    /**
     * The un-pressed color used throughout the app. If this method is changed, it will not have
     * any effect on the button press states, and those must be changed separately.
     **/
    public static int getGrayColorId() {
        return R.color.clock_gray;
    }

    /** Setup to find out when the quarter-hour changes (e.g. Kathmandu is GMT+5:45) **/
    public static long getAlarmOnQuarterHour() {
        Calendar nextQuarter = Calendar.getInstance();
        // Set 1 second to ensure quarter-hour threshold passed.
        nextQuarter.set(Calendar.SECOND, 1);
        int minute = nextQuarter.get(Calendar.MINUTE);
        nextQuarter.add(Calendar.MINUTE, 15 - (minute % 15));
        long alarmOnQuarterHour = nextQuarter.getTimeInMillis();
        if (0 >= (alarmOnQuarterHour - System.currentTimeMillis()) || (alarmOnQuarterHour - System.currentTimeMillis()) > 901000) {
            Log.wtf("quarterly alarm calculation error");
        }
        return alarmOnQuarterHour;
    }


    public static void cancelAlarmOnQuarterHour(Context context, PendingIntent quarterlyIntent) {
        if (quarterlyIntent != null && context != null) {
            ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).cancel(
                    quarterlyIntent);
        }
    }


    /**
     * Setup alarm refresh when the quarter-hour changes *
     */
    public static PendingIntent startAlarmOnQuarterHour(Context context) {
        if (context != null) {
            PendingIntent quarterlyIntent = PendingIntent.getBroadcast(
                    context, 0, new Intent(Utils.ACTION_ON_QUARTER_HOUR), 0);
            ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).setRepeating(
                    AlarmManager.RTC, getAlarmOnQuarterHour(),
                    AlarmManager.INTERVAL_FIFTEEN_MINUTES, quarterlyIntent);
            return quarterlyIntent;
        } else {
            return null;
        }
    }

    public static PendingIntent refreshAlarmOnQuarterHour(
            Context context, PendingIntent quarterlyIntent) {
        cancelAlarmOnQuarterHour(context, quarterlyIntent);
        return startAlarmOnQuarterHour(context);
    }
    /**
     * For screensavers to set whether the digital or analog clock should be displayed.
     * Returns the view to be displayed.
     */
    public static View setClockStyle(Context context, View digitalClock, View analogClock, String clockStyleKey) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String defaultClockStyle = context.getResources().getString(R.string.default_clock_style);
        String style = sharedPref.getString(clockStyleKey, defaultClockStyle);
        View returnView;
        if (style.equals(CLOCK_TYPE_ANALOG)) {
            digitalClock.setVisibility(View.GONE);
            analogClock.setVisibility(View.VISIBLE);
            returnView = analogClock;
        } else {
            digitalClock.setVisibility(View.VISIBLE);
            analogClock.setVisibility(View.GONE);
            returnView = digitalClock;
        }

        TextView timeDisplayHours = (TextView)digitalClock.findViewById(R.id.timeDisplayHours);
        TextView timeDisplayMinutes = (TextView)digitalClock.findViewById(R.id.timeDisplayMinutes);
        TextView timeAmPm = (TextView)digitalClock.findViewById(R.id.am_pm);
        Utils.setTimeFont(timeDisplayHours, timeDisplayMinutes, timeAmPm);

        return returnView;
    }

    /**
     * For screensavers to dim the lights if necessary.
     */
    public static void dimClockView(boolean dim, View clockView) {
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setColorFilter(new PorterDuffColorFilter((dim ? 0x60FFFFFF : 0xC0FFFFFF), PorterDuff.Mode.MULTIPLY));
        clockView.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
    }
    
    
    /**
     * For screensavers to dim the lights if necessary.
     */
    public static void dimView(int dim, View view) {
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        dim=dim<<24;
        dim|=0x00FFFFFF;
        paint.setColorFilter(new PorterDuffColorFilter(dim, PorterDuff.Mode.MULTIPLY));
        view.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
    }

    /** Clock views can call this to refresh their alarm to the next upcoming value. **/
    @SuppressWarnings("deprecation")
    public static void refreshAlarm(Context context, View clock) {
        String nextAlarm = Settings.System.getString(context.getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED);
        TextView nextAlarmView;
        nextAlarmView = (TextView) clock.findViewById(R.id.nextAlarm);

        if (nextAlarmView != null) {
            if (!TextUtils.isEmpty(nextAlarm)) {
                nextAlarmView.setText(context.getString(R.string.control_set_alarm_with_existing, nextAlarm));
                nextAlarmView.setContentDescription(context.getResources().getString(R.string.next_alarm_description, nextAlarm));
                nextAlarmView.setVisibility(View.VISIBLE);
            } else nextAlarmView.setVisibility(View.GONE);
        }
    }

    /** Clock views can call this to refresh their date. **/
    public static void updateDate(String dateFormat, String dateFormatForAccessibility, View clock) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());

        CharSequence newDate = DateFormat.format(dateFormat, cal);
        TextView dateDisplay;
        dateDisplay = (TextView) clock.findViewById(R.id.date);
        if (dateDisplay != null) {
            dateDisplay.setVisibility(View.VISIBLE);
            dateDisplay.setText(newDate);
            dateDisplay.setContentDescription(DateFormat.format(dateFormatForAccessibility, cal));
        }
    }

    @SuppressWarnings("deprecation")
    public static void setAlarmTextView(Context context, TextView alarm) {
        String nextAlarm = Settings.System.getString(context.getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED);
        if (nextAlarm==null || nextAlarm.isEmpty()) {
            alarm.setVisibility(View.GONE);
        } else {
            alarm.setVisibility(View.VISIBLE);
            alarm.setText(nextAlarm);
        }
    }

    public static void setDateTextView(Context context, TextView dateView) {
        dateView.setText(new SimpleDateFormat(context.getString(R.string.abbrev_wday_month_day_no_year)).format(new Date()));
    }

    public static void setBatteryStatus(Context context, TextView batteryView) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);

        if (batteryStatus == null) {
            return;
        }

        // Are we charging / charged?
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;

        // How are we charging?
        int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
        boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = level / (float) scale*100;

        String text = "";
        if (status == BatteryManager.BATTERY_STATUS_FULL) {
            batteryView.setVisibility(View.GONE);
        } else {
            if (isCharging) {
                text += context.getString(R.string.battery_charging);
                if (usbCharge)
                    text += context.getString(R.string._usb_);
                if (acCharge)
                    text += context.getString(R.string._ac_);
                text += ", ";
            }
            text += (int)batteryPct + "%";
            batteryView.setText(text);
            batteryView.setVisibility(View.VISIBLE);
        }


    }

    public static Intent getAlarmPackage(Context context) {
        // Verify clock implementation
        String clockImpls[][] = {
                {"HTC Alarm Clock", "com.htc.android.worldclock",
                        "com.htc.android.worldclock.WorldClockTabControl" },
                {"Standar Alarm Clock", "com.android.deskclock",
                        "com.android.deskclock.AlarmClock"},
                {"Froyo Nexus Alarm Clock", "com.google.android.deskclock",
                        "com.android.deskclock.DeskClock"},
                {"Moto Blur Alarm Clock", "com.motorola.blur.alarmclock",
                        "com.motorola.blur.alarmclock.AlarmClock"},
                {"Samsung Galaxy Clock", "com.sec.android.app.clockpackage",
                        "com.sec.android.app.clockpackage.ClockPackage"} ,
                {"Sony Ericsson Xperia Z", "com.sonyericsson.organizer",
                        "com.sonyericsson.organizer.Organizer_WorldClock" },
                {"ASUS Tablets", "com.asus.deskclock",
                        "com.asus.deskclock.DeskClock"},
        };

        PackageManager manager = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);

        for (String[] clockImpl : clockImpls) { // int i = 0; i < clockImpls.length; i++) {

            ComponentName c = new ComponentName(clockImpl[1], clockImpl[2]);
            intent.setComponent(c);

            if (isCallable(intent, context))
                return intent;
        }

        return null;
    }

    public static boolean isCallable(Intent intent, Context context) {
        List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    /**
     * <p>Set the screen in immersive mode or low profile depending of the Android version.
     * Retry a second time 2 seconds later to fix an issue on Android 5.0 and 5.1.</p>
     */
    public static void hideSystemUiAndRetry(final View view) {
        hideSystemUI(view);

        new CountDownTimer(20000, 2000) {
            public void onTick(long millisUntilFinished) { hideSystemUI(view); }
            public void onFinish() {}
        }.start();
    }

    /**
     * <p>Set the screen in immersive mode or low profile depending of the Android version.</p>
     */
    public static void hideSystemUI(View view) {
        if(Build.VERSION.SDK_INT>=19){
            view.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }else{
            view.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }


    public float getSizeRatio(Context context) {
        float resizeRatio;

        String size = PreferenceManager.getDefaultSharedPreferences(context).getString(
                ScreensaverSettingsActivity.KEY_CLOCK_SIZE,
                ScreensaverSettingsActivity.SIZE_DEFAULT);

        switch (size) {
            case CLOCK_SIZE_MEDIUM:
                resizeRatio = (float)1.75;
                break;
            case CLOCK_SIZE_LARGE:
                resizeRatio = (float)2.5;
                break;
            case CLOCK_SIZE_XLARGE:
                resizeRatio = (float)5;
                break;
            case CLOCK_SIZE_2XLARGE:
                resizeRatio = (float)7.5;
                break;
            default:
                resizeRatio = 1;
        }

        return resizeRatio;
    }

    /**
     * <p>Resize the TextView fields to requested size.
     * There is no mechanism to prevent overflow off the screen.
     * Size can be small, medium or large.</p>
     */
    public static void resizeContent(ViewGroup parent) {
        float resizeRatio;
        int orientation = parent.getResources().getConfiguration().orientation;

        String size = PreferenceManager.getDefaultSharedPreferences(parent.getContext()).getString(
                ScreensaverSettingsActivity.KEY_CLOCK_SIZE,
                ScreensaverSettingsActivity.SIZE_DEFAULT);

        boolean landscapeBigger = PreferenceManager.getDefaultSharedPreferences(parent.getContext()).getBoolean(
                ScreensaverSettingsActivity.KEY_LANDSCAPE_BIGGER,
                ScreensaverSettingsActivity.KEY_LANDSCAPE_BIGGER_DEFAULT);

        switch (size) {
            case CLOCK_SIZE_MEDIUM:
                resizeRatio = (float)1.75;
                break;
            case CLOCK_SIZE_LARGE:
                resizeRatio = (float)2.25;
                break;
            case CLOCK_SIZE_XLARGE:
                resizeRatio = (float)4.5;
                break;
            case CLOCK_SIZE_2XLARGE:
                resizeRatio = (float)6.75;
                break;
            default:
                resizeRatio = 1;
        }

        if (orientation == Configuration.ORIENTATION_LANDSCAPE && landscapeBigger)
            resizeRatio *= 1.5;

        // First adjustment in size
        resizeContent(parent, resizeRatio);
    }

    /**
     * <p>Resize the TextView fields to requested size.
     * There is no mechanism to prevent overflow off the screen.</p>
     */
    public static void resizeContent(ViewGroup parent, float resizeRatio) {
        for (int i = parent.getChildCount() - 1; i >= 0; i--) {
            final View child = parent.getChildAt(i);
            if (child instanceof ViewGroup) {
                resizeContent((ViewGroup) child, resizeRatio);
                // DO SOMETHING WITH VIEWGROUP, AFTER CHILDREN HAS BEEN LOOPED
            } else {
                if (child != null) {
                    // DO SOMETHING WITH VIEW
                    if (child instanceof TextView)  {
                        float textSize;
                        float newTextSize;
                        int paddingLeft, paddingRight, paddingTop, paddingBottom;

                        TextView tv = ((TextView) child);

                        textSize = ((TextView) child).getTextSize();
                        newTextSize = textSize * resizeRatio;
                        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);

                        paddingLeft = (int) (resizeRatio * tv.getPaddingLeft());
                        paddingRight = (int) (resizeRatio * tv.getPaddingRight());
                        paddingTop = (int) (resizeRatio * tv.getPaddingTop());
                        paddingBottom = (int) (resizeRatio * tv.getPaddingBottom());

                        tv.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
                    }
                }
            }
        }
    }

    public static void setTimeFont(TextView timeDisplayHours, TextView timeDisplayMinutes, TextView timeDisplayAmPm) {
        Context context = timeDisplayHours.getContext();

        final Typeface robotoThin = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Thin.ttf");
        final Typeface robotoBold = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Bold.ttf");
        final Typeface robotoRegular = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Regular.ttf");

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String defaultClockStyle = context.getResources().getString(R.string.default_clock_style);
        String style = sharedPref.getString(ScreensaverSettingsActivity.KEY_CLOCK_STYLE, defaultClockStyle);

        if (style.equals(Utils.CLOCK_TYPE_DIGITAL))
            timeDisplayHours.setTypeface(robotoBold);
        else
            timeDisplayHours.setTypeface(robotoThin);

        timeDisplayMinutes.setTypeface(robotoThin);
        timeDisplayAmPm.setTypeface(robotoRegular);
    }

    public static void setBrightness(Window window, View saverView,
                                     ScreensaverMoveSaverRunnable moveSaverRunnable) {
        setBrightness(window, saverView, moveSaverRunnable, null);
    }


    /**
     * Will set the brightness settings
     * If called from daydream, it will set setScreenBright if needed
     * If the option for auto brightness is set it will initialise moveSaverRunnable
     * will full brightness and an adjustment ratio. The moveSaver will take care of dimming the
     * screen
     */
    public static void setBrightness(Window window, View saverView,
                                     ScreensaverMoveSaverRunnable moveSaverRunnable,
                                     DreamService dream) {
        boolean brightnessAuto = PreferenceManager.getDefaultSharedPreferences(saverView.getContext()).getBoolean(
                ScreensaverSettingsActivity.KEY_BRIGHTNESS_AUTO,
                ScreensaverSettingsActivity.KEY_BRIGHTNESS_AUTO_DEFAULT);

        int brightness, auto_brightness_adj;

        brightness = PreferenceManager.getDefaultSharedPreferences(saverView.getContext()).getInt(
                ScreensaverSettingsActivity.KEY_BRIGHTNESS,
                ScreensaverSettingsActivity.BRIGHTNESS_DEFAULT);
        auto_brightness_adj = PreferenceManager.getDefaultSharedPreferences(saverView.getContext()).getInt(
                ScreensaverSettingsActivity.KEY_BRIGHTNESS_AUTO_ADJ,
                ScreensaverSettingsActivity.KEY_BRIGHTNESS_AUTO_ADJ_DEFAULT);
        boolean useAutoBrightness = PreferenceManager.getDefaultSharedPreferences(saverView.getContext()).getBoolean(
                ScreensaverSettingsActivity.KEY_BRIGHTNESS_AUTO,
                ScreensaverSettingsActivity.KEY_BRIGHTNESS_AUTO_DEFAULT);


        boolean dim = brightness < SeekBarPreference.BRIGHTNESS_NIGHT;
        if (dim) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.buttonBrightness = 0;
            lp.screenBrightness = 0.01f;
            window.setAttributes(lp);
        }

        if (dream != null) {
            dream.setScreenBright(!dim);
        }

        if (moveSaverRunnable != null){

            if (useAutoBrightness)
                brightness = ScreensaverSettingsActivity.BRIGHTNESS_MAX;

            //TODO: with auto brightness, it should be dynamic!
            Utils.dimView(brightness, saverView);

            if (useAutoBrightness)
                moveSaverRunnable.setAutoBrightness(true, (float)auto_brightness_adj / 100);
        }
    }

    /**
     * Gets version code of given application.
     */
    public static int getVersionCode(Context context) {
        PackageInfo pinfo;
        try {
            pinfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pinfo.versionCode;
        } catch (NameNotFoundException e) {
            android.util.Log.e(context.getApplicationInfo().name, "Version code not available.");
        }
        return 0;
    }

    /**
     * Gets version name of given application.
     */
    public static String getVersionName(Context context) {
        PackageInfo pinfo;
        try {
            pinfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pinfo.versionName;
        } catch (NameNotFoundException e) {
            android.util.Log.e(context.getApplicationInfo().name, "Version name not available.");
        }
        return null;
    }

    public static List<NotificationInfo> getNotifications(StatusBarNotification[] notifs) {
        List<NotificationInfo> notifications = new ArrayList<>();
        for (StatusBarNotification notif : notifs) {

            try {
                if (notif.getNotification().priority > Notification.PRIORITY_MIN &&
                        (notif.getNotification().flags & Notification.FLAG_ONGOING_EVENT) == 0) {
                    notifications.add(new NotificationInfo(null, notif.getPackageName(), notif.getNotification()));
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            } catch (IconNotFoundException e) {
                e.printStackTrace();
            }

        }
        return notifications;
    }

    public static NotificationInfo getNotificationInfo(Context context, StatusBarNotification notif) {
        NotificationInfo notification = null;

        try {
            if (notif.getNotification().priority > Notification.PRIORITY_MIN &&
                    (notif.getNotification().flags & Notification.FLAG_ONGOING_EVENT) == 0) {
                notification = new NotificationInfo(context, notif.getPackageName(), notif.getNotification());
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (IconNotFoundException e) {
            e.printStackTrace();
        }

        return notification;
    }

    public static boolean isInterestingNotification(StatusBarNotification notif) {

        if (notif.getNotification().priority > Notification.PRIORITY_MIN &&
                (notif.getNotification().flags & Notification.FLAG_ONGOING_EVENT) == 0)
            return true;
        else
            return false;

    }
}
