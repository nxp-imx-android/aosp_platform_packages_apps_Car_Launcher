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

package com.android.car.docklib

import android.car.Car
import android.car.content.pm.CarPackageManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.android.car.docklib.events.DockEventsReceiver
import com.android.car.docklib.task.DockTaskStackChangeListener
import com.android.car.docklib.view.DockAdapter
import com.android.car.docklib.view.DockView
import com.android.systemui.shared.system.TaskStackChangeListeners
import java.lang.ref.WeakReference
import java.util.function.Consumer

/**
 * Create a controller for DockView. It initializes the view with default and persisted icons. Upon
 * initializing, it will listen to broadcast events, and update the view.
 *
 * @param userContext the foreground user context, since the view may be hosted on system context
 * @param dockView the inflated dock view
 * @param intentDelegate the system context will need to handle clicks and actions on the icons
 */
class DockViewController(
    private val userContext: Context,
    dockView: DockView,
    intentDelegate: Consumer<Intent>
) : DockInterface {
    private companion object {
        private const val TAG = "DockViewController"
        private val DEBUG = Build.isDebuggable()
    }

    private val numItems: Int
    private val car: Car
    private val dockViewWeakReference: WeakReference<DockView>
    private val dockViewModel: DockViewModel
    private var dockHelper: DockHelper? = null
    private val dockEventsReceiver: DockEventsReceiver
    private val taskStackChangeListeners: TaskStackChangeListeners
    private val dockTaskStackChangeListener: DockTaskStackChangeListener

    init {
        numItems = userContext.resources.getInteger(R.integer.config_numDockApps)
        val adapter = DockAdapter(numItems, intentDelegate, userContext)
        dockView.setAdapter(adapter)
        dockViewWeakReference = WeakReference(dockView)
        dockViewModel = DockViewModel(numItems) { updatedApps ->
            dockViewWeakReference.get()?.getAdapter()?.setItems(updatedApps)
                ?: throw NullPointerException("the View referenced does not exist")
        }
        car =
            Car.createCar(
                userContext,
                null, // handler
                Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT
            ) { car, ready ->
                run {
                    if (ready) {
                        val carPackageManager = car.getCarManager(CarPackageManager::class.java)
                        carPackageManager?.let { carPM ->
                            adapter.setCarPackageManager(carPM)
                            // todo(b/314859963): create the DockHelper without depending on carPM
                            dockHelper = DockHelper(userContext, carPM)
                            dockHelper?.let { dockViewModel.updateDefaultApps(it.defaultApps) }
                        }
                    }
                }
            }
        dockEventsReceiver = DockEventsReceiver.registerDockReceiver(userContext, this)
        dockTaskStackChangeListener = DockTaskStackChangeListener { appLaunched(it) }
        taskStackChangeListeners = TaskStackChangeListeners.getInstance()
        taskStackChangeListeners.registerTaskStackListener(dockTaskStackChangeListener)
    }

    /** Method to stop the dock. Call this upon View being destroyed. */
    fun destroy() {
        if (DEBUG) Log.d(TAG, "Destroy called")
        car.disconnect()
        userContext.unregisterReceiver(dockEventsReceiver)
        dockViewModel.destroy()
        taskStackChangeListeners.unregisterTaskStackListener(dockTaskStackChangeListener)
    }

    override fun appPinned(componentName: ComponentName) {
        // TODO("Not yet implemented")
    }

    override fun appLaunched(componentName: ComponentName) {
        if (DEBUG) Log.d(TAG, "App launched: $componentName")
        dockHelper?.let {
            if (it.excludedPackages.contains(componentName.packageName)) return
            if (it.excludedComponents.contains(componentName.flattenToString())) return

            val appItem = it.toDockAppItem(componentName)
            if (DEBUG) Log.d(TAG, "Dynamic app add to dock: $appItem")
            dockViewModel.addDynamicItem(appItem)
        }
    }

    override fun appUnpinned(componentName: ComponentName) {
        // TODO("Not yet implemented")
    }
}
