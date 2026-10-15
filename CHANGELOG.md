# Andesk changelog

## Unreleased

- Fix the desktop taskbar never appearing: the horizontal (default) taskbar crashed while hiding the legacy collapse button, which silently stopped the taskbar service and left only the wallpaper.
- Make desktop widget setup more robust: list widgets from work profiles, fall back to the system widget picker when the bind confirmation screen is missing, skip optional configuration screens, allow widget configuration screens to open on Android 14+, and log the reason when a widget cannot be added.
- Respect the Appearance "Full length" setting inside desktop sessions instead of always forcing a full-width taskbar.
- Add "Customize taskbar" to the Start button's context menu, opening the taskbar Appearance settings directly.
- New Appearance options: taskbar icon spacing, rounded taskbar corners, a toggle for running-app indicators, and app icon and label sizes for the Start menu and desktop.

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
