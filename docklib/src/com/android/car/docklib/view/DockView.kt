package com.android.car.docklib.view

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.android.car.docklib.R

class DockView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    val recyclerView: RecyclerView

    init {
        inflate(context, R.layout.dock_view, this)
        recyclerView = requireViewById(R.id.recycler_view)
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
    }
}
