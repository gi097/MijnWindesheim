package com.giovanniterlingen.windesheim;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A schedule app for Windesheim students
 *
 * @author Giovanni Terlingen
 */
class ScheduleAdapter extends CursorAdapter {

    public ScheduleAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.schedule_adapter_item, parent, false);
    }

    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {
        TextView lessonName = (TextView) view.findViewById(R.id.schedule_list_row_name);
        final TextView lessonTime = (TextView) view.findViewById(R.id.schedule_list_row_time);
        TextView lessonRoom = (TextView) view.findViewById(R.id.schedule_list_row_room);
        TextView lessonComponent = (TextView) view.findViewById(R.id.schedule_list_row_component);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmm");
        long databaseDateStart = Long.parseLong(cursor.getString(2).replaceAll("-", "") + cursor.getString(3).replaceAll(":", ""));
        long databaseDateEnd = Long.parseLong(cursor.getString(2).replaceAll("-", "") + cursor.getString(4).replaceAll(":", ""));
        long currentDate = Long.parseLong(simpleDateFormat.format(new Date()));

        lessonName.setText(cursor.getString(5));
        lessonRoom.setText(cursor.getString(6));
        lessonComponent.setText(cursor.getString(7));

        if (databaseDateStart <= currentDate && databaseDateEnd >= currentDate) {
            lessonTime.setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
            lessonTime.setText(ApplicationLoader.applicationContext.getResources().getString(R.string.lesson_started));
            lessonTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!lessonTime.getText().toString().equals(ApplicationLoader.applicationContext.getResources().getString(R.string.lesson_started))) {
                        TranslateAnimation animation = new TranslateAnimation(
                                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                                Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF, 0.0f
                        );
                        animation.setDuration(100);
                        lessonTime.setAnimation(animation);
                        lessonTime.setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
                        lessonTime.setText(ApplicationLoader.applicationContext.getResources().getString(R.string.lesson_started));
                    } else {
                        TranslateAnimation animation = new TranslateAnimation(
                                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                                Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, 0.0f
                        );
                        animation.setDuration(100);
                        lessonTime.setAnimation(animation);
                        String lessonTimes = cursor.getString(3) + " - " + cursor.getString(4);
                        lessonTime.setTextColor(ContextCompat.getColor(context, R.color.colorSecondaryText));
                        lessonTime.setText(lessonTimes);
                    }
                }
            });
        } else if (databaseDateEnd < currentDate) {
            lessonTime.setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
            lessonTime.setText(ApplicationLoader.applicationContext.getResources().getString(R.string.finished));
            lessonTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!lessonTime.getText().toString().equals(ApplicationLoader.applicationContext.getResources().getString(R.string.finished))) {
                        TranslateAnimation animation = new TranslateAnimation(
                                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                                Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF, 0.0f
                        );
                        animation.setDuration(100);
                        lessonTime.setAnimation(animation);
                        lessonTime.setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
                        lessonTime.setText(ApplicationLoader.applicationContext.getResources().getString(R.string.finished));
                    } else {
                        TranslateAnimation animation = new TranslateAnimation(
                                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                                Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, 0.0f
                        );
                        animation.setDuration(100);
                        lessonTime.setAnimation(animation);
                        String lessonTimes = cursor.getString(3) + " - " + cursor.getString(4);
                        lessonTime.setTextColor(ContextCompat.getColor(context, R.color.colorSecondaryText));
                        lessonTime.setText(lessonTimes);
                    }
                }
            });
        } else {
            String lessonTimes = cursor.getString(3) + " - " + cursor.getString(4);
            lessonTime.setTextColor(ContextCompat.getColor(context, R.color.colorSecondaryText));
            lessonTime.setText(lessonTimes);
        }
    }
}
