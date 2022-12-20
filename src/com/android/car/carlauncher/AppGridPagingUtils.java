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

import android.view.View;

/**
 * A utility class that handles all the pagination and page rounding logic for AppGridAdapter, and
 * should be initiated by the AppGridAdapter.
 */
public class AppGridPagingUtils {
    int mNumOfCols;
    int mNumOfRows;
    int mLayoutDirection;

    public AppGridPagingUtils(int numOfCols, int numOfRows) {
        mNumOfCols = numOfCols;
        mNumOfRows = numOfRows;
    }

    /**
     * Layout direction needs to be reset onResume as to not crash when user switches to another
     * language with different layout direction.
     */
    void setLayoutDirection(int layoutDirection) {
        mLayoutDirection = layoutDirection;
    }

    /**
     * Grid position refers to the default position in a RecyclerView used to draw out a horizontal
     * grid layout, shown below.
     *
     * This is the value returned by the default ViewHolder.getAbsoluteAdapterPosition().
     *
     *          * Grid Position example
     *          *  | 0  3  6  9  12 | 15 18 21 24 27 |
     *          *  | 1  4  7  10 13 | 16 19 22 25 28 |
     *          *  | 2  5  8  11 14 | 17 20 23 26 29 |
     *
     */
    public int gridPositionToAdaptorIndex(int position) {
        int positionOnPage = position % (mNumOfCols * mNumOfRows);
        // page the item resides on
        int pid = position / (mNumOfCols * mNumOfRows);
        // row of the item, in matrix order
        int rid = positionOnPage % mNumOfRows;
        // column of the item, in matrix order / LTR order
        int cid = positionOnPage / mNumOfRows;

        if (mLayoutDirection == View.LAYOUT_DIRECTION_RTL) {
            cid = mNumOfCols - cid - 1;
        }
        return (pid * mNumOfRows * mNumOfCols) + (rid * mNumOfCols) + cid;
    }

    /**
     * Adapter index refers to the "business logic" index, which is the order which the users will
     * read the app in their language (either LTR or RTL on each page)
     *
     * This is the value returned by mLauncherItems.indexOf(appItem) in the launcher data model.
     *
     *          * Adapter index example, in LTR
     *          *  | 0  1  2  3  4  | 15 16 17 18 19 |
     *          *  | 5  6  7  8  9  | 20 21 22 23 24 |
     *          *  | 10 11 12 13 14 | 25 26 27 28 29 |
     *
     *          * Adapter index in RTL languages
     *          *  | 4  3  2  1  0  | 19 18 17 16 15 |
     *          *  | 9  8  7  6  5  | 24 23 22 21 20 |
     *          *  | 14 13 12 11 10 | 29 28 27 26 25 |
     *
     */
    public int adaptorIndexToGridPosition(int index) {
        int indexOnPage = index % (mNumOfCols * mNumOfRows);
        // page the item resides on
        int pid = index / (mNumOfCols * mNumOfRows);
        // row of the item, in matrix order
        int rid = indexOnPage / mNumOfCols;
        // column of the item, in matrix order / LTR order
        int cid = indexOnPage % mNumOfCols;

        if (mLayoutDirection == View.LAYOUT_DIRECTION_RTL) {
            cid =  mNumOfCols - cid - 1;
        }
        return (pid * mNumOfRows * mNumOfCols) + (cid * mNumOfRows) + rid;
    }

    /**
     * Returns the grid position of the FIRST item on the page. The result is always the same in RTL
     * and LTR since the first and last index of every page is always mirrored.
     *
     *          * Grid Position example
     *          *  |[0] 3  6  9  12 |[15] 18 21 24 27 |
     *          *  | 1  4  7  10 13 | 16  19 22 25 28 |
     *          *  | 2  5  8  11 14 | 17  20 23 26 29 |
     *
     */
    public int roundToLeftmostIndexOnPage(int gridPosition) {
        int pidRoundedDown = gridPosition / (mNumOfCols * mNumOfRows);
        return pidRoundedDown * (mNumOfCols * mNumOfRows);
    }

    /**
     * Returns the grid position of the LAST item on the page. The result is always the same in RTL
     * and LTR since the first and last index of every page is always mirrored.
     *
     *          * Grid Position example
     *          *  | 0  3  6  9  12   | 15  18 21 24  27  |
     *          *  | 1  4  7  10 13   | 16  19 22 25  28  |
     *          *  | 2  5  8  11 [14] | 17  20 23 26 [29] |
     *
     */
    public int roundToRightmostIndexOnPage(int gridPosition) {
        int pidRoundedUp = gridPosition / (mNumOfCols * mNumOfRows) + 1;
        return pidRoundedUp * (mNumOfCols * mNumOfRows) - 1;
    }
}
