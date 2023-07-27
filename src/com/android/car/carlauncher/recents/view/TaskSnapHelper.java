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

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.LayoutManager;
import androidx.recyclerview.widget.SnapHelper;

import com.android.car.carlauncher.recents.RecentsUtils;

/**
 * Snaps the first item or a page of items to the center of the RecyclerView.
 */
public class TaskSnapHelper extends SnapHelper {
    private static final int INVALID_MEASURE = Integer.MAX_VALUE;
    private final int mSpanCount;
    private final int mColPerPage;
    private final int mItemsInPageCount;
    private final int mItemWidth;
    private final int mColumnSpacing;
    private final int mSnapTolerance;
    private RecyclerView mRecyclerView;
    // keeps track if the item used for snapping is last item. If false, it is the first item.
    private boolean mIsLastItem = false;

    /**
     * @param spanCount     span set in the recyclerview grid layout (number of rows).
     * @param colPerPage    number of columns to be snapped together to form a page.
     * @param itemWidth     fixed width of an items in the recyclerview (not the first item width).
     * @param columnSpacing spacing between two items/columns in the recyclerView.
     * @param snapTolerance distance by/under which if center of a page is moved won't trigger a
     *                      snap.
     */
    public TaskSnapHelper(int spanCount, int colPerPage, int itemWidth, int columnSpacing,
            int snapTolerance) {
        mSpanCount = spanCount;
        mColPerPage = colPerPage;
        mItemsInPageCount = mColPerPage * mSpanCount;
        mItemWidth = itemWidth;
        mColumnSpacing = columnSpacing;
        mSnapTolerance = snapTolerance;
    }

    @Nullable
    @Override
    public int[] calculateDistanceToFinalSnap(@NonNull LayoutManager layoutManager,
            @NonNull View targetView) {
        if (mRecyclerView == null) {
            return new int[]{0, 0};
        }
        int adapterPosition = mRecyclerView.getChildAdapterPosition(targetView);
        int childCenter = INVALID_MEASURE;
        if (adapterPosition == 0) {
            childCenter = findCenterOfView(targetView);
        } else {
            int targetPosition = getChildPosition(adapterPosition, layoutManager, mRecyclerView);
            if (targetPosition == RecyclerView.NO_POSITION) {
                return new int[]{0, 0};
            }
            int firstChildIndexInPage = mIsLastItem
                    ? getFirstChildIndexFromLastChild(targetPosition, mItemsInPageCount)
                    : targetPosition;
            childCenter = findCenterOfPage(firstChildIndexInPage);
        }
        if (childCenter == INVALID_MEASURE) {
            return new int[]{0, 0};
        }
        int center = layoutManager.getWidth() / 2;

        return new int[]{childCenter - center, 0};
    }

    /**
     * @return View at {@code 0} adapter position if that view is to be snapped else returns 1st
     * view in the page that is to be snapped
     */
    @Nullable
    @Override
    public View findSnapView(LayoutManager layoutManager) {
        int childCount = layoutManager.getChildCount();
        if (childCount == 0 || mRecyclerView == null) {
            return null;
        }
        int center = layoutManager.getWidth() / 2;
        View closestView = null;
        int closestDistance = INVALID_MEASURE;
        int i = 0;
        while (i < childCount) {
            View child = layoutManager.getChildAt(i);
            if (child == null) {
                i++;
                continue;
            }
            int adapterPosition = mRecyclerView.getChildAdapterPosition(child);
            if (adapterPosition == RecyclerView.NO_POSITION) {
                i++;
                continue;
            }
            int childCenter;
            boolean isLastItem = false;
            if (adapterPosition == 0) {
                childCenter = findCenterOfView(child);
                i++;
            } else {
                // colNum represents the column that the item appears in. colNum starts from 0 for
                // the 0th item, 1 for 1-mSpanCount items
                int colNum = (int) Math.ceil(adapterPosition / (float) mSpanCount);
                boolean isInFirstColOfPage = colNum % mColPerPage == 1;
                boolean isInFirstRow = adapterPosition % mSpanCount == 1;
                boolean isInLastColOfPage = colNum % mColPerPage == 0;
                boolean isInLastRow = adapterPosition % mSpanCount == 0;

                // only look at the first/last item of the page to calculate its center and skip
                // the other items since this can already give us center of the page.
                if (isInFirstColOfPage && isInFirstRow) {
                    // child is the first item of its page
                    childCenter = findCenterOfPage(i);
                    i += mItemsInPageCount;
                } else if (isInLastColOfPage && isInLastRow) {
                    // child is the last item of its page
                    int firstChildIndexInPage = getFirstChildIndexFromLastChild(i,
                            mItemsInPageCount);
                    childCenter = findCenterOfPage(firstChildIndexInPage);
                    isLastItem = true;
                    i++;
                } else {
                    // child is neither first nor last item of its page
                    i++;
                    continue;
                }
            }
            if (childCenter == INVALID_MEASURE) {
                continue;
            }
            int distanceToCenter = Math.abs(center - childCenter);
            if (distanceToCenter < closestDistance) {
                closestView = child;
                closestDistance = distanceToCenter;
                mIsLastItem = isLastItem;
            }
        }
        if (closestDistance <= mSnapTolerance) {
            // already reached to the closest page given the tolerance, no snapping required.
            return null;
        }
        return closestView;
    }

