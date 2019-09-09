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
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.R;
import com.giovanniterlingen.windesheim.controllers.CalendarController;
import com.giovanniterlingen.windesheim.controllers.ColorController;
import com.giovanniterlingen.windesheim.controllers.DatabaseController;
import com.giovanniterlingen.windesheim.controllers.WindesheimAPIController;
import com.giovanniterlingen.windesheim.models.Lesson;
import com.giovanniterlingen.windesheim.view.Adapters.ScheduleAdapter;
import com.giovanniterlingen.windesheim.view.ScheduleActivity;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class ScheduleFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private final int[] dateStrings = new int[]{
            R.string.january,
            R.string.february,
            R.string.march,
            R.string.april,
            R.string.may,
            R.string.june,
            R.string.july,
            R.string.august,
            R.string.september,
            R.string.october,
            R.string.november,
            R.string.december
    };
    private Calendar calendar;
    private Date date;
    private SimpleDateFormat dayDateFormat;

    private ScheduleAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyTextView;
    private ProgressBar spinner;
    private RecyclerView recyclerView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        calendar = CalendarController.getCalendar();
        date = (Date) getArguments().getSerializable("date");
        dayDateFormat = CalendarController.getDayDateFormat();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.isMenuVisible() && this.isVisible()) {
            if (recyclerView != null && recyclerView.getAdapter() == null) {
                new ScheduleFetcher(this, false, true, false)
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                return;
            }
            new ScheduleFetcher(this, false, false, false).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private void updateToolbar() {
        if (this.isMenuVisible() && this.isVisible()) {
            calendar.setTime(date);
            int month = calendar.get(GregorianCalendar.MONTH);
            int year = calendar.get(GregorianCalendar.YEAR);
            String monthString = getResources().getString(dateStrings[month]);
            ActionBar toolbar = ((ScheduleActivity) getActivity()).getSupportActionBar();
            if (toolbar != null) {
                String title = dayDateFormat.format(date) + " " + monthString + " " + year;
                if (!title.contentEquals(toolbar.getTitle())) {
                    toolbar.setTitle(title);
                }
            }
        }
    }

    public void updateAdapter() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
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
        swipeRefreshLayout.setColorSchemeColors(ColorController.colors);
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

            fragment.updateToolbar();
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
                    WindesheimAPIController.getAndSaveLessons(fragment.date, false);
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