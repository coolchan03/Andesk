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
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Per-item icon replacements for the desktop: an emoji or a picked image can be applied to any
 * app, file or folder shortcut. Overrides are keyed by {@link #appKey} or {@link #documentKey}.
 */
public final class IconOverrides {
    private static final String PREF_ICON_OVERRIDES = "andesk_icon_overrides";
    private static final String KEY_EMOJI = "emoji";
    private static final String KEY_IMAGE = "image";
    private static final int IMAGE_MAX_PIXELS = 256;

    private IconOverrides() {}

    public static String appKey(String componentName) {
        return "app:" + componentName;
    }

    public static String documentKey(String uri) {
        return "doc:" + uri;
    }

    public static boolean hasOverride(Context context, String key) {
        return load(context).has(key);
    }

    public static void setEmoji(Context context, String key, String emoji) {
        if(emoji == null || emoji.trim().isEmpty()) return;

        deleteImage(context, load(context).optJSONObject(key));
        put(context, key, KEY_EMOJI, emoji.trim());
    }

    /** Copies a downscaled copy of the image into app storage. */
    public static boolean setImage(Context context, String key, Uri uri) {
        File imagesDir = new File(context.getFilesDir(), "tb_images");
        imagesDir.mkdirs();

        String filename = "icon_" + Integer.toHexString(key.hashCode()) + "_"
                + System.currentTimeMillis();
        Bitmap bitmap = decodeScaled(context, uri);
        if(bitmap == null) return false;

        try(FileOutputStream output = new FileOutputStream(new File(imagesDir, filename))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
        } catch(IOException e) {
            Log.w("Andesk", "Unable to save icon image", e);
            return false;
        }

        deleteImage(context, load(context).optJSONObject(key));
        put(context, key, KEY_IMAGE, filename);
        return true;
    }

    public static void clear(Context context, String key) {
        JSONObject all = load(context);
        deleteImage(context, all.optJSONObject(key));
        all.remove(key);
        save(context, all);
    }

    /** @return the replacement icon, or null when the item uses its normal icon. */
    public static Drawable getDrawable(Context context, String key, int sizePx) {
        JSONObject override = load(context).optJSONObject(key);
        if(override == null) return null;

        String emoji = override.optString(KEY_EMOJI, null);
        if(emoji != null)
            return emojiDrawable(context, emoji, sizePx);

        String image = override.optString(KEY_IMAGE, null);
        if(image != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(
                    new File(new File(context.getFilesDir(), "tb_images"), image).getPath());
            if(bitmap != null) {
                BitmapDrawable drawable = new BitmapDrawable(context.getResources(), bitmap);
                drawable.setFilterBitmap(true);
                return drawable;
            }
        }

        return null;
    }

    public static Drawable emojiDrawable(Context context, String emoji, int sizePx) {
        int size = Math.max(1, sizePx);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(size * 0.78f);

        // Shrink long emoji sequences (flags, families) so they stay inside the icon.
        float width = paint.measureText(emoji);
        if(width > size * 0.95f)
            paint.setTextSize(paint.getTextSize() * size * 0.95f / width);

        Rect bounds = new Rect();
        paint.getTextBounds(emoji, 0, emoji.length(), bounds);
        canvas.drawText(emoji, size / 2f, size / 2f - bounds.exactCenterY(), paint);

        return new BitmapDrawable(context.getResources(), bitmap);
    }

    private static Bitmap decodeScaled(Context context, Uri uri) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try(InputStream input = context.getContentResolver().openInputStream(uri)) {
                if(input == null) return null;
                BitmapFactory.decodeStream(input, null, bounds);
            }

            int sample = 1;
            while(bounds.outWidth / sample > IMAGE_MAX_PIXELS * 2
                    || bounds.outHeight / sample > IMAGE_MAX_PIXELS * 2)
                sample *= 2;

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sample;
            try(InputStream input = context.getContentResolver().openInputStream(uri)) {
                if(input == null) return null;
                Bitmap decoded = BitmapFactory.decodeStream(input, null, options);
                if(decoded == null) return null;

                float scale = Math.min(1f, (float) IMAGE_MAX_PIXELS
                        / Math.max(decoded.getWidth(), decoded.getHeight()));
                if(scale >= 1f) return decoded;

                return Bitmap.createScaledBitmap(decoded,
                        Math.max(1, Math.round(decoded.getWidth() * scale)),
                        Math.max(1, Math.round(decoded.getHeight() * scale)), true);
            }
        } catch(IOException | RuntimeException e) {
            Log.w("Andesk", "Unable to read icon image", e);
            return null;
        }
    }

    private static void deleteImage(Context context, JSONObject override) {
        if(override == null) return;
        String image = override.optString(KEY_IMAGE, null);
        if(image != null)
            //noinspection ResultOfMethodCallIgnored
            new File(new File(context.getFilesDir(), "tb_images"), image).delete();
    }

    private static void put(Context context, String key, String type, String value) {
        JSONObject all = load(context);
        try {
            all.put(key, new JSONObject().put(type, value));
        } catch(JSONException e) {
            Log.w("Andesk", "Unable to save icon override", e);
            return;
        }
        save(context, all);
    }

    private static JSONObject load(Context context) {
        try {
            return new JSONObject(U.getSharedPreferences(context).getString(PREF_ICON_OVERRIDES, "{}"));
        } catch(JSONException e) {
            return new JSONObject();
        }
    }

    private static void save(Context context, JSONObject all) {
        SharedPreferences.Editor editor = U.getSharedPreferences(context).edit();
        editor.putString(PREF_ICON_OVERRIDES, all.toString()).apply();
    }
}
