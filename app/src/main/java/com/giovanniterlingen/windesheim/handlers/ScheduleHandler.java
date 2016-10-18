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
package com.giovanniterlingen.windesheim.handlers;

import android.annotation.SuppressLint;
import android.database.Cursor;

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.objects.Schedule;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class ScheduleHandler {

    /**
     * Gets a list of available classes, teachers or lessons depending on type.
     *
     * @param type The type of the object
     * @return The retrieved response, it will be JSON format
     * @throws Exception
     */
    public static String getListFromServer(int type) throws Exception {
        StringBuilder stringBuffer = new StringBuilder("");
        URL urlLink = new URL("https://roosters.windesheim.nl/WebUntis/Timetable.do?" +
                "ajaxCommand=getPageConfig&type=" + type);
        HttpURLConnection connection = (HttpURLConnection) urlLink.openConnection();
        connection.setConnectTimeout(10000);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Cookie", "schoolname=\"_V2luZGVzaGVpbQ==\"");
        connection.setDoInput(true);
        connection.connect();

        InputStream inputStream = connection.getInputStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = rd.readLine()) != null) {
            stringBuffer.append(line);
        }
        return stringBuffer.toString();
    }

    /**
     * Gets a JSONObject of lessons depending on date, id and type.
     *
     * @param id   The class' teacher's or subject's id
     * @param date The date we want the schedule from
     * @param type Type specifies schedule type, class, subject or teacher
     * @return The JSONObject containing the schedule of that day
     * @throws Exception
     */
    @SuppressLint("SimpleDateFormat")
    public static JSONObject getScheduleFromServer(String id, Date date, int type)
            throws Exception {
        URL urlLink = new URL("https://roosters.windesheim.nl/WebUntis/Timetable.do?" +
                "ajaxCommand=getWeeklyTimetable&elementType=" + type + "&elementId=" + id +
                "&date=" + new SimpleDateFormat("yyyyMMdd").format(date));
        HttpURLConnection connection = (HttpURLConnection) urlLink.openConnection();
        connection.setConnectTimeout(10000);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Cookie", "schoolname=\"_V2luZGVzaGVpbQ==\"");
        connection.setDoInput(true);
        connection.connect();

        StringBuilder stringBuffer = new StringBuilder("");
        InputStream inputStream = connection.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuffer.append(line);
        }
        return new JSONObject(stringBuffer.toString());
    }

    /**
     * Here we will parse and the retrieved JSON data in the local database. WebUntis uses a weird
     * JSON structure. It's hard to explain here, so take a look at the for-loops I used.
     *
     * @param jsonObject  Contains the fetched lessons
     * @param date        Specifies the date of the fetched lessons
     * @param componentId Specifies the schedule's id
     * @param compare     Defines if we need to check for schedule changes
     * @throws Exception
     */
    @SuppressLint("SimpleDateFormat")
    public static synchronized void saveSchedule(JSONObject jsonObject, Date date, String componentId, boolean compare)
            throws Exception {
        // get the user filtered lessons to exclude them during fetch
        List<String> list = new ArrayList<>();
        Cursor cursor = ApplicationLoader.scheduleDatabase.getFilteredLessons();
        while (cursor.moveToNext()) {
            list.add(cursor.getString(0));
        }
        cursor.close();

        // only select the old data if we need to compare
        Schedule[] oldData = null;
        if (compare && ApplicationLoader.scheduleDatabase.isFetched(date)) {
            Cursor oldCursor = ApplicationLoader.scheduleDatabase.getLessonsForCompare(date, componentId);
            oldData = new Schedule[oldCursor.getCount()];
            while (oldCursor.moveToNext()) {
                oldData[oldCursor.getPosition()] = new Schedule(oldCursor.getString(0),
                        oldCursor.getString(1), oldCursor.getString(2), oldCursor.getString(3),
                        oldCursor.getString(4), oldCursor.getString(5), oldCursor.getString(6),
                        oldCursor.getString(7));
            }
            oldCursor.close();
        }

        // delete old schedule data
        ApplicationLoader.scheduleDatabase.clearScheduleData(date);
        // delete fetch date
        ApplicationLoader.scheduleDatabase.clearFetched(date);

        // init required global values
        String component = "";
        String classRoom = "";
        String module = "";

        // start parsing the json object
        JSONObject resultData = jsonObject.getJSONObject("result").getJSONObject("data");
        JSONArray data = resultData.getJSONObject("elementPeriods").getJSONArray(componentId);
        for (int i = 0; i < data.length(); i++) {
            JSONObject lessonObject = data.getJSONObject(i);
            JSONArray lessonElements = lessonObject.getJSONArray("elements");
            for (int j = 0; j < lessonElements.length(); j++) {
                JSONObject elementObject = lessonElements.getJSONObject(j);
                String elementId = elementObject.getString("id");
                String elementType = elementObject.getString("type");
                JSONArray elements = resultData.getJSONArray("elements");
                for (int k = 0; k < elements.length(); k++) {
                    JSONObject elementsObject = elements.getJSONObject(k);
                    if (elementsObject.getString("id").equals(elementId) &&
                            elementsObject.getString("type").equals(elementType)) {
                        if (elementType.equals("1")) {
                            component = elementsObject.getString("name");
                        }
                        if (elementType.equals("2")) {
                            component = elementsObject.getString("longName");
                        }
                        if (elementType.equals("3")) {
                            module = elementsObject.getString("name");
                        }
                        if (elementType.equals("4")) {
                            classRoom = elementsObject.getString("name");
                        }
                    }
                }
            }
            String subject = lessonObject.getString("lessonText");
            if (module.equals("")) {
                module = subject;
            } else if (!subject.equals("")) {
                module += " - " + subject;
            }
            // save it and reset fields
            ApplicationLoader.scheduleDatabase.saveScheduleData(lessonObject.getString("lessonId"),
                    lessonObject.getString("date"), parseTime(lessonObject.getString("startTime")),
                    parseTime(lessonObject.getString("endTime")), module, classRoom, component,
                    componentId, list.contains(lessonObject.getString("lessonId")) ? 0 : 1);
            component = "";
            classRoom = "";
            module = "";
        }

        if (compare && oldData != null) {
            // start comparing old data with new one
            Cursor newCursor = ApplicationLoader.scheduleDatabase.getLessonsForCompare(date, componentId);
            Schedule[] newData = new Schedule[newCursor.getCount()];
            while (newCursor.moveToNext()) {
                newData[newCursor.getPosition()] = new Schedule(newCursor.getString(0),
                        newCursor.getString(1), newCursor.getString(2), newCursor.getString(3),
                        newCursor.getString(4), newCursor.getString(5), newCursor.getString(6),
                        newCursor.getString(7));
            }
            newCursor.close();

            if (oldData.length == newData.length) {
                for (int i = 0; i < oldData.length; i++) {
                    if (!oldData[i].equals(newData[i])) {
                        NotificationHandler.createScheduleChangedNotification();
                        break;
                    }
                }
            } else {
                NotificationHandler.createScheduleChangedNotification();
            }
        }
        ApplicationLoader.scheduleDatabase.addFetched(date);
    }

    /**
     * Returns a link to download the ical file we need.
     *
     * @return The generated url
     */
    public static String getIcalLink(String id, Date date, int type) {
        return "https://roosters.windesheim.nl/WebUntis/Ical.do?" +
                "elemType=" + type + "&elemId=" + id +
                "&rpt_sd=" + new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    /**
     * A little workaround to parse the time. We need to add a 0 to let SQLite sort the entries
     * properly.
     *
     * @param time The string we want to pad with a 0
     * @return The parsed string
     */
    private static String parseTime(String time) {
        if (time.length() == 3) {
            time = "0" + time;
        }
        String hours = time.substring(0, 2);
        String minutes = time.substring(2);
        return hours + ":" + minutes;
    }
}
