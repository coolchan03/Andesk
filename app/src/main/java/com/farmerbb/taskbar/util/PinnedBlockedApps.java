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

package com.farmerbb.taskbar.util;

import android.content.Context;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PinnedBlockedApps implements Serializable {
    static final long serialVersionUID = 6649239269100390617L;

    private List<AppEntry> pinnedApps = new ArrayList<>();
    private List<AppEntry> blockedApps = new ArrayList<>();

    private static PinnedBlockedApps theInstance;

    private PinnedBlockedApps() {}

    /** @return a snapshot, safe to iterate while another thread edits the list */
    public synchronized List<AppEntry> getPinnedApps() {
        return new ArrayList<>(pinnedApps);
    }

    /** @return a snapshot, safe to iterate while another thread edits the list */
    public synchronized List<AppEntry> getBlockedApps() {
        return new ArrayList<>(blockedApps);
    }

    public synchronized void addPinnedApp(Context context, AppEntry entry) {
        if(isPinned(entry.getComponentName())) return;

        pinnedApps.add(entry);
        save(context);
    }

    /**
     * Moves a pinned app by the given number of places in the pinned order.
     *
     * @return true if the order changed
     */
    public synchronized boolean movePinnedApp(Context context, String componentName, int delta) {
        int from = -1;
        for(int i = 0; i < pinnedApps.size(); i++) {
            if(pinnedApps.get(i).getComponentName().equals(componentName)) {
                from = i;
                break;
            }
        }

        int to = from + delta;
        if(from == -1 || to < 0 || to >= pinnedApps.size() || to == from)
            return false;

        pinnedApps.add(to, pinnedApps.remove(from));
        save(context);
        return true;
    }

    public synchronized void addBlockedApp(Context context, AppEntry entry) {
        if(isBlocked(entry.getComponentName())) return;

        blockedApps.add(entry);
        save(context);
    }

    public synchronized void removePinnedApp(Context context, String componentName) {
        int number = -1;

        for(int i = 0; i < pinnedApps.size(); i++) {
            if(pinnedApps.get(i).getComponentName().equals(componentName)) {
                number = i;
                break;
            }
        }

        if(number != -1) pinnedApps.remove(number);

        save(context);
    }

    public synchronized void removeBlockedApp(Context context, String componentName) {
        int number = -1;

        for(int i = 0; i < blockedApps.size(); i++) {
            if(blockedApps.get(i).getComponentName().equals(componentName)) {
                number = i;
                break;
            }
        }

        if(number != -1) blockedApps.remove(number);

        save(context);
    }

    public synchronized boolean isPinned(String componentName) {
        for(int i = 0; i < pinnedApps.size(); i++) {
            if(pinnedApps.get(i).getComponentName().equals(componentName))
                return true;
        }

        return false;
    }

    public synchronized boolean isBlocked(String componentName) {
        for(int i = 0; i < blockedApps.size(); i++) {
            if(blockedApps.get(i).getComponentName().equals(componentName))
                return true;
        }

        return false;
    }

    public synchronized void clear(Context context) {
        pinnedApps.clear();
        blockedApps.clear();
        save(context);
    }

    private boolean save(Context context) {
        return SerializedStore.save(context, "PinnedBlockedApps", this);
    }

    public static synchronized PinnedBlockedApps getInstance(Context context) {
        if(theInstance == null) {
            theInstance = SerializedStore.load(context, "PinnedBlockedApps", PinnedBlockedApps.class);
            if(theInstance == null) theInstance = new PinnedBlockedApps();
        }

        return theInstance;
    }
}
