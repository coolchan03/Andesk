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

import com.farmerbb.taskbar.activity.DesktopLauncherActivity;

import static com.farmerbb.taskbar.util.Constants.*;

public class StartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent == null || !ACTION_START.equals(intent.getAction()))
            return;

        // Every user-facing start path launches the desktop owner first. The
        // desktop Activity itself owns overlay-permission onboarding so Android
        // can return directly to Andesk after the user grants permission.
        Intent desktopIntent = new Intent(context, DesktopLauncherActivity.class);
        desktopIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(desktopIntent);
    }
}
