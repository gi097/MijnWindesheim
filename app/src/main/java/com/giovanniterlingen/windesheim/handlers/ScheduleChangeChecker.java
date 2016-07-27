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

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;

import com.giovanniterlingen.windesheim.ApplicationLoader;

import java.util.Date;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class ScheduleChangeChecker extends Thread {

    @Override
    public void run() {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(ApplicationLoader.applicationContext);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String componentId = sharedPreferences.getString("componentId", "");
        int type = sharedPreferences.getInt("type", 0);
        while (true) {
            if (sharedPreferences.getLong("checkTime", 0) == 0 ||
                    !DateUtils.isToday(sharedPreferences.getLong("checkTime", 0))) {
                try {
                    Date date = new Date();
                    ScheduleHandler.saveSchedule(ScheduleHandler.getScheduleFromServer(
                            componentId, date, type), date, componentId, true);
                    editor.putLong("checkTime", System.currentTimeMillis());
                    if (android.os.Build.VERSION.SDK_INT >= 9) {
                        editor.apply();
                    } else {
                        editor.commit();
                    }
                } catch (Exception ignore) {
                    // We don't need to fix this, because we need to recheck anyway
                }
            }
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                //
            }
        }
    }
}