    @Override
    public int findTargetSnapPosition(LayoutManager layoutManager, int velocityX, int velocityY) {
        return RecyclerView.NO_POSITION;
    }

    @Override
    public void attachToRecyclerView(@Nullable RecyclerView recyclerView)
            throws IllegalStateException {
        super.attachToRecyclerView(recyclerView);
        if (mRecyclerView == recyclerView) {
            return;
        }
        if (mRecyclerView != null) {
            mRecyclerView.setOnFlingListener(null);
        }
        mRecyclerView = recyclerView;
        if (mRecyclerView == null) {
            return;
        }
        mRecyclerView.setOnFlingListener(new RecyclerView.OnFlingListener() {
            @Override
            public boolean onFling(int velocityX, int velocityY) {
                if (mRecyclerView == null) {
                    return false;
                }
                RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
                if (layoutManager == null) {
                    return false;
                }
                calculateDistanceToFinalSnapAndScroll(layoutManager, mRecyclerView,
                        findSnapView(layoutManager));
                return true;
            }
        });
    }

    private void calculateDistanceToFinalSnapAndScroll(@NonNull LayoutManager layoutManager,
            @NonNull RecyclerView recyclerView, @Nullable View targetView) {
        if (targetView == null) {
            return;
        }
        int[] snapDistance = calculateDistanceToFinalSnap(layoutManager, targetView);
        if (snapDistance == null || (snapDistance[0] == 0 && snapDistance[1] == 0)) {
            return;
        }
        recyclerView.smoothScrollBy(snapDistance[0], snapDistance[1]);
    }

    private int getChildPosition(int adapterPosition, @NonNull LayoutManager layoutManager,
            @NonNull RecyclerView recyclerView) {
        if (adapterPosition == RecyclerView.NO_POSITION) return RecyclerView.NO_POSITION;
        for (int i = 0; i < layoutManager.getChildCount(); i++) {
            View child = layoutManager.getChildAt(i);
            if (child != null && adapterPosition == recyclerView.getChildAdapterPosition(child)) {
                return i;
            }
        }
        return RecyclerView.NO_POSITION;
    }

    private int findCenterOfView(@NonNull View child) {
        int left = child.getLeft();
        int right = child.getRight();
        return (left + right) / 2;
    }

    private int findCenterOfPage(int firstChildInPageIndex) {
        if (mRecyclerView == null || mRecyclerView.getLayoutManager() == null) {
            return INVALID_MEASURE;
        }
        return findCenterOfPage(firstChildInPageIndex, mColPerPage, mItemsInPageCount,
                mItemWidth, mColumnSpacing,
                RecentsUtils.areItemsRightToLeft(mRecyclerView), mRecyclerView.getLayoutManager());
    }

    private int findCenterOfPage(int firstChildInPageIndex, int colPerPage, int itemsInPageCount,
            int itemWidth, int columnSpacing, boolean isLayoutReversed,
            @NonNull LayoutManager layoutManager) {
        View firstChild = layoutManager.getChildAt(firstChildInPageIndex);
        View lastChild = layoutManager.getChildAt(
                getLastChildIndexFromFirstChild(firstChildInPageIndex, itemsInPageCount));
        if (firstChild == null && lastChild == null) {
            return INVALID_MEASURE;
        }
        int pageWidth = getPageWidth(itemWidth, columnSpacing, colPerPage);
        if (lastChild != null && firstChild == null) {
            return isLayoutReversed
                    ? calculateCenterFromLeftEdge(lastChild, columnSpacing, pageWidth)
                    : calculateCenterFromRightEdge(lastChild, columnSpacing, pageWidth);
        }
        return isLayoutReversed
                ? calculateCenterFromRightEdge(firstChild, columnSpacing, pageWidth)
                : calculateCenterFromLeftEdge(firstChild, columnSpacing, pageWidth);
    }

    private int calculateCenterFromLeftEdge(View view, int columnSpacing, int pageWidth) {
        return view.getLeft() - columnSpacing / 2 + (pageWidth / 2);
    }

    private int calculateCenterFromRightEdge(View view, int columnSpacing, int pageWidth) {
        return view.getRight() + columnSpacing / 2 - (pageWidth / 2);
    }

    private int getLastChildIndexFromFirstChild(int firstChildIndex, int itemsInPageCount) {
        return firstChildIndex + (itemsInPageCount - 1);
    }

    private int getFirstChildIndexFromLastChild(int lastChildIndex, int itemsInPageCount) {
        return lastChildIndex - (itemsInPageCount - 1);
    }

    private int getPageWidth(int itemWidth, int columnSpacing, int colPerPage) {
        return (itemWidth + columnSpacing) * colPerPage;
    }
}
