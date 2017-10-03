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

import com.giovanniterlingen.windesheim.models.Result;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
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
public class WindesheimAPIController {

    public String getStudyInfo(String studentNumber) throws Exception {
        URL url = new URL("https://windesheimapi.azurewebsites.net/api/v1/Persons/"
                + studentNumber + "/Study?onlydata=true");
        return getDataFromUrl(url);
    }

    public String getResults(String studentNumber, String isat) throws Exception {
        URL url = new URL("https://windesheimapi.azurewebsites.net/api/v1/Persons/"
                + studentNumber + "/Study/" + isat
                + "/CourseResults?onlydata=true&$orderby=lastmodified");
        return getDataFromUrl(url);
    }

    private String getDataFromUrl(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(10000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Cookie", new CookieController().getEducatorCookie());
        connection.setDoInput(true);
        connection.connect();

        StringBuilder stringBuffer = new StringBuilder("");

        InputStream inputStream = connection.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuffer.append(line);
        }
        return stringBuffer.toString();
    }

    public Result[] getResultArray(JSONArray resultsJSON) throws Exception {
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
