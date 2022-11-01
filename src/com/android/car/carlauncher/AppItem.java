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

/**
 * AppItem of an app including the package name, the display name and the app's metadata.
 */
public class AppItem extends LauncherItem {
    private AppMetaData mAppMetaData;

    public AppItem(String packageName, String displayName, AppMetaData appMetaData) {
        super(packageName, displayName);
        mAppMetaData = appMetaData;
    }

    public static final Creator<LauncherItem> CREATOR = new Creator<LauncherItem>() {
        @Override
        public AppItem createFromParcel(Parcel in) {
            return new AppItem(in);
        }

        @Override
        public LauncherItem[] newArray(int size) {
            return new LauncherItem[size];
        }
    };

    protected AppItem(Parcel in) {
        super(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public AppMetaData getAppMetaData() {
        return mAppMetaData;
    }
}
