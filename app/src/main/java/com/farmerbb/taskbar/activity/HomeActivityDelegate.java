/* Copyright 2016 Braden Farmer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.farmerbb.taskbar.activity;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.LauncherApps;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.appcompat.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.Display;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.helper.DisplayHelper;
import com.farmerbb.taskbar.helper.GlobalHelper;
import com.farmerbb.taskbar.util.Callbacks;
import com.farmerbb.taskbar.util.TaskbarPosition;
import com.farmerbb.taskbar.service.DashboardService;
import com.farmerbb.taskbar.service.NotificationService;
import com.farmerbb.taskbar.widget.DesktopWidgetManager;
import com.farmerbb.taskbar.service.StartMenuService;
import com.farmerbb.taskbar.service.TaskbarService;
import com.farmerbb.taskbar.ui.DashboardController;
import com.farmerbb.taskbar.ui.UIHost;
import com.farmerbb.taskbar.ui.ViewParams;
import com.farmerbb.taskbar.ui.StartMenuController;
import com.farmerbb.taskbar.ui.TaskbarController;
import com.farmerbb.taskbar.util.AppEntry;
import com.farmerbb.taskbar.util.DesktopIconInfo;
import com.farmerbb.taskbar.util.DesktopSessionState;
import com.farmerbb.taskbar.util.DisplayInfo;
import com.farmerbb.taskbar.util.FABWrapper;
import com.farmerbb.taskbar.helper.FreeformHackHelper;
import com.farmerbb.taskbar.helper.LauncherHelper;
import com.farmerbb.taskbar.helper.MenuHelper;
import com.farmerbb.taskbar.util.U;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.farmerbb.taskbar.util.Constants.*;

public class HomeActivityDelegate extends AppCompatActivity implements UIHost {
    private TaskbarController taskbarController;
    private StartMenuController startMenuController;
    private DashboardController dashboardController;

    private FrameLayout layout;
    // Taskbar, Start menu and dashboard views hosted directly in the desktop layout.
    private final List<View> hostedViews = new ArrayList<>();
    private GridLayout desktopIcons;
    private FABWrapper fab;
    private ImageView wallpaper;

    private boolean forceTaskbarStart = false;
    private AlertDialog dialog;

    private WindowManager windowManager;
    private boolean shouldDelayFreeformHack;
    private int hits;

    private boolean isDesktopIconsEnabled;
    private boolean iconArrangeMode = false;
    private int startDragIndex;
    private int endDragIndex;

    private boolean isSecondaryHome;
    private boolean isDesktopLauncher;
    private DesktopWidgetManager desktopWidgets;
    private TextView desktopAddButton;
    private boolean waitingForPermission;
    // In freeform mode the taskbar floats above app windows as an overlay for the session.
    private boolean floatingTaskbar;
    private boolean desktopStarted;
    private boolean isWallpaperEnabled;
    private boolean isTaskVirtualDisplay;

    private GestureDetector detector;

    private final BroadcastReceiver killReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // The desktop launcher is a regular app, so it must not be killed
            // just because the (unrelated) home screen component got disabled
            if(!isDesktopLauncher)
                killHomeActivity();
        }
    };

    private final BroadcastReceiver exitDesktopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    private final BroadcastReceiver forceTaskbarStartReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            forceTaskbarStart = true;
        }
    };

    private final BroadcastReceiver restartReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(isDesktopLauncher) {
                // Appearance changes redraw the Activity-hosted taskbar in place
                // rather than starting the legacy overlay services.
                boolean wasHosting = taskbarController != null || (floatingTaskbar && desktopStarted);
                destroyDesktopControllers();
                if(floatingTaskbar) stopFloatingTaskbar();
                if(wasHosting) startTaskbar();
                return;
            }
            if(taskbarController != null) taskbarController.onRecreateHost(HomeActivityDelegate.this);
            if(startMenuController != null) startMenuController.onRecreateHost(HomeActivityDelegate.this);
            if(dashboardController != null) dashboardController.onRecreateHost(HomeActivityDelegate.this);
        }
    };

    private final BroadcastReceiver freeformToggleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(isDesktopIconsEnabled == U.isDesktopIconsEnabled(HomeActivityDelegate.this))
                updateWindowFlags();
            else
                recreate();
        }
    };

    private final BroadcastReceiver refreshDesktopIconsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshDesktopIcons();
        }
    };

    private final BroadcastReceiver iconArrangeModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            enterIconArrangeMode();
        }
    };

    private final BroadcastReceiver sortDesktopIconsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            sortDesktopIcons();
        }
    };

    private final BroadcastReceiver updateMarginsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateMargins();
        }
    };

    private final BroadcastReceiver removeDesktopWallpaperReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            removeCustomWallpaper();
        }
    };

    private final BroadcastReceiver wallpaperChangeRequestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            U.showImageChooser(HomeActivityDelegate.this);
        }
    };

    private final LauncherApps.Callback callback = new LauncherApps.Callback() {
        @Override
        public void onPackageRemoved(String packageName, UserHandle user) {
            refreshDesktopIcons();
        }

        @Override
        public void onPackageAdded(String packageName, UserHandle user) {
            refreshDesktopIcons();
        }

        @Override
        public void onPackageChanged(String packageName, UserHandle user) {
            refreshDesktopIcons();
        }

        @Override
        public void onPackagesAvailable(String[] packageNames, UserHandle user, boolean replacing) {
            refreshDesktopIcons();
        }

        @Override
        public void onPackagesUnavailable(String[] packageNames, UserHandle user, boolean replacing) {
            refreshDesktopIcons();
        }
    };

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences pref = U.getSharedPreferences(this);

        isSecondaryHome = this instanceof SecondaryHomeActivity;
        isDesktopLauncher = this instanceof DesktopLauncherActivity;
        if(isDesktopLauncher) {
            LauncherHelper.getInstance().setDesktopLauncherOpen(true);
            updateDesktopNavigationBar();
        }
        if(isSecondaryHome) {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();

            isTaskVirtualDisplay = display.getName().startsWith("TaskVirtualDisplay");

            if(display.getDisplayId() == Display.DEFAULT_DISPLAY
                    || (!U.isDesktopModeActive(this))) {
                finish();
                return;
            }

            if(pref.getBoolean(PREF_DIM_SCREEN, false)
                    && U.launcherIsDefault(this)
                    && !GlobalHelper.getInstance().isOnMainActivity()) {
                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory(Intent.CATEGORY_HOME);
                homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                getApplicationContext().startActivity(homeIntent);
            }
        }

        shouldDelayFreeformHack = true;
        hits = 0;

        WindowManager.LayoutParams params = getWindow().getAttributes();
        if(U.applyDisplayCutoutModeTo(params))
            getWindow().setAttributes(params);

        layout = new FrameLayout(this) {
            @Override
            protected void onAttachedToWindow() {
                super.onAttachedToWindow();

                WallpaperManager wallpaperManager = (WallpaperManager) getSystemService(WALLPAPER_SERVICE);
                try {
                    wallpaperManager.setWallpaperOffsets(getWindowToken(), 0.5f, 0.5f);
                } catch(RuntimeException e) {
                    android.util.Log.w("Andesk", "Wallpaper offsets unavailable", e);
                }

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    DisplayInfo display = U.getDisplayInfo(HomeActivityDelegate.this);
                    if(display.width > 0 && display.height > 0) {
                        try {
                            wallpaperManager.suggestDesiredDimensions(display.width, display.height);
                        } catch (RuntimeException ignored) {}
                    }
                }

                boolean shouldStartFreeformHack = shouldDelayFreeformHack && hits > 0;
                shouldDelayFreeformHack = false;

                if(shouldStartFreeformHack)
                    startFreeformHack();
            }
        };

        isWallpaperEnabled = !isTaskVirtualDisplay && (isSecondaryHome || isDesktopLauncher || U.isChromeOs(this));
        if(isWallpaperEnabled) {
            wallpaper = new ImageView(this);
            wallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);
            layout.addView(wallpaper);
        }

        isDesktopIconsEnabled = !isTaskVirtualDisplay && U.isDesktopIconsEnabled(this);
        if(isDesktopIconsEnabled) {
            layout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    layout.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    if(savedInstanceState != null)
                        iconArrangeMode = savedInstanceState.getBoolean("icon_arrange_mode");

                    initDesktopIcons();
                }
            });
        } else if(!isTaskVirtualDisplay) {
            layout.setOnClickListener(
                    view1 -> U.sendBroadcast(this, ACTION_HIDE_START_MENU));

            layout.setOnLongClickListener(view2 -> {
                if(!U.isFreeformModeEnabled(this))
                    setWallpaper();

                return false;
            });

            layout.setOnGenericMotionListener((view3, motionEvent) -> {
                if(motionEvent.getAction() == MotionEvent.ACTION_BUTTON_PRESS
                        && motionEvent.getButtonState() == MotionEvent.BUTTON_SECONDARY
                        && !U.isFreeformModeEnabled(this))
                    setWallpaper();

                return false;
            });
        }

        layout.setFitsSystemWindows(true);
        if(isDesktopLauncher && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // With gesture navigation the bottom inset holds only the gesture handle,
            // which is not tappable. Let the desktop taskbar reach the bottom edge like
            // a real desktop taskbar; only reserve space for tappable 3-button bars.
            layout.setOnApplyWindowInsetsListener((v, insets) -> {
                android.graphics.Insets system = insets.getSystemWindowInsets();
                android.graphics.Insets tappable = insets.getTappableElementInsets();
                v.setPadding(system.left, system.top, system.right, tappable.bottom);
                return insets.consumeSystemWindowInsets();
            });
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P 
                && isDesktopIconsEnabled
               ) {
            detector = new GestureDetector(this, new GestureDetector.OnGestureListener() {
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return false;
                }

                @Override
                public void onShowPress(MotionEvent e) {}

                @Override
                public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                    return false;
                }

                @Override
                public void onLongPress(MotionEvent e) {}

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    return false;
                }

                @Override
                public boolean onDown(MotionEvent e) {
                    return false;
                }
            });

            detector.setOnDoubleTapListener(new GestureDetector.OnDoubleTapListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    if(!pref.getBoolean(PREF_DONT_SHOW_DOUBLE_TAP_DIALOG, false)
                            && !isSecondaryHome) {
                        if(pref.getBoolean(PREF_DOUBLE_TAP_TO_SLEEP, false)) {
                            U.lockDevice(HomeActivityDelegate.this);
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(U.wrapContext(HomeActivityDelegate.this));
                            builder.setTitle(R.string.tb_double_tap_to_sleep)
                                    .setMessage(R.string.tb_enable_double_tap_to_sleep)
                                    .setNegativeButton(pref.getBoolean(PREF_DOUBLE_TAP_DIALOG_SHOWN, false)
                                            ? R.string.tb_action_dont_show_again
                                            : R.string.tb_action_cancel, (dialog, which) -> pref.edit().putBoolean(pref.getBoolean(PREF_DOUBLE_TAP_DIALOG_SHOWN, false)
                                            ? PREF_DONT_SHOW_DOUBLE_TAP_DIALOG
                                            : PREF_DOUBLE_TAP_DIALOG_SHOWN, true).apply())
                                    .setPositiveButton(R.string.tb_action_ok, (dialog, which) -> {
                                        pref.edit().putBoolean(PREF_DOUBLE_TAP_TO_SLEEP, true).apply();
                                        U.lockDevice(HomeActivityDelegate.this);
                                    });

                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                    }

                    return false;
                }

                @Override
                public boolean onDoubleTapEvent(MotionEvent e) {
                    return false;
                }

                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    return false;
                }

            });
        }

        if((this instanceof HomeActivity
                || isSecondaryHome
                || isDesktopLauncher
               )) {
            setContentView(layout);

            if(isWallpaperEnabled) {
                File file = new File(getFilesDir() + "/tb_images", "desktop_wallpaper");
                if(file.exists()) {
                    U.applyCustomImage(this, "desktop_wallpaper", wallpaper, null);
                    getWindow().setNavigationBarColor(Color.BLACK);
                }
            }

            if(isDesktopLauncher) {
                desktopWidgets = new DesktopWidgetManager(this, layout);
                desktopWidgets.restorePendingState(savedInstanceState);
                updateMargins();
                TextView addDesktopItem = new TextView(this);
                addDesktopItem.setText("+ Add to desktop");
                addDesktopItem.setTextColor(Color.WHITE);
                addDesktopItem.setTextSize(15);
                addDesktopItem.setBackgroundColor(Color.argb(200, 30, 45, 65));
                int padding = Math.round(12 * getResources().getDisplayMetrics().density);
                addDesktopItem.setPadding(padding, padding, padding, padding);
                addDesktopItem.setOnClickListener(v -> {
                    showDesktopMenu();
                    v.setVisibility(View.GONE);
                });
                addDesktopItem.setOnLongClickListener(v -> {
                    startActivity(new Intent(this, MainActivity.class));
                    return true;
                });
                desktopAddButton = addDesktopItem;
                FrameLayout.LayoutParams addParams = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                addParams.gravity = Gravity.TOP | Gravity.END;
                addParams.setMargins(padding, padding, padding, 0);
                layout.addView(addDesktopItem, addParams);
                addDesktopItem.postDelayed(
                        () -> addDesktopItem.setVisibility(View.GONE), 2000);
                layout.setOnLongClickListener(v -> {
                    showDesktopMenu();
                    return true;
                });
                layout.setOnGenericMotionListener((v, motion) -> {
                    if(motion.getAction() == MotionEvent.ACTION_BUTTON_PRESS
                            && (motion.getButtonState() & MotionEvent.BUTTON_SECONDARY) != 0) {
                        showDesktopMenu();
                        return true;
                    }
                    return false;
                });
            }

            // The desktop launcher doesn't register as a system home screen,
            // so it must not touch the home screen / desktop mode preferences
            if(!isDesktopLauncher) {
                pref.edit()
                        .putBoolean(PREF_LAUNCHER, !isSecondaryHome)
                        .putBoolean(PREF_DESKTOP_MODE, U.isDesktopModeSupported(this) && isSecondaryHome)
                        .apply();
            }
        } else
            killHomeActivity();

        updateWindowFlags();

        U.registerReceiver(this, killReceiver, ACTION_KILL_HOME_ACTIVITY);
        U.registerReceiver(this, forceTaskbarStartReceiver, ACTION_FORCE_TASKBAR_RESTART);

        U.registerReceiver(this, freeformToggleReceiver,
                ACTION_UPDATE_FREEFORM_CHECKBOX,
                ACTION_TOUCH_ABSORBER_STATE_CHANGED,
                ACTION_FREEFORM_PREF_CHANGED);

        if(isSecondaryHome || isDesktopLauncher)
            U.registerReceiver(this, restartReceiver, ACTION_RESTART);

        if(isDesktopLauncher)
            U.registerReceiver(this, exitDesktopReceiver, ACTION_EXIT_DESKTOP_LAUNCHER);

        if(isWallpaperEnabled) {
            U.registerReceiver(this, removeDesktopWallpaperReceiver, ACTION_REMOVE_DESKTOP_WALLPAPER);
            U.registerReceiver(this, wallpaperChangeRequestReceiver, ACTION_WALLPAPER_CHANGE_REQUESTED);
        }

        if(isDesktopIconsEnabled) {
            U.registerReceiver(this, refreshDesktopIconsReceiver, ACTION_REFRESH_DESKTOP_ICONS);
            U.registerReceiver(this, iconArrangeModeReceiver, ACTION_ENTER_ICON_ARRANGE_MODE);
            U.registerReceiver(this, sortDesktopIconsReceiver, ACTION_SORT_DESKTOP_ICONS);
            U.registerReceiver(this, updateMarginsReceiver, ACTION_UPDATE_HOME_SCREEN_MARGINS);

            LauncherApps launcherApps = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);
            launcherApps.registerCallback(callback);
        }

        U.initPrefs(this);
    }

    private void openDesktopDocumentPicker(boolean folder) {
        Intent picker = new Intent(folder ? Intent.ACTION_OPEN_DOCUMENT_TREE : Intent.ACTION_OPEN_DOCUMENT);
        if(!folder) {
            picker.setType("*/*");
            picker.addCategory(Intent.CATEGORY_OPENABLE);
        }
        picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(picker, folder ? 9102 : 9101);
        } catch(ActivityNotFoundException error) {
            android.util.Log.w("Andesk", "No Android document picker available", error);
        }
    }

    private void showDesktopMenu() {
        CharSequence[] items = {
                "Add app shortcut",
                "Add file shortcut",
                "Add folder shortcut",
                getString(R.string.tb_add_widget),
                "Taskbar appearance",
                "All settings",
                getString(R.string.tb_set_wallpaper)
        };

        new AlertDialog.Builder(this)
                .setItems(items, (dialog, which) -> {
                    if(which == 0) {
                        if(desktopIcons != null) {
                            try {
                                org.json.JSONArray icons = new org.json.JSONArray(U.getSharedPreferences(this).getString(PREF_DESKTOP_ICONS, "[]"));
                                org.json.JSONArray documents = new org.json.JSONArray(
                                        U.getSharedPreferences(this).getString("andesk_documents", "[]"));
                                for(int i = 0; i < desktopIcons.getChildCount(); i++) {
                                    DesktopIconInfo info = getDesktopIconInfo(i);
                                    if(!isOccupied(icons, info) && !isOccupied(documents, info)) {
                                        Intent picker = U.getThemedIntent(this, DesktopIconSelectAppActivity.class);
                                        picker.putExtra("desktop_icon", info);
                                        startActivity(picker);
                                        break;
                                    }
                                }
                            } catch(org.json.JSONException e) {
                                android.util.Log.w("Andesk", "Unable to read desktop shortcuts", e);
                            }
                        }
                    } else if(which == 1 || which == 2) {
                        openDesktopDocumentPicker(which == 2);
                    } else if(which == 3)
                        desktopWidgets.startAddWidget();
                    else if(which == 6)
                        setWallpaper();
                    else {
                        Intent appearance = new Intent(this, MainActivity.class);
                        appearance.putExtra("theme_change", which == 4);
                        startActivity(appearance);
                    }
                })
                .show();
    }

    private void updateDesktopNavigationBar() {
        if(!isDesktopLauncher)
            return;

        int color = TaskbarPosition.isBottom(this)
                ? U.getTaskbarBackgroundColor(this)
                : Color.TRANSPARENT;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            getWindow().setNavigationBarContrastEnforced(false);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            getWindow().setNavigationBarDividerColor(color);

        getWindow().setNavigationBarColor(color);
    }

    private void setWallpaper() {
        U.sendBroadcast(this, ACTION_TEMP_HIDE_TASKBAR);

        try {
            startActivity(Intent.createChooser(new Intent(Intent.ACTION_SET_WALLPAPER), getString(R.string.tb_set_wallpaper)));
        } catch (ActivityNotFoundException ignored) {}
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    protected void onResume() {
        super.onResume();

        updateDesktopNavigationBar();
        if(isDesktopLauncher) {
            waitingForPermission = false;
            init();
        }

        if(canBootToFreeform()) {
            if(U.launcherIsDefault(this))
                startFreeformHack();
            else {
                U.showToastLong(this, R.string.tb_set_as_default_home);

                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory(Intent.CATEGORY_HOME);
                homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                try {
                    startActivity(homeIntent);
                    finish();
                } catch (ActivityNotFoundException ignored) {}
            }
        } else
            performOnResumeLogic();
    }

    @Override
    protected void onStart() {
        super.onStart();
        desktopStarted = true;

        U.sendBroadcast(this, ACTION_HIDE_START_MENU);
        if(desktopWidgets != null) desktopWidgets.startListening();
        // Reattach in-Activity desktop controllers on resume, not from onStart().
        if(!isDesktopLauncher) init();
    }

    private void init() {
        // The desktop taskbar is drawn inside Andesk itself; external overlay
        // access is optional and must never block the on-screen desktop.
        if(isDesktopLauncher || U.canDrawOverlays(this)) {
            SharedPreferences pref = U.getSharedPreferences(this);
            if(!canBootToFreeform()) {
                setOnHomeScreen(true);

                if(forceTaskbarStart) {
                    forceTaskbarStart = false;
                    U.newHandler().postDelayed(() -> {
                        setOnHomeScreen(true);
                        startTaskbar();
                    }, 250);
                } else
                    startTaskbar();
            } else if(U.launcherIsDefault(this))
                startFreeformHack();
        } else if(!waitingForPermission)
            dialog = U.showPermissionDialog(U.wrapContext(this), new Callbacks(
                    () -> dialog = U.showErrorDialog(U.wrapContext(this), "SYSTEM_ALERT_WINDOW"),
                    () -> waitingForPermission = true));
    }

    private void startTaskbar() {
        // Direct Activity-hosted views stay alive until onStop(). Do not create
        // duplicate taskbars when returning from widget/system Settings screens.
        if(isDesktopLauncher && taskbarController != null && startMenuController != null)
            return;
        if(isDesktopLauncher && floatingTaskbar) {
            if(shouldUseFloatingTaskbar() && U.isServiceRunning(this, TaskbarService.class)) {
                DesktopSessionState.foreground(this);
                U.sendBroadcast(this, ACTION_DESKTOP_TASKBAR_EXPAND);
                return;
            }
            stopFloatingTaskbar();
        }
        // Ensure that the freeform hack is started whenever Taskbar starts
        if(!isDesktopLauncher && U.hasFreeformSupport(this)
                && U.isFreeformModeEnabled(this)
                && !FreeformHackHelper.getInstance().isFreeformHackActive()) {
            U.startFreeformHack(this, true);
        }

        SharedPreferences pref = U.getSharedPreferences(this);
        if(pref.getBoolean(PREF_FIRST_RUN, true)) {
            SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean(PREF_FIRST_RUN, false);
            editor.putBoolean(PREF_COLLAPSED, !isDesktopLauncher);
            editor.apply();

            dialog = U.showRecentAppsDialog(U.wrapContext(this), new Callbacks(
                    () -> dialog = U.showErrorDialog(U.wrapContext(this), "GET_USAGE_STATS"),
                    null));
        }

        if(isSecondaryHome) {
            // Stop any currently running services and switch to using HomeActivityDelegate as UI host
            stopService(new Intent(this, TaskbarService.class));
            stopService(new Intent(this, StartMenuService.class));
            stopService(new Intent(this, DashboardService.class));

            taskbarController = new TaskbarController(this);
            startMenuController = new StartMenuController(this);
            dashboardController = new DashboardController(this);

            taskbarController.onCreateHost(this);
            startMenuController.onCreateHost(this);
            dashboardController.onCreateHost(this);
        } else if(isDesktopLauncher) {
            // The desktop is an ordinary foreground Activity. Host its UI in
            // the Activity's own FrameLayout instead of a TYPE_APPLICATION_OVERLAY
            // service, which Android/OEMs may reject even when permission is granted.
            DesktopSessionState.foreground(this);
            SharedPreferences.Editor editor = pref.edit()
                    .putBoolean(PREF_IS_HIDDEN, false)
                    .putBoolean(PREF_COLLAPSED, false)
                    .putLong(PREF_TIME_OF_SERVICE_START, System.currentTimeMillis());

            if(getResources().getConfiguration().smallestScreenWidthDp >= 600
                    && !pref.contains(PREF_CENTERED_ICONS))
                editor.putBoolean(PREF_CENTERED_ICONS, true);
            editor.apply();

            // A v0.1.1 overlay service may still be running after an upgrade.
            // Never leave a second, detached taskbar over other Android apps.
            if(!shouldUseFloatingTaskbar()) {
                stopService(new Intent(this, TaskbarService.class));
                stopService(new Intent(this, StartMenuService.class));
            }
            stopService(new Intent(this, DashboardService.class));
            if(!shouldUseFloatingTaskbar())
                stopService(new Intent(this, NotificationService.class));

            // In freeform mode, overlay services draw the taskbar and Start menu above
            // app windows; otherwise they are hosted inside this Activity.
            if(!startFloatingTaskbar())
                attachDesktopControllers();
        } else {
            // Legacy background and secondary-launcher modes still use overlay services.
            try {
                startService(new Intent(this, TaskbarService.class));
                startService(new Intent(this, StartMenuService.class));
                startService(new Intent(this, DashboardService.class));
            } catch(RuntimeException error) {
                android.util.Log.e("Andesk", "Legacy overlay service startup failed", error);
            }
        }

        // Service startup is asynchronous. Do not clear the desktop-active flag
        // immediately after startService(), or NotificationService can observe a
        // false flag and shut itself down before its onCreate() completes.
        if(!isDesktopLauncher
                && pref.getBoolean(PREF_TASKBAR_ACTIVE, false)
                && !U.isServiceRunning(this, NotificationService.class))
            pref.edit().putBoolean(PREF_TASKBAR_ACTIVE, false).apply();

        // Show the Taskbar temporarily, as nothing else will be visible on screen
        U.newHandler().postDelayed(() ->
                U.sendBroadcast(this, ACTION_TEMP_SHOW_TASKBAR), 100);
    }

    private void attachDesktopControllers() {
        try {
            if(taskbarController == null) {
                taskbarController = new TaskbarController(this);
                taskbarController.onCreateHost(this);
            }
        } catch(RuntimeException error) {
            android.util.Log.e("Andesk", "Taskbar could not attach to desktop Activity", error);
            if(taskbarController != null) {
                try { taskbarController.onDestroyHost(this); }
                catch(RuntimeException cleanupError) {
                    android.util.Log.w("Andesk", "Taskbar cleanup failed", cleanupError);
                }
                taskbarController = null;
            }
            new AlertDialog.Builder(this)
                    .setTitle("Taskbar startup failed")
                    .setMessage("Could not draw the taskbar: " + error.getClass().getSimpleName()
                            + ". Please report this error.")
                    .setPositiveButton(R.string.tb_action_ok, null)
                    .show();
        }

        try {
            if(startMenuController == null) {
                startMenuController = new StartMenuController(this);
                startMenuController.onCreateHost(this);
            }
        } catch(RuntimeException error) {
            android.util.Log.e("Andesk", "Start menu could not attach to desktop Activity", error);
            if(startMenuController != null) {
                try { startMenuController.onDestroyHost(this); }
                catch(RuntimeException cleanupError) {
                    android.util.Log.w("Andesk", "Start menu cleanup failed", cleanupError);
                }
                startMenuController = null;
            }
        }

        try {
            if(dashboardController == null) {
                dashboardController = new DashboardController(this);
                dashboardController.onCreateHost(this);
            }
        } catch(RuntimeException error) {
            android.util.Log.w("Andesk", "Dashboard unavailable", error);
            dashboardController = null;
        }
    }

    /** Freeform windows sit above this Activity, so only an overlay taskbar can stay usable. */
    private boolean shouldUseFloatingTaskbar() {
        return isDesktopLauncher
                && U.hasFreeformSupport(this)
                && U.isFreeformModeEnabled(this)
                && U.canDrawOverlays(this);
    }

    private boolean startFloatingTaskbar() {
        if(!shouldUseFloatingTaskbar()) return false;

        destroyDesktopControllers();
        try {
            startService(new Intent(this, TaskbarService.class));
            startService(new Intent(this, StartMenuService.class));
            // A foreground notification keeps the floating taskbar alive after leaving the
            // desktop (Android stops background services) and lets the user close it.
            requestNotificationPermissionOnce();
            U.startForegroundService(this, new Intent(this, NotificationService.class));
        } catch(RuntimeException error) {
            android.util.Log.e("Andesk", "Floating taskbar could not start", error);
            stopFloatingTaskbar();
            return false;
        }

        floatingTaskbar = true;
        U.sendBroadcast(this, ACTION_DESKTOP_TASKBAR_EXPAND);

        // Some devices reject overlay windows after the service starts. Fall back to the
        // in-Activity taskbar rather than leaving the desktop without one.
        U.newHandler().postDelayed(() -> {
            if(floatingTaskbar && desktopStarted && !isFinishing()
                    && !U.isServiceRunning(this, TaskbarService.class)) {
                android.util.Log.w("Andesk", "Floating taskbar unavailable; using desktop taskbar");
                stopFloatingTaskbar();
                attachDesktopControllers();
            }
        }, 1500);
        return true;
    }

    private void stopFloatingTaskbar() {
        floatingTaskbar = false;
        stopService(new Intent(this, TaskbarService.class));
        stopService(new Intent(this, StartMenuService.class));
        stopService(new Intent(this, NotificationService.class));
    }

    private void requestNotificationPermissionOnce() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED)
            return;

        SharedPreferences pref = U.getSharedPreferences(this);
        if(pref.getBoolean("andesk_notification_permission_requested", false))
            return;

        pref.edit().putBoolean("andesk_notification_permission_requested", true).apply();
        requestPermissions(new String[] { android.Manifest.permission.POST_NOTIFICATIONS }, 43);
    }

    private boolean canBootToFreeform() {
        // The desktop launcher is never the system home screen, so it can't boot to freeform
        return !isDesktopLauncher && U.canBootToFreeform(this);
    }

    private void startFreeformHack() {
        if(shouldDelayFreeformHack)
            hits++;
        else
            U.startFreeformHack(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        desktopStarted = false;

        if(desktopWidgets != null) {
            desktopWidgets.exitEditMode();
            desktopWidgets.stopListening();
        }

        SharedPreferences pref = U.getSharedPreferences(this);
        if(!canBootToFreeform()) {
            if(!isDesktopLauncher && U.shouldCollapse(this, false)) {
                U.sendBroadcast(this, ACTION_TEMP_HIDE_TASKBAR);
            }

            if(isSecondaryHome) {
                if(taskbarController != null) taskbarController.onDestroyHost(this);
                if(startMenuController != null) startMenuController.onDestroyHost(this);
                if(dashboardController != null) dashboardController.onDestroyHost(this);

                U.clearCaches(this);

                // Stop using HomeActivityDelegate as UI host and restart services if needed
                if(pref.getBoolean(PREF_TASKBAR_ACTIVE, false) && !pref.getBoolean(PREF_IS_HIDDEN, false)) {
                    startService(new Intent(this, TaskbarService.class));
                    startService(new Intent(this, StartMenuService.class));
                    startService(new Intent(this, DashboardService.class));
                }
            } else {
                if(isDesktopLauncher && floatingTaskbar && !isFinishing()) {
                    // Freeform mode: shrink the floating taskbar to its Start button so
                    // more apps can be opened without covering the current one. It is
                    // removed when the desktop session ends.
                    U.sendBroadcast(this, ACTION_DESKTOP_TASKBAR_COMPACT);
                } else if(isDesktopLauncher) {
                    // Opening another app must not end the desktop session, but
                    // no taskbar, Start menu or notification should remain over it.
                    destroyDesktopControllers();
                    DesktopSessionState.background(this);
                    stopService(new Intent(this, TaskbarService.class));
                    stopService(new Intent(this, StartMenuService.class));
                    stopService(new Intent(this, DashboardService.class));
                    stopService(new Intent(this, NotificationService.class));
                } else if(!pref.getBoolean(PREF_TASKBAR_ACTIVE, false)
                        || pref.getBoolean(PREF_IS_HIDDEN, false)) {
                    stopService(new Intent(this, TaskbarService.class));
                    stopService(new Intent(this, StartMenuService.class));

                    if(!pref.getBoolean(PREF_DONT_STOP_DASHBOARD, false))
                        stopService(new Intent(this, DashboardService.class));

                    U.clearCaches(this);
                }
            }

            if(isChangingConfigurations())
                setOnHomeScreen(false);
            else
                U.newHandler().post(() -> setOnHomeScreen(false));
        }

        if(dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(isDesktopLauncher) {
            U.unregisterReceiver(this, exitDesktopReceiver);

            // A finished activity is a closed session; do not stop on rotation.
            if(isFinishing() && !isChangingConfigurations())
                stopDesktopSession();
        }

        U.unregisterReceiver(this, killReceiver);
        U.unregisterReceiver(this, forceTaskbarStartReceiver);
        U.unregisterReceiver(this, freeformToggleReceiver);

        if(isSecondaryHome || isDesktopLauncher)
            U.unregisterReceiver(this, restartReceiver);

        if(isWallpaperEnabled) {
            U.unregisterReceiver(this, removeDesktopWallpaperReceiver);
            U.unregisterReceiver(this, wallpaperChangeRequestReceiver);
        }

        if(isDesktopIconsEnabled) {
            U.unregisterReceiver(this, refreshDesktopIconsReceiver);
            U.unregisterReceiver(this, iconArrangeModeReceiver);
            U.unregisterReceiver(this, sortDesktopIconsReceiver);
            U.unregisterReceiver(this, updateMarginsReceiver);

            LauncherApps launcherApps = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);
            launcherApps.unregisterCallback(callback);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("icon_arrange_mode", iconArrangeMode);
        if(desktopWidgets != null) desktopWidgets.savePendingState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if(isDesktopLauncher) {
            // Windows/Meta key opens and closes the start menu
            if((keyCode == KeyEvent.KEYCODE_META_LEFT || keyCode == KeyEvent.KEYCODE_META_RIGHT)
                    && !event.isAltPressed() && !event.isCtrlPressed()) {
                U.sendBroadcast(this, ACTION_TOGGLE_START_MENU);
                return true;
            }

            // Desktop shortcuts remain available from a physical keyboard.
            if(keyCode == KeyEvent.KEYCODE_N && event.isCtrlPressed() && event.isShiftPressed()) {
                openDesktopDocumentPicker(true);
                return true;
            }
            if(keyCode == KeyEvent.KEYCODE_A && event.isCtrlPressed() && event.isAltPressed()) {
                showDesktopMenu();
                return true;
            }

            if(keyCode == KeyEvent.KEYCODE_S && event.isCtrlPressed() && event.isAltPressed()) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            }
            if(keyCode == KeyEvent.KEYCODE_F10 && event.isShiftPressed()) {
                showDesktopMenu();
                return true;
            }
            if(keyCode == KeyEvent.KEYCODE_F && event.isCtrlPressed() && event.isShiftPressed()) {
                openDesktopDocumentPicker(false);
                return true;
            }
            if(keyCode == KeyEvent.KEYCODE_ESCAPE && MenuHelper.getInstance().isStartMenuOpen()) {
                U.sendBroadcast(this, ACTION_HIDE_START_MENU);
                return true;
            }
            // Alt+F4 exits the desktop
            if(keyCode == KeyEvent.KEYCODE_F4 && event.isAltPressed()) {
                Intent quitIntent = new Intent(ACTION_QUIT);
                quitIntent.setPackage(getPackageName());
                quitIntent.putExtra(EXTRA_DESKTOP_SESSION, true);
                sendBroadcast(quitIntent);
                return true;
            }
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if(desktopWidgets != null && desktopWidgets.exitEditMode())
            return;

        if(isDesktopLauncher && !MenuHelper.getInstance().isStartMenuOpen()) {
            Intent quitIntent = new Intent(ACTION_QUIT);
            quitIntent.setPackage(getPackageName());
            quitIntent.putExtra(EXTRA_DESKTOP_SESSION, true);
            sendBroadcast(quitIntent);
            return;
        }

        U.sendBroadcast(this, ACTION_HIDE_START_MENU);
    }

    private void killHomeActivity() {
        if(isSecondaryHome) {
            if(taskbarController != null) taskbarController.onDestroyHost(this);
            if(startMenuController != null) startMenuController.onDestroyHost(this);
            if(dashboardController != null) dashboardController.onDestroyHost(this);

            U.clearCaches(this);
            U.stopFreeformHack(this);

            // Stop using HomeActivityDelegate as UI host and restart services if needed
            SharedPreferences pref = U.getSharedPreferences(this);
            if(pref.getBoolean(PREF_TASKBAR_ACTIVE, false) && !pref.getBoolean(PREF_IS_HIDDEN, false)) {
                startService(new Intent(this, TaskbarService.class));
                startService(new Intent(this, StartMenuService.class));
                startService(new Intent(this, DashboardService.class));
            }
        } else {
            // Stop the Taskbar and Start Menu services if they should normally not be active
            SharedPreferences pref = U.getSharedPreferences(this);
            if(!pref.getBoolean(PREF_TASKBAR_ACTIVE, false) || pref.getBoolean(PREF_IS_HIDDEN, false)) {
                stopService(new Intent(this, TaskbarService.class));
                stopService(new Intent(this, StartMenuService.class));
                stopService(new Intent(this, DashboardService.class));

                U.clearCaches(this);
                U.stopFreeformHack(this);
            }
        }

        U.newHandler().post(() -> {
            setOnHomeScreen(false);
            finish();
        });
    }

    private void destroyDesktopControllers() {
        if(dashboardController != null) {
            try { dashboardController.onDestroyHost(this); }
            catch(RuntimeException error) {
                android.util.Log.w("Andesk", "Dashboard cleanup failed", error);
            }
            dashboardController = null;
        }
        if(startMenuController != null) {
            try { startMenuController.onDestroyHost(this); }
            catch(RuntimeException error) {
                android.util.Log.w("Andesk", "Start menu cleanup failed", error);
            }
            startMenuController = null;
        }
        if(taskbarController != null) {
            try { taskbarController.onDestroyHost(this); }
            catch(RuntimeException error) {
                android.util.Log.w("Andesk", "Taskbar cleanup failed", error);
            }
            taskbarController = null;
        }
    }

    private void stopDesktopSession() {
        destroyDesktopControllers();
        floatingTaskbar = false;
        LauncherHelper.getInstance().setDesktopLauncherOpen(false);

        DesktopSessionState.close(this);

        setOnHomeScreen(false);

        stopService(new Intent(this, TaskbarService.class));
        stopService(new Intent(this, StartMenuService.class));
        stopService(new Intent(this, DashboardService.class));
        stopService(new Intent(this, NotificationService.class));

        U.clearCaches(this);
        U.stopFreeformHack(this);
        U.sendBroadcast(this, ACTION_START_MENU_DISAPPEARING);
    }

    private void updateWindowFlags() {
        int flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        if(isDesktopLauncher) {
            // The real desktop must always remain touchable. The upstream
            // freeform touch absorber is unrelated to this on-screen host.
            getWindow().clearFlags(flags);
            return;
        }
        if(FreeformHackHelper.getInstance().isTouchAbsorberActive() && U.isOverridingFreeformHack(this))
            getWindow().setFlags(flags, flags);
        else
            getWindow().clearFlags(flags);
    }

    @Override
    public void addView(View view, ViewParams params) {
        if(isTaskVirtualDisplay) return;
        if(isDesktopLauncher) {
            layout.addView(view, params.toFrameLayoutParams());
            hostedViews.add(view);
            view.bringToFront();
        } else
            windowManager.addView(view, params.toWindowManagerParams());
    }

    @Override
    public void removeView(View view) {
        if(isTaskVirtualDisplay) return;
        if(isDesktopLauncher) {
            hostedViews.remove(view);
            if(view.getParent() == layout) layout.removeView(view);
        } else
            windowManager.removeView(view);
    }

    @Override
    public void terminate() {
        // no-op
    }

    @Override
    public void updateViewLayout(View view, ViewParams params) {
        if(isTaskVirtualDisplay) return;
        if(isDesktopLauncher) {
            if(view.getParent() == layout)
                view.setLayoutParams(params.toFrameLayoutParams());
        } else
            windowManager.updateViewLayout(view, params.toWindowManagerParams());
    }

    private void initDesktopIcons() {
        desktopIcons = new GridLayout(this);
        fab = new FABWrapper(this);

        updateMargins();
        refreshDesktopIcons();

        fab.setImageResource(R.drawable.tb_done);
        fab.view.setOnClickListener(v -> {
            iconArrangeMode = false;
            fab.hide();
            refreshDesktopIcons();
        });

        if(!iconArrangeMode) fab.hide();

        layout.addView(desktopIcons);
        layout.addView(fab.view);
        if(desktopWidgets != null) desktopWidgets.bringToFront();
        if(desktopAddButton != null) desktopAddButton.bringToFront();
        bringHostedViewsToFront();
    }

    /** The taskbar and Start menu must stay above icons and widgets added later. */
    private void bringHostedViewsToFront() {
        for(View view : new ArrayList<>(hostedViews)) {
            if(view.getParent() == layout) view.bringToFront();
        }
    }

    private void refreshDesktopIcons() {
        if(desktopIcons == null) return;

        boolean taskbarIsVertical = TaskbarPosition.isVertical(this);
        int iconSize = U.getTaskbarIconSize(this);
        int desktopIconSize = getResources().getDimensionPixelSize(R.dimen.tb_start_menu_grid_width);

        int columns = Math.max(1, (layout.getWidth() - (taskbarIsVertical ? iconSize : 0)) / desktopIconSize);
        int rows = Math.max(1, (layout.getHeight() - (!taskbarIsVertical ? iconSize : 0)) / desktopIconSize);

        desktopIcons.removeAllViews();
        desktopIcons.setOrientation(GridLayout.VERTICAL);
        desktopIcons.setColumnCount(columns);
        desktopIcons.setRowCount(rows);

        LauncherApps launcherApps = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);
        UserManager userManager = (UserManager) getSystemService(USER_SERVICE);

        SparseArray<DesktopIconInfo> icons = new SparseArray<>();
        List<Integer> iconsToRemove = new ArrayList<>();

        try {
            SharedPreferences pref = U.getSharedPreferences(this);
            JSONArray jsonIcons = new JSONArray(pref.getString(PREF_DESKTOP_ICONS, "[]"));

            for(int i = 0; i < jsonIcons.length(); i++) {
                DesktopIconInfo info = DesktopIconInfo.fromJson(jsonIcons.getJSONObject(i));
                if(info != null) {
                    if(launcherApps.isActivityEnabled(
                            ComponentName.unflattenFromString(info.entry.getComponentName()),
                            userManager.getUserForSerialNumber(info.entry.getUserId(this))))
                        icons.put(getIndex(info), info);
                    else
                        iconsToRemove.add(i);
                }
            }

            if(!iconsToRemove.isEmpty()) {
                // Remove from the end: removing ascending JSON indices skips items.
                for(int i = iconsToRemove.size() - 1; i >= 0; i--) {
                    jsonIcons.remove(iconsToRemove.get(i));
                }

                pref.edit().putString(PREF_DESKTOP_ICONS, jsonIcons.toString()).apply();
            }
        } catch (JSONException ignored) {}

        JSONArray documents = new JSONArray();
        try { documents = new JSONArray(U.getSharedPreferences(this).getString("andesk_documents", "[]")); }
        catch(org.json.JSONException ignored) {}

        if(desktopAddButton != null && (icons.size() > 0 || documents.length() > 0))
            desktopAddButton.setVisibility(View.GONE);
        for(int i = 0; i < columns * rows; i++) {
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f),
                    GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f));

            params.width = 0;
            params.height = 0;

            FrameLayout iconContainer = new FrameLayout(this);
            iconContainer.setLayoutParams(params);
            iconContainer.setOnDragListener(new DesktopIconDragListener());

            int index = i;

            iconContainer.setOnClickListener(view -> {
                boolean isStartMenuOpen = MenuHelper.getInstance().isStartMenuOpen();
                U.sendBroadcast(this, ACTION_HIDE_START_MENU);

                DesktopIconInfo info = icons.get(index);
                if(!isStartMenuOpen && info != null && info.entry != null) {
                    U.launchApp(
                            this,
                            info.entry,
                            null,
                            false,
                            false,
                            view
                    );
                }
            });

            iconContainer.setOnLongClickListener(view -> {
                DesktopIconInfo info = icons.get(index);
                if(isDesktopLauncher && (info == null || info.entry == null)) {
                    showDesktopMenu();
                    return true;
                }

                if(isDesktopLauncher && info != null && info.entry != null
                        && iconContainer.getChildCount() > 0) {
                    startDragIndex = index;
                    View iconView = iconContainer.getChildAt(0);
                    ClipData data = ClipData.newPlainText("", "");
                    View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(iconView);
                    iconView.startDrag(data, shadowBuilder, iconView, 0);
                    iconView.setVisibility(View.INVISIBLE);
                    return true;
                }

                int[] location = new int[2];
                view.getLocationOnScreen(location);
                if(info == null) info = getDesktopIconInfo(index);

                openContextMenu(info, location);
                return true;
            });

            iconContainer.setOnGenericMotionListener((view, motionEvent) -> {
                int action = motionEvent.getAction();

                if(action == MotionEvent.ACTION_BUTTON_PRESS
                        && motionEvent.getButtonState() == MotionEvent.BUTTON_SECONDARY) {
                    int[] location = new int[2];
                    view.getLocationOnScreen(location);

                    DesktopIconInfo info = icons.get(index);
                    if(isDesktopLauncher && (info == null || info.entry == null)) {
                        showDesktopMenu();
                        return true;
                    }
                    if(info == null) info = getDesktopIconInfo(index);

                    openContextMenu(info, location);
                }

                return false;
            });

            iconContainer.setOnTouchListener((v, event) -> {
                if(detector != null)
                    detector.onTouchEvent(event);

                return false;
            });

            iconContainer.setFocusable(false);

            DesktopIconInfo info = icons.get(index);
            if(info != null && info.entry != null && info.column < columns && info.row < rows)
                iconContainer.addView(inflateDesktopIcon(iconContainer, info.entry));

            if(info == null) {
                for(int j = 0; j < documents.length(); j++) {
                    org.json.JSONObject document = documents.optJSONObject(j);
                    if(document == null) continue;
                    DesktopIconInfo slot = getDesktopIconInfo(index);
                    if(document.optInt("column", -1) == slot.column && document.optInt("row", -1) == slot.row) {
                        TextView shortcutView = new TextView(this);
                        shortcutView.setText((document.optBoolean("folder") ? "📁 " : "📄 ") + document.optString("title", "Shortcut"));
                        shortcutView.setTextColor(Color.WHITE);
                        shortcutView.setOnClickListener(v -> openDocumentShortcut(document));
                        shortcutView.setOnLongClickListener(v -> {
                            showDocumentShortcutMenu(document);
                            return true;
                        });
                        shortcutView.setOnGenericMotionListener((v, motion) -> {
                            if(motion.getAction() == MotionEvent.ACTION_BUTTON_PRESS
                                    && (motion.getButtonState() & MotionEvent.BUTTON_SECONDARY) != 0) {
                                showDocumentShortcutMenu(document);
                                return true;
                            }
                            return false;
                        });
                        iconContainer.addView(shortcutView);
                        break;
                    }
                }
            }
            desktopIcons.addView(iconContainer);
        }
    }

    private void sortDesktopIcons() {
        try {
            SharedPreferences pref = U.getSharedPreferences(this);
            JSONArray jsonIcons = new JSONArray(pref.getString(PREF_DESKTOP_ICONS, "[]"));

            if(jsonIcons.length() == 0) {
                U.showToast(this, R.string.tb_no_icons_to_sort);
                return;
            }

            List<DesktopIconInfo> icons = new ArrayList<>();

            for(int i = 0; i < jsonIcons.length(); i++) {
                DesktopIconInfo info = DesktopIconInfo.fromJson(jsonIcons.getJSONObject(i));
                if(info != null)
                    icons.add(info);
            }

            Collections.sort(icons, (o1, o2) -> Collator.getInstance().compare(o1.entry.getLabel(), o2.entry.getLabel()));

            jsonIcons = new JSONArray();

            for(int i = 0; i < icons.size(); i++) {
                DesktopIconInfo oldInfo = icons.get(i);
                DesktopIconInfo newInfo = getDesktopIconInfo(i);

                oldInfo.column = newInfo.column;
                oldInfo.row = newInfo.row;

                jsonIcons.put(oldInfo.toJson(this));
            }

            pref.edit().putString(PREF_DESKTOP_ICONS, jsonIcons.toString()).apply();
            refreshDesktopIcons();
        } catch (JSONException ignored) {}
    }

    private void reassignDroppedIcon() {
        if(startDragIndex == endDragIndex) return;

        try {
            SharedPreferences pref = U.getSharedPreferences(this);
            JSONArray jsonIcons = new JSONArray(pref.getString(PREF_DESKTOP_ICONS, "[]"));
            JSONArray documents = new JSONArray(pref.getString("andesk_documents", "[]"));
            DesktopIconInfo oldSlot = getDesktopIconInfo(startDragIndex);
            DesktopIconInfo newSlot = getDesktopIconInfo(endDragIndex);

            if(isOccupied(documents, newSlot)) return;

            int sourceIndex = -1;
            int targetIndex = -1;
            DesktopIconInfo source = null;
            DesktopIconInfo target = null;

            for(int i = 0; i < jsonIcons.length(); i++) {
                DesktopIconInfo info = DesktopIconInfo.fromJson(jsonIcons.getJSONObject(i));
                if(info == null) continue;
                if(info.column == oldSlot.column && info.row == oldSlot.row) {
                    sourceIndex = i;
                    source = info;
                } else if(info.column == newSlot.column && info.row == newSlot.row) {
                    targetIndex = i;
                    target = info;
                }
            }

            if(sourceIndex < 0 || source == null) return;

            source.column = newSlot.column;
            source.row = newSlot.row;
            jsonIcons.put(sourceIndex, source.toJson(this));

            if(targetIndex >= 0 && target != null) {
                target.column = oldSlot.column;
                target.row = oldSlot.row;
                jsonIcons.put(targetIndex, target.toJson(this));
            }

            pref.edit().putString(PREF_DESKTOP_ICONS, jsonIcons.toString()).apply();
        } catch (JSONException ignored) {}
    }

    private void enterIconArrangeMode() {
        try {
            SharedPreferences pref = U.getSharedPreferences(this);
            JSONArray jsonIcons = new JSONArray(pref.getString(PREF_DESKTOP_ICONS, "[]"));

            if(jsonIcons.length() == 0) {
                U.showToast(this, R.string.tb_no_icons_to_arrange);
                return;
            }

            fab.view.setBackgroundTintList(
                    ColorStateList.valueOf(ColorUtils.setAlphaComponent(U.getAccentColor(this), 255)));

            iconArrangeMode = true;
            fab.show();
        } catch (JSONException ignored) {}
    }

    // Space taken up by the taskbar, as {left, top, right, bottom}
    private int[] getTaskbarMargins() {
        String position = TaskbarPosition.getTaskbarPosition(this);
        int iconSize = U.getTaskbarIconSize(this);

        int[] margins = new int[4];
        if(TaskbarPosition.isVerticalLeft(position))
            margins[0] = iconSize;
        else if(TaskbarPosition.isVerticalRight(position))
            margins[2] = iconSize;
        else if(TaskbarPosition.isBottom(position))
            margins[3] = iconSize;
        else
            margins[1] = iconSize;

        return margins;
    }

    private void updateMargins() {
        if(desktopWidgets != null) {
            int[] m = getTaskbarMargins();
            desktopWidgets.setMargins(m[0], m[1], m[2], m[3]);
        }

        if(desktopIcons == null || fab == null) return;

        int[] taskbarMargins = getTaskbarMargins();
        int left = taskbarMargins[0];
        int top = taskbarMargins[1];
        int right = taskbarMargins[2];
        int bottom = taskbarMargins[3];

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );

        params.setMargins(left, top, right, bottom);
        desktopIcons.setLayoutParams(params);

        int fabMargin = getResources().getDimensionPixelSize(R.dimen.tb_desktop_icon_fab_margin);
        left += fabMargin;
        top += fabMargin;
        right += fabMargin;
        bottom += fabMargin;

        FrameLayout.LayoutParams params2 = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );

        params2.gravity = Gravity.BOTTOM | Gravity.END;
        params2.setMargins(left, top, right, bottom);
        fab.view.setLayoutParams(params2);
    }

    private int getIndex(DesktopIconInfo info) {
        return (info.column * desktopIcons.getRowCount()) + info.row;
    }

    private DesktopIconInfo getDesktopIconInfo(int index) {
        int row = index % Math.max(1, desktopIcons.getRowCount());

        int pos = index;
        int column = -1;

        while(pos >= 0) {
            pos -= Math.max(1, desktopIcons.getRowCount());
            column++;
        }

        return new DesktopIconInfo(column, row, null);
    }

    private View inflateDesktopIcon(ViewGroup parent, AppEntry entry) {
        SharedPreferences pref = U.getSharedPreferences(this);
        View icon = LayoutInflater.from(this).inflate(R.layout.tb_row_alt, parent, false);

        TextView textView = icon.findViewById(R.id.name);
        textView.setText(pref.getBoolean(PREF_HIDE_ICON_LABELS, false) ? "" : entry.getLabel());
        textView.setTextColor(ContextCompat.getColor(this, R.color.tb_desktop_icon_text));
        textView.setShadowLayer(10, 0, 0, R.color.tb_desktop_icon_shadow);

        ImageView imageView = icon.findViewById(R.id.icon);
        imageView.setImageDrawable(entry.getIcon(this));

        icon.setOnTouchListener(new DesktopIconTouchListener());
        return icon;
    }

    private void openContextMenu(final DesktopIconInfo info, final int[] location) {
        if(iconArrangeMode) return;

        Bundle args = new Bundle();
        args.putSerializable("app_entry", info.entry);
        args.putSerializable("desktop_icon", info);
        args.putInt("x", location[0]);
        args.putInt("y", location[1]);

        U.startContextMenuActivity(this, args);
    }

    private void openDesktopIconMenu(int index, View anchor) {
        if(desktopIcons == null || index < 0) return;
        DesktopIconInfo slot = getDesktopIconInfo(index);
        try {
            JSONArray jsonIcons = new JSONArray(
                    U.getSharedPreferences(this).getString(PREF_DESKTOP_ICONS, "[]"));
            for(int i = 0; i < jsonIcons.length(); i++) {
                DesktopIconInfo info = DesktopIconInfo.fromJson(jsonIcons.getJSONObject(i));
                if(info != null && info.entry != null
                        && info.column == slot.column && info.row == slot.row) {
                    int[] location = new int[2];
                    anchor.getLocationOnScreen(location);
                    openContextMenu(info, location);
                    return;
                }
            }
        } catch(JSONException e) {
            android.util.Log.w("Andesk", "Unable to read desktop shortcuts", e);
        }
    }

    private final class DesktopIconTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if(iconArrangeMode && motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                startDragIndex = desktopIcons.indexOfChild((ViewGroup) view.getParent());

                ClipData data = ClipData.newPlainText("", "");
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                view.startDrag(data, shadowBuilder, view, 0);
                view.setVisibility(View.INVISIBLE);
                return true;
            } else
                return false;
        }
    }

    private final class DesktopIconDragListener implements View.OnDragListener {
        @Override
        public boolean onDrag(View v, DragEvent event) {
            switch(event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return isDesktopAppDrag(event) || event.getLocalState() instanceof View;
                default:
                    // do nothing
                    break;
                case DragEvent.ACTION_DRAG_ENTERED:
                    FrameLayout container = (FrameLayout) v;
                    if(container.getChildCount() == 0
                            || startDragIndex == desktopIcons.indexOfChild(container)) {
                        v.setBackgroundColor(U.getAccentColor(HomeActivityDelegate.this));
                        v.setAlpha(0.5f);
                    }
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    View view = event.getLocalState() instanceof View ? (View) event.getLocalState() : null;
                    if(view != null) view.setVisibility(View.VISIBLE);
                    // fall through
                case DragEvent.ACTION_DRAG_EXITED:
                    v.setBackground(null);
                    v.setAlpha(1);
                    break;
                case DragEvent.ACTION_DROP:
                    if(isDesktopAppDrag(event)) {
                        addDroppedApp(event, desktopIcons.indexOfChild(v));
                        return true;
                    }
                    if(!(event.getLocalState() instanceof View)) return false;
                    endDragIndex = desktopIcons.indexOfChild(v);
                    if(endDragIndex >= 0 && endDragIndex != startDragIndex) {
                        reassignDroppedIcon();
                        refreshDesktopIcons();
                    } else if(endDragIndex >= 0 && isDesktopLauncher && !iconArrangeMode) {
                        // Long-press released without moving: show the icon's menu
                        // (open settings, remove from desktop, app info, uninstall).
                        int menuIndex = endDragIndex;
                        v.post(() -> openDesktopIconMenu(menuIndex, v));
                    }
                    return true;
            }
            return true;
        }
    }

    private boolean isDesktopAppDrag(DragEvent event) {
        return event.getClipDescription() != null
                && event.getClipDescription().hasMimeType("text/plain")
                && "andesk-app".contentEquals(event.getClipDescription().getLabel());
    }

    private void addDroppedApp(DragEvent event, int index) {
        if(index < 0 || !isDesktopAppDrag(event) || event.getClipData() == null) return;
        try {
            String payload = event.getClipData().getItemAt(0).coerceToText(this).toString();
            if(!payload.startsWith("andesk-app:")) return;
            DesktopIconInfo app = DesktopIconInfo.fromJson(new org.json.JSONObject(payload.substring(11)));
            if(app == null) return;
            DesktopIconInfo slot = getDesktopIconInfo(index);
            app.column = slot.column;
            app.row = slot.row;
            SharedPreferences pref = U.getSharedPreferences(this);
            JSONArray entries = new JSONArray(pref.getString(PREF_DESKTOP_ICONS, "[]"));
            for(int i = 0; i < entries.length(); i++) {
                org.json.JSONObject old = entries.getJSONObject(i);
                if(old.optInt("column", -1) == slot.column && old.optInt("row", -1) == slot.row) return;
            }
            if(isOccupied(new JSONArray(pref.getString("andesk_documents", "[]")), slot)) return;
            entries.put(app.toJson(this));
            pref.edit().putString(PREF_DESKTOP_ICONS, entries.toString()).apply();
            refreshDesktopIcons();
        } catch(Exception error) {
            android.util.Log.w("Andesk", "Could not add dropped app", error);
        }
    }

    private void saveDocumentShortcut(android.net.Uri uri, boolean folder, int resultFlags) {
        try {
            getContentResolver().takePersistableUriPermission(uri,
                    resultFlags & Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch(SecurityException ignored) {}
        if(desktopIcons == null) return;
        try {
            SharedPreferences pref = U.getSharedPreferences(this);
            JSONArray apps = new JSONArray(pref.getString(PREF_DESKTOP_ICONS, "[]"));
            JSONArray documents = new JSONArray(pref.getString("andesk_documents", "[]"));
            for(int index = 0; index < desktopIcons.getChildCount(); index++) {
                DesktopIconInfo slot = getDesktopIconInfo(index);
                if(isOccupied(apps, slot) || isOccupied(documents, slot)) continue;
                String title = uri.getLastPathSegment();
                if(!folder) {
                    try(android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                        if(cursor != null && cursor.moveToFirst()) {
                            int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                            if(nameIndex >= 0) title = cursor.getString(nameIndex);
                        }
                    }
                }
                org.json.JSONObject document = new org.json.JSONObject();
                document.put("column", slot.column);
                document.put("row", slot.row);
                document.put("uri", uri.toString());
                document.put("folder", folder);
                document.put("title", title == null ? "Shortcut" : title);
                documents.put(document);
                pref.edit().putString("andesk_documents", documents.toString()).apply();
                refreshDesktopIcons();
                return;
            }
        } catch(Exception error) { android.util.Log.w("Andesk", "Cannot save document shortcut", error); }
    }

    private boolean isOccupied(JSONArray list, DesktopIconInfo slot) throws org.json.JSONException {
        for(int i = 0; i < list.length(); i++) {
            org.json.JSONObject item = list.getJSONObject(i);
            if(item.optInt("column", -1) == slot.column && item.optInt("row", -1) == slot.row)
                return true;
        }
        return false;
    }

    private void showDocumentShortcutMenu(org.json.JSONObject shortcut) {
        new AlertDialog.Builder(this).setTitle(shortcut.optString("title", "Shortcut"))
                .setItems(new String[] {"Open", "Open with", "Share file", "Details", "Rename shortcut", "Remove shortcut"}, (dialog, selected) -> {
                    if(selected == 0) openDocumentShortcut(shortcut);
                    if(selected == 1) openDocumentWithChooser(shortcut);
                    if(selected == 2) shareDocumentShortcut(shortcut);
                    if(selected == 3) showDocumentDetails(shortcut);
                    if(selected == 4) {
                        android.widget.EditText name = new android.widget.EditText(this);
                        name.setSingleLine(true);
                        name.setText(shortcut.optString("title", "Shortcut"));
                        new AlertDialog.Builder(this).setTitle("Rename shortcut").setView(name)
                                .setPositiveButton("Save", (d, w) -> modifyDocumentShortcut(shortcut, name.getText().toString(), false))
                                .setNegativeButton("Cancel", null).show();
                    }
                    if(selected == 5) modifyDocumentShortcut(shortcut, null, true);
                }).show();
    }

    private void openDocumentWithChooser(org.json.JSONObject shortcut) {
        android.net.Uri uri = android.net.Uri.parse(shortcut.optString("uri"));
        if(shortcut.optBoolean("folder", false)) {
            openDocumentShortcut(shortcut);
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String mime = getContentResolver().getType(uri);
        intent.setDataAndType(uri, mime == null ? "*/*" : mime);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try { startActivity(Intent.createChooser(intent, "Open with")); }
        catch(RuntimeException error) { android.util.Log.w("Andesk", "No file viewer available", error); }
    }

    private void shareDocumentShortcut(org.json.JSONObject shortcut) {
        if(shortcut.optBoolean("folder", false)) return;
        android.net.Uri uri = android.net.Uri.parse(shortcut.optString("uri"));
        Intent intent = new Intent(Intent.ACTION_SEND);
        String mime = getContentResolver().getType(uri);
        intent.setType(mime == null ? "application/octet-stream" : mime);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try { startActivity(Intent.createChooser(intent, "Share file")); }
        catch(RuntimeException error) { android.util.Log.w("Andesk", "Cannot share shortcut", error); }
    }

    private void showDocumentDetails(org.json.JSONObject shortcut) {
        android.net.Uri uri = android.net.Uri.parse(shortcut.optString("uri"));
        String details = "Type: " + (shortcut.optBoolean("folder", false) ? "Folder" : "File")
                + "\nName: " + shortcut.optString("title", "Shortcut")
                + "\nLocation: " + uri;
        new AlertDialog.Builder(this).setTitle("Shortcut details").setMessage(details)
                .setPositiveButton("Close", null).show();
    }

    private void modifyDocumentShortcut(org.json.JSONObject shortcut, String title, boolean remove) {
        try {
            SharedPreferences pref = U.getSharedPreferences(this);
            JSONArray current = new JSONArray(pref.getString("andesk_documents", "[]"));
            JSONArray updated = new JSONArray();
            for(int i = 0; i < current.length(); i++) {
                org.json.JSONObject item = current.getJSONObject(i);
                if(item.optString("uri").equals(shortcut.optString("uri"))
                        && item.optInt("column") == shortcut.optInt("column")
                        && item.optInt("row") == shortcut.optInt("row")) {
                    if(remove) continue;
                    item.put("title", title);
                }
                updated.put(item);
            }
            pref.edit().putString("andesk_documents", updated.toString()).apply();
            refreshDesktopIcons();
        } catch(org.json.JSONException error) {
            android.util.Log.e("Andesk", "Unable to update desktop shortcut", error);
        }
    }

    private void openDocumentShortcut(org.json.JSONObject shortcut) {
        android.net.Uri uri = android.net.Uri.parse(shortcut.optString("uri"));
        boolean folder = shortcut.optBoolean("folder", false);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String mimeType = folder ? android.provider.DocumentsContract.Document.MIME_TYPE_DIR
                : getContentResolver().getType(uri);
        intent.setDataAndType(uri, mimeType == null ? "*/*" : mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch(RuntimeException error) {
            if(folder) {
                // A file manager is optional on Android; use the system picker to browse.
                Intent browse = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                browse.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    browse.putExtra(android.provider.DocumentsContract.EXTRA_INITIAL_URI, uri);
                try { startActivity(browse); return; }
                catch(RuntimeException ignored) { android.util.Log.w("Andesk", "Folder browser unavailable", ignored); }
            }
            android.util.Log.w("Andesk", "Unable to open document shortcut", error);
            android.widget.Toast.makeText(this, "No app can open this shortcut", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    private void setOnHomeScreen(boolean value) {
        LauncherHelper helper = LauncherHelper.getInstance();

        if(isSecondaryHome) {
            int displayID = windowManager.getDefaultDisplay().getDisplayId();
            if(displayID == Display.DEFAULT_DISPLAY) {
                finish();
                return;
            }

            helper.setOnSecondaryHomeScreen(value, displayID);

            SharedPreferences pref = U.getSharedPreferences(this);
            if(pref.getBoolean(PREF_AUTO_HIDE_NAVBAR_DESKTOP_MODE, false))
                U.showHideNavigationBar(this, displayID, !value, 0);
        } else
            helper.setOnPrimaryHomeScreen(value);
    }

    @Override
    public void onTopResumedActivityChanged(boolean isTopResumedActivity) {
        if(isTopResumedActivity)
            performOnResumeLogic();
    }

    private void performOnResumeLogic() {
        if(waitingForPermission) {
            waitingForPermission = false;
            if(!isDesktopLauncher) init();
        }

        overridePendingTransition(0, R.anim.close_anim);
        U.sendBroadcast(this, ACTION_TEMP_SHOW_TASKBAR);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(isDesktopLauncher && requestCode == 42)
            init();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(desktopWidgets != null && desktopWidgets.handleActivityResult(requestCode, resultCode, data))
            return;

        if(resultCode != RESULT_OK)
            return;
        if(requestCode == 9101 || requestCode == 9102) {
            if(data != null && data.getData() != null) saveDocumentShortcut(data.getData(), requestCode == 9102, data.getFlags());
            return;
        }

        if(requestCode == U.IMAGE_REQUEST_CODE) {
            if(data.getData() == null)
                return;

            if(U.importImage(this, data.getData(), "desktop_wallpaper")) {
                U.applyCustomImage(this, "desktop_wallpaper", wallpaper, null);
                getWindow().setNavigationBarColor(Color.BLACK);
            }
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void removeCustomWallpaper() {
        File file = new File(getFilesDir() + "/tb_images", "desktop_wallpaper");
        if(file.exists()) file.delete();
        if(wallpaper != null) wallpaper.setImageDrawable(null);

        getWindow().setNavigationBarColor(0);
    }
}
