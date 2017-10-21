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
package com.giovanniterlingen.windesheim.view.Fragments;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.giovanniterlingen.windesheim.R;
import com.giovanniterlingen.windesheim.controllers.DownloadController;
import com.giovanniterlingen.windesheim.controllers.NatSchoolController;
import com.giovanniterlingen.windesheim.models.NatschoolContent;
import com.giovanniterlingen.windesheim.view.Adapters.NatschoolContentAdapter;
import com.giovanniterlingen.windesheim.view.NatschoolActivity;

import java.util.List;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class ContentsFragment extends Fragment {

    private static final String STUDYROUTE_ID = "STUDYROUTE_ID";
    private static final String PARENT_ID = "PARENT_ID";
    private static final String STUDYROUTE_NAME = "STUDYROUTE_NAME";
    private int studyRouteId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        final ViewGroup viewGroup = (ViewGroup) inflater.inflate(R.layout.fragment_contents,
                container, false);
        final RecyclerView recyclerView = viewGroup
                .findViewById(R.id.contents_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        final ProgressBar progressBar = viewGroup
                .findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.VISIBLE);
        Bundle bundle = this.getArguments();
        ActionBar toolbar = ((NatschoolActivity) getActivity()).getSupportActionBar();
        String studyRouteName;
        if (toolbar != null) {
            if (bundle != null && (studyRouteName = bundle.getString(STUDYROUTE_NAME)) != null &&
                    studyRouteName.length() != 0) {
                toolbar.setTitle(bundle.getString(STUDYROUTE_NAME));
            } else {
                toolbar.setTitle(getResources().getString(R.string.courses));
            }
            toolbar.setDisplayHomeAsUpEnabled(false);
            toolbar.setDisplayHomeAsUpEnabled(true);
        }
        new NatSchoolController((bundle == null ? -1 : (studyRouteId = bundle.getInt(STUDYROUTE_ID))),
                (bundle == null ? -1 : bundle.getInt(PARENT_ID, -1)), getActivity()) {
            @Override
            public void onFinished(final List<NatschoolContent> courses) {
                progressBar.setVisibility(View.GONE);
                TextView emptyTextView = viewGroup.findViewById(R.id.empty_textview);
                if (courses.isEmpty()) {
                    emptyTextView.setVisibility(View.VISIBLE);
                } else {
                    emptyTextView.setVisibility(View.GONE);
                }
                recyclerView.setAdapter(new NatschoolContentAdapter(getActivity(), courses) {
                    @Override
                    protected void onContentClick(NatschoolContent content) {
                        if (content.url == null || content.url.length() == 0) {
                            Bundle bundle = new Bundle();
                            if (content.id == -1) {
                                bundle.putInt(STUDYROUTE_ID, content.studyRouteItemId);
                            } else {
                                bundle.putInt(STUDYROUTE_ID, studyRouteId);
                                bundle.putInt(PARENT_ID, content.id);
                            }
                            bundle.putString(STUDYROUTE_NAME, content.name);

                            ContentsFragment contentsFragment = new ContentsFragment();
                            contentsFragment.setArguments(bundle);

                            getActivity().getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.contents_fragment, contentsFragment, "")
                                    .addToBackStack("")
                                    .commit();
                        } else {
                            if (content.type == 1 || content.type == 3) {
                                createWebview(content.url);
                                return;
                            }
                            if (content.type == 10) {
                                ProgressDialog mProgressDialog = new ProgressDialog(getActivity());
                                mProgressDialog.setMessage(getResources().getString(R.string.downloading));
                                mProgressDialog.setIndeterminate(true);
                                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                                mProgressDialog.setCancelable(true);
                                new DownloadController(getActivity(), mProgressDialog)
                                        .execute(content.url);
                            }
                        }
                    }
                });
            }
        }.execute();
        return viewGroup;
    }

    private void createWebview(String url) {
        if (!url.startsWith("http")) {
            url = "https://elo.windesheim.nl" + url;
        }
        Bundle bundle = new Bundle();
        bundle.putString(WebviewFragment.KEY_URL, url);

        WebviewFragment webviewFragment = new WebviewFragment();
        webviewFragment.setArguments(bundle);

        getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.contents_fragment, webviewFragment, "")
                .addToBackStack("")
                .commit();
    }
}
