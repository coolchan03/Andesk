/* Copyright 2026 Andesk contributors
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

package com.farmerbb.taskbar.util;

import android.app.AlertDialog;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.farmerbb.taskbar.R;

import java.util.List;

/** Save the pinned apps as a named set, and switch between saved sets. */
public final class PinnedAppSetsDialog {
    private final Context context;
    private final Context themed;
    private final Runnable onApplied;
    private AlertDialog dialog;
    private LinearLayout list;

    private PinnedAppSetsDialog(Context context, Runnable onApplied) {
        this.context = context;
        this.themed = Ui.dialogContext(context);
        this.onApplied = onApplied;
    }

    /** @param onApplied runs after a set replaced the pinned apps */
    public static void show(Context context, Runnable onApplied) {
        new PinnedAppSetsDialog(context, onApplied).display();
    }

    private void display() {
        LinearLayout root = Ui.column(themed);
        Ui.padDialogBody(root);

        list = Ui.column(themed);
        ScrollView scroll = new ScrollView(themed);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(themed, 260)));

        Button save = new Button(themed);
        save.setText(R.string.andesk_sets_save_current);
        save.setOnClickListener(v -> Ui.promptName(context, R.string.andesk_sets_save_current,
                R.string.andesk_sets_name_hint, PinnedAppSets.MAX_NAME_LENGTH, this::saveNamed));
        root.addView(save, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.andesk_sets_title)
                .setView(root)
                .setPositiveButton(R.string.andesk_close, null)
                .create();
        dialog.show();

        rebuild();
    }

    private void rebuild() {
        list.removeAllViews();

        List<String> names = PinnedAppSets.names(context);
        if(names.isEmpty()) {
            list.addView(Ui.emptyState(themed, R.string.andesk_sets_empty));
            return;
        }

        for(String name : names) {
            int count = PinnedAppSets.components(context, name).size();
            list.addView(Ui.namedRow(themed, name,
                    themed.getResources().getQuantityString(R.plurals.andesk_sets_apps, count, count),
                    () -> confirmApply(name), () -> delete(name)));
        }
    }

    private void saveNamed(String entered) {
        String name = PinnedAppSets.normalizeName(entered);
        if(name == null) return;

        if(PinnedAppSets.exists(context, name)) {
            new AlertDialog.Builder(context)
                    .setMessage(context.getString(R.string.andesk_sets_overwrite, name))
                    .setNegativeButton(R.string.tb_action_cancel, null)
                    .setPositiveButton(R.string.tb_action_ok, (d, which) -> store(name))
                    .show();
        } else {
            store(name);
        }
    }

    private void store(String name) {
        String saved = PinnedAppSets.saveCurrent(context, name);
        if(saved == null) {
            U.showToast(context, context.getString(R.string.andesk_sets_full, PinnedAppSets.MAX_SETS));
            return;
        }

        U.showToast(context, context.getString(R.string.andesk_sets_saved, saved));
        rebuild();
    }

    private void confirmApply(String name) {
        new AlertDialog.Builder(context)
                .setMessage(context.getString(R.string.andesk_sets_apply_confirm, name))
                .setNegativeButton(R.string.tb_action_cancel, null)
                .setPositiveButton(R.string.tb_action_ok, (d, which) -> apply(name))
                .show();
    }

    private void apply(String name) {
        PinnedAppSets.Result result = PinnedAppSets.apply(context, name);
        if(result == null) {
            rebuild();
            return;
        }

        U.showToast(context, result.skipped == 0
                ? context.getString(R.string.andesk_sets_applied, name)
                : context.getString(R.string.andesk_sets_applied_skipped, name, result.skipped));
        dialog.dismiss();
        if(onApplied != null) onApplied.run();
    }

    private void delete(String name) {
        PinnedAppSets.delete(context, name);
        U.showToast(context, context.getString(R.string.andesk_sets_deleted, name));
        rebuild();
    }
}
