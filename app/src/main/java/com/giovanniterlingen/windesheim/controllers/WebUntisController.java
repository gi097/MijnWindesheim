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
package com.giovanniterlingen.windesheim.controllers;

import com.giovanniterlingen.windesheim.models.Lesson;
import com.giovanniterlingen.windesheim.models.Schedule;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class WebUntisController {

    public synchronized void getAndSaveAllSchedules(Date date, boolean compare) throws Exception {
        DatabaseController.getInstance().deleteFetched(date);
        Schedule[] schedules = DatabaseController.getInstance().getSchedules();
        for (Schedule schedule : schedules) {
            JSONObject data = getScheduleFromServer(schedule.getId(), date, schedule.getType());
            saveSchedule(data, date, schedule.getId(), schedule.getType(), compare);
        }
        DatabaseController.getInstance().addFetched(date);
    }

    public JSONObject getListFromServer(int type) throws Exception {
        URL url = new URL("https://roosters.windesheim.nl/WebUntis/Timetable.do?" +
                "ajaxCommand=getPageConfig&type=" + type);
        return getJsonFromUrl(url);
    }

    private JSONObject getScheduleFromServer(int id, Date date, int type) throws Exception {
        URL url = new URL("https://roosters.windesheim.nl/WebUntis/Timetable.do?" +
                "ajaxCommand=getWeeklyTimetable&elementType=" + type + "&elementId=" + id +
                "&date=" + CalendarController.getInstance()
                .getYearMonthDayDateFormat().format(date));
        return getJsonFromUrl(url);
    }

    private JSONObject getJsonFromUrl(URL url) throws IOException, JSONException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
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

    private static synchronized void saveSchedule(JSONObject jsonObject, Date date, int id, int type, boolean compare)
            throws Exception {
        Lesson[] oldLessons = null;
        if (compare && DatabaseController.getInstance().isFetched(date)) {
            oldLessons = DatabaseController.getInstance().getLessonsForCompare(date, id);
        }
        Lesson[] hiddenLessons = DatabaseController.getInstance().getHiddenLessons();

        DatabaseController.getInstance().clearScheduleData(date, id);

        JSONObject resultData = jsonObject.getJSONObject("result").getJSONObject("data");
        JSONArray data = resultData.getJSONObject("elementPeriods")
                .getJSONArray(Integer.toString(id));

        Lesson lastLesson = null;
        for (int i = 0; i < data.length(); i++) {
            String teachers = "";
            String classNames = "";
            String room = "";
            String module = "";
            JSONObject lessonObject = data.getJSONObject(i);
            JSONArray lessonElements = lessonObject.getJSONArray("elements");
            for (int j = 0; j < lessonElements.length(); j++) {
                JSONObject elementObject = lessonElements.getJSONObject(j);
                int elementId = elementObject.getInt("id");
                int elementType = elementObject.getInt("type");
                JSONArray elements = resultData.getJSONArray("elements");
                for (int k = 0; k < elements.length(); k++) {
                    JSONObject elementsObject = elements.getJSONObject(k);
                    if (elementsObject.getInt("id") == elementId &&
                            elementsObject.getInt("type") == elementType) {
                        if (elementType == 1) {
                            if (classNames.length() > 0) {
                                classNames += ", ";
                            }
                            classNames += elementsObject.getString("name");
                        } else if (elementType == 2) {
                            if (teachers.length() > 0) {
                                teachers += ", ";
                            }
                            teachers += elementsObject.getString("longName") +
                                    " (" + elementsObject.getString("name") + ")";
                        } else if (elementType == 3) {
                            module = elementsObject.getString("name");
                        } else if (elementType == 4) {
                            room = elementsObject.getString("name");
                        }
                    }
                }
            }
            String subject = lessonObject.getString("lessonText");
            if (module.length() == 0) {
                module = subject;
            } else if (subject.length() > 0) {
                module += " - " + subject;
            }
            boolean visible = true;
            for (Lesson hiddenLesson : hiddenLessons) {
                if (hiddenLesson.getSubject().equals(module)) {
                    visible = false;
                    break;
                }
            }
            Lesson currentLesson = new Lesson(lessonObject.getInt("lessonId"),
                    module, lessonObject.getString("date"),
                    parseTime(lessonObject.getString("startTime")),
                    parseTime(lessonObject.getString("endTime")),
                    room, teachers, classNames, id, type,
                    visible ? 1 : 0);
            if (lastLesson != null) {
                if (lastLesson.getId() == currentLesson.getId() &&
                        lastLesson.getDate().equals(currentLesson.getDate()) &&
                        lastLesson.getEndTime().equals(currentLesson.getStartTime())) {
                    lastLesson.setEndTime(currentLesson.getEndTime());
                    if (i < data.length() - 1) {
                        continue;
                    }
                    DatabaseController.getInstance().saveScheduleData(lastLesson);
                    break;
                } else {
                    DatabaseController.getInstance().saveScheduleData(lastLesson);
                }
            }
            lastLesson = currentLesson;
            if (data.length() == 1 || i == data.length() - 1) {
                DatabaseController.getInstance().saveScheduleData(lastLesson);
            }
        }
        if (compare && oldLessons != null) {
            Lesson[] newLessons = DatabaseController.getInstance()
                    .getLessonsForCompare(date, id);
            if (oldLessons.length != newLessons.length) {
                NotificationController.getInstance().createScheduleChangedNotification();
                return;
            }
            for (int i = 0; i < oldLessons.length; i++) {
                Lesson oldLesson = oldLessons[i];
                Lesson newLesson = newLessons[i];
                if (oldLesson.getId() != newLesson.getId() ||
                        !oldLesson.getDate().equals(newLesson.getDate()) ||
                        !oldLesson.getStartTime().equals(newLesson.getStartTime()) ||
                        !oldLesson.getEndTime().equals(newLesson.getEndTime()) ||
                        !oldLesson.getRoom().equals(newLesson.getRoom()) ||
                        !oldLesson.getTeacher().equals(newLesson.getTeacher()) ||
                        !oldLesson.getClassName().equals(newLesson.getClassName())) {
                    NotificationController.getInstance().createScheduleChangedNotification();
                    return;
                }
            }
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
