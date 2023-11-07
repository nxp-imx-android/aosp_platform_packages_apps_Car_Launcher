package com.android.car.docklib.view

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.car.docklib.TestUtils
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
class DockAdapterTest {
    @Test
    fun setItems_dockSizeEqualToListSize_adapterHasDockSize() {
        val defaultApps =
            listOf(TestUtils.createAppItem(app = "a"), TestUtils.createAppItem(app = "b"))
        val adapter = spy(DockAdapter(defaultApps.size) {})

        adapter.setItems(defaultApps)

        assertThat(adapter.itemCount).isEqualTo(defaultApps.size)
        verify(adapter).notifyItemChanged(0)
        verify(adapter).notifyItemChanged(1)
    }

    @Test
    fun setItems_dockSizeLessThanListSize_adapterHasDockSize() {
        val defaultApps =
            listOf(
                TestUtils.createAppItem(app = "a"),
                TestUtils.createAppItem(app = "b"),
                TestUtils.createAppItem(app = "c")
            )
        val dockSize = 2
        val adapter = spy(DockAdapter(dockSize) {})

        adapter.setItems(defaultApps)

        assertThat(adapter.itemCount).isEqualTo(dockSize)
        verify(adapter).notifyItemChanged(0)
        verify(adapter).notifyItemChanged(1)
    }

    @Test
    fun setItems_dockSizeGreaterThanListSize_adapterHasDockSize() {
        val defaultApps =
            listOf(TestUtils.createAppItem(app = "a"), TestUtils.createAppItem(app = "b"))
        val dockSize = 3
        val adapter = spy(DockAdapter(dockSize) {})

        adapter.setItems(defaultApps)

        assertThat(adapter.itemCount).isEqualTo(dockSize)
        verify(adapter).notifyItemChanged(0)
        verify(adapter).notifyItemChanged(1)
        verify(adapter, times(0)).notifyItemChanged(2)
    }
}
