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

    private val iconStrokeWidth: Int
    private val excitedIconStrokeWidth: Int
    private val iconPadding: Int
    private val excitedIconPadding: Int
    private val iconStrokeColor: Int
    private val excitedIconStrokeColor: Int
    private val appIcon: ShapeableImageView

    init {
        appIcon = itemView.requireViewById(R.id.dock_app_icon)
        iconStrokeWidth = itemView.resources.getDimensionPixelSize(R.dimen.icon_stroke_width)
        iconPadding = iconStrokeWidth / 2
        excitedIconStrokeWidth =
            itemView.resources.getDimensionPixelSize(R.dimen.icon_stroke_width_excited)
        excitedIconPadding = excitedIconStrokeWidth / 2
        iconStrokeColor = itemView.resources.getColor(
            R.color.icon_default_stroke_color,
            null // theme
        )
        excitedIconStrokeColor = itemView.resources.getColor(
            R.color.icon_excited_stroke_color,
            null // theme
        )
    }

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
                            (iconLocation[0] + iconStrokeWidth),
                            (iconLocation[1] + iconStrokeWidth)
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

    private fun pinNewItem(componentName: ComponentName) {
        (bindingAdapter as? DockAdapter)?.pinItemAt(bindingAdapterPosition, componentName)
    }

    private fun exciteAppIcon() {
        // todo(b/312737692): add animations
        appIcon.strokeColor = ColorStateList.valueOf(excitedIconStrokeColor)
        appIcon.setColorFilter(Color.argb(0.3f, 0f, 0f, 0f), PorterDuff.Mode.DARKEN)
        appIcon.strokeWidth = excitedIconStrokeWidth.toFloat()
        appIcon.setPadding(excitedIconStrokeWidth / 2)
        appIcon.invalidate()
    }

    private fun resetAppIcon() {
        appIcon.strokeColor = ColorStateList.valueOf(iconStrokeColor)
        appIcon.colorFilter = null
        appIcon.strokeWidth = iconStrokeWidth.toFloat()
        appIcon.setPadding(iconStrokeWidth / 2)
        appIcon.invalidate()
    }

    private fun reset() {
        resetAppIcon()
        appIcon.contentDescription = null
        appIcon.setImageDrawable(null)
        appIcon.setOnClickListener(null)
        itemView.setOnDragListener(null)
    }

    // TODO: b/301484526 Add animation when app icon is changed
}
