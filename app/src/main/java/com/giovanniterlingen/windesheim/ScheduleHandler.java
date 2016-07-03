package com.giovanniterlingen.windesheim;

import android.database.Cursor;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * A schedule app for Windesheim students
 * Damn this JSON structure was hard to analyze...
 *
 * @author Giovanni Terlingen
 */
class ScheduleHandler {

    public static String getListFromServer(int type) throws Exception {
        StringBuilder stringBuffer = new StringBuilder("");
        URL urlLink = new URL("https://roosters.windesheim.nl/WebUntis/Timetable.do?ajaxCommand=getPageConfig&type=" + type);
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

    public static JSONObject getScheduleFromServer(String id, Date date, int type) throws Exception {
        URL urlLink = new URL("https://roosters.windesheim.nl/WebUntis/Timetable.do?ajaxCommand=getWeeklyTimetable&elementType=" + type + "&elementId=" + id + "&date=" + new SimpleDateFormat("yyyyMMdd").format(date));
        HttpURLConnection connection = (HttpURLConnection) urlLink.openConnection();
        connection.setConnectTimeout(10000);
        connection.setRequestMethod("GET");
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

    public static void saveSchedule(JSONObject jsonObject, Date date, String componentId) throws Exception {
        // get the user filtered lessons to exclude them during fetch
        List<String> list = new ArrayList<>();
        Cursor cursor = ApplicationLoader.scheduleDatabase.getFilteredLessons();
        while (cursor.moveToNext()) {
            list.add(cursor.getString(0));
        }
        cursor.close();

        // delete old schedule data
        ApplicationLoader.scheduleDatabase.clearScheduleData(date);

        // init required global values
        String component = "";
        String classRoom = "";
        String module = "";

        // start parsing json
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
            ApplicationLoader.scheduleDatabase.saveScheduleData(lessonObject.getString("lessonId"), lessonObject.getString("date"), parseTime(lessonObject.getString("startTime")), parseTime(lessonObject.getString("endTime")), module, classRoom, component, componentId, list.contains(lessonObject.getString("lessonId")) ? 0 : 1);
            component = "";
            classRoom = "";
            module = "";
        }
    }

    private static String parseTime(String time) {
        // pretend the time is 830; it must be 08:30 in order to sort it in SQLite
        if (time.length() == 3) {
            time = "0" + time;
        }
        String hours = time.substring(0, 2);
        String minutes = time.substring(2);
        return hours + ":" + minutes;
    }
}
