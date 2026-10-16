# Andesk changelog

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
