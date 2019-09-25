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
package com.giovanniterlingen.windesheim.view.Fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteConstraintException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.Constants;
import com.giovanniterlingen.windesheim.R;
import com.giovanniterlingen.windesheim.controllers.DatabaseController;
import com.giovanniterlingen.windesheim.controllers.WindesheimAPIController;
import com.giovanniterlingen.windesheim.models.ScheduleItem;
import com.giovanniterlingen.windesheim.utils.ColorUtils;
import com.giovanniterlingen.windesheim.utils.TelemetryUtils;
import com.giovanniterlingen.windesheim.view.Adapters.ChooseScheduleAdapter;
import com.giovanniterlingen.windesheim.view.ScheduleActivity;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class ChooseScheduleFragment extends Fragment {

    private Constants.SCHEDULE_TYPE type;
    private ChooseScheduleAdapter adapter;
    private RecyclerView recyclerView;
    private Context context;
    private ProgressBar spinner;
    private View view;
    private boolean isViewShown = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        if (this.getArguments() == null) {
            return;
        }
        int position = this.getArguments().getInt("position");
        switch (position) {
            case 0:
                type = Constants.SCHEDULE_TYPE.CLASS;
                break;
            case 1:
                type = Constants.SCHEDULE_TYPE.TEACHER;
                break;
            case 2:
                type = Constants.SCHEDULE_TYPE.SUBJECT;
                break;
        }
    }

    public void onVisible() {
        if (getView() != null) {
            isViewShown = true;
            startTask();
            return;
        }
        isViewShown = false;
    }

    private void startTask() {
        adapter = null;
        if (recyclerView != null) {
            recyclerView.setAdapter(null);
        }
        ComponentFetcher componentFetcher = new ComponentFetcher(ChooseScheduleFragment.this);
        componentFetcher.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_choose_schedule, container, false);
        TextView chooseTextview = view.findViewById(R.id.choose_textview);
        TextView descriptionTextview = view.findViewById(R.id.description_textview);
        EditText dataSearch = view.findViewById(R.id.filter_edittext);
        if (type == Constants.SCHEDULE_TYPE.CLASS) {
            chooseTextview.setText(getResources().getString(R.string.choose_class));
            descriptionTextview.setText(getResources().getString(R.string
                    .choose_class_description));
            dataSearch.setHint(getResources().getString(R.string.choose_class_hint));
        } else if (type == Constants.SCHEDULE_TYPE.TEACHER) {
            chooseTextview.setText(getResources().getString(R.string.choose_teacher));
            descriptionTextview.setText(getResources().getString(R.string
                    .choose_teacher_description));
            dataSearch.setHint(getResources().getString(R.string.choose_teacher_hint));
        } else if (type == Constants.SCHEDULE_TYPE.SUBJECT) {
            chooseTextview.setText(getResources().getString(R.string.choose_subject));
            descriptionTextview.setText(getResources().getString(R.string
                    .choose_subject_description));
            dataSearch.setHint(getResources().getString(R.string.choose_subject_hint));
        }
        recyclerView = view.findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        spinner = view.findViewById(R.id.progress_bar);
        dataSearch.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                if (adapter != null) {
                    adapter.filter(arg0.toString());
                }
            }

            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }

            public void afterTextChanged(Editable s) {
            }
        });
        if (!isViewShown) {
            startTask();
        }
        return view;
    }

    private void alertConnectionProblem() {
        if (!isVisible() && !isMenuVisible()) {
            return;
        }
        ApplicationLoader.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(context)
                        .setTitle(getResources().getString(R.string.alert_connection_title))
                        .setMessage(getResources().getString(R.string.alert_connection_description))
                        .setPositiveButton(getResources().getString(R.string.connect),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        startTask();
                                        dialog.cancel();
                                    }
                                })
                        .setNegativeButton(getResources().getString(R.string.cancel), new
                                DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                }).show();
            }
        });
    }

    private void alertScheduleExists() {
        new AlertDialog.Builder(context)
                .setTitle(getResources().getString(R.string.duplicate_title))
                .setMessage(getResources().getString(R.string.duplicate_description))
                .setNegativeButton(getResources().getString(R.string.cancel), new
                        DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        }).show();
    }

    private static class ComponentFetcher extends AsyncTask<Void, Void, Void> {

        private final WeakReference<ChooseScheduleFragment> weakReference;

        ComponentFetcher(ChooseScheduleFragment fragmentActivity) {
            weakReference = new WeakReference<>(fragmentActivity);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            ChooseScheduleFragment fragment = weakReference.get();
            if (fragment == null) {
                return;
            }

            fragment.spinner.setVisibility(View.VISIBLE);
            fragment.adapter = null;
        }

        @Override
        protected Void doInBackground(Void... params) {
            final ChooseScheduleFragment fragment = weakReference.get();
            if (fragment == null) {
                return null;
            }

            try {
                ScheduleItem[] items = null;
                switch (fragment.type) {
                    case CLASS:
                        items = WindesheimAPIController.getClasses();
                        break;
                    case TEACHER:
                        items = WindesheimAPIController.getTeachers();
                        break;
                    case SUBJECT:
                        items = WindesheimAPIController.getSubjects();
                        break;
                }

                ArrayList<ScheduleItem> scheduleItems = new ArrayList<>(Arrays.asList(items));
                fragment.adapter = new ChooseScheduleAdapter(fragment.context, scheduleItems) {
                    @Override
                    protected void onContentClick(String id, String name) {
                        try {
                            boolean hasSchedules = DatabaseController.getInstance().hasSchedules();

                            DatabaseController.getInstance().addSchedule(id, name, fragment.type);

                            SharedPreferences preferences = PreferenceManager
                                    .getDefaultSharedPreferences(ApplicationLoader.applicationContext);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.remove(Constants.PREFS_LAST_FETCH_TIME);
                            editor.apply();

                            Bundle bundle = new Bundle();
                            bundle.putString(Constants.TELEMETRY_PROPERTY_NAME, name);
                            TelemetryUtils.getInstance()
                                    .logEvent(Constants.TELEMETRY_KEY_SCHEDULE_ADDED, bundle);

                            ColorUtils.invalidateColorCache();

                            if (!hasSchedules) {
                                editor.putInt(Constants.PREFS_NOTIFICATIONS_TYPE,
                                        Constants.NOTIFICATION_TYPE_ALWAYS_ON);
                            }
                            editor.apply();

                            ApplicationLoader.postInitApplication();

                            if (!hasSchedules) {
                                Intent intent = new Intent(fragment.context,
                                        ScheduleActivity.class);
                                fragment.startActivity(intent);
                            }
                            fragment.getActivity().finish();
                        } catch (SQLiteConstraintException e) {
                            fragment.alertScheduleExists();
                        }
                    }
                };
            } catch (InterruptedException e) {
                //
            } catch (Exception e) {
                fragment.alertConnectionProblem();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            super.onPostExecute(param);

            ChooseScheduleFragment fragment = weakReference.get();
            if (fragment == null) {
                return;
            }

            fragment.spinner.setVisibility(View.GONE);
            fragment.recyclerView.setAdapter(fragment.adapter);
            EditText dataSearch = fragment.view.findViewById(R.id.filter_edittext);
            if (fragment.adapter != null && dataSearch.getText() != null &&
                    dataSearch.getText().toString().length() > 0) {
                fragment.adapter.filter(dataSearch.getText().toString());
            }
        }
    }
}
