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

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;

import com.giovanniterlingen.windesheim.controllers.CalendarController;
import com.giovanniterlingen.windesheim.controllers.DatabaseController;
import com.giovanniterlingen.windesheim.controllers.NotificationController;
import com.giovanniterlingen.windesheim.controllers.WebUntisController;
import com.giovanniterlingen.windesheim.models.Lesson;

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
        int notificationType = preferences.getInt("notifications_type",
                NotificationController.NOTIFICATION_NOT_SET);
        String notificationText;
        while (notificationType != 0 && notificationType != NotificationController.NOTIFICATION_OFF) {
            try {
                Date date = new Date();
                if (!DatabaseController.getInstance().isFetched(date)) {
                    new WebUntisController().getAndSaveAllSchedules(date, false);
                }
                Lesson[] lessons = DatabaseController.getInstance()
                        .getLessons(CalendarController.getInstance()
                                .getYearMonthDayDateFormat().format(date));
                for (int i = 0; i < lessons.length; i++) {
                    Lesson lesson = lessons[i];
                    String startTimeString = lesson.getStartTime();
                    String[] startTime = startTimeString.split(":");

                    Calendar calendar = CalendarController.getInstance().getCalendar();
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
                        long difference = lessonStartTime - System.currentTimeMillis();
                        long diffMinutes = (difference / 60000) % 60;
                        long diffHours = (difference / 3600000) % 24;
                        String timeReadable = DateUtils
                                .getRelativeTimeSpanString(lessonStartTime).toString()
                                .toLowerCase();
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
                        } else if (diffHours == 1 && diffMinutes == 0 &&
                                notificationType == NotificationController.NOTIFICATION_1_HOUR ||
                                diffHours == 0 && diffMinutes == 30 && notificationType ==
                                        NotificationController.NOTIFICATION_30_MIN ||
                                diffHours == 0 && diffMinutes == 15 && notificationType ==
                                        NotificationController.NOTIFICATION_15_MIN) {
                            NotificationController.getInstance()
                                    .createNotification(notificationText, false, true);
                        }
                        synchronized (minuteLock) {
                            minuteLock.wait();
                        }
                    }
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
}