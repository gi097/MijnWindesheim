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
package com.giovanniterlingen.windesheim.view.Adapters;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.giovanniterlingen.windesheim.R;
import com.giovanniterlingen.windesheim.models.NatschoolContent;
import com.giovanniterlingen.windesheim.view.DownloadsActivity;

import java.io.File;
import java.util.List;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public abstract class NatschoolContentAdapter extends RecyclerView.Adapter<NatschoolContentAdapter.ViewHolder> {

    private final List<NatschoolContent> content;
    private final Activity activity;

    private final int[] icons = {
            R.drawable.ic_file_blue,
            R.drawable.ic_file_green,
            R.drawable.ic_file_red,
            R.drawable.ic_file_yellow
    };

    protected NatschoolContentAdapter(Activity activity, List<NatschoolContent> content) {
        this.activity = activity;
        this.content = content;
    }

    protected abstract void onContentClick(NatschoolContent content);

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(activity).
                inflate(R.layout.adapter_item_content, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        TextView contentName = holder.contentName;
        ImageView icon = holder.icon;
        final FrameLayout menuButton = holder.menuButton;
        final ImageView menuButtonImage = holder.menuButtonImage;
        contentName.setText(content.get(position).name);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onContentClick(content.get(holder.getAdapterPosition()));
            }
        });
        if (content.get(position).type == -1) {
            icon.setImageDrawable(ResourcesCompat.getDrawable(activity.getResources(),
                    getDrawableByName(content.get(position).name), null));
            menuButton.setVisibility(View.VISIBLE);
            menuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    menuButtonImage.setImageDrawable(ResourcesCompat.getDrawable(
                            activity.getResources(), R.drawable.overflow_open, null));
                    PopupMenu popupMenu = new PopupMenu(activity, menuButton);
                    popupMenu.inflate(R.menu.menu_file);
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem item) {
                            if (item.getItemId() == R.id.delete_file) {
                                showPromptDialog(holder.getAdapterPosition());
                                return true;
                            }
                            return true;
                        }
                    });
                    popupMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
                        @Override
                        public void onDismiss(PopupMenu menu) {
                            menuButtonImage.setImageDrawable(ResourcesCompat.getDrawable(
                                    activity.getResources(), R.drawable.overflow_normal, null));
                        }
                    });
                    popupMenu.show();
                }
            });
        } else if (content.get(position).url == null || (content.get(position).url.length() == 0)) {
            if (content.get(position).imageUrl != null) {
                icon.setImageDrawable(ResourcesCompat.getDrawable(activity.getResources(),
                        R.drawable.ic_work, null));
            } else {
                icon.setImageDrawable(ResourcesCompat.getDrawable(activity.getResources(),
                        R.drawable.ic_folder, null));
            }
        } else {
            if (content.get(position).type == 1 || content.get(position).type == 3) {
                icon.setImageDrawable(ResourcesCompat.getDrawable(activity.getResources(),
                        R.drawable.ic_link, null));
            } else if (content.get(position).type == 10) {
                icon.setImageDrawable(ResourcesCompat.getDrawable(activity.getResources(),
                        getDrawableByName(content.get(position).url), null));
            }
        }
    }

    private void showPromptDialog(final int position) {
        new AlertDialog.Builder(activity)
                .setTitle(activity.getResources().getString(R.string.confirmation))
                .setMessage(activity.getResources().getString(R.string.delete_file_description))
                .setPositiveButton(activity.getResources().getString(R.string.delete),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                File file = new File(Environment
                                        .getExternalStorageDirectory().toString(),
                                        "MijnWindesheim" + File.separator + content.get(position).name);
                                if (file.exists()) {
                                    file.delete();
                                }
                                content.remove(position);
                                notifyItemRemoved(position);
                                if (content.size() == 0) {
                                    ((DownloadsActivity) activity).showEmptyTextview();
                                }
                                Snackbar snackbar = Snackbar.make(activity
                                                .findViewById(R.id.coordinator_layout),
                                        activity.getResources().getString(R.string.file_deleted),
                                        Snackbar.LENGTH_SHORT);
                                snackbar.show();
                                dialog.cancel();
                            }
                        })
                .setNegativeButton(activity.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                }).show();
    }

    private int getDrawableByName(String name) {
        if (name != null && name.length() != 0) {
            int color = -1;
            if (name.contains(".doc") || name.contains(".txt") || name.contains(".psd")) {
                color = 0;
            } else if (name.contains(".xls") || name.contains(".csv")) {
                color = 1;
            } else if (name.contains(".pdf") || name.contains(".ppt") || name.contains(".key")) {
                color = 2;
            } else if (name.contains(".zip") || name.contains(".rar") || name.contains(".ai") ||
                    name.contains(".mp3") || name.contains(".mov") || name.contains(".avi")) {
                color = 3;
            }
            if (color == -1) {
                int idx;
                String ext = (idx = name.lastIndexOf('.')) == -1 ? "" : name.substring(idx + 1);
                if (ext.length() != 0) {
                    color = ext.charAt(0) % icons.length;
                } else {
                    color = name.charAt(0) % icons.length;
                }
            }
            return icons[color];
        }
        return icons[0];
    }

    @Override
    public int getItemCount() {
        return content.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        final TextView contentName;
        final ImageView icon;
        final FrameLayout menuButton;
        final ImageView menuButtonImage;

        ViewHolder(View view) {
            super(view);
            contentName = view.findViewById(R.id.content_name);
            icon = view.findViewById(R.id.content_icon);
            menuButton = view.findViewById(R.id.menu_button);
            menuButtonImage = view.findViewById(R.id.menu_button_image);
        }
    }
}