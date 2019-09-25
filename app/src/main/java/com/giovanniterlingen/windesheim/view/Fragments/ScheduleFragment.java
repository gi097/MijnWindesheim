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

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.Constants;
import com.giovanniterlingen.windesheim.R;
import com.giovanniterlingen.windesheim.controllers.DatabaseController;
import com.giovanniterlingen.windesheim.controllers.WindesheimAPIController;
import com.giovanniterlingen.windesheim.models.Lesson;
import com.giovanniterlingen.windesheim.utils.ColorUtils;
import com.giovanniterlingen.windesheim.view.Adapters.ScheduleAdapter;
import com.giovanniterlingen.windesheim.view.ScheduleActivity;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class ScheduleFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private Date date;
    private ScheduleAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyTextView;
    private ProgressBar spinner;
    private RecyclerView recyclerView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        date = (Date) getArguments().getSerializable("date");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.isMenuVisible() && this.isVisible()) {
            // Check padding for recyclerview (LTRB)
            boolean hasBottomBar = ((ScheduleActivity) getActivity()).showBottomBar();
            recyclerView.setPadding(
                    recyclerView.getPaddingLeft(),
                    recyclerView.getPaddingTop(),
                    recyclerView.getPaddingRight(),
                    hasBottomBar ? (int) getResources().getDimension(R.dimen.bottom_bar_height)
                            : 0);

            SharedPreferences preferences = PreferenceManager
                    .getDefaultSharedPreferences(ApplicationLoader.applicationContext);
            long lastFetchTime = preferences.getLong(Constants.PREFS_LAST_FETCH_TIME, 0);
            if (lastFetchTime == 0 || System.currentTimeMillis() - lastFetchTime >
                    TimeUnit.DAYS.toMillis(1)) {
                new ScheduleFetcher(this, true, true, false)
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                return;
            }
            new ScheduleFetcher(this, false, false,
                    false).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    public void updateAdapter() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    public void onVisible() {
        if (this.isMenuVisible() && this.isVisible()) {
            new ScheduleFetcher(this, false, false,
                    false).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        ViewGroup viewGroup = (ViewGroup) inflater.inflate(R.layout.fragment_schedule, container, false);
        swipeRefreshLayout = viewGroup.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeColors(ColorUtils.colors);
        emptyTextView = viewGroup.findViewById(R.id.schedule_not_found);
        spinner = viewGroup.findViewById(R.id.progress_bar);
        recyclerView = viewGroup.findViewById(R.id.schedule_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        Lesson[] lessons = DatabaseController.getInstance().getLessons(date);
        if (lessons.length > 0) {
            adapter = new ScheduleAdapter((ScheduleActivity) getActivity(), lessons, date);
            recyclerView.setAdapter(adapter);
        } else {
            emptyTextView.setVisibility(View.VISIBLE);
        }
        return viewGroup;
    }

    private void alertConnectionProblem() {
        if (!this.isVisible()) {
            return;
        }
        ApplicationLoader.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(getActivity())
                        .setTitle(getResources().getString(R.string.alert_connection_title))
                        .setMessage(getResources().getString(R.string.alert_connection_description))
                        .setPositiveButton(getResources().getString(R.string.connect),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        new ScheduleFetcher(ScheduleFragment.this,
                                                true, false, true)
                                                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                                        dialog.cancel();
                                    }
                                })
                        .setNegativeButton(getResources().getString(R.string.cancel),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                }).show();
            }
        });
    }

    @Override
    public void onRefresh() {
        new ScheduleFetcher(this, true, false, true)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void updateLayout() {
        if (adapter == null || adapter.getItemCount() == 0) {
            emptyTextView.setVisibility(View.VISIBLE);
        } else {
            emptyTextView.setVisibility(View.GONE);
        }
    }

    private static class ScheduleFetcher extends AsyncTask<Void, Void, Void> {

        private final boolean fetchData;
        private final boolean showSpinner;
        private final boolean showSwipeRefresh;
        private final WeakReference<ScheduleFragment> weakReference;
        private Lesson[] lessons;


        ScheduleFetcher(ScheduleFragment fragment, boolean fetchData, boolean showSpinner,
                        boolean showSwipeRefresh) {
            this.fetchData = fetchData;
            this.showSpinner = showSpinner;
            this.showSwipeRefresh = showSwipeRefresh;
            this.weakReference = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            ScheduleFragment fragment = weakReference.get();
            if (fragment == null) {
                return;
            }

            if (fragment.adapter == null) {
                if (showSpinner) {
                    fragment.emptyTextView.setVisibility(View.GONE);
                    fragment.spinner.setVisibility(View.VISIBLE);
                }
            } else if (fragment.adapter.getItemCount() == 0) {
                fragment.emptyTextView.setVisibility(View.VISIBLE);
            }
            if (showSwipeRefresh) {
                fragment.swipeRefreshLayout.setRefreshing(true);
            }
        }

        @Override
        protected Void doInBackground(Void... param) {
            ScheduleFragment fragment = weakReference.get();
            if (fragment == null) {
                return null;
            }

            if (fetchData) {
                try {
                    WindesheimAPIController.getAndSaveLessons(false);
                } catch (Exception e) {
                    fragment.alertConnectionProblem();
                }
            }
            lessons = DatabaseController.getInstance().getLessons(fragment.date);
            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            super.onPostExecute(param);

            if (fetchData) {
                ApplicationLoader.restartNotificationThread();
            }

            ScheduleFragment fragment = weakReference.get();
            if (fragment == null) {
                return;
            }

            if (fragment.adapter == null) {
                fragment.adapter = new ScheduleAdapter((ScheduleActivity) fragment.getActivity(),
                        lessons, fragment.date);
                fragment.recyclerView.setAdapter(fragment.adapter);
            } else {
                fragment.adapter.updateLessons(lessons);
            }
            if (fragment.adapter.getItemCount() == 0) {
                fragment.emptyTextView.setVisibility(View.VISIBLE);
            } else {
                fragment.emptyTextView.setVisibility(View.GONE);
            }
            if (showSpinner) {
                fragment.spinner.setVisibility(View.GONE);
            }
            if (showSwipeRefresh && fragment.swipeRefreshLayout.isRefreshing()) {
                fragment.swipeRefreshLayout.setRefreshing(false);
            }
        }
    }
}