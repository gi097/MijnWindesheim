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

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.R;
import com.giovanniterlingen.windesheim.utils.TelemetryUtils;
import com.giovanniterlingen.windesheim.view.Fragments.ContentsFragment;
import com.google.android.material.snackbar.Snackbar;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class NatschoolActivity extends AppCompatActivity {

    private View view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contents);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        view = findViewById(R.id.coordinator_layout);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.contents_fragment, new ContentsFragment());
        ft.commit();
        ApplicationLoader.setDarkMode();
    }

    @Override
    protected void onResume() {
        super.onResume();
        TelemetryUtils.getInstance().setCurrentScreen(this, "NatschoolActivity");
    }

    @Override
    protected void onPause() {
        TelemetryUtils.getInstance().setCurrentScreen(this, null);
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            super.onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void downloadCanceled() {
        Snackbar snackbar = Snackbar.make(view, getResources()
                .getString(R.string.canceled_downloading), Snackbar.LENGTH_SHORT);
        snackbar.show();
    }

    public void noSupportedApp() {
        Snackbar snackbar = Snackbar.make(view, getResources()
                .getString(R.string.no_app_found), Snackbar.LENGTH_SHORT);
        snackbar.show();
    }
}
