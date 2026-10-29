/* Copyright 2026 Andesk contributors
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

package com.farmerbb.taskbar.util;

import android.content.Context;
import android.content.SharedPreferences;

import static com.farmerbb.taskbar.util.Constants.*;

/** Puts every appearance setting back to its default. */
public final class AppearanceDefaults {
    private static final String[] APPEARANCE_KEYS = {
            PREF_THEME,
            PREF_TASKBAR_STYLE,
            PREF_TASKBAR_SIZE,
            PREF_POSITION,
            PREF_FULL_LENGTH,
            PREF_CENTERED_ICONS,
            PREF_TASKBAR_TEXTURE,
            PREF_TASKBAR_TEXTURE_STRENGTH,
            PREF_BACKGROUND_TINT,
            PREF_ACCENT_COLOR,
            PREF_SYS_TRAY,
            PREF_SYS_TRAY + "_is_modified",
            PREF_BATTERY_PERCENT,
            PREF_CLOCK_FORMAT,
            PREF_CLOCK_SHOW_DATE,
            PREF_CLOCK_SHOW_SECONDS,
            PREF_START_BUTTON_IMAGE,
            PREF_ICON_PACK_USE_MASK,
            PREF_SHORTCUT_ICON,
            PREF_HIDE_ICON_LABELS,
            PREF_DESKTOP_ICON_SIZE,
            PREF_HIDE_STATUS_BAR,
            PREF_INVISIBLE_BUTTON,
            PREF_VISUAL_FEEDBACK,
            PREF_TRANSPARENT_START_MENU,
            "andesk_icon_overrides"
    };

    private AppearanceDefaults() {}

    /** Removes the saved value of every appearance setting so each falls back to its default. */
    public static void reset(Context context) {
        SharedPreferences.Editor editor = U.getSharedPreferences(context).edit();
        for(String key : APPEARANCE_KEYS)
            editor.remove(key);
        editor.apply();
    }
}
