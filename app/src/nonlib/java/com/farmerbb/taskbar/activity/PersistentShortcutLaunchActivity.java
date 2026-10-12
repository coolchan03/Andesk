/* Copyright 2020 Braden Farmer
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

package com.farmerbb.taskbar.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Process;
import android.os.UserManager;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.AppEntry;
import com.farmerbb.taskbar.util.U;

public class PersistentShortcutLaunchActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UserManager userManager = (UserManager) getSystemService(USER_SERVICE);

        String packageName = getIntent().getStringExtra("package_name");
        String componentName = getIntent().getStringExtra("component_name");
        String windowSize = getIntent().getStringExtra("window_size");
        long userId = getIntent().getLongExtra("user_id", userManager.getSerialNumberForUser(Process.myUserHandle()));

        if(packageName == null || componentName == null) {
            U.showToast(this, R.string.tb_invalid_shortcut);
            finish();
            return;
        }

        try {
            getPackageManager().getActivityInfo(new ComponentName(packageName, componentName), 0);
        } catch (PackageManager.NameNotFoundException | IllegalArgumentException e) {
            U.showToast(this, R.string.tb_invalid_shortcut);
            finish();
            return;
        }

        if(!U.canDrawOverlays(this) && windowSize != null) {
            Intent intent = new Intent(this, DummyActivity.class);
            intent.putExtra("show_permission_dialog", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(intent);
        } else {
            final AppEntry entry = new AppEntry(packageName, componentName, null, null, false);
            entry.setUserId(userId);
            U.launchApp(this, entry, windowSize,
                    () -> U.showToast(this, R.string.tb_invalid_shortcut));
        }

        finish();
    }
}