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
        String notificationText = "";
        long currentTimeMillis;
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
                    String subjectTimeString = lesson.getStartTime();
                    String[] subjectTimes = subjectTimeString.split(":");
                    Calendar calendar = CalendarController.getInstance().getCalendar();
                    calendar.set(GregorianCalendar.HOUR_OF_DAY,
                            Integer.parseInt(subjectTimes[0]));
                    calendar.set(GregorianCalendar.MINUTE,
                            Integer.parseInt(subjectTimes[1]));
                    long subjectTime = calendar.getTimeInMillis();
                    boolean multiple = i + 1 < lessons.length
                            && subjectTimeString.equals(lessons[i + 1].getStartTime());
                    String lessonName = lesson.getSubject();
                    String lessonLocation = lesson.getRoom();
                    while ((currentTimeMillis = System.currentTimeMillis()) < subjectTime) {
                        long difference = subjectTime - currentTimeMillis;
                        long diffMinutes = (difference / 60000) % 60;
                        long diffHours = (difference / 3600000) % 24;
                        if (diffHours >= 1) {
                            if (diffMinutes != 0) {
                                if (diffHours == 1) {
                                    if (diffMinutes == 1) {
                                        if (multiple) {
                                            notificationText = getString(R.string.multiple_lessons_one_hour_one_minute);
                                        } else {
                                            notificationText = getString(R.string.lesson_one_hour_one_minute, lessonName, lessonLocation);
                                        }
                                    } else {
                                        if (multiple) {
                                            notificationText = getString(R.string.multiple_lessons_one_hour_multiple_minutes, diffMinutes);
                                        } else {
                                            notificationText = getString(R.string.lesson_one_hour_multiple_minutes, lessonName, diffMinutes, lessonLocation);
                                        }
                                    }
                                } else {
                                    if (diffMinutes == 1) {
                                        if (multiple) {
                                            notificationText = getString(R.string.multiple_lessons_multiple_hours_one_minute, diffHours);
                                        } else {
                                            notificationText = getString(R.string.lesson_multiple_hours_one_minute, lessonName, diffHours, lessonLocation);
                                        }
                                    } else {
                                        if (multiple) {
                                            notificationText = getString(R.string.multiple_lessons_multiple_hours_multiple_minutes, diffHours, diffMinutes);
                                        } else {
                                            notificationText = getString(R.string.lesson_multiple_hours_multiple_minutes, lessonName, diffHours, diffMinutes, lessonLocation);
                                        }
                                    }
                                }
                            } else {
                                if (diffHours == 1) {
                                    if (multiple) {
                                        notificationText = getString(R.string.multiple_lessons_one_hour);
                                    } else {
                                        notificationText = getString(R.string.lesson_one_hour, lessonName, lessonLocation);
                                    }
                                } else {
                                    if (multiple) {
                                        notificationText = getString(R.string.multiple_lessons_multiple_hours, diffHours);
                                    } else {
                                        notificationText = getString(R.string.lesson_multiple_hours, lessonName, diffHours, lessonLocation);
                                    }
                                }
                            }
                        } else {
                            if (diffMinutes >= 1) {
                                if (diffMinutes == 1) {
                                    if (multiple) {
                                        notificationText = getString(R.string.multiple_lessons_one_minute);
                                    } else {
                                        notificationText = getString(R.string.lesson_one_minute, lessonName, lessonLocation);
                                    }
                                } else {
                                    if (multiple) {
                                        notificationText = getString(R.string.multiple_lessons_multiple_minutes, diffMinutes);
                                    } else {
                                        notificationText = getString(R.string.lesson_multiple_minutes, lessonName, diffMinutes, lessonLocation);
                                    }
                                }
                            }
                        }
                        if (notificationType == NotificationController.NOTIFICATION_ALWAYS_ON) {
                            NotificationController.getInstance()
                                    .createNotification(notificationText, true, false);
                        } else if (diffHours == 1 && diffMinutes == 0 && notificationType ==
                                NotificationController.NOTIFICATION_1_HOUR ||
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

    private String getString(int resId, Object... formatArgs) {
        return ApplicationLoader.applicationContext.getResources().getString(resId, formatArgs);
    }

    private String getString(int resId) {
        return ApplicationLoader.applicationContext.getResources().getString(resId);
    }
}