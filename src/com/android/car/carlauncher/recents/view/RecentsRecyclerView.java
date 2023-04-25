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

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.WindowMetrics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.carlauncher.R;
import com.android.car.carlauncher.recents.RecentTasksViewModel;
import com.android.internal.annotations.VisibleForTesting;

/**
 * RecyclerView that centers the first and last elements of the Recent task list by adding
 * appropriate padding.
 */
public class RecentsRecyclerView extends RecyclerView {
    private final int mFirstItemWidth;
    private final int mColSpacing;
    private final int mItemWidth;
    private final int mColsPerPage;
    private RecentTasksViewModel mRecentTasksViewModel;
    private WindowMetrics mWindowMetrics;

    public RecentsRecyclerView(@NonNull Context context) {
        this(context, /* attrs= */ null);
    }

    public RecentsRecyclerView(@NonNull Context context,
            @Nullable AttributeSet attrs) {
        this(context, attrs, androidx.recyclerview.R.attr.recyclerViewStyle);
    }


    public RecentsRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mRecentTasksViewModel = RecentTasksViewModel.getInstance();
        if (context instanceof Activity) {
            // needed for testing when using a mock context
            mWindowMetrics = ((Activity) context).getWindowManager().getCurrentWindowMetrics();
        }
        mFirstItemWidth = getResources().getDimensionPixelSize(R.dimen.recent_task_width_first);
        mItemWidth = getResources().getDimensionPixelSize(R.dimen.recent_task_width);
        mColSpacing = getResources().getDimensionPixelSize(R.dimen.recent_task_col_space);
        mColsPerPage = getResources().getInteger(R.integer.config_recents_columns_per_page);
    }

    @VisibleForTesting
    public RecentsRecyclerView(@NonNull Context context, RecentTasksViewModel recentTasksViewModel,
            WindowMetrics windowMetrics) {
        this(context);
        mRecentTasksViewModel = recentTasksViewModel;
        mWindowMetrics = windowMetrics;
    }

    /**
     * Resets the RecyclerView's start and end padding based on the Task list size,
     * recent task view width and window width where Recents activity is drawn.
     */
    public void resetPadding() {
        if (mRecentTasksViewModel.getRecentTasksSize() == 0) {
            setPadding(/* firstItemPadding= */ 0, /* lastItemPadding= */ 0);
            return;
        }
        int firstItemPadding, lastItemPadding;
        firstItemPadding = calculateFirstItemPadding(mWindowMetrics.getBounds().width());
        if (mRecentTasksViewModel.getRecentTasksSize() == 1) {
            // only one element is left, center it by adding equal padding
            lastItemPadding = firstItemPadding;
        } else {
            lastItemPadding = calculateLastItemPadding(mWindowMetrics.getBounds().width());
        }
        setPadding(/* firstItemPadding= */ firstItemPadding,
                /* lastItemPadding= */ lastItemPadding);
    }

    @Px
    @VisibleForTesting
    int calculateFirstItemPadding(@Px int windowWidth) {
        // This assumes that RecyclerView's width is same as the windowWidth. This is to add padding
        // before RecyclerView or its children is drawn.
        return Math.max(0, (windowWidth - (mFirstItemWidth + mColSpacing)) / 2);
    }

    @Px
    @VisibleForTesting
    int calculateLastItemPadding(@Px int windowWidth) {
        // This assumes that RecyclerView's width is same as the windowWidth. This is to add padding
        // before RecyclerView or its children is drawn.
        return Math.max(0, (windowWidth - (mColsPerPage * (mItemWidth + mColSpacing))) / 2);
    }


    /**
     * @param firstItemPadding padding set to recyclerView to fit the first item.
     * @param lastItemPadding  padding set to recyclerView to fit the last item.
     */
    private void setPadding(@Px int firstItemPadding, @Px int lastItemPadding) {
        boolean isReverseLayout = false;
        if (getLayoutManager() instanceof LinearLayoutManager) {
            LinearLayoutManager lm = (LinearLayoutManager) getLayoutManager();
            isReverseLayout = lm.getReverseLayout();
        }
        boolean shouldBeReversed = isLayoutRtl() ^ isReverseLayout;
        setPaddingRelative(
                /* start= */ shouldBeReversed ? lastItemPadding : firstItemPadding,
                getPaddingTop(),
                /* end= */ shouldBeReversed ? firstItemPadding : lastItemPadding,
                getPaddingBottom());
    }
}
