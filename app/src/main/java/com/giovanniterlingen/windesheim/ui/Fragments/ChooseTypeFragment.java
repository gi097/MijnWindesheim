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
package com.giovanniterlingen.windesheim.ui.Fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.giovanniterlingen.windesheim.handlers.ScheduleHandler;
import com.giovanniterlingen.windesheim.objects.Component;
import com.giovanniterlingen.windesheim.ui.Adapters.ComponentAdapter;
import com.giovanniterlingen.windesheim.ui.ScheduleActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class ChooseTypeFragment extends Fragment {

    private ArrayList<Component> componentList;
    private ComponentAdapter adapter;
    private RecyclerView recyclerView;
    private int type;
    private Context context;
    private ProgressBar spinner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        int position = getArguments().getInt("position");
        type = position + 1;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter == null) {
            new ComponentFetcher().execute();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_choose_type, container, false);
        TextView chooseTextview = (TextView) view.findViewById(R.id.choose_textview);
        TextView descriptionTextview = (TextView) view.findViewById(R.id.description_textview);
        EditText dataSearch = (EditText) view.findViewById(R.id.filter_edittext);
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
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        spinner = (ProgressBar) view.findViewById(R.id.progress_bar);
        dataSearch.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                if (adapter != null) {
                    adapter.filter(arg0.toString());
                }
            }

            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                if (adapter == null) {
                    new ComponentFetcher().execute();
                }
            }

            public void afterTextChanged(Editable arg0) {
            }
        });

        return view;
    }


    private void buildClassArray(JSONArray jsonArray) {
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                this.componentList.add(new Component(jsonObject.getInt("id"),
                        jsonObject.getString("name") + " - " + jsonObject.getString("longName")));
            } catch (JSONException e) {
                alertConnectionProblem();
                break;
            }
        }
    }

    private void alertConnectionProblem() {
        if (!getUserVisibleHint()) {
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
                                        new ComponentFetcher().execute();
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

    private class ComponentFetcher extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            spinner.setVisibility(View.VISIBLE);
            componentList = new ArrayList<>();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                buildClassArray(new JSONObject(ScheduleHandler.getListFromServer(type))
                        .getJSONArray("elements"));
                adapter = new ComponentAdapter(context, componentList) {
                    @Override
                    protected void onContentClick(int id) {
                        SharedPreferences preferences = PreferenceManager
                                .getDefaultSharedPreferences(context);
                        if (preferences.getString("componentId", "").length() != 0 &&
                                preferences.getInt("type", 0) != 0) {
                            ApplicationLoader.scheduleDatabase.clearFetched();
                        }
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("componentId", Integer.toString(id));
                        editor.putInt("notifications_type", 5);
                        editor.putInt("type", type);
                        if (preferences.getLong("checkTime", 0) > 0) {
                            editor.remove("checkTime");
                        }
                        editor.apply();

                        ApplicationLoader.restartDailyScheduleFetcher();
                        ApplicationLoader.restartNotificationThread();

                        Intent intent = new Intent(context, ScheduleActivity.class);
                        startActivity(intent);
                        getActivity().finish();
                    }
                };
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
        }
    }
}
