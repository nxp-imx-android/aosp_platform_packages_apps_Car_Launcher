package com.android.car.docklib

import android.content.ComponentName
import android.graphics.drawable.Drawable
import com.android.car.docklib.data.DockAppItem
import com.android.car.docklib.data.DockItemId
import java.util.UUID
import org.mockito.kotlin.mock

object TestUtils {
    /** Create a hardcoded dock item with optional fields */
    fun createAppItem(
            id: @DockItemId UUID = UUID.randomUUID(),
            type: DockAppItem.Type = DockAppItem.Type.DYNAMIC,
            app: String = "app",
            pkg: String = app,
            clazz: String = app,
            name: String = "app",
            component: ComponentName = ComponentName(pkg, clazz),
            icon: Drawable = mock<Drawable>(),
            isDrivingOptimized: Boolean = true
    ): DockAppItem {
        return DockAppItem(id, type, component, name, icon, isDrivingOptimized)
    }
}
