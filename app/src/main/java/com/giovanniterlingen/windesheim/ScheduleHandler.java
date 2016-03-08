package com.giovanniterlingen.windesheim;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A scheduler app for Windesheim students
 *
 * @author Giovanni Terlingen
 */
class ScheduleHandler {

    public static String getClassesFromServer() throws Exception {

        StringBuilder stringBuffer = new StringBuilder("");
        URL urlLink = new URL("https://roosters.windesheim.nl/WebUntis/Timetable.do?ajaxCommand=getPageConfig&type=1");
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

    public static BufferedReader getScheduleFromServer(String id, Date date) throws Exception {

        URL urlLink = new URL("https://roosters.windesheim.nl/WebUntis/lessoninfodlg.do?date=" + new SimpleDateFormat("yyyyMMdd").format(date) + "&starttime=0800&endtime=2300&elemid=" + id + "&elemtype=1");
        HttpURLConnection connection = (HttpURLConnection) urlLink.openConnection();
        connection.setConnectTimeout(10000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Cookie", "schoolname=\"_V2luZGVzaGVpbQ==\"");
        connection.setDoInput(true);
        connection.connect();
        InputStream inputStream = connection.getInputStream();
        return new BufferedReader(new InputStreamReader(inputStream));
    }

    public static void saveSchedule(BufferedReader reader, Date date, String classId) throws Exception {
        DateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        int countTd = 0;
        String module = null;
        String subject = null;
        String id = null;
        String start = null;
        String end = null;
        String teacher = null;
        String room = null;
        String line;
        ApplicationLoader.scheduleDatabase.clearScheduleData(simpleDateFormat.format(date));
        while ((line = reader.readLine()) != null) {
            if (line.contains("<td>")) {
                countTd++;
            }
            String les = line.replace("<td>", "").replace("</td>", "");
            switch (countTd) {
                case 1:
                    if (line.contains("<tooltip>")) {
                        module = line.replaceAll("\\s+", "").replace("<tooltip>", "").replace("</tooltip>", "");
                    }
                    break;
                case 3:
                    id = les;
                    break;
                case 4:
                    if (line.contains("</span>")) {
                        if (les.split("</span>")[0].split("\">").length != 2) {
                            teacher = "";
                            break;
                        } else {
                            teacher = les.split("</span>")[0].split("\">")[1];
                            break;
                        }
                    }
                    break;
                case 5:
                    if (!les.contains(".")) {
                        break;
                    }
                    room = les.replaceAll("\t", "");
                    break;
                case 6:
                    subject = les;
                    break;
                case 7:
                    String[] startSplit = les.split(":");
                    if (startSplit[0].length() == 1) {
                        les = 0 + les;
                    }
                    start = les;
                    break;
                case 8:
                    String[] endSplit = les.split(":");
                    if (endSplit[0].length() == 1) {
                        les = 0 + les;
                    }
                    end = les;
                    break;
                case 11:
                    ApplicationLoader.scheduleDatabase.saveScheduleData(id, simpleDateFormat.format(date), start, end, (subject.equals("") ? module : subject), room, teacher, classId);
                    countTd = 0;
                    break;
                default:
                    break;
            }
        }
    }
}
