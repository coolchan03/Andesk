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

/**
 * The size of the system bars as measured by the desktop. Windows that float over other apps
 * (the freeform taskbar and Start menu) are drawn below the status and navigation bars, so
 * they must offset themselves by these amounts to avoid being covered.
 *
 * Values are in pixels: the status bar on top, the navigation buttons (or large-screen system
 * taskbar) on the bottom and the sides. A gesture handle is not counted because it is not
 * tappable and the taskbar may extend under it.
 */
public final class SystemBarInsets {
    private static int left;
    private static int top;
    private static int right;
    private static int bottom;

    private SystemBarInsets() {}

    /** @return true if the values changed */
    public static synchronized boolean update(int newLeft, int newTop, int newRight, int newBottom) {
        boolean changed = left != newLeft || top != newTop || right != newRight || bottom != newBottom;
        left = newLeft;
        top = newTop;
        right = newRight;
        bottom = newBottom;
        return changed;
    }

    public static synchronized int getLeft() {
        return left;
    }

    public static synchronized int getTop() {
        return top;
    }

    public static synchronized int getRight() {
        return right;
    }

    public static synchronized int getBottom() {
        return bottom;
    }

    public static synchronized void reset() {
        update(0, 0, 0, 0);
    }
}
