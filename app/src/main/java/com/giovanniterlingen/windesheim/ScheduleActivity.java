package com.giovanniterlingen.windesheim;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * A scheduler app for Windesheim students
 *
 * @author Giovanni Terlingen
 */
public class ScheduleActivity extends AppCompatActivity {

    private static String componentId;
    private static int type;
    private SharedPreferences sharedPreferences;
    private Calendar calendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ScheduleActivity.this);
        // Fix previous versions
        String classId = sharedPreferences.getString("classId", "");
        if (classId.length() > 0) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("componentId", classId);
            editor.putInt("type", 1);
            editor.remove("classId");
            editor.commit();
        }
        componentId = sharedPreferences.getString("componentId", "");
        type = sharedPreferences.getInt("type", 0);
        if (componentId.length() == 0 || type == 0) {
            Intent intent = new Intent(ScheduleActivity.this, ChooseTypeActivity.class);
            startActivity(intent);
            super.onCreate(savedInstanceState);
            finish();
            return;
        }
        ApplicationLoader.postInitApplication();
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_schedule);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Instantiate a ViewPager and a PagerAdapter.
        ViewPager mPager = (ViewPager) findViewById(R.id.pager);
        ScreenSlidePagerAdapter mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mPager);

        // Check which page we need
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_schedule, menu);
        menu.findItem(R.id.action_notifications).setChecked(sharedPreferences.getBoolean("notifications", false));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_change_class) {
            Intent intent = new Intent(ScheduleActivity.this, ChooseTypeActivity.class);
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_notifications) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (item.isChecked()) {
                item.setChecked(false);
                editor.putBoolean("notifications", false);
                editor.commit();
                Snackbar snackbar = Snackbar.make(findViewById(R.id.schedule_coordinator_layout), "Notificaties uitgeschakeld", Snackbar.LENGTH_SHORT);
                snackbar.show();
            } else {
                item.setChecked(true);
                editor.putBoolean("notifications", true);
                editor.commit();
                Snackbar snackbar = Snackbar.make(findViewById(R.id.schedule_coordinator_layout), "Notificaties ingeschakeld", Snackbar.LENGTH_SHORT);
                snackbar.show();
            }
            return true;
        }
        if (id == R.id.action_about) {
            Intent intent = new Intent(ScheduleActivity.this, About.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
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
            args.putSerializable("Date", calendar.getTime());
            fragment.setArguments(args);

            return fragment;
        }

        @Override
        public int getCount() {
            return 10;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 0 && calendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY || position == 1 && calendar.get(Calendar.DAY_OF_WEEK) == Calendar.TUESDAY || position == 2 && calendar.get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY || position == 3 && calendar.get(Calendar.DAY_OF_WEEK) == Calendar.THURSDAY || position == 4 && calendar.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
                return "Vandaag";
            }
            switch (position) {
                case 0:
                case 5:
                    return "Maandag";
                case 1:
                case 6:
                    return "Dinsdag";
                case 2:
                case 7:
                    return "Woensdag";
                case 3:
                case 8:
                    return "Donderdag";
                case 4:
                case 9:
                    return "Vrijdag";
                default:
                    return null;
            }
        }
    }
}
