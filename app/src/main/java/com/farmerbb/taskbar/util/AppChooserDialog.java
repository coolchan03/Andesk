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
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.farmerbb.taskbar.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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
        PackageManager pm = context.getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);

        List<Item> all = new ArrayList<>();
        for(ResolveInfo info : pm.queryIntentActivities(main, 0)) {
            all.add(new Item(
                    String.valueOf(info.loadLabel(pm)),
                    new ComponentName(info.activityInfo.packageName, info.activityInfo.name),
                    info.loadIcon(pm)));
        }
        Collections.sort(all, (a, b) -> a.label.compareToIgnoreCase(b.label));

        // The first row restores the automatic behavior.
        all.add(0, new Item(context.getString(R.string.tb_app_automatic), null, null));

        float density = context.getResources().getDisplayMetrics().density;
        int pad = Math.round(16 * density);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(pad, pad / 2, pad, 0);

        EditText filter = new EditText(context);
        filter.setSingleLine(true);
        filter.setHint(R.string.tb_app_search_hint);
        content.addView(filter, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        List<Item> shown = new ArrayList<>(all);
        ArrayAdapter<Item> adapter = new ArrayAdapter<Item>(context, 0, shown) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                Item item = getItem(position);

                LinearLayout row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, Math.round(6 * density), 0, Math.round(6 * density));

                ImageView icon = new ImageView(context);
                if(item.icon != null)
                    icon.setImageDrawable(item.icon);
                row.addView(icon, new LinearLayout.LayoutParams(
                        Math.round(36 * density), Math.round(36 * density)));

                TextView label = new TextView(context);
                label.setText(item.label);
                label.setTextSize(16);
                label.setPadding(Math.round(12 * density), 0, 0, 0);
                row.addView(label);
                return row;
            }
        };

        ListView list = new ListView(context);
        list.setAdapter(adapter);
        content.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.round(320 * density)));

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(titleRes)
                .setView(content)
                .setNegativeButton(R.string.tb_action_cancel, null)
                .create();

        list.setOnItemClickListener((parent, view, position, id) -> {
            callback.onChosen(adapter.getItem(position).component);
            dialog.dismiss();
        });

        filter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim().toLowerCase(Locale.getDefault());
                adapter.clear();
                for(Item item : all) {
                    if(query.isEmpty() || item.label.toLowerCase(Locale.getDefault()).contains(query))
                        adapter.add(item);
                }
            }
        });

        dialog.show();
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
