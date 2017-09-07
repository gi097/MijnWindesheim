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
package com.giovanniterlingen.windesheim.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.giovanniterlingen.windesheim.R;
import com.giovanniterlingen.windesheim.handlers.WindesheimAPIHandler;
import com.giovanniterlingen.windesheim.objects.EC;
import com.giovanniterlingen.windesheim.objects.Result;
import com.giovanniterlingen.windesheim.ui.Adapters.ResultsAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class EducatorActivity extends AppCompatActivity {

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = PreferenceManager.getDefaultSharedPreferences(EducatorActivity.this);
        if (preferences.getString("username", "").length() == 0 ||
                preferences.getString("password", "").length() == 0) {
            Intent intent = new Intent(EducatorActivity.this, AuthenticationActivity.class);
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
        new ResultsFetcher().execute();
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

    public class ResultsFetcher extends AsyncTask<Void, Void, Void> {

        private ProgressBar progressBar;
        private Result[][] results;
        private EC[] ec;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar = findViewById(R.id.progress_bar);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... param) {
            try {
                String studentNumber = preferences.getString("username", "").split("@")[0];
                String response = WindesheimAPIHandler.getStudyInfo(studentNumber);
                JSONArray studyJson = new JSONArray(response);
                results = new Result[studyJson.length()][];
                ec = new EC[studyJson.length()];
                for (int i = 0; i < studyJson.length(); i++) {
                    JSONObject study = studyJson.getJSONObject(i).getJSONObject("WH_study");
                    String studyName = study.getString("description");
                    String isatCode = study.getString("isatcode");
                    JSONObject progress = studyJson.getJSONObject(i).getJSONObject("WH_studyProgress");
                    int maxECs = progress.getInt("ectsTeBehalen");
                    int currentECs = progress.getInt("ectsBehaald");
                    ec[i] = new EC(maxECs, currentECs, studyName);
                    String response2 = WindesheimAPIHandler.getResults(studentNumber, isatCode);
                    JSONArray resultsJSON = new JSONArray(response2);
                    results[i] = WindesheimAPIHandler.getResultArray(resultsJSON);
                }
            } catch (Exception e) {
                Intent intent = new Intent(EducatorActivity.this, AuthenticationActivity.class);
                intent.putExtra("educator", true);
                startActivity(intent);
                finish();
            }
            return null;
        }


        @Override
        protected void onPostExecute(Void param) {
            super.onPostExecute(param);
            progressBar.setVisibility(View.GONE);
            if (results != null && results.length > 0) {
                ResultsAdapter adapter = new ResultsAdapter(EducatorActivity.this, results, ec);
                RecyclerView recyclerView = findViewById(R.id.results_recyclerview);
                recyclerView.setLayoutManager(new LinearLayoutManager(EducatorActivity.this));
                recyclerView.setAdapter(adapter);
            }
        }
    }
}
