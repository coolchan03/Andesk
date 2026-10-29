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
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.widget.ColorWheelView;

import java.util.Locale;

/** A color picker built around a color wheel, with brightness, opacity, a color code and swatches. */
public final class ColorPickerDialog {
    public interface Callback {
        void onColorPicked(int argb);
    }

    private static final int[] SWATCHES = {
            0xFF000000, 0xFF202124, 0xFF607D8B, 0xFFFFFFFF, 0xFFE53935, 0xFFFB8C00,
            0xFFFDD835, 0xFF43A047, 0xFF00897B, 0xFF1E88E5, 0xFF5E35B1, 0xFFD81B60
    };

    private final Context context;
    private final float density;
    private final float[] hsv = new float[3];
    private int alpha;
    private boolean updatingHex;

    private ColorWheelView wheel;
    private SeekBar brightness;
    private SeekBar opacity;
    private View preview;
    private EditText hexField;

    private ColorPickerDialog(Context context) {
        this.context = context;
        this.density = context.getResources().getDisplayMetrics().density;
    }

    public static void show(Context context, int titleRes, int initialArgb, Callback callback) {
        new ColorPickerDialog(context).display(titleRes, initialArgb, callback);
    }

    private int dp(int value) {
        return Math.round(value * density);
    }

    private int current() {
        return Color.HSVToColor(alpha, hsv);
    }

    private void display(int titleRes, int initialArgb, Callback callback) {
        alpha = Color.alpha(initialArgb);
        Color.colorToHSV(initialArgb, hsv);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(20), dp(8), dp(20), dp(4));

        wheel = new ColorWheelView(context);
        wheel.setHueSaturation(hsv[0], hsv[1]);
        wheel.setValue(hsv[2]);
        wheel.setListener((hue, saturation) -> {
            hsv[0] = hue;
            hsv[1] = saturation;
            refresh(true);
        });
        content.addView(wheel, new LinearLayout.LayoutParams(dp(250), dp(250)));

        brightness = addSlider(content, R.string.tb_color_brightness, 100, Math.round(hsv[2] * 100));
        brightness.setOnSeekBarChangeListener(new SimpleListener() {
            @Override
            void onChanged(int progress) {
                hsv[2] = progress / 100f;
                wheel.setValue(hsv[2]);
                refresh(true);
            }
        });

        opacity = addSlider(content, R.string.tb_color_opacity, 255, alpha);
        opacity.setOnSeekBarChangeListener(new SimpleListener() {
            @Override
            void onChanged(int progress) {
                alpha = progress;
                refresh(true);
            }
        });

        LinearLayout codeRow = new LinearLayout(context);
        codeRow.setOrientation(LinearLayout.HORIZONTAL);
        codeRow.setGravity(Gravity.CENTER_VERTICAL);

        preview = new View(context);
        FrameLayout previewFrame = new FrameLayout(context);
        previewFrame.setBackground(makeBackdrop());
        previewFrame.addView(preview, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        codeRow.addView(previewFrame, new LinearLayout.LayoutParams(dp(48), dp(48)));

        hexField = new EditText(context);
        hexField.setSingleLine(true);
        hexField.setHint(R.string.tb_color_hex);
        hexField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        LinearLayout.LayoutParams hexParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        hexParams.setMargins(dp(12), 0, 0, 0);
        codeRow.addView(hexField, hexParams);
        content.addView(codeRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        hexField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if(updatingHex) return;

                Integer parsed = parseColor(s.toString());
                if(parsed == null) return;

                alpha = Color.alpha(parsed);
                Color.colorToHSV(parsed, hsv);
                wheel.setHueSaturation(hsv[0], hsv[1]);
                wheel.setValue(hsv[2]);
                brightness.setProgress(Math.round(hsv[2] * 100));
                opacity.setProgress(alpha);
                refresh(false);
            }
        });

        GridLayout swatches = new GridLayout(context);
        swatches.setColumnCount(6);
        for(int swatchColor : SWATCHES) {
            View swatch = new View(context);
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(swatchColor);
            circle.setStroke(dp(1), Color.argb(90, 128, 128, 128));
            swatch.setBackground(circle);
            swatch.setOnClickListener(v -> {
                // Swatches are opaque colors; keep the opacity the user already chose.
                Color.colorToHSV(swatchColor, hsv);
                wheel.setHueSaturation(hsv[0], hsv[1]);
                wheel.setValue(hsv[2]);
                brightness.setProgress(Math.round(hsv[2] * 100));
                refresh(true);
            });

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = dp(34);
            params.height = dp(34);
            params.setMargins(dp(5), dp(6), dp(5), dp(2));
            swatches.addView(swatch, params);
        }
        content.addView(swatches);

        ScrollView scroll = new ScrollView(context);
        scroll.addView(content);

        refresh(true);

        new AlertDialog.Builder(context)
                .setTitle(titleRes)
                .setView(scroll)
                .setPositiveButton(R.string.tb_action_ok, (dialog, which) -> callback.onColorPicked(current()))
                .setNegativeButton(R.string.tb_action_cancel, null)
                .show();
    }

    private SeekBar addSlider(LinearLayout parent, int labelRes, int max, int progress) {
        TextView label = new TextView(context);
        label.setText(labelRes);
        label.setTextSize(13);
        label.setPadding(dp(4), dp(10), 0, 0);
        parent.addView(label, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        SeekBar seekBar = new SeekBar(context);
        seekBar.setMax(max);
        seekBar.setProgress(progress);
        parent.addView(seekBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return seekBar;
    }

    /** A light and dark split behind the preview, so transparency is visible. */
    private GradientDrawable makeBackdrop() {
        GradientDrawable backdrop = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] { 0xFFEEEEEE, 0xFFEEEEEE, 0xFF333333, 0xFF333333 });
        backdrop.setCornerRadius(dp(10));
        return backdrop;
    }

    private void refresh(boolean updateHex) {
        int color = current();

        GradientDrawable swatch = new GradientDrawable();
        swatch.setColor(color);
        swatch.setCornerRadius(dp(10));
        preview.setBackground(swatch);

        if(updateHex) {
            updatingHex = true;
            hexField.setText(String.format(Locale.US, "#%08X", color));
            updatingHex = false;
        }
    }

    /** Accepts #RRGGBB and #AARRGGBB (the # is optional); returns null if it is not a color. */
    static Integer parseColor(String text) {
        String value = text.trim();
        if(value.startsWith("#")) value = value.substring(1);
        if(value.length() != 6 && value.length() != 8) return null;

        try {
            long parsed = Long.parseLong(value, 16);
            if(value.length() == 6) parsed |= 0xFF000000L;
            return (int) parsed;
        } catch(NumberFormatException e) {
            return null;
        }
    }

    private abstract static class SimpleListener implements SeekBar.OnSeekBarChangeListener {
        abstract void onChanged(int progress);

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if(fromUser) onChanged(progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {}
    }
}
