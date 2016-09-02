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
package com.giovanniterlingen.windesheim.objects;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class Schedule {

    private final String componentId;
    private final String date;
    private final String start;
    private final String end;
    private final String name;
    private final String room;
    private final String component;
    private final String classId;

    public Schedule(String componentId, String date, String start, String end, String name,
                    String room, String component, String classId) {
        this.componentId = componentId;
        this.date = date;
        this.start = start;
        this.end = end;
        this.name = name;
        this.room = room;
        this.component = component;
        this.classId = classId;
    }

    /**
     * Here we will check if the schedule contains the same values
     *
     * @param object The schedule object
     * @return true or false depending on the situation
     */
    @Override
    public boolean equals(Object object) {
        if (object instanceof Schedule) {
            try {
                if (this.componentId.equals(((Schedule) object).componentId) &&
                        this.date.equals(((Schedule) object).date) &&
                        this.start.equals(((Schedule) object).start) &&
                        this.end.equals(((Schedule) object).end) &&
                        this.name.equals(((Schedule) object).name) &&
                        this.room.equals(((Schedule) object).room) &&
                        this.component.equals(((Schedule) object).component) &&
                        this.classId.equals(((Schedule) object).classId)) {
                    return true;
                }
            } catch (NullPointerException e) {
                return false;
            }
        }
        return false;
    }
}
