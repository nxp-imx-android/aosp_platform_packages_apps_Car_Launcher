/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.car.carlauncher.recents.view;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.carlauncher.R;
import com.android.car.carlauncher.recents.RecentTasksViewModel;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RecentTasksAdapter extends RecyclerView.Adapter<TaskViewHolder> implements
        RecentTasksViewModel.RecentTasksChangeListener {
    private static final byte THUMBNAIL_UPDATED = 0x1; // 00000001
    private static final byte ICON_UPDATED = 0x2; // 00000010
    private final RecentTasksViewModel mRecentTasksViewModel;
    private final LayoutInflater mLayoutInflater;
    private final ItemTouchHelper mItemTouchHelper;

    public RecentTasksAdapter(LayoutInflater layoutInflater, ItemTouchHelper itemTouchHelper) {
        mRecentTasksViewModel = RecentTasksViewModel.getInstance();
        mRecentTasksViewModel.addRecentTasksChangeListener(this);
        mLayoutInflater = layoutInflater;
        mItemTouchHelper = itemTouchHelper;
    }

    @NotNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TaskViewHolder(mLayoutInflater.inflate(R.layout.recent_task_view, parent,
                        /* attachToRoot= */ false), mItemTouchHelper);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Drawable taskIcon = mRecentTasksViewModel.getRecentTaskIconAt(position);
        Bitmap taskThumbnail = mRecentTasksViewModel.getRecentTaskThumbnailAt(position);
        boolean isDisabled = mRecentTasksViewModel.isRecentTaskDisabled(position);
        View.OnClickListener onClickListener =
                isDisabled ? mRecentTasksViewModel.getDisabledTaskClickListener(position)
                        : new TaskClickListener(position);
        holder.bind(taskIcon, taskThumbnail, isDisabled, onClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position,
            @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
            return;
        }
        payloads.forEach(payload -> {
            if (payload instanceof Byte updateType) {
                if ((updateType & THUMBNAIL_UPDATED) > 0) {
                    Bitmap taskThumbnail = mRecentTasksViewModel.getRecentTaskThumbnailAt(position);
                    holder.updateThumbnail(taskThumbnail);
                }
                if ((updateType & ICON_UPDATED) > 0) {
                    Drawable taskIcon = mRecentTasksViewModel.getRecentTaskIconAt(position);
                    holder.updateIcon(taskIcon);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mRecentTasksViewModel.getRecentTasksSize();
    }

    @Override
    public void onRecentTasksFetched() {
        this.notifyDataSetChanged();
    }

    @Override
    public void onTaskThumbnailChange(int position) {
        this.notifyItemChanged(position, THUMBNAIL_UPDATED);
    }

    @Override
    public void onTaskIconChange(int position) {
        this.notifyItemChanged(position, ICON_UPDATED);
    }

    @Override
    public void onAllRecentTasksRemoved(int countRemoved) {
        this.notifyItemRangeRemoved(0, countRemoved);
    }

    @Override
    public void onRecentTaskRemoved(int position) {
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, getItemCount() - position);
    }
}

