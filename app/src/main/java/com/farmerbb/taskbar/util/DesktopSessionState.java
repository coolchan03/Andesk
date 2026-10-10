package com.farmerbb.taskbar.util;

import android.content.Context;
import android.content.SharedPreferences;

import static com.farmerbb.taskbar.util.Constants.PREF_DESKTOP_SESSION_ACTIVE;
import static com.farmerbb.taskbar.util.Constants.PREF_TASKBAR_ACTIVE;

/** Owns the persistent desktop-session and overlay-visibility flags. */
public final class DesktopSessionState {
    private DesktopSessionState() {}

    public static void foreground(Context context) {
        U.getSharedPreferences(context).edit()
                .putBoolean(PREF_DESKTOP_SESSION_ACTIVE, true)
                .putBoolean(PREF_TASKBAR_ACTIVE, true)
                .apply();
    }

    public static void background(Context context) {
        SharedPreferences pref = U.getSharedPreferences(context);
        if(pref.getBoolean(PREF_DESKTOP_SESSION_ACTIVE, false))
            pref.edit().putBoolean(PREF_TASKBAR_ACTIVE, false).apply();
    }

    public static void close(Context context) {
        U.getSharedPreferences(context).edit()
                .putBoolean(PREF_DESKTOP_SESSION_ACTIVE, false)
                .putBoolean(PREF_TASKBAR_ACTIVE, false)
                .apply();
    }
}
