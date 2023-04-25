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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Rect;
import android.testing.TestableContext;
import android.util.LayoutDirection;
import android.view.WindowMetrics;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.car.carlauncher.R;
import com.android.car.carlauncher.recents.RecentTasksViewModel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class RecentsRecyclerViewTest {
    private static final int WINDOW_WIDTH = 1000;
    private static final int FIRST_TASK_WIDTH = 800;
    private static final int TASK_WIDTH = 300;
    private static final int COL_SPACING_BETWEEN_TASKS = 50;
    private static final int COL_PER_PAGE = 2;
    private RecentsRecyclerView mRecentsRecyclerView;

    @Mock
    private RecentTasksViewModel mRecentTasksViewModel;
    @Mock
    private WindowMetrics mWindowMetrics;
    @Mock
    private GridLayoutManager mGridLayoutManager;

    @Rule
    public final TestableContext mContext = new TestableContext(
            InstrumentationRegistry.getInstrumentation().getTargetContext()) {
        @Override
        public Context createApplicationContext(ApplicationInfo application, int flags) {
            return this;
        }
    };

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        Rect windowBounds = new Rect(/* left= */ 0, /* top= */ 0,
                /* right= */ WINDOW_WIDTH, /* bottom= */ 0);
        when(mWindowMetrics.getBounds()).thenReturn(windowBounds);
        mContext.getOrCreateTestableResources().addOverride(R.dimen.recent_task_width_first,
                /* value= */ FIRST_TASK_WIDTH);
        mContext.getOrCreateTestableResources().addOverride(R.dimen.recent_task_width,
                /* value= */ TASK_WIDTH);
        mContext.getOrCreateTestableResources().addOverride(R.dimen.recent_task_col_space,
                /* value= */ COL_SPACING_BETWEEN_TASKS);
        mContext.getOrCreateTestableResources().addOverride(
                R.integer.config_recents_columns_per_page, /* value= */ COL_PER_PAGE);

        mRecentsRecyclerView = new RecentsRecyclerView(mContext, mRecentTasksViewModel,
                mWindowMetrics);
    }

    @Test
    public void resetPadding_setsStartPadding_toZero_whenNoRecentTasksPresent() {
        when(mRecentTasksViewModel.getRecentTasksSize()).thenReturn(0);

        mRecentsRecyclerView.resetPadding();

        assertThat(mRecentsRecyclerView.getPaddingStart()).isEqualTo(0);
    }


    @Test
    public void resetPadding_setsEndPadding_toZero_whenNoRecentTasksPresent() {
        when(mRecentTasksViewModel.getRecentTasksSize()).thenReturn(0);

        mRecentsRecyclerView.resetPadding();

        assertThat(mRecentsRecyclerView.getPaddingEnd()).isEqualTo(0);
    }

    @Test
    public void resetPadding_setsTheSameStartAndEndPadding_whenOneRecentTaskPresent() {
        when(mRecentTasksViewModel.getRecentTasksSize()).thenReturn(1);

        mRecentsRecyclerView.resetPadding();

        assertThat(mRecentsRecyclerView.getPaddingStart())
                .isEqualTo(mRecentsRecyclerView.getPaddingEnd());
    }

    @Test
    public void resetPadding_setsStartPadding_toStart_noReverseLayout_noRTL() {
        mRecentsRecyclerView.setLayoutDirection(LayoutDirection.LTR);
        mRecentsRecyclerView.setLayoutManager(mGridLayoutManager);
        when(mGridLayoutManager.getReverseLayout()).thenReturn(false);
        when(mRecentTasksViewModel.getRecentTasksSize()).thenReturn(5);

        int startPadding = mRecentsRecyclerView.calculateFirstItemPadding(WINDOW_WIDTH);
        mRecentsRecyclerView.resetPadding();

        assertThat(mRecentsRecyclerView.getPaddingStart()).isEqualTo(startPadding);
    }

    @Test
    public void resetPadding_setsEndPadding_toEnd_noReverseLayout_noRTL() {
        mRecentsRecyclerView.setLayoutDirection(LayoutDirection.LTR);
        mRecentsRecyclerView.setLayoutManager(mGridLayoutManager);
        when(mGridLayoutManager.getReverseLayout()).thenReturn(false);
        when(mRecentTasksViewModel.getRecentTasksSize()).thenReturn(5);

        int endPadding = mRecentsRecyclerView.calculateLastItemPadding(WINDOW_WIDTH);
        mRecentsRecyclerView.resetPadding();

        assertThat(mRecentsRecyclerView.getPaddingEnd()).isEqualTo(endPadding);
    }

    @Test
    public void resetPadding_setsStartPadding_toEnd_noReverseLayout_RTL() {
        mRecentsRecyclerView.setLayoutDirection(LayoutDirection.RTL);
        mRecentsRecyclerView.setLayoutManager(mGridLayoutManager);
        when(mGridLayoutManager.getReverseLayout()).thenReturn(false);
        when(mRecentTasksViewModel.getRecentTasksSize()).thenReturn(5);

        int startPadding = mRecentsRecyclerView.calculateFirstItemPadding(WINDOW_WIDTH);
        mRecentsRecyclerView.resetPadding();

        assertThat(mRecentsRecyclerView.getPaddingEnd()).isEqualTo(startPadding);
    }

    @Test
    public void resetPadding_setsEndPadding_toStart_noReverseLayout_RTL() {
        mRecentsRecyclerView.setLayoutDirection(LayoutDirection.RTL);
        mRecentsRecyclerView.setLayoutManager(mGridLayoutManager);
        when(mGridLayoutManager.getReverseLayout()).thenReturn(false);
        when(mRecentTasksViewModel.getRecentTasksSize()).thenReturn(5);

        int endPadding = mRecentsRecyclerView.calculateLastItemPadding(WINDOW_WIDTH);
        mRecentsRecyclerView.resetPadding();

        assertThat(mRecentsRecyclerView.getPaddingStart()).isEqualTo(endPadding);
    }

    @Test
    public void resetPadding_setsStartPadding_toEnd_reverseLayout_noRTL() {
        mRecentsRecyclerView.setLayoutDirection(LayoutDirection.LTR);
        mRecentsRecyclerView.setLayoutManager(mGridLayoutManager);
        when(mGridLayoutManager.getReverseLayout()).thenReturn(true);
        when(mRecentTasksViewModel.getRecentTasksSize()).thenReturn(5);

        int startPadding = mRecentsRecyclerView.calculateFirstItemPadding(WINDOW_WIDTH);
        mRecentsRecyclerView.resetPadding();

        assertThat(mRecentsRecyclerView.getPaddingEnd()).isEqualTo(startPadding);
    }

    @Test
    public void resetPadding_setsEndPadding_toStart_reverseLayout_noRTL() {
        mRecentsRecyclerView.setLayoutDirection(LayoutDirection.LTR);
        mRecentsRecyclerView.setLayoutManager(mGridLayoutManager);
        when(mGridLayoutManager.getReverseLayout()).thenReturn(true);
        when(mRecentTasksViewModel.getRecentTasksSize()).thenReturn(5);

        int endPadding = mRecentsRecyclerView.calculateLastItemPadding(WINDOW_WIDTH);
        mRecentsRecyclerView.resetPadding();

        assertThat(mRecentsRecyclerView.getPaddingStart()).isEqualTo(endPadding);
    }

    @Test
    public void resetPadding_setsStartPadding_toStart_reverseLayout_RTL() {
        mRecentsRecyclerView.setLayoutDirection(LayoutDirection.LTR);
        mRecentsRecyclerView.setLayoutManager(mGridLayoutManager);
        when(mGridLayoutManager.getReverseLayout()).thenReturn(false);
        when(mRecentTasksViewModel.getRecentTasksSize()).thenReturn(5);

        int startPadding = mRecentsRecyclerView.calculateFirstItemPadding(WINDOW_WIDTH);
        mRecentsRecyclerView.resetPadding();

        assertThat(mRecentsRecyclerView.getPaddingStart()).isEqualTo(startPadding);
    }

    @Test
    public void resetPadding_setsEndPadding_toEnd_reverseLayout_RTL() {
        mRecentsRecyclerView.setLayoutDirection(LayoutDirection.LTR);
        mRecentsRecyclerView.setLayoutManager(mGridLayoutManager);
        when(mGridLayoutManager.getReverseLayout()).thenReturn(false);
        when(mRecentTasksViewModel.getRecentTasksSize()).thenReturn(5);

        int endPadding = mRecentsRecyclerView.calculateLastItemPadding(WINDOW_WIDTH);
        mRecentsRecyclerView.resetPadding();

        assertThat(mRecentsRecyclerView.getPaddingEnd()).isEqualTo(endPadding);
    }
}
