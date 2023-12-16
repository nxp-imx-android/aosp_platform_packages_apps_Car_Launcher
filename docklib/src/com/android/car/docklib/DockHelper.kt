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

import android.car.content.pm.CarPackageManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.android.car.docklib.data.DockAppItem

/**
 * Helper that reads configs for defaults app to be showed on Dock and converts between
 * ComponentName and DockItem
 */
class DockHelper(
    private val context: Context,
    private val carPackageManager: CarPackageManager,
) {
    private val packageManager: PackageManager = context.packageManager
    val defaultApps by lazy {
        val defaultComponents = context.resources.getStringArray(R.array.config_defaultDockApps)
        defaultComponents.mapNotNull { component ->
            val componentName = ComponentName.unflattenFromString(component)
            componentName?.let { toDockAppItem(componentName) }
        }
    }
    val excludedComponents by lazy {
        HashSet<String>().apply {
            addAll(context.resources.getStringArray(R.array.config_componentsExcludedFromDock))
        }
    }
    val excludedPackages by lazy {
        HashSet<String>().apply {
            addAll(context.resources.getStringArray(R.array.config_packagesExcludedFromDock))
        }
    }

    /* Convert to Dock item from a ComponentName. */
    fun toDockAppItem(
        componentName: ComponentName,
        itemType: DockAppItem.Type = DockAppItem.Type.DYNAMIC
    ): DockAppItem {
        // TODO: Compare the component against LauncherApps to make sure the component
        // is launchable, similar to what app grid has
        val icon = packageManager.getApplicationIcon(componentName.packageName)
        val name = packageManager.getActivityInfo(componentName, 0).name
        return DockAppItem(
            itemType,
            componentName,
            name,
            icon,
            carPackageManager.isActivityDistractionOptimized(
                componentName.packageName,
                componentName.className
            )
        )
    }
}
