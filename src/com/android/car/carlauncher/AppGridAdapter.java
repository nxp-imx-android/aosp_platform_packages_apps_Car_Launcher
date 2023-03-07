/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static com.android.car.carlauncher.AppGridConstants.AppItemBoundDirection;
import static com.android.car.carlauncher.AppGridConstants.PageOrientation;

import android.content.Context;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.VisibleForTesting;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * The adapter that populates the grid view with apps.
 */
final class AppGridAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int RECENT_APPS_TYPE = 1;
    public static final int APP_ITEM_TYPE = 2;

    private static final String TAG = "AppGridAdapter";
    private final Context mContext;
    private final LayoutInflater mInflater;
    private final AppGridPagingUtils mPagingUtil;
    private final AppItemViewHolder.AppItemDragCallback mDragCallback;
    private final AppGridPageSnapper.AppGridPageSnapCallback mSnapCallback;
    private final int mNumOfCols;
    private final int mNumOfRows;
    private int mAppItemWidth;
    private int mAppItemHeight;
    private final LauncherViewModel mDataModel;
    // grid order of the mLauncherItems used by DiffUtils in dispatchUpdates to animate UI updates
    private final List<LauncherItem> mGridOrderedLauncherItems;

    private List<LauncherItem> mLauncherItems;
    private boolean mIsDistractionOptimizationRequired;
    private int mPageScrollDestination;
    // the global bounding rect of the app grid including margins (excluding position indicator bar)
    private Rect mGridBound;

    AppGridAdapter(Context context, int numOfCols, int numOfRows, LauncherViewModel dataModel,
            AppItemViewHolder.AppItemDragCallback dragCallback,
            AppGridPageSnapper.AppGridPageSnapCallback snapCallback) {
        this(context, numOfCols, numOfRows,
                context.getResources().getBoolean(R.bool.use_vertical_app_grid)
                        ? PageOrientation.VERTICAL : PageOrientation.HORIZONTAL,
                LayoutInflater.from(context), dataModel, dragCallback, snapCallback);
    }

    @VisibleForTesting
    AppGridAdapter(Context context, int numOfCols, int numOfRows,
            @PageOrientation int pageOrientation,
            LayoutInflater layoutInflater, LauncherViewModel dataModel,
            AppItemViewHolder.AppItemDragCallback dragCallback,
            AppGridPageSnapper.AppGridPageSnapCallback snapCallback) {
        mContext = context;
        mInflater = layoutInflater;
        mNumOfCols = numOfCols;
        mNumOfRows = numOfRows;
        mDragCallback = dragCallback;
        mSnapCallback = snapCallback;

        mPagingUtil = new AppGridPagingUtils(numOfCols, numOfRows, pageOrientation);
        mGridOrderedLauncherItems = new ArrayList<>();
        mDataModel = dataModel;
    }

    void updateAppGridDimensions(Rect gridBound, int appItemWidth, int appItemHeight) {
        mGridBound = gridBound;
        mAppItemWidth = appItemWidth;
        mAppItemHeight = appItemHeight;
    }

    void setIsDistractionOptimizationRequired(boolean isDistractionOptimizationRequired) {
        mIsDistractionOptimizationRequired = isDistractionOptimizationRequired;
        // notifyDataSetChanged will rebind distraction optimization to all app items
        notifyDataSetChanged();
    }
    /**
     * Sets a new list of launcher items to be displayed in the app grid.
     * This should only be called by onChanged() in the observer as a response to data change in the
     * adapter's LauncherViewModel.
     */
    public void setLauncherItems(List<LauncherItem> launcherItems) {
        mLauncherItems = launcherItems;
        int newSnapPosition = mSnapCallback.getSnapPosition();
        if (newSnapPosition != 0 && newSnapPosition >= getItemCount()) {
            // in case user deletes the only app item on the last page, the page should snap to the
            // last icon on the second last page.
            mSnapCallback.notifySnapToPosition(getItemCount() - 1);
        }
        dispatchUpdates();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 && hasRecentlyUsedApps()) {
            return RECENT_APPS_TYPE;
        }
        return APP_ITEM_TYPE;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == RECENT_APPS_TYPE) {
            View view =
                    mInflater.inflate(R.layout.recent_apps_row, parent, /* attachToRoot= */ false);
            return new RecentAppsRowViewHolder(view, mContext);
        } else {
            View view = mInflater.inflate(R.layout.app_item, parent, /* attachToRoot= */ false);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    mAppItemWidth, mAppItemHeight);
            view.setLayoutParams(layoutParams);
            return new AppItemViewHolder(view, mContext, mDragCallback, mSnapCallback,
                    mGridBound);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        AppItemViewHolder viewHolder = (AppItemViewHolder) holder;
        int adapterIndex = mPagingUtil.gridPositionToAdaptorIndex(position);
        if (adapterIndex >= mLauncherItems.size()) {
            // the current view holder is an empty item used to pad the last page.
            viewHolder.bind(null, mIsDistractionOptimizationRequired);
            return;
        }
        AppItem item = (AppItem) mLauncherItems.get(adapterIndex);
        viewHolder.bind(item.getAppMetaData(), mIsDistractionOptimizationRequired);
    }

    void setLayoutDirection(int layoutDirection) {
        mPagingUtil.setLayoutDirection(layoutDirection);
    }


    @Override
    public int getItemCount() {
        return getItemCountInternal(getLauncherItemsCount());
    }

    /** Returns the item count including padded spaces on the last page */
    private int getItemCountInternal(int unpaddedItemCount) {
        // item count should always be a multiple of block size to ensure pagination
        // is done properly. Extra spaces will have empty ViewHolders binded.
        float pageFraction = (float) unpaddedItemCount / (mNumOfCols * mNumOfRows);
        int pageCount = (int) Math.ceil(pageFraction);
        return pageCount * mNumOfCols * mNumOfRows;
    }

    @VisibleForTesting
    int getLauncherItemsCount() {
        return mLauncherItems == null ? 0 : mLauncherItems.size();
    }

    /**
     * Calculates the number of pages required to fit the all app items in the recycler view, with
     * minimum of 1 page when no items have been added to data model.
     */
    public int getPageCount() {
        return getPageCount(/* unpaddedItemCount */ getItemCount());
    }

    /**
     * Calculates the number of pages required to fit in
     */
    int getPageCount(int unpaddedItemCount) {
        int pageCount = getItemCountInternal(unpaddedItemCount) / (mNumOfRows * mNumOfCols);
        return Math.max(pageCount, 1);
    }

    @AppItemBoundDirection
    int getOffsetBoundDirection(int gridPosition) {
        return mPagingUtil.getOffsetBoundDirection(gridPosition);
    }


    private boolean hasRecentlyUsedApps() {
        // TODO (b/266988404): deprecate ui logic associated with recently used apps
        return false;
    }

    public void setDragStartPoint(int gridPosition) {
        mPageScrollDestination = mPagingUtil.roundToFirstIndexOnPage(gridPosition);
        mSnapCallback.notifySnapToPosition(mPageScrollDestination);
    }

    /**
     * The magical function that writes the new order to proto datastore.
     *
     * There should not be any calls to update RecyclerView, such as via notifyDatasetChanged in
     * this method since UI changes relating to data model should be handled by data observer.
     */
    public void moveAppItem(int gridPositionFrom, int gridPositionTo) {
        int adaptorIndexFrom = mPagingUtil.gridPositionToAdaptorIndex(gridPositionFrom);
        int adaptorIndexTo = mPagingUtil.gridPositionToAdaptorIndex(gridPositionTo);
        mPageScrollDestination = mPagingUtil.roundToFirstIndexOnPage(gridPositionTo);
        mSnapCallback.notifySnapToPosition(mPageScrollDestination);

        // we need to move package to target index even if the from and to index are the same to
        // ensure dispatchLayout gets called to re-anchor the recyclerview to current page.
        AppItem selectedApp = (AppItem) mLauncherItems.get(adaptorIndexFrom);
        mDataModel.movePackage(adaptorIndexTo, selectedApp.getAppMetaData());
    }


    /**
     * Updates page scroll destination after user has held the app item at the end of page for
     * longer than the scroll dispatch threshold.
     */
    void updatePageScrollDestination(boolean scrollToNextPage) {
        int newDestination;
        int blockSize = mNumOfCols * mNumOfRows;
        if (scrollToNextPage) {
            newDestination = mPageScrollDestination + blockSize;
            mPageScrollDestination = (newDestination >= getItemCount()) ? mPageScrollDestination :
                    mPagingUtil.roundToLastIndexOnPage(newDestination);
        } else {
            newDestination = mPageScrollDestination - blockSize;
            mPageScrollDestination = (newDestination < 0) ? mPageScrollDestination :
                    mPagingUtil.roundToFirstIndexOnPage(newDestination);
        }
        mSnapCallback.notifySnapToPosition(mPageScrollDestination);
    }

    @VisibleForTesting
    int getPageScrollDestination() {
        return mPageScrollDestination;
    }

    /**
     * Dispatches the paged reordering animation using async list differ, based on
     * the current adapter order when the method is called.
     */
    private void dispatchUpdates() {
        List<LauncherItem> newAppsList = new ArrayList<>();
        // we first need to pad the empty items on the last page
        for (int i = 0; i < getItemCount(); i++) {
            newAppsList.add(getEmptyLauncherItem());
        }

        for (int i = 0; i < mLauncherItems.size(); i++) {
            newAppsList.set(mPagingUtil.adaptorIndexToGridPosition(i), mLauncherItems.get(i));
        }
        LauncherItemDiffCallback callback = new LauncherItemDiffCallback(
                /* oldList */ mGridOrderedLauncherItems, /* newList */ newAppsList);
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(callback);

        mGridOrderedLauncherItems.clear();
        mGridOrderedLauncherItems.addAll(newAppsList);
        result.dispatchUpdatesTo(this);
    }

    private LauncherItem getEmptyLauncherItem() {
        return new AppItem(/* packageName*/ "", /* className */ "", /* displayName */ "",
                /* appMetaData */ null);
    }

    /**
     * Returns the grid position of the next intended rotary focus view. This should follow the
     * same logical order as the adapter indexes.
     */
    public int getNextRotaryFocus(int focusedGridPosition, int direction) {
        int targetAdapterIndex = mPagingUtil.gridPositionToAdaptorIndex(focusedGridPosition)
                + (direction == View.FOCUS_FORWARD ? 1 : -1);
        if (targetAdapterIndex < 0 || targetAdapterIndex >= getLauncherItemsCount()) {
            return focusedGridPosition;
        }
        return mPagingUtil.adaptorIndexToGridPosition(targetAdapterIndex);
    }
}
