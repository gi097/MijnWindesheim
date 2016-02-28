package com.giovanniterlingen.windesheim;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * A scheduler app for Windesheim students
 *
 * @author Giovanni Terlingen
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ApplicationLoader.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                ApplicationLoader.startPushService();
            }
        });
    }
}