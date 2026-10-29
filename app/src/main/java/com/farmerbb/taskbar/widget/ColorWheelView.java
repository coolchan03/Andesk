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
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * A hue/saturation color wheel. Hue goes around the circle and saturation grows from the
 * centre outwards; brightness is set from outside with {@link #setValue(float)}.
 */
public class ColorWheelView extends View {
    public interface Listener {
        void onWheelChanged(float hue, float saturation);
    }

    private final Paint huePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint saturationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knobFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knobStroke = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float hue;
    private float saturation;
    private float value = 1f;
    private float centerX;
    private float centerY;
    private float radius;
    private float knobRadius;
    private Listener listener;

    public ColorWheelView(Context context) {
        this(context, null);
    }

    public ColorWheelView(Context context, AttributeSet attrs) {
        super(context, attrs);

        float density = context.getResources().getDisplayMetrics().density;
        knobRadius = 11 * density;

        knobStroke.setStyle(Paint.Style.STROKE);
        knobStroke.setStrokeWidth(2.5f * density);
        knobStroke.setColor(Color.WHITE);
        knobStroke.setShadowLayer(3 * density, 0, 0, Color.argb(160, 0, 0, 0));
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setHueSaturation(float hue, float saturation) {
        this.hue = ((hue % 360f) + 360f) % 360f;
        this.saturation = Math.max(0f, Math.min(1f, saturation));
        invalidate();
    }

    public void setValue(float value) {
        this.value = Math.max(0f, Math.min(1f, value));
        invalidate();
    }

    public float getHue() {
        return hue;
    }

    public float getSaturation() {
        return saturation;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = Math.min(
                resolveSize(Integer.MAX_VALUE, widthMeasureSpec),
                resolveSize(Integer.MAX_VALUE, heightMeasureSpec));
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        centerX = w / 2f;
        centerY = h / 2f;
        radius = Math.max(1f, Math.min(w, h) / 2f - knobRadius);

        int[] hues = new int[7];
        for(int i = 0; i < hues.length; i++)
            hues[i] = Color.HSVToColor(new float[] { i * 60f, 1f, 1f });

        huePaint.setShader(new SweepGradient(centerX, centerY, hues, null));
        saturationPaint.setShader(new RadialGradient(
                centerX, centerY, radius, Color.WHITE, Color.TRANSPARENT, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawCircle(centerX, centerY, radius, huePaint);
        canvas.drawCircle(centerX, centerY, radius, saturationPaint);

        dimPaint.setColor(Color.argb(Math.round((1f - value) * 255f), 0, 0, 0));
        canvas.drawCircle(centerX, centerY, radius, dimPaint);

        double angle = Math.toRadians(hue);
        float knobX = centerX + (float) Math.cos(angle) * saturation * radius;
        float knobY = centerY + (float) Math.sin(angle) * saturation * radius;

        knobFill.setColor(Color.HSVToColor(new float[] { hue, saturation, value }));
        canvas.drawCircle(knobX, knobY, knobRadius, knobFill);
        canvas.drawCircle(knobX, knobY, knobRadius, knobStroke);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch(event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if(getParent() != null)
                    getParent().requestDisallowInterceptTouchEvent(true);
                // fall through
            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - centerX;
                float dy = event.getY() - centerY;
                float distance = (float) Math.hypot(dx, dy);

                hue = (float) ((Math.toDegrees(Math.atan2(dy, dx)) + 360.0) % 360.0);
                saturation = Math.min(1f, distance / radius);
                invalidate();

                if(listener != null)
                    listener.onWheelChanged(hue, saturation);
                return true;
            case MotionEvent.ACTION_UP:
                performClick();
                return true;
        }

        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
