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
                ApplicationLoader.notificationThread.notifyThread();
                ApplicationLoader.notificationThread.createNotification(intent.getStringExtra("notification"), true);
            }
        });
    }
}
