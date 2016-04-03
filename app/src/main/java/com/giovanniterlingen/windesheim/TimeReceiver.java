package com.giovanniterlingen.windesheim;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * A schedule app for Windesheim students
 *
 * @author Giovanni Terlingen
 */
public class TimeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (ApplicationLoader.notificationThread != null) {
            ApplicationLoader.notificationThread.cancelAlarm();
            ApplicationLoader.notificationThread.notifyThread();
        }
    }
}