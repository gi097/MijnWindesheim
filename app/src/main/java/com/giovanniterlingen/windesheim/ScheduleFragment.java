package com.giovanniterlingen.windesheim;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * A schedule app for Windesheim students
 *
 * @author Giovanni Terlingen
 */
public class ScheduleFragment extends ListFragment implements SwipeRefreshLayout.OnRefreshListener {

    private static String componentId;
    private static int type;
    private Date date;
    private ScheduleAdapter adapter;
    private DateFormat simpleDateFormat;
    private SwipeRefreshLayout swipeRefreshLayout;
    private long onLongClickId;
    private boolean isShowing = false;
    private TextView emptyTextView;
    private ProgressBar spinner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        componentId = getArguments().getString("componentId");
        type = getArguments().getInt("type");
        date = (Date) getArguments().getSerializable("date");
        simpleDateFormat = new SimpleDateFormat("yyyyMMdd");

        Cursor scheduleDay = ApplicationLoader.scheduleDatabase.getLessons(simpleDateFormat.format(date), componentId);
        if (scheduleDay != null && scheduleDay.getCount() > 0) {
            adapter = new ScheduleAdapter(getActivity(), scheduleDay);
            setListAdapter(adapter);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedState) {
        super.onActivityCreated(savedState);
        registerForContextMenu(getListView());
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                onLongClickId = arg3;
                (getActivity()).openContextMenu(getListView());
                return true;
            }
        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, 0, 0, getResources().getString(R.string.hide_lesson));
        menu.add(0, 1, 1, getResources().getString(R.string.save_lesson));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (getUserVisibleHint()) {
            if (item.getItemId() == 0) {
                showPromptDialog();
                return true;
            }
            if (item.getItemId() == 1) {
                Cursor cursor = ApplicationLoader.scheduleDatabase.getSingleLesson(onLongClickId);
                if (cursor.moveToFirst()) {
                    String[] startTimeStrings = cursor.getString(0).split(":");
                    String[] endTimeStrings = cursor.getString(1).split(":");

                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(date);

                    calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(startTimeStrings[0]));
                    calendar.set(Calendar.MINUTE, Integer.parseInt(startTimeStrings[1]));

                    Intent intent = new Intent(Intent.ACTION_EDIT);
                    intent.setType("vnd.android.cursor.item/event");
                    intent.putExtra("beginTime", calendar.getTimeInMillis());
                    intent.putExtra("allDay", false);

                    calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(endTimeStrings[0]));
                    calendar.set(Calendar.MINUTE, Integer.parseInt(endTimeStrings[1]));

                    intent.putExtra("endTime", calendar.getTimeInMillis());
                    intent.putExtra("title", cursor.getString(2));
                    intent.putExtra("eventLocation", cursor.getString(3));

                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        ScheduleActivity.showSnackbar(getResources().getString(R.string.no_calendar_found));
                    }
                }
                cursor.close();
                return true;
            }
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getUserVisibleHint()) {
            if (getListAdapter() == null) {
                new ScheduleFetcher(false, true).execute();
            } else {
                new ScheduleFetcher(false, false).execute();
            }
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd");
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            int month = calendar.get(Calendar.MONTH);
            String monthString = null;
            switch (month) {
                case 0:
                    monthString = getResources().getString(R.string.january);
                    break;
                case 1:
                    monthString = getResources().getString(R.string.february);
                    break;
                case 2:
                    monthString = getResources().getString(R.string.march);
                    break;
                case 3:
                    monthString = getResources().getString(R.string.april);
                    break;
                case 4:
                    monthString = getResources().getString(R.string.may);
                    break;
                case 5:
                    monthString = getResources().getString(R.string.june);
                    break;
                case 6:
                    monthString = getResources().getString(R.string.july);
                    break;
                case 7:
                    monthString = getResources().getString(R.string.august);
                    break;
                case 8:
                    monthString = getResources().getString(R.string.september);
                    break;
                case 9:
                    monthString = getResources().getString(R.string.october);
                    break;
                case 10:
                    monthString = getResources().getString(R.string.november);
                    break;
                case 11:
                    monthString = getResources().getString(R.string.december);
            }
            ActionBar toolbar = ((ScheduleActivity) getActivity()).getSupportActionBar();
            if (toolbar != null) {
                toolbar.setTitle(simpleDateFormat.format(date) + " " + monthString);
                // ugly workaround to fix toolbar title truncation
                toolbar.setDisplayHomeAsUpEnabled(true);
                toolbar.setDisplayHomeAsUpEnabled(false);
            }
            if (!ApplicationLoader.scheduleDatabase.containsWeek(date)) {
                new ScheduleFetcher(true, true).execute();
            } else {
                new ScheduleFetcher(false, true).execute();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        ViewGroup viewGroup = (ViewGroup) inflater.inflate(R.layout.fragment_schedule, container, false);
        swipeRefreshLayout = (SwipeRefreshLayout) viewGroup.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorAccent, R.color.colorPrimaryText, R.color.colorPrimary);
        emptyTextView = (TextView) viewGroup.findViewById(R.id.schedule_not_found);
        spinner = (ProgressBar) viewGroup.findViewById(R.id.progress_bar);
        if (getListAdapter() != null) {
            emptyTextView.setVisibility(View.GONE);
        }
        return viewGroup;
    }

    private void alertConnectionProblem() {
        if (isShowing) {
            return;
        }
        isShowing = true;
        ApplicationLoader.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(getActivity())
                        .setTitle(getResources().getString(R.string.alert_connection_title))
                        .setMessage(getResources().getString(R.string.alert_connection_description))
                        .setIcon(R.drawable.ic_launcher)
                        .setPositiveButton(getResources().getString(R.string.connect),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        new ScheduleFetcher(true, false).execute();
                                        dialog.cancel();
                                        isShowing = false;
                                    }
                                })
                        .setNegativeButton(getResources().getString(R.string.cancel),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                        isShowing = false;
                                    }
                                }).show();
            }
        });
    }

    private void showPromptDialog() {
        if (isShowing) {
            return;
        }
        isShowing = true;
        ApplicationLoader.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(getActivity())
                        .setTitle(getResources().getString(R.string.confirmation))
                        .setMessage(getResources().getString(R.string.deletion_description))
                        .setIcon(R.drawable.ic_launcher)
                        .setPositiveButton(getResources().getString(R.string.hide),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        ApplicationLoader.scheduleDatabase.clearLessons(onLongClickId);
                                        new ScheduleFetcher(false, false).execute();
                                        Snackbar snackbar = Snackbar.make(getActivity().findViewById(R.id.schedule_coordinator_layout), getResources().getString(R.string.lesson_hidden), Snackbar.LENGTH_SHORT);
                                        snackbar.show();
                                        ApplicationLoader.restartNotificationThread();
                                        dialog.cancel();
                                        isShowing = false;
                                    }
                                })
                        .setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                isShowing = false;
                            }
                        }).show();
            }
        });
    }

    @Override
    public void onRefresh() {
        new ScheduleFetcher(true, false).execute();
    }

    public class ScheduleFetcher extends AsyncTask<Void, Void, Void> {

        private final boolean fetchData;
        private final boolean showSpinner;
        private Cursor scheduleDay;

        public ScheduleFetcher(boolean fetchData, boolean showSpinner) {
            this.fetchData = fetchData;
            this.showSpinner = showSpinner;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (swipeRefreshLayout != null && adapter == null) {
                if (showSpinner) {
                    emptyTextView.setVisibility(View.GONE);
                    spinner.setVisibility(View.VISIBLE);
                } else {
                    swipeRefreshLayout.setRefreshing(true);
                }
            }
        }

        @Override
        protected Void doInBackground(Void... param) {
            if (fetchData) {
                try {
                    ScheduleHandler.saveSchedule(ScheduleHandler.getScheduleFromServer(componentId, date, type), date, componentId);
                } catch (Exception e) {
                    alertConnectionProblem();
                }
            }
            scheduleDay = ApplicationLoader.scheduleDatabase.getLessons(simpleDateFormat.format(date), componentId);
            return null;
        }


        @Override
        protected void onPostExecute(Void param) {
            super.onPostExecute(param);
            if (adapter == null) {
                adapter = new ScheduleAdapter(getActivity(), scheduleDay);
            } else {
                adapter.changeCursor(scheduleDay);
            }
            if (adapter.isEmpty()) {
                emptyTextView.setVisibility(View.VISIBLE);
            } else {
                emptyTextView.setVisibility(View.GONE);
            }
            if (getListAdapter() == null) {
                setListAdapter(adapter);
            }
            if (showSpinner) {
                spinner.setVisibility(View.GONE);
            } else {
                if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        }
    }
}