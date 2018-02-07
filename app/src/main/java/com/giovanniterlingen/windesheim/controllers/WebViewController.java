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
import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.webkit.DownloadListener;
import android.webkit.WebView;

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.R;

import java.io.File;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class WebViewController {

    private final Activity activity;

    public WebViewController(Activity activity) {
        this.activity = activity;
    }

    public WebView createWebView() {
        WebView webView = new WebView(activity);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);

        webView.setDownloadListener(new DownloadListener() {
            public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                        String mimetype, long contentLength) {
                int lastSlash = url.lastIndexOf('/');
                String fileName = url.substring(lastSlash + 1);

                DownloadManager downloadManager = (DownloadManager) activity
                        .getSystemService(Context.DOWNLOAD_SERVICE);
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.addRequestHeader("Cookie", new CookieController().getNatSchoolCookie())
                        .setTitle(fileName)
                        .setDescription(activity.getResources().getString(R.string.downloading))
                        .setDestinationInExternalPublicDir(File.separator +
                                ApplicationLoader.applicationContext.getResources()
                                        .getString(R.string.app_name), fileName)
                        .setNotificationVisibility(DownloadManager.Request.
                                VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                downloadManager.enqueue(request);
            }
        });
        return webView;
    }

    public void intentCustomTab(String url) {
        Uri uri = Uri.parse(url);

        CustomTabsIntent.Builder intentBuilder = new CustomTabsIntent.Builder();
        intentBuilder.setToolbarColor(ContextCompat.getColor(activity, R.color.colorPrimary));
        intentBuilder.setSecondaryToolbarColor(ContextCompat.getColor(activity,
                R.color.colorPrimary));
        intentBuilder.setStartAnimations(activity, R.anim.slide_in_right, R.anim.slide_out_left);
        intentBuilder.setExitAnimations(activity, R.anim.slide_in_left, R.anim.slide_out_right);

        CustomTabsIntent customTabsIntent = intentBuilder.build();
        customTabsIntent.launchUrl(activity, uri);
    }
}
