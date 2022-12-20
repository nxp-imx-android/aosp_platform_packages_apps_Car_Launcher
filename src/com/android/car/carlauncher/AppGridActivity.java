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

import static com.android.car.carlauncher.AppLauncherUtils.APP_TYPE_LAUNCHABLES;
import static com.android.car.carlauncher.AppLauncherUtils.APP_TYPE_MEDIA_SERVICES;

import android.app.AlertDialog;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.car.Car;
import android.car.CarNotConnectedException;
import android.car.content.pm.CarPackageManager;
import android.car.drivingstate.CarUxRestrictions;
import android.car.drivingstate.CarUxRestrictionsManager;
import android.car.media.CarMediaManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.carlauncher.AppLauncherUtils.LauncherAppsInfo;
import com.android.car.ui.AlertDialogBuilder;
import com.android.car.ui.FocusArea;
import com.android.car.ui.baselayout.Insets;
import com.android.car.ui.baselayout.InsetsChangedListener;
import com.android.car.ui.core.CarUi;
import com.android.car.ui.shortcutspopup.CarUiShortcutsPopup;
import com.android.car.ui.toolbar.MenuItem;
import com.android.car.ui.toolbar.NavButtonMode;
import com.android.car.ui.toolbar.ToolbarController;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Launcher activity that shows a grid of apps.
 */
