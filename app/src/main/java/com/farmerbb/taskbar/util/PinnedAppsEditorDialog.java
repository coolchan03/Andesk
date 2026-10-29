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
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.farmerbb.taskbar.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Lets the user reorder, add and remove the apps pinned to the taskbar. */
public final class PinnedAppsEditorDialog {
    private final Context context;
    private final float density;
    private LinearLayout list;

    private PinnedAppsEditorDialog(Context context) {
        this.context = context;
        this.density = context.getResources().getDisplayMetrics().density;
    }

    public static void show(Context context) {
        new PinnedAppsEditorDialog(context).display();
    }

    private int dp(int value) {
        return Math.round(value * density);
    }

    private void display() {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(8), dp(16), 0);

        list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);

        ScrollView scroll = new ScrollView(context);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(300)));

        Button add = new Button(context);
        add.setText(R.string.tb_pinned_add);
        add.setOnClickListener(v -> AppChooserDialog.show(context, R.string.tb_pinned_add, component -> {
            if(component != null) {
                U.pinApp(context, component);
                rebuild();
            }
        }));
        root.addView(add, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(context)
                .setTitle(R.string.tb_pinned_apps)
                .setView(root)
                .setPositiveButton(R.string.tb_pinned_done, null)
                .show();

        rebuild();
    }

    private void rebuild() {
        list.removeAllViews();

        PinnedBlockedApps pinnedBlockedApps = PinnedBlockedApps.getInstance(context);
        List<AppEntry> pinned = new ArrayList<>(pinnedBlockedApps.getPinnedApps());

        // A vertical taskbar lists its apps from the bottom up; show them as they appear.
        boolean vertical = TaskbarPosition.isVertical(context);
        if(vertical) Collections.reverse(pinned);

        if(pinned.isEmpty()) {
            TextView empty = new TextView(context);
            empty.setText(R.string.tb_pinned_apps_empty);
            empty.setPadding(0, dp(12), 0, dp(12));
            list.addView(empty);
            return;
        }

        for(int i = 0; i < pinned.size(); i++) {
            AppEntry entry = pinned.get(i);

            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(4), 0, dp(4));

            ImageView icon = new ImageView(context);
            icon.setImageDrawable(entry.getIcon(context));
            row.addView(icon, new LinearLayout.LayoutParams(dp(36), dp(36)));

            TextView label = new TextView(context);
            label.setText(entry.getLabel());
            label.setTextSize(16);
            label.setPadding(dp(12), 0, dp(8), 0);
            row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            // "Up" is toward the start of the visible order, which is the other way in the
            // stored order on a vertical taskbar.
            int towardStart = vertical ? 1 : -1;
            row.addView(makeButton("▲", i > 0, v -> move(entry, towardStart)));
            row.addView(makeButton("▼", i < pinned.size() - 1, v -> move(entry, -towardStart)));
            row.addView(makeButton("✕", true, v -> {
                pinnedBlockedApps.removePinnedApp(context, entry.getComponentName());
                rebuild();
            }));

            list.addView(row);
        }
    }

    private TextView makeButton(String symbol, boolean enabled, View.OnClickListener listener) {
        TextView button = new TextView(context);
        button.setText(symbol);
        button.setTextSize(18);
        button.setGravity(Gravity.CENTER);
        button.setAlpha(enabled ? 1f : 0.25f);
        button.setEnabled(enabled);
        button.setContentDescription(symbol);
        if(enabled) button.setOnClickListener(listener);
        button.setLayoutParams(new LinearLayout.LayoutParams(dp(44), dp(44)));
        return button;
    }

    private void move(AppEntry entry, int delta) {
        PinnedBlockedApps.getInstance(context).movePinnedApp(context, entry.getComponentName(), delta);
        rebuild();
    }
}
