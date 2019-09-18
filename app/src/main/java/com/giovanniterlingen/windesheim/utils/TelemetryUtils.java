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
package com.giovanniterlingen.windesheim.utils;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.preference.PreferenceManager;

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.Constants;
import com.giovanniterlingen.windesheim.controllers.DatabaseController;
import com.giovanniterlingen.windesheim.models.Schedule;
import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class TelemetryUtils {

    private static volatile TelemetryUtils Instance = null;
    private static FirebaseAnalytics mFirebaseAnalytics;

    private TelemetryUtils() {
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(ApplicationLoader.applicationContext);
        init();
    }

    public static TelemetryUtils getInstance() {
        TelemetryUtils localInstance = Instance;
        if (localInstance == null) {
            synchronized (TelemetryUtils.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new TelemetryUtils();
                }
            }
        }
        return localInstance;
    }

    private void init() {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(ApplicationLoader.applicationContext);

        int currentNightMode = ApplicationLoader.applicationContext.getResources()
                .getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean darkModeEnabled = preferences.getBoolean(Constants.PREFS_DARK_MODE,
                currentNightMode == Configuration.UI_MODE_NIGHT_YES);
        setUserProperty(Constants.TELEMETRY_PROPERTY_DARK_MODE_ENABLED,
                Boolean.toString(darkModeEnabled));

        String notificationTypeString;
        int notificationType = preferences.getInt(Constants.PREFS_NOTIFICATIONS_TYPE,
                Constants.NOTIFICATION_TYPE_NOT_SET);
        switch (notificationType) {
            case Constants.NOTIFICATION_TYPE_1_HOUR:
                notificationTypeString = "1_hour";
                break;
            case Constants.NOTIFICATION_TYPE_30_MIN:
                notificationTypeString = "30_minutes";
                break;
            case Constants.NOTIFICATION_TYPE_15_MIN:
                notificationTypeString = "15_minutes";
                break;
            case Constants.NOTIFICATION_TYPE_ALWAYS_ON:
                notificationTypeString = "persistent";
                break;
            case Constants.NOTIFICATION_TYPE_OFF:
                notificationTypeString = "off";
                break;
            case Constants.NOTIFICATION_TYPE_NOT_SET:
                notificationTypeString = "not_set";
                break;
            default:
                notificationTypeString = "none";
                break;
        }
        setUserProperty(Constants.TELEMETRY_PROPERTY_NOTIFICATION_TYPE, notificationTypeString);

        boolean introFinished = preferences.getBoolean(Constants.PREFS_INTRO_FINISHED, false);
        setUserProperty(Constants.TELEMETRY_PROPERTY_INTRO_FINISHED, Boolean.toString(introFinished));

        boolean scheduleChangeNotification = preferences.getBoolean(Constants.PREFS_SCHEDULE_CHANGE_NOTIFICATION, true);
        setUserProperty(Constants.TELEMETRY_PROPERTY_SCHEDULE_CHANGE_NOTIFICATION, Boolean.toString(scheduleChangeNotification));

        boolean allowTelemetry = preferences.getBoolean(Constants.PREFS_TELEMETRY_ALLOWED, true);
        setUserProperty(Constants.TELEMETRY_KEY_TELEMETRY_ENABLED,
                Boolean.toString(allowTelemetry));
        setAnalyticsCollectionEnabled(allowTelemetry);

        int studentScheduleCount = 0;
        int teacherScheduleCount = 0;
        int subjectScheduleCount = 0;
        Schedule[] schedules = DatabaseController.getInstance().getSchedules();
        for (Schedule schedule : schedules) {
            switch (schedule.getType()) {
                case CLASS:
                    studentScheduleCount++;
                    break;
                case TEACHER:
                    teacherScheduleCount++;
                    break;
                case SUBJECT:
                    subjectScheduleCount++;
                    break;
            }
        }
        setUserProperty(Constants.TELEMETRY_KEY_STUDENT_SCHEDULES_COUNT,
                Integer.toString(studentScheduleCount));
        setUserProperty(Constants.TELEMETRY_KEY_TEACHER_SCHEDULES_COUNT,
                Integer.toString(teacherScheduleCount));
        setUserProperty(Constants.TELEMETRY_KEY_SUBJECT_SCHEDULES_COUNT,
                Integer.toString(subjectScheduleCount));

        int weekCount = preferences.getInt(Constants.PREFS_WEEK_COUNT,
                Constants.DEFAULT_WEEK_COUNT);
        setUserProperty(Constants.TELEMETRY_PROPERTY_WEEK_COUNT, Integer.toString(weekCount));
    }

    public void setUserProperty(String key, String value) {
        mFirebaseAnalytics.setUserProperty(key, value);
    }

    public void logEvent(String key, Bundle params) {
        mFirebaseAnalytics.logEvent(key, params);
    }

    public void setCurrentScreen(Activity activity, String name) {
        mFirebaseAnalytics.setCurrentScreen(activity, name, null);
    }

    public void setAnalyticsCollectionEnabled(boolean allowTelemetry) {
        mFirebaseAnalytics.setAnalyticsCollectionEnabled(allowTelemetry);
    }
}
