package com.giovanniterlingen.windesheim;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

/**
 * A schedule app for Windesheim students
 *
 * @author Giovanni Terlingen
 */
public class About extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        TextView aboutTitle = (TextView) findViewById(R.id.about_title);
        String versionCode;
        try {
            versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            String title = getResources().getString(R.string.app_name) + " " + versionCode;
            aboutTitle.setText(title);
        } catch (PackageManager.NameNotFoundException e) {
            aboutTitle.setText(getResources().getString(R.string.app_name));
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
