package com.giovanniterlingen.windesheim;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * A schedule app for Windesheim students
 *
 * @author Giovanni Terlingen
 */
public class NotificationReceiver extends BroadcastReceiver {

    public void onReceive(Context context, final Intent intent) {
        ApplicationLoader.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                String notification = intent.getStringExtra("notification");
                ApplicationLoader.notificationThread.createNotification(notification, true);
                ApplicationLoader.notificationThread.notifyThread();
            }
        });
    }
}
