package com.giovanniterlingen.windesheim;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A scheduler app for Windesheim students
 *
 * @author Giovanni Terlingen
 */
class ScheduleAdapter extends CursorAdapter {

    public ScheduleAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.schedule_adapter_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        TextView lessonName = (TextView) view.findViewById(R.id.schedule_list_row_name);
        TextView lessonTime = (TextView) view.findViewById(R.id.schedule_list_row_time);
        TextView lessonRoom = (TextView) view.findViewById(R.id.schedule_list_row_room);
        TextView lessonTeacher = (TextView) view.findViewById(R.id.schedule_list_row_component);

        String lessonSubject = cursor.getString(5);
        String lessonTimes = cursor.getString(3) + " - " + cursor.getString(4);
        String lessonLocation = cursor.getString(6);
        String lessonTeachers = cursor.getString(7);

        lessonName.setText(lessonSubject);
        lessonTime.setText(lessonTimes);
        lessonRoom.setText(lessonLocation);
        lessonTeacher.setText(lessonTeachers);
    }
}
