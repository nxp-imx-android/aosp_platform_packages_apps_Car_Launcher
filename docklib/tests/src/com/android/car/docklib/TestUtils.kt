package com.android.car.docklib

import android.content.ComponentName
import android.graphics.drawable.Drawable
import com.android.car.docklib.data.DockAppItem
import org.mockito.Mockito.mock

object TestUtils {

    private val icon = mock(Drawable::class.java)

    /** Create a hardcoded dock item with optional fields */
    fun createAppItem(
        type: DockAppItem.Type = DockAppItem.Type.DYNAMIC,
        app: String = "app",
        pkg: String = app,
        clazz: String = app,
        name: String = "app",
        icon: Drawable = this.icon,
        isDrivingOptimized: Boolean = true
    ): DockAppItem {
        return DockAppItem(type, ComponentName(pkg, clazz), name, icon, isDrivingOptimized)
    }
}
