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

/**
 * [RecyclerView.Adapter] used to bind Dock items
 * @param numItems maximum num of items present in the dock
 * @param items initial list of items in the Dock
 */
class DockAdapter(
    private val numItems: Int,
    private val intentDelegate: Consumer<Intent>,
    private val userContext: Context,
    private val items: Array<DockAppItem?> = arrayOfNulls(numItems)
) : RecyclerView.Adapter<DockItemViewHolder>() {
    companion object {
        private val DEBUG = Build.isDebuggable()
        private const val TAG = "DockAdapter"
    }

    private var carPackageManager: CarPackageManager? = null

    enum class PayloadType {
        CHANGE_SAME_ITEM_TYPE,
    }

    override fun onBindViewHolder(
        viewHolder: DockItemViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty() ||
            payloads.getOrNull(0) == null ||
            payloads[0] !is PayloadType
        ) {
            return super.onBindViewHolder(viewHolder, position, payloads)
        }
        when (payloads[0]) {
            PayloadType.CHANGE_SAME_ITEM_TYPE ->
                items[position]?.let {
                    viewHolder.itemTypeChanged(it)
                }
        }
    }

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
     * Pin new app to the given position
     */
    fun pinItemAt(position: Int, componentName: ComponentName) {
        // todo(b/315222570): move to controller
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
     * Pin the DockItem at the given position. If the app is already pinned this call is a no-op.
     */
    fun pinItemAt(position: Int) {
        // todo(b/315222570): move to controller
        changeItemType(position, DockAppItem.Type.STATIC)
    }

    /**
     * Unpin the DockItem at the given position. If the app is already unpinned this call is a
     * no-op.
     */
    fun unpinItemAt(position: Int) {
        // todo(b/315222570): move to controller
        changeItemType(position, DockAppItem.Type.DYNAMIC)
    }

    private fun changeItemType(position: Int, newItemType: DockAppItem.Type) {
        if (!isValidPosition(position) || items[position]?.type == newItemType) {
            return
        }
        items[position]?.let {
            items[position] = it.copy(type = newItemType)
            notifyItemChanged(position, PayloadType.CHANGE_SAME_ITEM_TYPE)
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
