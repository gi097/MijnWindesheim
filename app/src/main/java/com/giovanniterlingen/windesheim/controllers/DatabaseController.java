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
package com.giovanniterlingen.windesheim.controllers;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.Constants;
import com.giovanniterlingen.windesheim.models.Lesson;
import com.giovanniterlingen.windesheim.models.Schedule;
import com.giovanniterlingen.windesheim.utils.TimeUtils;

import java.util.Date;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class DatabaseController extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 10;
    private static final String DATABASE_NAME = "schedulestore.db";
    private static final String SQL_CREATE_SCHEDULE_ENTRIES =
            "CREATE TABLE " + ScheduleEntry.TABLE_NAME + " (" +
                    ScheduleEntry._ID + " INTEGER PRIMARY KEY," +
                    ScheduleEntry.COLUMN_NAME_SCHEDULE_ID + " TEXT UNIQUE," +
                    ScheduleEntry.COLUMN_NAME_SCHEDULE_NAME + " TEXT," +
                    ScheduleEntry.COLUMN_NAME_SCHEDULE_TYPE + " INTEGER);";
    private static final String SQL_CREATE_LESSON_ENTRIES =
            "CREATE TABLE " + LessonEntry.TABLE_NAME + " (" +
                    LessonEntry._ID + " INTEGER PRIMARY KEY," +
                    LessonEntry.COLUMN_NAME_LESSON_ID + " INTEGER," +
                    LessonEntry.COLUMN_NAME_SUBJECT + " TEXT," +
                    LessonEntry.COLUMN_NAME_DATE + " TEXT," +
                    LessonEntry.COLUMN_NAME_START_TIME + " INTEGER," +
                    LessonEntry.COLUMN_NAME_END_TIME + " INTEGER," +
                    LessonEntry.COLUMN_NAME_ROOM + " TEXT," +
                    LessonEntry.COLUMN_NAME_TEACHER + " TEXT," +
                    LessonEntry.COLUMN_NAME_CLASS_NAME + " TEXT," +
                    LessonEntry.COLUMN_NAME_SCHEDULE_ID + " INTEGER," +
                    LessonEntry.COLUMN_NAME_SCHEDULE_TYPE + " INTEGER," +
                    LessonEntry.COLUMN_NAME_VISIBLE + " INTEGER)";
    private static volatile DatabaseController Instance = null;
    private final SQLiteDatabase database;

    private DatabaseController() {
        super(ApplicationLoader.applicationContext, DATABASE_NAME, null, DATABASE_VERSION);
        database = this.getWritableDatabase();
    }

    public static DatabaseController getInstance() {
        DatabaseController localInstance = Instance;
        if (localInstance == null) {
            synchronized (DatabaseController.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new DatabaseController();
                }
            }
        }
        return localInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(SQL_CREATE_SCHEDULE_ENTRIES);
        database.execSQL(SQL_CREATE_LESSON_ENTRIES);
    }

    void saveLessons(Lesson[] lessons) {
        for (Lesson lesson : lessons) {
            saveLesson(lesson);
        }
    }

    private void saveLesson(Lesson lesson) {
        ContentValues values = new ContentValues();
        values.put(LessonEntry.COLUMN_NAME_LESSON_ID, lesson.getId());
        values.put(LessonEntry.COLUMN_NAME_SUBJECT, lesson.getSubject());
        values.put(LessonEntry.COLUMN_NAME_DATE, TimeUtils.getYearMonthDayDateFormat()
                .format(lesson.getStartTime()));
        values.put(LessonEntry.COLUMN_NAME_START_TIME, lesson.getStartTime().getTime());
        values.put(LessonEntry.COLUMN_NAME_END_TIME, lesson.getEndTime().getTime());
        values.put(LessonEntry.COLUMN_NAME_ROOM, lesson.getRoom());
        values.put(LessonEntry.COLUMN_NAME_TEACHER, lesson.getTeacher());
        values.put(LessonEntry.COLUMN_NAME_CLASS_NAME, lesson.getClassName());
        values.put(LessonEntry.COLUMN_NAME_SCHEDULE_ID, lesson.getScheduleId());
        values.put(LessonEntry.COLUMN_NAME_SCHEDULE_TYPE, lesson.getScheduleType().ordinal());
        values.put(LessonEntry.COLUMN_NAME_VISIBLE, lesson.isVisible() ? 1 : 0);
        database.insert(LessonEntry.TABLE_NAME, null, values);
    }

    void clearScheduleData(String id) {
        String selection = "(" + LessonEntry.COLUMN_NAME_DATE + " < ? OR " + LessonEntry.COLUMN_NAME_DATE
                + " >= ?) AND " + LessonEntry.COLUMN_NAME_SCHEDULE_ID + " = ?";
        String[] weekDates = TimeUtils.getWeekDates(new Date());
        String[] selectionArgs = {
                weekDates[0], // Monday in the current week
                TimeUtils.getYearMonthDayDateFormat()
                        .format(new Date()), // Today
                id
        };
        database.delete(LessonEntry.TABLE_NAME, selection, selectionArgs);
    }

    public void hideLesson(Lesson lesson) {
        ContentValues values = new ContentValues();
        values.put(LessonEntry.COLUMN_NAME_VISIBLE, 0);
        String selection = LessonEntry.COLUMN_NAME_SUBJECT + " = ? AND " +
                LessonEntry.COLUMN_NAME_TEACHER + " = ?";
        String[] selectionArgs = {lesson.getSubject(), lesson.getTeacher()};
        database.update(LessonEntry.TABLE_NAME, values, selection, selectionArgs);
    }

    public void restoreLesson(Lesson lesson) {
        ContentValues values = new ContentValues();
        values.put(LessonEntry.COLUMN_NAME_VISIBLE, 1);
        String selection = LessonEntry.COLUMN_NAME_SUBJECT + " = ? AND " +
                LessonEntry.COLUMN_NAME_TEACHER + " = ?";
        String[] selectionArgs = {lesson.getSubject(), lesson.getTeacher()};
        database.update(LessonEntry.TABLE_NAME, values, selection, selectionArgs);
    }

    public Lesson[] getLessons(Date date) {
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
        String[] selectionArgs = {TimeUtils.getYearMonthDayDateFormat().format(date),
                Integer.toString(1)};
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
            Lesson lesson = new Lesson();
            lesson.setId(cursor.getString(cursor.getColumnIndex(LessonEntry.COLUMN_NAME_LESSON_ID)));
            lesson.setSubject(cursor.getString(cursor.getColumnIndex(LessonEntry.COLUMN_NAME_SUBJECT)));
            lesson.setStartTime(new Date(cursor.getLong(cursor.getColumnIndex(LessonEntry.COLUMN_NAME_START_TIME))));
            lesson.setEndTime(new Date(cursor.getLong(cursor.getColumnIndex(LessonEntry.COLUMN_NAME_END_TIME))));
            lesson.setRoom(cursor.getString(cursor.getColumnIndex(LessonEntry.COLUMN_NAME_ROOM)));
            lesson.setTeacher(cursor.getString(cursor.getColumnIndex(LessonEntry.COLUMN_NAME_TEACHER)));
            lesson.setClassName(cursor.getString(cursor.getColumnIndex(LessonEntry.COLUMN_NAME_CLASS_NAME)));
            lesson.setScheduleId(cursor.getString(cursor.getColumnIndex(LessonEntry.COLUMN_NAME_SCHEDULE_ID)));
            lesson.setScheduleType(Constants.SCHEDULE_TYPE.values()[cursor.getInt(cursor.getColumnIndex(LessonEntry.COLUMN_NAME_SCHEDULE_TYPE))]);
            lesson.setVisible(cursor.getInt(cursor.getColumnIndex(LessonEntry.COLUMN_NAME_VISIBLE)) == 1);
            lessons[i] = lesson;
            i++;
        }
        cursor.close();
        return lessons;
    }

    /**
     * Get lessons for this week to check if they are changed
     */
    Lesson[] getLessonsForCompare(String scheduleId) {
        String[] weekDates = TimeUtils.getWeekDates(new Date());
        String[] projection = {
                LessonEntry._ID,
                LessonEntry.COLUMN_NAME_LESSON_ID,
                LessonEntry.COLUMN_NAME_SUBJECT,
                LessonEntry.COLUMN_NAME_START_TIME,
                LessonEntry.COLUMN_NAME_END_TIME,
                LessonEntry.COLUMN_NAME_ROOM,
                LessonEntry.COLUMN_NAME_TEACHER,
                LessonEntry.COLUMN_NAME_CLASS_NAME,
                LessonEntry.COLUMN_NAME_SCHEDULE_ID,
                LessonEntry.COLUMN_NAME_SCHEDULE_TYPE,
                LessonEntry.COLUMN_NAME_VISIBLE,
        };
        String selection = "(" + LessonEntry.COLUMN_NAME_DATE + " >= ? OR " +
                LessonEntry.COLUMN_NAME_DATE + " <= ?) AND " +
                LessonEntry.COLUMN_NAME_SCHEDULE_ID + " = ?";
        String[] selectionArgs = {weekDates[0], weekDates[1], scheduleId};
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
            Lesson lesson = new Lesson();
            lesson.setId(cursor.getString(cursor.getColumnIndex(LessonEntry.COLUMN_NAME_LESSON_ID)));
            lesson.setSubject(cursor.getString(cursor.getColumnIndex(LessonEntry.COLUMN_NAME_SUBJECT)));
            lesson.setStartTime(new Date(cursor.getLong(cursor.getColumnIndex(LessonEntry.COLUMN_NAME_START_TIME))));
            lesson.setEndTime(new Date(cursor.getLong(cursor.getColumnIndex(LessonEntry.COLUMN_NAME_END_TIME))));
            lesson.setRoom(cursor.getString(cursor.getColumnIndex(LessonEntry.COLUMN_NAME_ROOM)));
            lesson.setTeacher(cursor.getString(cursor.getColumnIndex(LessonEntry.COLUMN_NAME_TEACHER)));
            lesson.setClassName(cursor.getString(cursor.getColumnIndex(LessonEntry.COLUMN_NAME_CLASS_NAME)));
            lesson.setScheduleId(cursor.getString(cursor.getColumnIndex(LessonEntry.COLUMN_NAME_SCHEDULE_ID)));
            lesson.setScheduleType(Constants.SCHEDULE_TYPE.values()[cursor.getInt(cursor.getColumnIndex(LessonEntry.COLUMN_NAME_SCHEDULE_TYPE))]);
            lesson.setVisible(cursor.getInt(cursor.getColumnIndex(LessonEntry.COLUMN_NAME_VISIBLE)) == 1);
            lessons[i] = lesson;
            i++;
        }
        cursor.close();
        return lessons;
    }

    public void addSchedule(String id, String name, Constants.SCHEDULE_TYPE type) throws SQLiteConstraintException {
        ContentValues values = new ContentValues();
        values.put(ScheduleEntry.COLUMN_NAME_SCHEDULE_ID, id);
        values.put(ScheduleEntry.COLUMN_NAME_SCHEDULE_NAME, name);
        values.put(ScheduleEntry.COLUMN_NAME_SCHEDULE_TYPE, type.ordinal());
        database.insertOrThrow(ScheduleEntry.TABLE_NAME, null, values);
    }

    public void deleteSchedule(String id) {
        String selection = ScheduleEntry.COLUMN_NAME_SCHEDULE_ID + " = ?";
        String[] selectionArgs = {id};
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
        String group = ScheduleEntry.COLUMN_NAME_SCHEDULE_ID;
        Cursor cursor = database.query(
                ScheduleEntry.TABLE_NAME,
                projection,
                null,
                null,
                group,
                null,
                null
        );
        Schedule[] schedules = new Schedule[cursor.getCount()];
        int i = 0;
        while (cursor.moveToNext()) {
            Schedule schedule = new Schedule();
            schedule.setId(cursor.getString(cursor.getColumnIndex(ScheduleEntry.COLUMN_NAME_SCHEDULE_ID)));
            schedule.setName(cursor.getString(cursor.getColumnIndex(ScheduleEntry.COLUMN_NAME_SCHEDULE_NAME)));
            schedule.setType(Constants.SCHEDULE_TYPE.values()[cursor.getInt(cursor.getColumnIndex(ScheduleEntry.COLUMN_NAME_SCHEDULE_TYPE))]);
            schedules[i] = schedule;
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

    public Lesson[] getHiddenLessons() {
        String[] projection = {
                LessonEntry._ID,
                LessonEntry.COLUMN_NAME_LESSON_ID,
                LessonEntry.COLUMN_NAME_SUBJECT,
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
        String group = LessonEntry.COLUMN_NAME_SUBJECT + " AND " + LessonEntry.COLUMN_NAME_TEACHER;
        String sortOrder = LessonEntry.COLUMN_NAME_SUBJECT;

        Cursor cursor = database.query(
                LessonEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                group,
                null,
                sortOrder
        );
        Lesson[] lessons = new Lesson[cursor.getCount()];
        int i = 0;
        while (cursor.moveToNext()) {
            Lesson lesson = new Lesson();
            lesson.setId(cursor.getString(cursor.getColumnIndex(LessonEntry.COLUMN_NAME_LESSON_ID)));
            lesson.setSubject(cursor.getString(cursor.getColumnIndex(LessonEntry.COLUMN_NAME_SUBJECT)));
            lesson.setStartTime(new Date(cursor.getLong(cursor.getColumnIndex(LessonEntry.COLUMN_NAME_START_TIME))));
            lesson.setEndTime(new Date(cursor.getLong(cursor.getColumnIndex(LessonEntry.COLUMN_NAME_END_TIME))));
            lesson.setRoom(cursor.getString(cursor.getColumnIndex(LessonEntry.COLUMN_NAME_ROOM)));
            lesson.setTeacher(cursor.getString(cursor.getColumnIndex(LessonEntry.COLUMN_NAME_TEACHER)));
            lesson.setClassName(cursor.getString(cursor.getColumnIndex(LessonEntry.COLUMN_NAME_CLASS_NAME)));
            lesson.setScheduleId(cursor.getString(cursor.getColumnIndex(LessonEntry.COLUMN_NAME_SCHEDULE_ID)));
            lesson.setScheduleType(Constants.SCHEDULE_TYPE.values()[cursor.getInt(cursor.getColumnIndex(LessonEntry.COLUMN_NAME_SCHEDULE_TYPE))]);
            lesson.setVisible(cursor.getInt(cursor.getColumnIndex(LessonEntry.COLUMN_NAME_VISIBLE)) == 1);
            lessons[i] = lesson;
            i++;
        }
        cursor.close();
        return lessons;
    }

    public int getPositionByScheduleId(String id) {
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
            if (id.equals(cursor.getString(cursor
                    .getColumnIndex(ScheduleEntry.COLUMN_NAME_SCHEDULE_ID)))) {
                int i = cursor.getPosition();
                cursor.close();
                return i;
            }
        }
        cursor.close();
        return 0;
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if (oldVersion == 9 && newVersion == 10) {
            database.execSQL("DROP TABLE IF EXISTS " + LessonEntry.TABLE_NAME);
            database.execSQL(SQL_CREATE_LESSON_ENTRIES);
            return;
        }
        database.execSQL("DROP TABLE IF EXISTS fetched_dates");
        database.execSQL("DROP TABLE IF EXISTS " + ScheduleEntry.TABLE_NAME);
        database.execSQL("DROP TABLE IF EXISTS " + LessonEntry.TABLE_NAME);

        database.execSQL(SQL_CREATE_SCHEDULE_ENTRIES);
        database.execSQL(SQL_CREATE_LESSON_ENTRIES);
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
}
