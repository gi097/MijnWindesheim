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
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.PowerManager;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.text.format.Formatter;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.giovanniterlingen.windesheim.NotificationCenter;
import com.giovanniterlingen.windesheim.R;
import com.giovanniterlingen.windesheim.view.NatschoolActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 * @author Thomas Visch
 */
public class DownloadController extends AsyncTask<String, Object, String>
        implements NotificationCenter.NotificationCenterDelegate {

    private final Activity activity;
    private final TextView contentNameTextView;
    private final TextView progressTextView;
    private final ProgressBar progressBar;
    private final FrameLayout cancelButton;
    private final String url;

    private PowerManager.WakeLock wakeLock;

    public DownloadController(Activity activity, TextView contentNameTextView,
                              TextView progressTextView, ProgressBar progressBar,
                              FrameLayout cancelButton, String url) {
        this.activity = activity;
        this.contentNameTextView = contentNameTextView;
        this.progressTextView = progressTextView;
        this.progressBar = progressBar;
        this.cancelButton = cancelButton;
        this.url = url;
    }

    @Override
    protected void onPreExecute() {
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.stopDownloadTasks);
        PowerManager powerManager = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        wakeLock.acquire();

        contentNameTextView.setVisibility(View.GONE);
        progressTextView.setVisibility(View.VISIBLE);
        progressTextView.setText(activity.getResources().getString(R.string.downloading));
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(true);
        cancelButton.setVisibility(View.VISIBLE);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (wakeLock.isHeld()) {
                    wakeLock.release();
                }
                DownloadController.this.cancel(true);
                contentNameTextView.setVisibility(View.VISIBLE);
                progressTextView.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                cancelButton.setVisibility(View.GONE);
                ((NatschoolActivity) activity).downloadCanceled();
            }
        });
    }

    @Override
    protected String doInBackground(final String... strings) {
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        int lastSlash = url.lastIndexOf('/');
        String fileName = url.substring(lastSlash + 1);
        File newFile = new File(Environment.getExternalStorageDirectory().toString(),
                "MijnWindesheim" + File.separator + fileName);
        try {
            if (newFile.exists()) {
                newFile.delete();
            }
            newFile.getParentFile().mkdirs();
            newFile.createNewFile();

            URI uri = new URI("https", "elo.windesheim.nl", url, null);
            URL url = uri.toURL();
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Cookie", new CookieController().getNatSchoolCookie());
            connection.connect();

            int fileLength = connection.getContentLength();

            input = connection.getInputStream();
            output = new FileOutputStream(newFile);

            byte data[] = new byte[32];
            int total = 0;
            long previousMillis = 0;
            int count;
            while ((count = input.read(data)) != -1) {
                if (isCancelled()) {
                    input.close();
                    newFile.delete();
                    return null;
                }
                total += count;
                if (fileLength > 0 && previousMillis + 1000 < System.currentTimeMillis()) {
                    String s = Formatter.formatFileSize(activity, total) + "/"
                            + Formatter.formatFileSize(activity, fileLength);
                    publishProgress(total, fileLength, s);
                    previousMillis = System.currentTimeMillis();
                }
                output.write(data, 0, count);
            }
        } catch (Exception e) {
            if (android.os.Build.VERSION.SDK_INT >= 23 &&
                    !new PermissionController().verifyStoragePermissions(activity)) {
                return "permission";
            }
            if (e instanceof IOException) {
                newFile.delete();
                if (e.getMessage().contains("ENOSPC")) {
                    return "nospace";
                }
                return "exception";
            }
        } finally {
            try {
                if (output != null) {
                    output.close();
                }
                if (input != null) {
                    input.close();
                }
            } catch (IOException ignored) {
                //
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
        return newFile.getAbsolutePath();
    }

    @Override
    protected void onProgressUpdate(Object... progress) {
        super.onProgressUpdate(progress);
        progressBar.setIndeterminate(false);
        progressBar.setMax((int) progress[1]);
        progressBar.setProgress((int) progress[0]);
        progressTextView.setText((String) progress[2]);
    }

    @Override
    protected void onCancelled(String s) {
        super.onCancelled(s);
        onPostExecute(null);
    }

    @Override
    protected void onPostExecute(final String result) {
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.stopDownloadTasks);
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
        contentNameTextView.setVisibility(View.VISIBLE);
        progressTextView.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        cancelButton.setVisibility(View.GONE);
        if ("permission".equals(result)) {
            ((NatschoolActivity) activity).noPermission();
            return;
        }
        if ("nospace".equals(result)) {
            ((NatschoolActivity) activity).noSpace();
            return;
        }
        if ("exception".equals(result)) {
            new AlertDialog.Builder(activity)
                    .setTitle(activity.getResources().getString(R.string.alert_connection_title))
                    .setMessage(activity.getResources()
                            .getString(R.string.alert_connection_description))
                    .setPositiveButton(activity.getResources().getString(R.string.connect),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    DownloadController.this.cancel(true);
                                    new DownloadController(activity, contentNameTextView,
                                            progressTextView, progressBar, cancelButton, url)
                                            .execute();
                                    dialog.cancel();
                                }
                            })
                    .setNegativeButton(activity.getResources()
                                    .getString(R.string.cancel),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            }).show();
            return;
        }
        if (result != null) {
            Uri uri;
            Intent target = new Intent(Intent.ACTION_VIEW);
            if (android.os.Build.VERSION.SDK_INT >= 24) {
                uri = FileProvider.getUriForFile(activity,
                        "com.giovanniterlingen.windesheim.provider", new File(result));
                target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                uri = Uri.fromFile(new File(result));
                target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            }
            String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            String mimetype = android.webkit.MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(extension);
            target.setDataAndType(uri, mimetype);
            try {
                activity.startActivity(target);
            } catch (ActivityNotFoundException e) {
                ((NatschoolActivity) activity).noSupportedApp();
            }
        }
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.stopDownloadTasks) {
            if (!isCancelled()) {
                this.cancel(true);
            }
        }
    }
}
