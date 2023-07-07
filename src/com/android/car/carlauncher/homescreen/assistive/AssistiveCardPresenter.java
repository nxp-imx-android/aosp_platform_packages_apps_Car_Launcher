/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.android.car.carlauncher.R;
import com.android.car.carlauncher.homescreen.CardPresenter;
import com.android.car.carlauncher.homescreen.HomeCardFragment;
import com.android.car.carlauncher.homescreen.HomeCardInterface;

import java.util.List;

/**
 * The {@link CardPresenter} for an assistive card.
 */
public class AssistiveCardPresenter extends CardPresenter {

    private static final String TAG = "AssistiveCardPresenter";
    private HomeCardFragment mHomeCardFragment;

    private AssistiveModel mCurrentModel;
    private List<HomeCardInterface.Model> mModels;

    private HomeCardFragment.OnViewClickListener mOnViewClickListener =
            new HomeCardFragment.OnViewClickListener() {
                @Override
                public void onViewClicked() {
                    Intent intent = mCurrentModel.getIntent();
                    if (intent != null && intent.resolveActivity(
                            mHomeCardFragment.getContext().getPackageManager()) != null) {
                        mHomeCardFragment.getContext().startActivity(intent);
                    } else {
                        Log.e(TAG, "No activity component found to handle intent with action: "
                                + intent.getAction());
                        Toast.makeText(mHomeCardFragment.getContext(),
                                mHomeCardFragment.getContext().getResources().getString(
                                        R.string.projected_onclick_launch_error_toast_text),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            };

    private HomeCardFragment.OnViewLifecycleChangeListener mOnViewLifecycleChangeListener =
            new HomeCardFragment.OnViewLifecycleChangeListener() {
                @Override
                public void onViewCreated() {
                    for (HomeCardInterface.Model model : mModels) {
                        setPresenterInModel(model);
                        model.onCreate(getFragment().requireContext());
                    }
                }

                @Override
                public void onViewDestroyed() {
                    if (mModels != null) {
                        for (HomeCardInterface.Model model : mModels) {
                            model.onDestroy(getFragment().requireContext());
                        }
                    }
                }
            };

    @Override
    public void setView(HomeCardInterface.View view) {
        super.setView(view);
        mHomeCardFragment = (HomeCardFragment) view;
        mHomeCardFragment.setOnViewClickListener(mOnViewClickListener);
        mHomeCardFragment.setOnViewLifecycleChangeListener(mOnViewLifecycleChangeListener);
    }

    @Override
    public void setModels(List<HomeCardInterface.Model> models) {
        mModels = models;
    }

    private void setPresenterInModel(HomeCardInterface.Model model) {
        model.setPresenter(this);
    }

    /**
     * Called when a Model is updated.
     */
    @Override
    public void onModelUpdated(HomeCardInterface.Model model) {
        AssistiveModel assistiveModel = (AssistiveModel) model;
        if (assistiveModel.getCardHeader() == null) {
            if (mCurrentModel != null && model.getClass() == mCurrentModel.getClass()) {
                if (mModels != null) {
                    // Check if any other models have content to display
                    for (HomeCardInterface.Model candidate : mModels) {
                        if (candidate.getCardHeader() != null) {
                            mCurrentModel = (AssistiveModel) candidate;
                            super.onModelUpdated(candidate);
                            return;
                        }
                    }
                }
            } else {
                // Otherwise, another model is already on display,
                return;
            }
        }
        mCurrentModel = assistiveModel;
        super.onModelUpdated(model);
    }
}
