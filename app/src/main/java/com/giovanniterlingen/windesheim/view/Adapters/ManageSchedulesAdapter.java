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

import android.app.Activity;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.R;
import com.giovanniterlingen.windesheim.controllers.ColorController;
import com.giovanniterlingen.windesheim.controllers.DatabaseController;
import com.giovanniterlingen.windesheim.models.Schedule;
import com.giovanniterlingen.windesheim.view.ManageSchedulesActivity;

import org.jetbrains.annotations.NotNull;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class ManageSchedulesAdapter extends RecyclerView.Adapter<ManageSchedulesAdapter.ViewHolder> {

    private final Activity activity;
    private Schedule[] schedules;

    public ManageSchedulesAdapter(Activity activity, Schedule[] schedules) {
        this.activity = activity;
        this.schedules = schedules;
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(activity)
                .inflate(R.layout.adapter_item_manage_schedule, parent, false);
        return new ManageSchedulesAdapter.ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NotNull final ViewHolder holder, int position) {
        TextView scheduleName = holder.scheduleName;
        View scheduleIdentifier = holder.scheduleIdentifier;

        String name = schedules[holder.getAdapterPosition()].getName();
        scheduleName.setText(name);
        scheduleIdentifier.setBackgroundColor(ColorController
                .getColorById(schedules[position].getId()));
        Button deleteButton = holder.delete;
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPromptDialog(holder);
            }
        });
    }

    private void showPromptDialog(final ViewHolder holder) {
        new AlertDialog.Builder(activity)
                .setTitle(activity.getResources().getString(R.string.confirmation))
                .setMessage(activity.getResources().getString(R.string.delete_schedule_description))
                .setPositiveButton(activity.getResources().getString(R.string.delete),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                DatabaseController.getInstance()
                                        .deleteSchedule(schedules[holder.getAdapterPosition()].getId());
                                ApplicationLoader.restartNotificationThread();

                                ColorController.invalidateColorCache();
                                schedules = DatabaseController.getInstance().getSchedules();
                                notifyDataSetChanged();

                                ((ManageSchedulesActivity) activity).showDeletionSnackbar();
                                if (schedules.length == 0) {
                                    ((ManageSchedulesActivity) activity).intent();
                                }
                                dialog.cancel();
                            }
                        })
                .setNegativeButton(activity.getResources()
                        .getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                }).show();
    }

    @Override
    public int getItemCount() {
        if (schedules == null) {
            return 0;
        }
        return schedules.length;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        final Button delete;
        final TextView scheduleName;
        final View scheduleIdentifier;

        ViewHolder(View view) {
            super(view);
            this.scheduleName = view.findViewById(R.id.schedule_name);
            this.delete = view.findViewById(R.id.delete_button);
            this.scheduleIdentifier = view.findViewById(R.id.schedule_identifier);
        }
    }
}
