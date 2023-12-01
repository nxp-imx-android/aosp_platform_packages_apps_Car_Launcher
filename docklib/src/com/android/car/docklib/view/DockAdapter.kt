package com.android.car.docklib.view

import android.car.content.pm.CarPackageManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.car.docklib.R
import com.android.car.docklib.data.DockAppItem
import java.util.function.Consumer

class DockAdapter(
    private val numItems: Int,
    private val intentDelegate: Consumer<Intent>,
    private val userContext: Context,
) : RecyclerView.Adapter<DockItemViewHolder>() {
    companion object {
        private val DEBUG = Build.isDebuggable()
        private const val TAG = "DockAdapter"
    }

    private val items: Array<DockAppItem?> = arrayOfNulls(numItems)
    private var carPackageManager: CarPackageManager? = null

    override fun onCreateViewHolder(parent: ViewGroup, p1: Int): DockItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.dock_app_item_view, // resource
            parent,
            false // attachToRoot
        )
        return DockItemViewHolder(view, intentDelegate)
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

    /**
     * Pin app to the given position
     */
    fun pinItemAt(position: Int, componentName: ComponentName) {
        if (!isValidPosition(position)) {
            return
        }
        try {
            val ai = userContext.packageManager
                .getActivityInfo(componentName, PackageManager.ComponentInfoFlags.of(0L))
            items[position] = DockAppItem(
                DockAppItem.Type.STATIC,
                componentName,
                ai.name,
                ai.loadIcon(userContext.packageManager),
                carPackageManager?.isActivityDistractionOptimized(
                    componentName.packageName,
                    componentName.className
                ) ?: false
            )
            notifyItemChanged(position)
        } catch (e: NameNotFoundException) {
            if (DEBUG) {
                // don't need to crash for a failed pin, log error instead
                Log.e(TAG, "Component $componentName not found, pinning failed $e")
            }
        }
    }

    /**
     * Setter for CarPackageManager
     */
    fun setCarPackageManager(carPackageManager: CarPackageManager) {
        this.carPackageManager = carPackageManager
    }

    private fun isValidPosition(position: Int): Boolean {
        return position >= 0 && position < items.size
    }
}
