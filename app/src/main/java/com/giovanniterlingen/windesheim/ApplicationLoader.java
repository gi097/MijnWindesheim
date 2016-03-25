package com.giovanniterlingen.windesheim;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;

/**
 * A schedule app for Windesheim students
 *
 * @author Giovanni Terlingen
 */
public class ApplicationLoader extends Application {

    public static volatile Context applicationContext;
    public static volatile ScheduleDatabase scheduleDatabase;
    public static volatile NotificationThread notificationThread;
    private static volatile boolean applicationInited = false;
    private static volatile Handler applicationHandler;

    public static void startPushService() {

        applicationContext.startService(new Intent(applicationContext,
                NotificationService.class));

        if (android.os.Build.VERSION.SDK_INT >= 19) {
            PendingIntent pintent = PendingIntent.getService(
                    applicationContext, 0, new Intent(applicationContext,
                            NotificationService.class), 0);
            AlarmManager alarm = (AlarmManager) applicationContext
                    .getSystemService(Context.ALARM_SERVICE);
            alarm.cancel(pintent);
        }
    }

    public static void postInitApplication() {
        if (applicationInited) {
            return;
        }
        applicationInited = true;

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        String classId = preferences.getString("componentId", "");
        if (classId.length() != 0) {
            notificationThread = new NotificationThread();
            notificationThread.startNotifications();
        }
    }

    public static void runOnUIThread(Runnable runnable) {
        applicationHandler.post(runnable);
    }

    public static void restartNotificationThread() {
        if (notificationThread != null) {
            notificationThread.stopRunning();
            notificationThread = null;
        }
        notificationThread = new NotificationThread();
        notificationThread.startNotifications();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        applicationContext = getApplicationContext();
        applicationHandler = new Handler(applicationContext.getMainLooper());
        scheduleDatabase = new ScheduleDatabase(applicationContext);
        scheduleDatabase.open();

        startPushService();
    }
}