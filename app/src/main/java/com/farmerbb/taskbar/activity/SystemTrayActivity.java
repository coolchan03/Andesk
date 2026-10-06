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
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.U;

import java.util.Date;

public class SystemTrayActivity extends Activity {
    private LinearLayout root;
    private int accentColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        accentColor = U.getTaskbarAccentColor(this);
        int backgroundColor = U.getTaskbarBackgroundColor(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(14), dp(16), dp(14));

        GradientDrawable background = new GradientDrawable();
        background.setColor(backgroundColor);
        background.setCornerRadius(dp(18));
        root.setBackground(background);

        addClockAndDate();

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.addView(makeTile(getWifiLabel(), v -> openWifiControls()), tileParams());
        row1.addView(makeTile(getBluetoothLabel(), v -> openBluetoothControls()), tileParams());
        root.addView(row1);

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.addView(makeTile(getString(R.string.tb_volume), v -> openVolumeControls()), tileParams());
        row2.addView(makeTile(getString(R.string.tb_notifications), v -> {
            U.sendAccessibilityAction(this, AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS);
            finish();
        }), tileParams());
        root.addView(row2);

        TextView settings = makeTile(getString(R.string.tb_settings), v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
        LinearLayout.LayoutParams settingsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        settingsParams.topMargin = dp(8);
        root.addView(settings, settingsParams);

        setContentView(root);
        setFinishOnTouchOutside(true);

        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setGravity(Gravity.BOTTOM | Gravity.END);
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = dp(320);
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.x = dp(8);
        params.y = U.getTaskbarIconSize(this) + dp(8);
        window.setAttributes(params);
    }

    private void addClockAndDate() {
        TextView time = new TextView(this);
        time.setText(DateFormat.getTimeFormat(this).format(new Date()));
        time.setTextColor(accentColor);
        time.setTextSize(26);
        time.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        time.setPadding(dp(4), 0, dp(4), 0);
        time.setOnClickListener(v -> openClock());
        root.addView(time, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView date = new TextView(this);
        date.setText(DateFormat.getDateFormat(this).format(new Date()));
        date.setTextColor(accentColor);
        date.setTextSize(15);
        date.setPadding(dp(4), 0, dp(4), dp(10));
        date.setOnClickListener(v -> openCalendar());
        root.addView(date, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private TextView makeTile(String text, android.view.View.OnClickListener listener) {
        TextView tile = new TextView(this);
        tile.setText(text);
        tile.setTextColor(accentColor);
        tile.setTextSize(15);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(10), dp(10), dp(10), dp(10));
        tile.setOnClickListener(listener);

        GradientDrawable background = new GradientDrawable();
        background.setColor(ColorUtils.setAlphaComponent(accentColor, 28));
        background.setCornerRadius(dp(14));
        background.setStroke(dp(1), ColorUtils.setAlphaComponent(accentColor, 80));
        tile.setBackground(background);

        return tile;
    }

    private LinearLayout.LayoutParams tileParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(72), 1f);
        params.setMargins(dp(4), dp(4), dp(4), dp(4));
        return params;
    }

    private String getWifiLabel() {
        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        boolean enabled = manager != null && manager.isWifiEnabled();
        return getString(R.string.tb_wifi) + "\n" + getString(enabled ? R.string.tb_on : R.string.tb_off);
    }

    private String getBluetoothLabel() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if(adapter == null)
            return getString(R.string.tb_bluetooth);

        try {
            return getString(R.string.tb_bluetooth) + "\n"
                    + getString(adapter.isEnabled() ? R.string.tb_on : R.string.tb_off);
        } catch(SecurityException ignored) {
            return getString(R.string.tb_bluetooth);
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

    private void openVolumeControls() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            startActivitySafely(new Intent(Settings.Panel.ACTION_VOLUME));
        else
            startActivitySafely(new Intent(Settings.ACTION_SOUND_SETTINGS));
    }

    private void openClock() {
        startActivitySafely(new Intent(AlarmClock.ACTION_SHOW_ALARMS));
    }

    private void openCalendar() {
        Uri uri = CalendarContract.CONTENT_URI.buildUpon()
                .appendPath("time")
                .appendPath(Long.toString(System.currentTimeMillis()))
                .build();
        startActivitySafely(new Intent(Intent.ACTION_VIEW, uri));
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
