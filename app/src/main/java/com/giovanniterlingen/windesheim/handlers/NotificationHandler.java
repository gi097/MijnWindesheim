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
package com.giovanniterlingen.windesheim.handlers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.text.format.DateUtils;

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.R;
import com.giovanniterlingen.windesheim.objects.Lesson;
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
    private Date date;

    private static final String PERSISTENT_NOTIFICATION_ID = "com.giovanniterlingen.windesheim.notification.persistent";
    private static final String PUSH_NOTIFICATION_ID = "com.giovanniterlingen.windesheim.notification.push";

    private static final int NOTIFICATION_1_HOUR = 2;
    private static final int NOTIFICATION_30_MIN = 3;
    private static final int NOTIFICATION_15_MIN = 4;
    public static final int NOTIFICATION_ALWAYS_ON = 5;
    public static final int NOTIFICATION_OFF = 6;

    @Override
    public void run() {
        mNotificationManager = (NotificationManager) ApplicationLoader.applicationContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
        date = new Date();
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(ApplicationLoader.applicationContext);
        notificationType = preferences.getInt("notifications_type", 0);
        DateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);
        String notificationText = "";
        long currentTimeMillis;
        while (isRunning() && notificationType != 0 && notificationType != NOTIFICATION_OFF) {
            try {
                date = new Date();
                if (!ApplicationLoader.scheduleDatabase.isFetched(date)) {
                    ScheduleHandler.getAndSaveAllSchedules(date, false);
                }
                Lesson[] lessons = ApplicationLoader.scheduleDatabase.getLessons(simpleDateFormat.format(date));
                if (lessons == null || lessons.length == 0) {
                    clearNotification();
                    while (checkIfNeedsContinue()) {
                        sleep(1000);
                    }
                } else {
                    for (int i = 0; i < lessons.length; i++) {
                        if (!checkIfNeedsContinue()) {
                            break;
                        }
                        Lesson lesson = lessons[i];
                        String subjectTimeString = lesson.getStartTime();
                        String[] subjectTimes = subjectTimeString.split(":");
                        Calendar subjectCalendar = Calendar.getInstance();
                        subjectCalendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(subjectTimes[0]));
                        subjectCalendar.set(Calendar.MINUTE, Integer.parseInt(subjectTimes[1]));
                        long subjectTime = subjectCalendar.getTimeInMillis();
                        boolean multiple = i + 1 < lessons.length
                                && subjectTimeString.equals(lessons[i + 1].getStartTime());
                        String lessonName = lesson.getSubject();
                        String lessonLocation = lesson.getRoom();
                        while ((currentTimeMillis = System.currentTimeMillis()) < subjectTime
                                && checkIfNeedsContinue()) {
                            long difference = subjectTime - currentTimeMillis;
                            long diffMinutes = (difference / 60000) % 60;
                            long diffHours = (difference / 3600000) % 24;
                            if (diffHours >= 1) {
                                if (diffMinutes != 0) {
                                    if (diffHours == 1) {
                                        if (diffMinutes == 1) {
                                            if (multiple) {
                                                notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.multiple_lessons_one_hour_one_minute);
                                            } else {
                                                notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.lesson_one_hour_one_minute, lessonName, lessonLocation);
                                            }
                                        } else {
                                            if (multiple) {
                                                notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.multiple_lessons_one_hour_multiple_minutes, diffMinutes);
                                            } else {
                                                notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.lesson_one_hour_multiple_minutes, lessonName, diffMinutes, lessonLocation);
                                            }
                                        }
                                    } else {
                                        if (diffMinutes == 1) {
                                            if (multiple) {
                                                notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.multiple_lessons_multiple_hours_one_minute, diffHours);
                                            } else {
                                                notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.lesson_multiple_hours_one_minute, lessonName, diffHours, lessonLocation);
                                            }
                                        } else {
                                            if (multiple) {
                                                notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.multiple_lessons_multiple_hours_multiple_minutes, diffHours, diffMinutes);
                                            } else {
                                                notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.lesson_multiple_hours_multiple_minutes, lessonName, diffHours, diffMinutes, lessonLocation);
                                            }
                                        }
                                    }
                                } else {
                                    if (diffHours == 1) {
                                        if (multiple) {
                                            notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.multiple_lessons_one_hour);
                                        } else {
                                            notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.lesson_one_hour, lessonName, lessonLocation);
                                        }
                                    } else {
                                        if (multiple) {
                                            notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.multiple_lessons_multiple_hours, diffHours);
                                        } else {
                                            notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.lesson_multiple_hours, lessonName, diffHours, lessonLocation);
                                        }
                                    }
                                }
                            } else {
                                if (diffMinutes >= 1) {
                                    if (diffMinutes == 1) {
                                        if (multiple) {
                                            notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.multiple_lessons_one_minute);
                                        } else {
                                            notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.lesson_one_minute, lessonName, lessonLocation);
                                        }
                                    } else {
                                        if (multiple) {
                                            notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.multiple_lessons_multiple_minutes, diffMinutes);
                                        } else {
                                            notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.lesson_multiple_minutes, lessonName, diffMinutes, lessonLocation);
                                        }
                                    }
                                }
                            }
                            if (notificationType == NOTIFICATION_ALWAYS_ON) {
                                createNotification(notificationText, true, false);
                            }
                            if (diffHours == 1 && diffMinutes == 0 && notificationType == NOTIFICATION_1_HOUR ||
                                    diffHours == 0 && diffMinutes == 30 && notificationType == NOTIFICATION_30_MIN ||
                                    diffHours == 0 && diffMinutes == 15 && notificationType == NOTIFICATION_15_MIN) {
                                createNotification(notificationText, false, true);
                            }
                            sleep(1000);
                        }
                    }
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

    private boolean isRunning() {
        return running;
    }

    public void stopRunning() {
        running = false;
    }

    public static void initChannels() {
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            NotificationChannel pushChannel = new NotificationChannel(PUSH_NOTIFICATION_ID,
                    ApplicationLoader.applicationContext.getResources()
                            .getString(R.string.push_notification),
                    NotificationManager.IMPORTANCE_HIGH);
            pushChannel.setDescription(ApplicationLoader.applicationContext.getResources()
                    .getString(R.string.push_notification_description));
            pushChannel.enableLights(true);
            pushChannel.enableVibration(true);
            pushChannel.setShowBadge(true);
            pushChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationChannel persistentChannel = new NotificationChannel(PERSISTENT_NOTIFICATION_ID,
                    ApplicationLoader.applicationContext.getResources()
                            .getString(R.string.persistent_notification),
                    NotificationManager.IMPORTANCE_MIN);
            pushChannel.setDescription(ApplicationLoader.applicationContext.getResources()
                    .getString(R.string.persistent_notification_description));
            persistentChannel.enableLights(false);
            persistentChannel.enableVibration(false);
            persistentChannel.setShowBadge(false);
            persistentChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager mManager = (NotificationManager) ApplicationLoader
                    .applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
            mManager.createNotificationChannel(pushChannel);
            mManager.createNotificationChannel(persistentChannel);
        }
    }

    static void createScheduleChangedNotification() {
        NotificationManager notificationManager = (NotificationManager) ApplicationLoader.applicationContext
                .getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(ApplicationLoader.applicationContext,
                ScheduleActivity.class);
        intent.putExtra("notification", true);
        PendingIntent pendingIntent = PendingIntent
                .getActivity(ApplicationLoader.applicationContext,
                        (int) System.currentTimeMillis(),
                        intent, 0);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                ApplicationLoader.applicationContext, PUSH_NOTIFICATION_ID)
                .setContentTitle(ApplicationLoader.applicationContext.getResources()
                        .getString(R.string.app_name))
                .setContentText(ApplicationLoader.applicationContext.getResources().getString(R.string.schedule_changed))
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.notifybar)
                .setOngoing(false)
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(ApplicationLoader.applicationContext.getResources().getString(R.string.schedule_changed)))
                .setColor(ContextCompat.getColor(ApplicationLoader.applicationContext,
                        R.color.colorPrimary))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL);

        notificationManager.notify(1, mBuilder.build());
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

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                    ApplicationLoader.applicationContext, headsUp ? PUSH_NOTIFICATION_ID : PERSISTENT_NOTIFICATION_ID)
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
                mBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
                mBuilder.setDefaults(Notification.DEFAULT_ALL);
            } else if (onGoing) {
                mBuilder.setPriority(NotificationCompat.PRIORITY_MIN);
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
        return (isRunning() && System.currentTimeMillis() >= date.getTime()
                && DateUtils.isToday(date.getTime()));
    }
}