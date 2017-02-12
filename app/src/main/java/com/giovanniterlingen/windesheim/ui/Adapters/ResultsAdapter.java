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
package com.giovanniterlingen.windesheim.ui.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.R;
import com.giovanniterlingen.windesheim.objects.EC;
import com.giovanniterlingen.windesheim.objects.Result;
import com.github.lzyzsd.circleprogress.DonutProgress;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class ResultsAdapter extends RecyclerView.Adapter<ResultsAdapter.ViewHolder> {

    private Context context;
    private Result[][] results;
    private EC[] ec;

    public ResultsAdapter(Context context, Result[][] results, EC[] ec) {
        this.context = context;
        this.results = results;
        this.ec = ec;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int resId = 0;
        if (viewType == 0) {
            resId = R.layout.result_adapter_header;
        } else if (viewType == 1) {
            resId = R.layout.result_adapter_divider;
        } else if (viewType == 2) {
            resId = R.layout.result_adapter_item;
        }
        View itemView = LayoutInflater.from(context).inflate(resId, parent, false);
        return new ResultsAdapter.ViewHolder(itemView, viewType);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        int index = 0;
        int total = 0;
        for (int i = 0; i < results.length; i++) {
            total += results[i].length + 2;
            if (position < total) {
                index = i;
                break;
            }
        }
        if (holder.type == 0) {
            DonutProgress progress = holder.progressBar;
            float percent = 0.0f;
            if (ec[index].getCurrentEC() > 0 && ec[index].getMaxEC() > 0) {
                percent = (float) ec[index].getCurrentEC() / (float) ec[index].getMaxEC() * 100f;
            }
            int scale = (int) Math.pow(10, 2);
            progress.setProgress((float) Math.floor(percent * scale) / scale);
            progress.setMax(100);
            TextView studyName = holder.studyName;
            studyName.setText(ec[index].getStudyName());
            TextView ecDescription = holder.description;
            ecDescription.setText(ApplicationLoader.applicationContext.getResources().getString(R.string.ec_description, Integer.toString(ec[index].getCurrentEC()), Integer.toString(ec[index].getMaxEC())));
        }
        if (holder.type == 2) {
            TextView nameTextView = holder.name;
            TextView markTextView = holder.result;
            int previous = 0;
            for (int i = 0; i < index; i++) {
                previous += results[i].length;
            }
            Result result = results[index][position - (((index + 1) * 2) + previous)];
            nameTextView.setText(result.getModule());
            if (result.getMark() != null && result.getMark().length() > 0) {
                float mark = Float.parseFloat(result.getMark());
                if (mark >= 5.5) {
                    markTextView.setTextColor(0xff689f38);
                } else {
                    markTextView.setTextColor(0xffd50000);
                }
                markTextView.setText(result.getMark());
            } else {
                markTextView.setText("");
            }
        }
    }

    @Override
    public int getItemCount() {
        if (results == null || results.length == 0) {
            return 0;
        }
        int total = 0;
        for (int i = 0; i < results.length; i++) {
            total += results[i].length + 2;
        }
        return total;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return 0;
        }
        if (position == 1) {
            return 1;
        }
        int total = 0;
        for (int i = 0; i < results.length; i++) {
            total += results[i].length + 2;
            if (position == total) {
                return 0;
            } else if (position == total + 1) {
                return 1;
            }
        }
        return 2;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public int type;
        public TextView name;
        public TextView result;

        public DonutProgress progressBar;
        public TextView studyName;
        public TextView description;

        public ViewHolder(View view, int viewType) {
            super(view);
            this.type = viewType;
            if (viewType == 0) {
                progressBar = (DonutProgress) view.findViewById(R.id.ec_progress);
                studyName = (TextView) view.findViewById(R.id.lesson_name);
                description = (TextView) view.findViewById(R.id.ec_description);
            } else if (viewType == 2) {
                name = (TextView) view.findViewById(R.id.lesson_name);
                result = (TextView) view.findViewById(R.id.result);
            }
        }
    }
}
