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

package com.farmerbb.taskbar.util;

import android.app.AlertDialog;
import android.content.Context;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.farmerbb.taskbar.R;

/** A small emoji chooser: tap a common emoji or type/paste any emoji (and an optional label). */
public final class EmojiPickerDialog {
    public interface Callback {
        void onPicked(String emoji, String label);
    }

    private static final int[] COMMON = {
            0x1F600, 0x1F60E, 0x1F973, 0x1F916, 0x1F47E, 0x1F3AE, 0x1F3B5, 0x1F3A7,
            0x1F4F7, 0x1F3AC, 0x1F4DA, 0x1F4DD, 0x1F4C1, 0x1F4C2, 0x1F4BC, 0x1F3E0,
            0x2B50, 0x2764, 0x1F525, 0x26A1, 0x1F319, 0x2600, 0x1F308, 0x1F34E,
            0x1F355, 0x2615, 0x1F697, 0x2708, 0x1F680, 0x1F4A1, 0x1F527, 0x2699,
            0x1F512, 0x1F511, 0x1F4F1, 0x1F4BB, 0x1F5A5, 0x1F4DE, 0x1F4E7, 0x1F4C5,
            0x23F0, 0x1F310, 0x1F6D2, 0x1F4B0, 0x1F381, 0x1F3C6, 0x1F9E9, 0x1F431,
            0x1F436, 0x1F98A, 0x1F338, 0x1F332, 0x1F3A8, 0x1F3AF, 0x1F4AC, 0x1F4CC,
            0x1F514, 0x1F4E6, 0x1F9F0, 0x1F9EA
    };

    private static final int COLUMNS = 6;

    private EmojiPickerDialog() {}

    public static void show(Context context, boolean askForLabel, Callback callback) {
        float density = context.getResources().getDisplayMetrics().density;
        int pad = Math.round(16 * density);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(pad, pad / 2, pad, 0);

        EditText emojiInput = new EditText(context);
        emojiInput.setSingleLine(true);
        emojiInput.setHint(R.string.tb_emoji_hint);
        content.addView(emojiInput, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        EditText labelInput = null;
        if(askForLabel) {
            labelInput = new EditText(context);
            labelInput.setSingleLine(true);
            labelInput.setHint(R.string.tb_emoji_label_hint);
            content.addView(labelInput, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        GridLayout grid = new GridLayout(context);
        grid.setColumnCount(COLUMNS);
        int cell = Math.round(48 * density);
        for(int codePoint : COMMON) {
            String emoji = new String(Character.toChars(codePoint));
            TextView item = new TextView(context);
            item.setText(emoji);
            item.setTextSize(24);
            item.setGravity(Gravity.CENTER);
            item.setOnClickListener(v -> {
                emojiInput.setText(emoji);
                emojiInput.setSelection(emojiInput.getText().length());
            });
            grid.addView(item, new GridLayout.LayoutParams(
                    new ViewGroup.LayoutParams(cell, cell)));
        }

        ScrollView scroll = new ScrollView(context);
        scroll.addView(grid);
        content.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.round(220 * density)));

        EditText finalLabelInput = labelInput;
        new AlertDialog.Builder(context)
                .setTitle(R.string.tb_pick_emoji)
                .setView(content)
                .setPositiveButton(R.string.tb_use, (dialog, which) -> {
                    String emoji = emojiInput.getText().toString().trim();
                    if(emoji.isEmpty()) return;

                    String label = finalLabelInput == null
                            ? "" : finalLabelInput.getText().toString().trim();
                    callback.onPicked(emoji, label);
                })
                .setNegativeButton(R.string.tb_action_cancel, null)
                .show();
    }
}
