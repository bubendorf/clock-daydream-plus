package ca.mlaflamme.clocktime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

public class NotificationListener extends NotificationListenerService {
    private static final String TAG = NotificationListener.class.getClass().getSimpleName();
    public static final String ACTION_NLS_CONTROL = "ca.mlaflamme.clocktime.NOTIFICATION_LISTENER_SERVICE";
    public static final String ACTION_NLS_RESPONSE = "ca.mlaflamme.clocktime.NOTIFICATION_LISTENER";
    private NLServiceReceiver mNLServiceReceiver;
    private boolean mBroadcastNotifications = false;

    @Override
    public void onCreate() {
        super.onCreate();
        mNLServiceReceiver = new NLServiceReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_NLS_CONTROL);
        registerReceiver(mNLServiceReceiver, filter);
    }


    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        broadcastNotification(sbn, true);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        broadcastNotification(sbn, false);
    }

    private void broadcastNotification(StatusBarNotification sbn, boolean adding) {
        if (mBroadcastNotifications) {
            boolean sendit = Utils.isInterestingNotification(sbn);

            if (sendit) {
                Intent i = new Intent(ACTION_NLS_RESPONSE);
                i.putExtra(adding ? "notif_new" : "notif_removed", sbn);
                sendBroadcast(i);
            }
        }
    }

    private void broadcastExistentNotifications() {
        StatusBarNotification[] notifs = getActiveNotifications();
        for (StatusBarNotification notif : notifs)
            broadcastNotification(notif, true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mNLServiceReceiver);
    }

    private void turnOn() {
        mBroadcastNotifications = true;

        broadcastExistentNotifications();
    }

    private void turnOff() {
        mBroadcastNotifications = false;
    }

    private class NLServiceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action;
            if (intent != null && intent.getAction() != null) {
                action = intent.getAction();
                if (action.equals(ACTION_NLS_CONTROL)) {
                    String command = intent.getStringExtra("command");
                    if (TextUtils.equals(command, "start_broadcast_notif")) {
                        NotificationListener.this.turnOn();
                    } else if (TextUtils.equals(command, "stop_broadcast_notif")) {
                        NotificationListener.this.turnOff();
                    }
                }
            }
        }
    }
}
