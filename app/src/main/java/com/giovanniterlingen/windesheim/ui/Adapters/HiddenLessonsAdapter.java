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

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.R;
import com.giovanniterlingen.windesheim.ui.HiddenLessonsActivity;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class HiddenLessonsAdapter extends CursorRecyclerViewAdapter<HiddenLessonsAdapter.ViewHolder> {

    private final Context context;

    public HiddenLessonsAdapter(Context context, Cursor cursor) {
        super(cursor);
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).
                inflate(R.layout.hidden_schedule_adapter_item, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final Cursor cursor) {
        TextView lessonName = viewHolder.lessonName;
        TextView lessonComponent = viewHolder.lessonComponent;
        lessonName.setText(cursor.getString(1));
        lessonComponent.setText(cursor.getString(2));
        final int position = cursor.getPosition();

        Button button = (Button) viewHolder.itemView.findViewById(R.id.restore_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                cursor.moveToPosition(position);
                ApplicationLoader.scheduleDatabase.restoreLessons(cursor.getLong(0));
                HiddenLessonsActivity.showSnackbar();
                ApplicationLoader.restartNotificationThread();
                changeCursor(ApplicationLoader.scheduleDatabase.getFilteredLessonsForAdapter());
                if (getCursor().getCount() == 0) {
                    HiddenLessonsActivity.showEmptyTextView();
                }
            }
        });
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        final TextView lessonName;
        final TextView lessonComponent;

        public ViewHolder(View view) {
            super(view);
            lessonName = (TextView) view.findViewById(R.id.schedule_list_row_name);
            lessonComponent = (TextView) view.findViewById(R.id.schedule_list_row_component);
        }
    }
}
