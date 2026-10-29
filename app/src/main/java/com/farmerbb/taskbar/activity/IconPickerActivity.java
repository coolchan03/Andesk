/* Copyright 2026 Andesk contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.farmerbb.taskbar.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.EmojiPickerDialog;
import com.farmerbb.taskbar.util.IconOverrides;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

/**
 * Lets the user replace an icon (a taskbar app or the Start button) with an emoji or a picked
 * image, or restore the original. The icon to change is passed in {@link #EXTRA_ICON_KEY}.
 */
public class IconPickerActivity extends Activity {
    private static final int REQUEST_IMAGE = 1;

    private String key;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        key = getIntent().getStringExtra(EXTRA_ICON_KEY);
        if(key == null) {
            finish();
            return;
        }

        if(savedInstanceState == null)
            showChoices();
    }

    private void showChoices() {
        CharSequence[] items = {
                getString(R.string.tb_icon_emoji),
                getString(R.string.tb_icon_image),
                getString(R.string.tb_icon_reset)
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.tb_change_icon)
                .setItems(items, (dialog, which) -> {
                    if(which == 0) {
                        EmojiPickerDialog.show(this, false, (emoji, label) -> {
                            IconOverrides.setEmoji(this, key, emoji);
                            finishAndRefresh();
                        });
                    } else if(which == 1) {
                        Intent picker = new Intent(Intent.ACTION_GET_CONTENT);
                        picker.setType("image/*");
                        picker.addCategory(Intent.CATEGORY_OPENABLE);
                        try {
                            startActivityForResult(picker, REQUEST_IMAGE);
                        } catch(ActivityNotFoundException e) {
                            finish();
                        }
                    } else {
                        IconOverrides.clear(this, key);
                        finishAndRefresh();
                    }
                })
                .setOnCancelListener(dialog -> finish())
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_IMAGE && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            if(!IconOverrides.setImage(this, key, data.getData()))
                U.showToast(this, R.string.tb_icon_image_failed);
            finishAndRefresh();
        } else
            finish();
    }

    private void finishAndRefresh() {
        U.sendBroadcast(this, ACTION_REFRESH_DESKTOP_ICONS);
        U.restartTaskbar(this);
        finish();
    }
}
