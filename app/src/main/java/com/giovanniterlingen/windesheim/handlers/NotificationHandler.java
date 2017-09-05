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
package com.giovanniterlingen.windesheim.handlers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.text.format.DateUtils;

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.R;
import com.giovanniterlingen.windesheim.ui.ScheduleActivity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class NotificationHandler extends Thread {

    private String lastNotification = "";
    private volatile boolean running = true;
    private NotificationManager mNotificationManager;
    private int notificationType;
    private Calendar calendar;

    private static final String PERSISTENT_NOTIFICATION_ID = "com.giovanniterlingen.windesheim.notification.persistent";
    private static final String NORMAL_NOTIFICATION_ID = "com.giovanniterlingen.windesheim.notification.normal";

    @Override
    public void run() {
        mNotificationManager = (NotificationManager) ApplicationLoader.applicationContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
        calendar = Calendar.getInstance();
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(ApplicationLoader.applicationContext);
        notificationType = preferences.getInt("notifications_type", 0);
        DateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);
        String notificationText = "";
        long currentTimeMillis;
        while (isRunning() && notificationType != 0 && notificationType != 6) {
            try {
                calendar = Calendar.getInstance();
                Date date = calendar.getTime();
                if (!ApplicationLoader.scheduleDatabase.isFetched(date)) {
                    ScheduleHandler.getAndSaveAllSchedules(date);
                }
                Cursor cursor = ApplicationLoader.scheduleDatabase
                        .getLessons(simpleDateFormat.format(date));
                Cursor cursor1 = ApplicationLoader.scheduleDatabase
                        .getLessons(simpleDateFormat.format(date));
                if (cursor != null && cursor.getCount() == 0) {
                    clearNotification();
                    while (checkIfNeedsContinue()) {
                        sleep(1000);
                    }
                } else {
                    while (cursor != null && cursor.moveToNext() && checkIfNeedsContinue()) {
                        String subjectTimeString = cursor.getString(3);
                        String[] subjectTimes = cursor.getString(3).split(":");
                        Calendar subjectCalendar = Calendar.getInstance();
                        subjectCalendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(subjectTimes[0]));
                        subjectCalendar.set(Calendar.MINUTE, Integer.parseInt(subjectTimes[1]));
                        long subjectTime = subjectCalendar.getTimeInMillis();
                        if (cursor1.moveToFirst() && cursor.getPosition() + 1 < cursor1.getCount()
                                && cursor1.moveToPosition(cursor.getPosition() + 1)
                                && cursor1.getString(3) != null
                                && subjectTimeString.equals(cursor1.getString(3))) {
                            while ((currentTimeMillis = System.currentTimeMillis()) < subjectTime
                                    && checkIfNeedsContinue()) {
                                long difference = subjectTime - currentTimeMillis;
                                long diffMinutes = (difference / 60000) % 60;
                                long diffHours = (difference / 3600000) % 24;
                                if (diffHours >= 1) {
                                    if (diffMinutes != 0) {
                                        if (diffHours == 1) {
                                            if (diffMinutes == 1) {
                                                notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.multiple_lessons_one_hour_one_minute);
                                            } else {
                                                notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.multiple_lessons_one_hour_multiple_minutes, diffMinutes);
                                            }
                                        } else {
                                            if (diffMinutes == 1) {
                                                notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.multiple_lessons_multiple_hours_one_minute, diffHours);
                                            } else {
                                                notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.multiple_lessons_multiple_hours_multiple_minutes, diffHours, diffMinutes);
                                            }
                                        }
                                    } else {
                                        if (diffHours == 1) {
                                            notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.multiple_lessons_one_hour);
                                        } else {
                                            notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.multiple_lessons_multiple_hours, diffHours);
                                        }
                                    }
                                } else {
                                    if (diffMinutes >= 1) {
                                        if (diffMinutes == 1) {
                                            notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.multiple_lessons_one_minute);
                                        } else {
                                            notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.multiple_lessons_multiple_minutes, diffMinutes);
                                        }
                                    }
                                }
                                if (notificationType == 5) {
                                    createNotification(notificationText, true, false);
                                }
                                if (diffHours == 1 && diffMinutes == 0 && notificationType == 2 || diffHours == 0 && diffMinutes == 30 && notificationType == 3 || diffHours == 0 && diffMinutes == 15 && notificationType == 4) {
                                    createNotification(notificationText, false, true);
                                }
                                sleep(1000);
                            }
                        } else {
                            String lessonName = cursor.getString(5);
                            String lessonLocation = cursor.getString(6);
                            while ((currentTimeMillis = System.currentTimeMillis()) < subjectTime && checkIfNeedsContinue()) {
                                long difference = subjectTime - currentTimeMillis;
                                long diffMinutes = (difference / 60000) % 60;
                                long diffHours = (difference / 3600000) % 24;
                                if (diffHours >= 1) {
                                    if (diffMinutes != 0) {
                                        if (diffHours == 1) {
                                            if (diffMinutes == 1) {
                                                notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.lesson_one_hour_one_minute, lessonName, lessonLocation);
                                            } else {
                                                notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.lesson_one_hour_multiple_minutes, lessonName, diffMinutes, lessonLocation);
                                            }
                                        } else {
                                            if (diffMinutes == 1) {
                                                notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.lesson_multiple_hours_one_minute, lessonName, diffHours, lessonLocation);
                                            } else {
                                                notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.lesson_multiple_hours_multiple_minutes, lessonName, diffHours, diffMinutes, lessonLocation);
                                            }
                                        }
                                    } else {
                                        if (diffHours == 1) {
                                            notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.lesson_one_hour, lessonName, lessonLocation);
                                        } else {
                                            notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.lesson_multiple_hours, lessonName, diffHours, lessonLocation);
                                        }
                                    }
                                } else {
                                    if (diffMinutes >= 1) {
                                        if (diffMinutes == 1) {
                                            notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.lesson_one_minute, lessonName, lessonLocation);
                                        } else {
                                            notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.lesson_multiple_minutes, lessonName, diffMinutes, lessonLocation);
                                        }
                                    }
                                }
                                if (notificationType == 5) {
                                    createNotification(notificationText, true, false);
                                }
                                if (diffHours == 1 && diffMinutes == 0 && notificationType == 2 || diffHours == 0 && diffMinutes == 30 && notificationType == 3 || diffHours == 0 && diffMinutes == 15 && notificationType == 4) {
                                    createNotification(notificationText, false, true);
                                }
                                sleep(1000);
                            }
                        }
                    }
                }
                if (cursor != null) {
                    cursor.close();
                }
                if (cursor1 != null) {
                    cursor1.close();
                }
                clearNotification();
                while (checkIfNeedsContinue()) {
                    sleep(1000);
                }
            } catch (InterruptedException e) {
                //
            } catch (Exception e) {
                createNotification(ApplicationLoader.applicationContext
                        .getResources().getString(R.string.connection_problem), false, false);
                stopRunning();
            }
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void stopRunning() {
        running = false;
    }

    private void createNotification(String notificationText, boolean onGoing, boolean headsUp) {
        if (notificationType != 0 && notificationType != 6 && mNotificationManager != null) {
            if (lastNotification.equals(notificationText)) {
                return;
            }
            lastNotification = notificationText;

            Intent intent = new Intent(ApplicationLoader.applicationContext,
                    ScheduleActivity.class);
            intent.putExtra("notification", true);
            PendingIntent pendingIntent = PendingIntent
                    .getActivity(ApplicationLoader.applicationContext,
                            (int) System.currentTimeMillis(),
                            intent, 0);

            // create Android O channel
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                // normal notifications
                NotificationChannel normalChannel = new NotificationChannel(NORMAL_NOTIFICATION_ID,
                        ApplicationLoader.applicationContext.getResources()
                                .getString(R.string.normal_notification),
                        NotificationManager.IMPORTANCE_HIGH);
                normalChannel.enableLights(true);
                normalChannel.enableVibration(true);
                normalChannel.setLightColor(Color.YELLOW);
                normalChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

                // persistent notification, does not need headsup
                NotificationChannel persistentChannel = new NotificationChannel(PERSISTENT_NOTIFICATION_ID,
                        ApplicationLoader.applicationContext.getResources()
                                .getString(R.string.persistent_notification),
                        NotificationManager.IMPORTANCE_LOW);
                persistentChannel.enableLights(false);
                persistentChannel.enableVibration(false);
                persistentChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

                NotificationManager mManager = (NotificationManager) ApplicationLoader
                        .applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
                mManager.createNotificationChannel(normalChannel);
                mManager.createNotificationChannel(persistentChannel);
            }

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                    ApplicationLoader.applicationContext, headsUp ? NORMAL_NOTIFICATION_ID : PERSISTENT_NOTIFICATION_ID)
                    .setContentTitle(ApplicationLoader.applicationContext.getResources()
                            .getString(R.string.app_name))
                    .setContentText(notificationText)
                    .setContentIntent(pendingIntent)
                    .setSmallIcon(R.drawable.notifybar)
                    .setOngoing(onGoing)
                    .setAutoCancel(!onGoing)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(notificationText))
                    .setColor(ContextCompat.getColor(ApplicationLoader.applicationContext,
                            R.color.colorPrimary));
            if (headsUp) {
                if (android.os.Build.VERSION.SDK_INT >= 16) {
                    mBuilder.setPriority(NotificationManagerCompat.IMPORTANCE_HIGH);
                }
                mBuilder.setDefaults(Notification.DEFAULT_ALL);
            }
            mNotificationManager.notify(0, mBuilder.build());
        } else {
            clearNotification();
        }
    }

    public void clearNotification() {
        lastNotification = "";
        if (mNotificationManager != null) {
            mNotificationManager.cancel(0);
        }
    }

    private boolean checkIfNeedsContinue() {
        return (isRunning() && System.currentTimeMillis() >= calendar.getTimeInMillis()
                && DateUtils.isToday(calendar.getTimeInMillis()));
    }
}
