package com.giovanniterlingen.windesheim;

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
class NotificationThread extends Thread {
    private static String lastNotification = "";
    private boolean running = true;
    private SharedPreferences preferences;
    private NotificationManager mNotificationManager;
    private int notificationType;

    @Override
    public void run() {
        synchronized (this) {
            mNotificationManager = (NotificationManager) ApplicationLoader.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
            preferences = PreferenceManager.getDefaultSharedPreferences(ApplicationLoader.applicationContext);
            notificationType = preferences.getInt("notifications_type", 0);
            String componentId = preferences.getString("componentId", "");
            int type = preferences.getInt("type", 0);
            DateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            while (isRunning() && componentId.length() > 0 && type != 0 && notificationType != 0) {
                try {
                    Calendar calendar = Calendar.getInstance();
                    Date date = calendar.getTime();
                    Cursor cursor = ApplicationLoader.scheduleDatabase.getLessons(simpleDateFormat.format(date), componentId);
                    Cursor cursor1 = ApplicationLoader.scheduleDatabase.getLessons(simpleDateFormat.format(date), componentId);
                    if (cursor == null || cursor.getCount() == 0) {
                        ScheduleHandler.saveSchedule(ScheduleHandler.getScheduleFromServer(componentId, date, type), date, componentId, type);
                        cursor = ApplicationLoader.scheduleDatabase.getLessons(simpleDateFormat.format(date), componentId);
                    }
                    if (cursor != null && cursor.getCount() == 0) {
                        while (checkIfNeedsContinue(calendar) && notificationType == 5) {
                            createNotification(ApplicationLoader.applicationContext.getResources().getString(R.string.no_lessons_found), false, false);
                            Thread.sleep(2000);
                        }
                    } else {
                        while (cursor != null && cursor.moveToNext() && checkIfNeedsContinue(calendar)) {
                            String notificationText = "";
                            String subjectTimeString = cursor.getString(3);
                            String[] subjectTimes = subjectTimeString.split(":");
                            Calendar subjectCalendar = Calendar.getInstance();
                            subjectCalendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(subjectTimes[0]));
                            subjectCalendar.set(Calendar.MINUTE, Integer.parseInt(subjectTimes[1]));
                            long subjectTime = subjectCalendar.getTimeInMillis();
                            if (cursor1.moveToFirst() && cursor.getPosition() + 1 < cursor1.getCount() && cursor1.moveToPosition(cursor.getPosition() + 1) && cursor1.getString(3) != null && subjectTimeString.equals(cursor1.getString(3))) {
                                while (System.currentTimeMillis() < subjectTime && checkIfNeedsContinue(calendar)) {
                                    long difference = subjectTime - System.currentTimeMillis();
                                    long diffMinutes = (difference / (1000 * 60)) % 60;
                                    long diffHours = (difference / (1000 * 60 * 60)) % 24;
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
                                    Thread.sleep(2000);
                                }
                            } else {
                                while (System.currentTimeMillis() < subjectTime && checkIfNeedsContinue(calendar)) {
                                    long difference = subjectTime - System.currentTimeMillis();
                                    long diffMinutes = (difference / (1000 * 60)) % 60;
                                    long diffHours = (difference / (1000 * 60 * 60)) % 24;
                                    if (diffHours >= 1) {
                                        if (diffMinutes != 0) {
                                            if (diffHours == 1) {
                                                if (diffMinutes == 1) {
                                                    notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.lesson_one_hour_one_minute, cursor.getString(5), cursor.getString(6));
                                                } else {
                                                    notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.lesson_one_hour_multiple_minutes, cursor.getString(5), diffMinutes, cursor.getString(6));
                                                }
                                            } else {
                                                if (diffMinutes == 1) {
                                                    notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.lesson_multiple_hours_one_minute, cursor.getString(5), diffHours, cursor.getString(6));
                                                } else {
                                                    notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.lesson_multiple_hours_multiple_minutes, cursor.getString(5), diffHours, diffMinutes, cursor.getString(6));
                                                }
                                            }
                                        } else {
                                            if (diffHours == 1) {
                                                notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.lesson_one_hour, cursor.getString(5), cursor.getString(6));
                                            } else {
                                                notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.lesson_multiple_hours, cursor.getString(5), diffHours, cursor.getString(6));
                                            }
                                        }
                                    } else {
                                        if (diffMinutes >= 1) {
                                            if (diffMinutes == 1) {
                                                notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.lesson_one_minute, cursor.getString(5), cursor.getString(6));
                                            } else {
                                                notificationText = ApplicationLoader.applicationContext.getResources().getString(R.string.lesson_multiple_minutes, cursor.getString(5), diffMinutes, cursor.getString(6));
                                            }
                                        }
                                    }
                                    if (notificationType == 5) {
                                        createNotification(notificationText, true, false);
                                    }
                                    if (diffHours == 1 && diffMinutes == 0 && notificationType == 2 || diffHours == 0 && diffMinutes == 30 && notificationType == 3 || diffHours == 0 && diffMinutes == 15 && notificationType == 4) {
                                        createNotification(notificationText, false, true);
                                    }
                                    Thread.sleep(2000);
                                }
                            }
                        }
                    }
                    while (checkIfNeedsContinue(calendar) && notificationType == 5) {
                        createNotification(ApplicationLoader.applicationContext.getString(R.string.no_more_lessons), false, false);
                        Thread.sleep(2000);
                    }
                    if (cursor != null) {
                        cursor.close();
                    }
                    if (cursor1 != null) {
                        cursor1.close();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    createNotification(ApplicationLoader.applicationContext.getResources().getString(R.string.connection_problem), false, false);
                    stopRunning();
                }
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
        if (notificationType != 0 && notificationType != 6 || mNotificationManager == null) {
            if (lastNotification.equals(notificationText)) {
                return;
            }
            lastNotification = notificationText;

            Intent intent = new Intent(ApplicationLoader.applicationContext, ScheduleActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(ApplicationLoader.applicationContext, (int) System.currentTimeMillis(), intent, 0);

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(ApplicationLoader.applicationContext)
                    .setContentTitle(ApplicationLoader.applicationContext.getResources().getString(R.string.app_name))
                    .setContentText(notificationText)
                    .setContentIntent(pendingIntent)
                    .setSmallIcon(R.drawable.notifybar)
                    .setOngoing(onGoing)
                    .setAutoCancel(!onGoing)
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

    public void clearNotification() {
        lastNotification = "";
        if (mNotificationManager != null) {
            mNotificationManager.cancel(0);
        }
    }

    private boolean checkIfNeedsContinue(Calendar calendar) {
        Calendar calendar1 = Calendar.getInstance();
        return (isRunning() && calendar1.getTimeInMillis() >= calendar.getTimeInMillis() && calendar.get(Calendar.YEAR) == calendar1.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == calendar1.get(Calendar.DAY_OF_YEAR));
    }
}
