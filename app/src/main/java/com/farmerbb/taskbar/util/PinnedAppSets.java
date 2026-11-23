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

import android.content.ComponentName;
import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Named arrangements of pinned apps (for example "Work" and "Media") that can be switched
 * between from the pinned apps editor. Only each app's component is stored; labels and icons are
 * looked up again when a set is applied, so a set follows app updates and skips apps that have
 * since been uninstalled.
 */
public final class PinnedAppSets {
    public static final int MAX_SETS = 12;
    public static final int MAX_NAME_LENGTH = 30;

    private static final String PREF_SETS = "andesk_pinned_app_sets";

    /** What happened when a set was applied. */
    public static final class Result {
        public final int pinned;
        public final int skipped;

        Result(int pinned, int skipped) {
            this.pinned = pinned;
            this.skipped = skipped;
        }
    }

    private PinnedAppSets() {}

    public static String normalizeName(String name) {
        if(name == null) return null;

        String cleaned = name.replaceAll("\\s+", " ").trim();
        if(cleaned.length() > MAX_NAME_LENGTH)
            cleaned = cleaned.substring(0, MAX_NAME_LENGTH).trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

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

    /** The apps in a set, in pinned order; empty if the set does not exist. */
    public static List<ComponentName> components(Context context, String name) {
        List<ComponentName> components = new ArrayList<>();
        JSONObject all = load(context);
        String key = find(all, name);
        JSONArray array = key == null ? null : all.optJSONArray(key);
        if(array == null) return components;

        for(int i = 0; i < array.length(); i++) {
            ComponentName component = ComponentName.unflattenFromString(array.optString(i, ""));
            if(component != null) components.add(component);
        }
        return components;
    }

    /**
     * Stores the currently pinned apps under a name, replacing a set of the same name.
     *
     * @return the stored name, or null if the name is unusable or the limit is reached
     */
    public static String saveCurrent(Context context, String name) {
        String cleaned = normalizeName(name);
        if(cleaned == null) return null;

        JSONObject all = load(context);
        String existing = find(all, cleaned);
        if(existing == null && all.length() >= MAX_SETS) return null;

        JSONArray array = new JSONArray();
        for(AppEntry entry : PinnedBlockedApps.getInstance(context).getPinnedApps())
            array.put(entry.getComponentName());

        try {
            if(existing != null) all.remove(existing);
            all.put(cleaned, array);
        } catch(JSONException e) {
            return null;
        }

        store(context, all);
        return cleaned;
    }

    /**
     * Replaces the pinned apps with those of a set. Apps that can no longer be found are
     * skipped, and the taskbar's size limit is respected. If the set does not exist the pinned
     * apps are left alone.
     *
     * @return null if the set does not exist
     */
    public static Result apply(Context context, String name) {
        if(!exists(context, name)) return null;

        List<ComponentName> components = components(context, name);

        PinnedBlockedApps pinned = PinnedBlockedApps.getInstance(context);
        for(AppEntry entry : pinned.getPinnedApps())
            pinned.removePinnedApp(context, entry.getComponentName());

        int applied = 0;
        for(ComponentName component : components) {
            if(U.pinApp(context, component)) applied++;
        }
        return new Result(applied, components.size() - applied);
    }

    public static boolean delete(Context context, String name) {
        JSONObject all = load(context);
        String key = find(all, name);
        if(key == null) return false;

        all.remove(key);
        store(context, all);
        return true;
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
            return new JSONObject(U.getSharedPreferences(context).getString(PREF_SETS, "{}"));
        } catch(JSONException e) {
            return new JSONObject();
        }
    }

    private static void store(Context context, JSONObject all) {
        U.getSharedPreferences(context).edit().putString(PREF_SETS, all.toString()).apply();
    }
}
