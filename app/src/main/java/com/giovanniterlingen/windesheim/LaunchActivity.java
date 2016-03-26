package com.giovanniterlingen.windesheim;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

/**
 * A schedule app for Windesheim students
 *
 * @author Giovanni Terlingen
 */
public class LaunchActivity extends Activity {

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(LaunchActivity.this, ScheduleActivity.class);
                startActivity(intent);
                finish();
            }
        }, 2000);
    }
}