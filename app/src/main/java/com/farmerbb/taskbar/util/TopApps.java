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

public class TopApps implements Serializable {
    static final long serialVersionUID = 8541436853152666400L;

    private List<BlacklistEntry> topApps = new ArrayList<>();

    private static TopApps theInstance;

    private TopApps() {}

    /** @return a snapshot, safe to iterate while another thread edits the list */
    public synchronized List<BlacklistEntry> getTopApps() {
        return new ArrayList<>(topApps);
    }

    public synchronized void addTopApp(Context context, BlacklistEntry entry) {
        if(isTopApp(entry.getPackageName())) return;

        topApps.add(entry);
        save(context);
    }

    public synchronized void removeTopApp(Context context, String packageName) {
        int number = -1;

        for(int i = 0; i < topApps.size(); i++) {
            if(topApps.get(i).getPackageName().equals(packageName)) {
                number = i;
                break;
            }
        }

        if(number != -1) topApps.remove(number);

        save(context);
    }

    public synchronized boolean isTopApp(String packageName) {
        for(int i = 0; i < topApps.size(); i++) {
            if(topApps.get(i).getPackageName().equals(packageName))
                return true;
        }

        return false;
    }

    private boolean save(Context context) {
        return SerializedStore.save(context, "TopApps", this);
    }

    public synchronized void clear(Context context) {
        topApps.clear();
        save(context);
    }

    public static synchronized TopApps getInstance(Context context) {
        if(theInstance == null) {
            theInstance = SerializedStore.load(context, "TopApps", TopApps.class);
            if(theInstance == null) theInstance = new TopApps();
        }

        return theInstance;
    }
}