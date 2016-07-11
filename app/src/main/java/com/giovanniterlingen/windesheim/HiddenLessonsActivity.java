package com.giovanniterlingen.windesheim;

import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

/**
 * A schedule app for Windesheim students
 *
 * @author Giovanni Terlingen
 */
public class HiddenLessonsActivity extends AppCompatActivity {

    private static View view;

    public static void showSnackbar() {
        ApplicationLoader.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                if (view != null) {
                    Snackbar snackbar = Snackbar.make(view, ApplicationLoader.applicationContext.getResources().getString(R.string.lesson_restored), Snackbar.LENGTH_SHORT);
                    snackbar.show();
                }
            }
        });
    }

    public static void showEmptyTextView() {
        ApplicationLoader.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                if (view != null) {
                    TextView emptyTextView = (TextView) view.findViewById(R.id.hidden_schedule_not_found);
                    emptyTextView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hidden_lessons);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        view = findViewById(R.id.schedule_coordinator_layout);
        Cursor cursor = ApplicationLoader.scheduleDatabase.getFilteredLessonsForAdapter();
        HiddenLessonsAdapter adapter = new HiddenLessonsAdapter(HiddenLessonsActivity.this, cursor);
        ListView listView = (ListView) findViewById(R.id.listview);
        listView.setAdapter(adapter);

        if (adapter.isEmpty()) {
            showEmptyTextView();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
