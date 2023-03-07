/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.car.carlauncher;

import static com.android.car.carlauncher.AppGridConstants.PageOrientation;
import static com.android.car.carlauncher.AppGridConstants.isHorizontal;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout.LayoutParams;

import androidx.core.content.ContextCompat;

/**
 * A scrollbar like view that dynamically adjusts its offset and size to indicate to users which
 * page they are on when scrolling through the app grid.
 */
public class AppGridPositionIndicator extends View {
    final long mAppearAnimationDurationMs;
    final long mAppearAnimationDelayMs;
    final long mFadeAnimationDurationMs;
    final long mFadeAnimationDelayMs;

    private int mAppGridWidth;
    private int mAppGridHeight;
    private int mAppGridOffset;
    private int mPageCount = 1;

    @PageOrientation
    private int mAppGridOrientation;
    private float mOffsetScaleFactor = 1.f;

    public AppGridPositionIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        mFadeAnimationDurationMs = getResources().getInteger(
                R.integer.ms_scrollbar_fade_animation_duration);
        mFadeAnimationDelayMs = getResources().getInteger(
                R.integer.ms_scrollbar_fade_animation_delay);
        mAppearAnimationDurationMs = getResources().getInteger(
                R.integer.ms_scrollbar_appear_animation_duration);
        mAppearAnimationDelayMs = getResources().getInteger(
                R.integer.ms_scrollbar_appear_animation_delay);
        mAppGridOrientation = getResources().getBoolean(R.bool.use_vertical_app_grid)
                ? PageOrientation.VERTICAL : PageOrientation.HORIZONTAL;
        setBackground(ContextCompat.getDrawable(context, R.drawable.position_indicator_bar));
        setLayoutParams(new LayoutParams(/* width */ mAppGridWidth,
                /* height */ LayoutParams.MATCH_PARENT));
        setAlpha(0.f);
    }

    void updateAppGridDimensions(int windowWidth, int windowHeight, int appGridWidth,
            int appGridHeight) {
        mAppGridWidth = appGridWidth;
        mAppGridHeight = appGridHeight;
        mOffsetScaleFactor = isHorizontal(mAppGridOrientation)
                ? (float) mAppGridWidth / windowWidth : (float) mAppGridHeight / windowHeight;
        updatePageCount(mPageCount);
    }

    /**
     * Updates the dimensions of the scroll bar when number of pages in app grid changes.
     */
    void updatePageCount(int pageCount) {
        mPageCount = pageCount;
        LayoutParams params = (LayoutParams) getLayoutParams();
        if (isHorizontal(mAppGridOrientation)) {
            params.width = mAppGridWidth / mPageCount;
        } else {
            params.height = mAppGridHeight / mPageCount;
        }
        setLayoutParams(params);
        updateOffset(mAppGridOffset);
    }

    /**
     * Updates the offset when recyclerview has been scrolled to xOffset.
     */
    void updateOffset(int appGridOffset) {
        mAppGridOffset = appGridOffset;
        LayoutParams params = (LayoutParams) getLayoutParams();
        int offset = (int) (mAppGridOffset * mOffsetScaleFactor / mPageCount);
        if (isHorizontal(mAppGridOrientation)) {
            params.leftMargin = offset;
        } else {
            params.topMargin = offset;
        }
        setLayoutParams(params);
    }

    void animateAppearance() {
        animate().alpha(1.f)
                 .setDuration(mAppearAnimationDurationMs)
                 .setStartDelay(mAppearAnimationDelayMs);
    }

    void animateFading() {
        animate().alpha(0.f)
                 .setDuration(mFadeAnimationDelayMs)
                 .setStartDelay(mFadeAnimationDelayMs);
    }
}