public class AppGridActivity extends AppCompatActivity implements InsetsChangedListener,
        AppGridPageSnapper.PageSnapListener, AppItemViewHolder.AppItemDragListener,
        AppLauncherUtils.ShortcutsListener{
    private static final String TAG = "AppGridActivity";
    private static final String MODE_INTENT_EXTRA = "com.android.car.carlauncher.mode";
    private static CarUiShortcutsPopup sCarUiShortcutsPopup;

    private boolean mShowAllApps = true;
    private boolean mShowToolbar = true;
    private final Set<String> mHiddenApps = new HashSet<>();
    private final Set<String> mCustomMediaComponents = new HashSet<>();
    private PackageManager mPackageManager;
    private UsageStatsManager mUsageStatsManager;
    private AppInstallUninstallReceiver mInstallUninstallReceiver;
    private Car mCar;
    private CarUxRestrictionsManager mCarUxRestrictionsManager;
    private CarPackageManager mCarPackageManager;
    private CarMediaManager mCarMediaManager;
    private Mode mMode;
    private AlertDialog mStopAppAlertDialog;
    private LauncherAppsInfo mAppsInfo;
    private LauncherViewModel mLauncherModel;
    private AppGridAdapter mAdapter;
    private AppGridRecyclerView mRecyclerView;
    private AppGridPositionIndicator mPositionIndicator;
    private AppGridLayoutManager mLayoutManager;
    private FrameLayout mPositionIndicatorContainer;
    private boolean mIsCurrentlyDragging;
    private long mOffPageHoverBeforeScrollMs;

    private int mNumOfRows;
    private int mNumOfCols;
    private int mAppGridMargin;
    private int mAppGridWidth;
    private int mAppGridHeight;

    private int mCurrentScrollXOffset;
    private int mCurrentScrollState;
    private int mNextScrollDestination;
    private RecyclerView.ItemDecoration mPagePaddingDecorator;
    private AppGridPageSnapper.AppGridPageSnapCallback mSnapCallback;
    private AppItemViewHolder.AppItemDragCallback mDragCallback;

    /**
     * enum to define the state of display area possible.
     * CONTROL_BAR state is when only control bar is visible.
     * FULL state is when display area hosting default apps  cover the screen fully.
     * DEFAULT state where maps are shown above DA for default apps.
     */
    public enum CAR_LAUNCHER_STATE {
        CONTROL_BAR, DEFAULT, FULL
    }

    private enum Mode {
        ALL_APPS(R.string.app_launcher_title_all_apps,
                APP_TYPE_LAUNCHABLES + APP_TYPE_MEDIA_SERVICES,
                true),
        MEDIA_ONLY(R.string.app_launcher_title_media_only,
                APP_TYPE_MEDIA_SERVICES,
                true),
        MEDIA_POPUP(R.string.app_launcher_title_media_only,
                APP_TYPE_MEDIA_SERVICES,
                false),
        ;
        public final @StringRes int mTitleStringId;
        public final @AppLauncherUtils.AppTypes int mAppTypes;
        public final boolean mOpenMediaCenter;

        Mode(@StringRes int titleStringId, @AppLauncherUtils.AppTypes int appTypes,
                boolean openMediaCenter) {
            mTitleStringId = titleStringId;
            mAppTypes = appTypes;
            mOpenMediaCenter = openMediaCenter;
        }
    }

    private ServiceConnection mCarConnectionListener = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                mCarUxRestrictionsManager = (CarUxRestrictionsManager) mCar.getCarManager(
                        Car.CAR_UX_RESTRICTION_SERVICE);
                CarUxRestrictions carUxRestrictions = mCarUxRestrictionsManager
                        .getCurrentCarUxRestrictions();
                boolean isDistractionOptimizationRequired;
                if (carUxRestrictions == null) {
                    Log.v(TAG, "No CarUxRestrictions on display");
                    isDistractionOptimizationRequired = false;
                } else {
                    isDistractionOptimizationRequired = carUxRestrictions
                            .isRequiresDistractionOptimization();
                }
                mAdapter
                        .setIsDistractionOptimizationRequired(isDistractionOptimizationRequired);
                mCarUxRestrictionsManager.registerListener(restrictionInfo -> {
                    boolean requiresDistractionOptimization =
                            restrictionInfo.isRequiresDistractionOptimization();
                    mAdapter.setIsDistractionOptimizationRequired(
                            requiresDistractionOptimization);
                    if (requiresDistractionOptimization) {
                        dismissForceStopMenus();
                    }
                });
                mCarPackageManager = (CarPackageManager) mCar.getCarManager(Car.PACKAGE_SERVICE);
                mCarMediaManager = (CarMediaManager) mCar.getCarManager(Car.CAR_MEDIA_SERVICE);
                initializeLauncherModel();
            } catch (CarNotConnectedException e) {
                Log.e(TAG, "Car not connected in CarConnectionListener", e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mCarUxRestrictionsManager = null;
            mCarPackageManager = null;
        }
    };

    private void initializeLauncherModel() {
        ExecutorService fetchOrderExecutorService = Executors.newSingleThreadExecutor();
        fetchOrderExecutorService.execute(() -> {
            mLauncherModel.updateAppsOrder();
            fetchOrderExecutorService.shutdown();
        });
        ExecutorService alphabetizeExecutorService = Executors.newSingleThreadExecutor();
        alphabetizeExecutorService.execute(() -> {
            Set<String> appsToHide = mShowAllApps ? Collections.emptySet() : mHiddenApps;
            mAppsInfo = AppLauncherUtils.getLauncherApps(getApplicationContext(),
                    appsToHide,
                    mCustomMediaComponents,
                    mMode.mAppTypes,
                    mMode.mOpenMediaCenter,
                    getSystemService(LauncherApps.class),
                    mCarPackageManager,
                    mPackageManager,
                    new AppLauncherUtils.VideoAppPredicate(mPackageManager),
                    mCarMediaManager,
                    AppGridActivity.this);
            mLauncherModel.generateAlphabetizedAppOrder(mAppsInfo);
            alphabetizeExecutorService.shutdown();
        });
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // TODO (b/267548246) deprecate toolbar and find another way to hide debug apps
        mShowToolbar = false;
        if (mShowToolbar) {
            setTheme(R.style.Theme_Launcher_AppGridActivity);
        } else {
            setTheme(R.style.Theme_Launcher_AppGridActivity_NoToolbar);
        }
        super.onCreate(savedInstanceState);

        mPackageManager = getPackageManager();
        mUsageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        mLauncherModel = new ViewModelProvider(AppGridActivity.this,
                new LauncherViewModelFactory(getFilesDir())).get(
                LauncherViewModel.class);
        mLauncherModel.getCurrentLauncher().observe(
                AppGridActivity.this, new Observer<List<LauncherItem>>() {
                    @Override
                    public void onChanged(List<LauncherItem> launcherItems) {
                        mAdapter.setLauncherItems(launcherItems);
                        mNextScrollDestination = mSnapCallback.getSnapPosition();
                        updateScrollState();
                        mLauncherModel.maybeSaveAppsOrder();
                    }
                }
        );
        mCar = Car.createCar(this, mCarConnectionListener);
        mHiddenApps.addAll(Arrays.asList(getResources().getStringArray(R.array.hidden_apps)));
        mCustomMediaComponents.addAll(
                Arrays.asList(getResources().getStringArray(R.array.custom_media_packages)));
        setContentView(R.layout.app_grid_activity);
        updateMode();

        if (mShowToolbar) {
            ToolbarController toolbar = CarUi.requireToolbar(this);

            toolbar.setNavButtonMode(NavButtonMode.CLOSE);

            if (Build.IS_DEBUGGABLE) {
                toolbar.setMenuItems(Collections.singletonList(MenuItem.builder(this)
                        .setDisplayBehavior(MenuItem.DisplayBehavior.NEVER)
                        .setTitle(R.string.hide_debug_apps)
                        .setOnClickListener(i -> {
                            mShowAllApps = !mShowAllApps;
                            i.setTitle(mShowAllApps
                                    ? R.string.hide_debug_apps
                                    : R.string.show_debug_apps);
                        })
                        .build()));
            }
        }

        mSnapCallback = new AppGridPageSnapper.AppGridPageSnapCallback(this);
        mDragCallback = new AppItemViewHolder.AppItemDragCallback(this);
        mNumOfCols = getResources().getInteger(R.integer.car_app_selector_column_number);
        mNumOfRows = getResources().getInteger(R.integer.car_app_selector_row_number);
        mOffPageHoverBeforeScrollMs = getResources().getInteger(
                R.integer.ms_off_page_hover_before_scroll);

        mRecyclerView = requireViewById(R.id.apps_grid);
        mRecyclerView.setFocusable(false);
        mLayoutManager = new AppGridLayoutManager(this, mNumOfRows,
                /* orientation */ GridLayoutManager.HORIZONTAL, /* reverseLayout */ false);
        mRecyclerView.setLayoutManager(mLayoutManager);

        AppGridPageSnapper pageSnapper = new AppGridPageSnapper(this, mSnapCallback);
        pageSnapper.attachToRecyclerView(mRecyclerView);

        mRecyclerView.setItemAnimator(new AppGridItemAnimator());

        // hide the default scrollbar and replace it with a visual position indicator
        mRecyclerView.setVerticalScrollBarEnabled(false);
        mRecyclerView.setHorizontalScrollBarEnabled(false);
        mPositionIndicatorContainer = requireViewById(R.id.position_indicator_container);
        mPositionIndicator = requireViewById(R.id.position_indicator);

        // recycler view is set to LTR to prevent layout manager from reassigning layout direction.
        // instead, AppGridPagingUtil will determine the grid index based on the system layout
        // direction and provide LTR mapping at adapter level.
        mRecyclerView.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        mPositionIndicatorContainer.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        // we create but do not attach the adapter to recyclerview until view tree layout is
        // complete and the total size of the app grid is measureable.
        mAdapter = new AppGridAdapter(this, mNumOfCols, mNumOfRows,
                /* dataModel */ mLauncherModel, /* dragCallback */ mDragCallback,
                /* snapCallback */ mSnapCallback);
        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                // scroll state will need to be updated after item has been dropped
                mNextScrollDestination = mSnapCallback.getSnapPosition();
                updateScrollState();
            }
        });

        // set scroll listener
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                mCurrentScrollXOffset = mCurrentScrollXOffset + dx;
                mPositionIndicator.updateXOffset(mCurrentScrollXOffset);
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                mCurrentScrollState = newState;
                mSnapCallback.setScrollState(mCurrentScrollState);
                switch (newState) {
                    case RecyclerView.SCROLL_STATE_DRAGGING:
                        if (!mIsCurrentlyDragging) {
                            mDragCallback.cancelDragTasks();
                        }
                        dismissShortcutPopup();
                        mPositionIndicator.animateAppearance();
                        break;

                    case RecyclerView.SCROLL_STATE_SETTLING:
                        mPositionIndicator.animateAppearance();
                        break;

                    case RecyclerView.SCROLL_STATE_IDLE:
                        if (mIsCurrentlyDragging) {
                            mLayoutManager.setShouldLayoutChildren(false);
                        }
                        mPositionIndicator.animateFading();
                        // in case the recyclerview was scrolled by rotary input, we need to handle
                        // focusing the correct element: either on the first or last element on page
                        mRecyclerView.maybeHandleRotaryFocus();
                }
            }
        });

        // set drag listener and global layout listener, which will dynamically adjust app grid
        // height and width depending on device screen size.
        boolean configAllowReordering = getResources().getBoolean(R.bool.config_allow_reordering);
        if (configAllowReordering) {
            mRecyclerView.setOnDragListener(new AppGridDragListener());
        }
        LinearLayout appGridLayout = requireViewById(R.id.apps_grid_background);
        appGridLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    // app grid dimensions should fill up all the spaces allocated
                    int appGridWidth, appGridHeight;
                    int definedMargin = getResources().getDimensionPixelSize(
                            R.dimen.app_grid_margin_horizontal);
                    // if the app grid is configured to use predefined widths instead of filling
                    // the spaces available, use that resource instead.
                    boolean useDefinedDimensions = getResources().getBoolean(
                            R.bool.use_defined_app_grid_dimensions);
                    if (useDefinedDimensions) {
                        appGridWidth = getResources().getDimensionPixelSize(
                                R.dimen.app_grid_width) - 2 * definedMargin;
                        appGridHeight = getResources().getDimensionPixelSize(
                                R.dimen.app_grid_height) - getResources().getDimensionPixelSize(
                                R.dimen.position_indicator_height);
                    } else {
                        appGridWidth = appGridLayout.getMeasuredWidth() - 2 * definedMargin;
                        appGridHeight = appGridLayout.getMeasuredHeight()
                                - getResources().getDimensionPixelSize(
                                R.dimen.position_indicator_height);
                    }
                    // dimensions should be rounded down to the nearest modulo of mNumOfCols and
                    // mNumOfRows to have a pixel-exact fit.
                    appGridWidth = appGridWidth / mNumOfCols * mNumOfCols;
                    appGridHeight = appGridHeight / mNumOfRows * mNumOfRows;

                    if (appGridWidth != mAppGridWidth || appGridHeight != mAppGridHeight) {
                        // layout app grid again for launcher
                        mAppGridWidth = appGridWidth;
                        mAppGridHeight = appGridHeight;
                        mAppGridMargin = (appGridLayout.getMeasuredWidth() - mAppGridWidth) / 2;

                        ViewGroup.LayoutParams appGridParams = mRecyclerView.getLayoutParams();
                        appGridParams.width = appGridLayout.getMeasuredWidth();
                        appGridParams.height = mAppGridHeight;
                        mRecyclerView.setLayoutParams(appGridParams);

                        // adjust decorator with the new measured app grid margins
                        if (mPagePaddingDecorator != null) {
                            mRecyclerView.removeItemDecoration(mPagePaddingDecorator);
                        }
                        mPagePaddingDecorator = new RecyclerView.ItemDecoration() {
                            @Override
                            public void onDraw(@NotNull Canvas c, @NotNull RecyclerView parent,
                                    @NotNull RecyclerView.State state) {
                                super.onDraw(c, parent, state);
                            }

                            @Override
                            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                    @NonNull RecyclerView parent,
                                    @NonNull RecyclerView.State state) {
                                // the first and last column of each page should have extra margins
                                // to indicate start and end of a page.
                                int columnId = mAdapter.getColumnId(
                                        parent.getChildAdapterPosition(view));
                                if (columnId == 0) {
                                    outRect.left = mAppGridMargin;
                                } else if (columnId == mNumOfCols - 1) {
                                    outRect.right = mAppGridMargin;
                                }
                            }
                        };
                        mRecyclerView.addItemDecoration(mPagePaddingDecorator);

                        ViewGroup.LayoutParams containerParams =
                                mPositionIndicatorContainer.getLayoutParams();
                        containerParams.width = mAppGridWidth;
                        mPositionIndicatorContainer.setLayoutParams(containerParams);
                        mPositionIndicator.setAppGridDimensions(mAppGridWidth, mAppGridMargin);

                        // reattach the adapter to recreated view holders with new dimen
                        Rect pageBound = new Rect();
                        mRecyclerView.getGlobalVisibleRect(pageBound);
                        mAdapter.updateAppGridDimensions(pageBound, mAppGridWidth / mNumOfCols,
                                mAppGridHeight / mNumOfRows);
                        mRecyclerView.setAdapter(mAdapter);
                    }
                }
            });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        updateMode();
    }

    @Override
    protected void onDestroy() {
        if (mCar != null && mCar.isConnected()) {
            mCar.disconnect();
            mCar = null;
        }
        super.onDestroy();
    }

    private void updateMode() {
        mMode = parseMode(getIntent());
        setTitle(mMode.mTitleStringId);
        if (mShowToolbar) {
            CarUi.requireToolbar(this).setTitle(mMode.mTitleStringId);
        }
    }

    /**
     * Note: This activity is exported, meaning that it might receive intents from any source.
     * Intent data parsing must be extra careful.
     */
    @NonNull
    private Mode parseMode(@Nullable Intent intent) {
        String mode = intent != null ? intent.getStringExtra(MODE_INTENT_EXTRA) : null;
        try {
            return mode != null ? Mode.valueOf(mode) : Mode.ALL_APPS;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Received invalid mode: " + mode, e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateScrollState();
        mAdapter.setLayoutDirection(getResources().getConfiguration().getLayoutDirection());
    }

    /**
     * Updates the scroll state after receiving data changes, such as new apps being added or
     * reordered, and when user returns to launcher onResume.
     *
     * Additionally, notify position indicator to handle resizing in case new app addition creates a
     * new page or deleted a page.
     */
    void updateScrollState() {
        int page = mNextScrollDestination / (mNumOfRows * mNumOfCols);
        mCurrentScrollXOffset = page * (mAppGridWidth + 2 * mAppGridMargin);
        mRecyclerView.suppressLayout(false);
        mLayoutManager.scrollToPositionWithOffset(page * mNumOfRows * mNumOfCols, 0);

        mPositionIndicator.updateDimensions(mAdapter.getPageCount());
        mPositionIndicator.updateXOffset(mCurrentScrollXOffset);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // register broadcast receiver for package installation and uninstallation
        mInstallUninstallReceiver = new AppInstallUninstallReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        registerReceiver(mInstallUninstallReceiver, filter);

        // Connect to car service
        mCar.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // disconnect from app install/uninstall receiver
        if (mInstallUninstallReceiver != null) {
            unregisterReceiver(mInstallUninstallReceiver);
            mInstallUninstallReceiver = null;
        }
        // disconnect from car listeners
        try {
            if (mCarUxRestrictionsManager != null) {
                mCarUxRestrictionsManager.unregisterListener();
            }
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Error unregistering listeners", e);
        }
        if (mCar != null) {
            mCar.disconnect();
        }
    }

    @Override
    protected void onPause() {
        dismissForceStopMenus();
        super.onPause();
    }

    @Override
    public void onSnapToPosition(int position) {
        mNextScrollDestination = position;
    }

    @Override
    public void onItemLongPressed(boolean isLongPressed) {
        // after the user long presses the app icon, scrolling should be disabled until long press
        // is canceled as to allow MotionEvent to be interpreted as attempt to drag the app icon.
        mRecyclerView.suppressLayout(isLongPressed);
    }

    @Override
    public void onItemSelected(int gridPositionFrom) {
        mIsCurrentlyDragging = true;
        mLayoutManager.setShouldLayoutChildren(false);
        mAdapter.setDragStartPoint(gridPositionFrom);
        dismissShortcutPopup();
    }

    @Override
    public void onItemDropped(int gridPositionFrom, int gridPositionTo) {
        mLayoutManager.setShouldLayoutChildren(true);
        mAdapter.moveAppItem(gridPositionFrom, gridPositionTo);
    }

    /**
     * Note that in order to obtain usage stats from the previous boot,
     * the device must have gone through a clean shut down process.
     */
    private List<AppMetaData> getMostRecentApps(LauncherAppsInfo appsInfo) {
        ArrayList<AppMetaData> apps = new ArrayList<>();
        if (appsInfo.isEmpty()) {
            return apps;
        }

        // get the usage stats starting from 1 year ago with a INTERVAL_YEARLY granularity
        // returning entries like:
        // "During 2017 App A is last used at 2017/12/15 18:03"
        // "During 2017 App B is last used at 2017/6/15 10:00"
        // "During 2018 App A is last used at 2018/1/1 15:12"
        List<UsageStats> stats =
                mUsageStatsManager.queryUsageStats(
                        UsageStatsManager.INTERVAL_YEARLY,
                        System.currentTimeMillis() - DateUtils.YEAR_IN_MILLIS,
                        System.currentTimeMillis());

        if (stats == null || stats.size() == 0) {
            return apps; // empty list
        }

        stats.sort(new LastTimeUsedComparator());

        int currentIndex = 0;
        int itemsAdded = 0;
        int statsSize = stats.size();
        int itemCount = Math.min(mNumOfCols, statsSize);
        while (itemsAdded < itemCount && currentIndex < statsSize) {
            UsageStats usageStats = stats.get(currentIndex);
            String packageName = usageStats.mPackageName;
            currentIndex++;

            // do not include self
            if (packageName.equals(getPackageName())) {
                continue;
            }

            // TODO(b/136222320): UsageStats is obtained per package, but a package may contain
            //  multiple media services. We need to find a way to get the usage stats per service.
            ComponentName componentName = AppLauncherUtils.getMediaSource(mPackageManager,
                    packageName);
            // Exempt media services from background and launcher checks
            if (!appsInfo.isMediaService(componentName)) {
                // do not include apps that only ran in the background
                if (usageStats.getTotalTimeInForeground() == 0) {
                    continue;
                }

                // do not include apps that don't support starting from launcher
                Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
                if (intent == null || !intent.hasCategory(Intent.CATEGORY_LAUNCHER)) {
                    continue;
                }
            }

            AppMetaData app = appsInfo.getAppMetaData(componentName);
            // Prevent duplicated entries
            // e.g. app is used at 2017/12/31 23:59, and 2018/01/01 00:00
            if (app != null && !apps.contains(app)) {
                apps.add(app);
                itemsAdded++;
            }
        }
        return apps;
    }

    @Override
    public void onCarUiInsetsChanged(Insets insets) {
        requireViewById(R.id.apps_grid)
                .setPadding(0, insets.getTop(), 0, insets.getBottom());
        FocusArea focusArea = requireViewById(R.id.focus_area);
        focusArea.setHighlightPadding(0, insets.getTop(), 0, insets.getBottom());
        focusArea.setBoundsOffset(0, insets.getTop(), 0, insets.getBottom());

        requireViewById(android.R.id.content)
                .setPadding(insets.getLeft(), 0, insets.getRight(), 0);
    }

    @Override
    public void onShortcutsShow(CarUiShortcutsPopup carUiShortcutsPopup) {
        sCarUiShortcutsPopup = carUiShortcutsPopup;
    }

    @Override
    public void onShortcutsItemClick(String packageName, CharSequence displayName,
            boolean allowStopApp) {
        AlertDialogBuilder builder = new AlertDialogBuilder(this)
                .setTitle(R.string.app_launcher_stop_app_dialog_title);

        if (allowStopApp) {
            builder.setMessage(R.string.app_launcher_stop_app_dialog_text)
                    .setPositiveButton(android.R.string.ok,
                            (d, w) -> AppLauncherUtils.forceStop(packageName, AppGridActivity.this,
                                    displayName, mCarMediaManager, mAppsInfo.getMediaServices(),
                                    this))
                    .setNegativeButton(android.R.string.cancel, /* onClickListener= */ null);
        } else {
            builder.setMessage(R.string.app_launcher_stop_app_cant_stop_text)
                    .setNeutralButton(android.R.string.ok, /* onClickListener= */ null);
        }
        mStopAppAlertDialog = builder.show();
    }

    @Override
    public void onStopAppSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void dismissShortcutPopup() {
        // TODO (b/268563442): shortcut popup is set to be static since its
        // sometimes recreated when taskview is present, find out why
        if (sCarUiShortcutsPopup != null) {
            sCarUiShortcutsPopup.dismiss();
            sCarUiShortcutsPopup = null;
        }
    }

    private void dismissForceStopMenus() {
        if (sCarUiShortcutsPopup != null) {
            sCarUiShortcutsPopup.dismissImmediate();
            sCarUiShortcutsPopup = null;
        }
        if (mStopAppAlertDialog != null) {
            mStopAppAlertDialog.dismiss();
        }
    }

    /**
     * Comparator for {@link UsageStats} that sorts the list by the "last time used" property
     * in descending order.
     */
    private static class LastTimeUsedComparator implements Comparator<UsageStats> {
        @Override
        public int compare(UsageStats stat1, UsageStats stat2) {
            Long time1 = stat1.getLastTimeUsed();
            Long time2 = stat2.getLastTimeUsed();
            return time2.compareTo(time1);
        }
    }

    private class AppInstallUninstallReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String packageName = intent.getData().getSchemeSpecificPart();
            if (TextUtils.isEmpty(packageName)) {
                Log.e(TAG, "System sent an empty app install/uninstall broadcast");
                return;
            }
            // TODO b/256684061: find better way to get AppInfo from package name.
            initializeLauncherModel();
        }
    }

    /**
     * Private onDragListener for handling dispatching off page scroll event when user holds the app
     * icon at the page margin.
     */
    private class AppGridDragListener implements View.OnDragListener {
        private static final int PAGE_SCROLL_STATE_IDLE = 0;
        private static final int PAGE_SCROLL_STATE_DISPATCHED = 1;
        private final AtomicInteger mOffPageScrollState;
        private final Handler mHandler;

        AppGridDragListener() {
            mOffPageScrollState = new AtomicInteger(PAGE_SCROLL_STATE_IDLE);
            mHandler = new Handler(getMainLooper());
        }

        @Override
        public boolean onDrag(View v, DragEvent event) {
            int action = event.getAction();
            if (AppItemViewHolder.isAppItemDragEvent(event)) {
                if (action == DragEvent.ACTION_DRAG_LOCATION
                        && mOffPageScrollState.get() == PAGE_SCROLL_STATE_IDLE) {
                    if (event.getX() >= mAppGridWidth + mAppGridMargin) {
                        dispatchPageScrollTask(/* scrollToRightPage */ true);
                    } else if (event.getX() <= mAppGridMargin) {
                        dispatchPageScrollTask(/* scrollToRightPage */ false);
                    }
                    // no else case since page margins could still receive other drop events
                    // that has in between ranges.
                }

                if (action == DragEvent.ACTION_DRAG_EXITED) {
                    resetPageScrollState();
                }
            }
            if (action == DragEvent.ACTION_DROP || action == DragEvent.ACTION_DRAG_ENDED) {
                mIsCurrentlyDragging = false;
                mDragCallback.resetCallbackState();
                mLayoutManager.setShouldLayoutChildren(true);
                resetPageScrollState();
            }
            return true;
        }

        private void resetPageScrollState() {
            mHandler.removeCallbacksAndMessages(null);
            mOffPageScrollState.set(PAGE_SCROLL_STATE_IDLE);
        }


        private void dispatchPageScrollTask(boolean scrollToRightPage) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler.postDelayed(new Runnable() {
                public void run() {
                    if (mCurrentScrollState == RecyclerView.SCROLL_STATE_IDLE) {
                        mAdapter.updatePageScrollDestination(scrollToRightPage);
                        mNextScrollDestination = mSnapCallback.getSnapPosition();

                        mLayoutManager.setShouldLayoutChildren(true);
                        mRecyclerView.smoothScrollToPosition(mNextScrollDestination);
                    }
                    // another delayed scroll will be queued to enable the user to input multiple
                    // page scrolls by holding the recyclerview at the app grid margin
                    dispatchPageScrollTask(scrollToRightPage);
                }
            }, mOffPageHoverBeforeScrollMs);
            mOffPageScrollState.set(PAGE_SCROLL_STATE_DISPATCHED);
        }
    }
}