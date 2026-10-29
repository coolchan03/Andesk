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

package com.farmerbb.taskbar.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

import static com.farmerbb.taskbar.util.Constants.*;

/** A small drawing of a screen with a taskbar in the given style, for the style chooser. */
public class TaskbarStylePreviewView extends View {
    private static final int[] APP_COLORS = {
            0xFFEF5350, 0xFFFFCA28, 0xFF66BB6A, 0xFF42A5F5, 0xFFAB47BC
    };

    private final String style;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float density;

    public TaskbarStylePreviewView(Context context, String style) {
        super(context);
        this.style = style;
        this.density = context.getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float corner = 8 * density;

        // The "wallpaper".
        paint.setShader(new LinearGradient(0, 0, w, h, 0xFF3B6FB6, 0xFF8E5BB5, Shader.TileMode.CLAMP));
        rect.set(0, 0, w, h);
        canvas.drawRoundRect(rect, corner, corner, paint);
        paint.setShader(null);

        float bar = h * 0.2f;
        float margin = 6 * density;

        switch(style) {
            case PREF_TASKBAR_STYLE_WINDOWS_7:
                drawBottomBar(canvas, w, h, bar, 0, Color.argb(200, 45, 91, 143), false, 0);
                drawHighlight(canvas, 0, h - bar, w);
                break;
            case PREF_TASKBAR_STYLE_WINDOWS_10:
                drawBottomBar(canvas, w, h, bar, 0, Color.rgb(32, 32, 32), false, 0);
                break;
            case PREF_TASKBAR_STYLE_WINDOWS_11:
                drawBottomBar(canvas, w, h, bar, 0, Color.argb(232, 32, 32, 32), true, 0);
                break;
            case PREF_TASKBAR_STYLE_CHROMEOS:
                drawBottomBar(canvas, w, h, bar, 0, Color.argb(235, 32, 33, 36), true, 0);
                break;
            case PREF_TASKBAR_STYLE_KDE:
                drawBottomBar(canvas, w, h, bar, 0, Color.argb(235, 27, 30, 32), false, 0xFF3DAEE9);
                break;
            case PREF_TASKBAR_STYLE_MACOS:
                drawFloatingDock(canvas, w, h, bar, margin, Color.argb(170, 242, 242, 247));
                break;
            case PREF_TASKBAR_STYLE_GNOME:
                drawSideDock(canvas, w, h, margin, Color.argb(215, 46, 52, 54));
                break;
            default:
                drawBottomBar(canvas, w, h, bar, 0, Color.argb(235, 48, 48, 48), false, 0);
                break;
        }
    }

    private void drawBottomBar(Canvas canvas, float w, float h, float bar, float radius,
                               int color, boolean centered, int underline) {
        paint.setColor(color);
        rect.set(0, h - bar, w, h);
        canvas.drawRect(rect, paint);

        float icon = bar * 0.55f;
        float gap = icon * 0.6f;
        float total = APP_COLORS.length * icon + (APP_COLORS.length - 1) * gap;
        float x = centered ? (w - total) / 2f : 6 * density + icon + gap;
        float y = h - bar + (bar - icon) / 2f;

        // The start button.
        paint.setColor(Color.WHITE);
        float startX = centered ? x - icon - gap : 6 * density;
        rect.set(startX, y, startX + icon, y + icon);
        canvas.drawRoundRect(rect, 3 * density, 3 * density, paint);

        for(int i = 0; i < APP_COLORS.length - 1; i++) {
            paint.setColor(APP_COLORS[i]);
            rect.set(x, y, x + icon, y + icon);
            canvas.drawRoundRect(rect, 3 * density, 3 * density, paint);

            if(underline != 0 && i == 0) {
                paint.setColor(underline);
                canvas.drawRect(x, h - 2 * density, x + icon, h, paint);
            }

            x += icon + gap;
        }

        // The clock at the end.
        paint.setColor(Color.argb(210, 255, 255, 255));
        rect.set(w - 6 * density - icon * 1.6f, y + icon * 0.2f, w - 6 * density, y + icon * 0.8f);
        canvas.drawRoundRect(rect, 2 * density, 2 * density, paint);
    }

    private void drawHighlight(Canvas canvas, float left, float top, float right) {
        paint.setColor(Color.argb(110, 255, 255, 255));
        canvas.drawRect(left, top, right, top + Math.max(1f, density), paint);
    }

    private void drawFloatingDock(Canvas canvas, float w, float h, float bar, float margin, int color) {
        float icon = bar * 0.7f;
        float gap = icon * 0.35f;
        float total = APP_COLORS.length * icon + (APP_COLORS.length - 1) * gap + 2 * margin;
        float left = (w - total) / 2f;
        float top = h - bar - margin * 1.4f;

        paint.setColor(color);
        rect.set(left, top, left + total, top + bar + 2 * margin * 0.3f);
        canvas.drawRoundRect(rect, 10 * density, 10 * density, paint);

        float x = left + margin;
        float y = top + (rect.height() - icon) / 2f;
        for(int appColor : APP_COLORS) {
            paint.setColor(appColor);
            this.rect.set(x, y, x + icon, y + icon);
            canvas.drawRoundRect(this.rect, 4 * density, 4 * density, paint);
            x += icon + gap;
        }
    }

    private void drawSideDock(Canvas canvas, float w, float h, float margin, int color) {
        float icon = h * 0.13f;
        float gap = icon * 0.35f;
        float total = APP_COLORS.length * icon + (APP_COLORS.length - 1) * gap + 2 * margin;
        float dockWidth = icon + 2 * margin;
        float top = (h - total) / 2f;

        paint.setColor(color);
        rect.set(margin, top, margin + dockWidth, top + total);
        canvas.drawRoundRect(rect, 9 * density, 9 * density, paint);

        float y = top + margin;
        for(int appColor : APP_COLORS) {
            paint.setColor(appColor);
            rect.set(margin + margin, y, margin + margin + icon, y + icon);
            canvas.drawRoundRect(rect, 3 * density, 3 * density, paint);
            y += icon + gap;
        }
    }
}
