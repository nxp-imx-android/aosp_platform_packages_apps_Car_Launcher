package com.android.car.docklib.view

import android.content.Intent
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.android.car.docklib.R
import com.android.car.docklib.data.DockAppItem
import java.util.function.Consumer

class DockItemViewHolder(itemView: View, private val intentDelegate: Consumer<Intent>) :
    RecyclerView.ViewHolder(itemView) {

    private val appIcon = itemView.requireViewById<ImageView>(R.id.dock_app_icon)

    fun bind(dockAppItem: DockAppItem?) {
        reset()
        if (dockAppItem == null) return

        appIcon.contentDescription = dockAppItem.name
        appIcon.setImageDrawable(dockAppItem.icon)
        appIcon.setOnClickListener {
            val intent =
                Intent(Intent.ACTION_MAIN)
                    .setComponent(dockAppItem.component)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intentDelegate.accept(intent)
        }
    }

    private fun reset() {
        appIcon.contentDescription = null
        appIcon.setImageDrawable(null)
        appIcon.setOnClickListener(null)
    }

    // TODO: b/301484526 Add animation when app icon is changed
}
