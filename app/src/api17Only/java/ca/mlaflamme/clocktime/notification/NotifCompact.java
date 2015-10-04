package ca.mlaflamme.clocktime.notification;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.CallLog;
import android.view.View;
import ca.mlaflamme.clocktime.GmailContract;
import ca.mlaflamme.clocktime.Log;
import ca.mlaflamme.clocktime.R;

public class NotifCompact {
    private final Handler mHandler;

    public NotifCompact() {
        mHandler = new Handler();
    }

    public static final class LabelColumns {
        public static final String CANONICAL_NAME = "canonicalName";
        public static final String NAME = "name";
        public static final String NUM_CONVERSATIONS = "numConversations";
        public static final String NUM_UNREAD_CONVERSATIONS = "numUnreadConversations";
    }

    public NotificationInfo checkGmail(final Context context) {
        // Get the account list, and pick the first one
        final String ACCOUNT_TYPE_GOOGLE = "com.google";
        final String[] FEATURES_MAIL = {
                "service_mail"
        };
        AccountManagerFuture<Account[]> future = AccountManager.get(context).getAccountsByTypeAndFeatures(ACCOUNT_TYPE_GOOGLE, FEATURES_MAIL, null, null);

        Account[] accounts;
        try {
            accounts = future.getResult();
            if (accounts != null && accounts.length > 0) {
                NotificationInfo notif;
                for (Account account : accounts) {
                    String selectedAccount = account.name;
                    notif = queryLabels(selectedAccount, context);
                    if (notif != null)
                        return notif;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    // TODO: Doesn't seem to get my unread mails in my inbox
    private NotificationInfo queryLabels(String selectedAccount, Context context) {
        Log.d("Gmail - " + selectedAccount);

        NotificationInfo notificationInfo = null;

        Cursor labelsCursor = context.getContentResolver().query(GmailContract.Labels.getLabelsUri(selectedAccount), null, null, null, null);
        assert labelsCursor != null;
        labelsCursor.moveToFirst();
        do {
            String name = labelsCursor.getString(labelsCursor.getColumnIndex(GmailContract.Labels.CANONICAL_NAME));
            int unread = labelsCursor.getInt(labelsCursor.getColumnIndex(GmailContract.Labels.NUM_UNREAD_CONVERSATIONS));// here's the value you need
            if (name.equals(GmailContract.Labels.LabelCanonicalNames.CANONICAL_NAME_INBOX) && unread > 0) {
                Log.d("Gmail - " + name + "-" + unread);
                notificationInfo = new NotificationInfo(context, R.drawable.stat_notify_gmail);
                break;
            }
        } while (labelsCursor.moveToNext());

        labelsCursor.close();

        return notificationInfo;
    }


    private void setViewVisibility(final View view, final int visibility) {
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                view.setVisibility(visibility);
            }
        });
    }

    //: TODO: Find a way to get the SMS count from the time the clock started only.
    public NotificationInfo checkSMS(Context context) {
        Cursor cur = null;
        try {
            Uri uriSMSURI = Uri.parse("content://sms/inbox");
            cur = context.getContentResolver().query(uriSMSURI, null, "read = 0", null, null);
            assert cur != null;
            Log.d("SMS - " + cur.getCount());
            if (cur.getCount() > 0) {
                return new NotificationInfo(context, R.drawable.stat_notify_messages);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
        return null;

    }

    public NotificationInfo checkMissedCalls(Context context) {
        final String[] projection = null;
        final String selection = CallLog.Calls.TYPE + "=" + CallLog.Calls.MISSED_TYPE + " AND " + CallLog.Calls.IS_READ + "=0";
        final String[] selectionArgs = null;
        final String sortOrder = null;
        Cursor cursor = null;
        //noinspection TryFinallyCanBeTryWithResources
        try {
            cursor = context.getContentResolver().query(CallLog.Calls.CONTENT_URI, projection, selection, selectionArgs, sortOrder);
            if ((cursor != null ? cursor.getCount() : 0) > 0) {
                return new NotificationInfo(context, R.drawable.stat_notify_missed_call);
            }
        } catch (Exception ex) {
            Log.e("ERROR: " + ex.toString());
        } finally {
            assert cursor != null;
            cursor.close();
        }

        return null;
    }
}
