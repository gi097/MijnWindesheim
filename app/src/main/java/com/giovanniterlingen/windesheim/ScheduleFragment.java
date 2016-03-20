package com.giovanniterlingen.windesheim;

import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * A scheduler app for Windesheim students
 *
 * @author Giovanni Terlingen
 */
public class ScheduleFragment extends ListFragment implements SwipeRefreshLayout.OnRefreshListener {

    private static String componentId;
    private static int type;
    private Date date;
    private Context context;
    private ScheduleAdapter adapter;
    private DateFormat simpleDateFormat;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ViewGroup viewGroup;
    private long onLongClickId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        componentId = getArguments().getString("componentId");
        type = getArguments().getInt("type");
        date = (Date) getArguments().getSerializable("date");
        context = getActivity();
        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

        Cursor scheduleDay = ApplicationLoader.scheduleDatabase.getLessons(simpleDateFormat.format(date), componentId);
        if (scheduleDay != null && scheduleDay.getCount() > 0) {
            adapter = new ScheduleAdapter(context, scheduleDay, 0);
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
        menu.add(0, 0, 0, "Verberg deze les");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (getUserVisibleHint()) {
            if (item.getItemId() == 0) {
                showPromptDialog();
                return true;
            }
        }
        return false;
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
                    monthString = "januari";
                    break;
                case 1:
                    monthString = "februari";
                    break;
                case 2:
                    monthString = "maart";
                    break;
                case 3:
                    monthString = "april";
                    break;
                case 4:
                    monthString = "mei";
                    break;
                case 5:
                    monthString = "juni";
                    break;
                case 6:
                    monthString = "juli";
                    break;
                case 7:
                    monthString = "augustus";
                    break;
                case 8:
                    monthString = "september";
                    break;
                case 9:
                    monthString = "oktober";
                    break;
                case 10:
                    monthString = "november";
                    break;
                case 11:
                    monthString = "december";
            }
            ((ScheduleActivity) context).getSupportActionBar().setTitle(simpleDateFormat.format(date) + " " + monthString);
            if (getListAdapter() == null) {
                new ScheduleFetcher(true).execute();
            } else {
                new ScheduleFetcher(false).execute();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        viewGroup = (ViewGroup) inflater.inflate(R.layout.fragment_schedule, container, false);
        swipeRefreshLayout = (SwipeRefreshLayout) viewGroup.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorAccent, R.color.colorPrimaryText, R.color.colorPrimary);
        if (getListAdapter() == null) {
            TextView emptyTextView = (TextView) viewGroup.findViewById(R.id.schedule_not_found);
            emptyTextView.setVisibility(View.VISIBLE);
        }
        return viewGroup;
    }

    private void alertConnectionProblem() {
        ApplicationLoader.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(context)
                        .setTitle("Probleem met verbinden!")
                        .setMessage("De gegevens konden niet worden opgevraagd. Controleer je internetverbinding en probeer het opnieuw.")
                        .setIcon(R.drawable.ic_launcher)
                        .setPositiveButton("Verbinden",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        new ScheduleFetcher(true).execute();
                                        dialog.cancel();
                                    }
                                })
                        .setNegativeButton("Annuleren", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        }).show();
            }
        });
    }

    private void showPromptDialog() {
        ApplicationLoader.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(context)
                        .setTitle("Weet je het zeker?")
                        .setMessage("Alle lessen van dit vak met deze klas of docent worden verborgen, deze kunnen hersteld worden via het menu.")
                        .setIcon(R.drawable.ic_launcher)
                        .setPositiveButton("Verbergen",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        ApplicationLoader.scheduleDatabase.clearLessons(onLongClickId);
                                        new ScheduleFetcher(false).execute();
                                        Snackbar snackbar = Snackbar.make(getActivity().findViewById(R.id.schedule_coordinator_layout), "Lessen zijn verborgen", Snackbar.LENGTH_SHORT);
                                        snackbar.show();
                                        ApplicationLoader.restartNotificationThread();
                                    }
                                })
                        .setNegativeButton("Annuleren", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        }).show();
            }
        });
    }

    @Override
    public void onRefresh() {
        new ScheduleFetcher(true).execute();
    }

    public class ScheduleFetcher extends AsyncTask<Void, Void, Void> {

        private boolean fetchData;
        private Cursor scheduleDay;

        public ScheduleFetcher(boolean fetchData) {
            this.fetchData = fetchData;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (swipeRefreshLayout != null && adapter == null) {
                swipeRefreshLayout.setRefreshing(true);
            }
        }

        @Override
        protected Void doInBackground(Void... param) {
            if (fetchData) {
                try {
                    ScheduleHandler.saveSchedule(ScheduleHandler.getScheduleFromServer(componentId, date, type), date, componentId, type);
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
            TextView emptyTextView = (TextView) viewGroup.findViewById(R.id.schedule_not_found);
            if (adapter == null) {
                adapter = new ScheduleAdapter(context, scheduleDay, 0);
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
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
        }
    }
}