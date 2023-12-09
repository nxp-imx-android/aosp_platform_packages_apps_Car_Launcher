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

package com.android.car.docklib.data

import android.content.ComponentName
import android.graphics.drawable.Drawable

/** Data class that describes an app being showed on Dock */
data class DockAppItem(
    val type: Type,
    val component: ComponentName,
    val name: String,
    val icon: Drawable,
    val isDistractionOptimized: Boolean,
) {
    // todo(b/315210225): handle getting icon lazily
    enum class Type(val value: String) {
        DYNAMIC("DYNAMIC"),
        STATIC("STATIC");

        override fun toString(): String {
            return value
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DockAppItem) return false

        if (this.type != other.type) return false
        if (this.name != other.name) return false
        if (this.component != other.component) return false
        if (this.icon.constantState != other.icon.constantState) return false
        if (this.isDistractionOptimized != other.isDistractionOptimized) return false

        return true
    }

    override fun toString(): String {
        return ("DockAppItem#${hashCode()}{name: $name, component: $component, type: $type, " +
                "isDistractionOptimized: $isDistractionOptimized, icon: $icon}")
    }
}
