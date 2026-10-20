/* Copyright 2016 Braden Farmer
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

package com.farmerbb.taskbar.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.service.quicksettings.TileService;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import com.farmerbb.taskbar.activity.MainActivity;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.DependencyUtils;
import com.farmerbb.taskbar.util.DesktopSessionState;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class NotificationService extends Service {

    private boolean receiversRegistered;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(!DesktopSessionState.isActive(this)) {
            DesktopSessionState.close(this);
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if(TaskbarService.isDesktopLauncherTask(rootIntent)) {
            Intent quitIntent = new Intent(ACTION_QUIT);
            quitIntent.setPackage(getPackageName());
            quitIntent.putExtra(EXTRA_DESKTOP_SESSION, true);
            sendBroadcast(quitIntent);
        }

        super.onTaskRemoved(rootIntent);
    }

    BroadcastReceiver userForegroundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(DesktopSessionState.isActive(context)) {
                startService(new Intent(context, TaskbarService.class));
                startService(new Intent(context, StartMenuService.class));
            } else {
                stopSelf();
            }
        }
    };

    BroadcastReceiver userBackgroundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopService(new Intent(context, TaskbarService.class));
            stopService(new Intent(context, StartMenuService.class));

            U.clearCaches(context);
        }
    };

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences pref = U.getSharedPreferences(this);
        if(!DesktopSessionState.isActive(this)) {
            DesktopSessionState.close(this);
            stopSelf();
            return;
        }

        if(pref.getBoolean(PREF_TASKBAR_ACTIVE, false)) {
            if(U.canDrawOverlays(this)) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                Intent receiverIntent2 = new Intent(ACTION_QUIT);
                receiverIntent2.setPackage(getPackageName());
                receiverIntent2.putExtra(EXTRA_DESKTOP_SESSION, true);

                PendingIntent settingsIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                PendingIntent receiverPendingIntent2 = PendingIntent.getBroadcast(this, 0, receiverIntent2, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                // The floating desktop taskbar stays over other apps until the session ends;
                // tapping this notification is the quick way to close it.
                PendingIntent contentIntent = receiverPendingIntent2;

                String id = "taskbar_notification_channel";

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    CharSequence name = getString(R.string.tb_app_name);
                    int importance = NotificationManager.IMPORTANCE_MIN;

                    NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.createNotificationChannel(new NotificationChannel(id, name, importance));
                }

                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, id)
                        .setSmallIcon(pref.getString(PREF_START_BUTTON_IMAGE,
                                U.getDefaultStartButtonImage(this))
                                .equals(PREF_START_BUTTON_IMAGE_APP_LOGO)
                                ? R.drawable.tb_system
                                : R.drawable.tb_allapps)
                        .setContentIntent(contentIntent)
                        .setContentTitle(getString(R.string.tb_taskbar_is_active))
                        .setContentText(getString(R.string.tb_tap_to_close_desktop))
                        .setColor(ContextCompat.getColor(this, R.color.tb_colorPrimary))
                        .setPriority(Notification.PRIORITY_MIN)
                        .setShowWhen(false)
                        .setOngoing(true);

                if(U.canEnableFreeform(this) && !U.isChromeOs(this) && !pref.getBoolean(PREF_DESKTOP_MODE, false)) {
                    String freeformLabel = getString(pref.getBoolean(PREF_FREEFORM_HACK, false) ? R.string.tb_freeform_off : R.string.tb_freeform_on);

                    Intent freeformIntent = new Intent(ACTION_TOGGLE_FREEFORM_MODE);
                    freeformIntent.setPackage(getPackageName());

                    PendingIntent freeformPendingIntent = PendingIntent.getBroadcast(this, 0, freeformIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                    mBuilder.addAction(0, freeformLabel, freeformPendingIntent);

                }

                mBuilder.addAction(0, getString(R.string.tb_settings), settingsIntent);
                mBuilder.addAction(0, getString(R.string.tb_action_quit), receiverPendingIntent2);

                startForeground(8675309, mBuilder.build());

                U.sendBroadcast(this, ACTION_UPDATE_SWITCH);

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    TileService.requestListeningState(this, new ComponentName(getPackageName(), QuickSettingsTileService.class.getName()));

                registerReceiver(userForegroundReceiver, new IntentFilter(Intent.ACTION_USER_FOREGROUND));
                registerReceiver(userBackgroundReceiver, new IntentFilter(Intent.ACTION_USER_BACKGROUND));
                receiversRegistered = true;
            } else {
                pref.edit().putBoolean(PREF_TASKBAR_ACTIVE, false).apply();

                stopSelf();
            }
        } else stopSelf();
    }

    @Override
    public void onDestroy() {
        SharedPreferences pref = U.getSharedPreferences(this);
        if(pref.getBoolean(PREF_IS_RESTARTING, false))
            pref.edit().remove(PREF_IS_RESTARTING).apply();
        else {
            U.sendBroadcast(this, ACTION_UPDATE_SWITCH);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                TileService.requestListeningState(this, new ComponentName(getPackageName(), QuickSettingsTileService.class.getName()));

            if(!U.launcherIsDefault(this) || U.isChromeOs(this))
                U.stopFreeformHack(this);
        }

        super.onDestroy();

        if(receiversRegistered) {
            unregisterReceiver(userForegroundReceiver);
            unregisterReceiver(userBackgroundReceiver);
        }
    }
}
