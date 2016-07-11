package com.giovanniterlingen.windesheim;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

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
    private static View view;
    private SharedPreferences sharedPreferences;
    private long onPauseMillis;
    private DrawerLayout mDrawerLayout;

    public static void showSnackbar(final String text) {
        ApplicationLoader.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                if (view != null) {
                    Snackbar snackbar = Snackbar.make(view, text, Snackbar.LENGTH_SHORT);
                    snackbar.show();
                }
            }
        });
    }

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
        if (sharedPreferences.getInt("notifications_type", 0) == 0) {
            editor.putInt("notifications_type", 5);
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

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (navigationView != null) {
            setupDrawerContent(navigationView);
        }

        view = findViewById(R.id.schedule_coordinator_layout);

        setViewPager();

        showRateSnackbar();
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView
                .setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        menuItem.setChecked(true);
                        mDrawerLayout.closeDrawers();
                        switch (menuItem.getItemId()) {
                            case R.id.webmail:
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse("https://outlook.com/windesheim.nl"));
                                startActivity(intent);
                                return true;
                            case R.id.about:
                                Intent intent1 = new Intent(ApplicationLoader.applicationContext,
                                        About.class);
                                startActivity(intent1);
                                return true;
                            case R.id.support_development:
                                Intent intent2 = new Intent(Intent.ACTION_VIEW);
                                intent2.setData(Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=gogio%40live%2enl&lc=US&item_name=Donate%20to%20Giovanni&currency_code=EUR&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHosted"));
                                startActivity(intent2);
                                return true;
                        }
                        return true;
                    }
                });
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
        onPauseMillis = System.currentTimeMillis();
    }

    @Override
    public void onResume() {
        // Lets check if day has changed while app was in background
        super.onResume();
        if (ApplicationLoader.notificationThread != null && !ApplicationLoader.notificationThread.isRunning()) {
            ApplicationLoader.restartNotificationThread();
        }
        if (!DateUtils.isToday(onPauseMillis)) {
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
        subMenu.add(2, 5, 5, getResources().getString(R.string.menuitem_always_on));
        subMenu.add(2, 6, 6, getResources().getString(R.string.menuitem_off));

        subMenu.setGroupCheckable(2, true, true);
        menu.add(0, 7, 2, getResources().getString(R.string.menuitem_restore_lessons));

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
                case android.R.id.home:
                    mDrawerLayout.openDrawer(GravityCompat.START);
                    return true;
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
                case 5:
                    item.setChecked(true);
                    editor.putInt("notifications_type", id);
                    editor.commit();
                    if (ApplicationLoader.notificationThread != null) {
                        ApplicationLoader.notificationThread.clearNotification();
                    }
                    ApplicationLoader.restartNotificationThread();
                    showSnackbar(getResources().getString(R.string.persistent_notification));
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
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void showRateSnackbar() {
        if (view != null) {
            Snackbar snackbar = Snackbar.make(view, getResources().getString(R.string.rate_dialog), Snackbar.LENGTH_LONG);
            snackbar.setAction(getResources().getString(R.string.rate), new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String appName = ApplicationLoader.applicationContext.getPackageName();
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=" + appName)));
                    } catch (ActivityNotFoundException e) {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("http://play.google.com/store/apps/details?id=" + appName)));
                    }
                }
            });
            snackbar.show();
        }
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
            Calendar calendar = Calendar.getInstance();
            if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                ApplicationLoader.scheduleDatabase.clearOldScheduleData(simpleDateFormat.format(calendar.getTime()));
                ApplicationLoader.scheduleDatabase.deleteOldFetched(simpleDateFormat.format(calendar.getTime()));
                calendar.add(Calendar.DATE, 2);
            } else if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                ApplicationLoader.scheduleDatabase.clearOldScheduleData(simpleDateFormat.format(calendar.getTime()));
                ApplicationLoader.scheduleDatabase.deleteOldFetched(simpleDateFormat.format(calendar.getTime()));
                calendar.add(Calendar.DATE, 1);
            } else {
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                ApplicationLoader.scheduleDatabase.clearOldScheduleData(simpleDateFormat.format(calendar.getTime()));
                ApplicationLoader.scheduleDatabase.deleteOldFetched(simpleDateFormat.format(calendar.getTime()));
            }
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
