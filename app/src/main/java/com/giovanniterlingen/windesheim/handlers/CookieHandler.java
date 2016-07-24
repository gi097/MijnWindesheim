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

import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import com.giovanniterlingen.windesheim.ApplicationLoader;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class CookieHandler {

    public static boolean hasCookie() {
        if (getCookie() == null || getCookie().length() == 0) {
            resetCookie();
            return false;
        }
        return true;
    }

    private static void resetCookie() {
        CookieSyncManager.createInstance(ApplicationLoader.applicationContext);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setCookie("https://elo.windesheim.nl/Pages/mobile/", "N%40TCookie=");
        cookieManager.removeSessionCookie();
    }

    public static String getCookie() {
        CookieSyncManager.createInstance(ApplicationLoader.applicationContext);
        CookieManager cookieManager = CookieManager.getInstance();
        return cookieManager.getCookie("https://elo.windesheim.nl/Pages/mobile/");
    }
}
