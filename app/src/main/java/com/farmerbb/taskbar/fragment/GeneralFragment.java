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

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.provider.Settings;
import android.widget.EditText;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.SelectAppActivity;
import com.farmerbb.taskbar.util.AppChooserDialog;
import com.farmerbb.taskbar.util.Blacklist;
import com.farmerbb.taskbar.util.PinnedAppsEditorDialog;
import com.farmerbb.taskbar.util.TopApps;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class GeneralFragment extends SettingsFragment {

    @Override
    protected void loadPrefs() {
        // Add preferences
        addPreferencesFromResource(R.xml.tb_pref_general);

        // Set OnClickListeners for certain preferences
        findPreference(PREF_BLACKLIST).setOnPreferenceClickListener(this);

        Preference startOnBoot = findPreference(PREF_START_ON_BOOT);
        if(startOnBoot != null)
            getPreferenceScreen().removePreference(startOnBoot);
        U.getSharedPreferences(getActivity()).edit().putBoolean(PREF_START_ON_BOOT, false).apply();

        if(U.isAndroidTV(getActivity()))
            getPreferenceScreen().removePreference(findPreference(PREF_NOTIFICATION_SETTINGS));
        else
            findPreference(PREF_NOTIFICATION_SETTINGS).setOnPreferenceClickListener(this);

        if(U.canEnableFreeform(getActivity())
                && !U.isChromeOs(getActivity())
                && !U.isOverridingFreeformHack(getActivity(), false))
            findPreference(PREF_HIDE_TASKBAR).setSummary(R.string.tb_hide_taskbar_disclaimer);

        findPreference(PREF_PINNED_APPS).setOnPreferenceClickListener(this);
        findPreference(PREF_KEYBOARD_SHORTCUTS).setOnPreferenceClickListener(this);
        findPreference(PREF_CLOCK_APP).setOnPreferenceClickListener(this);
        findPreference(PREF_CALENDAR_APP).setOnPreferenceClickListener(this);
        findPreference(PREF_SEARCH_APP).setOnPreferenceClickListener(this);
        findPreference(PREF_SEARCH_URL).setOnPreferenceClickListener(this);
        bindPreferenceSummaryToValue(findPreference(PREF_SEARCH_ENGINE));

        bindPreferenceSummaryToValue(findPreference(PREF_START_MENU_LAYOUT));
        bindPreferenceSummaryToValue(findPreference(PREF_SCROLLBAR));
        bindPreferenceSummaryToValue(findPreference(PREF_POSITION));
        bindPreferenceSummaryToValue(findPreference(PREF_ANCHOR));
        bindPreferenceSummaryToValue(findPreference(PREF_ALT_BUTTON_CONFIG));
        bindPreferenceSummaryToValue(findPreference(PREF_SHOW_SEARCH_BAR));
        bindPreferenceSummaryToValue(findPreference(PREF_HIDE_WHEN_KEYBOARD_SHOWN));

        if(U.isChromeOs(getActivity()) && U.getCurrentApiVersion() < 30.0f)
            bindPreferenceSummaryToValue(findPreference(PREF_CHROME_OS_CONTEXT_MENU_FIX));
        else
            getPreferenceScreen().removePreference(findPreference(PREF_CHROME_OS_CONTEXT_MENU_FIX));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setTitle(R.string.tb_pref_header_general);
        ActionBar actionBar = activity.getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(getActivity() == null) return;

        int size = Blacklist.getInstance(getActivity()).getBlockedApps().size();
        String summary = size == 1 ? getString(R.string.tb_app_hidden) : getString(R.string.tb_apps_hidden, size);

        size = TopApps.getInstance(getActivity()).getTopApps().size();
        summary = summary + "\n" + (size == 1 ? getString(R.string.tb_top_app) : getString(R.string.tb_top_apps, size));

        Preference blacklistPref = findPreference(PREF_BLACKLIST);
        if(blacklistPref != null) {
            blacklistPref.setSummary(summary);
        }

        updateAppSummaries();
    }

    private void updateAppSummaries() {
        setAppSummary(PREF_CLOCK_APP);
        setAppSummary(PREF_CALENDAR_APP);

        Preference searchApp = findPreference(PREF_SEARCH_APP);
        ComponentName chosen = U.getChosenApp(getActivity(), PREF_SEARCH_APP);
        String label = AppChooserDialog.getLabel(getActivity(), chosen);
        if(searchApp != null)
            searchApp.setSummary(label != null ? label : getString(R.string.tb_pref_description_search_app));

        Preference searchUrl = findPreference(PREF_SEARCH_URL);
        if(searchUrl != null) {
            String url = U.getSharedPreferences(getActivity()).getString(PREF_SEARCH_URL, "");
            searchUrl.setSummary(url == null || url.isEmpty() ? getString(R.string.tb_search_url_hint) : url);
        }
    }

    private void setAppSummary(String key) {
        Preference preference = findPreference(key);
        if(preference == null) return;

        String label = AppChooserDialog.getLabel(getActivity(), U.getChosenApp(getActivity(), key));
        preference.setSummary(label != null ? label : getString(R.string.tb_app_automatic));
    }

    private void chooseApp(String key, int titleRes) {
        AppChooserDialog.show(getActivity(), titleRes, component -> {
            U.getSharedPreferences(getActivity()).edit()
                    .putString(key, component == null ? "" : component.flattenToString())
                    .apply();
            updateAppSummaries();
        });
    }

    private void showKeyboardShortcuts() {
        String shortcuts = String.join("\n\n",
                "Windows key: open or close the Start menu",
                "Esc: close the Start menu",
                "Alt+F4: exit the desktop",
                "Ctrl+Alt+S: Andesk settings",
                "Ctrl+Alt+A or Shift+F10: desktop menu (add apps, files, widgets)",
                "Ctrl+Shift+F: add a file shortcut",
                "Ctrl+Shift+N: add a folder shortcut",
                "Ctrl-, Shift- or middle-click an app: open it in a new window",
                "Ctrl+Enter or Shift+Enter in Start menu search: search the web",
                "Right-click: the same as a long press");

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.tb_keyboard_shortcuts)
                .setMessage(shortcuts)
                .setPositiveButton(R.string.tb_action_ok, null)
                .show();
    }

    private void editSearchUrl() {
        EditText input = new EditText(getActivity());
        input.setSingleLine(true);
        input.setHint(R.string.tb_search_url_hint);
        input.setText(U.getSharedPreferences(getActivity()).getString(PREF_SEARCH_URL, ""));

        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.tb_pref_title_search_url)
                .setView(input)
                .setPositiveButton(R.string.tb_action_ok, null)
                .setNegativeButton(R.string.tb_action_cancel, null)
                .create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String url = input.getText().toString().trim();
            if(!url.contains("%s")) {
                U.showToastLong(getActivity(), R.string.tb_search_url_invalid);
                return;
            }

            U.getSharedPreferences(getActivity()).edit().putString(PREF_SEARCH_URL, url).apply();
            updateAppSummaries();
            dialog.dismiss();
        });
    }

    @TargetApi(Build.VERSION_CODES.O)
    @Override
    public boolean onPreferenceClick(final Preference p) {
        if(getActivity() == null) return false;
        switch(p.getKey()) {
            case PREF_BLACKLIST:
                Intent intent = U.getThemedIntent(getActivity(), SelectAppActivity.class);
                startActivity(intent);
                break;
            case PREF_PINNED_APPS:
                PinnedAppsEditorDialog.show(getActivity());
                break;
            case PREF_KEYBOARD_SHORTCUTS:
                showKeyboardShortcuts();
                break;
            case PREF_CLOCK_APP:
                chooseApp(PREF_CLOCK_APP, R.string.tb_pref_title_clock_app);
                break;
            case PREF_CALENDAR_APP:
                chooseApp(PREF_CALENDAR_APP, R.string.tb_pref_title_calendar_app);
                break;
            case PREF_SEARCH_APP:
                chooseApp(PREF_SEARCH_APP, R.string.tb_pref_title_search_app);
                break;
            case PREF_SEARCH_URL:
                editSearchUrl();
                break;
            case PREF_NOTIFICATION_SETTINGS:
                Intent intent2 = new Intent();
                intent2.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);

                if(Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1)
                    intent2.putExtra(Settings.EXTRA_APP_PACKAGE, getActivity().getPackageName());
                else {
                    intent2.putExtra("app_package", getActivity().getPackageName());
                    intent2.putExtra("app_uid", getActivity().getApplicationInfo().uid);
                }

                try {
                    startActivity(intent2);
                    restartNotificationService = true;
                } catch (ActivityNotFoundException ignored) {}
                break;
        }

        return super.onPreferenceClick(p);
    }
}
