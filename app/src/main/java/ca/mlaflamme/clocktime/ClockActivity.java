
package ca.mlaflamme.clocktime;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

public class ClockActivity extends BaseScreenOnActivity {
    private final static String TAG = Utils.class.getName();
    private static final int SCREENSAVER_DELAY = 1000 * 30;
    private TextView mDate;
    private TextView mNextAlarm;
    private final Handler mHandler = new Handler();
    private final Runnable startScreenSaverRunnable = new Runnable() {

        @Override
        public void run() {
            startActivity(new Intent(ClockActivity.this, ScreensaverActivity.class));
            overridePendingTransition(R.anim.undim, R.anim.dim);
        }
    };
    private String mDateFormat;
    private String mDateFormatForAccessibility;
    

    private void setClockStyle() {
        String style = Utils.getClockStyle(this);
        Utils.setAnalogOrDigitalView(this.getWindow(), style, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDateFormat = getString(R.string.abbrev_wday_month_day_no_year);
        mDateFormatForAccessibility = getString(R.string.full_wday_month_day_no_year);
    }

    // TODO: It doesn't detect cable plugged in and out
    @Override
    public void onResume() {
        setClockStyle();

        View mainClockFrame = findViewById(R.id.main_clock_frame);

        Utils.resizeContent((ViewGroup) mainClockFrame);

        ((View)mainClockFrame.getParent().getParent()).setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                startScreenSaverRunnable.run();
                return true;
            }
        });
        mDate = (TextView) findViewById(R.id.date);
        mNextAlarm = (TextView) findViewById(R.id.nextAlarm);


        updateViews();


        mHandler.postDelayed(startScreenSaverRunnable, SCREENSAVER_DELAY);
        super.onResume();
    }

    @Override
    public void onPause() {
        mHandler.removeCallbacks(startScreenSaverRunnable);
        super.onPause();
    }
    
    @Override
    public void onUserInteraction() {
        mHandler.removeCallbacks(startScreenSaverRunnable);
        mHandler.postDelayed(startScreenSaverRunnable, SCREENSAVER_DELAY);
        super.onUserInteraction();
    }

    @Override
    protected void updateViews() {
        Utils.setAlarmTextView(this, mNextAlarm);
        Utils.setDateTextView(this, mDate, mDateFormat, mDateFormatForAccessibility);
    }

    // TODO: Menu icon? Should I refresh the style?
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item;

        // TODO: bring the alarm clock activity if possible
        final Intent alarmIntent = Utils.getAlarmPackage(this);
        if (alarmIntent != null) {
            item = menu.add("Alarms").setIcon(R.drawable.ic_action_alarm);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    startActivity(alarmIntent);
                    return true;
                }
            });
        }

        item = menu.add("Settings").setIcon(R.drawable.ic_action_settings);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                startActivity(new Intent(ClockActivity.this, ScreensaverSettingsActivity.class));
                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }


}
