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
package com.giovanniterlingen.windesheim.SQLite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import com.giovanniterlingen.windesheim.objects.Lesson;
import com.giovanniterlingen.windesheim.objects.Schedule;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class ScheduleDatabase extends SQLiteOpenHelper {

    private SQLiteDatabase database;

    private static final int DATABASE_VERSION = 8;
    private static final String DATABASE_NAME = "schedulestore.db";

    private static final String SQL_CREATE_SCHEDULE_ENTRIES =
            "CREATE TABLE " + ScheduleEntry.TABLE_NAME + " (" +
                    ScheduleEntry._ID + " INTEGER PRIMARY KEY," +
                    ScheduleEntry.COLUMN_NAME_SCHEDULE_ID + " INTEGER UNIQUE," +
                    ScheduleEntry.COLUMN_NAME_SCHEDULE_NAME + " TEXT," +
                    ScheduleEntry.COLUMN_NAME_SCHEDULE_TYPE + " INTEGER);";
    private static final String SQL_CREATE_LESSON_ENTRIES =
            "CREATE TABLE " + LessonEntry.TABLE_NAME + " (" +
                    LessonEntry._ID + " INTEGER PRIMARY KEY," +
                    LessonEntry.COLUMN_NAME_LESSON_ID + " INTEGER," +
                    LessonEntry.COLUMN_NAME_SUBJECT + " TEXT," +
                    LessonEntry.COLUMN_NAME_DATE + " TEXT," +
                    LessonEntry.COLUMN_NAME_START_TIME + " TEXT," +
                    LessonEntry.COLUMN_NAME_END_TIME + " TEXT," +
                    LessonEntry.COLUMN_NAME_ROOM + " TEXT," +
                    LessonEntry.COLUMN_NAME_TEACHER + " TEXT," +
                    LessonEntry.COLUMN_NAME_CLASS_NAME + " TEXT," +
                    LessonEntry.COLUMN_NAME_SCHEDULE_ID + " INTEGER," +
                    LessonEntry.COLUMN_NAME_SCHEDULE_TYPE + " INTEGER," +
                    LessonEntry.COLUMN_NAME_VISIBLE + " INTEGER)";
    private static final String SQL_CREATE_FETCHED_DATES_ENTRIES =
            "CREATE TABLE " + FetchedDateEntry.TABLE_NAME + " (" +
                    FetchedDateEntry.COLUMN_NAME_DATE + " TEXT UNIQUE)";

    public ScheduleDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void open() {
        database = this.getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(SQL_CREATE_SCHEDULE_ENTRIES);
        database.execSQL(SQL_CREATE_LESSON_ENTRIES);
        database.execSQL(SQL_CREATE_FETCHED_DATES_ENTRIES);
    }

    public void saveScheduleData(Lesson lesson) {
        ContentValues values = new ContentValues();
        values.put(LessonEntry.COLUMN_NAME_LESSON_ID, lesson.getId());
        values.put(LessonEntry.COLUMN_NAME_SUBJECT, lesson.getSubject());
        values.put(LessonEntry.COLUMN_NAME_DATE, lesson.getDate());
        values.put(LessonEntry.COLUMN_NAME_START_TIME, lesson.getStartTime());
        values.put(LessonEntry.COLUMN_NAME_END_TIME, lesson.getEndTime());
        values.put(LessonEntry.COLUMN_NAME_ROOM, lesson.getRoom());
        values.put(LessonEntry.COLUMN_NAME_TEACHER, lesson.getTeacher());
        values.put(LessonEntry.COLUMN_NAME_CLASS_NAME, lesson.getClassName());
        values.put(LessonEntry.COLUMN_NAME_SCHEDULE_ID, lesson.getScheduleId());
        values.put(LessonEntry.COLUMN_NAME_SCHEDULE_TYPE, lesson.getScheduleType());
        values.put(LessonEntry.COLUMN_NAME_VISIBLE, lesson.getVisible());
        database.insert(LessonEntry.TABLE_NAME, null, values);
    }

    public void clearScheduleData(Date date, int id) {
        String[] weekDates = getWeekDates(date);
        String selection = LessonEntry.COLUMN_NAME_DATE + " >= ? AND " +
                LessonEntry.COLUMN_NAME_DATE + " <= ? AND " +
                LessonEntry.COLUMN_NAME_SCHEDULE_ID + " = ? AND " +
                LessonEntry.COLUMN_NAME_VISIBLE + " = ?";
        String[] selectionArgs = {weekDates[0], weekDates[1], Integer.toString(id),
                Integer.toString(1)};
        database.delete(LessonEntry.TABLE_NAME, selection, selectionArgs);
    }

    public void clearOldScheduleData(Date date) {
        String selection = LessonEntry.COLUMN_NAME_DATE + " < ? AND " +
                LessonEntry.COLUMN_NAME_VISIBLE + " = ?";
        String[] selectionArgs = {parseDate(date), Integer.toString(1)};
        database.delete(LessonEntry.TABLE_NAME, selection, selectionArgs);
        deleteOldFetched(date);
    }

    public void hideLesson(int lessonId) {
        ContentValues values = new ContentValues();
        values.put(LessonEntry.COLUMN_NAME_VISIBLE, 0);
        String selection = LessonEntry.COLUMN_NAME_LESSON_ID + " = ?";
        String[] selectionArgs = {Integer.toString(lessonId)};
        database.update(LessonEntry.TABLE_NAME, values, selection, selectionArgs);
    }

    public void restoreLesson(int lessonId) {
        ContentValues values = new ContentValues();
        values.put(LessonEntry.COLUMN_NAME_VISIBLE, 1);
        String selection = LessonEntry.COLUMN_NAME_LESSON_ID + " = ?";
        String[] selectionArgs = {Integer.toString(lessonId)};
        database.update(LessonEntry.TABLE_NAME, values, selection, selectionArgs);
    }

    public Lesson[] getLessons(String date) {
        String[] projection = {
                LessonEntry._ID,
                LessonEntry.COLUMN_NAME_LESSON_ID,
                LessonEntry.COLUMN_NAME_SUBJECT,
                LessonEntry.COLUMN_NAME_DATE,
                LessonEntry.COLUMN_NAME_START_TIME,
                LessonEntry.COLUMN_NAME_END_TIME,
                LessonEntry.COLUMN_NAME_ROOM,
                LessonEntry.COLUMN_NAME_TEACHER,
                LessonEntry.COLUMN_NAME_CLASS_NAME,
                LessonEntry.COLUMN_NAME_SCHEDULE_ID,
                LessonEntry.COLUMN_NAME_SCHEDULE_TYPE,
                LessonEntry.COLUMN_NAME_VISIBLE,
        };

        String selection = LessonEntry.COLUMN_NAME_DATE + " = ? AND " +
                LessonEntry.COLUMN_NAME_VISIBLE + " = ?";
        String[] selectionArgs = {date, Integer.toString(1)};
        String sortOrder = LessonEntry.COLUMN_NAME_START_TIME + ", " +
                LessonEntry.COLUMN_NAME_END_TIME + ", " + LessonEntry.COLUMN_NAME_SUBJECT;

        Cursor cursor = database.query(
                LessonEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
        Lesson[] lessons = new Lesson[cursor.getCount()];
        int i = 0;
        while (cursor.moveToNext()) {
            lessons[i] = new Lesson(cursor.getLong(0), cursor.getInt(1), cursor.getString(2),
                    cursor.getString(3), cursor.getString(4), cursor.getString(5),
                    cursor.getString(6), cursor.getString(7), cursor.getString(8), cursor.getInt(9),
                    cursor.getInt(10), cursor.getInt(11));
            i++;
        }
        cursor.close();
        return lessons;
    }

    public Lesson[] getLessonForCompare(Date date, int scheduleId) {
        String[] weekDates = getWeekDates(date);
        String[] projection = {
                LessonEntry._ID,
                LessonEntry.COLUMN_NAME_LESSON_ID,
                LessonEntry.COLUMN_NAME_SUBJECT,
                LessonEntry.COLUMN_NAME_DATE,
                LessonEntry.COLUMN_NAME_START_TIME,
                LessonEntry.COLUMN_NAME_END_TIME,
                LessonEntry.COLUMN_NAME_ROOM,
                LessonEntry.COLUMN_NAME_TEACHER,
                LessonEntry.COLUMN_NAME_CLASS_NAME,
                LessonEntry.COLUMN_NAME_SCHEDULE_ID,
                LessonEntry.COLUMN_NAME_SCHEDULE_TYPE,
                LessonEntry.COLUMN_NAME_VISIBLE,
        };
        String selection = LessonEntry.COLUMN_NAME_DATE + " >= ? AND " +
                LessonEntry.COLUMN_NAME_DATE + " <= ? AND " +
                LessonEntry.COLUMN_NAME_SCHEDULE_ID + " = ?";
        String[] selectionArgs = {weekDates[0], weekDates[1], Integer.toString(scheduleId)};
        String sortOrder = LessonEntry.COLUMN_NAME_START_TIME + ", " +
                LessonEntry.COLUMN_NAME_END_TIME + ", " + LessonEntry.COLUMN_NAME_SUBJECT;

        Cursor cursor = database.query(
                LessonEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
        Lesson[] lessons = new Lesson[cursor.getCount()];
        int i = 0;
        while (cursor.moveToNext()) {
            lessons[i] = new Lesson(cursor.getLong(0), cursor.getInt(1), cursor.getString(2),
                    cursor.getString(3), cursor.getString(4), cursor.getString(5),
                    cursor.getString(6), cursor.getString(7), cursor.getString(8), cursor.getInt(9),
                    cursor.getInt(10), cursor.getInt(11));
            i++;
        }
        cursor.close();
        return lessons;
    }

    public void addSchedule(int id, String name, int type) {
        ContentValues values = new ContentValues();
        values.put(ScheduleEntry.COLUMN_NAME_SCHEDULE_ID, id);
        values.put(ScheduleEntry.COLUMN_NAME_SCHEDULE_NAME, name);
        values.put(ScheduleEntry.COLUMN_NAME_SCHEDULE_TYPE, type);
        database.insert(ScheduleEntry.TABLE_NAME, null, values);
    }

    public void deleteSchedule(int id) {
        String selection = ScheduleEntry.COLUMN_NAME_SCHEDULE_ID + " = ?";
        String[] selectionArgs = {Integer.toString(id)};
        database.delete(ScheduleEntry.TABLE_NAME, selection, selectionArgs);

        String selection2 = LessonEntry.COLUMN_NAME_SCHEDULE_ID + " = ?";
        database.delete(LessonEntry.TABLE_NAME, selection2, selectionArgs);
    }

    public Schedule[] getSchedules() {
        String[] projection = {
                ScheduleEntry.COLUMN_NAME_SCHEDULE_ID,
                ScheduleEntry.COLUMN_NAME_SCHEDULE_NAME,
                ScheduleEntry.COLUMN_NAME_SCHEDULE_TYPE
        };
        Cursor cursor = database.query(
                ScheduleEntry.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                null
        );
        Schedule[] schedules = new Schedule[cursor.getCount()];
        int i = 0;
        while (cursor.moveToNext()) {
            schedules[i] = new Schedule(cursor.getInt(0), cursor.getString(1), cursor.getInt(2));
            i++;
        }
        cursor.close();
        return schedules;
    }

    public boolean hasSchedules() {
        return countSchedules() > 0;
    }

    public int countSchedules() {
        String[] projection = {ScheduleEntry._ID};
        Cursor cursor = database.query(
                ScheduleEntry.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                null
        );
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    public Lesson getSingleLesson(long id) {
        String[] projection = {
                LessonEntry._ID,
                LessonEntry.COLUMN_NAME_LESSON_ID,
                LessonEntry.COLUMN_NAME_SUBJECT,
                LessonEntry.COLUMN_NAME_DATE,
                LessonEntry.COLUMN_NAME_START_TIME,
                LessonEntry.COLUMN_NAME_END_TIME,
                LessonEntry.COLUMN_NAME_ROOM,
                LessonEntry.COLUMN_NAME_TEACHER,
                LessonEntry.COLUMN_NAME_CLASS_NAME,
                LessonEntry.COLUMN_NAME_SCHEDULE_ID,
                LessonEntry.COLUMN_NAME_SCHEDULE_TYPE,
                LessonEntry.COLUMN_NAME_VISIBLE,
        };
        String selection = LessonEntry._ID + " = ?";
        String[] selectionArgs = {Long.toString(id)};

        Cursor cursor = database.query(
                LessonEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );
        Lesson lesson = null;
        if (cursor.moveToFirst()) {
            lesson = new Lesson(cursor.getLong(0), cursor.getInt(1), cursor.getString(2),
                    cursor.getString(3), cursor.getString(4), cursor.getString(5),
                    cursor.getString(6), cursor.getString(7), cursor.getString(8), cursor.getInt(9),
                    cursor.getInt(10), cursor.getInt(11));
        }
        cursor.close();
        return lesson;
    }

    public Lesson[] getHiddenLessons() {
        String[] projection = {
                LessonEntry._ID,
                LessonEntry.COLUMN_NAME_LESSON_ID,
                LessonEntry.COLUMN_NAME_SUBJECT,
                LessonEntry.COLUMN_NAME_DATE,
                LessonEntry.COLUMN_NAME_START_TIME,
                LessonEntry.COLUMN_NAME_END_TIME,
                LessonEntry.COLUMN_NAME_ROOM,
                LessonEntry.COLUMN_NAME_TEACHER,
                LessonEntry.COLUMN_NAME_CLASS_NAME,
                LessonEntry.COLUMN_NAME_SCHEDULE_ID,
                LessonEntry.COLUMN_NAME_SCHEDULE_TYPE,
                LessonEntry.COLUMN_NAME_VISIBLE,
        };

        String selection = LessonEntry.COLUMN_NAME_VISIBLE + " = ?";
        String[] selectionArgs = {Integer.toString(0)};
        String sortOrder = LessonEntry.COLUMN_NAME_SUBJECT;

        Cursor cursor = database.query(
                LessonEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
        Lesson[] lessons = new Lesson[cursor.getCount()];
        int i = 0;
        while (cursor.moveToNext()) {
            lessons[i] = new Lesson(cursor.getLong(0), cursor.getInt(1), cursor.getString(2),
                    cursor.getString(3), cursor.getString(4), cursor.getString(5),
                    cursor.getString(6), cursor.getString(7), cursor.getString(8), cursor.getInt(9),
                    cursor.getInt(10), cursor.getInt(11));
            i++;
        }
        cursor.close();
        return lessons;
    }

    public int getPositionByScheduleId(int id) {
        String[] projection = {ScheduleEntry.COLUMN_NAME_SCHEDULE_ID};
        Cursor cursor = database.query(
                ScheduleEntry.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                null
        );
        while (cursor.moveToNext()) {
            if (id == cursor.getInt(0)) {
                int i = cursor.getPosition();
                cursor.close();
                return i;
            }
        }
        cursor.close();
        deleteSchedule(id);
        return 0;
    }

    public boolean isFetched(Date date) {
        String[] weekDates = getWeekDates(date);
        String[] projection = {FetchedDateEntry.COLUMN_NAME_DATE};
        String selection = FetchedDateEntry.COLUMN_NAME_DATE + " >= ? AND " +
                FetchedDateEntry.COLUMN_NAME_DATE + " <= ?";
        String[] selectionArgs = {weekDates[0], weekDates[1]};
        Cursor cursor = database.query(
                FetchedDateEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );
        boolean bool = cursor.getCount() > 0;
        cursor.close();
        return bool;
    }

    public void addFetched(Date date) {
        String[] weekdates = getWeekDates(date);
        for (String weekdate : weekdates) {
            ContentValues values = new ContentValues();
            values.put(FetchedDateEntry.COLUMN_NAME_DATE, weekdate);
            database.insertWithOnConflict(FetchedDateEntry.TABLE_NAME, null, values,
                    SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    private void deleteOldFetched(Date date) {
        String[] weekDates = getWeekDates(date);
        String selection = FetchedDateEntry.COLUMN_NAME_DATE + "< ?";
        String[] selectionArgs = {weekDates[1]};
        database.delete(FetchedDateEntry.TABLE_NAME, selection, selectionArgs);
    }

    public void clearFetched() {
        database.delete(FetchedDateEntry.TABLE_NAME, null, null);
    }

    public void deleteFetched(Date date) {
        String[] weekDates = getWeekDates(date);
        String selection = FetchedDateEntry.COLUMN_NAME_DATE + " >= ? AND "
                + FetchedDateEntry.COLUMN_NAME_DATE + " <= ?";
        String[] selectionArgs = {weekDates[0], weekDates[1]};
        database.delete(FetchedDateEntry.TABLE_NAME, selection, selectionArgs);
    }

    private String parseDate(Date date) {
        DateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);
        return simpleDateFormat.format(date);
    }

    private String[] getWeekDates(Date date) {
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(date);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        String lowestDate = parseDate(calendar.getTime());
        calendar.add(Calendar.DATE, 4);
        String highestDate = parseDate(calendar.getTime());
        return new String[]{lowestDate, highestDate};
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if (oldVersion == 7 && newVersion == 8) {
            database.execSQL("ALTER TABLE schedules RENAME TO tmp_schedules");
            database.execSQL(SQL_CREATE_SCHEDULE_ENTRIES);
            database.execSQL("INSERT INTO " + ScheduleEntry.TABLE_NAME + " (" +
                    ScheduleEntry.COLUMN_NAME_SCHEDULE_ID + ", " +
                    ScheduleEntry.COLUMN_NAME_SCHEDULE_NAME + ", " +
                    ScheduleEntry.COLUMN_NAME_SCHEDULE_TYPE + ") " +
                    "SELECT schedule_id, schedule_name, schedule_type FROM tmp_schedules");
            database.execSQL("DROP TABLE tmp_schedules");
        } else {
            database.execSQL("DROP TABLE schedules");
            database.execSQL(SQL_CREATE_SCHEDULE_ENTRIES);
        }
        database.execSQL("DROP TABLE subject");
        database.execSQL("DROP TABLE fetched_dates");

        database.execSQL(SQL_CREATE_LESSON_ENTRIES);
        database.execSQL(SQL_CREATE_FETCHED_DATES_ENTRIES);
    }

    private class ScheduleEntry implements BaseColumns {
        static final String TABLE_NAME = "schedules";
        static final String COLUMN_NAME_SCHEDULE_ID = "schedule_id";
        static final String COLUMN_NAME_SCHEDULE_NAME = "name";
        static final String COLUMN_NAME_SCHEDULE_TYPE = "type";
    }

    private class LessonEntry implements BaseColumns {
        static final String TABLE_NAME = "lessons";
        static final String COLUMN_NAME_LESSON_ID = "lesson_id";
        static final String COLUMN_NAME_SUBJECT = "subject";
        static final String COLUMN_NAME_DATE = "date";
        static final String COLUMN_NAME_START_TIME = "start_time";
        static final String COLUMN_NAME_END_TIME = "end_time";
        static final String COLUMN_NAME_ROOM = "room";
        static final String COLUMN_NAME_TEACHER = "teacher";
        static final String COLUMN_NAME_CLASS_NAME = "class_name";
        static final String COLUMN_NAME_SCHEDULE_ID = "schedule_id";
        static final String COLUMN_NAME_SCHEDULE_TYPE = "schedule_type";
        static final String COLUMN_NAME_VISIBLE = "visible";
    }

    private class FetchedDateEntry implements BaseColumns {
        static final String TABLE_NAME = "fetched_dates";
        static final String COLUMN_NAME_DATE = "date";
    }
}