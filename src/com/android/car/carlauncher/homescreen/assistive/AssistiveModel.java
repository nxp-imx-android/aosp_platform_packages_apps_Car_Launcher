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

package com.android.car.carlauncher.homescreen.assistive;

import com.android.car.carlauncher.homescreen.HomeCardInterface;

/**
 * An extension of {@link HomeCardInterface.Model} for assistive models.
 */
public interface AssistiveModel extends HomeCardInterface.Model {
    /**
     * Called by the Presenter to handle when the View is clicked
     */
    default void onClick(android.view.View view) {}
}
