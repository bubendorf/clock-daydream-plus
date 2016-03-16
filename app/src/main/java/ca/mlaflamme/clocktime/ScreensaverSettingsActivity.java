/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.*;
import android.widget.Toast;

import static java.lang.System.arraycopy;

/**
 * Settings for the Alarm Clock Dream (cz.mpelant.ca.mlaflamme.clocktime.Screensaver).
 */
public class ScreensaverSettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {
    private final static String TAG = Utils.class.getName();
    static final String KEY_ALARM_COLOR = "screensaver_alarm_color";
    static final String KEY_ALARM_SIZE = "screensaver_alarm_size";
    static final String KEY_ALARM_FONT = "screensaver_alarm_font";
    static final String KEY_CLOCK_STYLE = "screensaver_clock_style";
    static final String KEY_CLOCK_SIZE = "screensaver_clock_size";
    static final String KEY_CLOCK_COLOR = "screensaver_clock_color";
    static final String KEY_DATE_COLOR = "screensaver_date_color";
    static final String KEY_DATE_SIZE = "screensaver_date_size";
    static final String KEY_DATE_FONT = "screensaver_date_font";
    static final String KEY_LANDSCAPE_BIGGER = "landscape_bigger";
    static final boolean KEY_LANDSCAPE_BIGGER_DEFAULT = false;
    //    static final String KEY_NIGHT_MODE = "screensaver_night_mode";
    static final String KEY_BRIGHTNESS_AUTO = "light_sensor";
    static final boolean KEY_BRIGHTNESS_AUTO_DEFAULT = false;
    static final String KEY_BRIGHTNESS = "brightness";
    static final int BRIGHTNESS_DEFAULT = 192;
    static final int BRIGHTNESS_MAX = 255;
    static final String KEY_BRIGHTNESS_MIN_ADJ = "min_brightness_adj";
    static final String KEY_BRIGHTNESS_AUTO_ADJ = "auto_brightness_adj";
    static final int KEY_BRIGHTNESS_MIN_ADJ_DEFAULT = 0;
    static final int KEY_BRIGHTNESS_AUTO_ADJ_DEFAULT = 100;
    static final String SIZE_DEFAULT = "medium";
    static final String KEY_NOTIF_LISTENER = "notif_listener";
    static final String KEY_NOTIF_GMAIL = "notif_gmail";
    static final String KEY_NOTIF_SMS = "notif_sms";
    static final String KEY_ORIENTATION = "orientation";
    static final String KEY_NOTIF_MISSED_CALLS = "notif_missed_calls";
    static final String KEY_HIDE_ACTIVITY = "hide_activity";
    static final String KEY_BATTERY = "battery";
    static final String KEY_SLIDE_EFFECT = "slide";
    static final boolean KEY_SLIDE_EFFECT_DEFAULT = false;
    static final String KEY_ABOUT = "about";
    static final long TIP_DELAY = 1000 * 3600 * 24; // 24h
    public static final int REQUEST_CODE_NOTIF = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new ScreensaverPreferenceFragment()).commit();

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (System.currentTimeMillis() - sp.getLong("tip", 0) > TIP_DELAY) {
            sp.edit().putLong("tip", System.currentTimeMillis()).apply();
//            Toast.makeText(this, R.string.tip_unread_gmail, Toast.LENGTH_LONG).show();
        }
    }

    public static class ScreensaverPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preferences_1);
            addPreferencesFromResource(R.xml.preferences_2_notif);
            addPreferencesFromResource(R.xml.preferences_3);

            initPreferences();
        }

        private void initPreferences() {
            setAutoBrightnessCheckbox();
            setAvailableSizes();
            setNotificationListener();
            setHideActivityListener();
            setVersion();
        }

        private void setAutoBrightnessCheckbox() {
            final Preference pref_auto_bright, pref_adjust_bright, pref_adjust_bright_auto, pref_adjust_min_bright;
            pref_auto_bright = findPreference(KEY_BRIGHTNESS_AUTO);
            pref_adjust_bright = findPreference(KEY_BRIGHTNESS);
            pref_adjust_bright_auto = findPreference(KEY_BRIGHTNESS_AUTO_ADJ);
            pref_adjust_min_bright = findPreference(KEY_BRIGHTNESS_MIN_ADJ);

            if (pref_auto_bright != null) {
                CheckBoxPreference check = (CheckBoxPreference) pref_auto_bright;
                PreferenceScreen screen = getPreferenceScreen();

                boolean mHasLightSensor =
                        getPreferenceScreen().getContext().getPackageManager().
                                hasSystemFeature(PackageManager.FEATURE_SENSOR_LIGHT);

                if (!mHasLightSensor) {
                    check.setChecked(false);
                    check.setEnabled(false);
                }


                if (check.isChecked()) {
                    screen.removePreference(pref_adjust_bright);
                }
                else {
                    screen.removePreference(pref_adjust_bright_auto);
                    screen.removePreference(pref_adjust_min_bright);
                }


                try {
                    pref_auto_bright.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            CheckBoxPreference check = (CheckBoxPreference) preference;
                            PreferenceScreen screen = getPreferenceScreen();

                            if (check.isChecked()) {
                                screen.removePreference(pref_adjust_bright);
                                screen.addPreference(pref_adjust_bright_auto);
                                screen.addPreference(pref_adjust_min_bright);
                            }
                            else {
                                screen.removePreference(pref_adjust_bright_auto);
                                screen.removePreference(pref_adjust_min_bright);
                                screen.addPreference(pref_adjust_bright);
                            }

                            return true;
                        }
                    });
                }catch (NoClassDefFoundError error){//weird SGSIII error - pref should be null because it is only in xml-v18
                    error.printStackTrace();
                }
            }
        }

        private void setAvailableSizes() {
            ListPreference listPref;
            CharSequence[] entries;
            CharSequence[] entryValues;
            int nbEntries;

            listPref = (ListPreference) findPreference(KEY_CLOCK_SIZE);
            entries = listPref.getEntries();
            entryValues = listPref.getEntryValues();

            int configLayout = getResources().getConfiguration().screenLayout;
            int screenSize = configLayout & Configuration.SCREENLAYOUT_SIZE_MASK;

            switch (screenSize) {
                case Configuration.SCREENLAYOUT_SIZE_SMALL:
                    nbEntries = 4;
                    break;
                case Configuration.SCREENLAYOUT_SIZE_NORMAL:
                    nbEntries = 5;
                    break;
                case Configuration.SCREENLAYOUT_SIZE_LARGE:
                    nbEntries = 6;
                    break;
                default:
                    nbEntries = 7;
            }

            CharSequence[] newEntries = new CharSequence[nbEntries];
            arraycopy(entries, 0, newEntries, 0, nbEntries);
            listPref.setEntries(newEntries);

            CharSequence[] newEntryValues = new CharSequence[nbEntries];
            arraycopy(entryValues, 0, newEntryValues, 0, nbEntries);
            listPref.setEntryValues(newEntryValues);
        }

        // TODO: this thing drains my battery!!! Changed a lot the NotificationListener... let's see...
        private void setNotificationListener() {
            Preference pref;
            pref = findPreference(KEY_NOTIF_LISTENER);
            if (pref != null) {
                try {
//                    ((CheckBoxPreference) pref).setChecked(NotificationListener.instance != null);
                    pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            Intent i = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                            startActivityForResult(i, REQUEST_CODE_NOTIF);
                            Toast.makeText(getActivity(), getString(R.string.enable_loc_listener_tip), Toast.LENGTH_LONG).show();
                            return true;
                        }
                    });
                }catch (NoClassDefFoundError error){//weird SGSIII error - pref should be null because it is only in xml-v18
                    error.printStackTrace();
                }
            }
        }

        private void setHideActivityListener() {
            Preference pref;
            pref = findPreference(KEY_HIDE_ACTIVITY);
            if (Build.VERSION.SDK_INT < 17) {
                pref.setEnabled(false);
                pref.setSelectable(false);
                pref.setSummary(R.string.action_not_available_in_this_android);
            } else {
                pref.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener) getActivity());
            }
        }

        private void setVersion() {
            Preference pref;
            pref = findPreference(KEY_ABOUT);

            String versionName = Utils.getVersionName(getActivity());
            int versionNumber = Utils.getVersionCode(getActivity());
            pref.setSummary("Version" + " " + versionName + " (" + String.valueOf(versionNumber) + ")");
        }
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        if (KEY_HIDE_ACTIVITY.equals(pref.getKey())) {
            int state = !((CheckBoxPreference) pref).isChecked() ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
            if (Build.VERSION.SDK_INT >= 17) {
                PackageManager p = getPackageManager();
                p.setComponentEnabledSetting(new ComponentName(ClockActivity.class.getPackage().getName(), ClockActivity.class.getName()), state, PackageManager.DONT_KILL_APP);
                Toast.makeText(this, R.string.restart_required, Toast.LENGTH_LONG).show();
            }

        }
        return true;
    }



}
