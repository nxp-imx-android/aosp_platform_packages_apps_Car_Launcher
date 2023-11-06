package com.android.car.docklib.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.car.docklib.R
import com.android.car.docklib.data.DockAppItem

class DockAdapter(private val numItems: Int) : RecyclerView.Adapter<DockItemViewHolder>() {

    private val items: Array<DockAppItem?> = arrayOfNulls(numItems)

    override fun onCreateViewHolder(parent: ViewGroup, p1: Int): DockItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.dock_app_item_view, parent)
        return DockItemViewHolder(view)
    }

    override fun getItemCount() = numItems

    override fun onBindViewHolder(viewHolder: DockItemViewHolder, position: Int) {
        viewHolder.bind(items[position])
    }

    fun setItems(items: List<DockAppItem?>) {
        for (i in 0..<numItems) {
            if (this.items[i] != items.getOrNull(i)) {
                this.items[i] = items.getOrNull(i)
                notifyItemChanged(i)
            }
        }
    }
}
