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

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.android.car.carlauncher.R;

public final class CalmModeFragment extends Fragment {
    private static final String TAG = CalmModeFragment.class.getSimpleName();
    private static final boolean DEBUG = Build.isDebuggable();
    private View mContainerView;
    private Group mMediaGroup;
    private Group mNavGroup;
    private Group mTemperatureGroup;
    private TextView mMediaTitleView;
    private TextView mNavStateView;
    private TextView mTemperatureView;
    private TextView mClockView;
    private TextView mDateView;
    private ViewModelProvider mViewModelProvider;
    private TemperatureViewModel mTemperatureViewModel;
    private LiveData<TemperatureData> mTemperatureData;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if (DEBUG) {
            Log.v(TAG, "onCreateView");
        }

        View rootView = inflater.inflate(R.layout.calm_mode_fragment, container, false);
        mViewModelProvider = new ViewModelProvider(getViewModelStore(),
                new ViewModelProvider.AndroidViewModelFactory(requireActivity().getApplication()));

        mContainerView = rootView.findViewById(R.id.calm_mode_container);
        mClockView = rootView.findViewById(R.id.clock);
        mDateView = rootView.findViewById(R.id.date);
        mNavGroup = rootView.findViewById(R.id.nav_group);
        mNavStateView = rootView.findViewById(R.id.nav_state);
        mTemperatureGroup = rootView.findViewById(R.id.temperature_group);
        mTemperatureView = rootView.findViewById(R.id.temperature);
        mMediaGroup = rootView.findViewById(R.id.media_group);
        mMediaTitleView = rootView.findViewById(R.id.media_title);

        initExitOnTouch();
        initClock();
        initDate();
        initNavState();
        initTemperature();
        initMediaTitle();

        return rootView;
    }

    private void initMediaTitle() {
        if (DEBUG) {
            Log.v(TAG, "initMediaTitle()");
        }
        if (shouldShowMedia()) {
            // TODO: Set up initial song title and update visibility
        }
    }

    private void initTemperature() {
        if (DEBUG) {
            Log.v(TAG, "initTemperature()");
        }
        if (shouldShowTemperature()) {
            mTemperatureViewModel = mViewModelProvider.get(TemperatureViewModel.class);
            mTemperatureData = mTemperatureViewModel.getTemperatureData();
            mTemperatureData.observe(getViewLifecycleOwner(), this::updateTemperatureData);
        }
    }

    private void initDate() {
        if (DEBUG) {
            Log.v(TAG, "initDate()");
        }
        if (shouldShowDate()) {
            mDateView.setVisibility(View.VISIBLE);
        }
    }

    private void initClock() {
        if (DEBUG) {
            Log.v(TAG, "initClock()");
        }
        if (shouldShowClock()) {
            mClockView.setVisibility(View.VISIBLE);
        }
    }

    private void initExitOnTouch() {
        if (DEBUG) {
            Log.v(TAG, "initExitOnTouch()");
        }
        mContainerView.setOnTouchListener((view, motionEvent) -> {
            if (DEBUG) {
                Log.v(TAG, "Detected touch, exiting Calm mode");
            }
            if (getActivity() != null) {
                getActivity().finish();
            }
            return true;
        });
    }

    private void initNavState() {
        if (DEBUG) {
            Log.v(TAG, "initNavState()");
        }

        if (shouldShowNavigation()) {
            // TODO: set up nav state listener
        }
    }

    private boolean shouldShowMedia() {
        return getResources().getBoolean(R.bool.config_calmMode_showMedia);
    }

    private boolean shouldShowTemperature() {
        return getResources().getBoolean(R.bool.config_calmMode_showTemperature);
    }

    private boolean shouldShowClock() {
        return getResources().getBoolean(R.bool.config_calmMode_showClock);
    }

    private boolean shouldShowDate() {
        return getResources().getBoolean(R.bool.config_calmMode_showDate);
    }

    private boolean shouldShowNavigation() {
        return getResources().getBoolean(R.bool.config_calmMode_showNavigation);
    }

    private void updateTemperatureData(@Nullable TemperatureData temperatureData) {
        if (temperatureData == null) {
            mTemperatureGroup.setVisibility(View.GONE);
            mTemperatureView.setText("");
            return;
        }
        mTemperatureGroup.setVisibility(View.VISIBLE);
        mTemperatureView.setText(
                TemperatureData.buildTemperatureString(
                        temperatureData, getResources().getConfiguration().getLocales().get(0)));
    }
}
