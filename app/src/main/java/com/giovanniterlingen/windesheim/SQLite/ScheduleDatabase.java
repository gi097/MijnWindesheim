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

import android.content.Context;
import android.database.Cursor;

import com.giovanniterlingen.windesheim.objects.Schedule;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import io.requery.android.database.sqlite.SQLiteDatabase;
import io.requery.android.database.sqlite.SQLiteOpenHelper;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class ScheduleDatabase extends SQLiteOpenHelper {

    private SQLiteDatabase database;

    public ScheduleDatabase(Context context) {
        super(context, "schedulestore.db", null, 7);
    }

    public void open() {
        database = this.getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE subject (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "component_id INTEGER, date TEXT, start TEXT, end TEXT, name TEXT, room TEXT, " +
                "component TEXT, class_id INTEGER, visible INTEGER)");
        database.execSQL("CREATE TABLE fetched_dates (date TEXT UNIQUE)");
        database.execSQL("CREATE TABLE schedules (schedule_id INTEGER UNIQUE, schedule_name TEXT, " +
                "schedule_type INTEGER)");
    }

    public void saveScheduleData(int id, String date, String start, String end, String name,
                                 String room, String component, int componentId, int visible) {
        database.execSQL("INSERT INTO subject (component_id, date, start, end, name, room, " +
                "component, class_id, visible) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", new
                Object[]{id, date, start, end, name, room, component, componentId, visible});
    }

    public void clearScheduleData(Date date, int id) {
        String[] weekDates = getWeekDates(date);
        database.execSQL("DELETE FROM subject WHERE date >= ? AND date <= ? AND class_id = ? " +
                "AND visible = 1", new Object[]{weekDates[0], weekDates[1], id});
    }

    public void clearOldScheduleData(Date date) {
        database.execSQL("DELETE FROM subject WHERE date < ? AND visible = 1",
                new String[]{parseDate(date)});
        deleteOldFetched(date);
    }

    public void hideLesson(long id) {
        Cursor cursor = database.rawQuery("SELECT component_id FROM subject WHERE _id = ?",
                new Object[]{id});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                database.execSQL("UPDATE subject SET visible = 0 WHERE component_id = ? AND " +
                        "visible = 1", new String[]{cursor.getString(0)});
            }
            cursor.close();
        }
    }

    public void restoreLessons(long id) {
        Cursor cursor = database.rawQuery("SELECT component_id FROM subject WHERE _id = ?",
                new Object[]{id});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                database.execSQL("UPDATE subject SET visible = 1 WHERE component_id = ? AND " +
                        "visible = 0", new String[]{cursor.getString(0)});
            }
            cursor.close();
        }
    }

    public Cursor getLessons(String date) {
        String query = "WITH cte AS (SELECT _id, component_id, date, start, end, name, " +
                "room, component, visible, class_id FROM subject t WHERE NOT EXISTS (SELECT NULL " +
                "FROM subject t2 WHERE t2.component_id = t.component_id AND t2.end = t.start AND " +
                "t.date = t2.date) UNION ALL SELECT t._id, t.component_id, t.date, cte.start, " +
                "t.end, t.name, t.room, t.component, t.visible, t.class_id FROM cte JOIN subject t ON " +
                "t.component_id = cte.component_id AND t.start = cte.end AND t.date = cte.date) " +
                "SELECT _id, component_id, date, MIN(start), MAX(end), name, MAX(room), " +
                "component, class_id, s.schedule_id FROM cte " +
                "INNER JOIN schedules s ON (cte.class_id = s.schedule_id) " +
                "WHERE date = ? AND visible = 1 " +
                "GROUP BY component_id, start " +
                "ORDER BY start, end, name";
        return database.rawQuery(query, new String[]{date});
    }

    public void addSchedule(int id, String name, int type) {
        database.execSQL("INSERT INTO schedules (schedule_id, schedule_name, schedule_type) " +
                "VALUES (?, ?, ?)", new Object[]{id, name, type});
    }

    public void deleteSchedule(int id) {
        database.execSQL("DELETE FROM schedules WHERE schedule_id = ?", new Object[]{id});
        database.execSQL("DELETE FROM subject WHERE class_id = ?", new Object[]{id});
    }

    public Schedule[] getSchedules() {
        Cursor cursor = database.rawQuery("SELECT schedule_id, schedule_name, schedule_type " +
                "FROM schedules", null);
        Schedule[] schedules = new Schedule[cursor.getCount()];
        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);
            schedules[i] = new Schedule(cursor.getString(1), cursor.getInt(0), cursor.getInt(2));
        }
        cursor.close();
        return schedules;
    }

    public boolean hasSchedules() {
        return countSchedules() > 0;
    }

    public int countSchedules() {
        Cursor cursor = database.rawQuery("SELECT schedule_id FROM schedules", null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    public Cursor getSingleLesson(String date, long id) {
        Cursor cursor = database.rawQuery("SELECT component_id FROM subject WHERE _id = ?", new
                Object[]{id});
        String componentId = null;
        while (cursor.moveToNext()) {
            componentId = cursor.getString(0);
        }
        cursor.close();
        return database.rawQuery("SELECT MIN(start), MAX(end), name, MAX(room) FROM subject WHERE" +
                " date = ? AND component_id = ? AND visible = 1 GROUP BY component_id", new
                String[]{date, componentId});
    }

    public Cursor getFilteredLessonsForAdapter() {
        return database.rawQuery("SELECT _id, name, component, class_id FROM subject WHERE " +
                "visible = 0 GROUP BY component_id ORDER BY name", null);
    }

    public Cursor getFilteredLessons() {
        return database.rawQuery("SELECT component_id FROM subject WHERE visible = 0 GROUP BY " +
                "component_id", null);
    }

    public int getPositionByScheduleId(int id) {
        Cursor cursor = database.rawQuery("SELECT schedule_id FROM schedules", null);
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
        Cursor cursor = database.rawQuery("SELECT date FROM fetched_dates WHERE date >= ? AND " +
                "date <= ?", new String[]{weekDates[0], weekDates[1]});
        boolean bool = cursor.getCount() > 0;
        cursor.close();
        return bool;
    }

    public void addFetched(Date date) {
        String[] weekdates = getWeekDates(date);
        for (String weekdate : weekdates) {
            database.execSQL("INSERT OR IGNORE INTO fetched_dates VALUES (?)",
                    new String[]{weekdate});
        }
    }

    private void deleteOldFetched(Date date) {
        String[] weekDates = getWeekDates(date);
        database.execSQL("DELETE FROM fetched_dates WHERE date < ?", new String[]{weekDates[1]});
    }

    public void clearFetched() {
        database.execSQL("DELETE FROM fetched_dates");
    }

    public void deleteFetched(Date date) {
        String[] weekDates = getWeekDates(date);
        database.execSQL("DELETE FROM fetched_dates WHERE date >= ? AND date <= ?",
                new String[]{weekDates[0], weekDates[1]});
    }

    private String parseDate(Date date) {
        DateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);
        return simpleDateFormat.format(date);
    }

    private String[] getWeekDates(Date date) {
        Calendar calendar = GregorianCalendar.getInstance(Locale.FRANCE);
        calendar.setTime(date);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        String lowestDate = parseDate(calendar.getTime());
        calendar.add(Calendar.DATE, 6);
        String highestDate = parseDate(calendar.getTime());
        return new String[]{lowestDate, highestDate};
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        database.execSQL("DROP TABLE subject");
        database.execSQL("DROP TABLE fetched_dates");
        //database.execSQL("DROP TABLE schedules");

        database.execSQL("CREATE TABLE subject (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "component_id INTEGER, date TEXT, start TEXT, end TEXT, name TEXT, room TEXT, " +
                "component TEXT, class_id INTEGER, visible INTEGER)");
        database.execSQL("CREATE TABLE fetched_dates (date TEXT UNIQUE)");
        database.execSQL("CREATE TABLE schedules (schedule_id INTEGER UNIQUE, " +
                "schedule_type INTEGER, schedule_name TEXT)");
    }
}