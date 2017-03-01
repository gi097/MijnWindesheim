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
package com.giovanniterlingen.windesheim.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
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
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.R;
import com.giovanniterlingen.windesheim.handlers.CookieHandler;
import com.giovanniterlingen.windesheim.objects.IScheduleView;
import com.giovanniterlingen.windesheim.ui.Fragments.ScheduleFragment;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import java.util.Calendar;
import java.util.List;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class ScheduleActivity extends AppCompatActivity implements IScheduleView {

    private String componentId;
    private int type;
    private View view;
    private FragmentManager fragmentManager;
    private long onPauseMillis;
    private DrawerLayout mDrawerLayout;

    @Override
    public void showSnackbar(final String text) {
        Snackbar snackbar = Snackbar.make(view, text, Snackbar.LENGTH_SHORT);
        snackbar.show();
    }

    @Override
    public void updateFragmentView() {
        List<Fragment> fragments = fragmentManager.getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                if (fragment != null && fragment.isMenuVisible()) {
                    ((ScheduleFragment) fragment).updateLayout();
                    break;
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ScheduleActivity.this);
        // Fix previous versions
        String classId = sharedPreferences.getString("classId", "");
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (classId.length() > 0) {
            editor.putString("componentId", classId);
            editor.putInt("type", 1);
            editor.remove("classId");
            editor.remove("schedule_change_service");
            editor.apply();
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
            editor.apply();
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

        view = findViewById(R.id.coordinator_layout);

        final LinearLayout layout = (LinearLayout) findViewById(R.id.pager_container);
        int height = AdSize.SMART_BANNER.getHeightInPixels(ScheduleActivity.this);
        layout.setPadding(0, 0, 0, height);

        final AdView mAdView = (AdView) findViewById(R.id.bottom_ad);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        setViewPager();

        showRateSnackbar();
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView
                .setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                        menuItem.setChecked(true);
                        mDrawerLayout.closeDrawers();
                        switch (menuItem.getItemId()) {
                            case R.id.change_schedule:
                                Intent intent = new Intent(ScheduleActivity.this,
                                        ChooseTypeActivity.class);
                                startActivity(intent);
                                finish();
                                menuItem.setChecked(false);
                                return true;
                            case R.id.natschool:
                                CookieHandler.checkCookieAndIntent(ScheduleActivity.this, false);
                                menuItem.setChecked(false);
                                return true;
                            case R.id.educator:
                                CookieHandler.checkCookieAndIntent(ScheduleActivity.this, true);
                                menuItem.setChecked(false);
                                return true;
                            case R.id.downloads:
                                Intent intent1 = new Intent(ScheduleActivity.this,
                                        DownloadsActivity.class);
                                startActivity(intent1);
                                menuItem.setChecked(false);
                                return true;
                            case R.id.restore_lessons:
                                Intent intent4 = new Intent(ScheduleActivity.this,
                                        HiddenLessonsActivity.class);
                                startActivity(intent4);
                                menuItem.setChecked(false);
                                return true;
                            case R.id.about:
                                Intent intent3 = new Intent(ApplicationLoader.applicationContext,
                                        AboutActivity.class);
                                startActivity(intent3);
                                menuItem.setChecked(false);
                                return true;
                            case R.id.settings:
                                Intent intent5 = new Intent(ApplicationLoader.applicationContext,
                                        SettingsActivity.class);
                                startActivity(intent5);
                                menuItem.setChecked(false);
                        }
                        return true;
                    }
                });
    }

    /**
     * Bind activities and set the position according to the current date.
     */
    private void setViewPager() {
        Calendar calendar = Calendar.getInstance();
        fragmentManager = getSupportFragmentManager();
        ViewPager mPager = (ViewPager) findViewById(R.id.pager);
        if (mPager != null) {
            ScreenSlidePagerAdapter mPagerAdapter = new ScreenSlidePagerAdapter(
                    getSupportFragmentManager());
            mPager.setAdapter(mPagerAdapter);
            TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
            if (tabLayout != null) {
                tabLayout.setupWithViewPager(mPager);
            }
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
        if (!DateUtils.isToday(onPauseMillis)) {
            setViewPager();
        }
    }

    /**
     * Show a snackbar which asks to review this app in the PlayStore.
     */
    private void showRateSnackbar() {
        if (view != null) {
            Snackbar snackbar = Snackbar.make(view, getResources().getString(R.string.rate_dialog),
                    Snackbar.LENGTH_LONG);
            snackbar.setAction(getResources().getString(R.string.rate), new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String appName = ApplicationLoader.applicationContext.getPackageName();
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=" + appName)));
                    } catch (ActivityNotFoundException e) {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("http://play.google.com/store/apps/details?id="
                                        + appName)));
                    }
                }
            });
            snackbar.show();
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

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

        ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Calendar calendar = Calendar.getInstance();
            if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                ApplicationLoader.scheduleDatabase.clearOldScheduleData(calendar.getTime());
                calendar.add(Calendar.DATE, 2);
            } else if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                ApplicationLoader.scheduleDatabase.clearOldScheduleData(calendar.getTime());
                calendar.add(Calendar.DATE, 1);
            } else {
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                ApplicationLoader.scheduleDatabase.clearOldScheduleData(calendar.getTime());
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
