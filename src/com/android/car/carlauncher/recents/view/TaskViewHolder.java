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
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.carlauncher.R;

public class TaskViewHolder extends RecyclerView.ViewHolder {
    private final ImageView mThumbnailImageView;
    private final ImageView mIconImageView;
    private final View.OnTouchListener mTaskTouchListener;

    public TaskViewHolder(@NonNull View itemView, ItemTouchHelper itemTouchHelper,
            float startSwipeThreshold) {
        super(itemView);
        mThumbnailImageView = itemView.findViewById(R.id.task_thumbnail);
        mIconImageView = itemView.findViewById(R.id.task_icon);
        mTaskTouchListener = new TaskTouchListener(startSwipeThreshold,
                itemTouchHelper, /* viewHolder= */ this);
    }

    public void bind(@Nullable Drawable icon, @Nullable Bitmap thumbnail,
            @Nullable View.OnClickListener onClickListener) {
        updateThumbnail(thumbnail);
        updateIcon(icon);

        setListeners(mThumbnailImageView, mTaskTouchListener, onClickListener);
        setListeners(mIconImageView, mTaskTouchListener, onClickListener);
    }

    public void updateThumbnail(@Nullable Bitmap thumbnail) {
        if (thumbnail == null) {
            mThumbnailImageView.setImageDrawable(null);
            return;
        }
        mThumbnailImageView.setImageBitmap(thumbnail);
    }

    public void updateIcon(@Nullable Drawable icon) {
        if (icon == null) {
            mIconImageView.setImageDrawable(null);
            return;
        }
        mIconImageView.setImageDrawable(icon);
    }

    private void setListeners(View view, View.OnTouchListener onTouchListener,
            View.OnClickListener onClickListener) {
        view.setOnTouchListener(onTouchListener);
        view.setOnClickListener(onClickListener);
    }
}