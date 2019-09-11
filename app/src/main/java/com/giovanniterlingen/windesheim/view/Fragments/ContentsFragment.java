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
package com.giovanniterlingen.windesheim.view.Fragments;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.NotificationCenter;
import com.giovanniterlingen.windesheim.R;
import com.giovanniterlingen.windesheim.controllers.DownloadController;
import com.giovanniterlingen.windesheim.models.Download;
import com.giovanniterlingen.windesheim.models.NatschoolContent;
import com.giovanniterlingen.windesheim.utils.CookieUtils;
import com.giovanniterlingen.windesheim.utils.WebViewUtils;
import com.giovanniterlingen.windesheim.view.Adapters.NatschoolContentAdapter;
import com.giovanniterlingen.windesheim.view.AuthenticationActivity;
import com.giovanniterlingen.windesheim.view.NatschoolActivity;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class ContentsFragment extends Fragment
        implements NotificationCenter.NotificationCenterDelegate {

    private static final String STUDYROUTE_ID = "STUDYROUTE_ID";
    private static final String PARENT_ID = "PARENT_ID";
    private static final String STUDYROUTE_NAME = "STUDYROUTE_NAME";
    private int studyRouteId = -1;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {

        NotificationCenter.getInstance().addObserver(this, NotificationCenter.downloadPending);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.downloadUpdated);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.downloadFinished);

        final ViewGroup viewGroup = (ViewGroup) inflater.inflate(R.layout.fragment_contents,
                container, false);

        recyclerView = viewGroup.findViewById(R.id.contents_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        progressBar = viewGroup.findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.VISIBLE);

        Bundle bundle = this.getArguments();

        ActionBar toolbar = ((NatschoolActivity) getActivity()).getSupportActionBar();
        if (toolbar != null) {
            String studyRouteName;
            if (bundle != null && (studyRouteName = bundle.getString(STUDYROUTE_NAME)) != null &&
                    studyRouteName.length() != 0) {
                toolbar.setTitle(bundle.getString(STUDYROUTE_NAME));
            } else {
                toolbar.setTitle(getResources().getString(R.string.courses));
            }
            toolbar.setDisplayHomeAsUpEnabled(false);
            toolbar.setDisplayHomeAsUpEnabled(true);
        }
        new NatschoolFetcher((bundle == null ? -1 :
                (studyRouteId = bundle.getInt(STUDYROUTE_ID))),
                (bundle == null ? -1 : bundle.getInt(PARENT_ID, -1)), this)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        return viewGroup;
    }

    private void createWebView(String url) {
        if (url.startsWith("/")) {
            url = "https://elo.windesheim.nl" + url;

            Bundle bundle = new Bundle();
            bundle.putString(WebViewFragment.KEY_URL, url);

            WebViewFragment webViewFragment = new WebViewFragment();
            webViewFragment.setArguments(bundle);

            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.contents_fragment, webViewFragment, "")
                    .addToBackStack("")
                    .commit();
            return;
        }
        WebViewUtils webviewUtils = new WebViewUtils(getActivity());
        webviewUtils.intentCustomTab(url);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.downloadPending);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.downloadUpdated);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.downloadFinished);
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        NatschoolContentAdapter adapter;
        if (recyclerView == null || (adapter = (NatschoolContentAdapter)
                recyclerView.getAdapter()) == null) {
            return;
        }
        if (id == NotificationCenter.downloadPending && (int) args[0] == studyRouteId) {
            adapter.updateItemStarted((int) args[1], (int) args[2]);
        }
        if (id == NotificationCenter.downloadUpdated && (int) args[0] == studyRouteId) {
            adapter.updateItemProgress((int) args[1], (int) args[2], (int) args[3], (String) args[4]);
            return;
        }
        if (id == NotificationCenter.downloadFinished && (int) args[0] == studyRouteId) {
            adapter.updateItemFinished((int) args[1], (int) args[2]);
        }
    }

    private static class NatschoolFetcher extends AsyncTask<Void, Void, Void> {

        private final List<NatschoolContent> content = new ArrayList<>();
        private final WeakReference<ContentsFragment> weakReference;
        private final String type;
        private final int courseId;
        private final int id;

        NatschoolFetcher(int courseId, int id, ContentsFragment contentsFragment) {
            if (courseId == -1 && id == -1) {
                this.type = "courses";
            } else {
                this.type = "content";
            }
            this.courseId = courseId;
            this.id = id;
            weakReference = new WeakReference<>(contentsFragment);
        }

        @Override
        protected Void doInBackground(Void... params) {
            if ("courses".equals(type)) {
                createWebRequest("LoadStudyroutes", "start=0&length=1000&filter=0&search=");
                return null;
            }
            if ("content".equals(type)) {
                createWebRequest("LoadStudyrouteContent", "studyrouteid=" + courseId + "&parentid="
                        + id + "&start=0&length=100");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            final ContentsFragment fragment = weakReference.get();
            if (fragment == null) {
                return;
            }

            fragment.progressBar.setVisibility(View.GONE);

            ViewGroup viewGroup = ((ViewGroup) fragment.getView());
            if (viewGroup == null) {
                return;
            }

            TextView emptyTextView = viewGroup.findViewById(R.id.empty_textview);
            if (content.isEmpty()) {
                emptyTextView.setVisibility(View.VISIBLE);
            } else {
                emptyTextView.setVisibility(View.GONE);
            }

            NatschoolContentAdapter adapter = new NatschoolContentAdapter(fragment.getActivity(), content) {
                @Override
                protected void onContentClick(NatschoolContent content, int position) {
                    if (content.url == null || content.url.length() == 0) {
                        Bundle bundle = new Bundle();
                        if (content.id == -1) {
                            bundle.putInt(STUDYROUTE_ID, content.studyRouteItemId);
                        } else {
                            bundle.putInt(STUDYROUTE_ID, fragment.studyRouteId);
                            bundle.putInt(PARENT_ID, content.id);
                        }
                        bundle.putString(STUDYROUTE_NAME, content.name);

                        ContentsFragment contentsFragment = new ContentsFragment();
                        contentsFragment.setArguments(bundle);

                        fragment.getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.contents_fragment, contentsFragment, "")
                                .addToBackStack("")
                                .commit();
                        return;
                    }
                    if (content.type == 1 || content.type == 3 || content.type == 11) {
                        fragment.createWebView(content.url);
                        return;
                    }
                    if (content.type == 10 && !content.downloading) {
                        new DownloadController(fragment.getActivity(), content.url,
                                fragment.studyRouteId, content.id, position)
                                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                }
            };
            fragment.recyclerView.setAdapter(adapter);
        }

        private void createWebRequest(final String path, final String urlParameters) {
            HttpURLConnection connection = null;
            try {
                URL url = new URL("https://elo.windesheim.nl/services/Studyroutemobile.asmx/"
                        + path + "?" + urlParameters);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("Content-Length", Integer
                        .toString(urlParameters.getBytes().length));
                connection.setRequestProperty("Content-Language", "en-US");
                connection.setRequestProperty("Cookie", CookieUtils.getNatSchoolCookie());

                connection.setUseCaches(false);
                connection.setDoInput(true);
                connection.setDoOutput(true);

                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();

                int status = connection.getResponseCode();

                InputStream is = status >= 400 ? connection.getErrorStream() :
                        connection.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = rd.readLine()) != null) {
                    response.append(line);
                    response.append('\r');
                }
                rd.close();
                JSONObject jsonObject = new JSONObject(response.toString());
                if (type.equals("courses")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("STUDYROUTES");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonobj = jsonArray.getJSONObject(i);
                        content.add(new NatschoolContent(jsonobj.getInt("ID"),
                                jsonobj.getString("NAME"),
                                (jsonobj.has("IMAGEURL_24") ?
                                        jsonobj.getString("IMAGEURL_24") : null)));
                    }
                }
                if (type.equals("content")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("STUDYROUTE_CONTENT");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonobj = jsonArray.getJSONObject(i);
                        int type = jsonobj.getInt("ITEMTYPE");
                        if (type == 0 || type == 1 || type == 3 || type == 10 || type == 11) {
                            NatschoolContent natschoolContent =
                                    new NatschoolContent(jsonobj.getInt("ID"),
                                            jsonobj.getString("NAME"),
                                            jsonobj.getInt("STUDYROUTE_ITEM_ID"), type,
                                            (jsonobj.has("URL") ?
                                                    jsonobj.getString("URL") : null));
                            Download currentDownload = DownloadController.activeDownloads
                                    .get(natschoolContent.id);
                            if (currentDownload == null) {
                                natschoolContent.downloading = false;
                            } else {
                                natschoolContent.downloading = true;
                                natschoolContent.progress = currentDownload.getProgress();
                                natschoolContent.progressString = currentDownload.getProgressString();
                            }
                            content.add(natschoolContent);
                        }
                    }
                }
            } catch (Exception e) {
                final ContentsFragment fragment = weakReference.get();
                if (fragment == null) {
                    return;
                }

                if (e instanceof JSONException) {
                    Intent intent = new Intent(fragment.getActivity(), AuthenticationActivity.class);
                    intent.putExtra("educator", false);

                    if (fragment.getActivity() == null) {
                        return;
                    }
                    fragment.getActivity().startActivity(intent);
                    fragment.getActivity().finish();
                    return;
                }
                ApplicationLoader.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        final Activity activity = fragment.getActivity();
                        if (activity == null) {
                            return;
                        }
                        new AlertDialog.Builder(activity)
                                .setTitle(activity.getResources()
                                        .getString(R.string.alert_connection_title))
                                .setMessage(activity.getResources()
                                        .getString(R.string.alert_connection_description))
                                .setPositiveButton(activity.getResources().getString(R.string.connect),
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                NatschoolFetcher.this.cancel(true);
                                                new NatschoolFetcher(courseId, id, fragment)
                                                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                                                dialog.cancel();
                                            }
                                        })
                                .setNegativeButton(activity.getResources().getString(R.string.cancel),
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                dialog.cancel();
                                            }
                                        }).show();
                    }
                });
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
    }
}
