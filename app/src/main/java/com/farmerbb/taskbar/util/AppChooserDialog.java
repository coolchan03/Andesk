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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.farmerbb.taskbar.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** A searchable list of the apps that can be launched, used to choose an app for an action. */
public final class AppChooserDialog {
    public interface Callback {
        /** @param component the chosen app's launcher activity, or null for "automatic" */
        void onChosen(ComponentName component);
    }

    private static final class Item {
        final String label;
        final ComponentName component;
        final Drawable icon;

        Item(String label, ComponentName component, Drawable icon) {
            this.label = label;
            this.component = component;
            this.icon = icon;
        }
    }

    private AppChooserDialog() {}

    public static void show(Context context, int titleRes, Callback callback) {
        Context themed = Ui.dialogContext(context);

        LinearLayout content = Ui.column(themed);
        Ui.padDialogBody(content);

        EditText filter = new EditText(themed);
        filter.setSingleLine(true);
        filter.setHint(R.string.tb_app_search_hint);
        filter.setEnabled(false);
        content.addView(filter, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // The app list is built off the main thread: loading every launcher icon takes long
        // enough on a device with many apps to freeze the screen the dialog opens from.
        ProgressBar progress = new ProgressBar(themed);
        FrameLayout body = new FrameLayout(themed);
        body.addView(progress, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        content.addView(body, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(themed, 320)));

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(titleRes)
                .setView(content)
                .setNegativeButton(R.string.tb_action_cancel, null)
                .create();
        dialog.show();

        Context app = context.getApplicationContext();
        Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            List<Item> loaded = loadItems(app);
            main.post(() -> {
                if(!dialog.isShowing()) return;

                loaded.add(0, new Item(app.getString(R.string.tb_app_automatic), null, null));
                bind(themed, dialog, content, filter, body, loaded, callback);
            });
        }, "AndeskAppChooser").start();
    }

    private static List<Item> loadItems(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);

        Set<ComponentName> seen = new HashSet<>();
        List<Item> all = new ArrayList<>();
        for(ResolveInfo info : pm.queryIntentActivities(main, 0)) {
            ComponentName component = new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
            if(!seen.add(component)) continue;

            all.add(new Item(String.valueOf(info.loadLabel(pm)), component, info.loadIcon(pm)));
        }

        Collections.sort(all, (a, b) -> a.label.compareToIgnoreCase(b.label));
        return all;
    }

    private static void bind(Context themed, AlertDialog dialog, LinearLayout content, EditText filter,
            FrameLayout body, List<Item> all, Callback callback) {
        List<Item> shown = new ArrayList<>(all);
        ArrayAdapter<Item> adapter = new ArrayAdapter<Item>(themed, 0, shown) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                Item item = getItem(position);
                return Ui.listRow(themed, item.icon, item.label);
            }
        };

        ListView list = new ListView(themed);
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> {
            callback.onChosen(adapter.getItem(position).component);
            dialog.dismiss();
        });

        body.removeAllViews();
        body.addView(list, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        filter.setEnabled(true);
        filter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim().toLowerCase(Locale.getDefault());
                adapter.setNotifyOnChange(false);
                adapter.clear();
                for(Item item : all) {
                    if(query.isEmpty() || item.label.toLowerCase(Locale.getDefault()).contains(query))
                        adapter.add(item);
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    /** The label of an app's launcher activity, or null if it is not installed. */
    public static String getLabel(Context context, ComponentName component) {
        if(component == null) return null;

        try {
            PackageManager pm = context.getPackageManager();
            return String.valueOf(pm.getActivityInfo(component, 0).loadLabel(pm));
        } catch(PackageManager.NameNotFoundException | RuntimeException e) {
            return null;
        }
    }
}
