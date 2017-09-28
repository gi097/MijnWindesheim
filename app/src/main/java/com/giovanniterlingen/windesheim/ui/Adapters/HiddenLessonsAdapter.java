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
package com.giovanniterlingen.windesheim.ui.Adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.R;
import com.giovanniterlingen.windesheim.handlers.ColorHandler;
import com.giovanniterlingen.windesheim.objects.Lesson;
import com.giovanniterlingen.windesheim.ui.HiddenLessonsActivity;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class HiddenLessonsAdapter extends RecyclerView.Adapter<HiddenLessonsAdapter.ViewHolder> {

    private final HiddenLessonsActivity activity;
    private Lesson[] lessons;

    public HiddenLessonsAdapter(HiddenLessonsActivity activity, Lesson[] lessons) {
        this.activity = activity;
        this.lessons = lessons;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(activity).
                inflate(R.layout.adapter_item_hidden_schedule, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        TextView lessonName = holder.lessonName;
        TextView lessonComponent = holder.lessonComponent;
        View scheduleIdentifier = holder.scheduleIdentifier;

        Lesson lesson = lessons[position];
        lessonName.setText(lesson.getSubject());
        lessonComponent.setText(lesson.getScheduleType() == 2 ? lesson.getClassName() : lesson.getTeacher());
        lessonComponent.setSelected(true);

        scheduleIdentifier.setBackgroundColor(ColorHandler.getColorById(lesson.getScheduleId()));

        Button button = holder.itemView.findViewById(R.id.restore_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Lesson lesson = lessons[holder.getAdapterPosition()];
                ApplicationLoader.scheduleDatabase.restoreLesson(lesson.getId());
                activity.showSnackbar();
                ApplicationLoader.restartNotificationThread();

                updateLessons(ApplicationLoader.scheduleDatabase.getHiddenLessons());
                if (lessons.length == 0) {
                    activity.showEmptyTextView();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return lessons.length;
    }

    private void updateLessons(Lesson[] lessons) {
        this.lessons = lessons;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        final TextView lessonName;
        final TextView lessonComponent;
        final View scheduleIdentifier;

        ViewHolder(View view) {
            super(view);
            lessonName = view.findViewById(R.id.schedule_list_row_name);
            lessonComponent = view.findViewById(R.id.schedule_list_row_component);
            scheduleIdentifier = view.findViewById(R.id.schedule_identifier);
        }
    }
}
