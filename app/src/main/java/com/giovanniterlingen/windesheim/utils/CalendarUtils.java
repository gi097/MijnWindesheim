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

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.CalendarContract;

import androidx.preference.PreferenceManager;

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.Constants;
import com.giovanniterlingen.windesheim.controllers.DatabaseController;
import com.giovanniterlingen.windesheim.models.Calendar;
import com.giovanniterlingen.windesheim.models.Lesson;

import java.util.TimeZone;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class CalendarUtils {

    public static Calendar[] getCalendars() {
        if (Build.VERSION.SDK_INT >= 23 && ApplicationLoader.applicationContext.checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        String[] projection = {CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME};
        Uri contentUri = CalendarContract.Calendars.CONTENT_URI;

        ContentResolver contentResolver = ApplicationLoader.applicationContext.getContentResolver();
        Cursor cursor = contentResolver.query(contentUri, projection, null, null, null);

        Calendar[] calendars = new Calendar[cursor.getCount()];
        while (cursor.moveToNext()) {
            Calendar calendar = new Calendar();
            calendar.setId(cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)));
            calendar.setName(cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)));
            calendars[cursor.getPosition()] = calendar;
        }
        cursor.close();
        return calendars;
    }

    public static boolean calendarExists(long id) {
        if (Build.VERSION.SDK_INT >= 23 && ApplicationLoader.applicationContext.checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        String[] projection = {CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME};
        Uri contentUri = CalendarContract.Calendars.CONTENT_URI;

        ContentResolver contentResolver = ApplicationLoader.applicationContext.getContentResolver();
        Cursor cursor = contentResolver.query(contentUri, projection, null, null, null);

        boolean exists = false;
        while (cursor.moveToNext()) {
            if (id == cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID))) {
                exists = true;
            }
        }
        cursor.close();
        return exists;
    }

    public static String getCalendarNameById(long id) {
        if (Build.VERSION.SDK_INT >= 23 && ApplicationLoader.applicationContext.checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

        String[] projection = {CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME};
        Uri contentUri = CalendarContract.Calendars.CONTENT_URI;

        ContentResolver contentResolver = ApplicationLoader.applicationContext.getContentResolver();
        Cursor cursor = contentResolver.query(contentUri, projection, null, null, null);

        String name = null;
        while (cursor.moveToNext()) {
            if (id == cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID))) {
                name = cursor.getString(cursor
                        .getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME));
                break;
            }
        }
        cursor.close();
        return name;
    }

    public static synchronized void syncLessonsWithCalendar(Lesson[] lessons) {
        if (lessons == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 23 &&
                ApplicationLoader.applicationContext.checkSelfPermission(Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        long calendarId;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ApplicationLoader.applicationContext);
        calendarId = preferences.getLong(Constants.PREFS_SYNC_CALENDAR_ID, -1);
        if (calendarId == -1) {
            return;
        }

        ContentResolver contentResolver = ApplicationLoader.applicationContext.getContentResolver();
        for (Lesson lesson : lessons) {
            ContentValues values = new ContentValues();
            values.put(CalendarContract.Events.DTSTART, lesson.getStartTime().getTime());
            values.put(CalendarContract.Events.DTEND, lesson.getEndTime().getTime());
            values.put(CalendarContract.Events.TITLE, lesson.getSubject());
            values.put(CalendarContract.Events.ALL_DAY, false);
            values.put(CalendarContract.Events.TITLE, lesson.getSubject());
            values.put(CalendarContract.Events.EVENT_LOCATION, lesson.getRoom());
            values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
            values.put(CalendarContract.Events.CALENDAR_ID, calendarId);
            values.put(CalendarContract.Events.EVENT_COLOR, ColorUtils.getColorById(lesson.getScheduleId()));

            Uri eventUri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values);
            if (eventUri == null) {
                continue;
            }

            long id = ContentUris.parseId(eventUri);
            DatabaseController.getInstance().addCalendarRow(id);
        }
    }

    public static synchronized void deleteAllLessonsFromCalendar() {
        if (Build.VERSION.SDK_INT >= 23 &&
                ApplicationLoader.applicationContext.checkSelfPermission(Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ContentResolver contentResolver = ApplicationLoader.applicationContext.getContentResolver();

        // First delete current calendar rows saved by us
        long[] eventIds = DatabaseController.getInstance().getCalendarRows();
        for (long eventId : eventIds) {
            Uri eventUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId);
            contentResolver.delete(eventUri, null, null);
        }
        DatabaseController.getInstance().deleteAllCalendarRows();
    }
}
