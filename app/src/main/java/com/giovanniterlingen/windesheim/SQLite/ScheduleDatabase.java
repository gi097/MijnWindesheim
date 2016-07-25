/**
 * Copyright (c) 2016 Giovanni Terlingen
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class ScheduleDatabase extends SQLiteOpenHelper {

    private static SQLiteDatabase database;

    public ScheduleDatabase(Context context) {
        super(context, "schedulestore.db", null, 5);
    }

    /**
     * Open the database once to keep communicating with it. According to Google, it isn't a bad
     * idea to leave a database open.
     */
    public void open() {
        database = this.getWritableDatabase();
    }

    /**
     * Create needed tables, only if they don't exist yet.
     *
     * @param database The opened database object.
     */
    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE `subject` (_id INTEGER PRIMARY KEY AUTOINCREMENT, `component_id` TEXT, `date` TEXT, `start` TEXT, `end` TEXT, `name` TEXT, `room` TEXT, `component` TEXT, `class_id` TEXT, `visible` INTEGER)");
        database.execSQL("CREATE TABLE `fetched_dates` (`date` TEXT UNIQUE)");
    }

    /**
     * Insert all schedule data in the database.
     *
     * @param id          The id from the lesson
     * @param date        The date of the lesson
     * @param start       The start time of the lesson
     * @param end         The end time of the lesson
     * @param name        The name of the lesson
     * @param room        The location of the lesson.
     * @param component   Class or teacher name
     * @param componentId The lesson's id
     * @param visible     Defines if the user has hidden or shown the lesson
     */
    public void saveScheduleData(String id, String date, String start, String end, String name, String room, String component, String componentId, int visible) {
        database.execSQL("INSERT INTO `subject` (`component_id`, `date`, `start`, `end`, `name`, `room`, `component`, `class_id`, `visible`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", new String[]{id, date, start, end, name, room, component, componentId, Integer.toString(visible)});
    }

    /**
     * Clears all lessons for one week.
     *
     * @param date The date in the week we want to delete.
     */
    public void clearScheduleData(Date date) {
        String[] weekDates = getWeekDates(date);
        database.execSQL("DELETE FROM `subject` WHERE `date` >= ? AND `date` <= ? AND `visible` = 1", new String[]{weekDates[0], weekDates[1]});
    }

    /**
     * Clears all schedule data we don't need anymore. It will only delete visible lessons.
     *
     * @param date The date we need to keep, everything before that date will be deleted.
     */
    public void clearOldScheduleData(String date) {
        database.execSQL("DELETE FROM `subject` WHERE `date` < ? AND `visible` = 1", new String[]{date});
    }

    /**
     * Sets a lesson to unvisible, so the user will not see the lesson anymore.
     *
     * @param id The database id that needs to be set to unvisible
     */
    public void hideLesson(long id) {
        Cursor cursor = database.rawQuery("SELECT `component_id` FROM `subject` WHERE `_id` = ?", new String[]{Long.toString(id)});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                database.execSQL("UPDATE `subject` SET `visible` = 0 WHERE `component_id` = ? AND `visible` = 1", new String[]{cursor.getString(0)});
            }
            cursor.close();
        }
    }

    /**
     * Sets a lesson to visible again, so the user will see it.
     *
     * @param id The database id that needs to be set to visible
     */
    public void restoreLessons(long id) {
        Cursor cursor = database.rawQuery("SELECT `component_id` FROM `subject` WHERE `_id` = ?", new String[]{(Long.toString(id))});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                database.execSQL("UPDATE `subject` SET `visible` = 1 WHERE `component_id` = ? AND `visible` = 0", new String[]{cursor.getString(0)});
            }
            cursor.close();
        }
    }

    /**
     * Get all saved lessons from the database.
     *
     * @param date        The date of the lessons we want to see
     * @param componentId The id of the schedule
     * @return The Cursor containing all lessons.
     */
    public Cursor getLessons(String date, String componentId) {
        return database.rawQuery("SELECT _id, `component_id`, `date`, MIN(`start`), MAX(`end`), `name`, MAX(`room`), `component`, `class_id` FROM `subject` WHERE `date` = ? AND `class_id` = ? AND `visible` = 1 GROUP BY `component_id` ORDER BY `start`, `name`", new String[]{date, componentId});
    }

    /**
     * Get all saved lessons from the database, except visibility
     *
     * @param date        The date of the lessons we want to see
     * @param componentId The id of the schedule
     * @return The Cursor containing all lessons.
     */
    public Cursor getLessonsForCompare(String date, String componentId) {
        return database.rawQuery("SELECT `component_id`, `date`, `start`, `end`, `name`, `room`, `component`, `class_id` FROM `subject` WHERE `date` = ? AND `class_id` = ?", new String[]{date, componentId});
    }

    /**
     * Get a lesson by the database id
     *
     * @param id The database id
     * @return The cursor containing a lesson
     */
    public Cursor getSingleLesson(long id) {
        return database.rawQuery("SELECT MIN(`start`), MAX(`end`), `name`, MAX(`room`) FROM `subject` WHERE `_id` = ?", new String[]{Long.toString(id)});
    }

    /**
     * Gets all hidden lessons for the adapter.
     *
     * @return the Cursor containing all hidden lessons
     */
    public Cursor getFilteredLessonsForAdapter() {
        return database.rawQuery("SELECT `_id`, `name`, `component` FROM `subject` WHERE `visible` = 0 GROUP BY `component_id` ORDER BY `name`", null);
    }

    /**
     * Gets unvisible lessons.
     *
     * @return the lessons which are hidden
     */
    public Cursor getFilteredLessons() {
        return database.rawQuery("SELECT `component_id` FROM `subject` WHERE `visible` = 0 GROUP BY `component_id`", null);
    }

    /**
     * Checks if the database contains lessons for a specific week.
     *
     * @param date The date in the week we want to check.
     * @return true or false, depending on cursor size.
     */
    public boolean containsWeek(Date date) {
        String[] weekDates = getWeekDates(date);
        Cursor cursor = database.rawQuery("SELECT _id FROM `subject` WHERE `date` >= ? AND `date` <= ? AND `visible` = 1", new String[]{weekDates[0], weekDates[1]});
        boolean bool = cursor.getCount() > 0;
        cursor.close();
        return bool;
    }

    /**
     * Checks if a week was already fetched from the server.
     *
     * @param date The date within a week
     * @return true if the database contains values, which means the schedule was already fetched
     * before
     */
    public boolean isFetched(Date date) {
        String[] weekDates = getWeekDates(date);
        Cursor cursor = database.rawQuery("SELECT `date` FROM `fetched_dates` WHERE `date` >= ? AND `date` <= ?", new String[]{weekDates[0], weekDates[1]});
        boolean bool = cursor.getCount() > 0;
        cursor.close();
        return bool;
    }

    /**
     * Means we fetched the schedule for a specific day.
     *
     * @param date The date we fetched.
     */
    public void addFetched(Date date) {
        database.execSQL("INSERT OR IGNORE INTO `fetched_dates` VALUES (?)", new String[]{parseDate(date)});
    }

    /**
     * Clears old dates which specifies the fetch times.
     *
     * @param date The last date we need to keep, everything smaller than that date will be deleted.
     */
    public void deleteOldFetched(String date) {
        database.execSQL("DELETE FROM `fetched_dates` WHERE `date` < ?", new String[]{date});
    }

    /**
     * Parsed the Date object to a string
     *
     * @param date The date we want to parse
     * @return The parsed date
     */
    @SuppressLint("SimpleDateFormat")
    private String parseDate(Date date) {
        DateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        return simpleDateFormat.format(date);
    }

    /**
     * Creates an array of all the lowest and highest day in a week.
     *
     * @param date The date in a week
     * @return An array of the lowest and highest date
     */
    private String[] getWeekDates(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - calendar.getFirstDayOfWeek();
        calendar.add(Calendar.DAY_OF_MONTH, -dayOfWeek);
        String lowestDate = parseDate(calendar.getTime());
        calendar.add(Calendar.DAY_OF_MONTH, 6);
        String highestDate = parseDate(calendar.getTime());
        return new String[]{lowestDate, highestDate};
    }

    /**
     * Clear the database if the scheme get's updated.
     */
    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        database.execSQL("DROP TABLE `subject`");
        // database.execSQL("DROP TABLE `fetched_dates`");
        database.execSQL("CREATE TABLE `subject` (_id INTEGER PRIMARY KEY AUTOINCREMENT, `component_id` TEXT, `date` TEXT, `start` TEXT, `end` TEXT, `name` TEXT, `room` TEXT, `component` TEXT, `class_id` TEXT, `visible` INTEGER)");
        database.execSQL("CREATE TABLE `fetched_dates` (`date` TEXT UNIQUE)");
    }

}