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
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.farmerbb.taskbar.R;

import java.util.List;

/** Save the current look under a name, and switch between saved looks. */
public final class AppearanceProfilesDialog {
    private final Context context;
    private final Context themed;
    private final Runnable onApplied;
    private AlertDialog dialog;
    private LinearLayout list;

    private AppearanceProfilesDialog(Context context, Runnable onApplied) {
        this.context = context;
        this.themed = Ui.dialogContext(context);
        this.onApplied = onApplied;
    }

    /** @param onApplied runs after a profile has been applied, so the caller can redraw */
    public static void show(Context context, Runnable onApplied) {
        new AppearanceProfilesDialog(context, onApplied).display();
    }

    private void display() {
        LinearLayout root = Ui.column(themed);
        Ui.padDialogBody(root);

        list = Ui.column(themed);
        ScrollView scroll = new ScrollView(themed);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(themed, 280)));

        Button save = new Button(themed);
        save.setText(R.string.andesk_profiles_save_current);
        save.setOnClickListener(v -> promptForName());
        root.addView(save, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.andesk_profiles_title)
                .setView(root)
                .setPositiveButton(R.string.andesk_close, null)
                .create();
        dialog.show();

        rebuild();
    }

    private void rebuild() {
        list.removeAllViews();

        List<String> names = AppearanceProfiles.names(context);
        if(names.isEmpty()) {
            list.addView(Ui.emptyState(themed, R.string.andesk_profiles_empty));
            return;
        }

        for(String name : names) {
            LinearLayout row = Ui.row(themed);
            row.setMinimumHeight(Ui.dp(themed, Ui.TOUCH_TARGET));

            // Tapping the name applies the profile; the trash button removes it.
            LinearLayout text = Ui.column(themed);
            text.setPadding(0, Ui.dp(themed, Ui.SPACE_XS), Ui.dp(themed, Ui.SPACE_S), Ui.dp(themed, Ui.SPACE_XS));
            text.addView(Ui.body(themed, name));
            text.addView(Ui.caption(themed, themed.getString(R.string.andesk_profiles_count,
                    AppearanceProfiles.settingCount(context, name))));
            Ui.makeSelectable(text);
            text.setContentDescription(themed.getString(R.string.andesk_apply) + " " + name);
            text.setOnClickListener(v -> apply(name));
            row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            row.addView(Ui.iconButton(themed, R.drawable.tb_delete, R.string.andesk_delete, true,
                    v -> delete(name)));
            list.addView(row);
        }
    }

    private void apply(String name) {
        if(!AppearanceProfiles.apply(context, name)) {
            rebuild();
            return;
        }

        U.showToast(context, context.getString(R.string.andesk_profiles_applied, name));
        dialog.dismiss();
        if(onApplied != null) onApplied.run();
    }

    private void delete(String name) {
        AppearanceProfiles.delete(context, name);
        U.showToast(context, context.getString(R.string.andesk_profiles_deleted, name));
        rebuild();
    }

    private void promptForName() {
        EditText input = new EditText(themed);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setHint(R.string.andesk_profiles_name_hint);
        input.setFilters(new android.text.InputFilter[] {
                new android.text.InputFilter.LengthFilter(AppearanceProfiles.MAX_NAME_LENGTH) });

        LinearLayout holder = Ui.column(themed);
        Ui.padDialogBody(holder);
        holder.addView(input);

        AlertDialog prompt = new AlertDialog.Builder(context)
                .setTitle(R.string.andesk_profiles_save_current)
                .setView(holder)
                .setNegativeButton(R.string.tb_action_cancel, null)
                .setPositiveButton(R.string.tb_action_ok, null)
                .create();
        prompt.setOnShowListener(d -> prompt.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener((View v) -> {
            String name = AppearanceProfiles.normalizeName(input.getText().toString());
            if(name == null) {
                input.setError(themed.getString(R.string.andesk_profiles_name_required));
                return;
            }

            if(AppearanceProfiles.exists(context, name)) {
                confirmOverwrite(name, prompt);
            } else {
                store(name, prompt);
            }
        }));
        prompt.show();
    }

    private void confirmOverwrite(String name, AlertDialog prompt) {
        new AlertDialog.Builder(context)
                .setMessage(context.getString(R.string.andesk_profiles_overwrite, name))
                .setNegativeButton(R.string.tb_action_cancel, null)
                .setPositiveButton(R.string.tb_action_ok, (d, which) -> store(name, prompt))
                .show();
    }

    private void store(String name, AlertDialog prompt) {
        String saved = AppearanceProfiles.save(context, name);
        if(saved == null) {
            U.showToast(context, context.getString(R.string.andesk_profiles_full,
                    AppearanceProfiles.MAX_PROFILES));
            return;
        }

        U.showToast(context, context.getString(R.string.andesk_profiles_saved, saved));
        prompt.dismiss();
        rebuild();
    }
}
