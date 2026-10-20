/* Copyright 2016 Braden Farmer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.farmerbb.taskbar.activity;

import android.accessibilityservice.AccessibilityService;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.core.graphics.ColorUtils;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.Constants;
import com.farmerbb.taskbar.util.U;

import java.util.Calendar;
import java.util.Date;

public class SystemTrayActivity extends Activity {
    private LinearLayout root;
    private int foregroundColor;
    private int activeColor;
    private boolean darkPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Match the taskbar: same color and transparency, rounded "flyout" panel.
        int panelColor = U.getTaskbarBackgroundColor(this);
        if(Color.alpha(panelColor) == 255)
            panelColor = ColorUtils.setAlphaComponent(panelColor, 238);
        darkPanel = ColorUtils.calculateLuminance(ColorUtils.setAlphaComponent(panelColor, 255)) < 0.5;
        foregroundColor = darkPanel ? Color.WHITE : Color.rgb(32, 32, 32);
        activeColor = pickActiveColor(U.getAccentColor(this));

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(12));

        GradientDrawable background = new GradientDrawable();
        background.setColor(panelColor);
        background.setCornerRadius(dp(24));
        background.setStroke(dp(1), ColorUtils.setAlphaComponent(foregroundColor, 28));
        root.setBackground(background);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            root.setElevation(dp(8));

        addClockAndDate();

        Boolean wifi = isWifiEnabled();
        Boolean bluetooth = isBluetoothEnabled();

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.addView(makeTile(R.drawable.tb_cc_wifi, getString(R.string.tb_wifi),
                stateLabel(wifi), Boolean.TRUE.equals(wifi), v -> openWifiControls()), tileParams());
        row1.addView(makeTile(R.drawable.tb_cc_bluetooth, getString(R.string.tb_bluetooth),
                stateLabel(bluetooth), Boolean.TRUE.equals(bluetooth), v -> openBluetoothControls()), tileParams());
        row1.addView(makeTile(R.drawable.tb_cc_volume, getString(R.string.tb_volume),
                null, false, v -> openVolumeControls()), tileParams());
        root.addView(row1);

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.addView(makeTile(R.drawable.tb_cc_notifications, getString(R.string.tb_notifications),
                null, false, v -> {
                    U.sendAccessibilityAction(this, AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS);
                    finish();
                }), tileParams());
        row2.addView(makeTile(R.drawable.tb_cc_settings, getString(R.string.tb_settings),
                null, false, v -> {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                }), tileParams());
        row2.addView(makeTile(R.drawable.tb_cc_exit, getString(R.string.tb_exit_desktop),
                null, false, v -> exitDesktop()), tileParams());
        root.addView(row2);

        setContentView(root);
        setFinishOnTouchOutside(true);

        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setGravity(Gravity.BOTTOM | Gravity.END);
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = Math.min(dp(360), getResources().getDisplayMetrics().widthPixels - dp(16));
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.x = dp(8);
        params.y = U.getTaskbarIconSize(this) + dp(8);
        window.setAttributes(params);
    }

    private void addClockAndDate() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(6), 0, dp(6), dp(8));

        TextView time = new TextView(this);
        time.setText(DateFormat.getTimeFormat(this).format(new Date()));
        time.setTextColor(foregroundColor);
        time.setTextSize(34);
        time.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        time.setOnClickListener(v -> openClock());
        header.addView(time, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout dateRow = new LinearLayout(this);
        dateRow.setOrientation(LinearLayout.HORIZONTAL);
        dateRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView date = new TextView(this);
        date.setText(DateFormat.format("EEEE, MMMM d", new Date()));
        date.setTextColor(ColorUtils.setAlphaComponent(foregroundColor, 200));
        date.setTextSize(15);
        date.setOnClickListener(v -> openCalendar(System.currentTimeMillis()));
        dateRow.addView(date, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        // A compact month view expands from the chevron beside the date.
        TextView toggle = new TextView(this);
        toggle.setId(R.id.control_center_calendar_toggle);
        toggle.setText("\u25BE");
        toggle.setContentDescription(getString(R.string.tb_show_month));
        toggle.setTextColor(foregroundColor);
        toggle.setTextSize(18);
        toggle.setGravity(Gravity.CENTER);
        toggle.setBackground(roundedFill(ColorUtils.setAlphaComponent(foregroundColor, 24), dp(16)));
        dateRow.addView(toggle, new LinearLayout.LayoutParams(dp(32), dp(32)));
        header.addView(dateRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        CalendarView calendar = new CalendarView(new ContextThemeWrapper(this, darkPanel
                ? android.R.style.Theme_DeviceDefault
                : android.R.style.Theme_DeviceDefault_Light));
        calendar.setId(R.id.control_center_calendar);
        calendar.setVisibility(View.GONE);
        calendar.setBackground(roundedFill(ColorUtils.setAlphaComponent(foregroundColor, 14), dp(16)));
        calendar.setOnDateChangeListener((view, year, month, day) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, day, 9, 0, 0);
            openCalendar(selected.getTimeInMillis());
        });
        LinearLayout.LayoutParams calendarParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(300));
        calendarParams.setMargins(dp(4), 0, dp(4), dp(8));
        root.addView(calendar, calendarParams);

        toggle.setOnClickListener(v -> {
            boolean show = calendar.getVisibility() != View.VISIBLE;
            calendar.setVisibility(show ? View.VISIBLE : View.GONE);
            toggle.setText(show ? "\u25B4" : "\u25BE");
        });
    }

    private View makeTile(int icon, String label, String state, boolean active,
                          View.OnClickListener listener) {
        int contentColor = active ? contrastingColor(activeColor) : foregroundColor;

        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(6), dp(10), dp(6), dp(10));
        tile.setClickable(true);
        tile.setFocusable(true);
        tile.setOnClickListener(listener);
        tile.setContentDescription(state == null ? label : label + ", " + state);
        tile.setBackground(roundedFill(active
                ? activeColor
                : ColorUtils.setAlphaComponent(foregroundColor, 22), dp(16)));

        ImageView image = new ImageView(this);
        image.setImageResource(icon);
        image.setColorFilter(contentColor);
        tile.addView(image, new LinearLayout.LayoutParams(dp(24), dp(24)));

        TextView text = new TextView(this);
        text.setText(label);
        text.setTextColor(contentColor);
        text.setTextSize(12);
        text.setGravity(Gravity.CENTER);
        text.setMaxLines(1);
        text.setEllipsize(android.text.TextUtils.TruncateAt.END);
        text.setPadding(0, dp(6), 0, 0);
        tile.addView(text, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        if(state != null) {
            TextView stateText = new TextView(this);
            stateText.setText(state);
            stateText.setTextColor(ColorUtils.setAlphaComponent(contentColor, 180));
            stateText.setTextSize(11);
            stateText.setGravity(Gravity.CENTER);
            tile.addView(stateText, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        return tile;
    }

    private LinearLayout.LayoutParams tileParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(84), 1f);
        params.setMargins(dp(4), dp(4), dp(4), dp(4));
        return params;
    }

    private GradientDrawable roundedFill(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    /** Use the user's accent for "on" tiles unless it is too pale to stand out. */
    private static int pickActiveColor(int accent) {
        int opaque = ColorUtils.setAlphaComponent(accent, 255);
        return ColorUtils.calculateLuminance(opaque) > 0.85 ? Color.rgb(61, 126, 255) : opaque;
    }

    private static int contrastingColor(int background) {
        return ColorUtils.calculateLuminance(background) > 0.5 ? Color.rgb(32, 32, 32) : Color.WHITE;
    }

    private String stateLabel(Boolean enabled) {
        return enabled == null ? null : getString(enabled ? R.string.tb_on : R.string.tb_off);
    }

    private Boolean isWifiEnabled() {
        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        return manager == null ? null : manager.isWifiEnabled();
    }

    private Boolean isBluetoothEnabled() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if(adapter == null)
            return null;

        try {
            return adapter.isEnabled();
        } catch(SecurityException ignored) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private void openWifiControls() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startActivitySafely(new Intent(Settings.Panel.ACTION_WIFI));
        } else {
            WifiManager manager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            if(manager != null)
                manager.setWifiEnabled(!manager.isWifiEnabled());
            recreate();
        }
    }

    @SuppressWarnings("deprecation")
    private void openBluetoothControls() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if(adapter == null)
            return;

        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            try {
                if(adapter.isEnabled())
                    adapter.disable();
                else
                    adapter.enable();
                recreate();
                return;
            } catch(SecurityException ignored) {}
        }

        startActivitySafely(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
    }

    @VisibleForTesting
    Intent getExitDesktopIntent() {
        Intent intent = new Intent(Constants.ACTION_QUIT);
        intent.setPackage(getPackageName());
        intent.putExtra(Constants.EXTRA_DESKTOP_SESSION, true);
        return intent;
    }

    private void exitDesktop() {
        sendBroadcast(getExitDesktopIntent());
        finish();
    }

    private void openVolumeControls() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            startActivitySafely(new Intent(Settings.Panel.ACTION_VOLUME));
        else
            startActivitySafely(new Intent(Settings.ACTION_SOUND_SETTINGS));
    }

    private void openClock() {
        if(U.openClockApp(this))
            finish();
        else
            U.showToast(this, R.string.tb_no_clock_app);
    }

    private void openCalendar(long timeMillis) {
        if(U.openCalendarApp(this, timeMillis))
            finish();
        else
            U.showToast(this, R.string.tb_no_calendar_app);
    }

    private void startActivitySafely(Intent intent) {
        try {
            startActivity(intent);
        } catch(ActivityNotFoundException ignored) {}
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
