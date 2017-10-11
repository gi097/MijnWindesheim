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
package com.giovanniterlingen.windesheim.view.Fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteConstraintException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.R;
import com.giovanniterlingen.windesheim.controllers.ColorController;
import com.giovanniterlingen.windesheim.controllers.DatabaseController;
import com.giovanniterlingen.windesheim.controllers.NotificationController;
import com.giovanniterlingen.windesheim.controllers.WebUntisController;
import com.giovanniterlingen.windesheim.models.ScheduleItem;
import com.giovanniterlingen.windesheim.view.Adapters.ChooseScheduleAdapter;
import com.giovanniterlingen.windesheim.view.ScheduleActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class ChooseScheduleFragment extends Fragment {

    private ChooseScheduleAdapter adapter;
    private RecyclerView recyclerView;
    private int type;
    private Context context;
    private ProgressBar spinner;
    private View view;
    private ComponentFetcher componentFetcher;
    private boolean isViewShown = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        int position = getArguments().getInt("position");
        type = position + 1;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (getView() != null) {
            isViewShown = true;
            startTask();
        } else {
            isViewShown = false;
        }
    }

    private void startTask() {
        if (componentFetcher != null) {
            if (componentFetcher.getStatus() == AsyncTask.Status.RUNNING
                    || componentFetcher.getStatus() == AsyncTask.Status.PENDING) {
                componentFetcher.cancel(true);
            }
        }
        if (isMenuVisible()) {
            (componentFetcher = new ComponentFetcher()).execute();
        } else {
            if (adapter != null) {
                adapter = null;
            }
            if (recyclerView != null) {
                recyclerView.setAdapter(null);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_choose_schedule, container, false);
        TextView chooseTextview = view.findViewById(R.id.choose_textview);
        TextView descriptionTextview = view.findViewById(R.id.description_textview);
        EditText dataSearch = view.findViewById(R.id.filter_edittext);
        if (type == 1) {
            chooseTextview.setText(getResources().getString(R.string.choose_class));
            descriptionTextview.setText(getResources().getString(R.string
                    .choose_class_description));
            dataSearch.setHint(getResources().getString(R.string.choose_class_hint));
        }
        if (type == 2) {
            chooseTextview.setText(getResources().getString(R.string.choose_teacher));
            descriptionTextview.setText(getResources().getString(R.string
                    .choose_teacher_description));
            dataSearch.setHint(getResources().getString(R.string.choose_teacher_hint));
        }
        if (type == 3) {
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

    private synchronized ArrayList<ScheduleItem> buildClassArray(JSONArray jsonArray) {
        ArrayList<ScheduleItem> scheduleItems = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                if (Thread.interrupted()) {
                    return null;
                }
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                scheduleItems.add(new ScheduleItem(jsonObject.getInt("id"),
                        jsonObject.getString("name") + " - " + jsonObject.getString("longName")));
            } catch (JSONException e) {
                alertConnectionProblem();
                return null;
            }
        }
        return scheduleItems;
    }

    private void alertConnectionProblem() {
        if (!getUserVisibleHint()) {
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(context)
                        .setTitle(getResources().getString(R.string.alert_connection_title))
                        .setMessage(getResources().getString(R.string.alert_connection_description))
                        .setPositiveButton(getResources().getString(R.string.connect),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        if (componentFetcher != null) {
                                            if (componentFetcher.getStatus() == AsyncTask.Status.RUNNING
                                                    || componentFetcher.getStatus() == AsyncTask.Status.PENDING) {
                                                componentFetcher.cancel(true);
                                            }
                                        }
                                        (componentFetcher = new ComponentFetcher()).execute();
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

    private class ComponentFetcher extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            spinner.setVisibility(View.VISIBLE);
            adapter = null;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                ArrayList<ScheduleItem> scheduleItems = buildClassArray(new WebUntisController()
                        .getListFromServer(type).getJSONArray("elements"));
                adapter = new ChooseScheduleAdapter(context, scheduleItems) {
                    @Override
                    protected void onContentClick(int id, String name) {
                        try {
                            boolean hasSchedules = DatabaseController.getInstance().hasSchedules();

                            DatabaseController.getInstance().addSchedule(id, name, type);
                            DatabaseController.getInstance().clearFetched();

                            ColorController.getInstance().invalidateColorCache();

                            SharedPreferences preferences = PreferenceManager
                                    .getDefaultSharedPreferences(context);
                            SharedPreferences.Editor editor = preferences.edit();
                            if (!hasSchedules) {
                                editor.putInt("notifications_type",
                                        NotificationController.NOTIFICATION_ALWAYS_ON);
                            }
                            editor.apply();

                            ApplicationLoader.restartNotificationThread();
                            ApplicationLoader.startServices();

                            if (!hasSchedules) {
                                Intent intent = new Intent(context, ScheduleActivity.class);
                                startActivity(intent);
                            }
                            getActivity().finish();
                        } catch (SQLiteConstraintException e) {
                            alertScheduleExists();
                        }
                    }
                };
            } catch (InterruptedException e) {
                //
            } catch (Exception e) {
                alertConnectionProblem();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            super.onPostExecute(param);
            spinner.setVisibility(View.GONE);
            recyclerView.setAdapter(adapter);
            EditText dataSearch = view.findViewById(R.id.filter_edittext);
            if (adapter != null && dataSearch.getText() != null && dataSearch.getText().toString().length() > 0) {
                adapter.filter(dataSearch.getText().toString());
            }
        }
    }
}
