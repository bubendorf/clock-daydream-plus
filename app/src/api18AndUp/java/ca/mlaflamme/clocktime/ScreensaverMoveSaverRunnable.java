
package ca.mlaflamme.clocktime;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.service.notification.StatusBarNotification;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import ca.mlaflamme.clocktime.notification.NotificationInfo;
import ca.mlaflamme.clocktime.notification.NotificationLayout;

import java.util.List;

/**
 * Runnable for use with screensaver and dream, to move the clock every minute.
 * registerViews() must be called prior to posting.
 */
public class ScreensaverMoveSaverRunnable implements Runnable, SensorEventListener {
    static final long MOVE_DELAY = 60000; // DeskClock.SCREEN_SAVER_MOVE_DELAY;
    static final long SLIDE_TIME = 10000;
    static final long FADE_TIME = 3000;
    static final float MAX_SPACE_RATIO = 0.8f; //Safety measure to resize the content in case
                                                // the content couldn't move.
    static final float SHRINKING_RATIO = 0.85f; // Percentage of the original to shrink and
                                                // expand before and after moving.

    static boolean mSlideEffect = true;

    private View mContentView, mSaverView;
    private TextView mDate;
    private TextView mBattery;
    private NotificationLayout mNotifLayout;
    private View mTest;
    private TextView mNextAlarm;
    private final Handler mHandler;

    private static TimeInterpolator mSlowStartWithBrakes;
    private static float mSizeRatio;
    private float mNextAlpha;
    private float mLastAlpha = 0;
    private SensorManager mSensorManager;
    private NotificationReceiver mReceiver;
    private Sensor mLight;
    private boolean mInitSensor;

    private boolean mUseAutoBrightness = false;
    private float mAdjustBrightness;

    public ScreensaverMoveSaverRunnable(Handler handler) {
        mHandler = handler;
        mInitSensor = false;
        mSlowStartWithBrakes = new TimeInterpolator() {
            @Override
            public float getInterpolation(float x) {
                return (float) (Math.cos((Math.pow(x, 3) + 1) * Math.PI) / 2.0f) + 0.5f;
            }
        };
        mNextAlpha = (float) ScreensaverSettingsActivity.BRIGHTNESS_DEFAULT /
                ScreensaverSettingsActivity.BRIGHTNESS_MAX;
    }

    public void setSlideEffect(boolean useSlideEffect) {
        mSlideEffect = useSlideEffect;
    }

    public void setAutoBrightness(boolean useAutoBrightness, float adjFactor) {
        mUseAutoBrightness = useAutoBrightness;
        mAdjustBrightness = adjFactor;
    }

    public void registerViews(View contentView, View saverView) {
        mContentView = contentView;
        mDate = (TextView) contentView.findViewById(R.id.date);
        mBattery = (TextView) contentView.findViewById(R.id.battery);
        mNotifLayout = (NotificationLayout) contentView.findViewById(R.id.notifLayout);
        mNextAlarm = (TextView) contentView.findViewById(R.id.nextAlarm);
        mSaverView = saverView;
        mSensorManager = (SensorManager) mContentView.getContext().getSystemService(Context.SENSOR_SERVICE);
        mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        initNotificationReceiver();
        handleUpdate();
        startReceivingNotif();
    }

    private void initNotificationReceiver() {
        mReceiver = new NotificationReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("ca.mlaflamme.clocktime.NOTIFICATION_LISTENER");
        mContentView.getContext().registerReceiver(mReceiver, filter);
    }

