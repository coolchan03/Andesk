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

package com.farmerbb.taskbar.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.farmerbb.taskbar.activity.DummyActivity;
import com.farmerbb.taskbar.service.NotificationService;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class StartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent == null || !ACTION_START.equals(intent.getAction()))
            return;

        if(!U.canDrawOverlays(context)) {
            U.newHandler().postDelayed(() -> {
                Intent permissionIntent = new Intent(context, DummyActivity.class);
                permissionIntent.putExtra(EXTRA_SHOW_PERMISSION_DIALOG, true);
                permissionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(permissionIntent);
            }, 250);
            return;
        }

        // This fork is session-scoped: every user-facing "start Taskbar"
        // entry point launches the desktop owner activity instead of creating
        // a background-only persistent taskbar.
        Intent desktopIntent = new Intent(context, com.farmerbb.taskbar.activity.DesktopLauncherActivity.class);
        desktopIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(desktopIntent);
    }
}
