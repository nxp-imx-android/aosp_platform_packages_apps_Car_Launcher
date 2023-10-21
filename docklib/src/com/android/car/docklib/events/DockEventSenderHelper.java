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

import static com.android.car.docklib.events.DockEventsReceiver.EXTRA_COMPONENT;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.Display;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.car.docklib.R;

/**
 * Helper used to send Dock Events.
 */
public class DockEventSenderHelper {
    private final Context mContext;
    private final ComponentName mReceiverComponent;

    public DockEventSenderHelper(Context context) {
        mContext = context;
        mReceiverComponent = new ComponentName(context.getString(R.string.config_dockViewPackage),
                DockEventsReceiver.class.getName());
    }

    /**
     * Used to send launch event to the dock. Generally used when an app is launched.
     */
    public void sendLaunchEvent(ActivityManager.RunningTaskInfo taskInfo) {
        sendEventBroadcast(DockEvent.LAUNCH, taskInfo);
    }

    /**
     * Used to send pin event to the dock. Generally used when an app should be pinned to the dock.
     */
    public void sendPinEvent(ActivityManager.RunningTaskInfo taskInfo) {
        sendEventBroadcast(DockEvent.PIN, taskInfo);
    }

    /**
     * Used to send unpin event to the dock. Generally used when an app should be unpinned from the
     * dock.
     */
    public void sendUnpinEvent(ActivityManager.RunningTaskInfo taskInfo) {
        sendEventBroadcast(DockEvent.UNPIN, taskInfo);
    }

    @VisibleForTesting
    void sendEventBroadcast(DockEvent event,
                            ActivityManager.RunningTaskInfo taskInfo) {
        if (taskInfo.getDisplayId() != Display.DEFAULT_DISPLAY) {
            return;
        }
        ComponentName component = getComponentName(taskInfo);
        if (component != null) {
            sendEventBroadcast(event, component);
        }
    }

    private void sendEventBroadcast(DockEvent event, ComponentName component) {
        Intent intent = new Intent();
        intent.setComponent(mReceiverComponent);
        intent.setAction(event.toString());
        intent.putExtra(EXTRA_COMPONENT, component);
        mContext.sendBroadcast(intent);
    }

    @Nullable
    private ComponentName getComponentName(ActivityManager.RunningTaskInfo taskInfo) {
        if (taskInfo.baseActivity == null && taskInfo.baseIntent.getComponent() == null) {
            return null;
        }
        return taskInfo.baseActivity != null ? taskInfo.baseActivity
                : taskInfo.baseIntent.getComponent();
    }
}
