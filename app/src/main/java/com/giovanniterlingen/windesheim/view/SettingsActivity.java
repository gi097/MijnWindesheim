/**
 * Copyright (c) 2019 Giovanni Terlingen
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
package com.giovanniterlingen.windesheim.view;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.preference.PreferenceManager;

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.R;
import com.giovanniterlingen.windesheim.utils.CookieUtils;
import com.giovanniterlingen.windesheim.utils.NotificationUtils;
import com.google.android.material.snackbar.Snackbar;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences preferences;
    private TextView intervalTextview;
    private SwitchCompat lessonStart;
    private SwitchCompat darkMode;
    private CharSequence[] items;
    private int notificationId = NotificationUtils.NOTIFICATION_NOT_SET;

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

        lessonStart = findViewById(R.id.lesson_notification_switch);
        lessonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = preferences.edit();
                if (lessonStart.isChecked()) {
                    editor.putInt("notifications_type", NotificationUtils.NOTIFICATION_ALWAYS_ON);
                } else {
                    editor.putInt("notifications_type", NotificationUtils.NOTIFICATION_OFF);
                }
                editor.apply();

                ApplicationLoader.restartNotificationThread();
                updateIntervalTextView();
            }
        });
        int pref = preferences.getInt("notifications_type", 0);
        lessonStart.setChecked(pref != 0 && pref != NotificationUtils.NOTIFICATION_OFF);

        darkMode = findViewById(R.id.dark_mode_switch);
        darkMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("dark_mode", darkMode.isChecked());
                editor.commit(); // Make sure to use commit()

                if (darkMode.isChecked()) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
                restart();
            }
        });
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean useDarkMode = preferences.getBoolean("dark_mode", currentNightMode == Configuration.UI_MODE_NIGHT_YES);
        darkMode.setChecked(useDarkMode);

        Button deleteAccountButton = findViewById(R.id.logout_button);
        deleteAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CookieUtils.deleteCookies();
                SharedPreferences.Editor editor = preferences.edit();
                editor.remove("username");
                editor.remove("password");
                editor.apply();

                CoordinatorLayout coordinatorLayout = findViewById(R.id.coordinator_layout);
                Snackbar snackbar = Snackbar.make(coordinatorLayout,
                        getString(R.string.settings_logout_msg), Snackbar.LENGTH_SHORT);
                snackbar.show();
            }
        });

        intervalTextview = findViewById(R.id.settings_interval_textview);
        updateIntervalTextView();

        View intervalRow = findViewById(R.id.settings_interval_row);
        intervalRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createNotificationPrompt();
            }
        });

        final SwitchCompat scheduleChangeSwitch = findViewById(R.id.schedule_change_notification_switch);
        scheduleChangeSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("schedule_change_notification", scheduleChangeSwitch.isChecked());
                editor.apply();
            }
        });
        scheduleChangeSwitch.setChecked(preferences.getBoolean("schedule_change_notification", true));
    }

    private void updateIntervalTextView() {
        int interval = preferences.getInt("notifications_type",
                NotificationUtils.NOTIFICATION_NOT_SET);
        if (interval == NotificationUtils.NOTIFICATION_OFF) {
            intervalTextview.setText(getResources().getString(R.string.interval_off));
        } else if (interval != NotificationUtils.NOTIFICATION_NOT_SET) {
            intervalTextview.setText(items[interval - 2]);
        }
    }

    private void updateLessonSwitch() {
        int interval = preferences.getInt("notifications_type",
                NotificationUtils.NOTIFICATION_NOT_SET);
        lessonStart.setChecked(interval != NotificationUtils.NOTIFICATION_NOT_SET &&
                interval != NotificationUtils.NOTIFICATION_OFF);
    }

    private void createNotificationPrompt() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.settings_interval))
                .setSingleChoiceItems(items, preferences.getInt("notifications_type",
                        NotificationUtils.NOTIFICATION_OFF) - 2,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                notificationId = item;
                            }
                        });
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences.Editor editor = preferences.edit();
                if (notificationId > -1) {
                    int id = notificationId + 2;
                    editor.putInt("notifications_type", id);
                    editor.apply();

                    ApplicationLoader.restartNotificationThread();
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
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void restart() {
        Intent intent = new Intent(this, LaunchActivity.class);
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
        Runtime.getRuntime().exit(0);
    }
}
