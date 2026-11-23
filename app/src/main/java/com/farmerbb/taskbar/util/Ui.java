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
import android.content.res.TypedArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Andesk's design system for views built in code: the spacing scale, type sizes and the few
 * building blocks (rows, icon buttons, captions) that every programmatic dialog shares, so they
 * all look like the XML screens. Colors always come from the active theme, never from literals,
 * so light and dark dialogs both read correctly.
 */
public final class Ui {
    /** Spacing scale in dp. */
    public static final int SPACE_XS = 4;
    public static final int SPACE_S = 8;
    public static final int SPACE_M = 12;
    public static final int SPACE_L = 16;

    /** Minimum touch target and list icon size in dp. */
    public static final int TOUCH_TARGET = 48;
    public static final int LIST_ICON = 36;

    /** Type scale in sp. */
    public static final float TEXT_BODY = 16f;
    public static final float TEXT_CAPTION = 13f;

    private Ui() {}

    public static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    /**
     * A context carrying the theme AlertDialogs use, so views added to a dialog resolve
     * their colors against the dialog's palette rather than the screen behind it.
     */
    public static Context dialogContext(Context context) {
        return new AlertDialog.Builder(context).getContext();
    }

    public static int themeColor(Context context, int attr, int fallback) {
        TypedValue value = new TypedValue();
        if(context.getTheme().resolveAttribute(attr, value, true)) {
            if(value.type >= TypedValue.TYPE_FIRST_COLOR_INT && value.type <= TypedValue.TYPE_LAST_COLOR_INT)
                return value.data;
            if(value.resourceId != 0) {
                try {
                    return context.getResources().getColor(value.resourceId, context.getTheme());
                } catch(RuntimeException ignored) {}
            }
        }
        return fallback;
    }

    public static int textPrimary(Context context) {
        return themeColor(context, android.R.attr.textColorPrimary, 0xFF202124);
    }

    public static int textSecondary(Context context) {
        return themeColor(context, android.R.attr.textColorSecondary, 0xFF5F6368);
    }

    public static int accent(Context context) {
        return themeColor(context, android.R.attr.colorAccent, 0xFF0A7A67);
    }

    public static LinearLayout column(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    public static LinearLayout row(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    /** Standard dialog body padding: space on the sides, a little above, none below. */
    public static void padDialogBody(View view) {
        Context context = view.getContext();
        view.setPadding(dp(context, SPACE_L), dp(context, SPACE_S), dp(context, SPACE_L), 0);
    }

    public static TextView body(Context context, CharSequence text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_BODY);
        view.setTextColor(textPrimary(context));
        return view;
    }

    public static TextView caption(Context context, CharSequence text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_CAPTION);
        view.setTextColor(textSecondary(context));
        return view;
    }

    /** A message shown in place of an empty list. */
    public static TextView emptyState(Context context, int textRes) {
        TextView view = caption(context, context.getString(textRes));
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_BODY);
        view.setGravity(Gravity.CENTER);
        int pad = dp(context, SPACE_L);
        view.setPadding(pad, pad * 2, pad, pad * 2);
        return view;
    }

    /** Gives a view the platform's own ripple/press feedback, clipped to the view. */
    public static void makeSelectable(View view) {
        TypedArray array = view.getContext().obtainStyledAttributes(
                new int[] { android.R.attr.selectableItemBackground });
        try {
            view.setBackground(array.getDrawable(0));
        } finally {
            array.recycle();
        }
        view.setClickable(true);
        view.setFocusable(true);
    }

    /** A 48dp icon button with a spoken label; disabled buttons stay in place but dimmed. */
    public static ImageButton iconButton(Context context, int iconRes, int descriptionRes,
            boolean enabled, View.OnClickListener listener) {
        ImageButton button = new ImageButton(context);
        button.setImageResource(iconRes);
        button.setScaleType(ImageView.ScaleType.CENTER);
        button.setContentDescription(context.getString(descriptionRes));
        makeSelectable(button);
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.3f);
        if(enabled) button.setOnClickListener(listener);
        button.setLayoutParams(new LinearLayout.LayoutParams(
                dp(context, TOUCH_TARGET), dp(context, TOUCH_TARGET)));
        return button;
    }

    /** A list row: icon (optional), a single-line title that takes the free width. */
    public static LinearLayout listRow(Context context, android.graphics.drawable.Drawable icon, CharSequence title) {
        LinearLayout row = row(context);
        int pad = dp(context, SPACE_XS);
        row.setPadding(0, pad, 0, pad);
        row.setMinimumHeight(dp(context, TOUCH_TARGET));

        if(icon != null) {
            ImageView image = new ImageView(context);
            image.setImageDrawable(icon);
            image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            row.addView(image, new LinearLayout.LayoutParams(
                    dp(context, LIST_ICON), dp(context, LIST_ICON)));
        }

        TextView label = body(context, title);
        label.setSingleLine(true);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        label.setPadding(icon != null ? dp(context, SPACE_M) : 0, 0, dp(context, SPACE_S), 0);
        row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }
}
