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
package com.giovanniterlingen.windesheim.models;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class Lesson {

    private long rowId;
    private final int id;
    private final String subject;
    private final String date;
    private final String startTime;
    private String endTime;
    private final String room;
    private final String teacher;
    private final String className;
    private final int scheduleId;
    private final int scheduleType;
    private final int visible;

    public Lesson(int id, String subject, String date, String startTime, String endTime, String room, String teacher, String className, int scheduleId, int scheduleType, int visible) {
        this.id = id;
        this.subject = subject;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.room = room;
        this.teacher = teacher;
        this.className = className;
        this.scheduleId = scheduleId;
        this.scheduleType = scheduleType;
        this.visible = visible;
    }

    public Lesson(long rowId, int id, String subject, String date, String startTime, String endTime, String room, String teacher, String className, int scheduleId, int scheduleType, int visible) {
        this.rowId = rowId;
        this.id = id;
        this.subject = subject;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.room = room;
        this.teacher = teacher;
        this.className = className;
        this.scheduleId = scheduleId;
        this.scheduleType = scheduleType;
        this.visible = visible;
    }

    public long getRowId() {
        return rowId;
    }

    public int getId() {
        return id;
    }

    public String getSubject() {
        return subject;
    }

    public String getDate() {
        return date;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getRoom() {
        return room;
    }

    public String getTeacher() {
        return teacher;
    }

    public String getClassName() {
        return className;
    }

    public int getScheduleId() {
        return scheduleId;
    }

    public int getScheduleType() {
        return scheduleType;
    }

    public int getVisible() {
        return visible;
    }

}
