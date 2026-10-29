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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

import java.util.Random;

import static com.farmerbb.taskbar.util.Constants.*;

/** A subtle pattern drawn over the taskbar and Start menu background. */
public final class TaskbarTexture {
    private TaskbarTexture() {}

    /** The chosen pattern's name, e.g. "dots", or "none". */
    public static String getPattern(Context context) {
        return U.getSharedPreferences(context).getString(PREF_TASKBAR_TEXTURE, "none");
    }

    /**
     * Puts the chosen pattern on top of a background.
     *
     * @param base the background; returned unchanged when no pattern is chosen
     * @param backgroundColor used to decide between a light or dark pattern
     * @param cornerRadius the background's corner radius in pixels, so the pattern follows it
     */
    public static Drawable wrap(Context context, Drawable base, int backgroundColor, float cornerRadius) {
        String pattern = getPattern(context);
        if(pattern == null || pattern.equals("none") || base == null)
            return base;

        String strength = U.getSharedPreferences(context).getString(PREF_TASKBAR_TEXTURE_STRENGTH, "medium");
        Drawable overlay = new PatternDrawable(
                createTile(context, pattern, strength, U.isDarkBackground(backgroundColor)),
                cornerRadius);

        return new LayerDrawable(new Drawable[] { base, overlay });
    }

    /** A small repeating image for the pattern. */
    static Bitmap createTile(Context context, String pattern, String strength, boolean darkBackground) {
        float density = context.getResources().getDisplayMetrics().density;
        int base = darkBackground ? Color.WHITE : Color.BLACK;

        int alpha;
        switch(strength) {
            case "subtle":
                alpha = 22;
                break;
            case "strong":
                alpha = 70;
                break;
            default:
                alpha = 40;
                break;
        }

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.argb(alpha, Color.red(base), Color.green(base), Color.blue(base)));

        int size;
        Bitmap tile;
        Canvas canvas;

        switch(pattern) {
            case "dots":
                size = Math.max(4, Math.round(8 * density));
                tile = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                canvas = new Canvas(tile);
                canvas.drawCircle(size / 2f, size / 2f, Math.max(1f, density), paint);
                return tile;
            case "lines":
                size = Math.max(4, Math.round(8 * density));
                tile = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                canvas = new Canvas(tile);
                paint.setStrokeWidth(Math.max(1f, density));
                // Diagonal lines that join up across neighbouring tiles.
                canvas.drawLine(0, size, size, 0, paint);
                canvas.drawLine(-size / 2f, size / 2f, size / 2f, -size / 2f, paint);
                canvas.drawLine(size / 2f, size * 1.5f, size * 1.5f, size / 2f, paint);
                return tile;
            case "grid":
                size = Math.max(6, Math.round(10 * density));
                tile = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                canvas = new Canvas(tile);
                paint.setStrokeWidth(Math.max(1f, density * 0.8f));
                canvas.drawLine(0, 0, size, 0, paint);
                canvas.drawLine(0, 0, 0, size, paint);
                return tile;
            case "carbon":
                size = Math.max(4, Math.round(8 * density));
                tile = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                canvas = new Canvas(tile);
                canvas.drawRect(0, 0, size / 2f, size / 2f, paint);
                canvas.drawRect(size / 2f, size / 2f, size, size, paint);
                return tile;
            case "noise":
            default:
                size = 64;
                tile = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                Random random = new Random(42);
                for(int x = 0; x < size; x++) {
                    for(int y = 0; y < size; y++) {
                        if(random.nextInt(100) < 35) {
                            int speckle = Math.round(alpha * (0.4f + random.nextFloat() * 0.9f));
                            tile.setPixel(x, y, Color.argb(Math.min(255, speckle),
                                    Color.red(base), Color.green(base), Color.blue(base)));
                        }
                    }
                }
                return tile;
        }
    }

    /** Draws a bitmap tile repeatedly, clipped to a rounded rectangle. */
    static final class PatternDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF bounds = new RectF();
        private final float radius;

        PatternDrawable(Bitmap tile, float radius) {
            this.radius = radius;
            paint.setShader(new BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
        }

        @Override
        public void draw(Canvas canvas) {
            bounds.set(getBounds());
            canvas.drawRoundRect(bounds, radius, radius, paint);
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
