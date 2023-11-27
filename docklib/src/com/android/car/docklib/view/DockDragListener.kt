package com.android.car.docklib.view

import android.content.ClipData
import android.content.ComponentName
import android.graphics.Point
import android.os.Build
import android.util.Log
import android.view.DragEvent
import android.view.SurfaceControl
import android.view.SurfaceControl.Transaction
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.core.animation.Animator
import androidx.core.animation.PropertyValuesHolder
import androidx.core.animation.ValueAnimator
import androidx.recyclerview.widget.RecyclerView
import com.android.car.docklib.R
import java.lang.Exception
import java.lang.IndexOutOfBoundsException

/**
 * {@link View.OnDragListener} for Dock. Receives a drop and moves it to correct location,
 * transformed to the given size. This should be applied to all individual items in the dock that
 * wants to receive a drop.
 */
open class DockDragListener(
    private val viewHolder: RecyclerView.ViewHolder,
    private val callback: Callback
) : View.OnDragListener {
    companion object {
        @VisibleForTesting
        const val APP_ITEM_DRAG_TAG = "com.android.car.launcher.APP_ITEM_DRAG_TAG"

        @VisibleForTesting
        const val PVH_POSITION_X = "PVH_POSITION_X"

        @VisibleForTesting
        const val PVH_POSITION_Y = "PVH_POSITION_Y"

        @VisibleForTesting
        const val PVH_SCALE_X = "PVH_SCALE_X"

        @VisibleForTesting
        const val PVH_SCALE_Y = "PVH_SCALE_Y"

        private const val TAG = "DockDragListener"
        private val DEBUG = Build.isDebuggable()
    }

    private val animateInDuration: Int

    init {
        val resources = viewHolder.itemView.context.resources
        animateInDuration = resources.getInteger(R.integer.drag_drop_animate_in_duration)
    }

    override fun onDrag(view: View, dragEvent: DragEvent): Boolean {
        when (dragEvent.action) {
            DragEvent.ACTION_DRAG_STARTED ->
                return APP_ITEM_DRAG_TAG.contentEquals(dragEvent.clipDescription?.label)

            DragEvent.ACTION_DRAG_ENTERED -> {
                callback.exciteView()
                return true
            }

            DragEvent.ACTION_DRAG_EXITED -> {
                callback.resetView()
                return true
            }

            DragEvent.ACTION_DROP -> {
                if (viewHolder.bindingAdapterPosition == RecyclerView.NO_POSITION) {
                    if (DEBUG) Log.d(TAG, "Drop at invalid position")
                    callback.resetView()
                    return false
                }
                if (DEBUG) Log.d(TAG, "Drop at position: " + viewHolder.bindingAdapterPosition)

                val item: ClipData.Item
                try {
                    item = dragEvent.clipData.getItemAt(0)
                    if (item.text == null) throw NullPointerException("ClipData item text is null")
                } catch (e: Exception) {
                    when (e) {
                        is IndexOutOfBoundsException, is NullPointerException -> {
                            if (DEBUG) Log.d(TAG, "No/Invalid clipData sent with the drop: $e")
                            callback.resetView()
                            return false
                        }

                        else -> {
                            throw e
                        }
                    }
                }

                val component: ComponentName? =
                    ComponentName.unflattenFromString(item.text.toString())
                if (component == null) {
                    if (DEBUG) {
                        Log.d(TAG, "Invalid component string sent with drop: " + item.text)
                    }
                    callback.resetView()
                    return false
                }
                if (DEBUG) Log.v(TAG, "Dropped component: $component")

                // todo(b/312718542): hidden api(dragEvent.dragSurface) usage
                dragEvent.dragSurface?.let {
                    callback.dragAccepted(component)
                    animateSurfaceIn(it, dragEvent)
                    return true
                } ?: run {
                    if (DEBUG) Log.d(TAG, "Could not retrieve the drag surface")
                    // drag is success but animation is not possible since there is no dragSurface
                    callback.dragAccepted(component)
                    return false
                }
            }

            DragEvent.ACTION_DRAG_ENDED -> {
                if (!dragEvent.result) {
                    // if drop was accepted the drop action should handle resetting
                    callback.resetView()
                }
                return true
            }
        }
        return false
    }

    /**
     * Animates the surface from where the it was dropped to the final position and size. Also
     * responsible for cleaning up the surface after the animation.
     */
    private fun animateSurfaceIn(
        surfaceControl: SurfaceControl,
        dragEvent: DragEvent,
    ) {
        val dropContainerLocation = callback.getDropContainerLocation()
        // todo(b/312718542): hidden api(offsetX and offsetY) usage
        val fromX: Float = dropContainerLocation.x + (dragEvent.x - dragEvent.offsetX)
        val fromY: Float = dropContainerLocation.y + (dragEvent.y - dragEvent.offsetY)

        val dropLocation = callback.getDropLocation()
        val toX: Float = dropLocation.x.toFloat()
        val toY: Float = dropLocation.y.toFloat()

        val toScaleX: Float = callback.getDropWidth() / surfaceControl.width
        val toScaleY: Float = callback.getDropHeight() / surfaceControl.height

        getAnimator(surfaceControl, fromX, fromY, toX, toY, toScaleX, toScaleY).start()
    }

    /**
     * Get the animator responsible for animating the {@code surfaceControl} from
     * {@code fromX, fromY} to its final position {@code toX, toY} with correct scale
     * {@code toScaleX, toScaleY}.
     * Default values are added to make this method easier to test. Generally all parameters are
     * expected to be sent by the caller.
     */
    @VisibleForTesting
    open fun getAnimator(
        surfaceControl: SurfaceControl,
        fromX: Float = 0f,
        fromY: Float = 0f,
        toX: Float = 0f,
        toY: Float = 0f,
        toScaleX: Float = 1f,
        toScaleY: Float = 1f
    ): ValueAnimator {
        val pvhX: PropertyValuesHolder =
            PropertyValuesHolder.ofFloat(PVH_POSITION_X, fromX, toX)
        val pvhY: PropertyValuesHolder =
            PropertyValuesHolder.ofFloat(PVH_POSITION_Y, fromY, toY)
        val pvhScaleX =
            PropertyValuesHolder.ofFloat(PVH_SCALE_X, 1f, toScaleX)
        val pvhScaleY =
            PropertyValuesHolder.ofFloat(PVH_SCALE_Y, 1f, toScaleY)

        val animator: ValueAnimator =
            ValueAnimator.ofPropertyValuesHolder(
                pvhX,
                pvhY,
                pvhScaleX,
                pvhScaleY
            )
        animator.setDuration(animateInDuration.toLong())
        val trx = Transaction()
        animator.addUpdateListener(getAnimatorUpdateListener(surfaceControl, trx))
        animator.addListener(
            getAnimatorListener(surfaceControl, trx, cleanupTrx = Transaction())
        )
        return animator
    }

    /**
     * Not expected to be used directly or overridden.
     *
     * @param trx Transaction used to animate the {@code SurfaceControl} in place.
     */
    @VisibleForTesting
    fun getAnimatorUpdateListener(
        surfaceControl: SurfaceControl,
        trx: Transaction
    ): Animator.AnimatorUpdateListener {
        return Animator.AnimatorUpdateListener {
                updatedAnimation ->
            if (updatedAnimation is ValueAnimator) {
                trx.setPosition(
                    surfaceControl,
                    updatedAnimation.getAnimatedValue(PVH_POSITION_X) as Float,
                    updatedAnimation.getAnimatedValue(PVH_POSITION_Y) as Float
                ).setScale(
                    surfaceControl,
                    updatedAnimation.getAnimatedValue(PVH_SCALE_X) as Float,
                    updatedAnimation.getAnimatedValue(PVH_SCALE_Y) as Float
                ).apply()
            }
        }
    }

    /**
     * Not expected to be used directly or overridden.
     * {@code trx} and {@code cleanupTrx} will be closed by this listener.
     *
     * @param trx Transaction used to animate the {@code SurfaceControl} in place.
     * @param cleanupTrx Transaction used to animate out and hide {@code SurfaceControl}
     */
    @VisibleForTesting
    fun getAnimatorListener(
        surfaceControl: SurfaceControl,
        trx: Transaction,
        cleanupTrx: Transaction
    ): Animator.AnimatorListener {
        return object : Animator.AnimatorListener {
            private var isCancelled = false
            override fun onAnimationStart(var1: Animator) {
                isCancelled = false
            }

            override fun onAnimationEnd(var1: Animator) {
                if (!isCancelled) {
                    cleanup()
                }
            }

            override fun onAnimationCancel(var1: Animator) {
                isCancelled = true
                cleanup()
            }

            override fun onAnimationRepeat(var1: Animator) {
                // no-op
            }

            fun cleanup() {
                trx.close()
                if (surfaceControl.isValid) {
                    // todo(b/312737692): add animations
                    cleanupTrx.hide(surfaceControl)
                    cleanupTrx.remove(surfaceControl)
                    cleanupTrx.apply()
                    cleanupTrx.close()
                }
            }
        }
    }

    /**
     * {@link DockDragListener} communicates events back and requests data from the caller using
     * this callback.
     */
    interface Callback {
        /**
         * Drag is accepted/successful for the {@code componentName}
         */
        fun dragAccepted(componentName: ComponentName) {}

        /**
         * Excite the view to indicate the item can be dropped in this position when dragged inside
         * the drop bounds.
         */
        fun exciteView() {}

        /**
         * Reset the view after a drop or if the drop failed or if the item is dragged outside the
         * drop bounds.
         */
        fun resetView() {}

        /**
         * Get the location of the container that holds the dropped item
         */
        fun getDropContainerLocation(): Point

        /**
         * Get the final location of the dropped item
         */
        fun getDropLocation(): Point

        /**
         * Get the final width of the dropped item
         */
        fun getDropWidth(): Float

        /**
         * Get the final height of the dropped item
         */
        fun getDropHeight(): Float
    }
}
