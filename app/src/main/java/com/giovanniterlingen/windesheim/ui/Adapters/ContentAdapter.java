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

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.R;
import com.giovanniterlingen.windesheim.objects.Content;
import com.giovanniterlingen.windesheim.ui.DownloadsActivity;

import java.io.File;
import java.util.List;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public abstract class ContentAdapter extends RecyclerView.Adapter<ContentAdapter.ViewHolder> {

    private List<Content> content;
    private Activity context;

    private int icons[] = {
            R.drawable.ic_file_blue,
            R.drawable.ic_file_green,
            R.drawable.ic_file_red,
            R.drawable.ic_file_yellow
    };


    public ContentAdapter(Activity context, List<Content> content) {
        this.context = context;
        this.content = content;
    }

    protected abstract void onContentClick(Content content);

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).
                inflate(R.layout.content_adapter_item, parent, false);
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
                onContentClick(content.get(position));
            }
        });
        if (content.get(position).type == -1) {
            icon.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), getDrawableByName(content.get(position).name), null));
            menuButton.setVisibility(View.VISIBLE);
            menuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    menuButtonImage.setImageDrawable(ResourcesCompat.getDrawable(
                            context.getResources(), R.drawable.overflow_open, null));
                    PopupMenu popupMenu = new PopupMenu(context, menuButton);
                    popupMenu.inflate(R.menu.file_menu);
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
                                    context.getResources(), R.drawable.overflow_normal, null));
                        }
                    });
                    popupMenu.show();
                }
            });
        } else if (content.get(position).url == null || (content.get(position).url.length() == 0)) {
            if (content.get(position).imageUrl != null) {
                icon.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_work, null));
            } else {
                icon.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_folder, null));
            }
        } else {
            if (content.get(position).type == 1 || content.get(position).type == 3) {
                icon.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_link, null));
            } else if (content.get(position).type == 10) {
                icon.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), getDrawableByName(content.get(position).url), null));
            } else {
                icon.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_unknown, null));
            }
        }
    }

    private void showPromptDialog(final int position) {
        ApplicationLoader.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(context)
                        .setTitle(context.getResources().getString(R.string.confirmation))
                        .setMessage(context.getResources().getString(R.string.delete_file_description))
                        .setPositiveButton(context.getResources().getString(R.string.delete),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        File file = new File(Environment.getExternalStorageDirectory().toString(), "MijnWindesheim/" + content.get(position).name);
                                        if (file.exists()) {
                                            file.delete();
                                        }
                                        content.remove(position);
                                        notifyItemRemoved(position);
                                        if (content.size() == 0) {
                                            DownloadsActivity.showEmptyTextview();
                                        }
                                        Snackbar snackbar = Snackbar.make(context.findViewById(R.id.coordinator_layout), context.getResources().getString(R.string.file_deleted), Snackbar.LENGTH_SHORT);
                                        snackbar.show();
                                        dialog.cancel();
                                    }
                                })
                        .setNegativeButton(context.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        }).show();
            }
        });
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
            } else if (name.contains(".zip") || name.contains(".rar") || name.contains(".ai") || name.contains(".mp3") || name.contains(".mov") || name.contains(".avi")) {
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

    public class ViewHolder extends RecyclerView.ViewHolder {

        final TextView contentName;
        final ImageView icon;
        final FrameLayout menuButton;
        final ImageView menuButtonImage;

        public ViewHolder(View view) {
            super(view);
            contentName = (TextView) view.findViewById(R.id.content_name);
            icon = (ImageView) view.findViewById(R.id.content_icon);
            menuButton = (FrameLayout) view.findViewById(R.id.menu_button);
            menuButtonImage = (ImageView) view.findViewById(R.id.menu_button_image);
        }
    }
}