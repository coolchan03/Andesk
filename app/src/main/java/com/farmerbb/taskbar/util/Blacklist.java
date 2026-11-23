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

public class Blacklist implements Serializable {
    static final long serialVersionUID = 9033765395690141037L;

    private List<BlacklistEntry> blockedApps = new ArrayList<>();

    private static Blacklist theInstance;

    private Blacklist() {}

    /** @return a snapshot, safe to iterate while another thread edits the list */
    public synchronized List<BlacklistEntry> getBlockedApps() {
        return new ArrayList<>(blockedApps);
    }

    public synchronized void addBlockedApp(Context context, BlacklistEntry entry) {
        if(isBlocked(entry.getPackageName())) return;

        blockedApps.add(entry);
        save(context);
    }

    public synchronized void removeBlockedApp(Context context, String packageName) {
        int number = -1;

        for(int i = 0; i < blockedApps.size(); i++) {
            if(blockedApps.get(i).getPackageName().equals(packageName)) {
                number = i;
                break;
            }
        }

        if(number != -1) blockedApps.remove(number);

        save(context);
    }

    public synchronized boolean isBlocked(String packageName) {
        for(int i = 0; i < blockedApps.size(); i++) {
            if(blockedApps.get(i).getPackageName().equals(packageName))
                return true;
        }

        return false;
    }

    private boolean save(Context context) {
        return SerializedStore.save(context, "Blacklist", this);
    }

    public synchronized void clear(Context context) {
        blockedApps.clear();
        save(context);
    }

    public static synchronized Blacklist getInstance(Context context) {
        if(theInstance == null) {
            theInstance = SerializedStore.load(context, "Blacklist", Blacklist.class);
            if(theInstance == null) theInstance = new Blacklist();
        }

        return theInstance;
    }
}