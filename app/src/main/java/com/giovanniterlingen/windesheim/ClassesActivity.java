package com.giovanniterlingen.windesheim;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * A scheduler app for Windesheim students
 *
 * @author Giovanni Terlingen
 */
public class ClassesActivity extends AppCompatActivity {

    private final ArrayList<Integer> classesId = new ArrayList<>();
    private final ArrayList<String> classesList = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_classes);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        listView = (ListView) findViewById(R.id.classes_listview);
        EditText dataSearch = (EditText) findViewById(R.id.classes_filter);
        dataSearch.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                if (adapter != null) {
                    adapter.getFilter().filter(arg0);
                }
            }

            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                listView.setAdapter(adapter);
            }

            public void afterTextChanged(Editable arg0) {
            }
        });


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ClassesActivity.this);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("classId", classesId.get(classesList.indexOf(listView.getItemAtPosition(arg2))).toString());
                editor.putBoolean("notifications", true);
                editor.commit();

                ApplicationLoader.restartNotificationThread();

                Intent intent = new Intent(ClassesActivity.this, ScheduleActivity.class);
                startActivity(intent);
                finish();
            }

        });

        new ClassesFetcher().execute();
    }

    private void buildClassArray(JSONArray jsonArray) {
        JSONObject jsonObject;
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                jsonObject = jsonArray.getJSONObject(i);
                this.classesList.add(jsonObject.getString("name") + " - " + jsonObject.getString("longName"));
                this.classesId.add(jsonObject.getInt("id"));
            } catch (JSONException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    private void alertConnectionProblem() {
        ApplicationLoader.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(ClassesActivity.this)
                        .setTitle("Probleem met verbinden!")
                        .setMessage("De gegevens konden niet worden opgevraagd. Controleer je internetverbinding en probeer het opnieuw.")
                        .setIcon(R.drawable.ic_launcher)
                        .setNeutralButton("Verbinden",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                new ClassesFetcher().execute();
                                dialog.cancel();
                            }
                        }).show();
            }
        });
    }

    private class ClassesFetcher extends AsyncTask<Void, Void, Void> {

        private ProgressDialog progressDailog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDailog = new ProgressDialog(ClassesActivity.this);
            progressDailog.setMessage("Klassenlijst downloaden...");
            progressDailog.setIndeterminate(false);
            progressDailog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDailog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                String fetchedData = ScheduleHandler.getClassesFromServer();
                buildClassArray(new JSONObject(fetchedData).getJSONArray("elements"));
                adapter = new ArrayAdapter<>(ClassesActivity.this, R.layout.class_adapter_item, R.id.class_item, classesList);
            } catch (Exception e) {
                alertConnectionProblem();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            super.onPostExecute(param);
            progressDailog.dismiss();
        }
    }
}
