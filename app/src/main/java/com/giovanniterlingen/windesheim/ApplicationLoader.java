/**
 * Copyright (c) 2019 Giovanni Terlingen
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
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;
import com.giovanniterlingen.windesheim.controllers.DatabaseController;
import com.giovanniterlingen.windesheim.controllers.NotificationController;

import java.util.concurrent.TimeUnit;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class ApplicationLoader extends Application {

    private static final IntentFilter intentFilter;
    public static volatile Context applicationContext;
    public static volatile Handler applicationHandler;
    private static volatile boolean applicationInited = false;
    private static NotificationThread notificationThread;

    static {
        intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_TIME_TICK);
        intentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        intentFilter.addAction(Intent.ACTION_TIME_CHANGED);
        intentFilter.addAction(Intent.ACTION_DATE_CHANGED);
    }

    public static void startPushService() {
        Intent intent = new Intent(applicationContext, NotificationService.class);
        if (Build.VERSION.SDK_INT >= 26) {
            applicationContext.startForegroundService(intent);
        } else {
            applicationContext.startService(intent);
        }
    }

    public static void postInitApplication() {
        if (applicationInited) {
            return;
        }
        applicationInited = true;

        if (DatabaseController.getInstance().hasSchedules()) {
            restartNotificationThread();
            startFetcher();
        }
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
                        (int) TimeUnit.DAYS.toSeconds(1) +
                                (int) TimeUnit.HOURS.toSeconds(1)))
                .setConstraints(Constraint.ON_UNMETERED_NETWORK)
                .setReplaceCurrent(false)
                .setRetryStrategy(RetryStrategy.DEFAULT_LINEAR)
                .build();
        dispatcher.mustSchedule(fetcherJob);
    }

    public static void restartNotificationThread() {
        if (notificationThread != null) {
            notificationThread.interrupt();
            NotificationController.getInstance().clearNotification();
        }
        notificationThread = new NotificationThread();
        notificationThread.start();
    }

    public static void notifyDateChanged() {
        if (notificationThread != null) {
            notificationThread.notifyDateChanged();
        }
    }

    public static void notifyMinuteChanged() {
        if (notificationThread != null) {
            notificationThread.notifyMinuteChanged();
        }
    }

    public static boolean isConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) ApplicationLoader
                .applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public static void runOnUIThread(Runnable runnable) {
        applicationHandler.post(runnable);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        applicationContext = getApplicationContext();
        applicationHandler = new Handler(applicationContext.getMainLooper());

        registerReceiver(new TimeReceiver(), intentFilter);

        NotificationController.getInstance().initNotificationChannels();
        startPushService();

        setDarkMode();
    }

    private void setDarkMode() {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(applicationContext);
        int currentNightMode = applicationContext.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean useDarkMode = preferences.getBoolean("dark_mode", currentNightMode == Configuration.UI_MODE_NIGHT_YES);
        if (useDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            return;
        }
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}
