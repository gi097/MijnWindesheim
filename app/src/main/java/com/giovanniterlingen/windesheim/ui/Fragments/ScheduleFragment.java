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

import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.R;
import com.giovanniterlingen.windesheim.handlers.ScheduleHandler;
import com.giovanniterlingen.windesheim.ui.Adapters.ScheduleAdapter;
import com.giovanniterlingen.windesheim.ui.ScheduleActivity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

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
    private Date date;
    private ScheduleAdapter adapter;
    private DateFormat simpleDateFormat;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyTextView;
    private ProgressBar spinner;
    private RecyclerView recyclerView;
    private SimpleDateFormat dayFormat;
    private Calendar calendar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        date = (Date) getArguments().getSerializable("date");
        simpleDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);
        dayFormat = new SimpleDateFormat("dd", Locale.US);
        calendar = Calendar.getInstance();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isMenuVisible()) {
            if (recyclerView != null && recyclerView.getAdapter() == null) {
                new ScheduleFetcher(false, true, false).execute();
            } else {
                new ScheduleFetcher(false, false, false).execute();
            }
        }
    }

    private void updateToolbar() {
        if (isMenuVisible() && dayFormat != null && calendar != null) {
            calendar.setTime(date);
            int month = calendar.get(Calendar.MONTH);
            String monthString = getResources().getString(dateStrings[month]);
            ActionBar toolbar = ((ScheduleActivity) getActivity()).getSupportActionBar();
            if (toolbar != null) {
                String title = dayFormat.format(date) + " " + monthString;
                if (!title.equals(toolbar.getTitle())) {
                    toolbar.setTitle(title);
                    toolbar.setDisplayHomeAsUpEnabled(false);
                    toolbar.setDisplayHomeAsUpEnabled(true);
                }
            }
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        if (isVisible()) {
            if (!ApplicationLoader.scheduleDatabase.isFetched(date)) {
                new ScheduleFetcher(true, true, false).execute();
            } else {
                new ScheduleFetcher(false, false, false).execute();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        ViewGroup viewGroup = (ViewGroup) inflater.inflate(R.layout.fragment_schedule, container, false);
        swipeRefreshLayout = viewGroup.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorAccent, R.color.colorPrimaryText, R.color.colorPrimary);
        emptyTextView = viewGroup.findViewById(R.id.schedule_not_found);
        spinner = viewGroup.findViewById(R.id.progress_bar);
        recyclerView = viewGroup.findViewById(R.id.schedule_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        Cursor scheduleDay = ApplicationLoader.scheduleDatabase.getLessons(simpleDateFormat.format(date));
        if (scheduleDay.getCount() > 0) {
            adapter = new ScheduleAdapter(getActivity(), scheduleDay, simpleDateFormat.format(date), date);
            recyclerView.setAdapter(adapter);
        } else {
            emptyTextView.setVisibility(View.VISIBLE);
        }
        return viewGroup;
    }

    private void alertConnectionProblem() {
        if (!isMenuVisible()) {
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
                                        new ScheduleFetcher(true, false, true).execute();
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
        new ScheduleFetcher(true, false, true).execute();
    }

    public void updateLayout() {
        if (emptyTextView != null) {
            if (adapter == null || adapter.getItemCount() == 0) {
                emptyTextView.setVisibility(View.VISIBLE);
            } else {
                emptyTextView.setVisibility(View.GONE);
            }
        }
    }

    public class ScheduleFetcher extends AsyncTask<Void, Void, Void> {

        private final boolean fetchData;
        private final boolean showSpinner;
        private final boolean showSwipeRefresh;
        private Cursor scheduleDay;

        ScheduleFetcher(boolean fetchData, boolean showSpinner, boolean showSwipeRefresh) {
            this.fetchData = fetchData;
            this.showSpinner = showSpinner;
            this.showSwipeRefresh = showSwipeRefresh;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            updateToolbar();
            if (adapter == null) {
                if (showSpinner) {
                    emptyTextView.setVisibility(View.GONE);
                    spinner.setVisibility(View.VISIBLE);
                }
            } else if (adapter.getItemCount() == 0) {
                emptyTextView.setVisibility(View.VISIBLE);
            }
            if (showSwipeRefresh) {
                swipeRefreshLayout.setRefreshing(true);
            }
        }

        @Override
        protected Void doInBackground(Void... param) {
            if (fetchData) {
                try {
                    ScheduleHandler.getAndSaveAllSchedules(date);
                } catch (Exception e) {
                    alertConnectionProblem();
                }
            }
            return null;
        }


        @Override
        protected void onPostExecute(Void param) {
            super.onPostExecute(param);
            scheduleDay = ApplicationLoader.scheduleDatabase.getLessons(simpleDateFormat.format(date));
            if (adapter == null) {
                adapter = new ScheduleAdapter(getActivity(), scheduleDay, simpleDateFormat.format(date), date);
                recyclerView.setAdapter(adapter);
            } else {
                adapter.changeCursor(scheduleDay);
            }
            if (adapter.getItemCount() == 0) {
                emptyTextView.setVisibility(View.VISIBLE);
            } else {
                emptyTextView.setVisibility(View.GONE);
            }
            if (showSpinner) {
                spinner.setVisibility(View.GONE);
            }
            if (showSwipeRefresh && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
        }
    }
}