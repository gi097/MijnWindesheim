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

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.NetworkReceiver;
import com.giovanniterlingen.windesheim.R;
import com.giovanniterlingen.windesheim.ui.AuthenticationActivity;
import com.giovanniterlingen.windesheim.ui.EducatorActivity;
import com.giovanniterlingen.windesheim.ui.NatschoolActivity;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class CookieHandler {

    public static void checkCookieAndIntent(final Context context, final boolean educator) {
        if (NetworkReceiver.isConnected()) {
            if (educator) {
                if (getEducatorCookie() != null && getEducatorCookie().length() > 0) {
                    Intent intent = new Intent(context, EducatorActivity.class);
                    context.startActivity(intent);
                } else {
                    Intent intent = new Intent(context, AuthenticationActivity.class);
                    intent.putExtra("educator", true);
                    context.startActivity(intent);
                }
            } else {
                if (getNatSchoolCookie() != null && getNatSchoolCookie().length() > 0) {
                    Intent intent = new Intent(context, NatschoolActivity.class);
                    context.startActivity(intent);
                } else {
                    Intent intent = new Intent(context, AuthenticationActivity.class);
                    intent.putExtra("educator", false);
                    context.startActivity(intent);
                }
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
                                            checkCookieAndIntent(context, educator);
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
    }

    public static String getNatSchoolCookie() {
        CookieManager cookieManager = CookieManager.getInstance();
        if (Build.VERSION.SDK_INT < 21) {
            CookieSyncManager.createInstance(ApplicationLoader.applicationContext);
        }
        return cookieManager.getCookie("https://elo.windesheim.nl");
    }

    public static String getEducatorCookie() {
        CookieManager cookieManager = CookieManager.getInstance();
        if (Build.VERSION.SDK_INT < 21) {
            CookieSyncManager.createInstance(ApplicationLoader.applicationContext);
        }
        return cookieManager.getCookie("https://windesheimapi.azurewebsites.net");
    }

    public static void deleteCookies() {
        CookieManager cookieManager = CookieManager.getInstance();
        if (Build.VERSION.SDK_INT < 21) {
            CookieSyncManager.createInstance(ApplicationLoader.applicationContext);
        }
        cookieManager.removeAllCookie();
    }
}
