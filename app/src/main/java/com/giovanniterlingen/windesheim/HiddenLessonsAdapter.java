package com.giovanniterlingen.windesheim;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * A schedule app for Windesheim students
 *
 * @author Giovanni Terlingen
 */
class HiddenLessonsAdapter extends CursorAdapter {

    public HiddenLessonsAdapter(Activity context, Cursor cursor) {
        super(context, cursor, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.hidden_schedule_adapter_item, parent, false);
    }

    @Override
    public void bindView(final View view, Context context, final Cursor cursor) {
        TextView lessonName = (TextView) view.findViewById(R.id.schedule_list_row_name);
        TextView lessonComponent = (TextView) view.findViewById(R.id.schedule_list_row_component);
        lessonName.setText(cursor.getString(1));
        lessonComponent.setText(cursor.getString(2));
        final int position = cursor.getPosition();

        Button button = (Button) view.findViewById(R.id.restore_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                cursor.moveToPosition(position);
                ApplicationLoader.scheduleDatabase.restoreLessons(cursor.getLong(0));
                HiddenLessonsActivity.showSnackbar();
                ApplicationLoader.restartNotificationThread();
                changeCursor(ApplicationLoader.scheduleDatabase.getFilteredLessonsForAdapter());
                if (isEmpty()) {
                    TextView emptyTextView = (TextView) view.findViewById(R.id.hidden_schedule_not_found);
                    emptyTextView.setVisibility(View.VISIBLE);
                }
            }
        });
    }
}
