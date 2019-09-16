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

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.Constants;
import com.giovanniterlingen.windesheim.R;
import com.giovanniterlingen.windesheim.utils.TelemetryUtils;
import com.google.android.material.textfield.TextInputLayout;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 * @author Thomas Visch
 */
public class AuthenticationActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText passwordEditText;
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

        usernameEditText = findViewById(R.id.input_username);
        passwordEditText = findViewById(R.id.input_password);
        progressBar = findViewById(R.id.login_progress);
        headerTextView = findViewById(R.id.login_header);
        usernameTextLayout = findViewById(R.id.input_username_layout);
        passwordTextLayout = findViewById(R.id.input_password_layout);

        preferences = PreferenceManager
                .getDefaultSharedPreferences(AuthenticationActivity.this);

        String username;
        String password;

        Button button = findViewById(R.id.login_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isValid = true;
                String username = usernameEditText.getText().toString();
                String password = passwordEditText.getText().toString();
                if (username.endsWith("@windesheim.nl")) {
                    passwordTextLayout.setErrorEnabled(false);
                    usernameTextLayout.setError(getString(R.string.auth_student_only));
                    isValid = false;
                } else if (!username.toLowerCase().startsWith("s") ||
                        !username.toLowerCase().endsWith("@student.windesheim.nl")) {
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
                    usernameTextLayout.setErrorEnabled(false);
                    passwordTextLayout.setErrorEnabled(false);
                    progressBar.setVisibility(View.VISIBLE);
                    authenticate(usernameEditText.getText().toString(),
                            passwordEditText.getText().toString());
                }
            }
        });
        if ((username = preferences.getString(Constants.PREFS_USERNAME, "")).length() > 0 &&
                (password = preferences.getString(Constants.PREFS_PASSWORD, "")).length() > 0) {
            isRedirected = true;
            usernameEditText.setText(username);
            usernameEditText.setSelection(username.length());
            passwordEditText.setText(password);
            progressBar.setVisibility(View.VISIBLE);
            authenticate(username, password);
        }
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

        headerTextView.setText(getString(R.string.auth_loading));

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
                    editor.putString(Constants.PREFS_USERNAME, username);
                    editor.putString(Constants.PREFS_PASSWORD, password);
                    editor.apply();

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
                            editor.remove(Constants.PREFS_USERNAME);
                            editor.remove(Constants.PREFS_PASSWORD);
                            editor.apply();
                        }
                        headerTextView.setText(getString(R.string.auth_add_account));
                        progressBar.setVisibility(View.GONE);
                        usernameTextLayout.setError(getString(R.string.auth_login_failed));
                        passwordTextLayout.setError(getString(R.string.auth_login_failed));
                        loginUrl = null;
                        isBusy = false;

                        Bundle bundle = new Bundle();
                        bundle.putBoolean(Constants.TELEMETRY_PROPERTY_LOGIN_SUCCESSFUL, false);
                        TelemetryUtils.getInstance()
                                .logEvent(Constants.TELEMETRY_LOGIN, bundle);

                        return;
                    }
                    view.loadUrl(getJavascriptString(username, password));
                    loginUrl = url;
                } else if (url.startsWith("https://windesheimapi.azurewebsites.net")) {
                    usernameTextLayout.setErrorEnabled(false);
                    passwordTextLayout.setErrorEnabled(false);

                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString(Constants.PREFS_USERNAME, username);
                    editor.putString(Constants.PREFS_PASSWORD, password);
                    editor.apply();

                    isBusy = false;

                    Bundle bundle = new Bundle();
                    bundle.putBoolean(Constants.TELEMETRY_PROPERTY_LOGIN_SUCCESSFUL, true);
                    TelemetryUtils.getInstance()
                            .logEvent(Constants.TELEMETRY_LOGIN, bundle);

                    if (username.startsWith("s") && username.endsWith("@student.windesheim.nl")) {
                        String studentNumber = username.substring(1)
                                .replace("@student.windesheim.nl", "");
                        TelemetryUtils.getInstance()
                                .setUserProperty(Constants.TELEMETRY_PROPERTY_STUDENT_NUMBER,
                                        studentNumber);
                    }

                    Intent intent = new Intent(AuthenticationActivity.this, EducatorActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });
        webView.loadUrl(isEducator ? "https://login.microsoftonline.com/login.srf?wa=wsignin1.0&whr=windesheim.nl&wreply=https://liveadminwindesheim.sharepoint.com/sites/wip" : "https://elo.windesheim.nl/");
    }

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
    protected void onResume() {
        super.onResume();
        TelemetryUtils.getInstance().setCurrentScreen(this, "AuthenticationActivity");
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
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}