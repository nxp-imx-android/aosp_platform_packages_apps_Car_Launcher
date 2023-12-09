package com.android.car.dockutil.shortcuts

import android.content.res.Resources
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class PinShortcutItemTest {
    private val resourcesMock = mock<Resources> {}
    private val pinItemClickDelegateMock = mock<Runnable> {}
    private val unpinItemClickDelegateMock = mock<Runnable> {}

    @Test
    fun onClick_pinnedItem_runUnpinDelegate() {
        val pinShortcutItem = PinShortcutItem(
            resourcesMock,
            isItemPinned = true,
            pinItemClickDelegateMock,
            unpinItemClickDelegateMock
        )

        pinShortcutItem.onClick()

        verify(unpinItemClickDelegateMock).run()
    }

    @Test
    fun onClick_unpinnedItem_runPinDelegate() {
        val pinShortcutItem = PinShortcutItem(
            resourcesMock,
            isItemPinned = false,
            pinItemClickDelegateMock,
            unpinItemClickDelegateMock
        )

        pinShortcutItem.onClick()

        verify(pinItemClickDelegateMock).run()
    }
}
