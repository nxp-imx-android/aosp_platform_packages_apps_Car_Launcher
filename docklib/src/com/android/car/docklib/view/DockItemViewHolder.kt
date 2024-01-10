/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.car.docklib.view

import android.content.ComponentName
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Point
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build
import android.util.Log
import android.util.TypedValue
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.animation.Animator
import androidx.recyclerview.widget.RecyclerView
import com.android.car.docklib.DockInterface
import com.android.car.docklib.R
import com.android.car.docklib.data.DockAppItem
import com.android.car.docklib.view.animation.ExcitementAnimationHelper
import com.google.android.material.imageview.ShapeableImageView
import kotlin.math.floor

class DockItemViewHolder(
        private val dockController: DockInterface,
        itemView: View,
) : RecyclerView.ViewHolder(itemView) {

    companion object {
        private const val TAG = "DockItemViewHolder"
        private val DEBUG = Build.isDebuggable()
        private const val INITIAL_COLOR_FILTER_ALPHA = 0f
    }

    private val iconStrokeWidth: Float
    private val excitedIconStrokeWidth: Float
    private val defaultIconStrokeColor: Int
    private val staticIconStrokeColor: Int
    private val excitedIconStrokeColor: Int
    private val excitedIconColorFilterAlpha: Float
    private val appIcon: ShapeableImageView
    private val exciteAnimationDuration: Int
    private val dockDragListener: DockDragListener
    private var dockItemLongClickListener: DockItemLongClickListener? = null
    private var exciteAnimator: Animator? = null
    private var iconStrokeColor: Int

    init {
        iconStrokeWidth = itemView.resources.getDimension(R.dimen.icon_stroke_width)
        excitedIconStrokeWidth = itemView.resources.getDimension(R.dimen.icon_stroke_width_excited)
        defaultIconStrokeColor = itemView.resources.getColor(
                R.color.icon_default_stroke_color,
                null // theme
        )
        staticIconStrokeColor = itemView.resources.getColor(
                R.color.icon_static_stroke_color,
                null // theme
        )
        iconStrokeColor = defaultIconStrokeColor
        excitedIconStrokeColor = itemView.resources.getColor(
                R.color.icon_excited_stroke_color,
                null // theme
        )
        appIcon = itemView.requireViewById(R.id.dock_app_icon)
        val typedValue = TypedValue()
        itemView.resources.getValue(
                R.dimen.icon_colorFilter_alpha_excited,
                typedValue,
                true // resolveRefs
        )
        this.excitedIconColorFilterAlpha = typedValue.float
        exciteAnimationDuration =
                itemView.resources.getInteger(R.integer.excite_icon_animation_duration_ms)

        dockDragListener = DockDragListener(
                resources = this.itemView.resources,
                object : DockDragListener.Callback {
                    override fun dropSuccessful(
                            componentName: ComponentName,
                            cleanupCallback: Runnable?
                    ) {
                        (bindingAdapter as? DockAdapter)
                                ?.setCallback(bindingAdapterPosition, cleanupCallback)
                        dockController.appPinned(componentName, bindingAdapterPosition)
                    }

                    override fun dropAnimationScaleDownComplete(componentName: ComponentName) {
                        // todo: strokeColor should be primary color of the new pinned app
                        val highlightColor = Color.rgb(240, 126, 117)
                        updateAppIcon(
                                strokeColor = highlightColor,
                                excitedIconStrokeWidth,
                                colorFilterAlpha = null,
                                colorFilterColor = highlightColor
                        )
                    }

                    override fun exciteView() {
                        animateAppIconExcited(isExciting = true)
                    }

                    override fun resetView() {
                        animateAppIconExcited(isExciting = false)
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
                })
    }

    /**
     * @param callback [Runnable] to be called after the new item is bound
     */
    fun bind(dockAppItem: DockAppItem, callback: Runnable? = null) {
        reset()

        itemTypeChanged(dockAppItem)

        appIcon.contentDescription = dockAppItem.name
        appIcon.setImageDrawable(dockAppItem.icon)
        appIcon.post { callback?.run() }
        appIcon.setOnClickListener { dockController.launchApp(dockAppItem.component) }
        dockItemLongClickListener = DockItemLongClickListener(
                dockAppItem,
                pinItemClickDelegate = { dockController.appPinned(dockAppItem.id) },
                unpinItemClickDelegate = { dockController.appUnpinned(dockAppItem.id) },
        )
        appIcon.onLongClickListener = dockItemLongClickListener

        itemView.setOnDragListener(dockDragListener)
    }

    fun itemTypeChanged(dockAppItem: DockAppItem) {
        // todo(b/314859977): dynamic strokeColor should be decided by the app primary color
        iconStrokeColor = when (dockAppItem.type) {
            DockAppItem.Type.STATIC -> staticIconStrokeColor
            DockAppItem.Type.DYNAMIC -> defaultIconStrokeColor
        }
        updateAppIcon(strokeColor = iconStrokeColor)
        dockItemLongClickListener?.setDockAppItem(dockAppItem)
    }

    /**
     * Animate the app icon to be excited or reset after being excited.
     * @param isExciting {@code true} if the view is being excited, {@code false} if view is being
     * reset
     */
    private fun animateAppIconExcited(isExciting: Boolean) {
        val isAnimationOngoing = exciteAnimator?.isRunning ?: false
        if (DEBUG) {
            Log.d(
                    TAG,
                    "Excite animation{ isExciting: $isExciting, " +
                            "isAnimationOngoing: $isAnimationOngoing }"
            )
        }
        exciteAnimator?.cancel()

        val toStrokeWidth: Float
        val toContentPadding: Int
        val toColorFilterAlpha: Float
        val toStrokeColor: Int
        val successCallback: Function<Unit>
        if (isExciting) {
            toStrokeWidth = excitedIconStrokeWidth
            toContentPadding = getContentPaddingFromStrokeWidth(excitedIconStrokeWidth)
            toColorFilterAlpha = excitedIconColorFilterAlpha
            toStrokeColor = excitedIconStrokeColor
            successCallback = {
                exciteAnimator = null
                exciteAppIcon()
            }
        } else {
            toStrokeWidth = iconStrokeWidth
            toContentPadding = getContentPaddingFromStrokeWidth(iconStrokeWidth)
            toColorFilterAlpha = INITIAL_COLOR_FILTER_ALPHA
            toStrokeColor = iconStrokeColor
            successCallback = {
                exciteAnimator = null
                resetAppIcon()
            }
        }
        val failureCallback = { exciteAnimator = null }

        exciteAnimator = ExcitementAnimationHelper.getExcitementAnimator(
                appIcon,
                exciteAnimationDuration.toLong(),
                toStrokeWidth,
                toStrokeColor,
                toContentPadding,
                toColorFilterAlpha,
                successCallback,
                failureCallback
        )
        exciteAnimator?.start()
    }

    private fun exciteAppIcon() {
        updateAppIcon(
                excitedIconStrokeColor,
                excitedIconStrokeWidth,
                getContentPaddingFromStrokeWidth(excitedIconStrokeWidth),
                excitedIconColorFilterAlpha
        )
    }

    private fun resetAppIcon() {
        updateAppIcon(iconStrokeColor, iconStrokeWidth)
    }

    /**
     * null colorFilterAlpha and null colorFilterColor value results in colorFilter to be null
     */
    private fun updateAppIcon(
            strokeColor: Int,
            strokeWidth: Float? = null,
            contentPadding: Int? = null,
            colorFilterAlpha: Float? = null,
            @ColorInt colorFilterColor: Int? = null,
    ) {
        if (DEBUG) {
            Log.d(
                    TAG,
                    "updateAppIcon at $bindingAdapterPosition { " +
                            "strokeColor: $strokeColor " +
                            "strokeWidth: $strokeWidth " +
                            "contentPadding: $contentPadding " +
                            "colorFilterAlpha: $colorFilterAlpha " +
                            "colorFilterColor: $colorFilterColor" +
                            "}"
            )
        }
        appIcon.strokeColor = ColorStateList.valueOf(strokeColor)
        val sw = strokeWidth ?: appIcon.strokeWidth
        appIcon.strokeWidth = sw
        val cp = contentPadding ?: getContentPaddingFromStrokeWidth(sw)
        appIcon.setContentPadding(cp, cp, cp, cp)
        val cfc = colorFilterColor ?: colorFilterAlpha?.let { Color.argb(it, 0f, 0f, 0f) }
        appIcon.colorFilter =
                cfc?.let { color -> PorterDuffColorFilter(color, PorterDuff.Mode.DARKEN) }
        appIcon.invalidate()
    }

    private fun reset() {
        exciteAnimator?.cancel()
        exciteAnimator = null
        appIcon.contentDescription = null
        appIcon.setImageDrawable(null)
        appIcon.setOnClickListener(null)
        resetAppIcon()
        itemView.setOnDragListener(null)
    }

    private fun getContentPaddingFromStrokeWidth(strokeWidth: Float): Int =
            floor(strokeWidth / 2).toInt()

    // TODO: b/301484526 Add animation when app icon is changed
}
