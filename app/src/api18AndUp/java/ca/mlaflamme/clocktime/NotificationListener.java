package ca.mlaflamme.clocktime;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Parcelable;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.content.LocalBroadcastManager;

import ca.mlaflamme.clocktime.notification.IconNotFoundException;
import ca.mlaflamme.clocktime.notification.NotificationInfo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * NotificationListener.java
 *
 * @author eMan s.r.o.
 * @project clock-daydream-plus
 * @package ca.mlaflamme.clocktime
 * @since 9/1/13
 */

public class NotificationListener extends NotificationListenerService {
    // TODO: I think the bad stuff is here!!!
    //public static NotificationListener instance;


    private String TAG = this.getClass().getSimpleName();
    private NLServiceReceiver mNLServiceReceiver;
    private boolean mBroadcastNotifications = false;

    @Override
    public void onCreate() {
//        super.onCreate();
//        instance = this;




        super.onCreate();
        mNLServiceReceiver = new NLServiceReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("ca.mlaflamme.clocktime.NOTIFICATION_LISTENER_SERVICE");
        registerReceiver(mNLServiceReceiver, filter);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (mBroadcastNotifications) {
            boolean sendit = Utils.isInterestingNotification(sbn);

            if (sendit) {
                Intent i = new Intent("ca.mlaflamme.clocktime.NOTIFICATION_LISTENER");
                i.putExtra("notif_new", (Parcelable) sbn);
                sendBroadcast(i);
            }
        }
    }


    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {}

    public List<NotificationInfo> getNotifications() {
        List<NotificationInfo> notifications = new ArrayList<>();
        StatusBarNotification[] notifs = getActiveNotifications();
        for (StatusBarNotification notif : notifs) {

            try {
                if (notif.getNotification().priority > Notification.PRIORITY_MIN &&
                        (notif.getNotification().flags & Notification.FLAG_ONGOING_EVENT) == 0) {
                    notifications.add(new NotificationInfo(this, notif.getPackageName(), notif.getNotification()));
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            } catch (IconNotFoundException e) {
                e.printStackTrace();
            }

        }
        return notifications;
    }

    @Override
    public void onDestroy() {
//        super.onDestroy();
//        instance = null;


        unregisterReceiver(mNLServiceReceiver);
    }

    class NLServiceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getStringExtra("command").equals("start_broadcast_notif")){
                mBroadcastNotifications = true;

//                Intent i = new Intent("ca.mlaflamme.clocktime.NOTIFICATION_LISTENER");
//                // TODO: Okay this is too much data to bind :(
//                i.putExtra("notification_event", (Parcelable[]) NotificationListener.this.getActiveNotifications());
//                //i.putExtra("regis", (String) "Patate poil maudit cave!!!");
//                sendBroadcast(i);

//                Intent i = new Intent("ca.mlaflamme.clocktime.NOTIFICATION_LISTENER");
//
//                for (StatusBarNotification sbn : getActiveNotifications()) {
//                    i.putExtra("notification_event", (Parcelable) sbn);
//                    //i.putExtra("regis", (String) "Patate poil maudit cave!!!");
//                    sendBroadcast(i);
//                }
            } else if(intent.getStringExtra("command").equals("stop_broadcast_notif")) {
                mBroadcastNotifications = false;
            }
        }
    }
}
