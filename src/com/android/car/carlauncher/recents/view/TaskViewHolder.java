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

import com.android.car.carlauncher.R;

/**
 * Builds onto BaseViewHolder by adding functionality of disabled state and adding click and touch
 * listeners.
 */
public class TaskViewHolder extends BaseViewHolder {
    private final ImageView mThumbnailImageView;
    private final ImageView mIconImageView;
    private final float mDisabledStateAlpha;

    public TaskViewHolder(@NonNull View itemView) {
        super(itemView);
        mThumbnailImageView = itemView.findViewById(R.id.task_thumbnail);
        mIconImageView = itemView.findViewById(R.id.task_icon);
        mDisabledStateAlpha = itemView.getResources().getFloat(
                R.dimen.disabled_recent_task_alpha);
    }

    public void bind(@Nullable Drawable icon, @Nullable Bitmap thumbnail, boolean isDisabled,
            @Nullable View.OnClickListener onClickListener,
            @Nullable View.OnTouchListener taskTouchListener) {
        super.bind(icon, thumbnail);

        setDisabledAlpha(mThumbnailImageView, isDisabled);
        setDisabledAlpha(mIconImageView, isDisabled);

        setListeners(mThumbnailImageView, taskTouchListener, onClickListener);
        setListeners(mIconImageView, taskTouchListener, onClickListener);
    }

    private void setListeners(View view, View.OnTouchListener onTouchListener,
            View.OnClickListener onClickListener) {
        view.setOnTouchListener(onTouchListener);
        view.setOnClickListener(onClickListener);
    }

    private void setDisabledAlpha(View view, boolean isDisabled) {
        if (isDisabled) {
            view.setAlpha(mDisabledStateAlpha);
        } else {
            view.setAlpha(1f);
        }
    }
}