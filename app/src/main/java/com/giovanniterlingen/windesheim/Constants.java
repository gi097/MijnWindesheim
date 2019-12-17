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

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class Constants {

    public static final String PREFS_DARK_MODE = "dark_mode";
    public static final String PREFS_TELEMETRY_ALLOWED = "telemetry_allowed";
    public static final String PREFS_NOTIFICATIONS_TYPE = "notifications_type";
    public static final String PREFS_INTRO_FINISHED = "intro_finished";
    public static final String PREFS_SCHEDULE_CHANGE_NOTIFICATION = "schedule_change_notification";
    public static final String PREFS_USERNAME = "username";
    public static final String PREFS_PASSWORD = "password";
    public static final String PREFS_WEEK_COUNT = "week_count";
    public static final String PREFS_LAST_REVIEW_PROMPT_TIME = "last_review_prompt_time";
    public static final String PREFS_LAST_FETCH_TIME = "last_fetch_time";
    public static final String PREFS_SYNC_CALENDAR = "sync_calendar";
    public static final String PREFS_SYNC_CALENDAR_ID = "calendar_id";

    public static final String TELEMETRY_PROPERTY_NAME = "name";
    public static final String TELEMETRY_PROPERTY_NOTIFICATION_TYPE = "notification_type";
    public static final String TELEMETRY_PROPERTY_DARK_MODE_ENABLED = "dark_mode_enabled";
    public static final String TELEMETRY_PROPERTY_WEEK_COUNT = "week_count";
    public static final String TELEMETRY_KEY_SCHEDULE_ADDED = "schedule_added";
    public static final String TELEMETRY_KEY_SCHEDULE_DELETED = "schedule_deleted";
    public static final String TELEMETRY_KEY_LESSON_RESTORED = "lesson_restored";
    public static final String TELEMETRY_KEY_LESSON_HIDDEN = "lesson_hidden";
    public static final String TELEMETRY_KEY_LESSON_ADDED_CALENDAR = "lesson_added_calendar";
    public static final String TELEMETRY_KEY_DOWNLOAD_STARTED = "download_started";
    public static final String TELEMETRY_KEY_TELEMETRY_ENABLED = "telemetry_enabled";
    public static final String TELEMETRY_KEY_TEACHER_SCHEDULES_COUNT = "teacher_schedules_count";
    public static final String TELEMETRY_KEY_STUDENT_SCHEDULES_COUNT = "student_schedules_count";
    public static final String TELEMETRY_KEY_SUBJECT_SCHEDULES_COUNT = "subject_schedules_count";
    public static final String TELEMETRY_LOGIN = "login";
    public static final String TELEMETRY_PROPERTY_LOGIN_SUCCESSFUL = "login_successful";
    public static final String TELEMETRY_KEY_WEEK_COUNT_CHANGED = "week_count_changed";
    public static final String TELEMETRY_PROPERTY_INTRO_FINISHED = "intro_finished";
    public static final String TELEMETRY_PROPERTY_SCHEDULE_CHANGE_NOTIFICATION = "schedule_change_notification";

    public static final int NOTIFICATION_TYPE_1_HOUR = 2;
    public static final int NOTIFICATION_TYPE_30_MIN = 3;
    public static final int NOTIFICATION_TYPE_15_MIN = 4;
    public static final int NOTIFICATION_TYPE_ALWAYS_ON = 5;
    public static final int NOTIFICATION_TYPE_OFF = 6;
    public static final int NOTIFICATION_TYPE_NOT_SET = -1;

    public static final int MAX_SCHEDULES_COUNT = 5;
    public static final int MAX_WEEK_COUNT = 5;
    public static final int DEFAULT_WEEK_COUNT = 2;
    public static final int WEEKDAYS_COUNT = 5;
    public static final int WEEKEND_DAYS_COUNT = 2;
    public static final int[] MONTH_STRING_IDS = new int[]{
            R.string.january,
            R.string.february,
            R.string.march,
            R.string.april,
            R.string.may,
            R.string.june,
            R.string.july,
            R.string.august,
            R.string.september,
            R.string.october,
            R.string.november,
            R.string.december
    };

    public enum SCHEDULE_TYPE {
        CLASS,
        TEACHER,
        SUBJECT
    }
}
