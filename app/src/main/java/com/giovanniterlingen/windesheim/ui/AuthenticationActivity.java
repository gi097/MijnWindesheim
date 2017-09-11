/**
 * Copyright (c) 2017 Giovanni Terlingen
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

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.R;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 * @author Thomas Visch
 */
public class AuthenticationActivity extends AppCompatActivity {

    private TextView usernameTextView;
    private TextView passwordTextView;
    private TextView headerTextView;
    private TextInputLayout usernameTextLayout;
    private TextInputLayout passwordTextLayout;
    private ProgressBar progressBar;
    private boolean isRedirected = false;
    private WebView webView;
    private boolean isEducator = false;
    private SharedPreferences preferences;
    private boolean isBusy = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Intent intent = getIntent();
        isEducator = intent.getBooleanExtra("educator", false);

        usernameTextView = findViewById(R.id.input_username);
        passwordTextView = findViewById(R.id.input_password);
        progressBar = findViewById(R.id.login_progress);
        headerTextView = findViewById(R.id.login_header);
        usernameTextLayout = findViewById(R.id.input_username_layout);
        passwordTextLayout = findViewById(R.id.input_password_layout);

        preferences = PreferenceManager
                .getDefaultSharedPreferences(AuthenticationActivity.this);

        String username;
        String password;

        if ((username = preferences.getString("username", "")).length() > 0 &&
                (password = preferences.getString("password", "")).length() > 0) {
            isRedirected = true;
            usernameTextView.setText(username);
            passwordTextView.setText(password);
            progressBar.setVisibility(View.VISIBLE);
            authenticate(username, password);
            return;
        }

        Button button = findViewById(R.id.login_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isValid = true;
                String username = usernameTextView.getText().toString();
                String password = passwordTextView.getText().toString();
                if (username.length() <= 1 || !username.toLowerCase().startsWith("s") &&
                        !username.toLowerCase().startsWith("p") ||
                        !username.toLowerCase().contains("@student.windesheim.nl") &&
                                !username.toLowerCase().contains("windesheim.nl")) {
                    passwordTextLayout.setErrorEnabled(false);
                    usernameTextLayout.setError(getString(R.string.auth_invalid_username));
                    isValid = false;
                }
                if (password.length() == 0) {
                    if (isValid) {
                        usernameTextLayout.setErrorEnabled(false);
                    }
                    passwordTextLayout.setError(getString(R.string.auth_invalid_password));
                    return;
                }
                if (isValid) {
                    headerTextView.setText(getString(R.string.auth_loading));
                    usernameTextLayout.setErrorEnabled(false);
                    passwordTextLayout.setErrorEnabled(false);
                    progressBar.setVisibility(View.VISIBLE);
                    authenticate(usernameTextView.getText().toString(),
                            passwordTextView.getText().toString());
                }
            }
        });
    }

    private void authenticate(final String username, final String password) {
        if (isBusy) {
            return;
        }
        if (!ApplicationLoader.isConnected()) {
            headerTextView.setText(getString(R.string.auth_add_account));
            progressBar.setVisibility(View.GONE);
            showConnectionError();
            return;
        }

        isBusy = true;

        if (webView != null) {
            webView.destroy();
        }

        webView = new WebView(AuthenticationActivity.this);
        webView.getSettings().setJavaScriptEnabled(true);
        if (android.os.Build.VERSION.SDK_INT < 26) {
            webView.getSettings().setSaveFormData(false);
        }

        webView.setWebViewClient(new WebViewClient() {

            String loginUrl;

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                usernameTextLayout.setErrorEnabled(false);
                passwordTextLayout.setErrorEnabled(false);
                if (!isEducator && url.equals("https://elo.windesheim.nl/pages/default.aspx") ||
                        url.equals("https://elo.windesheim.nl/Pages/Mobile/index.html")) {
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("username", username);
                    editor.putString("password", password);
                    editor.commit();

                    isBusy = false;

                    Intent intent = new Intent(AuthenticationActivity.this, NatschoolActivity.class);
                    startActivity(intent);
                    finish();
                } else if (url.startsWith("https://liveadminwindesheim.sharepoint.com/sites/wip")) {
                    webView.loadUrl("https://windesheimapi.azurewebsites.net");
                }
            }

            @Override
            public void onPageFinished(final WebView view, String url) {
                if (url.startsWith("https://sts.windesheim.nl/adfs/ls/")) {
                    // We have the proper login page
                    if (loginUrl != null && loginUrl.equals(url)) {
                        // second time the login page displays, wrong credentials
                        if (isRedirected) {
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.remove("username");
                            editor.remove("password");
                            editor.commit();
                        }
                        headerTextView.setText(getString(R.string.auth_add_account));
                        progressBar.setVisibility(View.GONE);
                        usernameTextLayout.setError(getString(R.string.auth_login_failed));
                        loginUrl = null;
                        isBusy = false;
                        return;
                    }
                    view.loadUrl(getJavascriptString(username, password));
                    loginUrl = url;
                } else if (url.startsWith("https://windesheimapi.azurewebsites.net")) {
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("username", username);
                    editor.putString("password", password);
                    editor.commit();

                    isBusy = false;

                    Intent intent = new Intent(AuthenticationActivity.this, EducatorActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });
        webView.loadUrl(isEducator ? "https://login.microsoftonline.com/login.srf?wa=wsignin1.0&whr=windesheim.nl&wreply=https://liveadminwindesheim.sharepoint.com/sites/wip" : "https://elo.windesheim.nl/");
    }

    /**
     * Ugly workaround to get login working
     *
     * @return A javascript string to insert the values
     */
    private String getJavascriptString(String username, String password) {
        return "javascript:document.getElementById('userNameInput').value='" +
                username +
                "';document.getElementById('passwordInput').value='" +
                password +
                "';document.getElementById('submitButton').onclick();";
    }

    private void showConnectionError() {
        new AlertDialog.Builder(AuthenticationActivity.this)
                .setTitle(getResources().getString(R.string.alert_connection_title))
                .setMessage(getResources().getString(R.string.alert_connection_description))
                .setNegativeButton(getResources().getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        }).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}