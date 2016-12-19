/**
 * Copyright (c) 2016 Giovanni Terlingen
 * <p/>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 **/
package com.giovanniterlingen.windesheim;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;

import com.giovanniterlingen.windesheim.SQLite.ScheduleDatabase;
import com.giovanniterlingen.windesheim.handlers.DailyScheduleHandler;
import com.giovanniterlingen.windesheim.handlers.NotificationHandler;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class ApplicationLoader extends Application {

    public static ScheduleDatabase scheduleDatabase;
    public static NotificationHandler notificationHandler;
    public static DailyScheduleHandler dailyScheduleHandler;
    public static volatile Context applicationContext;
    private static volatile Handler applicationHandler;
    private static volatile boolean applicationInited = false;

    /**
     * We want to start a background service in order to get notifications, also when the app is
     * in the background.
     */
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

    /**
     * Is called first when the app starts, it will start a NotificationHandler in order to show
     * notifications.
     */
    public static void postInitApplication() {
        if (applicationInited) {
            return;
        }
        applicationInited = true;

        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(applicationContext);
        String classId = preferences.getString("componentId", "" );
        if (classId.length() != 0) {
            notificationHandler = new NotificationHandler();
            notificationHandler.start();
            dailyScheduleHandler = new DailyScheduleHandler();
            dailyScheduleHandler.start();
        }
    }

    /**
     * A useful class to parse a Runnable to the main thread and run them.
     *
     * @param runnable The Runnable to run on the main thread.
     */
    public static void runOnUIThread(Runnable runnable) {
        applicationHandler.post(runnable);
    }

    /**
     * Closes an already running NotificationHandler and starts a new one.
     */
    public static void restartNotificationThread() {
        if (notificationHandler != null) {
            notificationHandler.stopRunning();
            notificationHandler = null;
        }
        notificationHandler = new NotificationHandler();
        notificationHandler.start();
    }

    /**
     * Closes an already running ScheduleChangeHandlerThread and starts a new one.
     */
    public static void restartScheduleChangeHandlerThread() {
        if (dailyScheduleHandler != null) {
            dailyScheduleHandler.stopRunning();
            dailyScheduleHandler = null;
        }
        dailyScheduleHandler = new DailyScheduleHandler();
        dailyScheduleHandler.start();
    }

    /**
     * Let's assign the variables, open databases and start the service.
     */
    @Override
    public void onCreate() {
        super.onCreate();

        applicationContext = getApplicationContext();
        applicationHandler = new Handler(applicationContext.getMainLooper());
        scheduleDatabase = new ScheduleDatabase(applicationContext);
        scheduleDatabase.open();

        NetworkReceiver.update(false);

        startPushService();
    }
}