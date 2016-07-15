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
package com.giovanniterlingen.windesheim.ui.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.TextView;

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.R;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class ScheduleAdapter extends CursorRecyclerViewAdapter<ScheduleAdapter.ViewHolder> {

    private static Context context;
    private int position;

    public ScheduleAdapter(Context context, Cursor cursor) {
        super(cursor);
        ScheduleAdapter.context = context;
    }

    @SuppressLint("SimpleDateFormat")
    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final Cursor cursor) {
        TextView lessonName = viewHolder.lessonName;
        final TextView lessonTime = viewHolder.lessonTime;
        TextView lessonRoom = viewHolder.lessonRoom;
        TextView lessonComponent = viewHolder.lessonComponent;

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
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
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
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
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
            viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    setPosition(viewHolder.getAdapterPosition());
                    return false;
                }
            });
        }
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        holder.itemView.setOnLongClickListener(null);
        super.onViewRecycled(holder);
    }

    /**
     * Gets the row id in SQLite based on the adapter's position.
     *
     * @return the id
     */
    public long getLessonId() {
        return super.getItemId(position);
    }

    private void setPosition(int position) {
        this.position = position;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).
                inflate(R.layout.schedule_adapter_item, parent, false);
        return new ViewHolder(itemView);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        final TextView lessonName;
        final TextView lessonTime;
        final TextView lessonRoom;
        final TextView lessonComponent;

        public ViewHolder(View view) {
            super(view);
            lessonName = (TextView) view.findViewById(R.id.schedule_list_row_name);
            lessonTime = (TextView) view.findViewById(R.id.schedule_list_row_time);
            lessonRoom = (TextView) view.findViewById(R.id.schedule_list_row_room);
            lessonComponent = (TextView) view.findViewById(R.id.schedule_list_row_component);
        }
    }
}
