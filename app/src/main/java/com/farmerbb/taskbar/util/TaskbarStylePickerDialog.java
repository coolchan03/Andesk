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
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.widget.TaskbarStylePreviewView;

/** Shows every taskbar style as a small picture so the choice is visible, not just named. */
public final class TaskbarStylePickerDialog {
    public interface Callback {
        void onStylePicked(String value);
    }

    private TaskbarStylePickerDialog() {}

    /** The user-facing name of a style value, e.g. "Windows 11-like". */
    public static String getStyleName(Context context, String value) {
        String[] names = context.getResources().getStringArray(R.array.tb_pref_taskbar_style);
        String[] values = context.getResources().getStringArray(R.array.tb_pref_taskbar_style_values);
        for(int i = 0; i < values.length && i < names.length; i++) {
            if(values[i].equals(value)) return names[i];
        }

        return value;
    }

    public static void show(Context context, String current, Callback callback) {
        String[] names = context.getResources().getStringArray(R.array.tb_pref_taskbar_style);
        String[] values = context.getResources().getStringArray(R.array.tb_pref_taskbar_style_values);
        String[] descriptions = context.getResources().getStringArray(R.array.tb_pref_taskbar_style_descriptions);

        float density = context.getResources().getDisplayMetrics().density;
        int accent = U.getAccentColor(context);

        GridLayout grid = new GridLayout(context);
        grid.setColumnCount(2);
        grid.setPadding(Math.round(8 * density), Math.round(8 * density),
                Math.round(8 * density), Math.round(8 * density));

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        ScrollView scroll = new ScrollView(context);
        scroll.addView(grid);
        builder.setTitle(R.string.tb_choose_style)
                .setView(scroll)
                .setNegativeButton(R.string.tb_action_cancel, null);
        AlertDialog dialog = builder.create();

        for(int i = 0; i < values.length && i < names.length; i++) {
            String value = values[i];
            boolean selected = value.equals(current);

            LinearLayout card = new LinearLayout(context);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(Math.round(8 * density), Math.round(8 * density),
                    Math.round(8 * density), Math.round(8 * density));

            GradientDrawable border = new GradientDrawable();
            border.setCornerRadius(12 * density);
            border.setColor(Color.argb(selected ? 36 : 14, 128, 128, 128));
            border.setStroke(Math.round((selected ? 2.5f : 1f) * density),
                    selected ? opaque(accent) : Color.argb(60, 128, 128, 128));
            card.setBackground(border);

            card.addView(new TaskbarStylePreviewView(context, value), new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, Math.round(92 * density)));

            TextView title = new TextView(context);
            title.setText(names[i]);
            title.setTextSize(14);
            title.setTypeface(null, android.graphics.Typeface.BOLD);
            title.setPadding(0, Math.round(6 * density), 0, 0);
            card.addView(title);

            if(i < descriptions.length) {
                TextView description = new TextView(context);
                description.setText(descriptions[i]);
                description.setTextSize(12);
                description.setAlpha(0.75f);
                card.addView(description);
            }

            card.setOnClickListener(v -> {
                callback.onStylePicked(value);
                dialog.dismiss();
            });

            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, 1f),
                    GridLayout.spec(GridLayout.UNDEFINED, 1f));
            params.width = 0;
            params.setGravity(Gravity.FILL_HORIZONTAL);
            params.setMargins(Math.round(5 * density), Math.round(5 * density),
                    Math.round(5 * density), Math.round(5 * density));
            grid.addView(card, params);
        }

        dialog.show();
    }

    /** Keeps the accent color opaque for the selection outline. */
    private static int opaque(int color) {
        return Color.rgb(Color.red(color), Color.green(color), Color.blue(color));
    }
}
