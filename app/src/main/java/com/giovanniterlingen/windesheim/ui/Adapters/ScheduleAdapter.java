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
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.R;
import com.giovanniterlingen.windesheim.ui.ScheduleActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class ScheduleAdapter extends CursorRecyclerViewAdapter<ScheduleAdapter.ViewHolder> {

    private static Activity context;
    private String dateString;
    private Date date;
    private String componentId;

    public ScheduleAdapter(Activity context, Cursor cursor, String dateString, String componentId,
                           Date date) {
        super(cursor);
        ScheduleAdapter.context = context;
        this.dateString = dateString;
        this.componentId = componentId;
        this.date = date;
    }

    @SuppressLint("SimpleDateFormat")
    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final Cursor cursor) {
        final TextView lessonName = viewHolder.lessonName;
        final TextView lessonTime = viewHolder.lessonTime;
        final TextView lessonRoom = viewHolder.lessonRoom;
        final TextView lessonComponent = viewHolder.lessonComponent;
        final FrameLayout menuButton = viewHolder.menuButton;

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
            viewHolder.cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cursor.moveToPosition(viewHolder.getAdapterPosition());
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
            viewHolder.cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cursor.moveToPosition(viewHolder.getAdapterPosition());
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
            viewHolder.cardView.setOnClickListener(null);
        }
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(context, menuButton);
                popupMenu.inflate(R.menu.schedule_menu);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        cursor.moveToPosition(viewHolder.getAdapterPosition());
                        if (item.getItemId() == R.id.hide_lesson) {
                            showPromptDialog(cursor.getLong(0));
                            return true;
                        }
                        if (item.getItemId() == R.id.save_lesson) {
                            showCalendarDialog(cursor.getLong(0));
                        }
                        return true;
                    }
                });
                popupMenu.show();
            }
        });
    }

    private void showCalendarDialog(final long lessonId) {
        Cursor cursor = ApplicationLoader.scheduleDatabase.getSingleLesson(lessonId);
        if (cursor.moveToFirst()) {
            String[] startTimeStrings = cursor.getString(0).split(":");
            String[] endTimeStrings = cursor.getString(1).split(":");

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);

            calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(startTimeStrings[0]));
            calendar.set(Calendar.MINUTE, Integer.parseInt(startTimeStrings[1]));

            Intent intent = new Intent(Intent.ACTION_EDIT);
            intent.setType("vnd.android.cursor.item/event");
            intent.putExtra("beginTime", calendar.getTimeInMillis());
            intent.putExtra("allDay", false);

            calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(endTimeStrings[0]));
            calendar.set(Calendar.MINUTE, Integer.parseInt(endTimeStrings[1]));

            intent.putExtra("endTime", calendar.getTimeInMillis());
            intent.putExtra("title", cursor.getString(2));
            intent.putExtra("eventLocation", cursor.getString(3));

            try {
                context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                ScheduleActivity.showSnackbar(context.getResources()
                        .getString(R.string.no_calendar_found));
            }
        }
        cursor.close();
    }

    private void showPromptDialog(final long lessonId) {
        ApplicationLoader.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(context)
                        .setTitle(context.getResources().getString(R.string.confirmation))
                        .setMessage(context.getResources().getString(R.string.deletion_description))
                        .setPositiveButton(context.getResources().getString(R.string.hide),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        ApplicationLoader.scheduleDatabase.hideLesson(lessonId);
                                        changeCursor(ApplicationLoader.scheduleDatabase
                                                .getLessons(dateString, componentId));
                                        final boolean isEmpty = getItemCount() == 0;
                                        if (isEmpty) {
                                            ScheduleActivity.updateFragmentView();
                                        }
                                        Snackbar snackbar = Snackbar.make(context.findViewById(R.id.coordinator_layout), context.getResources().getString(R.string.lesson_hidden), Snackbar.LENGTH_SHORT);
                                        snackbar.setAction(context.getResources().getString(R.string.undo), new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                ApplicationLoader.scheduleDatabase.restoreLessons(lessonId);
                                                changeCursor(ApplicationLoader.scheduleDatabase.getLessons(
                                                        dateString, componentId
                                                ));
                                                if (isEmpty) {
                                                    ScheduleActivity.updateFragmentView();
                                                }
                                                Snackbar snackbar1 = Snackbar.make(context.findViewById(R.id.coordinator_layout), context.getResources().getString(R.string.lesson_restored), Snackbar.LENGTH_SHORT);
                                                snackbar1.show();
                                                ApplicationLoader.restartNotificationThread();
                                            }
                                        });
                                        snackbar.show();
                                        ApplicationLoader.restartNotificationThread();
                                        dialog.cancel();
                                    }
                                })
                        .setNegativeButton(context.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        }).show();
            }
        });
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
        final FrameLayout menuButton;
        final CardView cardView;

        public ViewHolder(View view) {
            super(view);
            lessonName = (TextView) view.findViewById(R.id.schedule_list_row_name);
            lessonTime = (TextView) view.findViewById(R.id.schedule_list_row_time);
            lessonRoom = (TextView) view.findViewById(R.id.schedule_list_row_room);
            lessonComponent = (TextView) view.findViewById(R.id.schedule_list_row_component);
            menuButton = (FrameLayout) view.findViewById(R.id.menu_button);
            cardView = (CardView) view.findViewById(R.id.card);
        }
    }
}
