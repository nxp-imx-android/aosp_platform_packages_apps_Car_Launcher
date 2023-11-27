package com.android.car.docklib

import android.car.Car
import android.car.content.pm.CarPackageManager
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
class DockViewController(userContext: Context, view: DockView, intentDelegate: Consumer<Intent>) {

    private val numItems: Int
    private val car: Car

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
                            adapter.setItems(DefaultAppsProvider(userContext, carPM).defaultApps)
                        }
                    }
                }
            }
    }

    /** Method to stop the dock. Call this upon View being destroyed. */
    fun destroy() {
        car.disconnect()
    }
}
