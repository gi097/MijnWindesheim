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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.view.View;

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.R;
import com.giovanniterlingen.windesheim.ui.ContentsActivity;
import com.giovanniterlingen.windesheim.ui.ScheduleActivity;

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
public class DownloadHandler extends AsyncTask<String, Integer, String> {

    private final Activity context;
    private final ProgressDialog progressDialog;
    private PowerManager.WakeLock wakeLock;
    private boolean isNatschoolRequest;

    public DownloadHandler(Activity context, ProgressDialog progressDialog, boolean isNatschoolRequest) {
        this.context = context;
        this.progressDialog = progressDialog;
        this.isNatschoolRequest = isNatschoolRequest;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        wakeLock.acquire();
        progressDialog.show();
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (wakeLock.isHeld()) {
                    wakeLock.release();
                }
                DownloadHandler.this.cancel(true);
                View view = null;
                if (context instanceof ContentsActivity) {
                    view = ((ContentsActivity) context).view;
                } else if (context instanceof ScheduleActivity) {
                    view = ScheduleActivity.view;
                }
                if (view != null) {
                    Snackbar snackbar = Snackbar.make(view, context.getResources().getString(
                            R.string.canceled_downloading), Snackbar.LENGTH_SHORT);
                    snackbar.show();
                }
            }
        });
    }

    @Override
    protected String doInBackground(final String... strings) {
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        int lastSlash = strings[0].lastIndexOf('/');
        String fileName;
        if (isNatschoolRequest) {
            fileName = strings[0].substring(lastSlash + 1);
        } else {
            fileName = "calendar.ics";
        }
        File newFile = new File(Environment.getExternalStorageDirectory().toString(),
                "MijnWindesheim" + File.separator + fileName);
        try {
            if (newFile.exists()) {
                newFile.delete();
            }
            newFile.getParentFile().mkdirs();
            newFile.createNewFile();

            URI uri;
            if (isNatschoolRequest) {
                uri = new URI("https", "elo.windesheim.nl", strings[0], null);
            } else {
                uri = new URI(strings[0]);
            }
            URL url = uri.toURL();
            connection = (HttpURLConnection) url.openConnection();
            if (isNatschoolRequest) {
                connection.setRequestProperty("Cookie", CookieHandler.getCookie());
            } else {
                connection.setRequestProperty("Cookie", "schoolname=\"_V2luZGVzaGVpbQ==\"");
            }
            connection.connect();

            int fileLength = connection.getContentLength();

            input = connection.getInputStream();
            output = new FileOutputStream(newFile);

            byte data[] = new byte[32];
            long total = 0;
            int count;
            while ((count = input.read(data)) != -1) {
                if (isCancelled()) {
                    input.close();
                    newFile.delete();
                    return null;
                }
                total += count;
                if (fileLength > 0) {
                    publishProgress((int) (total * 100 / fileLength));
                }
                output.write(data, 0, count);
            }
        } catch (Exception e) {
            // Ask user to let the app write to sdcard
            if (android.os.Build.VERSION.SDK_INT >= 23 &&
                    !PermissionHandler.verifyStoragePermissions(context)) {
                return "permission";
            }
            if (e instanceof IOException) {
                newFile.delete();
                if (wakeLock.isHeld()) {
                    wakeLock.release();
                }
                if (e.getMessage().contains("ENOSPC")) {
                    View view = null;
                    if (context instanceof ContentsActivity) {
                        view = ((ContentsActivity) context).view;
                    } else if (context instanceof ScheduleActivity) {
                        view = ScheduleActivity.view;
                    }
                    if (view != null) {
                        Snackbar snackbar = Snackbar.make(view, context.getResources().getString(
                                R.string.storage_full), Snackbar.LENGTH_SHORT);
                        snackbar.show();
                    }
                } else {
                    ApplicationLoader.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            new AlertDialog.Builder(context)
                                    .setTitle(context.getResources().getString(R.string.alert_connection_title))
                                    .setMessage(context.getResources().getString(R.string.alert_connection_description))
                                    .setPositiveButton(context.getResources().getString(R.string.connect),
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    DownloadHandler.this.cancel(true);
                                                    new DownloadHandler(context, progressDialog, true).execute(strings);
                                                    dialog.cancel();
                                                }
                                            })
                                    .setNegativeButton(context.getResources().getString(R.string.cancel),
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    dialog.cancel();
                                                }
                                            }).show();
                        }
                    });
                }
                return null;
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
    protected void onProgressUpdate(Integer... progress) {
        super.onProgressUpdate(progress);
        progressDialog.setIndeterminate(false);
        progressDialog.setMax(100);
        progressDialog.setProgress(progress[0]);
    }

    @Override
    protected void onPostExecute(String result) {
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
        progressDialog.dismiss();

        if (result != null && result.equals("permission")) {
            View view = null;
            if (context instanceof ContentsActivity) {
                view = ((ContentsActivity) context).view;
            } else if (context instanceof ScheduleActivity) {
                view = ScheduleActivity.view;
            }
            if (view != null) {
                Snackbar snackbar = Snackbar.make(view, context.getResources().getString(R.string.no_permission),
                        Snackbar.LENGTH_LONG);
                snackbar.setAction(context.getResources().getString(R.string.fix), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                        intent.setData(uri);
                        context.startActivity(intent);
                    }
                });
                snackbar.show();
            }
            return;
        }
        if (result != null) {
            Uri uri;
            Intent target = new Intent(Intent.ACTION_VIEW);
            if (android.os.Build.VERSION.SDK_INT >= 24) {
                uri = FileProvider.getUriForFile(context,
                        "com.giovanniterlingen.windesheim.provider",
                        new File(result));
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
                context.startActivity(target);
            } catch (ActivityNotFoundException e) {
                View view = null;
                if (context instanceof ContentsActivity) {
                    view = ((ContentsActivity) context).view;
                } else if (context instanceof ScheduleActivity) {
                    view = ScheduleActivity.view;
                }
                if (view != null) {
                    Snackbar snackbar = Snackbar.make(view, context.getResources().getString(R.string.no_app_found),
                            Snackbar.LENGTH_SHORT);
                    snackbar.show();
                }
            }
        }
    }
}
