package com.giovanniterlingen.windesheim;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

/**
 * A scheduler app for Windesheim students
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
        } catch (PackageManager.NameNotFoundException e) {
            versionCode = null;
        }
        if (versionCode != null) {
            aboutTitle.setText("Windesheim " + versionCode);
        } else {
            aboutTitle.setText("Windesheim");
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
