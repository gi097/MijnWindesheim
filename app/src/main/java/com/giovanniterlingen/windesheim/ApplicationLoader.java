package com.giovanniterlingen.windesheim;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.preference.PreferenceManager;

/**
 * A scheduler app for Windesheim students
 *
 * @author Giovanni Terlingen
 */
public class ApplicationLoader extends Application {

    public static volatile Context applicationContext;
    public static volatile ScheduleDatabase scheduleDatabase;
    private static volatile boolean applicationInited = false;
    private static volatile Handler applicationHandler;
    private static volatile NotificationThread notificationThread;

    public static void startPushService() {

        applicationContext.startService(new Intent(applicationContext,
                NotificationService.class));

        if (android.os.Build.VERSION.SDK_INT >= 19) {
            // Calendar cal = Calendar.getInstance();
            // PendingIntent pintent =
            // PendingIntent.getService(applicationContext, 0, new
            // Intent(applicationContext, NotificationsService.class),
            // 0);
            // AlarmManager alarm = (AlarmManager)
            // applicationContext.getSystemService(Context.ALARM_SERVICE);
            // alarm.setRepeating(AlarmManager.RTC_WAKEUP,
            // cal.getTimeInMillis(), 30000, pintent);

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
            notificationThread.start();
        }
    }

    public static void runOnUIThread(Runnable runnable) {
        applicationHandler.post(runnable);
    }

    public static boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static void restartNotificationThread() {
        if (notificationThread != null) {
            notificationThread.setRunning(false);
            notificationThread = null;
        }
        notificationThread = new NotificationThread();
        notificationThread.start();
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