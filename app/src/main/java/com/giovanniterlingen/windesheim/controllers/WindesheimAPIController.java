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

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.Constants;
import com.giovanniterlingen.windesheim.models.Lesson;
import com.giovanniterlingen.windesheim.models.Result;
import com.giovanniterlingen.windesheim.models.Schedule;
import com.giovanniterlingen.windesheim.models.ScheduleItem;
import com.giovanniterlingen.windesheim.utils.CookieUtils;
import com.giovanniterlingen.windesheim.utils.NotificationUtils;
import com.giovanniterlingen.windesheim.utils.TimeUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class WindesheimAPIController {

    private static final String WINDESHEIM_API_URL = "http://api.windesheim.nl/api";
    private static final String WINDESHEIM_AZURE_API_URL = "https://windesheimapi.azurewebsites.net/api/v1";

    public static synchronized void getAndSaveLessons(boolean notify) throws Exception {
        Schedule[] schedules = DatabaseController.getInstance().getSchedules();
        for (Schedule schedule : schedules) {
            Lesson[] hiddenLessons = DatabaseController.getInstance().getHiddenLessons();
            Lesson[] oldLessons = DatabaseController.getInstance().getLessonsForCompare(schedule.getId());

            Lesson[] lessons = getLessons(schedule.getId(), schedule.getType());
            for (Lesson hiddenLesson : hiddenLessons) {
                for (Lesson lesson : lessons) {
                    if (hiddenLesson.getSubject().equals(lesson.getSubject()) &&
                            hiddenLesson.getTeacher().equals(lesson.getTeacher())) {
                        lesson.setVisible(false);
                    }
                }
            }
            DatabaseController.getInstance().clearScheduleData(schedule.getId());
            DatabaseController.getInstance().saveLessons(lessons);

            Lesson[] newLessons = DatabaseController.getInstance().getLessonsForCompare(schedule.getId());
            if (!notify) {
                continue;
            }
            if (oldLessons.length != newLessons.length) {
                NotificationUtils.getInstance().createScheduleChangedNotification();
                continue;
            }
            for (int i = 0; i < oldLessons.length; i++) {
                Lesson oldLesson = oldLessons[i];
                Lesson newLesson = newLessons[i];
                if (!oldLesson.getId().equals(newLesson.getId()) ||
                        !oldLesson.getStartTime().equals(newLesson.getStartTime()) ||
                        !oldLesson.getEndTime().equals(newLesson.getEndTime()) ||
                        !oldLesson.getRoom().equals(newLesson.getRoom()) ||
                        !oldLesson.getTeacher().equals(newLesson.getTeacher()) ||
                        !oldLesson.getClassName().equals(newLesson.getClassName())) {
                    NotificationUtils.getInstance().createScheduleChangedNotification();
                }
            }
        }

        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(ApplicationLoader.applicationContext);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(Constants.PREFS_LAST_FETCH_TIME, System.currentTimeMillis());
        editor.apply();
    }

    private static Lesson[] getLessons(String id, Constants.SCHEDULE_TYPE type) throws Exception {
        String typeString;
        if (type == Constants.SCHEDULE_TYPE.CLASS) {
            typeString = "Klas";
        } else if (type == Constants.SCHEDULE_TYPE.TEACHER) {
            typeString = "Docent";
        } else {
            typeString = "Vak";
        }
        URL url = new URL(WINDESHEIM_API_URL + "/" + typeString + "/" + id + "/Les");
        String response = makeRequest(url, false);

        JSONArray jsonArray = new JSONArray(response);
        Lesson[] lessons = new Lesson[jsonArray.length()];

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject object = jsonArray.getJSONObject(i);

            Lesson lesson = new Lesson();
            lesson.setId(object.getString("id"));

            if (type == Constants.SCHEDULE_TYPE.SUBJECT) {
                lesson.setSubject(object.getString("commentaar"));
            } else if (object.getString("vaknaam").length() > 0) {
                lesson.setSubject(object.getString("vaknaam"));
            } else if (object.getString("vakcode").length() > 0) {
                lesson.setSubject(object.getString("vakcode"));
            } else {
                lesson.setSubject(object.getString("commentaar"));
            }

            lesson.setStartTime(new Date(TimeUtils.removeTimeOffset(object
                    .getLong("starttijd"))));
            lesson.setEndTime(new Date(TimeUtils.removeTimeOffset(object
                    .getLong("eindtijd"))));

            lesson.setRoom(object.getString("lokaal"));
            lesson.setClassName(object.getString("groepcode"));
            lesson.setScheduleType(type);
            lesson.setVisible(true);
            lesson.setScheduleId(id);

            StringBuilder stringBuilder = new StringBuilder();
            JSONArray jsonArray1 = object.getJSONArray("docentnamen");
            for (int j = 0; j < jsonArray1.length(); j++) {
                stringBuilder.append(jsonArray1.getString(j));
                if (j < jsonArray1.length() - 1) {
                    stringBuilder.append(", ");
                }
            }
            lesson.setTeacher(stringBuilder.toString());
            lessons[i] = lesson;
        }
        return lessons;
    }

    public static ScheduleItem[] getClasses() throws Exception {
        URL url = new URL(WINDESHEIM_API_URL + "/Klas/");
        String response = makeRequest(url, false);

        JSONArray jsonArray = new JSONArray(response);
        ScheduleItem[] items = new ScheduleItem[jsonArray.length()];

        for (int i = 0; i < items.length; i++) {
            JSONObject object = jsonArray.getJSONObject(i);
            items[i] = new ScheduleItem(object.getString("id"), object.getString("klasnaam"));
        }
        return items;
    }

    public static ScheduleItem[] getTeachers() throws Exception {
        URL url = new URL(WINDESHEIM_API_URL + "/Docent/");
        String response = makeRequest(url, false);

        JSONArray jsonArray = new JSONArray(response);
        ScheduleItem[] items = new ScheduleItem[jsonArray.length()];

        for (int i = 0; i < items.length; i++) {
            JSONObject object = jsonArray.getJSONObject(i);
            items[i] = new ScheduleItem(object.getString("id"), object.getString("naam"));
        }
        return items;
    }

    public static ScheduleItem[] getSubjects() throws Exception {
        URL url = new URL(WINDESHEIM_API_URL + "/Vak/");
        String response = makeRequest(url, false);

        JSONArray jsonArray = new JSONArray(response);
        ScheduleItem[] items = new ScheduleItem[jsonArray.length()];

        for (int i = 0; i < items.length; i++) {
            JSONObject object = jsonArray.getJSONObject(i);
            items[i] = new ScheduleItem(object.getString("id"), object.getString("code"));
        }
        return items;
    }

    public static String getStudyInfo(String studentNumber) throws Exception {
        URL url = new URL(WINDESHEIM_AZURE_API_URL + "/Persons/"
                + studentNumber + "/Study?onlydata=true");
        return makeRequest(url, true);
    }

    public static String getResults(String studentNumber, String isat) throws Exception {
        URL url = new URL(WINDESHEIM_AZURE_API_URL + "/Persons/"
                + studentNumber + "/Study/" + isat
                + "/CourseResults?onlydata=true");
        return makeRequest(url, true);
    }

    public static Result[] getResultArray(JSONArray resultsJSON) throws Exception {
        ArrayList<Result> results = new ArrayList<>();
        for (int i = 0; i < resultsJSON.length(); i++) {
            JSONObject current = resultsJSON.getJSONObject(i);
            String result = current.getString("grade");
            String name = current.getJSONObject("course").getString("name");
            if (result.length() > 0 && name.length() > 0) {
                results.add(new Result(name, result));
            }
        }
        return results.toArray(new Result[0]);
    }

    private static String makeRequest(URL url, boolean authenticate) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(10000);
        connection.setRequestMethod("GET");
        if (authenticate) {
            connection.setRequestProperty("Cookie", CookieUtils.getEducatorCookie());
        }
        connection.setDoInput(true);
        connection.connect();

        StringBuilder stringBuffer = new StringBuilder();

        InputStream inputStream = connection.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuffer.append(line);
        }
        return stringBuffer.toString();
    }
}
