/**
 * Copyright (c) 2017 Giovanni Terlingen
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

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;
import com.giovanniterlingen.windesheim.SQLite.ScheduleDatabase;
import com.giovanniterlingen.windesheim.handlers.NotificationHandler;
import com.google.android.gms.ads.MobileAds;

import java.util.concurrent.TimeUnit;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class ApplicationLoader extends Application {

    public static ScheduleDatabase scheduleDatabase;
    public static NotificationHandler notificationHandler;

    public static volatile Context applicationContext;
    private static volatile Handler applicationHandler;
    private static volatile boolean notificationThreadInited = false;

    @Override
    public void onCreate() {
        super.onCreate();

        applicationContext = getApplicationContext();
        applicationHandler = new Handler(applicationContext.getMainLooper());
        scheduleDatabase = new ScheduleDatabase(applicationContext);
        scheduleDatabase.open();

        restartNotificationThread();
        startServices();

        MobileAds.initialize(getApplicationContext(), "ca-app-pub-3076066986942675~1680475744");

        NotificationHandler.initChannels();
    }

    public static void startServices() {
        if (scheduleDatabase.hasSchedules()) {
            startBackground(true);
            startBackground(false);
            startFetcher();
        }
    }

    private static void startBackground(boolean immediately) {
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher
                (new GooglePlayDriver(applicationContext));

        Job notificationJob = dispatcher.newJobBuilder()
                .setService(NotificationService.class)
                .setTag("backgroundJob-" + System.currentTimeMillis())
                .setRecurring(!immediately)
                .setLifetime(Lifetime.UNTIL_NEXT_BOOT)
                .setTrigger(immediately ? Trigger.NOW :
                        Trigger.executionWindow(50, 60))
                .setReplaceCurrent(false)
                .build();
        dispatcher.mustSchedule(notificationJob);
    }

    private static void startFetcher() {
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher
                (new GooglePlayDriver(applicationContext));

        Job fetcherJob = dispatcher.newJobBuilder()
                .setService(FetchService.class)
                .setTag("fetchJob")
                .setRecurring(true)
                .setLifetime(Lifetime.UNTIL_NEXT_BOOT)
                .setTrigger(Trigger.executionWindow((int) TimeUnit.DAYS.toSeconds(1),
                        (int) TimeUnit.DAYS.toSeconds(1) + (int) TimeUnit.HOURS.toSeconds(1)))
                .setConstraints(Constraint.ON_UNMETERED_NETWORK)
                .setReplaceCurrent(false)
                .setRetryStrategy(RetryStrategy.DEFAULT_LINEAR)
                .build();
        dispatcher.mustSchedule(fetcherJob);
    }

    public static void initNotificationThread() {
        if (notificationThreadInited) {
            return;
        }
        notificationThreadInited = true;
        restartNotificationThread();
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

    public static boolean isConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) ApplicationLoader
                .applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}