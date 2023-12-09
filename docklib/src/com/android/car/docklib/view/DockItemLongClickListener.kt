package com.android.car.docklib.view

import android.content.res.Resources
import android.view.View
import androidx.annotation.OpenForTesting
import androidx.annotation.VisibleForTesting
import com.android.car.docklib.data.DockAppItem
import com.android.car.dockutil.shortcuts.PinShortcutItem
import com.android.car.ui.shortcutspopup.CarUiShortcutsPopup

/**
 * {@link View.OnLongClickListener} for handling long clicks on dock item.
 * It is responsible to create and show th popup window
 *
 * @param dockAppItem the {@link DockAppItem} to be used on long click.
 * @param pinItemClickDelegate called when item should be pinned at that position
 * @param unpinItemClickDelegate called when item should be unpinned at that position
 */
@OpenForTesting
open class DockItemLongClickListener(
    private var dockAppItem: DockAppItem,
    private val pinItemClickDelegate: Runnable,
    private val unpinItemClickDelegate: Runnable
) : View.OnLongClickListener {
    override fun onLongClick(view: View?): Boolean {
        if (view == null) return false

        createCarUiShortcutsPopupBuilder()
            .addShortcut(
                createPinShortcutItem(
                    view.context.resources,
                    isItemPinned = (dockAppItem.type == DockAppItem.Type.STATIC),
                    pinItemClickDelegate,
                    unpinItemClickDelegate
                )
            )
            .build(view.context, view)
            .show()
        return true
    }

    /**
     * Set the {@link DockAppItem} to be used on long click.
     */
    fun setDockAppItem(dockAppItem: DockAppItem) {
        this.dockAppItem = dockAppItem
    }

    /**
     * Need to be overridden in test.
     */
    @VisibleForTesting
    @OpenForTesting
    open fun createCarUiShortcutsPopupBuilder(): CarUiShortcutsPopup.Builder =
        CarUiShortcutsPopup.Builder()

    /**
     * Need to be overridden in test.
     */
    @VisibleForTesting
    fun createPinShortcutItem(
        resources: Resources,
        isItemPinned: Boolean,
        pinItemClickDelegate: Runnable,
        unpinItemClickDelegate: Runnable
    ): PinShortcutItem = PinShortcutItem(
        resources,
        isItemPinned,
        pinItemClickDelegate,
        unpinItemClickDelegate
    )
}
