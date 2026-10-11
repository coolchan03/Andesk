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

import android.os.Bundle;
import android.preference.Preference;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.MainActivity;
import com.farmerbb.taskbar.util.U;

import java.util.Calendar;
import java.util.TimeZone;

import static com.farmerbb.taskbar.util.Constants.*;

public class AboutFragment extends SettingsFragment {

    @Override
    protected void loadPrefs() {
        // Add preferences
        addPreferencesFromResource(R.xml.tb_pref_base);
        addPreferencesFromResource(R.xml.tb_pref_about);

        boolean isLibrary = U.isLibrary(getActivity());
        if(!isLibrary)
            getPreferenceScreen().removePreference(findPreference("donate_category"));

        // Set OnClickListeners for certain preferences
        if(U.canEnableFreeform(getActivity()))
            findPreference(PREF_PREF_SCREEN_FREEFORM).setOnPreferenceClickListener(this);
        else
            getPreferenceScreen().removePreference(findPreference(PREF_PREF_SCREEN_FREEFORM));

        // The original secondary-home desktop mode is intentionally disabled
        // in this fork. DesktopLauncherActivity is the only desktop-session owner.
        getPreferenceScreen().removePreference(findPreference(PREF_PREF_SCREEN_DESKTOP_MODE));

        findPreference(PREF_PREF_SCREEN_GENERAL).setOnPreferenceClickListener(this);
        findPreference(PREF_PREF_SCREEN_APPEARANCE).setOnPreferenceClickListener(this);
        findPreference(PREF_PREF_SCREEN_RECENT_APPS).setOnPreferenceClickListener(this);
        findPreference(PREF_PREF_SCREEN_ADVANCED).setOnPreferenceClickListener(this);

        if(!isLibrary) {
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("America/Denver"));
            calendar.setTimeInMillis(BuildConfig.TIMESTAMP);

            int year = calendar.get(Calendar.YEAR);
            if(U.isConsumerBuild(getActivity())) {
                String emoji = new String(Character.toChars(0x1F601));

                findPreference(PREF_ABOUT).setSummary(getString(R.string.tb_pref_about_description, year, emoji));
                findPreference(PREF_ABOUT).setOnPreferenceClickListener(this);
            } else
                findPreference(PREF_ABOUT).setSummary(getString(R.string.tb_pref_about_description_alt, year));
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setTitle(((MainActivity) getActivity()).getAboutFragmentTitle());
        ActionBar actionBar = activity.getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(((MainActivity) getActivity()).getAboutFragmentBackArrow());
    }

    @Override
    public boolean onPreferenceClick(final Preference p) {
        switch(p.getKey()) {
            case PREF_ABOUT:
                U.checkForUpdates(getActivity());
                break;
            case PREF_PREF_SCREEN_GENERAL:
                navigateTo(new GeneralFragment());
                break;
            case PREF_PREF_SCREEN_APPEARANCE:
                navigateTo(new AppearanceFragment());
                break;
            case PREF_PREF_SCREEN_RECENT_APPS:
                navigateTo(new RecentAppsFragment());
                break;
            case PREF_PREF_SCREEN_FREEFORM:
                navigateTo(new FreeformModeFragment());
                break;
            case PREF_PREF_SCREEN_ADVANCED:
                navigateTo(new AdvancedFragment());
                break;
        }

        return super.onPreferenceClick(p);
    }

}