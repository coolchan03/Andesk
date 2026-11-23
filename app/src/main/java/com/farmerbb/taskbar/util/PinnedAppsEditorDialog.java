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
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.farmerbb.taskbar.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Lets the user reorder, add and remove the apps pinned to the taskbar. */
public final class PinnedAppsEditorDialog {
    private final Context context;
    private final Context themed;
    private LinearLayout list;
    private boolean changed;

    private PinnedAppsEditorDialog(Context context) {
        this.context = context;
        this.themed = Ui.dialogContext(context);
    }

    public static void show(Context context) {
        new PinnedAppsEditorDialog(context).display();
    }

    private void display() {
        LinearLayout root = Ui.column(themed);
        Ui.padDialogBody(root);

        list = Ui.column(themed);

        ScrollView scroll = new ScrollView(themed);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(themed, 300)));

        LinearLayout actions = Ui.row(themed);
        Button add = new Button(themed);
        add.setText(R.string.tb_pinned_add);
        add.setOnClickListener(v -> AppChooserDialog.show(context, R.string.tb_pinned_add, component -> {
            if(component == null) return;

            PinnedBlockedApps pinned = PinnedBlockedApps.getInstance(context);
            if(pinned.isPinned(component.flattenToString())) {
                U.showToast(context, R.string.tb_pinned_already);
            } else if(pinned.getPinnedApps().size() >= U.getMaxNumOfEntries(context)) {
                U.showToast(context, R.string.tb_pinned_full);
            } else if(U.pinApp(context, component)) {
                changed = true;
                rebuild();
            } else {
                U.showToast(context, R.string.tb_pinned_failed);
            }
        }));

        Button clear = new Button(themed);
        clear.setText(R.string.tb_pinned_clear);
        clear.setOnClickListener(v -> new AlertDialog.Builder(context)
                .setMessage(R.string.tb_pinned_clear_confirm)
                .setNegativeButton(R.string.tb_action_cancel, null)
                .setPositiveButton(R.string.tb_action_ok, (confirm, which) -> {
                    PinnedBlockedApps pinned = PinnedBlockedApps.getInstance(context);
                    for(AppEntry entry : pinned.getPinnedApps())
                        pinned.removePinnedApp(context, entry.getComponentName());
                    changed = true;
                    rebuild();
                })
                .show());

        Button sets = new Button(themed);
        sets.setText(R.string.andesk_sets_button);
        sets.setOnClickListener(v -> PinnedAppSetsDialog.show(context, () -> {
            changed = true;
            rebuild();
        }));

        actions.addView(add, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        actions.addView(sets, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        actions.addView(clear, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(actions);

        new AlertDialog.Builder(context)
                .setTitle(R.string.tb_pinned_apps)
                .setView(root)
                .setPositiveButton(R.string.tb_pinned_done, null)
                // Redraw the taskbar once, after all edits, rather than on every tap.
                .setOnDismissListener(dialog -> {
                    if(changed) U.restartTaskbar(context);
                })
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
            list.addView(Ui.emptyState(themed, R.string.tb_pinned_apps_empty));
            return;
        }

        // "Up" is toward the start of the visible order, which is the other way in the
        // stored order on a vertical taskbar.
        int towardStart = vertical ? 1 : -1;

        for(int i = 0; i < pinned.size(); i++) {
            AppEntry entry = pinned.get(i);

            LinearLayout row = Ui.listRow(themed, entry.getIcon(context), entry.getLabel());
            row.addView(Ui.iconButton(themed, R.drawable.tb_arrow_up, R.string.andesk_move_up,
                    i > 0, v -> move(entry, towardStart)));
            row.addView(Ui.iconButton(themed, R.drawable.tb_arrow_down, R.string.andesk_move_down,
                    i < pinned.size() - 1, v -> move(entry, -towardStart)));
            row.addView(Ui.iconButton(themed, R.drawable.tb_close, R.string.andesk_remove, true, v -> {
                pinnedBlockedApps.removePinnedApp(context, entry.getComponentName());
                changed = true;
                rebuild();
            }));

            list.addView(row);
        }
    }

    private void move(AppEntry entry, int delta) {
        if(PinnedBlockedApps.getInstance(context)
                .movePinnedApp(context, entry.getComponentName(), delta)) {
            changed = true;
            rebuild();
        }
    }
}
