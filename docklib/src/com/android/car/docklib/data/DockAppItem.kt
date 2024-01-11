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
import java.util.UUID

/** Data class that describes an app being showed on Dock */
data class DockAppItem(
        val id: @DockItemId UUID = UUID.randomUUID(),
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
        return (this === other) ||
                (other is DockAppItem &&
                        this.id == other.id &&
                        this.name == other.name &&
                        this.type == other.type &&
                        this.component == other.component &&
                        this.icon.constantState == other.icon.constantState &&
                        this.isDistractionOptimized == other.isDistractionOptimized)
    }

    override fun toString(): String {
        return ("DockAppItem#${hashCode()}{name: $name, component: $component, type: $type, " +
                "isDistractionOptimized: $isDistractionOptimized, icon: $icon}")
    }
}
