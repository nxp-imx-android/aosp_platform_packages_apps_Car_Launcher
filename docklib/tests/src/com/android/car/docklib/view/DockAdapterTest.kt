package com.android.car.docklib.view

import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.car.docklib.TestUtils
import com.android.car.docklib.data.DockAppItem
import com.google.common.truth.Truth.assertThat
import java.util.function.Consumer
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never

@RunWith(AndroidJUnit4::class)
class DockAdapterTest {
    private val contextMock = mock<Context> {}
    private val intentConsumerMock = mock<Consumer<Intent>> {}
    private val dockItemViewHolderMock = mock<DockItemViewHolder> {}

    @Test
    fun setItems_dockSizeEqualToListSize_adapterHasDockSize() {
        val defaultApps =
            listOf(TestUtils.createAppItem(app = "a"), TestUtils.createAppItem(app = "b"))
        val adapter = spy(DockAdapter(defaultApps.size, intentConsumerMock, contextMock))

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
        val adapter = spy(DockAdapter(dockSize, intentConsumerMock, contextMock))

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
        val adapter = spy(DockAdapter(dockSize, intentConsumerMock, contextMock))

        adapter.setItems(defaultApps)

        assertThat(adapter.itemCount).isEqualTo(dockSize)
        verify(adapter).notifyItemChanged(0)
        verify(adapter).notifyItemChanged(1)
        verify(adapter, times(0)).notifyItemChanged(2)
    }

    @Test
    fun onBindViewHolder_emptyPayload_onBindViewHolderWithoutPayloadCalled() {
        val adapter = spy(DockAdapter(3, intentConsumerMock, contextMock))

        adapter.onBindViewHolder(dockItemViewHolderMock, 1, MutableList(0) {})

        verify(adapter).onBindViewHolder(eq(dockItemViewHolderMock), eq(1))
    }

    @Test
    fun onBindViewHolder_nullPayload_onBindViewHolderWithoutPayloadCalled() {
        val adapter = spy(DockAdapter(3, intentConsumerMock, contextMock))

        adapter.onBindViewHolder(dockItemViewHolderMock, 1, MutableList(1) {})

        verify(adapter).onBindViewHolder(eq(dockItemViewHolderMock), eq(1))
    }

    @Test
    fun onBindViewHolder_payloadOfIncorrectType_onBindViewHolderWithoutPayloadCalled() {
        class DummyPayload
        val adapter = spy(DockAdapter(3, intentConsumerMock, contextMock))

        adapter.onBindViewHolder(dockItemViewHolderMock, 1, MutableList(1) {
            DummyPayload()
        })

        verify(adapter).onBindViewHolder(eq(dockItemViewHolderMock), eq(1))
    }

    @Test
    fun onBindViewHolder_payload_CHANGE_SAME_ITEM_TYPE_itemTypeChangedCalled() {
        val dockAppItem0 = mock<DockAppItem> {}
        val dockAppItem1 = mock<DockAppItem> {}
        val dockAppItem2 = mock<DockAppItem> {}
        val adapter = spy(
            DockAdapter(
                numItems = 3,
                intentConsumerMock,
                contextMock,
                items = arrayOf(dockAppItem0, dockAppItem1, dockAppItem2)
            )
        )

        adapter.onBindViewHolder(dockItemViewHolderMock, 1, MutableList(1) {
            DockAdapter.PayloadType.CHANGE_SAME_ITEM_TYPE
        })

        verify(adapter, never()).onBindViewHolder(eq(dockItemViewHolderMock), eq(1))
        verify(dockItemViewHolderMock).itemTypeChanged(eq(dockAppItem1))
    }
}
