/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.car.carlauncher;

import android.os.Parcel;
import android.os.Parcelable;

import com.android.car.carlauncher.LauncherItemProto.LauncherItemMessage;

/**
 * LauncherItem can be an app or a folder that contains app
 */
public abstract class LauncherItem implements Parcelable {
    private String mPackageName;
    private String mDisplayName;

    public LauncherItem(String packageName, String displayName) {
        mPackageName = packageName;
        mDisplayName = displayName;
    }

    protected LauncherItem(Parcel in) {
        mPackageName = in.readString();
        mDisplayName = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mPackageName);
        dest.writeString(mDisplayName);
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public String getPackageName() {
        return mPackageName;
    }

    /**
     * This method is used to convert a LauncherItem to a protobuf class
     */
    public LauncherItemMessage launcherItem2Msg(int relativePosition, int containerID) {
        LauncherItemMessage.Builder builder = LauncherItemMessage.newBuilder().setPackageName(
                mPackageName).setDisplayName(mDisplayName).setRelativePosition(
                relativePosition).setContainerID(containerID);
        return builder.build();
    }
}
