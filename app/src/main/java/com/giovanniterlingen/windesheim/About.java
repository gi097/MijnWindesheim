package com.giovanniterlingen.windesheim;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

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
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
