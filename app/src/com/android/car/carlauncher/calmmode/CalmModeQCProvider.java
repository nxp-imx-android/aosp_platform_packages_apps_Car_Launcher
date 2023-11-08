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

package com.android.car.carlauncher.calmmode;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.car.carlauncher.R;
import com.android.car.qc.QCItem;
import com.android.car.qc.QCList;
import com.android.car.qc.QCRow;
import com.android.car.qc.provider.BaseQCProvider;

import java.util.Set;

/**
 * Remote Quick Control provider for Calm mode in CarLauncher.
 */
public class CalmModeQCProvider extends BaseQCProvider {
    private static final boolean DEBUG = Build.IS_DEBUGGABLE;
    public static final String AUTHORITY = "com.android.car.carlauncher.calmmode";
    private static final String TAG = CalmModeQCProvider.class.getSimpleName();

    private static final String CALM_MODE_SEGMENT = "calm_mode";
    // Start Uris
    public static final Uri CALM_MODE_URI = new Uri.Builder().scheme(
                    ContentResolver.SCHEME_CONTENT)
            .authority(AUTHORITY)
            .appendPath(CALM_MODE_SEGMENT)
            .build();
    private Set<String> mAllowListedPackages;
    private Context mContext;
    @VisibleForTesting
    QCItem mQCItem;

    /**
     * Returns a uri without its parameters (or null if the provided uri is null).
     */
    public static Uri removeParameterFromUri(Uri uri) {
        return uri != null ? uri.buildUpon().clearQuery().build() : null;
    }

    @Override
    public boolean onCreate() {
        boolean returnVal = super.onCreate();
        if (DEBUG) {
            Log.d(TAG, "onCreate() returnVal " + returnVal);
        }
        mAllowListedPackages = Set.of(getContext().getResources().getStringArray(
                R.array.launcher_qc_provider_package_allowlist));
        mContext = getContext();
        mQCItem = getQCItem();
        return returnVal;
    }

    @Override
    public QCItem onBind(Uri uri) {
        boolean isValidCalmModeURI = removeParameterFromUri(uri).equals(CALM_MODE_URI);
        if (DEBUG) {
            Log.d(TAG, "onBind() isValidCalmModeURI " + isValidCalmModeURI);
        }

        if (!isValidCalmModeURI) {
            throw new IllegalArgumentException("No QCItem found for uri: " + uri);
        }
        return mQCItem;
    }

    @Override
    protected Set<String> getAllowlistedPackages() {
        return mAllowListedPackages;
    }

    @VisibleForTesting
    QCItem getQCItem() {
        Intent intent = new Intent(mContext, CalmModeActivity.class);
        PendingIntent ambientModeIntent = PendingIntent.getActivity(mContext, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);

        QCRow calmModeRow = new QCRow.Builder()
                .setTitle(mContext.getString(R.string.calm_mode_title))
                .setPrimaryAction(ambientModeIntent)
                .build();

        return new QCList.Builder().addRow(calmModeRow).build();
    }
}
