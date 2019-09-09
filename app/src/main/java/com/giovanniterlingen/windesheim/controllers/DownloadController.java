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

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.format.Formatter;
import android.util.SparseArray;

import androidx.core.content.FileProvider;

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.NotificationCenter;
import com.giovanniterlingen.windesheim.R;
import com.giovanniterlingen.windesheim.models.Download;
import com.giovanniterlingen.windesheim.view.NatschoolActivity;

import java.io.File;
import java.lang.ref.WeakReference;
import java.net.URI;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 * @author Thomas Visch
 */
public class DownloadController extends AsyncTask<String, Object, String>
        implements NotificationCenter.NotificationCenterDelegate {

    public static final SparseArray<Download> activeDownloads = new SparseArray<>();
    private final WeakReference<Activity> weakReference;
    private final String url;
    private final int studyRouteId;
    private final int contentId;
    private final int adapterPosition;
    private DownloadManager downloadManager;
    private long currentDownloadId = -1;

    public DownloadController(Activity activity, String url, int studyRouteId, int contentId,
                              int adapterPosition) {
        this.weakReference = new WeakReference<>(activity);
        this.url = url;
        this.studyRouteId = studyRouteId;
        this.contentId = contentId;
        this.adapterPosition = adapterPosition;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        NotificationCenter.getInstance().addObserver(this,
                NotificationCenter.downloadCancelled);
        NotificationCenter.getInstance().postNotificationName(NotificationCenter.downloadPending,
                studyRouteId, adapterPosition, contentId);
    }

    @Override
    protected String doInBackground(final String... strings) {
        Activity activity = weakReference.get();
        if (activity == null || activity.isFinishing()) {
            return null;
        }

        try {
            activeDownloads.put(contentId, new Download());
            int lastSlash = url.lastIndexOf('/');
            String fileName = url.substring(lastSlash + 1);

            File directory = new File(activity.getExternalFilesDir(null), ApplicationLoader
                    .applicationContext.getResources().getString(R.string.app_name));
            if (!directory.exists()) {
                directory.mkdirs();
            }
            File file = new File(directory, fileName);

            final String encodedUrl = new URI("https", "elo.windesheim.nl", url,
                    null).toString();

            downloadManager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(encodedUrl));
            request.addRequestHeader("Cookie", CookieController.getNatSchoolCookie())
                    .setTitle(fileName)
                    .setDescription(activity.getResources().getString(R.string.downloading))
                    .setDestinationUri(Uri.fromFile(file));

            currentDownloadId = downloadManager.enqueue(request);
            while (true) {
                if (isCancelled()) {
                    return "cancelled";
                }
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(currentDownloadId);
                Cursor cursor = downloadManager.query(query);
                if (cursor.getCount() == 0) {
                    return "cancelled";
                }
                cursor.moveToFirst();
                int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                if (status == DownloadManager.STATUS_SUCCESSFUL ||
                        status == DownloadManager.STATUS_FAILED) {
                    break;
                }
                if (status == DownloadManager.STATUS_PAUSED ||
                        status == DownloadManager.STATUS_PENDING) {
                    // paused, reset download state to pending
                    activeDownloads.put(contentId, new Download());
                    NotificationCenter.getInstance()
                            .postNotificationName(NotificationCenter.downloadPending, studyRouteId,
                                    adapterPosition, contentId);
                    Thread.sleep(100);
                    continue;
                }
                long downloaded = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                long total = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                cursor.close();
                if (total > 0 && downloaded > 0) {
                    int progress = (int) (downloaded * 100 / total);
                    String s = Formatter.formatFileSize(activity, downloaded) + "/"
                            + Formatter.formatFileSize(activity, total);
                    activeDownloads.get(contentId).setProgress(progress);
                    activeDownloads.get(contentId).setProgressString(s);
                    publishProgress(progress, s);
                }
                Thread.sleep(100);
            }
            return file.getAbsolutePath();
        } catch (SecurityException e) {
            return "permission";
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void onProgressUpdate(Object... progress) {
        super.onProgressUpdate(progress);
        NotificationCenter.getInstance()
                .postNotificationName(NotificationCenter.downloadUpdated, studyRouteId,
                        adapterPosition, contentId, progress[0], progress[1]);
    }

    @Override
    protected void onCancelled(String s) {
        super.onCancelled(s);
        if (currentDownloadId > -1 && downloadManager != null) {
            downloadManager.remove(currentDownloadId);
        }
        onPostExecute("cancelled");
    }

    @Override
    protected void onPostExecute(final String result) {
        activeDownloads.remove(contentId);
        NotificationCenter.getInstance().removeObserver(this,
                NotificationCenter.downloadCancelled);
        NotificationCenter.getInstance().postNotificationName(NotificationCenter.downloadFinished,
                studyRouteId, adapterPosition, contentId);

        Activity activity = weakReference.get();
        if (activity == null || activity.isFinishing()) {
            return;
        }

        if ("permission".equals(result)) {
            ((NatschoolActivity) activity).noPermission();
            return;
        }
        if ("cancelled".equals(result)) {
            ((NatschoolActivity) activity).downloadCanceled();
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
        if (NotificationCenter.downloadCancelled == id && contentId == (int) args[0]) {
            this.cancel(true);
        }
    }
}
