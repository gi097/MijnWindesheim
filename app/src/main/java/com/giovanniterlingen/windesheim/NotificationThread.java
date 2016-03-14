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
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * A scheduler app for Windesheim students
 *
 * @author Giovanni Terlingen
 */
class NotificationThread extends Thread {
    private static String lastNotification = "";
    private boolean running = true;
    private SharedPreferences preferences;
    private NotificationManager mNotificationManager;

    @Override
    public void run() {
        mNotificationManager = (NotificationManager) ApplicationLoader.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
        preferences = PreferenceManager.getDefaultSharedPreferences(ApplicationLoader.applicationContext);
        String componentId = preferences.getString("componentId", "");
        int type = preferences.getInt("type", 0);
        DateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        while (isRunning() && componentId.length() > 0 && type != 0 && preferences.getInt("notifications_type", 0) != 0) {
            try {
                Calendar calendar = Calendar.getInstance();
                Date date = calendar.getTime();
                Cursor cursor = ApplicationLoader.scheduleDatabase.getLessons(simpleDateFormat.format(date), componentId);
                if (cursor == null || !cursor.moveToFirst()) {
                    ScheduleHandler.saveSchedule(ScheduleHandler.getScheduleFromServer(componentId, date, type), date, componentId, type);
                    cursor = ApplicationLoader.scheduleDatabase.getLessons(simpleDateFormat.format(date), componentId);
                }
                if (cursor != null && cursor.getCount() == 0) {
                    while (checkIfNeedsContinue(calendar) && preferences.getInt("notifications_type", 0) == 5) {
                        createNotification("Er zijn geen lessen gevonden voor vandaag", false);
                        Thread.sleep(1000);
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
                        Cursor secondCursor = ApplicationLoader.scheduleDatabase.getLessons(simpleDateFormat.format(date), componentId);
                        if (secondCursor.moveToFirst() && cursor.getPosition() + 1 < secondCursor.getCount()) {
                            secondCursor.moveToPosition(cursor.getPosition() + 1);
                        }
                        if (secondCursor.getString(3) != null && secondCursor.getString(3).equals(subjectTimeString)) {
                            while (System.currentTimeMillis() < subjectTime && checkIfNeedsContinue(calendar)) {
                                int notificationType = preferences.getInt("notifications_type", 0);
                                long difference = subjectTime - System.currentTimeMillis();
                                long diffMinutes = (difference / (1000 * 60)) % 60;
                                long diffHours = (difference / (1000 * 60 * 60)) % 24;
                                if (diffHours >= 1) {
                                    if (diffMinutes != 0) {
                                        if (type == 1) {
                                            notificationText = "Je hebt over " + diffHours + " uur en " + diffMinutes + (diffMinutes == 1 ? " minuut " : " minuten ") + "meerdere lessen";
                                        }
                                        if (type == 2) {
                                            notificationText = "U heeft over " + diffHours + " uur en " + diffMinutes + (diffMinutes == 1 ? " minuut " : " minuten ") + "meerdere lessen";
                                        }
                                    } else {
                                        if (type == 1) {
                                            notificationText = "Je hebt over " + diffHours + " uur meerdere lessen";
                                        }
                                        if (type == 2) {
                                            notificationText = "U heeft over " + diffHours + " uur meerdere lessen";
                                        }
                                    }
                                } else {
                                    if (diffMinutes >= 1) {
                                        if (diffMinutes == 1) {
                                            if (type == 1) {
                                                notificationText = "Je hebt over " + diffMinutes + " minuut meerdere lessen";
                                            }
                                            if (type == 2) {
                                                notificationText = "U heeft over " + diffMinutes + " minuut meerdere lessen";
                                            }
                                        } else {
                                            if (type == 1) {
                                                notificationText = "Je hebt over " + diffMinutes + " minuten meerdere lessen";
                                            }
                                            if (type == 2) {
                                                notificationText = "U heeft over " + diffMinutes + " minuten meerdere lessen";
                                            }
                                        }
                                    }
                                }
                                if (notificationType == 5) {
                                    createNotification(notificationText, true);
                                }
                                if (diffHours == 1 && diffMinutes == 0 && notificationType == 2 || diffHours == 0 && diffMinutes == 30 && notificationType == 3 || diffHours == 0 && diffMinutes == 15 && notificationType == 4) {
                                    createNotification(notificationText, false);
                                }
                                Thread.sleep(1000);
                            }
                        } else {
                            while (System.currentTimeMillis() < subjectTime && checkIfNeedsContinue(calendar)) {
                                int notificationType = preferences.getInt("notifications_type", 0);
                                long difference = subjectTime - System.currentTimeMillis();
                                long diffMinutes = (difference / (1000 * 60)) % 60;
                                long diffHours = (difference / (1000 * 60 * 60)) % 24;
                                if (diffHours >= 1) {
                                    if (diffMinutes != 0) {
                                        if (type == 1) {
                                            notificationText = "Je hebt over " + diffHours + " uur en " + diffMinutes + (diffMinutes == 1 ? " minuut " : " minuten ") + cursor.getString(5) + " in " + cursor.getString(6);
                                        }
                                        if (type == 2) {
                                            notificationText = "U heeft over " + diffHours + " uur en " + diffMinutes + (diffMinutes == 1 ? " minuut " : " minuten ") + cursor.getString(5) + " in " + cursor.getString(6);
                                        }
                                    } else {
                                        if (type == 1) {
                                            notificationText = "Je hebt over " + diffHours + " uur " + cursor.getString(5) + " in " + cursor.getString(6);
                                        }
                                        if (type == 2) {
                                            notificationText = "U heeft over " + diffHours + " uur " + cursor.getString(5) + " in " + cursor.getString(6);
                                        }
                                    }
                                } else {
                                    if (diffMinutes >= 1) {
                                        if (diffMinutes == 1) {
                                            if (type == 1) {
                                                notificationText = "Je hebt over " + diffMinutes + " minuut " + cursor.getString(5) + " in " + cursor.getString(6);
                                            }
                                            if (type == 2) {
                                                notificationText = "U heeft over " + diffMinutes + " minuut " + cursor.getString(5) + " in " + cursor.getString(6);
                                            }
                                        } else {
                                            if (type == 1) {
                                                notificationText = "Je hebt over " + diffMinutes + " minuten " + cursor.getString(5) + " in " + cursor.getString(6);
                                            }
                                            if (type == 2) {
                                                notificationText = "U heeft over " + diffMinutes + " minuten " + cursor.getString(5) + " in " + cursor.getString(6);
                                            }
                                        }
                                    }
                                }
                                if (notificationType == 5) {
                                    createNotification(notificationText, true);
                                }
                                if (diffHours == 1 && diffMinutes == 0 && notificationType == 2 || diffHours == 0 && diffMinutes == 30 && notificationType == 3 || diffHours == 0 && diffMinutes == 15 && notificationType == 4) {
                                    createNotification(notificationText, false);
                                }
                                Thread.sleep(1000);
                            }
                        }
                        secondCursor.close();
                    }
                }
                while (checkIfNeedsContinue(calendar) && preferences.getInt("notifications_type", 0) == 5) {
                    if (type == 1) {
                        createNotification("Je hebt geen lessen meer vandaag :)", false);
                    }
                    if (type == 2) {
                        createNotification("U hoeft geen les meer te geven vandaag :)", false);
                    }
                    Thread.sleep(1000);
                }
                if (cursor != null) {
                    cursor.close();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                createNotification("Probleem bij het ophalen van de gegevens", true);
                while (!ApplicationLoader.isNetworkAvailable()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        this.interrupt();
                    }
                }
                ApplicationLoader.restartNotificationThread();
            }
        }
    }

