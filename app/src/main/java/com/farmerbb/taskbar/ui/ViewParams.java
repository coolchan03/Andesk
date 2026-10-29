/* Copyright 2019 Braden Farmer
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

package com.farmerbb.taskbar.ui;

import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.farmerbb.taskbar.util.U;

import java.lang.reflect.Field;

public class ViewParams {
    public int width;
    public int height;
    public int gravity;
    public int flags;
    public int bottomMargin;
    // Desktop overlays may extend over the non-tappable gesture navigation area.
    public boolean extendIntoGestureArea;
    // Gap kept between a floating dock and the screen edge it is anchored to.
    public int edgeMargin;

    public ViewParams(int width, int height, int gravity, int flags, int bottomMargin) {
        this.width = width;
        this.height = height;
        this.gravity = gravity;
        this.flags = flags;
        this.bottomMargin = bottomMargin;
    }

    public WindowManager.LayoutParams toWindowManagerParams() {
        final WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams(
                width,
                height,
                U.getOverlayType(),
                flags,
                PixelFormat.TRANSLUCENT
        );

        if(gravity > -1)
            wmParams.gravity = gravity;

        if(bottomMargin > -1)
            wmParams.y = bottomMargin;

        if(edgeMargin > 0 && gravity > -1
                && (gravity & Gravity.HORIZONTAL_GRAVITY_MASK) != Gravity.CENTER_HORIZONTAL)
            wmParams.x = edgeMargin;

        // Without this the taskbar keeps clear of a camera cutout on the side of the screen
        // (landscape), which leaves the clock short of the screen edge.
        if(extendIntoGestureArea && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            wmParams.layoutInDisplayCutoutMode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                    ? WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    : WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;

        U.allowReflection();
        try {
            Class<?> layoutParamsClass = Class.forName("android.view.WindowManager$LayoutParams");

            Field privateFlags = layoutParamsClass.getField("privateFlags");
            Field noAnim = layoutParamsClass.getField("PRIVATE_FLAG_NO_MOVE_ANIMATION");

            int privateFlagsValue = privateFlags.getInt(wmParams);
            int noAnimFlag = noAnim.getInt(wmParams);
            privateFlagsValue |= noAnimFlag;

            privateFlags.setInt(wmParams, privateFlagsValue);
        } catch (Exception ignored) {}

        return wmParams;
    }

    /** Position an on-screen desktop view within its Activity, without an overlay. */
    public FrameLayout.LayoutParams toFrameLayoutParams() {
        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(width, height);
        frameParams.gravity = gravity >= 0 ? gravity : Gravity.TOP | Gravity.START;
        if(bottomMargin > 0) {
            if((frameParams.gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.BOTTOM)
                frameParams.bottomMargin = bottomMargin;
            else
                frameParams.topMargin = bottomMargin;
        }

        int horizontal = frameParams.gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
        if(edgeMargin > 0 && horizontal != Gravity.CENTER_HORIZONTAL) {
            if(horizontal == Gravity.RIGHT || horizontal == Gravity.END)
                frameParams.rightMargin = edgeMargin;
            else
                frameParams.leftMargin = edgeMargin;
        }
        return frameParams;
    }

    public ViewParams updateWidth(int width) {
        return copyWith(width, height, bottomMargin);
    }

    public ViewParams updateHeight(int height) {
        return copyWith(width, height, bottomMargin);
    }

    public ViewParams updateBottomMargin(int bottomMargin) {
        return copyWith(width, height, bottomMargin);
    }

    private ViewParams copyWith(int width, int height, int bottomMargin) {
        ViewParams copy = new ViewParams(width, height, gravity, flags, bottomMargin);
        copy.extendIntoGestureArea = extendIntoGestureArea;
        copy.edgeMargin = edgeMargin;
        return copy;
    }
}
