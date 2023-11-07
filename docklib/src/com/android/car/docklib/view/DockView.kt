package com.android.car.docklib.view

import android.content.ComponentName
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.android.car.docklib.R
import com.android.car.docklib.data.DockAppItem

class DockView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val numItems: Int

    init {
        inflate(context, R.layout.dock_view, this)
        val recyclerView: RecyclerView = requireViewById(R.id.recycler_view)
        recyclerView.addItemDecoration(
            object : ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    with(outRect) {
                        if (parent.getChildAdapterPosition(view) != 0) {
                            // TODO: b/301484526 set margins in case of RTL and vertical
                            left = resources.getDimensionPixelSize(R.dimen.dock_item_spacing)
                        }
                    }
                }
            }
        )

        // TODO: b/301483072 Move logic to ViewController
        numItems = context.resources.getInteger(R.integer.config_numDockApps)
        val adapter = DockAdapter(numItems)
        recyclerView.adapter = adapter
        adapter.setItems(getHardcodedApps())
    }

    // TODO: remove in next CL
    private fun getHardcodedApps() =
        List(
            numItems,
            init = {
                val componentName =
                    ComponentName(
                        "com.android.car.settings",
                        "com.android.car.settings.Settings_Launcher_Homepage"
                    )
                DockAppItem(
                    DockAppItem.Type.DYNAMIC,
                    componentName,
                    "Settings",
                    context.packageManager.getActivityIcon(componentName),
                    isDistractionOptimized = true
                )
            }
        )
}