    private boolean isRunning() {
        return running;
    }

    public void setRunning(boolean setRunning) {
        running = setRunning;
    }

    private void createNotification(String notificationText, boolean onGoing) {
        int notificationType = preferences.getInt("notifications_type", 0);
        if (notificationType != 0 && notificationType != 6 || mNotificationManager == null) {
            if (lastNotification.equals(notificationText)) {
                return;
            }
            lastNotification = notificationText;

            if (onGoing) {
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(ApplicationLoader.applicationContext)
                        .setContentTitle(ApplicationLoader.applicationContext.getResources().getString(R.string.app_name))
                        .setContentText(notificationText)
                        .setSmallIcon(R.drawable.notifybar)
                        .setOngoing(true)
                        .setAutoCancel(false)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(notificationText))
                        .setColor(ContextCompat.getColor(ApplicationLoader.applicationContext, R.color.colorPrimary));

                Intent resultIntent = new Intent(ApplicationLoader.applicationContext, ScheduleActivity.class);
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(ApplicationLoader.applicationContext);
                stackBuilder.addParentStack(ScheduleActivity.class);
                stackBuilder.addNextIntent(resultIntent);
                PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.setContentIntent(resultPendingIntent);

                mNotificationManager.notify(0, mBuilder.build());
            } else {
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(ApplicationLoader.applicationContext)
                        .setContentTitle(ApplicationLoader.applicationContext.getResources().getString(R.string.app_name))
                        .setContentText(notificationText)
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                        .setLights(0, 0, 0)
                        .setSmallIcon(R.drawable.notifybar)
                        .setOngoing(false)
                        .setAutoCancel(true)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(notificationText))
                        .setColor(ContextCompat.getColor(ApplicationLoader.applicationContext, R.color.colorPrimary));

                Intent resultIntent = new Intent(ApplicationLoader.applicationContext, ScheduleActivity.class);
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(ApplicationLoader.applicationContext);
                stackBuilder.addParentStack(ScheduleActivity.class);
                stackBuilder.addNextIntent(resultIntent);
                PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.setContentIntent(resultPendingIntent);

                mNotificationManager.notify(0, mBuilder.build());
            }
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
