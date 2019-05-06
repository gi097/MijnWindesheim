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

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import androidx.appcompat.app.AlertDialog;

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.R;
import com.giovanniterlingen.windesheim.view.AuthenticationActivity;
import com.giovanniterlingen.windesheim.view.EducatorActivity;
import com.giovanniterlingen.windesheim.view.NatschoolActivity;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class CookieController {

    public static void checkCookieAndIntent(final Context context, final boolean educator) {
        if (ApplicationLoader.isConnected()) {
            if (educator) {
                if (getEducatorCookie() != null && getEducatorCookie().length() > 0) {
                    Intent intent = new Intent(context, EducatorActivity.class);
                    context.startActivity(intent);
                    return;
                }
                Intent intent = new Intent(context, AuthenticationActivity.class);
                intent.putExtra("educator", true);
                context.startActivity(intent);
                return;
            }
            if (getNatSchoolCookie() != null && getNatSchoolCookie().length() > 0) {
                Intent intent = new Intent(context, NatschoolActivity.class);
                context.startActivity(intent);
                return;
            }
            Intent intent = new Intent(context, AuthenticationActivity.class);
            intent.putExtra("educator", false);
            context.startActivity(intent);
            return;

        }
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

    public static String getNatSchoolCookie() {
        CookieManager cookieManager = CookieManager.getInstance();
        if (Build.VERSION.SDK_INT < 21) {
            CookieSyncManager.createInstance(ApplicationLoader.applicationContext);
        }
        return cookieManager.getCookie("https://elo.windesheim.nl");
    }

    static String getEducatorCookie() {
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
