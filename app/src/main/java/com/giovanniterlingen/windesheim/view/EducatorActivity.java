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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.giovanniterlingen.windesheim.Constants;
import com.giovanniterlingen.windesheim.R;
import com.giovanniterlingen.windesheim.controllers.WindesheimAPIController;
import com.giovanniterlingen.windesheim.models.EC;
import com.giovanniterlingen.windesheim.models.PropaedeuticEC;
import com.giovanniterlingen.windesheim.models.Result;
import com.giovanniterlingen.windesheim.utils.TelemetryUtils;
import com.giovanniterlingen.windesheim.view.Adapters.ResultsAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class EducatorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(EducatorActivity.this);
        if (preferences.getString(Constants.PREFS_USERNAME, "").length() == 0 ||
                preferences.getString(Constants.PREFS_PASSWORD, "").length() == 0) {
            Intent intent = new Intent(EducatorActivity.this,
                    AuthenticationActivity.class);
            intent.putExtra("educator", true);
            startActivity(intent);
            finish();
            return;
        }
        setContentView(R.layout.activity_progress);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        new ResultsFetcher(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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

    @Override
    protected void onResume() {
        super.onResume();
        TelemetryUtils.getInstance().setCurrentScreen(this, "EducatorActivity");
    }

    @Override
    protected void onPause() {
        TelemetryUtils.getInstance().setCurrentScreen(this, null);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    static class ResultsFetcher extends AsyncTask<Void, Void, Void> {

        final WeakReference<EducatorActivity> weakReference;
        private Result[][] results;
        private PropaedeuticEC[] propaedeuticEC;
        private EC[] ec;

        ResultsFetcher(EducatorActivity educatorActivity) {
            weakReference = new WeakReference<>(educatorActivity);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            EducatorActivity activity = weakReference.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }
            ProgressBar progressBar = activity.findViewById(R.id.progress_bar);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... param) {
            EducatorActivity activity = weakReference.get();
            if (activity == null || activity.isFinishing()) {
                return null;
            }
            try {
                SharedPreferences preferences = PreferenceManager
                        .getDefaultSharedPreferences(activity);

                String studentNumber = preferences.getString(Constants.PREFS_USERNAME, "").split("@")[0];
                String response = WindesheimAPIController.getStudyInfo(studentNumber);

                JSONArray studyJson = new JSONArray(response);
                results = new Result[studyJson.length()][];
                propaedeuticEC = new PropaedeuticEC[studyJson.length()];
                ec = new EC[studyJson.length()];

                for (int i = 0; i < studyJson.length(); i++) {
                    JSONObject study = studyJson.getJSONObject(i).getJSONObject("WH_study");
                    String studyName = study.getString("description");
                    String isatCode = study.getString("isatcode");
                    JSONObject progress = studyJson.getJSONObject(i).getJSONObject("WH_studyProgress");
                    if (progress.has("ectsTeBehalenPropedeuse") &&
                            progress.has("ectsBehaaldPropedeuse")) {
                        int maxPropaedeuticECs = progress.getInt("ectsTeBehalenPropedeuse");
                        int currentPropaedeuticECs = progress.getInt("ectsBehaaldPropedeuse");
                        propaedeuticEC[i] = new PropaedeuticEC(maxPropaedeuticECs, currentPropaedeuticECs);
                    }
                    int maxECs = progress.getInt("ectsTeBehalen");
                    int currentECs = progress.getInt("ectsBehaald");
                    ec[i] = new EC(maxECs, currentECs, studyName);
                    String response2 = WindesheimAPIController.getResults(studentNumber, isatCode);
                    JSONArray resultsJSON = new JSONArray(response2);
                    results[i] = WindesheimAPIController.getResultArray(resultsJSON);
                }
            } catch (Exception e) {
                Intent intent = new Intent(activity, AuthenticationActivity.class);
                intent.putExtra("educator", true);
                activity.startActivity(intent);
                activity.finish();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            super.onPostExecute(param);

            EducatorActivity activity = weakReference.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }
            if (results == null || results.length == 0) {
                return;
            }

            ProgressBar progressBar = activity.findViewById(R.id.progress_bar);
            progressBar.setVisibility(View.GONE);

            ResultsAdapter adapter = new ResultsAdapter(activity, results, propaedeuticEC, ec);
            RecyclerView recyclerView = activity.findViewById(R.id.results_recyclerview);
            recyclerView.setLayoutManager(new LinearLayoutManager(activity));
            recyclerView.setAdapter(adapter);
        }
    }
}
