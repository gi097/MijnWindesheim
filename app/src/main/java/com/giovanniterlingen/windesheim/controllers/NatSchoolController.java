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

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.R;
import com.giovanniterlingen.windesheim.models.Download;
import com.giovanniterlingen.windesheim.models.NatschoolContent;
import com.giovanniterlingen.windesheim.view.AuthenticationActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 * @author Thomas Visch
 */
public abstract class NatSchoolController extends AsyncTask<Void, Void, Void> {

    private final List<NatschoolContent> content = new ArrayList<>();
    private final Activity activity;
    private final String type;
    private final int courseId;
    private final int id;

    protected NatSchoolController(int courseId, int id, Activity activity) {
        if (courseId == -1 && id == -1) {
            this.type = "courses";
        } else {
            this.type = "content";
        }
        this.courseId = courseId;
        this.id = id;
        this.activity = activity;
    }

    public abstract void onFinished(List<NatschoolContent> content);

    @Override
    protected Void doInBackground(Void... params) {
        if (type.equals("courses")) {
            createWebRequest("LoadStudyroutes", "start=0&length=1000&filter=0&search=");
        }
        if (type.equals("content")) {
            createWebRequest("LoadStudyrouteContent", "studyrouteid=" + courseId + "&parentid="
                    + id + "&start=0&length=100");
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        onFinished(content);
    }

    private void createWebRequest(final String path, final String urlParameters) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL("http://elo.windesheim.nl/services/Studyroutemobile.asmx/"
                    + path + "?" + urlParameters);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", Integer
                    .toString(urlParameters.getBytes().length));
            connection.setRequestProperty("Content-Language", "en-US");
            connection.setRequestProperty("Cookie", new CookieController().getNatSchoolCookie());

            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();

            int status = connection.getResponseCode();

            InputStream is = status >= 400 ? connection.getErrorStream() :
                    connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            JSONObject jsonObject = new JSONObject(response.toString());
            if (type.equals("courses")) {
                JSONArray jsonArray = jsonObject.getJSONArray("STUDYROUTES");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonobj = jsonArray.getJSONObject(i);
                    content.add(new NatschoolContent(jsonobj.getInt("ID"), jsonobj.getString("NAME"),
                            (jsonobj.has("IMAGEURL_24") ? jsonobj.getString("IMAGEURL_24") : null)));
                }
            }
            if (type.equals("content")) {
                JSONArray jsonArray = jsonObject.getJSONArray("STUDYROUTE_CONTENT");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonobj = jsonArray.getJSONObject(i);
                    int type = jsonobj.getInt("ITEMTYPE");
                    if (type == 0 || type == 1 || type == 3 || type == 10) {
                        NatschoolContent natschoolContent =
                                new NatschoolContent(jsonobj.getInt("ID"), jsonobj.getString("NAME"),
                                        jsonobj.getInt("STUDYROUTE_ITEM_ID"), type,
                                        (jsonobj.has("URL") ? jsonobj.getString("URL") : null));
                        Download currentDownload = DownloadController.activeDownloads
                                .get(natschoolContent.id);
                        if (currentDownload != null) {
                            natschoolContent.downloading = true;
                            natschoolContent.progress = currentDownload.getProgress();
                            natschoolContent.progressString = currentDownload.getProgressString();
                        }
                        content.add(natschoolContent);
                    }
                }
            }
        } catch (Exception e) {
            if (e instanceof JSONException) {
                Intent intent = new Intent(activity, AuthenticationActivity.class);
                intent.putExtra("educator", false);
                activity.startActivity(intent);
                activity.finish();
                return;
            }
            ApplicationLoader.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    new AlertDialog.Builder(activity)
                            .setTitle(activity.getResources()
                                    .getString(R.string.alert_connection_title))
                            .setMessage(activity.getResources()
                                    .getString(R.string.alert_connection_description))
                            .setPositiveButton(activity.getResources().getString(R.string.connect),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            NatSchoolController.this.cancel(true);
                                            new NatSchoolController(courseId, id, activity) {
                                                @Override
                                                public void onFinished(List<NatschoolContent> content) {
                                                    NatSchoolController.this.onFinished(content);
                                                }
                                            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                                            dialog.cancel();
                                        }
                                    })
                            .setNegativeButton(activity.getResources().getString(R.string.cancel),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    }).show();
                }
            });
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}