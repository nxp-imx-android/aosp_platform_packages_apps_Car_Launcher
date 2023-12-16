package com.android.car.docklib.view

import android.content.ComponentName
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Point
import android.graphics.PorterDuff
import android.view.View
import androidx.core.view.setPadding
import androidx.recyclerview.widget.RecyclerView
import com.android.car.docklib.R
import com.android.car.docklib.data.DockAppItem
import com.google.android.material.imageview.ShapeableImageView
import java.util.function.Consumer

class DockItemViewHolder(
    itemView: View,
    private val intentDelegate: Consumer<Intent>,
) : RecyclerView.ViewHolder(itemView) {

    companion object {
        private const val DEFAULT_STROKE_WIDTH = 0f
    }

    private val staticIconStrokeWidth: Float
    private val dynamicIconStrokeWidth: Float
    private val excitedIconStrokeWidth: Float
    private val iconStrokeColor: Int
    private val excitedIconStrokeColor: Int
    private val appIcon: ShapeableImageView
    private var dockItemLongClickListener: DockItemLongClickListener? = null
    private var iconStrokeWidth: Float = DEFAULT_STROKE_WIDTH

    init {
        staticIconStrokeWidth = itemView.resources.getDimension(R.dimen.static_icon_stroke_width)
        dynamicIconStrokeWidth = itemView.resources.getDimension(R.dimen.dynamic_icon_stroke_width)
        excitedIconStrokeWidth = itemView.resources.getDimension(R.dimen.icon_stroke_width_excited)
        // todo(b/314859977): iconStrokeColor should be decided by the app primary color
        iconStrokeColor = itemView.resources.getColor(
            R.color.icon_default_stroke_color,
            null // theme
        )
        excitedIconStrokeColor = itemView.resources.getColor(
            R.color.icon_excited_stroke_color,
            null // theme
        )
        appIcon = itemView.requireViewById(R.id.dock_app_icon)
    }

    fun bind(dockAppItem: DockAppItem?) {
        reset()
        if (dockAppItem == null) return

        itemTypeChanged(dockAppItem)

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
        dockItemLongClickListener = DockItemLongClickListener(
            dockAppItem,
            pinItemClickDelegate =
            { (bindingAdapter as? DockAdapter)?.pinItemAt(bindingAdapterPosition) },
            unpinItemClickDelegate =
            { (bindingAdapter as? DockAdapter)?.unpinItemAt(bindingAdapterPosition) }
        )
        appIcon.onLongClickListener = dockItemLongClickListener

        itemView.setOnDragListener(
            DockDragListener(
                viewHolder = this,
                object : DockDragListener.Callback {
                    override fun dragAccepted(componentName: ComponentName) {
                        pinNewItem(componentName)
                    }

                    override fun exciteView() {
                        exciteAppIcon()
                    }

                    override fun resetView() {
                        resetAppIcon()
                    }

                    override fun getDropContainerLocation(): Point {
                        val containerLocation = itemView.locationOnScreen
                        return Point(containerLocation[0], containerLocation[1])
                    }

                    override fun getDropLocation(): Point {
                        val iconLocation = appIcon.locationOnScreen
                        return Point(
                            (iconLocation[0] + iconStrokeWidth.toInt()),
                            (iconLocation[1] + iconStrokeWidth.toInt())
                        )
                    }

                    override fun getDropWidth(): Float {
                        return (appIcon.width.toFloat() - iconStrokeWidth * 2)
                    }

                    override fun getDropHeight(): Float {
                        return (appIcon.height.toFloat() - iconStrokeWidth * 2)
                    }
                }
            )
        )
    }

    fun itemTypeChanged(dockAppItem: DockAppItem) {
        iconStrokeWidth = when (dockAppItem.type) {
            DockAppItem.Type.STATIC -> staticIconStrokeWidth
            DockAppItem.Type.DYNAMIC -> dynamicIconStrokeWidth
        }
        appIcon.strokeWidth = iconStrokeWidth

        appIcon.invalidate()
        dockItemLongClickListener?.setDockAppItem(dockAppItem)
    }

    private fun pinNewItem(componentName: ComponentName) {
        (bindingAdapter as? DockAdapter)?.pinItemAt(bindingAdapterPosition, componentName)
    }

    private fun exciteAppIcon() {
        // todo(b/312737692): add animations
        appIcon.strokeColor = ColorStateList.valueOf(excitedIconStrokeColor)
        appIcon.setColorFilter(Color.argb(0.3f, 0f, 0f, 0f), PorterDuff.Mode.DARKEN)
        appIcon.strokeWidth = excitedIconStrokeWidth
        appIcon.setPadding(getPaddingFromStrokeWidth(excitedIconStrokeWidth))
        appIcon.invalidate()
    }

    private fun resetAppIcon() {
        appIcon.strokeColor = ColorStateList.valueOf(iconStrokeColor)
        appIcon.colorFilter = null
        appIcon.strokeWidth = iconStrokeWidth
        appIcon.setPadding(getPaddingFromStrokeWidth(iconStrokeWidth))
        appIcon.invalidate()
    }

    private fun reset() {
        iconStrokeWidth = DEFAULT_STROKE_WIDTH
        resetAppIcon()
        appIcon.contentDescription = null
        appIcon.setImageDrawable(null)
        appIcon.setOnClickListener(null)
        itemView.setOnDragListener(null)
    }

    private fun getPaddingFromStrokeWidth(strokeWidth: Float): Int = (strokeWidth / 2).toInt()

    // TODO: b/301484526 Add animation when app icon is changed
}
