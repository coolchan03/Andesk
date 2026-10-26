# Andesk changelog

## 0.1.2 - Desktop taskbar and widget-host corrections (October 15, 2026)

- Render the taskbar, Start menu and dashboard inside the desktop Activity instead of depending on an external overlay service to draw the desktop.
- Explicitly expand the desktop taskbar at startup (its legacy toggle returned early and left the app area hidden).
- Remove desktop overlay views when switching to another Android app, preserving the open desktop task without a lingering taskbar or background notification.
- Stop launching the upstream freeform workaround automatically during desktop startup.
- Fix a NullPointerException that prevented the desktop taskbar from being drawn with the default (horizontal) taskbar layouts.
- Keep the taskbar and Start menu above desktop shortcuts and widgets so they remain visible and tappable.
- Do not request the system-only BIND_APPWIDGET permission; widgets continue to use Android's user-approved binding screen.
- Redraw the Activity-hosted taskbar in place after appearance changes instead of starting legacy overlay services.
- With gesture navigation, extend the desktop taskbar to the bottom edge; space is only reserved for tappable 3-button navigation bars.
- Hide the Start menu when an app is dragged out of it so it can be dropped onto the desktop.
- Add a compact month calendar to the Control Center (tap the date; long-press opens the calendar app).
- Freeform mode: the taskbar floats above app windows while the desktop is open, so several freeform apps can be opened and kept on screen. When you leave the desktop it shrinks to just the Start button, and it is removed when the desktop session ends (Exit, Back, Alt+F4 or swiping Andesk from Recents). Requires "Display over other apps"; without it the in-desktop taskbar is used.
- Widgets whose setup screen is optional (such as Google's At a Glance) are added without opening it, and a setup screen that cannot be opened no longer cancels an already-approved widget.
- Long-pressing a widget opens its menu again (Move / resize, Remove widget); before, it went straight to move mode and widgets could not be removed. The long-press no longer also taps the widget.
- Fix widgets showing Android's "Couldn't add widget" error (e.g. Google's At a Glance): widget layouts are now inflated without AppCompat's view substitutions, which RemoteViews reject.
- Keep the clock and system tray pinned to the far end of the desktop taskbar: the app area now fills the remaining width instead of relying on pixel estimates that the battery percentage, screen insets and Windows-style start button threw off.
- Windows 7 style: draw the glass highlight only along the taskbar's inner edge instead of a border around it, which left a stray line along the bottom of the screen.
- The floating (freeform) taskbar and Start menu extend over the gesture-navigation strip at the bottom of the screen instead of floating above it; tappable 3-button navigation bars are still avoided.
- Redesigned Control Center: a translucent, rounded panel in the taskbar's colors with icon tiles (Wi-Fi and Bluetooth highlight when on), a larger clock and a full date header.
- The Calendar and Clock apps opened from the taskbar or Control Center launch like other taskbar apps, i.e. in a freeform window when freeform mode is on.
- While the floating (freeform) taskbar is running, Andesk shows an ongoing notification; tapping it closes the desktop taskbar. This also keeps the floating Start button from being stopped by Android after you leave the desktop.
- New desktop items start where you pressed: long-press (or right-click) an empty spot and choose Add app, file, folder, emoji or widget. They no longer all land in the top-left corner.
- Files, folders and emoji shortcuts can now be dragged and swapped like app icons (they could not be moved before); press and release for the menu. Files and folders look like app icons.
- Add emoji to the desktop, and force a custom icon (an emoji or a picked image) onto any app, file or folder from its menu ("Change icon"), with a reset option.
- Right-click now works on widgets (menu), on empty parts of the taskbar (taskbar menu) and on desktop items; long-pressing empty taskbar space opens the taskbar menu.
- Taskbar Settings, Calendar and Clock open as windows (like taskbar apps) instead of full screen, no longer pushing your open freeform windows behind them.
- Tapping the desktop no longer pulls the full-screen desktop in front of your freeform windows while the floating taskbar is running (the desktop window is non-focusable in that mode).
- The date/time at the end of the taskbar fits its text and sits flush against the edge instead of centering inside a fixed-width box.
- Reorder pinned taskbar apps from their menu (Move left/right, or up/down on a vertical taskbar).
- Four new taskbar presets: macOS-like floating dock, ChromeOS-like shelf, GNOME/Ubuntu-like side dock (moves the taskbar to the left edge) and KDE Plasma-like panel. Explicit color and transparency settings still override the preset colors.
- Settings: show or hide the battery percentage (it turns red at 15% or less when not charging), and choose the taskbar clock's 12/24-hour format, whether to show the date, and whether to show seconds.
- Lock desktop layout (desktop menu): icons and widgets can no longer be dragged, resized or rearranged by accident; long-press still opens their menu.
- Desktop icon size (Settings > Appearance): small, medium or large, with the grid adjusting to fit.
- Control Center: volume and brightness sliders. Brightness asks once for the "modify system settings" permission.
- Floating (freeform) taskbar: a thin "Show desktop" strip at its end brings the desktop forward.
- Add regression tests for desktop startup, leaving/returning, session shutdown, appearance restarts, z-order and widget permissions.
- Bind widgets using the provider's Android user profile and show the specific error stage when widget setup fails.

## 0.1.1 - Widget and taskbar startup fixes (October 15, 2026)

- Return from overlay permission Settings to the owning Andesk activity rather than launching a separate task.
- Retry a taskbar service that stops immediately after startup once, and explain persistent failures.
- Preserve pending widget approval/configuration IDs across activity recreation.
- Handle broken widget providers without crashing the desktop or losing other widgets.
- Improve widget binding error handling and diagnostic logs.

## 0.1.0 - Initial Andesk release (October 12, 2026)

Andesk is a session-owned Android desktop launcher developed from Braden Farmer's Taskbar.

- Open Andesk to start a desktop session with a taskbar, Start menu, desktop shortcuts, and widgets.
- Close or remove the desktop session to stop its overlays and services; opening other Android apps does not end the session.
- Add and arrange application shortcuts on the desktop, including drag-and-drop from the Start menu.
- Add, move, resize, and persist Android widgets using the platform's widget binding flow.
- Offer Classic, Windows 7, Windows 10, and Windows 11 inspired taskbar styles and taskbar size options.
- Constrain ordinary freeform app windows to the usable display while preserving the invisible offscreen freeform helper.
- Remove legacy Tasker, Kustom, paid/free settings transfer, Android-x86 and library integration.
- Replace the upstream installation identity with `st.advisory.andesk`; this is a separate Android installation from the previous package, so application data does not migrate automatically.

### Attribution

Andesk is derived from Braden Farmer's Taskbar and retains upstream copyright notices and Apache License 2.0 attribution. Upstream Taskbar history is available at https://github.com/farmerbb/Taskbar.
