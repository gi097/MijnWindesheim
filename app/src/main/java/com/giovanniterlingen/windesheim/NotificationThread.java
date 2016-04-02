package com.giovanniterlingen.windesheim;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * A schedule app for Windesheim students
 *
 * @author Giovanni Terlingen
 */
public class NotificationThread extends Thread {

    private volatile boolean running = true;

    private NotificationManager mNotificationManager;
    private int notificationType;

    @Override
    public void run() {
        mNotificationManager = (NotificationManager) ApplicationLoader.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ApplicationLoader.applicationContext);
        notificationType = preferences.getInt("notifications_type", 0);
        String componentId = preferences.getString("componentId", "");
        int type = preferences.getInt("type", 0);
        DateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String notificationText = "";
        while (isRunning() && componentId.length() > 0 && type != 0 && notificationType != 0 && notificationType != 6) {
            try {
                Calendar calendar = Calendar.getInstance();
                Date date = calendar.getTime();
                Cursor cursor = ApplicationLoader.scheduleDatabase.getLessons(simpleDateFormat.format(date), componentId);
                Cursor cursor1 = ApplicationLoader.scheduleDatabase.getLessons(simpleDateFormat.format(date), componentId);
                while (cursor != null && cursor.moveToNext()) {
                    String subjectTimeString = cursor.getString(3);
                    String[] subjectTimes = subjectTimeString.split(":");
                    Calendar subjectCalendar = Calendar.getInstance();
                    subjectCalendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(subjectTimes[0]));
                    subjectCalendar.set(Calendar.MINUTE, Integer.parseInt(subjectTimes[1]));
                    subjectCalendar.set(Calendar.SECOND, 0);
                    subjectCalendar.set(Calendar.MILLISECOND, 0);
                    long subjectTime = subjectCalendar.getTimeInMillis();
                    if (cursor1.moveToFirst() && cursor.getPosition() + 1 < cursor1.getCount() && cursor1.moveToPosition(cursor.getPosition() + 1) && cursor1.getString(3) != null && subjectTimeString.equals(cursor1.getString(3))) {
                        if (notificationType == 2) {
                            notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.multiple_lessons_one_hour);
                            subjectTime = subjectTime - 3600000;
                        }
                        if (notificationType == 3) {
                            notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.multiple_lessons_thirty_minutes);
                            subjectTime = subjectTime - 1800000;
                        }
                        if (notificationType == 4) {
                            notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.multiple_lessons_fifteen_minutes);
                            subjectTime = subjectTime - 900000;
                        }
                        if (!notificationText.equals("") && System.currentTimeMillis() < subjectTime) {
                            cancelAlarm();
                            Intent notificationIntent = new Intent(ApplicationLoader.applicationContext, NotificationReceiver.class);
                            notificationIntent.putExtra("notification", notificationText);
                            PendingIntent pendingIntent = PendingIntent.getBroadcast(ApplicationLoader.applicationContext, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                            AlarmManager alarmManager = (AlarmManager) ApplicationLoader.applicationContext.getSystemService(Context.ALARM_SERVICE);
                            if (android.os.Build.VERSION.SDK_INT >= 19) {
                                alarmManager.setExact(AlarmManager.RTC_WAKEUP, subjectTime, pendingIntent);
                            } else {
                                alarmManager.set(AlarmManager.RTC_WAKEUP, subjectTime, pendingIntent);
                            }
                            synchronized (this) {
                                wait();
                            }
                        }
                    } else {
                        String lessonName = cursor.getString(5);
                        String lessonLocation = cursor.getString(6);
                        if (notificationType == 2) {
                            notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.lesson_one_hour, lessonName, lessonLocation);
                            subjectTime = subjectTime - 3600000;
                        }
                        if (notificationType == 3) {
                            notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.lesson_thirty_minutes, lessonName, lessonLocation);
                            subjectTime = subjectTime - 1800000;
                        }
                        if (notificationType == 4) {
                            notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.lesson_fifteen_minutes, lessonName, lessonLocation);
                            subjectTime = subjectTime - 900000;
                        }
                        if (!notificationText.equals("") && System.currentTimeMillis() < subjectTime) {
                            cancelAlarm();
                            Intent notificationIntent = new Intent(ApplicationLoader.applicationContext, NotificationReceiver.class);
                            notificationIntent.putExtra("notification", notificationText);
                            PendingIntent pendingIntent = PendingIntent.getBroadcast(ApplicationLoader.applicationContext, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                            AlarmManager alarmManager = (AlarmManager) ApplicationLoader.applicationContext.getSystemService(Context.ALARM_SERVICE);
                            if (android.os.Build.VERSION.SDK_INT >= 19) {
                                alarmManager.setExact(AlarmManager.RTC_WAKEUP, subjectTime, pendingIntent);
                            } else {
                                alarmManager.set(AlarmManager.RTC_WAKEUP, subjectTime, pendingIntent);
                            }
                            synchronized (this) {
                                wait();
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
                synchronized (this) {
                    wait();
                }
            } catch (InterruptedException e) {
                interrupt();
            } catch (Exception e) {
                createNotification(ApplicationLoader.applicationContext.getResources().getString(R.string.connection_problem), false);
                stopRunning();
            }
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void stopRunning() {
        running = false;
        notifyThread();
        interrupt();
        cancelAlarm();
    }

    public void createNotification(String notificationText, boolean headsUp) {
        if (notificationType != 0 && notificationType != 6 && mNotificationManager != null) {
            Intent intent = new Intent(ApplicationLoader.applicationContext, ScheduleActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(ApplicationLoader.applicationContext, (int) System.currentTimeMillis(), intent, 0);

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(ApplicationLoader.applicationContext)
                    .setContentTitle(ApplicationLoader.applicationContext.getResources().getString(R.string.app_name))
                    .setContentText(notificationText)
                    .setContentIntent(pendingIntent)
                    .setSmallIcon(R.drawable.notifybar)
                    .setOngoing(false)
                    .setAutoCancel(true)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(notificationText))
                    .setColor(ContextCompat.getColor(ApplicationLoader.applicationContext, R.color.colorPrimary));
            if (headsUp) {
                if (android.os.Build.VERSION.SDK_INT >= 16) {
                    mBuilder.setPriority(Notification.PRIORITY_HIGH);
                }
                mBuilder.setDefaults(Notification.DEFAULT_ALL);
            }
            mNotificationManager.notify(0, mBuilder.build());
        } else {
            clearNotification();
        }
    }

    public void notifyThread() {
        synchronized (this) {
            notify();
        }
    }

    public void clearNotification() {
        if (mNotificationManager != null) {
            mNotificationManager.cancel(0);
        }
    }

    public void cancelAlarm() {
        Intent notificationIntent = new Intent(ApplicationLoader.applicationContext, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(ApplicationLoader.applicationContext, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) ApplicationLoader.applicationContext.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }
}
