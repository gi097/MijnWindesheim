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

import com.giovanniterlingen.windesheim.objects.Result;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class WindesheimAPIHandler {

    public static String getStudyInfo(String studentNumber) throws Exception {
        StringBuilder stringBuffer = new StringBuilder("");
        URL urlLink = new URL("https://windesheimapi.azurewebsites.net/api/v1/Persons/"
                + studentNumber + "/Study?onlydata=true");
        HttpURLConnection connection = (HttpURLConnection) urlLink.openConnection();
        connection.setConnectTimeout(10000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Cookie", CookieHandler.getEducatorCookie());
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

    public static String getResults(String studentNumber, String isat) throws Exception {
        StringBuilder stringBuffer = new StringBuilder("");
        URL urlLink = new URL("https://windesheimapi.azurewebsites.net/api/v1/Persons/"
                + studentNumber + "/Study/" + isat
                + "/CourseResults?onlydata=true&$orderby=lastmodified");
        HttpURLConnection connection = (HttpURLConnection) urlLink.openConnection();
        connection.setConnectTimeout(10000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Cookie", CookieHandler.getEducatorCookie());
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

    public static Result[] getResultArray(JSONArray resultsJSON) throws Exception {
        ArrayList<Result> results = new ArrayList<>();
        for (int i = resultsJSON.length() - 1; i >= 0; i--) {
            JSONObject current = resultsJSON.getJSONObject(i);
            String result = current.getString("grade");
            String name = current.getJSONObject("course").getString("name");
            if (result != null && result.length() > 0 && name != null && name.length() > 0) {
                results.add(new Result(name, result));
            }
        }
        return results.toArray(new Result[results.size()]);
    }
}
