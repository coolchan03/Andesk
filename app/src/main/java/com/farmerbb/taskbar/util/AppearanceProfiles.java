/* Copyright 2026 Andesk contributors
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

package com.farmerbb.taskbar.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Named snapshots of the taskbar's look (style, colors, size, position, clock and so on) that
 * can be saved and re-applied later. Profiles live in the app's preferences, so they travel with
 * settings backups.
 *
 * <p>Applying a profile first returns every appearance setting to its default and then writes the
 * profile's values. A setting that was at its default when the profile was saved therefore
 * stays at its default, even if it has been changed since.
 */
public final class AppearanceProfiles {
    public static final int MAX_PROFILES = 20;
    public static final int MAX_NAME_LENGTH = 40;

    private static final String TAG = "Andesk";
    private static final String PREF_PROFILES = "andesk_appearance_profiles";

    private static final String TYPE = "t";
    private static final String VALUE = "v";

    private AppearanceProfiles() {}

    /** Trims and bounds a user-typed name; returns null if nothing usable is left. */
    public static String normalizeName(String name) {
        if(name == null) return null;

        String cleaned = name.replaceAll("\\s+", " ").trim();
        if(cleaned.length() > MAX_NAME_LENGTH)
            cleaned = cleaned.substring(0, MAX_NAME_LENGTH).trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    /** Profile names, sorted alphabetically without regard to case. */
    public static List<String> names(Context context) {
        List<String> names = new ArrayList<>();
        Iterator<String> keys = load(context).keys();
        while(keys.hasNext()) names.add(keys.next());

        Collections.sort(names, String::compareToIgnoreCase);
        return names;
    }

    public static boolean exists(Context context, String name) {
        return find(load(context), name) != null;
    }

    /** @return how many settings the profile stores, or -1 if it does not exist */
    public static int settingCount(Context context, String name) {
        JSONObject all = load(context);
        String key = find(all, name);
        JSONObject profile = key == null ? null : all.optJSONObject(key);
        return profile == null ? -1 : profile.length();
    }

    /**
     * Stores the current look under the given name, replacing any profile with the same name
     * (compared without regard to case).
     *
     * @return the stored name, or null if the name is unusable, the limit is reached, or
     *         storing failed
     */
    public static String save(Context context, String name) {
        String cleaned = normalizeName(name);
        if(cleaned == null) return null;

        JSONObject all = load(context);
        String existing = find(all, cleaned);
        if(existing == null && all.length() >= MAX_PROFILES) return null;

        try {
            if(existing != null) all.remove(existing);
            all.put(cleaned, snapshot(U.getSharedPreferences(context)));
        } catch(JSONException e) {
            Log.w(TAG, "Unable to save appearance profile", e);
            return null;
        }

        store(context, all);
        return cleaned;
    }

    /** @return true if the profile existed and was applied */
    public static boolean apply(Context context, String name) {
        JSONObject all = load(context);
        String key = find(all, name);
        JSONObject profile = key == null ? null : all.optJSONObject(key);
        if(profile == null) return false;

        SharedPreferences.Editor editor = U.getSharedPreferences(context).edit();
        for(String setting : AppearanceDefaults.lookKeys())
            editor.remove(setting);

        Iterator<String> settings = profile.keys();
        while(settings.hasNext()) {
            String setting = settings.next();
            // Only restore keys that still belong to the look, so a hand-edited or newer
            // backup cannot write arbitrary preferences.
            if(!isLookKey(setting)) continue;

            JSONObject entry = profile.optJSONObject(setting);
            if(entry != null) put(editor, setting, entry);
        }

        editor.apply();
        return true;
    }

    public static boolean delete(Context context, String name) {
        JSONObject all = load(context);
        String key = find(all, name);
        if(key == null) return false;

        all.remove(key);
        store(context, all);
        return true;
    }

    /** What a profile would be saved as: the current value of each stored look setting. */
    static JSONObject snapshot(SharedPreferences pref) throws JSONException {
        JSONObject profile = new JSONObject();
        Map<String, ?> current = pref.getAll();

        for(String key : AppearanceDefaults.lookKeys()) {
            Object value = current.get(key);
            if(value == null) continue; // at its default

            JSONObject entry = new JSONObject();
            if(value instanceof Boolean) {
                entry.put(TYPE, "b").put(VALUE, value);
            } else if(value instanceof Integer) {
                entry.put(TYPE, "i").put(VALUE, value);
            } else if(value instanceof Long) {
                entry.put(TYPE, "l").put(VALUE, value);
            } else if(value instanceof Float) {
                entry.put(TYPE, "f").put(VALUE, (double) (Float) value);
            } else if(value instanceof String) {
                entry.put(TYPE, "s").put(VALUE, value);
            } else if(value instanceof Set) {
                JSONArray array = new JSONArray();
                for(Object item : new TreeSet<>((Set<?>) value)) array.put(String.valueOf(item));
                entry.put(TYPE, "set").put(VALUE, array);
            } else {
                continue;
            }
            profile.put(key, entry);
        }
        return profile;
    }

    private static void put(SharedPreferences.Editor editor, String key, JSONObject entry) {
        try {
            switch(entry.getString(TYPE)) {
                case "b":
                    editor.putBoolean(key, entry.getBoolean(VALUE));
                    break;
                case "i":
                    editor.putInt(key, entry.getInt(VALUE));
                    break;
                case "l":
                    editor.putLong(key, entry.getLong(VALUE));
                    break;
                case "f":
                    editor.putFloat(key, (float) entry.getDouble(VALUE));
                    break;
                case "s":
                    editor.putString(key, entry.getString(VALUE));
                    break;
                case "set":
                    JSONArray array = entry.getJSONArray(VALUE);
                    Set<String> values = new TreeSet<>();
                    for(int i = 0; i < array.length(); i++) values.add(array.getString(i));
                    editor.putStringSet(key, values);
                    break;
                default:
                    break;
            }
        } catch(JSONException e) {
            Log.w(TAG, "Skipping unreadable setting " + key + " in appearance profile", e);
        }
    }

    private static boolean isLookKey(String key) {
        for(String look : AppearanceDefaults.lookKeys()) {
            if(look.equals(key)) return true;
        }
        return false;
    }

    private static String find(JSONObject all, String name) {
        String wanted = normalizeName(name);
        if(wanted == null) return null;

        Iterator<String> keys = all.keys();
        while(keys.hasNext()) {
            String key = keys.next();
            if(key.toLowerCase(Locale.ROOT).equals(wanted.toLowerCase(Locale.ROOT))) return key;
        }
        return null;
    }

    private static JSONObject load(Context context) {
        try {
            return new JSONObject(U.getSharedPreferences(context).getString(PREF_PROFILES, "{}"));
        } catch(JSONException e) {
            return new JSONObject();
        }
    }

    private static void store(Context context, JSONObject all) {
        U.getSharedPreferences(context).edit().putString(PREF_PROFILES, all.toString()).apply();
    }
}
