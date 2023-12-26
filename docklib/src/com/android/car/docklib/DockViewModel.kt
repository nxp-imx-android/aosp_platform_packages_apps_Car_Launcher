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

package com.android.car.docklib

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.android.car.docklib.data.DockAppItem

/**
 * This class contains a live list of dock app items. All changes to dock items will go through it
 * and will be observed by the view layer.
 */
class DockViewModel(private val numItems: Int, private val observer: Observer<List<DockAppItem?>>) {
    private val currentItems = MutableLiveData<List<DockAppItem?>>()

    /* Maintain a mapping of dock index to dock item, with the order of addition,
     * so it's easier to find least recently updated position.
     * The order goes from least recently updated item to most recently updated item.
     * The key in each mapping is the index/position of the item being shown in Dock.
     */
    private val internalItems = LinkedHashMap<Int, DockAppItem>()

    init {
        currentItems.value = List(numItems) { null }
        currentItems.observeForever(observer)
    }

    /** Update default apps if the list is not populated */
    fun updateDefaultApps(defaultApps: List<DockAppItem>) {
        synchronized(internalItems) {
            if (internalItems.size >= numItems) return
            defaultApps.forEachIndexed { index, defaultAppItem ->
                if (!internalItems.containsKey(index) && index < numItems) {
                    // change to default app if the position is not populated
                    internalItems[index] = defaultAppItem
                }
            }
            currentItems.value = convertMapToList(internalItems)
        }
    }

    /**
     * Add a new app to the dock. If the app is already in the dock, the recency of the app is
     * refreshed. If not, and the dock has dynamic item(s) to update, then it will replace the least
     * recent dynamic item.
     */
    fun addDynamicItem(appItem: DockAppItem) {
        synchronized(internalItems) {
            val indexToUpdate =
                indexOfItemWithPackageName(appItem.component.packageName)
                    ?: indexOfLeastRecentDynamicItemInDock()

            indexToUpdate?.let {
                internalItems.remove(it)
                internalItems[it] = appItem
                currentItems.value = convertMapToList(internalItems)
            }
        }
    }

    /**
     * Removes all items of the given [packageName] from the dock.
     */
    fun removeItems(packageName: String) {
        // todo: Use createDockList instead of convertMapToList when ready
        internalItems.entries.removeAll { it.value.component.packageName == packageName }
        currentItems.value = convertMapToList(internalItems)
    }

    fun destroy() {
        currentItems.removeObserver(observer)
    }

    private fun indexOfLeastRecentDynamicItemInDock(): Int? {
        // edge case - if there is no apps being shown, update first position
        if (internalItems.size == 0) return 0
        // since map is ordered from least recent to most recent, return first dynamic entry found
        internalItems.forEach { appItemEntry ->
            if (appItemEntry.value.type == DockAppItem.Type.DYNAMIC) return appItemEntry.key
        }
        // there is no dynamic item in dock to be replaced
        return null
    }

    private fun indexOfItemWithPackageName(packageName: String): Int? {
        internalItems.forEach { appItemEntry ->
            if (appItemEntry.value.component.packageName == packageName) {
                return appItemEntry.key
            }
        }
        return null
    }

    /** Use the mapping index->item to create the ordered list of Dock items */
    private fun convertMapToList(map: Map<Int, DockAppItem>) =
        List(numItems) { index -> map[index] }
    // TODO b/314409899: use a default DockItem when a position is empty

    @VisibleForTesting
    fun setDockItems(dockList: List<DockAppItem>) {
        internalItems.clear()
        dockList.forEachIndexed { index, item -> internalItems[index] = item }
        currentItems.value = dockList
    }
}
