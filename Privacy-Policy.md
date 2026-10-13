# Andesk privacy policy

Andesk is an open-source, session-based Android desktop launcher derived from Braden Farmer's Taskbar. Its source code is available at [coolchan03/Andesk](https://github.com/coolchan03/Andesk) under the [Apache License 2.0](https://github.com/coolchan03/Andesk/blob/master/LICENSE). Original Taskbar attribution and licensing are retained.

## Connectivity and data

The Andesk application does **not request the Android `INTERNET` permission**, so Andesk itself cannot make ordinary internet connections. It can view system network availability through permissions such as `ACCESS_NETWORK_STATE` and `ACCESS_WIFI_STATE`, but these are not internet access permissions.

Andesk displays and organizes locally installed apps, desktop shortcuts, widgets, and taskbar controls. Their configuration is stored on the device. Launching a third-party app from Andesk does not change that app's own privacy practices.

Andesk does not intentionally collect analytics or send app usage, shortcut layouts, or widget configurations to an Andesk server.

## Permissions and system integrations

Android controls permissions and special-access grants. Depending on Android version and enabled features, Andesk may request:

- **Display over other apps (`SYSTEM_ALERT_WINDOW`):** show the taskbar, menus, and other overlays during an explicitly opened desktop session.
- **Notification permission (`POST_NOTIFICATIONS`) and foreground service:** show the desktop-session notification while running.
- **Usage access (`PACKAGE_USAGE_STATS`):** support running/recent app information and taskbar behavior.
- **Notification listener:** optionally display notification counts in the taskbar.
- **Accessibility service:** optionally support system actions through Android's accessibility framework.
- **Device app visibility (`QUERY_ALL_PACKAGES`):** list available apps for the Start menu and shortcuts.
- **Wallpaper hints (`SET_WALLPAPER_HINTS`):** adjust wallpaper layout.
- **Settings permissions (`WRITE_SETTINGS`, `WRITE_SECURE_SETTINGS`):** enable compatible device-specific settings where the system grants access.
- **Wi-Fi/Bluetooth and network state permissions:** show connectivity state and open or use supported system controls; legacy Wi-Fi state changes are restricted to supported Android versions.
- **Package deletion request (`REQUEST_DELETE_PACKAGES`):** initiate user-confirmed app uninstall flows.
- **Boot notification (`RECEIVE_BOOT_COMPLETED`):** clear stale session state and initialize compatibility preferences; **not** to automatically launch the desktop.
- **Optional Shizuku integration:** for user-initiated advanced settings where Shizuku is available and authorized.

Some integrations depend on device support and may not be available on all Android versions. Permissions do not themselves grant Andesk unrestricted access; Android permission dialogs and system settings control access.

## Backups

Android automatic application-data backup is disabled for Andesk (`android:allowBackup="false"`). This avoids restoring old widget IDs, window positions, and pinned components on a different device.

Andesk retains a **user-initiated manual backup and restore** option. A manually exported backup may contain application settings, pinned shortcuts, and configuration information. Store, share, and delete exported files according to your own privacy needs; external file locations may be accessible to applications with appropriate permission.

## Session lifecycle

Andesk is designed to run desktop overlays only during an explicitly opened desktop session. Closing the session should remove its taskbar, menus, and overlay services. After reboot or force-stop, opening Andesk again is required to start a new desktop session.

## Source and questions

For a permission-by-permission explanation and Android Settings instructions, see the [Andesk README](README.md#permissions-and-special-access).

Repository: [coolchan03/Andesk](https://github.com/coolchan03/Andesk). Report issues through the [Andesk issue tracker](https://github.com/coolchan03/Andesk/issues). Contributions: [CONTRIBUTING.md](CONTRIBUTING.md). Upstream project: [farmerbb/Taskbar](https://github.com/farmerbb/Taskbar).
