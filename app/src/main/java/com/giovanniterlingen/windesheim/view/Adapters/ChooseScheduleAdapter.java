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
package com.giovanniterlingen.windesheim.view.Adapters;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.giovanniterlingen.windesheim.R;
import com.giovanniterlingen.windesheim.models.ScheduleItem;

import java.util.ArrayList;
import java.util.List;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public abstract class ChooseScheduleAdapter extends RecyclerView.Adapter<ChooseScheduleAdapter.ViewHolder> {

    private final List<ScheduleItem> scheduleItems;
    private final List<ScheduleItem> scheduleItemsFilterable = new ArrayList<>();
    private final Context context;

    protected ChooseScheduleAdapter(Context context, List<ScheduleItem> scheduleItems) {
        this.context = context;
        this.scheduleItems = scheduleItems;
        this.scheduleItemsFilterable.addAll(scheduleItems);
    }

    protected abstract void onContentClick(int id, String name);

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).
                inflate(R.layout.adapter_item_schedule_item, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        TextView name = holder.name;
        name.setText(scheduleItemsFilterable.get(position).name);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onContentClick(scheduleItemsFilterable.get(holder.getAdapterPosition()).id,
                        scheduleItemsFilterable.get(holder.getAdapterPosition()).name);
            }
        });
    }

    @Override
    public int getItemCount() {
        return scheduleItemsFilterable.size();
    }

    public synchronized void filter(String text) {
        if (text.length() == 0) {
            scheduleItemsFilterable.clear();
            scheduleItemsFilterable.addAll(scheduleItems);
        } else {
            scheduleItemsFilterable.clear();
            text = text.toLowerCase();
            for (ScheduleItem scheduleItem : scheduleItems) {
                if (scheduleItem.name.toLowerCase().contains(text)) {
                    scheduleItemsFilterable.add(scheduleItem);
                }
            }
        }
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        final TextView name;

        ViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.schedule_item);
        }
    }
}