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

package com.farmerbb.taskbar.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.provider.Settings;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.IconPackActivity;
import com.farmerbb.taskbar.util.AppearanceDefaults;
import com.farmerbb.taskbar.util.ColorPickerDialog;
import com.farmerbb.taskbar.util.TaskbarStylePickerDialog;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class AppearanceFragment extends SettingsFragment {

    @Override
    protected void addPrefsToSanitize() {
        prefsToSanitize.put(PREF_SYS_TRAY, R.bool.class);
    }

    private enum ColorPickerType { BACKGROUND_TINT, ACCENT_COLOR }

    @Override
    protected void loadPrefs() {
        SharedPreferences pref = U.getSharedPreferences(getActivity());
        if(pref.getString(PREF_START_BUTTON_IMAGE, "null").equals("null"))
            pref.edit().putString(PREF_START_BUTTON_IMAGE, U.getDefaultStartButtonImage(getActivity())).apply();

        // Add preferences
        addPreferencesFromResource(R.xml.tb_pref_appearance);

        String taskbarStyle = pref.getString(PREF_TASKBAR_STYLE, PREF_TASKBAR_STYLE_CLASSIC);
        if(PREF_TASKBAR_STYLE_WINDOWS.equals(taskbarStyle))
            pref.edit().putString(PREF_TASKBAR_STYLE, PREF_TASKBAR_STYLE_WINDOWS_11).apply();
        else if(!pref.contains(PREF_TASKBAR_STYLE))
            pref.edit().putString(PREF_TASKBAR_STYLE, PREF_TASKBAR_STYLE_CLASSIC).apply();

        // Set OnClickListeners for certain preferences
        findPreference(PREF_ICON_PACK_LIST).setOnPreferenceClickListener(this);
        findPreference(PREF_RESET_COLORS).setOnPreferenceClickListener(this);
        findPreference(PREF_BACKGROUND_TINT_PREF).setOnPreferenceClickListener(this);
        findPreference(PREF_ACCENT_COLOR_PREF).setOnPreferenceClickListener(this);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(U.isAndroidTV(getActivity()))
                removePreferenceByKey(PREF_NOTIFICATION_COUNT);
            else
                findPreference(PREF_NOTIFICATION_COUNT).setOnPreferenceClickListener(this);
        } else {
            removePreferenceByKey(PREF_NOTIFICATION_COUNT);
            removePreferenceByKey(PREF_SYS_TRAY);
            removePreferenceByKey(PREF_BATTERY_PERCENT);
            removePreferenceByKey(PREF_CLOCK_FORMAT);
            removePreferenceByKey(PREF_CLOCK_SHOW_DATE);
            removePreferenceByKey(PREF_CLOCK_SHOW_SECONDS);
        }

        if(U.isAndroidGeneric(getActivity())) {
            String[] array = getResources().getStringArray(R.array.tb_pref_start_button_image_list);
            String flavor = U.getSystemProperty("ro.ag.flavor");

            if(flavor == null || flavor.isEmpty())
                flavor = getString(R.string.tb_rom);

            array[1] = getString(R.string.tb_pref_title_app_drawer_icon_ag, flavor);
            ((ListPreference) findPreference(PREF_START_BUTTON_IMAGE)).setEntries(array);
        }

        bindPreferenceSummaryToValue(findPreference(PREF_THEME));
        findPreference(PREF_TASKBAR_STYLE).setOnPreferenceClickListener(this);
        findPreference(PREF_TASKBAR_POSITION_PICKER).setOnPreferenceClickListener(this);
        findPreference(PREF_RESET_APPEARANCE).setOnPreferenceClickListener(this);
        bindPreferenceSummaryToValue(findPreference(PREF_TASKBAR_TEXTURE));
        bindPreferenceSummaryToValue(findPreference(PREF_TASKBAR_TEXTURE_STRENGTH));
        updateStyleSummary();
        updatePositionSummary();
        bindPreferenceSummaryToValue(findPreference(PREF_HIDE_STATUS_BAR));
        bindPreferenceSummaryToValue(findPreference(PREF_TASKBAR_SIZE));
        bindPreferenceSummaryToValue(findPreference(PREF_FULL_LENGTH));
        bindPreferenceSummaryToValue(findPreference(PREF_CENTERED_ICONS));
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            bindPreferenceSummaryToValue(findPreference(PREF_SYS_TRAY));
        bindPreferenceSummaryToValue(findPreference(PREF_INVISIBLE_BUTTON));
        bindPreferenceSummaryToValue(findPreference(PREF_START_BUTTON_IMAGE));
        bindPreferenceSummaryToValue(findPreference(PREF_ICON_PACK_USE_MASK));
        bindPreferenceSummaryToValue(findPreference(PREF_VISUAL_FEEDBACK));
        bindPreferenceSummaryToValue(findPreference(PREF_SHORTCUT_ICON));
        bindPreferenceSummaryToValue(findPreference(PREF_TRANSPARENT_START_MENU));
        bindPreferenceSummaryToValue(findPreference(PREF_HIDE_ICON_LABELS));
        bindPreferenceSummaryToValue(findPreference(PREF_DESKTOP_ICON_SIZE));
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            bindPreferenceSummaryToValue(findPreference(PREF_BATTERY_PERCENT));
            bindPreferenceSummaryToValue(findPreference(PREF_CLOCK_FORMAT));
            bindPreferenceSummaryToValue(findPreference(PREF_CLOCK_SHOW_DATE));
            bindPreferenceSummaryToValue(findPreference(PREF_CLOCK_SHOW_SECONDS));
        }

        updateColorPreviews();
    }

    /** Shows the current color on each color preference, as a dot and as a code. */
    private void updateColorPreviews() {
        setColorPreview(PREF_BACKGROUND_TINT_PREF, U.getBackgroundTint(getActivity()));
        setColorPreview(PREF_ACCENT_COLOR_PREF, U.getAccentColor(getActivity()));
    }

    private void setColorPreview(String key, int color) {
        Preference preference = findPreference(key);
        if(preference == null) return;

        preference.setSummary("#" + String.format("%08X", color));

        float density = getResources().getDisplayMetrics().density;
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(color);
        dot.setStroke(Math.round(1.5f * density), Color.argb(120, 128, 128, 128));
        dot.setSize(Math.round(28 * density), Math.round(28 * density));
        preference.setIcon(dot);
    }

    private void updatePositionSummary() {
        Preference position = findPreference(PREF_TASKBAR_POSITION_PICKER);
        if(position == null) return;

        String[] names = getResources().getStringArray(R.array.tb_pref_position_list);
        String[] values = getResources().getStringArray(R.array.tb_pref_position_list_values);
        String current = U.getSharedPreferences(getActivity()).getString(PREF_POSITION, POSITION_BOTTOM_LEFT);
        for(int i = 0; i < values.length; i++) {
            if(values[i].equals(current)) {
                position.setSummary(names[i]);
                return;
            }
        }
    }

    private void showPositionPicker() {
        String[] names = getResources().getStringArray(R.array.tb_pref_position_list);
        String[] values = getResources().getStringArray(R.array.tb_pref_position_list_values);
        String current = U.getSharedPreferences(getActivity()).getString(PREF_POSITION, POSITION_BOTTOM_LEFT);

        int checked = -1;
        for(int i = 0; i < values.length; i++) {
            if(values[i].equals(current)) checked = i;
        }

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.tb_choose_position)
                .setSingleChoiceItems(names, checked, (dialog, which) -> {
                    U.getSharedPreferences(getActivity()).edit()
                            .putString(PREF_POSITION, values[which])
                            .apply();
                    updatePositionSummary();
                    U.restartTaskbar(getActivity());
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.tb_action_cancel, null)
                .show();
    }

    private void updateStyleSummary() {
        Preference style = findPreference(PREF_TASKBAR_STYLE);
        if(style != null)
            style.setSummary(TaskbarStylePickerDialog.getStyleName(
                    getActivity(), U.getTaskbarStyle(getActivity())));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setTitle(R.string.tb_pref_header_appearance);
        ActionBar actionBar = activity.getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(getActivity() == null) return;

        Preference iconPackListPref = findPreference(PREF_ICON_PACK_LIST);
        if(iconPackListPref != null) {
            SharedPreferences pref = U.getSharedPreferences(getActivity());
            String iconPackPackage = pref.getString(PREF_ICON_PACK, getActivity().getPackageName());
            PackageManager pm = getActivity().getPackageManager();

            boolean iconPackValid = true;
            try {
                pm.getPackageInfo(iconPackPackage, 0);
            } catch (PackageManager.NameNotFoundException e) {
                iconPackValid = false;
            }

            if(!iconPackValid || iconPackPackage.equals(getActivity().getPackageName())) {
                iconPackListPref.setSummary(getString(R.string.tb_icon_pack_none));
            } else {
                try {
                    iconPackListPref.setSummary(pm.getApplicationLabel(pm.getApplicationInfo(iconPackPackage, 0)));
                } catch (PackageManager.NameNotFoundException ignored) {}
            }
        }
    }

    @Override
    public boolean onPreferenceClick(final Preference p) {
        if(getActivity() == null) return false;
        final SharedPreferences pref = U.getSharedPreferences(getActivity());

        switch(p.getKey()) {
            case PREF_ICON_PACK_LIST:
                Intent intent = U.getThemedIntent(getActivity(), IconPackActivity.class);
                startActivityForResult(intent, 123);
                break;
            case PREF_NOTIFICATION_COUNT:
                try {
                    startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                } catch (ActivityNotFoundException e) {
                    U.showToast(getActivity(), R.string.tb_lock_device_not_supported);
                }
                break;
            case PREF_RESET_COLORS:
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.tb_reset_colors)
                        .setMessage(R.string.tb_are_you_sure)
                        .setNegativeButton(R.string.tb_action_cancel, null)
                        .setPositiveButton(R.string.tb_action_ok, (dialog, which) -> {
                            finishedLoadingPrefs = false;

                            pref.edit().remove(PREF_BACKGROUND_TINT).remove(PREF_ACCENT_COLOR).apply();

                            updateColorPreviews();

                            finishedLoadingPrefs = true;
                            U.restartTaskbar(getActivity());
                        });

                AlertDialog dialog = builder.create();
                dialog.show();
                break;
            case PREF_TASKBAR_POSITION_PICKER:
                showPositionPicker();
                break;
            case PREF_RESET_APPEARANCE:
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.tb_pref_title_reset_appearance)
                        .setMessage(R.string.tb_reset_appearance_confirm)
                        .setNegativeButton(R.string.tb_action_cancel, null)
                        .setPositiveButton(R.string.tb_action_ok, (confirm, which) -> {
                            AppearanceDefaults.reset(getActivity());
                            U.sendBroadcast(getActivity(), ACTION_REFRESH_DESKTOP_ICONS);
                            U.restartTaskbar(getActivity());
                            U.showToast(getActivity(), R.string.tb_appearance_reset_done);

                            // Rebuild the screen (and the theme) with the defaults.
                            getActivity().recreate();
                        })
                        .show();
                break;
            case PREF_TASKBAR_STYLE:
                TaskbarStylePickerDialog.show(getActivity(), U.getTaskbarStyle(getActivity()), value -> {
                    // Presets such as the GNOME dock move the taskbar to their natural edge.
                    applyStylePosition(getActivity(), value);
                    pref.edit().putString(PREF_TASKBAR_STYLE, value).apply();
                    updateStyleSummary();
                    U.restartTaskbar(getActivity());
                });
                break;
            case PREF_BACKGROUND_TINT_PREF:
                showColorPicker(ColorPickerType.BACKGROUND_TINT);
                break;
            case PREF_ACCENT_COLOR_PREF:
                showColorPicker(ColorPickerType.ACCENT_COLOR);
                break;
        }

        return super.onPreferenceClick(p);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode != Activity.RESULT_OK)
            return;

        if(requestCode == 123) {
            U.refreshPinnedIcons(getActivity());
            U.restartTaskbar(getActivity());
        }

        if(requestCode == U.IMAGE_REQUEST_CODE) {
            if(data.getData() == null)
                return;

            if(U.importImage(getActivity(), data.getData(), "custom_image"))
                U.restartTaskbar(getActivity());
        }
    }

    @SuppressLint("SetTextI18n")
    private void showColorPicker(ColorPickerType type) {
        int color;
        int dialogTitle;
        String preferenceId;

        switch(type) {
            case ACCENT_COLOR:
                color = U.getAccentColor(getActivity());
                dialogTitle = R.string.tb_pref_title_accent_color;
                preferenceId = PREF_ACCENT_COLOR;
                break;
            case BACKGROUND_TINT:
            default:
                color = U.getBackgroundTint(getActivity());
                dialogTitle = R.string.tb_pref_title_background_tint;
                preferenceId = PREF_BACKGROUND_TINT;
                break;
        }

        ColorPickerDialog.show(getActivity(), dialogTitle, color, picked -> {
            U.getSharedPreferences(getActivity()).edit().putInt(preferenceId, picked).apply();
            updateColorPreviews();
            U.restartTaskbar(getActivity());
        });
    }
}
