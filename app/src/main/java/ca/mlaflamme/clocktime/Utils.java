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

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.service.dreams.DreamService;
import android.service.notification.StatusBarNotification;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
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


    public static final String CLOCK_SIZE_TINY = "tiny";
    public static final String CLOCK_SIZE_SMALLER = "smaller";
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
    public static void setClockStyle(Context context, View digitalClock, View analogClock, String clockStyleKey) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String defaultClockStyle = context.getResources().getString(R.string.default_clock_style);
        String style = sharedPref.getString(clockStyleKey, defaultClockStyle);

        if (style.equals(CLOCK_TYPE_ANALOG)) {
            digitalClock.setVisibility(View.GONE);
            analogClock.setVisibility(View.VISIBLE);
        } else {
            digitalClock.setVisibility(View.VISIBLE);
            analogClock.setVisibility(View.GONE);
        }

        TextView timeDisplayHours = (TextView)digitalClock.findViewById(R.id.timeDisplayHours);
        TextView timeDisplayMinutes = (TextView)digitalClock.findViewById(R.id.timeDisplayMinutes);
        TextView timeAmPm = (TextView)digitalClock.findViewById(R.id.am_pm);
        Utils.setTimeFont(timeDisplayHours, timeDisplayMinutes, timeAmPm, style);

    }

    public static String getClockStyle(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String defaultClockStyle = context.getResources().getString(R.string.default_clock_style);

        return sharedPref.getString("screensaver_clock_style", defaultClockStyle);
    }

    public static  void setAnalogOrDigitalView(Window window, String style, boolean mainClockFrameOnly) {
        if (style.equals(CLOCK_TYPE_ANALOG)) {
            if (mainClockFrameOnly)
                window.setContentView(R.layout.main_clock_frame_analog);
            else
                window.setContentView(R.layout.desk_clock_saver_analog);
        } else {
            if (mainClockFrameOnly)
                window.setContentView(R.layout.main_clock_frame_digital);
            else
                window.setContentView(R.layout.desk_clock_saver_digital);

            TextView timeDisplayHours = (TextView)window.findViewById(R.id.timeDisplayHours);
            TextView timeDisplayMinutes = (TextView)window.findViewById(R.id.timeDisplayMinutes);
            TextView timeAmPm = (TextView)window.findViewById(R.id.am_pm);
            Utils.setTimeFont(timeDisplayHours, timeDisplayMinutes, timeAmPm, style);
        }
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

    /** Clock views can call this to refresh their date. **/
    public static void updateDate(String dateFormat, String dateFormatForAccessibility, View clock) {
//        Calendar cal = Calendar.getInstance();
//        cal.setTimeInMillis(System.currentTimeMillis());
//
//        CharSequence newDate = DateFormat.format(dateFormat, cal);
//        TextView dateDisplay;
//        dateDisplay = (TextView) clock.findViewById(R.id.date);
//        if (dateDisplay != null) {
//            dateDisplay.setVisibility(View.VISIBLE);
//            dateDisplay.setText(newDate);
//            dateDisplay.setContentDescription(DateFormat.format(dateFormatForAccessibility, cal));
//        }
    }

    public static void setBackground(Context context, ImageView backgroundView, String imagePath){

        if(backgroundView != null && backgroundView.getDrawable() != null) {
            BitmapDrawable bd = (BitmapDrawable) backgroundView.getDrawable();
            bd.getBitmap().recycle();
            backgroundView.setImageBitmap(null);
        }

        if (!imagePath.isEmpty()){
            try{
                int brightness = PreferenceManager.getDefaultSharedPreferences(context).getInt(
                        ScreensaverSettingsActivity.KEY_BACKGROUND_BRIGHTNESS,
                        ScreensaverSettingsActivity.BACKGROUND_BRIGHTNESS_DEFAULT);

                backgroundView.setAlpha((float)brightness/100);
                // Todo: load image only once in memory instead of reloading it allways from imagePathUri
                backgroundView.setImageURI(Uri.parse(new File(imagePath).toString()));
            }catch (Exception e){
                Log.e("Cannot set background image",e);
            }
        }
    }


    public static String getImagePath(Context context, Uri uri){
        String path = "";
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(uri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static Intent createImagePickerIntent(){
        Intent sIntent = new Intent(Intent.ACTION_GET_CONTENT);
        sIntent.addCategory(Intent.CATEGORY_OPENABLE);

        // special intent for Samsung file manager
        sIntent.setType("image/*");
        sIntent.setAction(Intent.ACTION_GET_CONTENT);

        return Intent.createChooser(sIntent,"Select Picture");
    }


    public static void setWakeupView(Context context, ImageView view, String imagePath){
        float alpha = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AlarmManager alarmManager =( AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            AlarmManager.AlarmClockInfo alarmClockInfo = alarmManager.getNextAlarmClock();

            if(alarmClockInfo!=null){
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
                int wakeupStartTime = pref.getInt(ScreensaverSettingsActivity.KEY_WAKEUP_START_TIME, 30) * 60;
                int wakeupFullBrightnessTime = pref.getInt(ScreensaverSettingsActivity.KEY_WAKEUP_FULL_BRIGHTNESS_TIME, 5) * 60;

                alpha = getWakeupAlpha(alarmClockInfo.getTriggerTime(), wakeupStartTime, wakeupFullBrightnessTime);
            }
        }
        else{
            //TODO: Calculate milliseconds with old ALARM content provider to support not updated androids
            Log.w("Wakup animation is supported from SDK version 21 aka Lollipop");
        }

        if (!imagePath.isEmpty()){

            try{
                view.setAlpha(alpha);
                view.setImageURI(android.net.Uri.parse(imagePath));
            }catch (Exception e){
                Log.e("Cannot set background image",e);
            }
        }
    }

    public static float getWakeupAlpha( long alarmTimeWallTime, int startInSeconds, int fullAlphaInSeconds ){
        float alpha = 0;
        Date currentTime = new Date();
        long delta = alarmTimeWallTime - currentTime.getTime();
        long deltaSeconds = delta/1000;

        if(deltaSeconds < startInSeconds ){
            if(deltaSeconds < fullAlphaInSeconds){
                alpha = 1;
            }
            else {
                alpha = 1 - (float) (deltaSeconds - fullAlphaInSeconds) / (startInSeconds - fullAlphaInSeconds);
            }
        }

        return alpha;
    }

    public static void setAlarmTextView(Context context, TextView alarm) {
        String nextAlarm = Settings.System.getString(context.getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED);
        long nextAlarmTime = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AlarmManager alarmManager =( AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            AlarmManager.AlarmClockInfo alarmClockInfo = alarmManager.getNextAlarmClock();

            if(alarmClockInfo!=null){
                nextAlarmTime = alarmClockInfo.getTriggerTime();
            }
        }

        long currentTime = new Date().getTime();
        long timeUntilShowUp = PreferenceManager.getDefaultSharedPreferences(context).getInt(ScreensaverSettingsActivity.KEY_ALARM_HIDE_UNTIL_HOURS, 14) * 60 * 60;
        long delta = (nextAlarmTime - currentTime )/1000;

        if(nextAlarm == null || nextAlarm.isEmpty() || delta > timeUntilShowUp && timeUntilShowUp != 0){
            alarm.setVisibility(View.GONE);
        } else {
            alarm.setVisibility(View.VISIBLE);
            alarm.setText(nextAlarm);

            int color = getColorFromPreference(context, ScreensaverSettingsActivity.KEY_ALARM_COLOR, R.string.default_alarm_color);
            alarm.setTextColor(color);


            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            color = Color.argb(0xff, Color.red(color), Color.green(color), Color.blue(color));
            paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
            paint.setAlpha(0xff);
            paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
            alarm.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
        }
    }

    public static void setDateTextView(Context context, TextView dateView,
                                       String dateFormat, String dateFormatForAccessibility) {
        Date date = new Date();

        dateView.setText(new SimpleDateFormat(dateFormat).format(date));
        dateView.setContentDescription(new SimpleDateFormat(dateFormatForAccessibility).format(date));

        int color = getColorFromPreference(context, ScreensaverSettingsActivity.KEY_DATE_COLOR, R.string.default_date_color);
        dateView.setTextColor(color);
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
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = level / (float) scale*100;

        String text = "";
        if (status == BatteryManager.BATTERY_STATUS_FULL) {
            batteryView.setVisibility(View.GONE);
        } else {
            if (isCharging) {
                text += context.getString(R.string.battery_charging);
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


    public static float getSizeRatio(Context context, String key, String def_value) {
        float resizeRatio;

        String size = PreferenceManager.getDefaultSharedPreferences(context).getString(key, def_value);

        switch (size) {
            case CLOCK_SIZE_TINY:
                resizeRatio = (float)1;
                break;
            case CLOCK_SIZE_SMALLER:
                resizeRatio = (float)1.25;
                break;
            case CLOCK_SIZE_SMALL:
                resizeRatio = (float)1.50;
                break;
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

        boolean landscapeBigger = PreferenceManager.getDefaultSharedPreferences(parent.getContext()).getBoolean(
                ScreensaverSettingsActivity.KEY_LANDSCAPE_BIGGER,
                ScreensaverSettingsActivity.KEY_LANDSCAPE_BIGGER_DEFAULT);

        resizeRatio = getSizeRatio(parent.getContext(), ScreensaverSettingsActivity.KEY_CLOCK_SIZE, ScreensaverSettingsActivity.SIZE_DEFAULT);

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

    public static void resizeTextView(TextView mTextView, String key, String default_value) {

        float ratio = getSizeRatio(mTextView.getContext(), key, default_value);

        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextView.getTextSize() * ratio);
    }

    public static void setTextViewFont(TextView mTextView, String key, String defaultValue){

        String font = PreferenceManager.getDefaultSharedPreferences(mTextView.getContext()).getString(key, defaultValue);

        mTextView.setTypeface(Utils.getTypeface(mTextView.getContext(), font));

    }
   /* public static void setTimeFont(TextView timeDisplayHours, TextView timeDisplayMinutes, TextView timeDisplayAmPm) {
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

        int color = getColorFromPreference(context, ScreensaverSettingsActivity.KEY_CLOCK_COLOR, R.string.default_clock_color);

        timeDisplayHours.setTextColor(color);
        timeDisplayMinutes.setTextColor(color);
        timeDisplayAmPm.setTextColor(color);
    }*/

    public static void setTimeFont(TextView timeDisplayHours, TextView timeDisplayMinutes, TextView timeDisplayAmPm, String style) {
        Context context = timeDisplayHours.getContext();

        if (style.equals(Utils.CLOCK_TYPE_DIGITAL))
            timeDisplayHours.setTypeface(getTypeface(context, "fonts/Roboto-Bold.ttf") );
        else
            timeDisplayHours.setTypeface(getTypeface(context, "fonts/Roboto-Thin.ttf") );

        timeDisplayMinutes.setTypeface(getTypeface(context, "fonts/Roboto-Thin.ttf"));
        timeDisplayAmPm.setTypeface(getTypeface(context, "fonts/Roboto-Regular.ttf"));

        int color = getColorFromPreference(context, ScreensaverSettingsActivity.KEY_CLOCK_COLOR, R.string.default_clock_color );

        timeDisplayHours.setTextColor(color);
        timeDisplayMinutes.setTextColor(color);
        timeDisplayAmPm.setTextColor(color);
    }

    public static Typeface getTypeface(Context context, String key) {
        Typeface font;

        try {
            font = Typeface.createFromAsset(context.getAssets(), key);
        }catch(RuntimeException e){
            font = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Regular.ttf");

            android.util.Log.w(TAG, "getTypeface: asset " + key + " not found, default: \"fonts/Roboto-Regular.ttf\" used");
        }

        return font;
    }

    private static int getColorFromPreference(Context context, String key, int defaultValue) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String value = pref.getString(key, context.getResources().getString(defaultValue) );

        int colorId = context.getResources().getIdentifier(value, "color", context.getPackageName());
        return context.getResources().getColor(colorId);
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

        int brightness = PreferenceManager.getDefaultSharedPreferences(saverView.getContext()).getInt(
                ScreensaverSettingsActivity.KEY_BRIGHTNESS,
                ScreensaverSettingsActivity.BRIGHTNESS_DEFAULT);
        int auto_brightness_adj = PreferenceManager.getDefaultSharedPreferences(saverView.getContext()).getInt(
                ScreensaverSettingsActivity.KEY_BRIGHTNESS_AUTO_ADJ,
                ScreensaverSettingsActivity.KEY_BRIGHTNESS_AUTO_ADJ_DEFAULT);
        int min_brightness_adj = PreferenceManager.getDefaultSharedPreferences(saverView.getContext()).getInt(
                ScreensaverSettingsActivity.KEY_BRIGHTNESS_MIN_ADJ,
                ScreensaverSettingsActivity.KEY_BRIGHTNESS_MIN_ADJ_DEFAULT);
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
//                moveSaverRunnable.setAutoBrightness(true, (float)auto_brightness_adj / 100, (float)min_brightness_adj / 100);
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

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static List<NotificationInfo> getNotifications(StatusBarNotification[] notifs) {
        List<NotificationInfo> notifications = new ArrayList<>();
        for (StatusBarNotification notif : notifs) {

            try {
                if (notif.getNotification().priority > Notification.PRIORITY_MIN &&
                        (notif.getNotification().flags & Notification.FLAG_ONGOING_EVENT) == 0) {
                    notifications.add(new NotificationInfo(null, notif.getPackageName(), notif.getNotification()));
                }
            } catch (NameNotFoundException | IconNotFoundException e) {
                e.printStackTrace();
            }

        }
        return notifications;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static NotificationInfo getNotificationInfo(Context context, StatusBarNotification notif) {
        NotificationInfo notification = null;

        try {
            if (notif.getNotification().priority > Notification.PRIORITY_MIN &&
                    (notif.getNotification().flags & Notification.FLAG_ONGOING_EVENT) == 0) {
                notification = new NotificationInfo(context, notif.getPackageName(), notif.getNotification());
            }
        } catch (NameNotFoundException | IconNotFoundException e) {
            e.printStackTrace();
        }

        return notification;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static boolean isInterestingNotification(StatusBarNotification notif) {

        return notif.getNotification().priority > Notification.PRIORITY_MIN &&
                (notif.getNotification().flags & Notification.FLAG_ONGOING_EVENT) == 0;

    }
}
