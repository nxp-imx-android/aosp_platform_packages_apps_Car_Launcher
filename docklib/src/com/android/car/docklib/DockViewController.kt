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
import com.android.car.docklib.view.DockAdapter
import com.android.car.docklib.view.DockView
import java.util.function.Consumer

/**
 * Create a controller for DockView. It initializes the view with default and persisted icons. Upon
 * initializing, it will listen to broadcast events, and update the view.
 *
 * @param userContext the foreground user context, since the view may be hosted on system context
 * @param view the inflated dock view
 * @param intentDelegate the system context will need to handle clicks and actions on the icons
 */
class DockViewController(
    userContext: Context,
    view: DockView,
    intentDelegate: Consumer<Intent>
) : DockInterface {

    private val numItems: Int
    private val car: Car
    private lateinit var dockHelper: DockHelper

    init {
        numItems = userContext.resources.getInteger(R.integer.config_numDockApps)
        val adapter = DockAdapter(numItems, intentDelegate, userContext)
        view.recyclerView.adapter = adapter
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
                            dockHelper = DockHelper(userContext, carPM)
                            adapter.setItems(dockHelper.defaultApps)
                        }
                    }
                }
            }
    }

    /** Method to stop the dock. Call this upon View being destroyed. */
    fun destroy() {
        car.disconnect()
    }

    override fun appPinned(componentName: ComponentName) {
        // TODO("Not yet implemented")
    }

    override fun appLaunched(componentName: ComponentName) {
        // TODO("Not yet implemented")
    }

    override fun appUnpinned(componentName: ComponentName) {
        // TODO("Not yet implemented")
    }
}
