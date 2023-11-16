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
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@RunWith(AndroidJUnit4::class)
class DockHelperTest {

    private val context = mock(Context::class.java)
    private val carPackageManager = mock(CarPackageManager::class.java)
    private val packageManager = mock(PackageManager::class.java)

    @Test
    fun defaultApps_getCorrectAppsFromConfig() {
        val resources = mock(Resources::class.java)
        val item1 =
            TestUtils.createAppItem(
                app = "item1",
                name = "item1",
                icon = mock(Drawable::class.java),
                isDrivingOptimized = true
            )
        val item2 =
            TestUtils.createAppItem(
                app = "item2",
                name = "item2",
                icon = mock(Drawable::class.java),
                isDrivingOptimized = false
            )
        `when`(context.resources).thenReturn(resources)
        `when`(context.packageManager).thenReturn(packageManager)
        `when`(resources.getStringArray(anyInt()))
            .thenReturn(
                arrayOf(item1.component.flattenToString(), item2.component.flattenToString())
            )
        `when`(packageManager.getApplicationIcon(item1.component.packageName))
            .thenReturn(item1.icon)
        `when`(packageManager.getApplicationIcon(item2.component.packageName))
            .thenReturn(item2.icon)
        val activityInfo1 = mock(ActivityInfo::class.java)
        activityInfo1.name = item1.name
        `when`(packageManager.getActivityInfo(item1.component, 0)).thenReturn(activityInfo1)
        val activityInfo2 = mock(ActivityInfo::class.java)
        activityInfo2.name = item2.name
        `when`(packageManager.getActivityInfo(item2.component, 0)).thenReturn(activityInfo2)
        `when`(
                carPackageManager.isActivityDistractionOptimized(
                    item1.component.packageName,
                    item1.component.className
                )
            )
            .thenReturn(item1.isDistractionOptimized)
        `when`(
                carPackageManager.isActivityDistractionOptimized(
                    item2.component.packageName,
                    item2.component.className
                )
            )
            .thenReturn(item2.isDistractionOptimized)

        val defaultApps = DockHelper(context, carPackageManager).defaultApps

        assertThat(defaultApps.size).isEqualTo(2)
        assertThat(defaultApps[0]).isEqualTo(item1)
        assertThat(defaultApps[1]).isEqualTo(item2)
    }

    @Test
    fun toDockAppItem_fetchCorrectAppName() {
        val item1 = TestUtils.createAppItem(app = "app", name = "name")
        `when`(context.packageManager).thenReturn(packageManager)
        `when`(packageManager.getApplicationIcon(any(String::class.java)))
                .thenReturn(mock(Drawable::class.java))
        val activityInfo1 = mock(ActivityInfo::class.java)
        activityInfo1.name = item1.name
        `when`(packageManager.getActivityInfo(item1.component, 0)).thenReturn(activityInfo1)

        val dockAppItem = DockHelper(context, carPackageManager).toDockAppItem(item1.component)

        assertThat(dockAppItem.name).isEqualTo(item1.name)
    }

    @Test
    fun toDockAppItem_fetchCorrectAppIcon() {
        val item1 = TestUtils.createAppItem(app = "app", icon = mock(Drawable::class.java))
        `when`(context.packageManager).thenReturn(packageManager)
        val activityInfo = mock(ActivityInfo::class.java)
        activityInfo.name = ""
        `when`(packageManager.getActivityInfo(any(), eq(0))).thenReturn(activityInfo)
        `when`(packageManager.getApplicationIcon(item1.component.packageName))
                .thenReturn(item1.icon)

        val dockAppItem = DockHelper(context, carPackageManager).toDockAppItem(item1.component)

        assertThat(dockAppItem.icon).isEqualTo(item1.icon)
    }

    @Test
    fun toDockAppItem_fetchCorrectAppDO() {
        val item1 = TestUtils.createAppItem(app = "app", isDrivingOptimized = true)
        `when`(context.packageManager).thenReturn(packageManager)
        val activityInfo = mock(ActivityInfo::class.java)
        activityInfo.name = ""
        `when`(packageManager.getActivityInfo(any(), eq(0))).thenReturn(activityInfo)
        `when`(packageManager.getApplicationIcon(any(String::class.java)))
                .thenReturn(mock(Drawable::class.java))
        `when`(
                carPackageManager.isActivityDistractionOptimized(
                    item1.component.packageName,
                    item1.component.className
                )
            )
            .thenReturn(item1.isDistractionOptimized)

        val dockAppItem = DockHelper(context, carPackageManager).toDockAppItem(item1.component)

        assertThat(dockAppItem.isDistractionOptimized).isEqualTo(item1.isDistractionOptimized)
    }

    @Test
    fun toDockAppItem_fetchCorrectAppInfo() {
        val item1 =
            TestUtils.createAppItem(
                app = "item1",
                name = "item1",
                icon = mock(Drawable::class.java),
                isDrivingOptimized = true
            )
        `when`(context.packageManager).thenReturn(packageManager)
        `when`(packageManager.getApplicationIcon(item1.component.packageName))
            .thenReturn(item1.icon)
        val activityInfo1 = mock(ActivityInfo::class.java)
        activityInfo1.name = item1.name
        `when`(packageManager.getActivityInfo(item1.component, 0)).thenReturn(activityInfo1)
        `when`(
                carPackageManager.isActivityDistractionOptimized(
                    item1.component.packageName,
                    item1.component.className
                )
            )
            .thenReturn(item1.isDistractionOptimized)

        val dockAppItem = DockHelper(context, carPackageManager).toDockAppItem(item1.component)

        assertThat(dockAppItem).isEqualTo(item1)
    }
}
