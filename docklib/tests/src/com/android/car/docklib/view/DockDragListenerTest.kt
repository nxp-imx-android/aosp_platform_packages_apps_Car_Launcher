package com.android.car.docklib.view

import android.content.ClipData
import android.content.ClipDescription
import android.content.ComponentName
import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.view.DragEvent
import android.view.DragEvent.ACTION_DRAG_ENTERED
import android.view.DragEvent.ACTION_DRAG_EXITED
import android.view.DragEvent.ACTION_DRAG_STARTED
import android.view.DragEvent.ACTION_DROP
import android.view.SurfaceControl
import android.view.View
import androidx.core.animation.ValueAnimator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.car.docklib.R
import com.android.car.docklib.view.DockDragListener.Companion.APP_ITEM_DRAG_TAG
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyFloat
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class DockDragListenerTest {
    private val resourcesMock = mock<Resources> {
        on { getInteger(eq(R.integer.drag_drop_animate_in_duration)) } doReturn 0
    }
    private val contextMock = mock<Context> {
        on { resources } doReturn resourcesMock
    }
    private val itemViewMock = mock<View> {
        on { context } doReturn contextMock
    }
    private val viewHolderSpy = spy(object : ViewHolder(itemViewMock) {})
    private val viewMock = mock<View> {}
    private val dragEventMock = mock<DragEvent> {}
    private val clipDescriptionMock = mock<ClipDescription> {}
    private val clipDataMock = mock<ClipData> {}
    private val clipDataItemMock = mock<ClipData.Item> {}
    private val surfaceControlMock = mock<SurfaceControl> {}
    private val surfaceControlTransactionMock = mock<SurfaceControl.Transaction> {}
    private val surfaceControlTransactionMock2 = mock<SurfaceControl.Transaction> {}
    private val callbackMock = mock<DockDragListener.Callback> {}
    private val valueAnimatorMock = mock<ValueAnimator> {}
    private val componentNameCaptor = argumentCaptor<ComponentName>()
    private var dockDragListener = DockDragListener(viewHolderSpy, callbackMock)

    companion object {
        private val VALID_COMPONENT_NAME = ComponentName(
            "com.android.car.docklib.view",
            DockDragListenerTest::javaClass.name
        ).flattenToString()
        private const val VALID_ADAPTER_POSITION = 0
    }

    @Test
    fun onDrag_ACTION_DRAG_STARTED_clipDescriptionNameInvalid_returnFalse() {
        val invalidDragTag = "$APP_ITEM_DRAG_TAG-gibberish"
        whenever(dragEventMock.action) doReturn ACTION_DRAG_STARTED
        whenever(clipDescriptionMock.label) doReturn invalidDragTag
        whenever(dragEventMock.clipDescription) doReturn clipDescriptionMock

        val ret = dockDragListener.onDrag(viewMock, dragEventMock)

        assertThat(ret).isFalse()
    }

    @Test
    fun onDrag_ACTION_DRAG_STARTED_clipDescriptionNameValid_returnTrue() {
        val validDragTag = APP_ITEM_DRAG_TAG
        whenever(dragEventMock.action) doReturn ACTION_DRAG_STARTED
        whenever(clipDescriptionMock.label) doReturn validDragTag
        whenever(dragEventMock.clipDescription) doReturn clipDescriptionMock

        val ret = dockDragListener.onDrag(viewMock, dragEventMock)

        assertThat(ret).isTrue()
    }

    @Test
    fun onDrag_ACTION_DRAG_ENTERED_exciteViewCallbackTriggered() {
        whenever(dragEventMock.action) doReturn ACTION_DRAG_ENTERED

        dockDragListener.onDrag(viewMock, dragEventMock)

        verify(callbackMock).exciteView()
    }

    @Test
    fun onDrag_ACTION_DRAG_EXITED_resetViewCallbackTriggered() {
        whenever(dragEventMock.action) doReturn ACTION_DRAG_EXITED

        dockDragListener.onDrag(viewMock, dragEventMock)

        verify(callbackMock).resetView()
    }

    @Test
    fun onDrag_ACTION_DROP_invalidPosition_returnFalse() {
        whenever(dragEventMock.action) doReturn ACTION_DROP
        whenever(viewHolderSpy.bindingAdapterPosition) doReturn RecyclerView.NO_POSITION

        val ret = dockDragListener.onDrag(viewMock, dragEventMock)

        assertThat(ret).isFalse()
    }

    @Test
    fun onDrag_ACTION_DROP_invalidPosition_resetViewCallbackTriggered() {
        whenever(dragEventMock.action) doReturn ACTION_DROP
        whenever(viewHolderSpy.bindingAdapterPosition) doReturn RecyclerView.NO_POSITION

        dockDragListener.onDrag(viewMock, dragEventMock)

        verify(callbackMock).resetView()
    }

    @Test
    fun onDrag_ACTION_DROP_noClipData_returnFalse() {
        whenever(dragEventMock.action) doReturn ACTION_DROP
        whenever(viewHolderSpy.bindingAdapterPosition) doReturn VALID_ADAPTER_POSITION
        whenever(dragEventMock.clipData) doReturn clipDataMock
        whenever(clipDataMock.getItemAt(any())) doReturn null

        val ret = dockDragListener.onDrag(viewMock, dragEventMock)

        assertThat(ret).isFalse()
    }

    @Test
    fun onDrag_ACTION_DROP_noClipData_resetViewCallbackTriggered() {
        whenever(dragEventMock.action) doReturn ACTION_DROP
        whenever(viewHolderSpy.bindingAdapterPosition) doReturn VALID_ADAPTER_POSITION
        whenever(dragEventMock.clipData) doReturn clipDataMock
        whenever(clipDataMock.getItemAt(any())) doReturn null

        dockDragListener.onDrag(viewMock, dragEventMock)

        verify(callbackMock).resetView()
    }

    @Test
    fun onDrag_ACTION_DROP_clipDataWithNoText_returnFalse() {
        whenever(dragEventMock.action) doReturn ACTION_DROP
        whenever(viewHolderSpy.bindingAdapterPosition) doReturn VALID_ADAPTER_POSITION
        whenever(clipDataItemMock.text) doReturn null
        whenever(clipDataMock.getItemAt(eq(0))) doReturn clipDataItemMock
        whenever(dragEventMock.clipData) doReturn clipDataMock

        val ret = dockDragListener.onDrag(viewMock, dragEventMock)

        assertThat(ret).isFalse()
    }

    @Test
    fun onDrag_ACTION_DROP_clipDataWithNoText_resetViewCallbackTriggered() {
        whenever(dragEventMock.action) doReturn ACTION_DROP
        whenever(viewHolderSpy.bindingAdapterPosition) doReturn VALID_ADAPTER_POSITION
        whenever(clipDataItemMock.text) doReturn null
        whenever(clipDataMock.getItemAt(eq(0))) doReturn clipDataItemMock
        whenever(dragEventMock.clipData) doReturn clipDataMock

        dockDragListener.onDrag(viewMock, dragEventMock)

        verify(callbackMock).resetView()
    }

    @Test
    fun onDrag_ACTION_DROP_invalidComponentName_returnFalse() {
        val invalidComponentName = "invalidComponentName"
        whenever(dragEventMock.action) doReturn ACTION_DROP
        whenever(viewHolderSpy.bindingAdapterPosition) doReturn VALID_ADAPTER_POSITION
        whenever(clipDataItemMock.text) doReturn invalidComponentName
        whenever(clipDataMock.getItemAt(eq(0))) doReturn clipDataItemMock
        whenever(dragEventMock.clipData) doReturn clipDataMock

        val ret = dockDragListener.onDrag(viewMock, dragEventMock)

        assertThat(ret).isFalse()
    }

    @Test
    fun onDrag_ACTION_DROP_invalidComponentName_resetViewCallbackTriggered() {
        val invalidComponentName = "invalidComponentName"
        whenever(dragEventMock.action) doReturn ACTION_DROP
        whenever(viewHolderSpy.bindingAdapterPosition) doReturn VALID_ADAPTER_POSITION
        whenever(clipDataItemMock.text) doReturn invalidComponentName
        whenever(clipDataMock.getItemAt(eq(0))) doReturn clipDataItemMock
        whenever(dragEventMock.clipData) doReturn clipDataMock

        dockDragListener.onDrag(viewMock, dragEventMock)

        verify(callbackMock).resetView()
    }

    @Test
    fun onDrag_ACTION_DROP_noDragSurface_returnFalse() {
        whenever(dragEventMock.action) doReturn ACTION_DROP
        whenever(viewHolderSpy.bindingAdapterPosition) doReturn VALID_ADAPTER_POSITION
        whenever(clipDataItemMock.text) doReturn VALID_COMPONENT_NAME
        whenever(clipDataMock.getItemAt(eq(0))) doReturn clipDataItemMock
        whenever(dragEventMock.clipData) doReturn clipDataMock
        whenever(dragEventMock.dragSurface) doReturn null

        val ret = dockDragListener.onDrag(viewMock, dragEventMock)

        assertThat(ret).isFalse()
    }

    @Test
    fun onDrag_ACTION_DROP_noDragSurface_dragAcceptedCallbackTriggered() {
        whenever(dragEventMock.action) doReturn ACTION_DROP
        whenever(viewHolderSpy.bindingAdapterPosition) doReturn VALID_ADAPTER_POSITION
        whenever(clipDataItemMock.text) doReturn VALID_COMPONENT_NAME
        whenever(clipDataMock.getItemAt(eq(0))) doReturn clipDataItemMock
        whenever(dragEventMock.clipData) doReturn clipDataMock
        whenever(dragEventMock.dragSurface) doReturn null

        dockDragListener.onDrag(viewMock, dragEventMock)

        verify(callbackMock).dragAccepted(componentNameCaptor.capture())
        assertThat(componentNameCaptor.firstValue).isNotNull()
        assertThat(componentNameCaptor.firstValue.flattenToString()).isEqualTo(VALID_COMPONENT_NAME)
    }

    @Test
    fun onDrag_ACTION_DROP_validPositionComponentNameDragSurface_returnTrue() {
        whenever(dragEventMock.action) doReturn ACTION_DROP
        whenever(viewHolderSpy.bindingAdapterPosition) doReturn VALID_ADAPTER_POSITION
        whenever(clipDataItemMock.text) doReturn VALID_COMPONENT_NAME
        whenever(clipDataMock.getItemAt(eq(0))) doReturn clipDataItemMock
        whenever(dragEventMock.clipData) doReturn clipDataMock
        whenever(dragEventMock.dragSurface) doReturn surfaceControlMock
        whenever(callbackMock.getDropContainerLocation()).thenReturn(Point(0, 0))
        whenever(callbackMock.getDropLocation()).thenReturn(Point(0, 0))
        whenever(callbackMock.getDropHeight()).thenReturn(10f)
        whenever(callbackMock.getDropWidth()).thenReturn(10f)
        dockDragListener = object : DockDragListener(viewHolderSpy, callbackMock) {
            override fun getAnimator(
                surfaceControl: SurfaceControl,
                fromX: Float,
                fromY: Float,
                toX: Float,
                toY: Float,
                toScaleX: Float,
                toScaleY: Float
            ): ValueAnimator {
                return valueAnimatorMock
            }
        }

        val ret = dockDragListener.onDrag(viewMock, dragEventMock)

        assertThat(ret).isTrue()
    }

    @Test
    fun onDrag_ACTION_DROP_validPositionComponentNameDragSurface_animationStarted() {
        whenever(dragEventMock.action) doReturn ACTION_DROP
        whenever(viewHolderSpy.bindingAdapterPosition) doReturn VALID_ADAPTER_POSITION
        whenever(clipDataItemMock.text) doReturn VALID_COMPONENT_NAME
        whenever(clipDataMock.getItemAt(eq(0))) doReturn clipDataItemMock
        whenever(dragEventMock.clipData) doReturn clipDataMock
        whenever(dragEventMock.dragSurface) doReturn surfaceControlMock
        whenever(callbackMock.getDropContainerLocation()).thenReturn(Point(0, 0))
        whenever(callbackMock.getDropLocation()).thenReturn(Point(0, 0))
        whenever(callbackMock.getDropHeight()).thenReturn(10f)
        whenever(callbackMock.getDropWidth()).thenReturn(10f)
        dockDragListener = object : DockDragListener(viewHolderSpy, callbackMock) {
            override fun getAnimator(
                surfaceControl: SurfaceControl,
                fromX: Float,
                fromY: Float,
                toX: Float,
                toY: Float,
                toScaleX: Float,
                toScaleY: Float
            ): ValueAnimator {
                return valueAnimatorMock
            }
        }

        dockDragListener.onDrag(viewMock, dragEventMock)

        verify(valueAnimatorMock).start()
    }

    @Test
    fun getAnimator_onAnimationUpdate_surfaceControlSetToNewPosition() {
        val updatedX = 10f
        val updatedY = 20f
        val updatedScaleX = 0.9f
        val updatedScaleY = 0.9f
        whenever(
            valueAnimatorMock.getAnimatedValue(DockDragListener.PVH_POSITION_X)
        ) doReturn updatedX
        whenever(
            valueAnimatorMock.getAnimatedValue(DockDragListener.PVH_POSITION_Y)
        ) doReturn updatedY
        whenever(
            valueAnimatorMock.getAnimatedValue(DockDragListener.PVH_SCALE_X)
        ) doReturn updatedScaleX
        whenever(
            valueAnimatorMock.getAnimatedValue(DockDragListener.PVH_SCALE_Y)
        ) doReturn updatedScaleY
        whenever(
            surfaceControlTransactionMock.setPosition(any<SurfaceControl>(), anyFloat(), anyFloat())
        ) doReturn surfaceControlTransactionMock
        whenever(
            surfaceControlTransactionMock.setScale(any<SurfaceControl>(), anyFloat(), anyFloat())
        ) doReturn surfaceControlTransactionMock

        val updateListener = dockDragListener.getAnimatorUpdateListener(
            surfaceControlMock,
            surfaceControlTransactionMock
        )
        updateListener.onAnimationUpdate(valueAnimatorMock)

        verify(surfaceControlTransactionMock)
            .setPosition(eq(surfaceControlMock), eq(updatedX), eq(updatedY))
        verify(surfaceControlTransactionMock).apply()
    }

    @Test
    fun getAnimator_onAnimationUpdate_surfaceControlSetToNewScale() {
        val updatedX = 10f
        val updatedY = 20f
        val updatedScaleX = 0.9f
        val updatedScaleY = 0.9f
        whenever(
            valueAnimatorMock.getAnimatedValue(DockDragListener.PVH_POSITION_X)
        ) doReturn updatedX
        whenever(
            valueAnimatorMock.getAnimatedValue(DockDragListener.PVH_POSITION_Y)
        ) doReturn updatedY
        whenever(
            valueAnimatorMock.getAnimatedValue(DockDragListener.PVH_SCALE_X)
        ) doReturn updatedScaleX
        whenever(
            valueAnimatorMock.getAnimatedValue(DockDragListener.PVH_SCALE_Y)
        ) doReturn updatedScaleY
        whenever(
            surfaceControlTransactionMock.setPosition(any<SurfaceControl>(), anyFloat(), anyFloat())
        ) doReturn surfaceControlTransactionMock
        whenever(
            surfaceControlTransactionMock.setScale(any<SurfaceControl>(), anyFloat(), anyFloat())
        ) doReturn surfaceControlTransactionMock

        val updateListener = dockDragListener.getAnimatorUpdateListener(
            surfaceControlMock,
            surfaceControlTransactionMock
        )
        updateListener.onAnimationUpdate(valueAnimatorMock)

        verify(surfaceControlTransactionMock)
            .setScale(eq(surfaceControlMock), eq(updatedScaleX), eq(updatedScaleY))
        verify(surfaceControlTransactionMock).apply()
    }

    @Test
    fun getAnimatorListener_onAnimationEnd_transactionsClosed() {
        whenever(surfaceControlMock.isValid) doReturn true

        val listener = dockDragListener.getAnimatorListener(
            surfaceControlMock,
            surfaceControlTransactionMock,
            surfaceControlTransactionMock2
        )
        listener.onAnimationEnd(valueAnimatorMock)

        verify(surfaceControlTransactionMock).close()
        verify(surfaceControlTransactionMock2).close()
    }

    @Test
    fun getAnimatorListener_onAnimationCancel_transactionsClosed() {
        whenever(surfaceControlMock.isValid) doReturn true

        val listener = dockDragListener.getAnimatorListener(
            surfaceControlMock,
            surfaceControlTransactionMock,
            surfaceControlTransactionMock2
        )
        listener.onAnimationCancel(valueAnimatorMock)

        verify(surfaceControlTransactionMock).close()
        verify(surfaceControlTransactionMock2).close()
    }

    @Test
    fun getAnimatorListener_onAnimationCancelAndonAnimationEnd_transactionsClosedOnce() {
        whenever(surfaceControlMock.isValid) doReturn true

        val listener = dockDragListener.getAnimatorListener(
            surfaceControlMock,
            surfaceControlTransactionMock,
            surfaceControlTransactionMock2
        )
        listener.onAnimationCancel(valueAnimatorMock)
        listener.onAnimationEnd(valueAnimatorMock)

        verify(surfaceControlTransactionMock).close()
        verify(surfaceControlTransactionMock2).close()
    }

    @Test
    fun getAnimatorListener_onAnimationEnd_surfaceControlHidden() {
        whenever(surfaceControlMock.isValid) doReturn true

        val listener = dockDragListener.getAnimatorListener(
            surfaceControlMock,
            surfaceControlTransactionMock,
            surfaceControlTransactionMock2
        )
        listener.onAnimationEnd(valueAnimatorMock)

        verify(surfaceControlTransactionMock2).hide(eq(surfaceControlMock))
        verify(surfaceControlTransactionMock2).apply()
    }

    @Test
    fun getAnimatorListener_onAnimationCancel_surfaceControlHidden() {
        whenever(surfaceControlMock.isValid) doReturn true

        val listener = dockDragListener.getAnimatorListener(
            surfaceControlMock,
            surfaceControlTransactionMock,
            surfaceControlTransactionMock2
        )
        listener.onAnimationCancel(valueAnimatorMock)

        verify(surfaceControlTransactionMock2).hide(eq(surfaceControlMock))
        verify(surfaceControlTransactionMock2).apply()
    }

    @Test
    fun getAnimatorListener_onAnimationEnd_surfaceControlRemoved() {
        whenever(surfaceControlMock.isValid) doReturn true

        val listener = dockDragListener.getAnimatorListener(
            surfaceControlMock,
            surfaceControlTransactionMock,
            surfaceControlTransactionMock2
        )
        listener.onAnimationEnd(valueAnimatorMock)

        verify(surfaceControlTransactionMock2).remove(eq(surfaceControlMock))
        verify(surfaceControlTransactionMock2).apply()
    }

    @Test
    fun getAnimatorListener_onAnimationCancel_surfaceControlRemoved() {
        whenever(surfaceControlMock.isValid) doReturn true

        val listener = dockDragListener.getAnimatorListener(
            surfaceControlMock,
            surfaceControlTransactionMock,
            surfaceControlTransactionMock2
        )
        listener.onAnimationCancel(valueAnimatorMock)

        verify(surfaceControlTransactionMock2).remove(eq(surfaceControlMock))
        verify(surfaceControlTransactionMock2).apply()
    }
}
