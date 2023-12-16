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
package com.android.car.docklib.task;

import android.app.ActivityManager;
import android.content.ComponentName;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.systemui.shared.system.TaskStackChangeListener;

import java.util.function.Consumer;

public class DockTaskStackChangeListener implements TaskStackChangeListener {
    Consumer<ComponentName> mTaskLaunchDelegate;
    public DockTaskStackChangeListener(Consumer<ComponentName> taskLaunchDelegate) {
        mTaskLaunchDelegate = taskLaunchDelegate;
    }

    @Override
    public void onTaskMovedToFront(ActivityManager.RunningTaskInfo taskInfo) {
        ComponentName component = getComponentName(taskInfo);
        mTaskLaunchDelegate.accept(component);
    }

    @Nullable
    private ComponentName getComponentName(@NonNull ActivityManager.RunningTaskInfo taskInfo) {
        if (taskInfo.baseActivity == null && taskInfo.baseIntent.getComponent() == null) {
            return null;
        }
        return taskInfo.baseActivity != null ? taskInfo.baseActivity
                : taskInfo.baseIntent.getComponent();
    }


}
