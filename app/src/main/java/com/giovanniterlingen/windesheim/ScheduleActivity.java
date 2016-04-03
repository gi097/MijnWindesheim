package com.giovanniterlingen.windesheim;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * A schedule app for Windesheim students
 *
 * @author Giovanni Terlingen
 */
public class ScheduleActivity extends AppCompatActivity {

    private static String componentId;
    private static int type;
    private SharedPreferences sharedPreferences;
    private Calendar onPauseCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ScheduleActivity.this);
        // Fix previous versions
        String classId = sharedPreferences.getString("classId", "");
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (classId.length() > 0) {
            editor.putString("componentId", classId);
            editor.putInt("type", 1);
            editor.remove("classId");
            editor.commit();
        }
        editor.remove("notifications");
        componentId = sharedPreferences.getString("componentId", "");
        type = sharedPreferences.getInt("type", 0);
        if (componentId.length() == 0 || type == 0) {
            Intent intent = new Intent(ScheduleActivity.this, ChooseTypeActivity.class);
            startActivity(intent);
            super.onCreate(savedInstanceState);
            finish();
            return;
        }
        if (sharedPreferences.getInt("notifications_type", 0) == 0 || sharedPreferences.getInt("notifications_type", 0) == 5) {
            editor.putInt("notifications_type", 6);
            editor.commit();
        }
        if (ApplicationLoader.notificationThread != null && !ApplicationLoader.notificationThread.isRunning()) {
            ApplicationLoader.restartNotificationThread();
        }
        ApplicationLoader.postInitApplication();
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_schedule);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setViewPager();
    }

    private void setViewPager() {
        Calendar calendar = Calendar.getInstance();
        ViewPager mPager = (ViewPager) findViewById(R.id.pager);
        ScreenSlidePagerAdapter mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mPager);

        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.TUESDAY) {
            mPager.setCurrentItem(1);
        }
        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY) {
            mPager.setCurrentItem(2);
        }
        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.THURSDAY) {
            mPager.setCurrentItem(3);
        }
        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
            mPager.setCurrentItem(4);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        onPauseCalendar = Calendar.getInstance();
    }

    @Override
    public void onResume() {
        // Lets check if day has changed while app was in background
        super.onResume();
        if (ApplicationLoader.notificationThread != null && !ApplicationLoader.notificationThread.isRunning()) {
            ApplicationLoader.restartNotificationThread();
        }
        Calendar calendar = Calendar.getInstance();
        if (onPauseCalendar != null && calendar.getTimeInMillis() < onPauseCalendar.getTimeInMillis()
                || onPauseCalendar != null && onPauseCalendar.get(Calendar.YEAR) != calendar.get(Calendar.YEAR)
                || onPauseCalendar != null && onPauseCalendar.get(Calendar.DAY_OF_YEAR) != calendar.get(Calendar.DAY_OF_YEAR)) {
            setViewPager();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menu.add(0, 0, 0, getResources().getString(R.string.menuitem_change_schedule));

        SubMenu subMenu = menu.addSubMenu(1, 1, 1, getResources().getString(R.string.menuitem_notifications));
        subMenu.add(2, 2, 2, getResources().getString(R.string.menuitem_one_hour));
        subMenu.add(2, 3, 3, getResources().getString(R.string.menuitem_thirty_minutes));
        subMenu.add(2, 4, 4, getResources().getString(R.string.menuitem_fifteen_minutes));
        subMenu.add(2, 6, 6, getResources().getString(R.string.menuitem_off));

        subMenu.setGroupCheckable(2, true, true);
        menu.add(0, 7, 2, getResources().getString(R.string.menuitem_restore_lessons));
        menu.add(0, 8, 3, getResources().getString(R.string.menuitem_about));

        int notification_type = sharedPreferences.getInt("notifications_type", 0);
        if (notification_type >= 2 && notification_type <= 6) {
            menu.findItem(notification_type).setChecked(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (sharedPreferences != null && type != 0) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            switch (id) {
                case 0:
                    Intent intent = new Intent(ScheduleActivity.this, ChooseTypeActivity.class);
                    startActivity(intent);
                    finish();
                    return true;
                case 2:
                case 3:
                case 4:
                    item.setChecked(true);
                    editor.putInt("notifications_type", id);
                    editor.commit();
                    if (ApplicationLoader.notificationThread != null) {
                        ApplicationLoader.notificationThread.clearNotification();
                    }
                    ApplicationLoader.restartNotificationThread();
                    showSnackbar(getResources().getString(R.string.notification_interval_changed));
                    return true;
                case 6:
                    item.setChecked(true);
                    editor.putInt("notifications_type", id);
                    editor.commit();
                    if (ApplicationLoader.notificationThread != null) {
                        ApplicationLoader.notificationThread.clearNotification();
                    }
                    ApplicationLoader.restartNotificationThread();
                    showSnackbar(getResources().getString(R.string.notifications_turned_off));
                    return true;
                case 7:
                    Intent intent2 = new Intent(ScheduleActivity.this, HiddenLessonsActivity.class);
                    startActivity(intent2);
                    return true;
                case 8:
                    Intent intent3 = new Intent(ScheduleActivity.this, About.class);
                    startActivity(intent3);
                    return true;

            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSnackbar(String text) {
        Snackbar snackbar = Snackbar.make(findViewById(R.id.schedule_coordinator_layout), text, Snackbar.LENGTH_SHORT);
        snackbar.show();
    }

    private class ScreenSlidePagerAdapter extends FragmentPagerAdapter {

        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Calendar calendar = Calendar.getInstance();
            if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                calendar.add(Calendar.DATE, 2);
            }
            if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                calendar.add(Calendar.DATE, 1);
            } else {
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            }
            // Clear old cached schedule data
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            ApplicationLoader.scheduleDatabase.clearOldScheduleData(simpleDateFormat.format(calendar.getTime()));

            if (position <= 4) {
                calendar.add(Calendar.DATE, position);
            } else {
                calendar.add(Calendar.DATE, position + 2); // Skip weekends
            }

            Fragment fragment = new ScheduleFragment();
            Bundle args = new Bundle();
            args.putString("componentId", componentId);
            args.putInt("type", type);
            args.putSerializable("date", calendar.getTime());
            fragment.setArguments(args);

            return fragment;
        }

        @Override
        public int getCount() {
            return 10;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Calendar calendar = Calendar.getInstance();
            if (position == 0 && calendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY
                    || position == 1 && calendar.get(Calendar.DAY_OF_WEEK) == Calendar.TUESDAY
                    || position == 2 && calendar.get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY
                    || position == 3 && calendar.get(Calendar.DAY_OF_WEEK) == Calendar.THURSDAY
                    || position == 4 && calendar.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
                return getResources().getString(R.string.today);
            }
            switch (position) {
                case 0:
                case 5:
                    return getResources().getString(R.string.monday);
                case 1:
                case 6:
                    return getResources().getString(R.string.tuesday);
                case 2:
                case 7:
                    return getResources().getString(R.string.wednesday);
                case 3:
                case 8:
                    return getResources().getString(R.string.thursday);
                case 4:
                case 9:
                    return getResources().getString(R.string.friday);
                default:
                    return "";
            }
        }
    }
}
