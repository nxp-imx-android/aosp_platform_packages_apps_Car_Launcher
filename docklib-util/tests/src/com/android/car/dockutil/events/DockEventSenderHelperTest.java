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

package com.android.car.dockutil.events;

import static android.view.Display.DEFAULT_DISPLAY;

import static com.android.car.dockutil.events.DockEventSenderHelper.EXTRA_COMPONENT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.dockutil.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class DockEventSenderHelperTest {
    private static final String DOCK_RECEIVER_PKG = "DOCK_RECEIVER_PKG";
    private static final String DOCK_RECEIVER_CLS = "DOCK_RECEIVER_CLS";
    @Mock
    public ActivityManager.RunningTaskInfo mRunningTaskInfo;
    @Mock
    public Context mContext;
    @Mock
    public Intent mIntent;
    @Mock
    public ComponentName mAppComponent;
    @Captor
    public ArgumentCaptor<Intent> mIntentCaptor;
    private DockEventSenderHelper mDockEventSenderHelper;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getString(eq(R.string.config_dockViewPackage))).thenReturn(DOCK_RECEIVER_PKG);
        when(mContext.getString(eq(R.string.config_dockReceiver))).thenReturn(DOCK_RECEIVER_CLS);
        mDockEventSenderHelper = new DockEventSenderHelper(mContext);
    }

    @Test
    public void sendEventBroadcast_nonDefaultDisplay_broadcastNotSent() {
        when(mRunningTaskInfo.getDisplayId()).thenReturn(DEFAULT_DISPLAY + 1);

        mDockEventSenderHelper.sendEventBroadcast(DockEvent.LAUNCH, mRunningTaskInfo);

        verify(mContext, never()).sendBroadcast(any(Intent.class));
    }

    @Test
    public void sendEventBroadcast_noBastActivity_noBaseIntentComponent_broadcastNotSent() {
        when(mRunningTaskInfo.getDisplayId()).thenReturn(DEFAULT_DISPLAY);
        mRunningTaskInfo.baseActivity = null;
        when(mIntent.getComponent()).thenReturn(null);
        mRunningTaskInfo.baseIntent = mIntent;

        mDockEventSenderHelper.sendEventBroadcast(DockEvent.LAUNCH, mRunningTaskInfo);

        verify(mContext, never()).sendBroadcast(any(Intent.class));
    }

    @Test
    public void sendEventBroadcast_broadcastSent_receiverPackageSet() {
        when(mRunningTaskInfo.getDisplayId()).thenReturn(DEFAULT_DISPLAY);
        mRunningTaskInfo.baseActivity = mAppComponent;

        mDockEventSenderHelper.sendEventBroadcast(DockEvent.LAUNCH, mRunningTaskInfo);

        verify(mContext).sendBroadcast(mIntentCaptor.capture());
        Intent intentSent = mIntentCaptor.getValue();
        assertThat(intentSent.getComponent()).isNotNull();
        assertThat(intentSent.getComponent().getPackageName())
                .isEqualTo(DOCK_RECEIVER_PKG);
    }


    @Test
    public void sendEventBroadcast_launchEvent_broadcastSent() {
        when(mRunningTaskInfo.getDisplayId()).thenReturn(DEFAULT_DISPLAY);
        mRunningTaskInfo.baseActivity = mAppComponent;

        mDockEventSenderHelper.sendEventBroadcast(DockEvent.LAUNCH, mRunningTaskInfo);

        verify(mContext).sendBroadcast(mIntentCaptor.capture());
        Intent intentSent = mIntentCaptor.getValue();
        assertThat(intentSent.getAction()).isEqualTo(DockEvent.LAUNCH.toString());
        assertThat(intentSent.getExtras()).isNotNull();
        assertThat(intentSent.getExtras().getParcelable(EXTRA_COMPONENT, ComponentName.class))
                .isEqualTo(mAppComponent);
    }

    @Test
    public void sendEventBroadcast_pinEvent_broadcastSent() {
        when(mRunningTaskInfo.getDisplayId()).thenReturn(DEFAULT_DISPLAY);
        mRunningTaskInfo.baseActivity = mAppComponent;

        mDockEventSenderHelper.sendEventBroadcast(DockEvent.PIN, mRunningTaskInfo);

        verify(mContext).sendBroadcast(mIntentCaptor.capture());
        Intent intentSent = mIntentCaptor.getValue();
        assertThat(intentSent.getAction()).isEqualTo(DockEvent.PIN.toString());
        assertThat(intentSent.getExtras()).isNotNull();
        assertThat(intentSent.getExtras().getParcelable(EXTRA_COMPONENT, ComponentName.class))
                .isEqualTo(mAppComponent);
    }


    @Test
    public void sendEventBroadcast_unpinEvent_broadcastSent() {
        when(mRunningTaskInfo.getDisplayId()).thenReturn(DEFAULT_DISPLAY);
        mRunningTaskInfo.baseActivity = mAppComponent;

        mDockEventSenderHelper.sendEventBroadcast(DockEvent.UNPIN, mRunningTaskInfo);

        verify(mContext).sendBroadcast(mIntentCaptor.capture());
        Intent intentSent = mIntentCaptor.getValue();
        assertThat(intentSent.getAction()).isEqualTo(DockEvent.UNPIN.toString());
        assertThat(intentSent.getExtras()).isNotNull();
        assertThat(intentSent.getExtras().getParcelable(EXTRA_COMPONENT, ComponentName.class))
                .isEqualTo(mAppComponent);
    }
}
