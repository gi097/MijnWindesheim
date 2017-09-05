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
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.giovanniterlingen.windesheim.R;
import com.giovanniterlingen.windesheim.handlers.PermissionHandler;
import com.giovanniterlingen.windesheim.objects.Content;
import com.giovanniterlingen.windesheim.objects.IDownloadsView;
import com.giovanniterlingen.windesheim.ui.Adapters.ContentAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class DownloadsActivity extends AppCompatActivity implements IDownloadsView {

    private View view;
    private RecyclerView recyclerView;

    @Override
    public void showEmptyTextview() {
        TextView empty = view.findViewById(R.id.empty_textview);
        empty.setVisibility(View.VISIBLE);
        RecyclerView recyclerView = view.findViewById(R.id.downloads_recyclerview);
        recyclerView.setVisibility(View.GONE);
    }

    private void hideEmptyTextview() {
        TextView empty = view.findViewById(R.id.empty_textview);
        empty.setVisibility(View.GONE);
        RecyclerView recyclerView = view.findViewById(R.id.downloads_recyclerview);
        recyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloads);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        view = findViewById(R.id.coordinator_layout);
        checkPermissions();

        updateFilesList();
    }

    private void checkPermissions() {
        if (!PermissionHandler.verifyStoragePermissions(this)) {
            if (view != null) {
                Snackbar snackbar = Snackbar.make(view, getResources().getString(R.string.no_permission),
                        Snackbar.LENGTH_LONG);
                snackbar.setAction(getResources().getString(R.string.fix), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    }
                });
                snackbar.show();
            }
            showEmptyTextview();
        }
    }

    private void updateFilesList() {
        final File path = new File(Environment.getExternalStorageDirectory().toString(), "MijnWindesheim" + File.separator);
        File files[] = path.listFiles();
        if (files != null && files.length > 0) {
            List<Content> contents = new ArrayList<>();
            for (File f : files) {
                if (!f.isDirectory()) {
                    contents.add(new Content(f.getName()));
                }
            }
            if (contents.size() == 0) {
                showEmptyTextview();
            } else {
                hideEmptyTextview();
            }
            if (recyclerView == null) {
                recyclerView = findViewById(R.id.downloads_recyclerview);
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
            }
            recyclerView.setAdapter(new ContentAdapter(this, contents) {
                @Override
                protected void onContentClick(Content content) {
                    Uri uri;
                    File file = new File(path.getAbsolutePath() + File.separator + content.name);
                    Intent target = new Intent(Intent.ACTION_VIEW);
                    if (android.os.Build.VERSION.SDK_INT >= 24) {
                        uri = FileProvider.getUriForFile(DownloadsActivity.this,
                                "com.giovanniterlingen.windesheim.provider", file);
                        target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } else {
                        uri = Uri.fromFile(file);
                        target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    }
                    String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(uri.toString());
                    String mimetype = android.webkit.MimeTypeMap.getSingleton()
                            .getMimeTypeFromExtension(extension);
                    target.setDataAndType(uri, mimetype);

                    try {
                        startActivity(target);
                    } catch (ActivityNotFoundException e) {
                        if (view != null) {
                            Snackbar snackbar = Snackbar.make(view, getResources().getString(R.string.no_app_found),
                                    Snackbar.LENGTH_SHORT);
                            snackbar.show();
                        }
                    }
                }
            });
        } else {
            showEmptyTextview();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateFilesList();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
