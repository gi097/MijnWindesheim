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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class CalendarController {

    private final SimpleDateFormat yearMonthDayDateTimeFormat = new SimpleDateFormat("yyyyMMddHHmm", Locale.FRANCE);
    private final SimpleDateFormat yearMonthDayDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.FRANCE);
    private final SimpleDateFormat dayDateFormat = new SimpleDateFormat("dd", Locale.FRANCE);

    private static volatile CalendarController Instance = null;

    public static CalendarController getInstance() {
        CalendarController localInstance = Instance;
        if (localInstance == null) {
            synchronized (CalendarController.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new CalendarController();
                }
            }
        }
        return localInstance;
    }

    public SimpleDateFormat getYearMonthDayDateTimeFormat() {
        return yearMonthDayDateTimeFormat;
    }

    public SimpleDateFormat getYearMonthDayDateFormat() {
        return yearMonthDayDateFormat;
    }

    public SimpleDateFormat getDayDateFormat() {
        return dayDateFormat;
    }

    public Calendar getCalendar() {
        return GregorianCalendar.getInstance(Locale.FRANCE);
    }

    String[] getWeekDates(Date date) {
        Calendar calendar = GregorianCalendar.getInstance(Locale.FRANCE);
        calendar.setTime(date);
        calendar.set(GregorianCalendar.DAY_OF_WEEK, GregorianCalendar.MONDAY);
        String lowestDate = yearMonthDayDateFormat.format(calendar.getTime());
        calendar.add(GregorianCalendar.DATE, 6);
        String highestDate = yearMonthDayDateFormat.format(calendar.getTime());
        return new String[]{lowestDate, highestDate};
    }
}
