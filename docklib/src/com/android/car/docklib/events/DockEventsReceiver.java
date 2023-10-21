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

package com.android.car.docklib.events;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.android.car.docklib.DockInterface;

import javax.inject.Inject;

/**
 * BroadcastReceiver for Dock Events.
 */
public class DockEventsReceiver extends BroadcastReceiver {
    // Extras key for the ComponentName associated with the Event
    static final String EXTRA_COMPONENT = "EXTRA_COMPONENT";
    private final DockInterface mDockController;

    @Inject
    public DockEventsReceiver(DockInterface dockController) {
        mDockController = dockController;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        DockEvent event = DockEvent.toDockEvent(intent.getAction());
        ComponentName component = intent.getParcelableExtra(EXTRA_COMPONENT, ComponentName.class);

        if (event == null || component == null) {
            return;
        }

        switch (event) {
            case LAUNCH:
                mDockController.appLaunched(component);
                break;
            case PIN:
                mDockController.appPinned(component);
                break;
            case UNPIN:
                mDockController.appUnpinned(component);
                break;
        }
    }
}
