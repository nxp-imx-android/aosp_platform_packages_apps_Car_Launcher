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

package com.android.car.docklib.data

import android.graphics.drawable.Drawable
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.car.docklib.TestUtils
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@RunWith(AndroidJUnit4::class)
class DockAppItemTest {
    @Test
    fun compareAppItems_equal() {
        val item1: DockAppItem = TestUtils.createAppItem()
        val item2: DockAppItem = TestUtils.createAppItem()

        assertThat(item1).isEqualTo(item2)
    }

    @Test
    fun compareAppItems_notEqual_differentApps() {
        val item1: DockAppItem = TestUtils.createAppItem(app = "1")
        val item2: DockAppItem = TestUtils.createAppItem(app = "2")

        assertThat(item1).isNotEqualTo(item2)
    }

    @Test
    fun compareAppItems_notEqual_differentNames() {
        val item1: DockAppItem = TestUtils.createAppItem(name = "1")
        val item2: DockAppItem = TestUtils.createAppItem(name = "2")

        assertThat(item1).isNotEqualTo(item2)
    }

    @Test
    fun compareAppItems_notEqual_differentStates() {
        val item1: DockAppItem = TestUtils.createAppItem(type = DockAppItem.Type.DYNAMIC)
        val item2: DockAppItem = TestUtils.createAppItem(type = DockAppItem.Type.STATIC)

        assertThat(item1).isNotEqualTo(item2)
    }

    @Test
    fun compareAppItems_notEqual_differentIcons() {
        val icon1 = mock(Drawable::class.java)
        `when`(icon1.constantState).thenReturn(null)
        val icon2 = mock(Drawable::class.java)
        `when`(icon2.constantState).thenReturn(mock(Drawable.ConstantState::class.java))

        val item1: DockAppItem = TestUtils.createAppItem(icon = icon1)
        val item2: DockAppItem = TestUtils.createAppItem(icon = icon2)

        assertThat(item1).isNotEqualTo(item2)
    }

    @Test
    fun compareAppItems_notEqual_differentDrivingOptimized() {
        val item1: DockAppItem = TestUtils.createAppItem(isDrivingOptimized = true)
        val item2: DockAppItem = TestUtils.createAppItem(isDrivingOptimized = false)

        assertThat(item1).isNotEqualTo(item2)
    }
}
