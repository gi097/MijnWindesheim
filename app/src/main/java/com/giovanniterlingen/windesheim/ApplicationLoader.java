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
import android.os.Handler;

import com.giovanniterlingen.windesheim.SQLite.ScheduleDatabase;
import com.giovanniterlingen.windesheim.handlers.DailyScheduleHandler;
import com.giovanniterlingen.windesheim.handlers.NotificationHandler;
import com.google.android.gms.ads.MobileAds;

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

        if (scheduleDatabase.hasSchedules()) {
            notificationHandler = new NotificationHandler();
            notificationHandler.start();
            dailyScheduleHandler = new DailyScheduleHandler();
            dailyScheduleHandler.start();
        }
    }

    public static void runOnUIThread(Runnable runnable) {
        applicationHandler.post(runnable);
    }

    public static void restartNotificationThread() {
        if (notificationHandler != null) {
            notificationHandler.clearNotification();
            notificationHandler.interrupt();
            notificationHandler.stopRunning();
            notificationHandler = null;
        }
        notificationHandler = new NotificationHandler();
        notificationHandler.start();
    }

    public static void restartDailyScheduleFetcher() {
        if (dailyScheduleHandler != null) {
            dailyScheduleHandler.stopRunning();
            dailyScheduleHandler = null;
        }
        dailyScheduleHandler = new DailyScheduleHandler();
        dailyScheduleHandler.start();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        applicationContext = getApplicationContext();
        applicationHandler = new Handler(applicationContext.getMainLooper());
        scheduleDatabase = new ScheduleDatabase(applicationContext);
        scheduleDatabase.open();

        MobileAds.initialize(getApplicationContext(), "ca-app-pub-3076066986942675~1680475744");

        startPushService();
    }
}