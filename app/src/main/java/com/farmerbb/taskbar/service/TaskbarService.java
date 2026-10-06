/* Copyright 2019 Braden Farmer
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

package com.farmerbb.taskbar.service;

import android.content.ComponentName;
import android.content.Intent;

import com.farmerbb.taskbar.activity.DesktopLauncherActivity;
import com.farmerbb.taskbar.ui.UIHostService;
import com.farmerbb.taskbar.ui.UIController;
import com.farmerbb.taskbar.ui.TaskbarController;

import static com.farmerbb.taskbar.util.Constants.ACTION_QUIT;
import static com.farmerbb.taskbar.util.Constants.EXTRA_DESKTOP_SESSION;

public class TaskbarService extends UIHostService {
    @Override
    public UIController newController() {
        return new TaskbarController(this);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if(isDesktopLauncherTask(rootIntent)) {
            Intent quitIntent = new Intent(ACTION_QUIT);
            quitIntent.setPackage(getPackageName());
            quitIntent.putExtra(EXTRA_DESKTOP_SESSION, true);
            sendBroadcast(quitIntent);
        }

        super.onTaskRemoved(rootIntent);
    }

    static boolean isDesktopLauncherTask(Intent intent) {
        ComponentName component = intent == null ? null : intent.getComponent();
        return component != null
                && DesktopLauncherActivity.class.getName().equals(component.getClassName());
    }
}