    public void unregister() {
        stopReceivingNotif();
        mSensorManager.unregisterListener(this);
        mContentView.getContext().unregisterReceiver(mReceiver);
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT)
            handleLightSensorChanges(event.values);
    }

    // TODO: slow down the frequency after 1h
    // TODO: accellerate frequency 1h before wakeup alarm
    private void handleLightSensorChanges(float[] values) {
        float luxLight = values[0];
        int currentBrightness = 255;
        // Do something with this sensor data.

        if (luxLight <= SensorManager.LIGHT_NO_MOON) {
            mNextAlpha = 0.05f;
        } else if (luxLight <= SensorManager.LIGHT_FULLMOON) {
            mNextAlpha = 0.10f;
        } else if (luxLight <= 2) {
            mNextAlpha = 0.15f;
        } else if (luxLight <= 4) {
            mNextAlpha = 0.2f;
        } else if (luxLight <= 6) {
            mNextAlpha = 0.25f;
        } else if (luxLight <= 10) {
            mNextAlpha = 0.5f;
        } else if (luxLight <= SensorManager.LIGHT_CLOUDY) {
            mNextAlpha = 0.6f;
        } else if (luxLight <= SensorManager.LIGHT_SUNRISE) {
            mNextAlpha = 0.7f;
        } else if (luxLight <= SensorManager.LIGHT_OVERCAST) {
            mNextAlpha = 0.8f;
        } else if (luxLight <= SensorManager.LIGHT_SHADE) {
            mNextAlpha = 0.9f;
        } else if (luxLight <= SensorManager.LIGHT_SUNLIGHT) {
            mNextAlpha = 1;
        } else if (luxLight <= SensorManager.LIGHT_SUNLIGHT_MAX) {
            mNextAlpha = 1;
        }

        mNextAlpha *= mAdjustBrightness;

        Log.v("onSensorChanged -> luxLight: " + luxLight + " mNextAlpha: " + mNextAlpha);
    }


    @Override
    public void run() {
        long delay = MOVE_DELAY;
        if (mContentView == null || mSaverView == null) {
            mHandler.removeCallbacks(this);
            mHandler.postDelayed(this, delay);
            return;
        }

        if (mUseAutoBrightness && !mInitSensor) {
            mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL);
            mInitSensor = true;
        }


        final float xrange = mContentView.getWidth() - mSaverView.getWidth();
        final float yrange = mContentView.getHeight() - mSaverView.getHeight();
        Log.v("xrange: " + xrange + " yrange: " + yrange + " mNextAlpha: " + mNextAlpha);

        if (xrange == 0 && yrange == 0) {
            delay = 500; // back in a split second
        } else {
            final int nextx = (int) (Math.random() * xrange);
            final int nexty = (int) (Math.random() * yrange);

            AnimatorSet s = new AnimatorSet();

            if (mSaverView.getAlpha() == 0f) {
                // jump right there
                mSaverView.setX(nextx);
                mSaverView.setY(nexty);

                schickIfTooBig(s);
                Animator appear = ObjectAnimator.ofFloat(mSaverView, "alpha", 0f, mNextAlpha).setDuration(FADE_TIME);
                s.play(appear);
            } else {

                Animator xMove = ObjectAnimator.ofFloat(mSaverView, "x", mSaverView.getX(), nextx);
                Animator yMove = ObjectAnimator.ofFloat(mSaverView, "y", mSaverView.getY(), nexty);

                Animator xShrink = ObjectAnimator.ofFloat(mSaverView, "scaleX", mSizeRatio, mSizeRatio * SHRINKING_RATIO);
                Animator xGrow = ObjectAnimator.ofFloat(mSaverView, "scaleX", mSizeRatio * SHRINKING_RATIO, mSizeRatio);

                Animator yShrink = ObjectAnimator.ofFloat(mSaverView, "scaleY", mSizeRatio, mSizeRatio * SHRINKING_RATIO);
                Animator yGrow = ObjectAnimator.ofFloat(mSaverView, "scaleY", mSizeRatio * SHRINKING_RATIO, mSizeRatio);

                AnimatorSet shrink = new AnimatorSet();
                shrink.play(xShrink).with(yShrink);
                AnimatorSet grow = new AnimatorSet();
                grow.play(xGrow).with(yGrow);

                Animator fadeout = ObjectAnimator.ofFloat(mSaverView, "alpha", mLastAlpha, 0f);
                Animator fadein = ObjectAnimator.ofFloat(mSaverView, "alpha", 0f, mNextAlpha);
                Animator adjbrigh = ObjectAnimator.ofFloat(mSaverView, "alpha", mLastAlpha, mNextAlpha);


                if (mSlideEffect) {
                    s.play(xMove).with(yMove).with(adjbrigh);
                    s.setDuration(SLIDE_TIME);

                    s.play(shrink.setDuration(SLIDE_TIME / 2));
                    s.play(grow.setDuration(SLIDE_TIME / 2)).after(shrink);
                    s.setInterpolator(mSlowStartWithBrakes);
                } else {
                    AccelerateInterpolator accel = new AccelerateInterpolator();
                    DecelerateInterpolator decel = new DecelerateInterpolator();

                    shrink.setDuration(FADE_TIME).setInterpolator(accel);
                    fadeout.setDuration(FADE_TIME).setInterpolator(accel);
                    grow.setDuration(FADE_TIME).setInterpolator(decel);
                    fadein.setDuration(FADE_TIME).setInterpolator(decel);
                    s.play(shrink);
                    s.play(fadeout);
                    s.play(xMove.setDuration(0)).after(FADE_TIME);
                    s.play(yMove.setDuration(0)).after(FADE_TIME);
                    s.play(fadein).after(FADE_TIME);
                    s.play(grow).after(FADE_TIME);
                }
            }
            s.start();

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    handleUpdate();
                }
            }, FADE_TIME);

            mLastAlpha = mNextAlpha;

            long now = System.currentTimeMillis();
            long adjust = (now % MOVE_DELAY);
            delay = delay + (MOVE_DELAY - adjust) // minute aligned
                    - (mSlideEffect ? 0 : FADE_TIME) // start moving before the fade
            ;
        }

        mHandler.removeCallbacks(this);
        mHandler.postDelayed(this, delay);
    }

    private void initialSizing() {
        float xSpaceRatio;      // requested ratio between mSaverView and mContentView weight
        float ySpaceRatio;      // requested ratio between mSaverView and mContentView height
        float maxBetweenXAndY;

        xSpaceRatio = (float) mSaverView.getWidth() * mSizeRatio / mContentView.getWidth();
        ySpaceRatio = (float) mSaverView.getHeight() * mSizeRatio / mContentView.getHeight();
        maxBetweenXAndY = xSpaceRatio > ySpaceRatio ? xSpaceRatio : ySpaceRatio;

        if (maxBetweenXAndY > MAX_SPACE_RATIO) {
            if (xSpaceRatio > ySpaceRatio)
                mSizeRatio = MAX_SPACE_RATIO * mContentView.getWidth() / mSaverView.getWidth();
            else
                mSizeRatio = MAX_SPACE_RATIO * mContentView.getHeight() / mSaverView.getHeight();
        }

        Animator xShrink = ObjectAnimator.ofFloat(mSaverView, "scaleX", 1f, mSizeRatio);
        Animator yShrink = ObjectAnimator.ofFloat(mSaverView, "scaleY", 1f, mSizeRatio);
        AnimatorSet resize = new AnimatorSet();
        resize.play(xShrink).with(yShrink);
    }

    private void schickIfTooBig(AnimatorSet s) {
        final float xRatio = (float) mSaverView.getWidth() / mContentView.getWidth();
        final float yRatio = (float) mSaverView.getHeight() / mContentView.getHeight();
        final float biggerRatio = xRatio > yRatio ? xRatio : yRatio;

        if (biggerRatio > MAX_SPACE_RATIO) {
            mSizeRatio = MAX_SPACE_RATIO / biggerRatio;

            Animator xShrink = ObjectAnimator.ofFloat(mSaverView, "scaleX", 1, mSizeRatio);
            Animator yShrink = ObjectAnimator.ofFloat(mSaverView, "scaleY", 1, mSizeRatio);

            s.play(xShrink).with(yShrink);
        }
        else
            mSizeRatio = 1;


    }



    private void handleUpdate() {
        try {
            Utils.setAlarmTextView(mDate.getContext(), mNextAlarm);
            Utils.setDateTextView(mDate.getContext(), mDate);

            if (isPrefEnabled(ScreensaverSettingsActivity.KEY_BATTERY, true)) {
                mBattery.setVisibility(View.VISIBLE);
                Utils.setBatteryStatus(mDate.getContext(), mBattery);
            } else {
                mBattery.setVisibility(View.GONE);
            }

            //notifChange();



        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void startReceivingNotif() {
        Intent i = new Intent("ca.mlaflamme.clocktime.NOTIFICATION_LISTENER_SERVICE");
        i.putExtra("command", "start_broadcast_notif");
        mContentView.getContext().sendBroadcast(i);
    }

    private void stopReceivingNotif() {
        Intent i = new Intent("ca.mlaflamme.clocktime.NOTIFICATION_LISTENER_SERVICE");
        i.putExtra("command", "stop_broadcast_notif");
        mContentView.getContext().sendBroadcast(i);
    }

/*
    private void notifChange() {
        if (NotificationListener.instance != null) {
            Log.d("notif listener is running");
            List<NotificationInfo> notifications = NotificationListener.instance.getNotifications();
            Log.d("got " + notifications.size() + " icons");
            mNotifLayout.clear();
            for (NotificationInfo notificationInfo : notifications) {
                mNotifLayout.addNotification(notificationInfo);
            }
            mNotifLayout.notifyDatasetChanged();
        }
    }
*/
    public boolean isPrefEnabled(String prefName, boolean defValue) {
        return PreferenceManager.getDefaultSharedPreferences(mDate.getContext()).getBoolean(prefName, defValue);
    }

    class NotificationReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.hasExtra("notif_new")) {
                StatusBarNotification sbn = intent.getParcelableExtra("notif_new");
                NotificationInfo notifInfo = Utils.getNotificationInfo(mContentView.getContext(), sbn);
                if (notifInfo != null) {
                    mNotifLayout.addNotification(notifInfo);
                    mNotifLayout.notifyDatasetChanged();
                }
            }
        }
    }
}