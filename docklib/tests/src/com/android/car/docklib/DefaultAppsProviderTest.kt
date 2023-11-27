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
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class DefaultAppsProviderTest {
    val carPackageManagerMock = mock<CarPackageManager> {}
    val packageManagerMock = mock<PackageManager> {}
    val resourcesMock = mock<Resources> {}
    private val context = mock<Context> {
        on { resources } doReturn resourcesMock
        on { packageManager } doReturn packageManagerMock
    }

    @Test
    fun defaultApps_getCorrectAppsFromConfig() {
        val item1 = TestUtils.createAppItem(
            app = "item1",
            icon = mock<Drawable> {},
            isDrivingOptimized = true
        )
        val item2 = TestUtils.createAppItem(
            app = "item2",
            icon = mock<Drawable> {},
            isDrivingOptimized = false
        )
        val ai1 = mock<ActivityInfo> {}
        ai1.name = item1.name
        val ai2 = mock<ActivityInfo> {}
        ai2.name = item2.name
        whenever(resourcesMock.getStringArray(eq(R.array.config_defaultDockApps))) doReturn
                arrayOf(item1.component.flattenToString(), item2.component.flattenToString())
        whenever(
            packageManagerMock.getActivityInfo(
                eq(item1.component), any<PackageManager.ComponentInfoFlags>()
            )
        ) doReturn ai1
        whenever(
            packageManagerMock.getActivityInfo(
                eq(item2.component), any<PackageManager.ComponentInfoFlags>()
            )
        ) doReturn ai2
        whenever(packageManagerMock.getApplicationIcon(eq(item1.component.packageName))) doReturn
                item1.icon
        whenever(packageManagerMock.getApplicationIcon(eq(item2.component.packageName))) doReturn
                item2.icon
        whenever(
            carPackageManagerMock.isActivityDistractionOptimized(
                eq(item1.component.packageName),
                eq(item1.component.className)
            )
        ) doReturn item1.isDistractionOptimized
        whenever(
            carPackageManagerMock.isActivityDistractionOptimized(
                eq(item2.component.packageName),
                eq(item2.component.className)
            )
        ) doReturn item2.isDistractionOptimized

        val defaultApps = DefaultAppsProvider(context, carPackageManagerMock).defaultApps

        assertThat(defaultApps.size).isEqualTo(2)
        assertThat(defaultApps[0]).isEqualTo(item1)
        assertThat(defaultApps[1]).isEqualTo(item2)
    }
}
