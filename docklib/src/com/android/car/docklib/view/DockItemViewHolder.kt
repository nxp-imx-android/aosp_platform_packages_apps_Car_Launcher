package com.android.car.docklib.view

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.android.car.docklib.R
import com.android.car.docklib.data.DockAppItem

class DockItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val appIcon = itemView.requireViewById<ImageView>(R.id.dock_app_icon)

    fun bind(dockAppItem: DockAppItem?) {
        reset()
        if (dockAppItem == null) return

        appIcon.contentDescription = dockAppItem.name
        appIcon.setImageDrawable(dockAppItem.icon)
        // TODO: b/301483072 add click delegate
    }

    private fun reset() {
        appIcon.contentDescription = null
        appIcon.setImageDrawable(null)
    }

    // TODO: b/301484526 Add animation when app icon is changed
}
