/*
 * Copyright (C) 2020 Google Inc.
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

package com.android.car.carlauncher.homescreen.audio;

import android.app.ActivityOptions;
import android.content.Intent;
import android.view.Display;

import com.android.car.carlauncher.homescreen.CardPresenter;
import com.android.car.carlauncher.homescreen.HomeCardFragment;
import com.android.car.carlauncher.homescreen.HomeCardInterface;

import java.util.List;

/**
 * The {@link CardPresenter} for an audio card.
 *
 * For the audio card, the {@link AudioFragment} implements the View and displays information on
 * media from a {@link MediaViewModel}.
 */
public class HomeAudioCardPresenter extends CardPresenter {

    private AudioFragment mAudioFragment;

    private AudioModel mCurrentModel;
    private List<HomeCardInterface.Model> mModelList;
    private MediaViewModel mMediaViewModel;

    private HomeCardFragment.OnViewClickListener mOnViewClickListener =
            new HomeCardFragment.OnViewClickListener() {
                @Override
                public void onViewClicked() {
                    Intent intent = mCurrentModel.getIntent();
                    if (intent != null) {
                        ActivityOptions options = ActivityOptions.makeBasic();
                        options.setLaunchDisplayId(Display.DEFAULT_DISPLAY);
                        mAudioFragment.getContext().startActivity(intent, options.toBundle());
                    }
                }
            };

    private HomeCardFragment.OnViewLifecycleChangeListener mOnViewLifecycleChangeListener =
            new HomeCardFragment.OnViewLifecycleChangeListener() {
                @Override
                public void onViewCreated() {
                    for (HomeCardInterface.Model model : mModelList) {
                        if (model.getClass() == MediaViewModel.class) {
                            mMediaViewModel = (MediaViewModel) model;
                        }
                        model.setPresenter(HomeAudioCardPresenter.this);
                        model.onCreate(getFragment().requireContext());
                    }
                }

                @Override
                public void onViewDestroyed() {
                    if (mModelList != null) {
                        for (HomeCardInterface.Model model : mModelList) {
                            model.onDestroy(getFragment().requireContext());
                        }
                    }
                }
            };

    private AudioFragment.OnMediaViewInitializedListener mOnMediaViewInitializedListener =
            new AudioFragment.OnMediaViewInitializedListener() {
                @Override
                public void onMediaViewInitialized() {
                    // set playbackviewmodel on playback control actions view
                    mAudioFragment.getPlaybackControlsActionBar().setModel(
                            mMediaViewModel.getPlaybackViewModel(),
                            mAudioFragment.getViewLifecycleOwner());
                }
            };

    @Override
    public void setView(HomeCardInterface.View view) {
        super.setView(view);
        mAudioFragment = (AudioFragment) view;
        mAudioFragment.setOnViewLifecycleChangeListener(mOnViewLifecycleChangeListener);
        mAudioFragment.setOnViewClickListener(mOnViewClickListener);
        mAudioFragment.setOnMediaViewInitializedListener(mOnMediaViewInitializedListener);
    }

    @Override
    public void setModels(List<HomeCardInterface.Model> models) {
        mModelList = models;
    }

    protected List<HomeCardInterface.Model> getModels() {
        return mModelList;
    }

    protected AudioModel getCurrentModel() {
        return mCurrentModel;
    }

    /**
     * Updates the View appropriately when a Model has new content.
     *
     * If the updated model has content, it is displayed, regardless of what is currently shown on
     * the card. Otherwise if the model on display is updating to empty content (eg. when a call
     * ends, the InCallModel header and content are updated to null), default to showing the media
     * model if it has content.
     */
    @Override
    public void onModelUpdated(HomeCardInterface.Model model) {
        // Null card header indicates the model has no content to display
        if (model.getCardHeader() == null) {
            if (mCurrentModel != null && model.getClass() == mCurrentModel.getClass()) {
                // If the model currently on display is updating to empty content, check if there
                // is media content to display. If there is no media content the super method is
                // called with empty content, which hides the card.
                if (mMediaViewModel != null && mMediaViewModel.getCardHeader() != null) {
                    mCurrentModel = mMediaViewModel;
                    super.onModelUpdated(mMediaViewModel);
                    return;
                }
            } else {
                // Otherwise, another model is already on display, so don't update with this
                // empty content since that would hide the card.
                return;
            }
        } else if (mCurrentModel != null && mCurrentModel.getClass() == InCallModel.class
                && model.getClass() != InCallModel.class) {
            // If the Model has content, check if currentModel on display is an ongoing phone call.
            // If there is any ongoing phone call, do not update the View
            // if the model trying to update View is NOT a phone call.
            return;
        }
        mCurrentModel = (AudioModel) model;
        super.onModelUpdated(model);
    }
}
