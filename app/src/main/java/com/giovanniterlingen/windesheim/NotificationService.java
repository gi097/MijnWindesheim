package com.giovanniterlingen.windesheim;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * A schedule app for Windesheim students
 *
 * @author Giovanni Terlingen
 */
public class NotificationService extends Service {

    @Override
    public void onCreate() {
        ApplicationLoader.postInitApplication();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Intent intent = new Intent("com.giovanniterlingen.windesheim.start");
        sendBroadcast(intent);
    }
}