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

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.Constants;
import com.giovanniterlingen.windesheim.NotificationCenter;
import com.giovanniterlingen.windesheim.R;
import com.giovanniterlingen.windesheim.controllers.DatabaseController;
import com.giovanniterlingen.windesheim.utils.CookieUtils;
import com.giovanniterlingen.windesheim.utils.TelemetryUtils;
import com.giovanniterlingen.windesheim.utils.TimeUtils;
import com.giovanniterlingen.windesheim.view.Fragments.ScheduleFragment;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class ScheduleActivity extends AppCompatActivity
        implements NotificationCenter.NotificationCenterDelegate {

    private View view;
    private FragmentManager fragmentManager;
    private ViewPager mPager;
    private DrawerLayout mDrawerLayout;
    private RelativeLayout mBottomNavigation;

    private int currentDayIndex = -1;
    private long onPauseMillis;
    private int onPauseWeekCount;
    private int onPauseIndex = -1;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!DatabaseController.getInstance().hasSchedules()) {
            Intent intent = new Intent(ScheduleActivity.this, ChooseScheduleActivity.class);
            startActivity(intent);
            super.onCreate(savedInstanceState);
            finish();
            return;
        }
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ScheduleActivity.this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (sharedPreferences.getInt(Constants.PREFS_NOTIFICATIONS_TYPE, Constants
                .NOTIFICATION_TYPE_NOT_SET) == Constants.NOTIFICATION_TYPE_NOT_SET) {
            editor.putInt(Constants.PREFS_NOTIFICATIONS_TYPE,
                    Constants.NOTIFICATION_TYPE_ALWAYS_ON);
            editor.apply();
        }
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_schedule);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mDrawerLayout = findViewById(R.id.drawer_layout);

        mBottomNavigation = findViewById(R.id.bottom_navigation);
        Button bottomButtonBack = findViewById(R.id.bottom_button_back);
        bottomButtonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nearestMonday(false);
            }
        });

        Button bottomButtonNext = findViewById(R.id.bottom_button_next);
        bottomButtonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nearestMonday(true);
            }
        });

        NavigationView navigationView = findViewById(R.id.navigation_view);
        if (navigationView != null) {
            setupDrawerContent(navigationView);
        }

        view = findViewById(R.id.coordinator_layout);
        setViewPager();
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView
                .setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                        menuItem.setChecked(true);
                        mDrawerLayout.closeDrawers();
                        switch (menuItem.getItemId()) {
                            case R.id.manage_schedules:
                                Intent intent = new Intent(ScheduleActivity.this,
                                        ManageSchedulesActivity.class);
                                startActivity(intent);
                                finish();
                                break;
                            case R.id.natschool:
                                CookieUtils.checkCookieAndIntent(ScheduleActivity.this, false);
                                break;
                            case R.id.educator:
                                CookieUtils.checkCookieAndIntent(ScheduleActivity.this, true);
                                break;
                            case R.id.downloads:
                                Intent intent1 = new Intent(ScheduleActivity.this,
                                        DownloadsActivity.class);
                                startActivity(intent1);
                                break;
                            case R.id.restore_lessons:
                                Intent intent4 = new Intent(ScheduleActivity.this,
                                        HiddenLessonsActivity.class);
                                startActivity(intent4);
                                break;
                            case R.id.about:
                                Intent intent3 = new Intent(ApplicationLoader.applicationContext,
                                        AboutActivity.class);
                                startActivity(intent3);
                                break;
                            case R.id.settings:
                                Intent intent5 = new Intent(ApplicationLoader.applicationContext,
                                        SettingsActivity.class);
                                startActivity(intent5);
                                break;

                        }
                        menuItem.setChecked(false);
                        return true;
                    }
                });
    }

    private void setViewPager() {
        fragmentManager = getSupportFragmentManager();
        mPager = findViewById(R.id.pager);

        Calendar calendar = TimeUtils.getCalendar();
        int calendarDayIndex = calendar.get(GregorianCalendar.DAY_OF_WEEK);

        if (calendarDayIndex <= GregorianCalendar.FRIDAY) {
            currentDayIndex = calendar.get(GregorianCalendar.DAY_OF_WEEK) - 2;
        }

        final ScreenSlidePagerAdapter mPagerAdapter = new ScreenSlidePagerAdapter(
                getSupportFragmentManager(),
                FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        mPager.setAdapter(mPagerAdapter);
        mPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                updateToolbarAndBottomBar(position);
                ((ScheduleFragment) mPagerAdapter.getItem(position)).onVisible();
            }
        });

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mPager);

        if (onPauseIndex > -1 && onPauseIndex < mPager.getAdapter().getCount()) {
            mPager.setCurrentItem(onPauseIndex);
            updateToolbarAndBottomBar(onPauseIndex);
        } else if (currentDayIndex >= 0) {
            mPager.setCurrentItem(currentDayIndex);
            updateToolbarAndBottomBar(currentDayIndex);
        } else {
            updateToolbarAndBottomBar(0);
        }
    }

    @Override
    public void onPause() {
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.scheduleReload);
        onPauseMillis = System.currentTimeMillis();
        onPauseWeekCount = mPager.getAdapter().getCount();
        onPauseIndex = mPager.getCurrentItem();

        TelemetryUtils.getInstance().setCurrentScreen(this, null);
        super.onPause();
    }

    @Override
    public void onResume() {
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.scheduleReload);

        if (showBottomBar()) {
            mBottomNavigation.setVisibility(View.VISIBLE);
        } else {
            mBottomNavigation.setVisibility(View.GONE);
        }

        if (!DateUtils.isToday(onPauseMillis) || onPauseWeekCount != mPager.getAdapter().getCount()) {
            setViewPager();
        }
        super.onResume();

        TelemetryUtils.getInstance().setCurrentScreen(this, "ScheduleActivity");

        // Check if we have asked for a rating already
        long lastReviewPromptTime =
                sharedPreferences.getLong(Constants.PREFS_LAST_REVIEW_PROMPT_TIME, 0);
        if (lastReviewPromptTime == 0 || System.currentTimeMillis() - lastReviewPromptTime >
                TimeUnit.DAYS.toMillis(3)) {
            showRatingSnackbar();

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putLong(Constants.PREFS_LAST_REVIEW_PROMPT_TIME, System.currentTimeMillis());
            editor.apply();
        }
    }

    private void showRatingSnackbar() {
        Snackbar snackbar = Snackbar.make(view, getResources()
                .getString(R.string.rating_prompt_description), Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(R.string.rating_prompt_rate, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse("market://details?id=" +
                        ScheduleActivity.this.getPackageName());
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);

                if (Build.VERSION.SDK_INT >= 21) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                            Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                            Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                } else {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                            Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                }
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/details?id=" +
                                    ScheduleActivity.this.getPackageName())));
                }
            }
        });
        snackbar.show();
    }

    public boolean showBottomBar() {
        if (sharedPreferences == null) {
            return false;
        }
        int weekCount = sharedPreferences.getInt(Constants.PREFS_WEEK_COUNT,
                Constants.DEFAULT_WEEK_COUNT);
        return weekCount > Constants.DEFAULT_WEEK_COUNT;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getExtras() != null && intent.hasExtra("notification")) {
            boolean fromNotification = intent.getExtras().getBoolean("notification");
            if (fromNotification) {
                setViewPager();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            mDrawerLayout.openDrawer(GravityCompat.START);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void showSnackbar(final String text, boolean shrt) {
        Snackbar snackbar = Snackbar.make(view, text, shrt ? Snackbar.LENGTH_SHORT :
                Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    public void updateFragmentView() {
        List<Fragment> fragments = fragmentManager.getFragments();
        for (Fragment fragment : fragments) {
            if (fragment != null && fragment.isVisible()) {
                ((ScheduleFragment) fragment).updateLayout();
                return;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (currentDayIndex == -1 || mPager.getCurrentItem() == currentDayIndex) {
            super.onBackPressed();
            return;
        }
        mPager.setCurrentItem(currentDayIndex);
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.scheduleReload) {
            List<Fragment> fragments = fragmentManager.getFragments();
            for (Fragment fragment : fragments) {
                if (fragment != null && fragment.isVisible()) {
                    ((ScheduleFragment) fragment).updateAdapter();
                    return;
                }
            }
        }
    }

    private void updateToolbarAndBottomBar(int position) {
        Calendar calendar = getCalendarByPosition(position);

        int month = calendar.get(GregorianCalendar.MONTH);
        int year = calendar.get(GregorianCalendar.YEAR);
        String monthString = getResources().getString(Constants.MONTH_STRING_IDS[month]);
        ActionBar toolbar = getSupportActionBar();
        String title = getResources().getString(R.string.schedule_toolbar_text,
                TimeUtils.getDayDateFormat().format(calendar.getTime()), monthString, year);
        if (!title.contentEquals(toolbar.getTitle())) {
            toolbar.setTitle(title);
        }

        // Calculate week offset
        int currentWeek = (position / Constants.WEEKDAYS_COUNT) + 1;
        int totalWeeks = sharedPreferences.getInt(Constants.PREFS_WEEK_COUNT,
                Constants.DEFAULT_WEEK_COUNT);

        TextView bottomText = findViewById(R.id.bottom_text);
        bottomText.setText(getResources().getString(R.string.week_count_bottom_text,
                currentWeek, totalWeeks));
    }

    private Calendar getCalendarByPosition(int position) {
        Calendar calendar = TimeUtils.getCalendar();
        if (calendar.get(GregorianCalendar.DAY_OF_WEEK) == GregorianCalendar.SATURDAY) {
            calendar.add(GregorianCalendar.DATE, 2);
        } else if (calendar.get(GregorianCalendar.DAY_OF_WEEK) == GregorianCalendar.SUNDAY) {
            calendar.add(GregorianCalendar.DATE, 1);
        } else {
            calendar.set(GregorianCalendar.DAY_OF_WEEK, GregorianCalendar.MONDAY);
        }

        calendar.add(GregorianCalendar.DATE, position);

        // Skip weekends
        calendar.add(GregorianCalendar.DATE,
                (position / Constants.WEEKDAYS_COUNT) * Constants.WEEKEND_DAYS_COUNT);

        return calendar;
    }

    private void nearestMonday(boolean forward) {
        int currentIndex = mPager.getCurrentItem();
        int newIndex = (currentIndex / Constants.WEEKDAYS_COUNT + (forward ? 1 : -1)) *
                Constants.WEEKDAYS_COUNT;

        if (newIndex >= 0 && newIndex < mPager.getAdapter().getCount()) {
            mPager.setCurrentItem(newIndex);
        }
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

        ScreenSlidePagerAdapter(@NonNull FragmentManager fm, int behavior) {
            super(fm, behavior);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = new ScheduleFragment();
            Bundle args = new Bundle();
            args.putSerializable("date", getCalendarByPosition(position).getTime());
            fragment.setArguments(args);

            return fragment;
        }

        @Override
        public int getCount() {
            int weekCount = sharedPreferences.getInt(Constants.PREFS_WEEK_COUNT,
                    Constants.DEFAULT_WEEK_COUNT);
            return weekCount * Constants.WEEKDAYS_COUNT;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position == currentDayIndex) {
                return getResources().getString(R.string.today);
            }

            int calendarIndex = (position % Constants.WEEKDAYS_COUNT) +
                    Constants.WEEKEND_DAYS_COUNT;
            switch (calendarIndex) {
                case Calendar.MONDAY:
                    return getResources().getString(R.string.monday);
                case Calendar.TUESDAY:
                    return getResources().getString(R.string.tuesday);
                case Calendar.WEDNESDAY:
                    return getResources().getString(R.string.wednesday);
                case Calendar.THURSDAY:
                    return getResources().getString(R.string.thursday);
                case Calendar.FRIDAY:
                    return getResources().getString(R.string.friday);
                default:
                    return "";
            }
        }
    }
}
