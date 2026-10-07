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

package com.farmerbb.taskbar.ui;

import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;

import com.farmerbb.taskbar.helper.LauncherHelper;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.PREF_DESKTOP_SESSION_ACTIVE;
import static com.farmerbb.taskbar.util.Constants.PREF_TASKBAR_ACTIVE;

public abstract class UIHostService extends Service implements UIHost {

    private UIController controller;
    private WindowManager windowManager;
    private String configString;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(!isDesktopSessionActive()) {
            U.getSharedPreferences(this).edit()
                    .putBoolean(PREF_DESKTOP_SESSION_ACTIVE, false)
                    .putBoolean(PREF_TASKBAR_ACTIVE, false)
                    .apply();
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if(!isDesktopSessionActive()) {
            U.getSharedPreferences(this).edit()
                    .putBoolean(PREF_DESKTOP_SESSION_ACTIVE, false)
                    .putBoolean(PREF_TASKBAR_ACTIVE, false)
                    .apply();
            stopSelf();
            return;
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        configString = U.getConfigString(this);

        controller = newController();
        controller.onCreateHost(this);
    }

    private boolean isDesktopSessionActive() {
        return LauncherHelper.getInstance().isDesktopLauncherOpen()
                && U.getSharedPreferences(this).getBoolean(PREF_DESKTOP_SESSION_ACTIVE, false);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if(controller == null)
            return;

        String newConfigString = U.getConfigString(this);
        if(newConfigString.equals(configString)) return;

        configString = newConfigString;
        controller.onRecreateHost(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(controller != null)
            controller.onDestroyHost(this);
    }

    @Override
    public void addView(View view, ViewParams params) {
        if(windowManager != null)
            windowManager.addView(view, params.toWindowManagerParams());
    }

    @Override
    public void removeView(View view) {
        if(windowManager != null)
            windowManager.removeView(view);
    }

    @Override
    public void terminate() {
        stopSelf();
    }

    @Override
    public void updateViewLayout(View view, ViewParams params) {
        if(windowManager != null)
            windowManager.updateViewLayout(view, params.toWindowManagerParams());
    }

    public abstract UIController newController();
}
