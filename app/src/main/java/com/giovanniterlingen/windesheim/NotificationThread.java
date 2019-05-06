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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;

import androidx.annotation.VisibleForTesting;

import com.giovanniterlingen.windesheim.controllers.CalendarController;
import com.giovanniterlingen.windesheim.controllers.DatabaseController;
import com.giovanniterlingen.windesheim.controllers.NotificationController;
import com.giovanniterlingen.windesheim.controllers.WebUntisController;
import com.giovanniterlingen.windesheim.models.Lesson;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
class NotificationThread extends Thread {

    private final Object dateLock = new Object();
    private final Object minuteLock = new Object();

    @Override
    public void run() {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(ApplicationLoader.applicationContext);
        int notificationType;
        String notificationText;
        while ((notificationType = preferences.getInt("notifications_type",
                NotificationController.NOTIFICATION_NOT_SET)) != 0) {
            try {
                Date date = new Date();
                if (!DatabaseController.getInstance().isFetched(date)) {
                    new WebUntisController().getAndSaveAllSchedules(date, false);
                }
                Lesson[] lessons = DatabaseController.getInstance()
                        .getLessons(CalendarController.getYearMonthDayDateFormat().format(date));
                for (int i = 0; i < lessons.length; i++) {
                    Lesson lesson = lessons[i];
                    String startTimeString = lesson.getStartTime();
                    String[] startTime = startTimeString.split(":");

                    Calendar calendar = CalendarController.getCalendar();
                    calendar.set(GregorianCalendar.HOUR_OF_DAY, Integer.parseInt(startTime[0]));
                    calendar.set(GregorianCalendar.MINUTE, Integer.parseInt(startTime[1]));
                    calendar.set(GregorianCalendar.SECOND, 0);
                    calendar.set(GregorianCalendar.MILLISECOND, 0);

                    long lessonStartTime = calendar.getTimeInMillis();
                    boolean nextLessonSameTime = i + 1 < lessons.length
                            && startTimeString.equals(lessons[i + 1].getStartTime());
                    String lessonName = lesson.getSubject();
                    String lessonLocation = lesson.getRoom();

                    while (System.currentTimeMillis() < lessonStartTime) {
                        long delta = lessonStartTime - System.currentTimeMillis();

                        // Round delta upwards to the nearest whole minute. (e.g. 7m 58s -> 8m)
                        final long remainder = delta % DateUtils.MINUTE_IN_MILLIS;
                        delta += remainder == 0 ? 0 : (DateUtils.MINUTE_IN_MILLIS - remainder);

                        final int minutes = (int) delta / (1000 * 60) % 60;
                        final int hours = (int) delta / (1000 * 60 * 60) % 24;

                        String timeReadable = getTimeUntilLessonString(delta, minutes, hours);

                        if (nextLessonSameTime) {
                            notificationText = ApplicationLoader.applicationContext.getResources()
                                    .getString(R.string.multiple_lessons_notification,
                                            timeReadable);
                        } else {
                            notificationText = ApplicationLoader.applicationContext.getResources()
                                    .getString(R.string.single_lesson_notification, lessonName,
                                            timeReadable, lessonLocation);
                        }
                        if (notificationType == NotificationController.NOTIFICATION_ALWAYS_ON) {
                            NotificationController.getInstance()
                                    .createNotification(notificationText, true, false);
                        } else if (hours == 1 && minutes == 0 && notificationType ==
                                NotificationController.NOTIFICATION_1_HOUR ||
                                hours == 0 && minutes == 30 && notificationType ==
                                        NotificationController.NOTIFICATION_30_MIN ||
                                hours == 0 && minutes == 15 && notificationType ==
                                        NotificationController.NOTIFICATION_15_MIN) {
                            NotificationController.getInstance()
                                    .createNotification(notificationText, false, true);
                        }
                        synchronized (minuteLock) {
                            minuteLock.wait();
                        }
                    }
                    // Notify the UI that a lesson has been changed
                    NotificationCenter.getInstance()
                            .postNotificationName(NotificationCenter.scheduleReload);
                }
                NotificationController.getInstance().clearNotification();
                synchronized (dateLock) {
                    dateLock.wait();
                }
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                NotificationController.getInstance()
                        .createNotification(ApplicationLoader.applicationContext.getResources()
                                .getString(R.string.connection_problem), false, false);
                break;
            }
        }
    }

    void notifyDateChanged() {
        synchronized (dateLock) {
            dateLock.notify();
        }
    }

    void notifyMinuteChanged() {
        synchronized (minuteLock) {
            minuteLock.notify();
        }
    }

    /**
     * format "in 7 hours and 53 minutes"
     * <p>
     * Source from Android Open Source Project Deskclock
     */
    @VisibleForTesting
    private String getTimeUntilLessonString(long delta, int minutes, int hours) {
        // If the delta is less then 60 seconds, just report "less than a minute."
        final String[] formats = ApplicationLoader.applicationContext.getResources()
                .getStringArray(R.array.time_until);
        if (delta < DateUtils.MINUTE_IN_MILLIS) {
            return formats[0];
        }

        // Otherwise, format the remaining time until the lesson starts.

        String minSeq = getNumberFormattedQuantityString(ApplicationLoader.applicationContext,
                R.plurals.minutes, minutes);
        String hourSeq = getNumberFormattedQuantityString(ApplicationLoader.applicationContext,
                R.plurals.hours, hours);

        final boolean showHours = hours > 0;
        final boolean showMinutes = minutes > 0;

        // Compute the index of the most appropriate time format based on the time delta.
        final int index = (showHours ? 1 : 0) | (showMinutes ? 2 : 0);

        return String.format(formats[index], hourSeq, minSeq);
    }

    /**
     * @param id       Resource id of the plural
     * @param quantity integer value
     * @return string with properly localized numbers
     */
    private String getNumberFormattedQuantityString(Context context, int id, int quantity) {
        final String localizedQuantity = NumberFormat.getInstance().format(quantity);
        return context.getResources().getQuantityString(id, quantity, localizedQuantity);
    }
}