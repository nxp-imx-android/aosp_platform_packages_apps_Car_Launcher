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
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@RunWith(AndroidJUnit4::class)
class DefaultAppsProviderTest {

    @Test
    fun defaultApps_getCorrectAppsFromConfig() {
        val context = mock(Context::class.java)
        val carPackageManager = mock(CarPackageManager::class.java)
        val packageManager = mock(PackageManager::class.java)
        val resources = mock(Resources::class.java)
        val item1 =
            TestUtils.createAppItem(
                app = "item1",
                icon = mock(Drawable::class.java),
                isDrivingOptimized = true
            )
        val item2 =
            TestUtils.createAppItem(
                app = "item2",
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

        val defaultApps = DefaultAppsProvider(context, carPackageManager).defaultApps

        assertThat(defaultApps.size).isEqualTo(2)
        assertThat(defaultApps[0]).isEqualTo(item1)
        assertThat(defaultApps[1]).isEqualTo(item2)
    }
}
