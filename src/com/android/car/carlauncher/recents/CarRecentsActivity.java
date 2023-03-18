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

package com.android.car.carlauncher.recents;

import static android.widget.Toast.LENGTH_SHORT;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowMetrics;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.carlauncher.R;
import com.android.car.carlauncher.recents.view.RecentTasksAdapter;
import com.android.car.carlauncher.recents.view.TaskTouchHelperCallback;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CarRecentsActivity extends AppCompatActivity implements
        RecentTasksViewModel.RecentTasksChangeListener {
    public static final String OPEN_RECENT_TASK_ACTION =
            "com.android.car.carlauncher.recents.OPEN_RECENT_TASK_ACTION";
    private RecyclerView mRecentsRecyclerView;
    private GridLayoutManager mGridLayoutManager;
    private RecentTasksViewModel mRecentTasksViewModel;
    private Group mRecentTasksGroup;
    private View mEmptyStateView;
    private Animator mClearAllAnimator;
    private NonDOHiddenPackageProvider mNonDOHiddenPackageProvider;
    private Set<String> mPackagesToHideFromRecents;
    private int mRecentTaskRowSpacing;
    private int mRecentTaskColSpacing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recents_activity);
        mRecentsRecyclerView = findViewById(R.id.recent_tasks_list);
        mRecentTasksGroup = findViewById(R.id.recent_tasks_group);
        mEmptyStateView = findViewById(R.id.empty_state);
        mRecentTaskRowSpacing = getResources().getDimensionPixelSize(R.dimen.recent_task_row_space);
        mRecentTaskColSpacing = getResources().getDimensionPixelSize(R.dimen.recent_task_col_space);
        mPackagesToHideFromRecents = new HashSet<>(List.of(getResources().getStringArray(
                R.array.packages_hidden_from_recents)));
        mRecentTasksViewModel = RecentTasksViewModel.getInstance();
        mRecentTasksViewModel.addRecentTasksChangeListener(this);
        mRecentTasksViewModel.addHiddenTaskProvider(
                (packageName, className) -> mPackagesToHideFromRecents.contains(packageName));
        mNonDOHiddenPackageProvider = new NonDOHiddenPackageProvider(this);
        mRecentTasksViewModel.setDisabledTaskProvider(mNonDOHiddenPackageProvider);
        WindowMetrics windowMetrics = this.getWindowManager().getCurrentWindowMetrics();
        mRecentTasksViewModel.init(
                /* displayId= */ getDisplay().getDisplayId(),
                /* windowWidth= */ windowMetrics.getBounds().width(),
                /* windowHeight= */ windowMetrics.getBounds().height(),
                /* windowInsets= */ windowMetrics.getWindowInsets()
                        .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()).toRect(),
                /* defaultThumbnailColor= */
                getResources().getColor(R.color.default_recents_thumbnail_color, /* theme= */null));

        if (!(mRecentsRecyclerView.getLayoutManager() instanceof GridLayoutManager)) {
            throw new UnsupportedOperationException(
                    "Only classes that inherit GridLayoutManager are supported");
        }
        mGridLayoutManager = (GridLayoutManager) mRecentsRecyclerView.getLayoutManager();
        int gridSpanCount = mGridLayoutManager.getSpanCount();
        mGridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position == 0) {
                    return gridSpanCount;
                }
                return 1;
            }
        });

        mRecentsRecyclerView.addItemDecoration(getRecentTasksItemDecoration());
        float swipedThreshold = getResources().getFloat(R.dimen.recent_task_swiped_threshold);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(
                new TaskTouchHelperCallback(
                        /* dragDirs= */ 0, ItemTouchHelper.UP, swipedThreshold));
        itemTouchHelper.attachToRecyclerView(mRecentsRecyclerView);
        mRecentsRecyclerView.setAdapter(
                new RecentTasksAdapter(getLayoutInflater(), itemTouchHelper));

        mClearAllAnimator = AnimatorInflater.loadAnimator(this,
                R.animator.recents_clear_all);
        mClearAllAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mRecentTasksViewModel.removeAllRecentTasks();
                launchHomeIntent();
            }
        });
        mClearAllAnimator.setTarget(mRecentsRecyclerView);
        View clearAllButton = findViewById(R.id.clear_all_button);
        clearAllButton.setOnClickListener(v -> mClearAllAnimator.start());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (OPEN_RECENT_TASK_ACTION.equals(getIntent().getAction())) {
            mRecentTasksViewModel.openMostRecentTask();
            return;
        }
        mRecentTasksViewModel.fetchRecentTaskList();
        resetViewState();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mRecentTasksViewModel.clearCache();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mNonDOHiddenPackageProvider.terminate();
        mRecentTasksViewModel.terminate();
        mClearAllAnimator.end();
        mClearAllAnimator.removeAllListeners();
    }

    @Override
    public void onRecentTasksFetched() {
        resetViewState();
    }

    @Override
    public void onOpenRecentTaskFail() {
        // notify the user when there is a failure to open a task from recents
        Toast.makeText(this, R.string.failure_opening_recent_task_message, LENGTH_SHORT).show();
    }

    @Override
    public void onOpenTopRunningTaskFail() {
        launchHomeIntent();
    }

    @Override
    public void onRecentTaskRemoved(int position) {
        if (mRecentTasksViewModel.getRecentTasksSize() == 0) {
            launchHomeIntent();
        }
    }


    private RecyclerView.ItemDecoration getRecentTasksItemDecoration() {
        return new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                    @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                super.getItemOffsets(outRect, view, parent, state);
                outRect.left = mRecentTaskColSpacing;
                int position = parent.getChildAdapterPosition(view);
                if (position % 2 == 0) {
                    outRect.top = mRecentTaskRowSpacing;
                }
            }
        };
    }

    private void launchHomeIntent() {
        Intent homeActivityIntent = new Intent(Intent.ACTION_MAIN);
        homeActivityIntent.addCategory(Intent.CATEGORY_HOME);
        homeActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeActivityIntent);
    }

    private void resetViewState() {
        boolean isRecentTaskListEmpty = mRecentTasksViewModel.getRecentTasksSize() == 0;
        if (!isRecentTaskListEmpty) {
            mRecentsRecyclerView.setAlpha(1f);
            mGridLayoutManager.scrollToPositionWithOffset(/* position= */ 0, /* offset= */ 0);
        }
        mRecentTasksGroup.setVisibility(isRecentTaskListEmpty ? View.GONE : View.VISIBLE);
        mEmptyStateView.setVisibility(isRecentTaskListEmpty ? View.VISIBLE : View.GONE);
    }
}
