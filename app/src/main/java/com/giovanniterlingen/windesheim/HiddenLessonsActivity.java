package com.giovanniterlingen.windesheim;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

/**
 * A scheduler app for Windesheim students
 *
 * @author Giovanni Terlingen
 */
public class HiddenLessonsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hidden_lessons);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Cursor cursor = ApplicationLoader.scheduleDatabase.getFilteredLessonsForAdapter();
        HiddenLessonsAdapter adapter = new HiddenLessonsAdapter(HiddenLessonsActivity.this, cursor);
        ListView listView = (ListView) findViewById(R.id.listview);
        listView.setAdapter(adapter);

        if (adapter.isEmpty()) {
            TextView emptyTextView = (TextView) findViewById(R.id.hidden_schedule_not_found);
            emptyTextView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
