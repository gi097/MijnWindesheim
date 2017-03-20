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
import java.util.Locale;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class ScheduleHandler {

    public static synchronized void getAndSaveAllSchedules(Date date) throws Exception {
        ApplicationLoader.scheduleDatabase.deleteFetched(date);
        Schedule[] schedules = ApplicationLoader.scheduleDatabase.getSchedules();
        for (Schedule schedule : schedules) {
            ScheduleHandler.saveSchedule(ScheduleHandler
                    .getScheduleFromServer(schedule.getId(), date,
                            schedule.getType()), date, schedule.getId());
        }
        ApplicationLoader.scheduleDatabase.addFetched(date);
    }

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

    private static JSONObject getScheduleFromServer(int id, Date date, int type) throws Exception {
        URL urlLink = new URL("https://roosters.windesheim.nl/WebUntis/Timetable.do?" +
                "ajaxCommand=getWeeklyTimetable&elementType=" + type + "&elementId=" + id +
                "&date=" + new SimpleDateFormat("yyyyMMdd", Locale.US).format(date));
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

    private static synchronized void saveSchedule(JSONObject jsonObject, Date date, int id)
            throws Exception {
        List<String> list = new ArrayList<>();
        Cursor cursor = ApplicationLoader.scheduleDatabase.getFilteredLessons();
        while (cursor.moveToNext()) {
            list.add(cursor.getString(0));
        }
        cursor.close();

        ApplicationLoader.scheduleDatabase.clearScheduleData(date, id);

        String component = "";
        String classRoom = "";
        String module = "";

        JSONObject resultData = jsonObject.getJSONObject("result").getJSONObject("data");
        JSONArray data = resultData.getJSONObject("elementPeriods")
                .getJSONArray(Integer.toString(id));
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
            ApplicationLoader.scheduleDatabase.saveScheduleData(lessonObject.getInt("lessonId"),
                    lessonObject.getString("date"), parseTime(lessonObject.getString("startTime")),
                    parseTime(lessonObject.getString("endTime")), module, classRoom, component,
                    id, list.contains(lessonObject.getString
                            ("lessonId")) ? 0 : 1);
            component = "";
            classRoom = "";
            module = "";
        }
    }

    private static String parseTime(String time) {
        if (time.length() == 3) {
            time = "0" + time;
        }
        String hours = time.substring(0, 2);
        String minutes = time.substring(2);
        return hours + ":" + minutes;
    }
}
