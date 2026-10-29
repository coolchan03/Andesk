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
import android.content.res.XmlResourceParser;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.fragment.AdvancedFragment;
import com.farmerbb.taskbar.fragment.AppearanceFragment;
import com.farmerbb.taskbar.fragment.FreeformModeFragment;
import com.farmerbb.taskbar.fragment.GeneralFragment;
import com.farmerbb.taskbar.fragment.RecentAppsFragment;
import com.farmerbb.taskbar.fragment.SettingsFragment;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Finds settings by name across all of Andesk's settings screens. */
public final class SettingsSearch {
    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    public static final class Result {
        public final String key;
        public final String title;
        public final String summary;
        public final String section;
        public final Class<? extends SettingsFragment> fragment;

        Result(String key, String title, String summary, String section,
               Class<? extends SettingsFragment> fragment) {
            this.key = key;
            this.title = title;
            this.summary = summary;
            this.section = section;
            this.fragment = fragment;
        }
    }

    public interface Callback {
        void onResultChosen(Result result);
    }

    private SettingsSearch() {}

    private static final class Screen {
        final int xml;
        final int title;
        final Class<? extends SettingsFragment> fragment;

        Screen(int xml, int title, Class<? extends SettingsFragment> fragment) {
            this.xml = xml;
            this.title = title;
            this.fragment = fragment;
        }
    }

    private static List<Screen> screens(Context context) {
        List<Screen> screens = new ArrayList<>();
        screens.add(new Screen(R.xml.tb_pref_general, R.string.tb_pref_header_general, GeneralFragment.class));
        screens.add(new Screen(R.xml.tb_pref_appearance, R.string.tb_pref_header_appearance, AppearanceFragment.class));
        screens.add(new Screen(R.xml.tb_pref_recent_apps, R.string.tb_pref_header_recent_apps, RecentAppsFragment.class));
        if(U.canEnableFreeform(context))
            screens.add(new Screen(R.xml.tb_pref_freeform_hack, R.string.tb_pref_header_freeform, FreeformModeFragment.class));
        screens.add(new Screen(R.xml.tb_pref_advanced, R.string.tb_pref_header_advanced, AdvancedFragment.class));
        return screens;
    }

    /** Every setting that has a name, across all screens. */
    public static List<Result> index(Context context) {
        List<Result> results = new ArrayList<>();

        for(Screen screen : screens(context)) {
            String section = context.getString(screen.title);

            XmlResourceParser parser = context.getResources().getXml(screen.xml);
            try {
                int event;
                while((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
                    if(event != XmlPullParser.START_TAG) continue;

                    String tag = parser.getName();
                    if(tag.equals("PreferenceScreen") || tag.equals("PreferenceCategory")) continue;

                    String key = parser.getAttributeValue(ANDROID_NS, "key");
                    String title = text(context, parser, "title");
                    if(key == null || title == null || title.trim().isEmpty()) continue;

                    results.add(new Result(key, title, text(context, parser, "summary"), section, screen.fragment));
                }
            } catch(XmlPullParserException | IOException | RuntimeException e) {
                android.util.Log.w("Andesk", "Unable to index settings screen", e);
            } finally {
                parser.close();
            }
        }

        return results;
    }

    private static String text(Context context, XmlResourceParser parser, String attribute) {
        int resource = parser.getAttributeResourceValue(ANDROID_NS, attribute, 0);
        if(resource != 0) {
            try {
                return context.getString(resource);
            } catch(RuntimeException e) {
                return null;
            }
        }

        return parser.getAttributeValue(ANDROID_NS, attribute);
    }

    /** Settings whose name or description contains every word of the query. */
    public static List<Result> search(Context context, String query) {
        List<Result> matches = new ArrayList<>();
        String[] words = query.trim().toLowerCase(Locale.getDefault()).split("\\s+");
        if(words.length == 0 || words[0].isEmpty()) return matches;

        for(Result result : index(context)) {
            String haystack = (result.title + " " + (result.summary == null ? "" : result.summary)
                    + " " + result.section).toLowerCase(Locale.getDefault());

            boolean all = true;
            for(String word : words) {
                if(!haystack.contains(word)) {
                    all = false;
                    break;
                }
            }

            if(all) matches.add(result);
        }

        return matches;
    }

    public static void showDialog(Context context, Callback callback) {
        float density = context.getResources().getDisplayMetrics().density;
        int pad = Math.round(16 * density);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(pad, pad / 2, pad, 0);

        EditText input = new EditText(context);
        input.setSingleLine(true);
        input.setHint(R.string.tb_search_settings);
        content.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        List<Result> shown = new ArrayList<>();
        ArrayAdapter<Result> adapter = new ArrayAdapter<Result>(context, 0, shown) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                Result result = getItem(position);

                LinearLayout row = new LinearLayout(context);
                row.setOrientation(LinearLayout.VERTICAL);
                row.setPadding(0, Math.round(8 * density), 0, Math.round(8 * density));

                TextView title = new TextView(context);
                title.setText(result.title);
                title.setTextSize(16);
                row.addView(title);

                TextView where = new TextView(context);
                where.setText(result.section);
                where.setTextSize(12);
                where.setAlpha(0.7f);
                row.addView(where);
                return row;
            }
        };

        ListView list = new ListView(context);
        list.setAdapter(adapter);
        content.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.round(300 * density)));

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.tb_search_settings)
                .setView(content)
                .setNegativeButton(R.string.tb_action_cancel, null)
                .create();

        list.setOnItemClickListener((parent, view, position, id) -> {
            callback.onResultChosen(adapter.getItem(position));
            dialog.dismiss();
        });

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                adapter.clear();
                adapter.addAll(search(context, s.toString()));
            }
        });

        dialog.show();
    }
}
