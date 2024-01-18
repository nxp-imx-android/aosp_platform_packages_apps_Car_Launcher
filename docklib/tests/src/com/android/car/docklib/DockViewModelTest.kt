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

package com.android.car.docklib

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.car.docklib.data.DockAppItem
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DockViewModelTest {
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var items: List<DockAppItem?>
    private lateinit var model: DockViewModel
    private val apps =
        listOf(
            TestUtils.createAppItem(app = "a"),
            TestUtils.createAppItem(app = "b"),
            TestUtils.createAppItem(app = "c"),
            TestUtils.createAppItem(app = "d"),
        )

    @Before
    fun setUp() {
        model = DockViewModel(4) { items = it }
    }

    @Test
    fun setDefaultApps_listSetInOrder() {
        model.updateDefaultApps(apps)

        assertThat(items.size).isEqualTo(4)
        assertThat(items[0]).isEqualTo(apps[0])
        assertThat(items[1]).isEqualTo(apps[1])
        assertThat(items[2]).isEqualTo(apps[2])
        assertThat(items[3]).isEqualTo(apps[3])
    }

    @Test
    fun addDynamicItem_beforeDefaultApps_index0Updated() {
        val dynamicItem = TestUtils.createAppItem(app = "da")

        model.addDynamicItem(dynamicItem)

        assertThat(items[0]).isEqualTo(dynamicItem)
    }

    @Test
    fun setDefaultApps_afterDynamicItem_fillRemainingPositions() {
        val dynamicItem = TestUtils.createAppItem(app = "da")
        model.addDynamicItem(dynamicItem)

        model.updateDefaultApps(apps)

        assertThat(items.size).isEqualTo(4)
        assertThat(items[0]).isEqualTo(dynamicItem)
        assertThat(items[1]).isEqualTo(apps[1])
        assertThat(items[2]).isEqualTo(apps[2])
        assertThat(items[3]).isEqualTo(apps[3])
    }

    @Test
    fun addDynamicItem_allItemsDefault_index0Updated() {
        model.updateDefaultApps(apps)

        val dynamicItem = TestUtils.createAppItem(app = "da")
        model.addDynamicItem(dynamicItem)

        assertThat(items.size).isEqualTo(4)
        assertThat(items[0]).isEqualTo(dynamicItem)
        assertThat(items[1]).isEqualTo(apps[1])
        assertThat(items[2]).isEqualTo(apps[2])
        assertThat(items[3]).isEqualTo(apps[3])
    }

    @Test
    fun addDynamicItem_someItemsDefault_index1Updated() {
        model.updateDefaultApps(apps)
        val dynamicItem1 = TestUtils.createAppItem(app = "da1")
        model.addDynamicItem(dynamicItem1)

        val dynamicItem2 = TestUtils.createAppItem(app = "da2")
        model.addDynamicItem(dynamicItem2)

        assertThat(items.size).isEqualTo(4)
        assertThat(items[0]).isEqualTo(dynamicItem1)
        assertThat(items[1]).isEqualTo(dynamicItem2)
        assertThat(items[2]).isEqualTo(apps[2])
        assertThat(items[3]).isEqualTo(apps[3])
    }

    @Test
    fun addDynamicItem_someItemsDefault_index2Updated() {
        model.updateDefaultApps(apps)
        val dynamicItem1 = TestUtils.createAppItem(app = "da1")
        model.addDynamicItem(dynamicItem1)
        val dynamicItem2 = TestUtils.createAppItem(app = "da2")
        model.addDynamicItem(dynamicItem2)

        val dynamicItem3 = TestUtils.createAppItem(app = "da3")
        model.addDynamicItem(dynamicItem3)

        assertThat(items.size).isEqualTo(4)
        assertThat(items[0]).isEqualTo(dynamicItem1)
        assertThat(items[1]).isEqualTo(dynamicItem2)
        assertThat(items[2]).isEqualTo(dynamicItem3)
        assertThat(items[3]).isEqualTo(apps[3])
    }

    @Test
    fun addDynamicItem_oneItemDefault_index3Updated() {
        model.updateDefaultApps(apps)
        val dynamicItem1 = TestUtils.createAppItem(app = "da1")
        model.addDynamicItem(dynamicItem1)
        val dynamicItem2 = TestUtils.createAppItem(app = "da2")
        model.addDynamicItem(dynamicItem2)
        val dynamicItem3 = TestUtils.createAppItem(app = "da3")
        model.addDynamicItem(dynamicItem3)

        val dynamicItem4 = TestUtils.createAppItem(app = "da4")
        model.addDynamicItem(dynamicItem4)

        assertThat(items.size).isEqualTo(4)
        assertThat(items[0]).isEqualTo(dynamicItem1)
        assertThat(items[1]).isEqualTo(dynamicItem2)
        assertThat(items[2]).isEqualTo(dynamicItem3)
        assertThat(items[3]).isEqualTo(dynamicItem4)
    }

    @Test
    fun addDynamicItem_allItemsDynamic_leastRecentItemUpdated() {
        model.updateDefaultApps(apps)
        val dynamicItem1 = TestUtils.createAppItem(app = "da1")
        model.addDynamicItem(dynamicItem1)
        val dynamicItem2 = TestUtils.createAppItem(app = "da2")
        model.addDynamicItem(dynamicItem2)
        val dynamicItem3 = TestUtils.createAppItem(app = "da3")
        model.addDynamicItem(dynamicItem3)
        val dynamicItem4 = TestUtils.createAppItem(app = "da4")
        model.addDynamicItem(dynamicItem4)

        val dynamicItem5 = TestUtils.createAppItem(app = "da5")
        model.addDynamicItem(dynamicItem5)

        assertThat(items.size).isEqualTo(4)
        assertThat(items[0]).isEqualTo(dynamicItem5)
        assertThat(items[1]).isEqualTo(dynamicItem2)
        assertThat(items[2]).isEqualTo(dynamicItem3)
        assertThat(items[3]).isEqualTo(dynamicItem4)
    }

    @Test
    fun addDynamicItem_appInDock_itemsNotChanged() {
        model.updateDefaultApps(apps)
        val dynamicItem1 = TestUtils.createAppItem(app = "da1")
        model.addDynamicItem(dynamicItem1)
        val dynamicItem2 = TestUtils.createAppItem(app = "da2")
        model.addDynamicItem(dynamicItem2)
        val dynamicItem3 = TestUtils.createAppItem(app = "da3")
        model.addDynamicItem(dynamicItem3)
        val dynamicItem4 = TestUtils.createAppItem(app = "da4")
        model.addDynamicItem(dynamicItem4)

        val dynamicItem1B = TestUtils.createAppItem(app = "da1")
        model.addDynamicItem(dynamicItem1B)

        assertThat(items.size).isEqualTo(4)
        assertThat(items[0]).isEqualTo(dynamicItem1)
        assertThat(items[1]).isEqualTo(dynamicItem2)
        assertThat(items[2]).isEqualTo(dynamicItem3)
        assertThat(items[3]).isEqualTo(dynamicItem4)
    }

    @Test
    fun addDynamicItem_appInDock_recencyRefreshed() {
        model.updateDefaultApps(apps)
        val dynamicItem1 = TestUtils.createAppItem(app = "da1")
        model.addDynamicItem(dynamicItem1)
        val dynamicItem2 = TestUtils.createAppItem(app = "da2")
        model.addDynamicItem(dynamicItem2)
        val dynamicItem3 = TestUtils.createAppItem(app = "da3")
        model.addDynamicItem(dynamicItem3)
        val dynamicItem4 = TestUtils.createAppItem(app = "da4")
        model.addDynamicItem(dynamicItem4)

        val dynamicItem2B = TestUtils.createAppItem(app = "da2")
        model.addDynamicItem(dynamicItem2B)
        val dynamicItem5 = TestUtils.createAppItem(app = "da5")
        model.addDynamicItem(dynamicItem5)
        val dynamicItem6 = TestUtils.createAppItem(app = "da6")
        model.addDynamicItem(dynamicItem6)

        assertThat(items.size).isEqualTo(4)
        assertThat(items[0]).isEqualTo(dynamicItem5)
        assertThat(items[1]).isEqualTo(dynamicItem2B)
        assertThat(items[2]).isEqualTo(dynamicItem6)
        assertThat(items[3]).isEqualTo(dynamicItem4)
    }
}
