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
package com.giovanniterlingen.windesheim.ui;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.R;
import com.giovanniterlingen.windesheim.handlers.CookieHandler;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences preferences;
    private TextView intervalTextview;
    private SwitchCompat lessonStart;
    private CharSequence[] items;
    private int notificationId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        items = new CharSequence[]{getResources().getString(R.string.interval_one_hour),
                getResources().getString(R.string.interval_thirty_minutes),
                getResources().getString(R.string.interval_fifteen_minutes),
                getResources().getString(R.string.interval_always_on)};
        preferences = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this);

        lessonStart = (SwitchCompat) findViewById(R.id.lesson_notification_switch);
        if (lessonStart != null) {
            lessonStart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SharedPreferences.Editor editor = preferences.edit();
                    if (lessonStart.isChecked()) {
                        editor.putInt("notifications_type", 5);
                    } else {
                        editor.putInt("notifications_type", 6);
                    }
                    editor.apply();
                    if (ApplicationLoader.notificationHandler != null) {
                        ApplicationLoader.notificationHandler.clearNotification();
                    }
                    ApplicationLoader.restartNotificationThread();
                    updateIntervalTextView();
                }
            });
            if (preferences.getInt("notifications_type", -1) == 6) {
                lessonStart.setChecked(false);
            } else {
                lessonStart.setChecked(true);
            }

            Button deleteAccountButton = (Button) findViewById(R.id.logout_button);
            deleteAccountButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    CookieHandler.deleteCookie();
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.remove("username");
                    editor.remove("password");
                    editor.apply();

                    CoordinatorLayout coordinatorLayout = (CoordinatorLayout)
                            findViewById(R.id.coordinator_layout);
                    Snackbar snackbar = Snackbar.make(coordinatorLayout,
                            getString(R.string.settings_logout_msg), Snackbar.LENGTH_SHORT);
                    snackbar.show();
                }
            });
        }

        final SwitchCompat lessonChange = (SwitchCompat) findViewById(R.id.change_notification_switch);
        if (lessonChange != null) {
            lessonChange.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean("schedule_change_service", lessonChange.isChecked());
                    editor.apply();
                    ApplicationLoader.restartScheduleChangeHandlerThread();
                }
            });
            lessonChange.setChecked(preferences.getBoolean("schedule_change_service", true));
        }

        intervalTextview = (TextView) findViewById(R.id.settings_interval_textview);
        updateIntervalTextView();

        View intervalRow = findViewById(R.id.settings_interval_row);
        if (intervalRow != null) {
            intervalRow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    createNotificationPrompt();
                }
            });
        }
    }

    private void updateIntervalTextView() {
        if (intervalTextview != null) {
            int interval = preferences.getInt("notifications_type", -1);
            if (interval == 6) {
                intervalTextview.setText(getResources().getString(R.string.interval_off));
            } else if (interval > -1) {
                intervalTextview.setText(items[interval - 2]);
            }
        }
    }

    private void updateLessonSwitch() {
        if (lessonStart != null) {
            int interval = preferences.getInt("notifications_type", -1);
            lessonStart.setChecked(interval > -1 && interval != 6);
        }
    }

    private void createNotificationPrompt() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.settings_interval))
                .setSingleChoiceItems(items, preferences.getInt("notifications_type", 0) - 2,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                notificationId = item;
                            }
                        });
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences.Editor editor = preferences.edit();
                // Since we use an array, the index is 0 based.
                // Add 2 to support my older versions
                if (notificationId > -1) {
                    int id = notificationId + 2;
                    switch (notificationId) {
                        case 0:
                        case 1:
                        case 2:
                            editor.putInt("notifications_type", id);
                            editor.apply();
                            if (ApplicationLoader.notificationHandler != null) {
                                ApplicationLoader.notificationHandler.clearNotification();
                            }
                            ApplicationLoader.restartNotificationThread();
                            break;
                        case 3:
                            editor.putInt("notifications_type", id);
                            editor.apply();
                            if (ApplicationLoader.notificationHandler != null) {
                                ApplicationLoader.notificationHandler.clearNotification();
                            }
                            ApplicationLoader.restartNotificationThread();
                            break;
                        case 4:
                            editor.putInt("notifications_type", id);
                            editor.apply();
                            if (ApplicationLoader.notificationHandler != null) {
                                ApplicationLoader.notificationHandler.clearNotification();
                            }
                            ApplicationLoader.restartNotificationThread();
                            break;

                    }
                    updateLessonSwitch();
                    updateIntervalTextView();
                    dialog.dismiss();
                }
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
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
