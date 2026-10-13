# Contributing to Andesk

Contributions to [coolchan03/Andesk](https://github.com/coolchan03/Andesk) are welcome. Please keep changes focused and avoid reintroducing the persistent-taskbar lifecycle inherited from upstream Taskbar.

## Session and compatibility requirements

Andesk's desktop session is owned by `DesktopLauncherActivity`. Starting other apps must not terminate that session. Closing or removing Andesk's desktop must stop its taskbar, menus, overlay services, and session notification. Boot, package upgrade, and stale preferences must not start a desktop without an explicit user launch.

Keep the existing Java namespace `com.farmerbb.taskbar` unless a separate migration is justified. The current Android application ID is `st.advisory.andesk`. Preserve Apache 2.0 notices and upstream Taskbar attribution when reusing code.

## Scope and style

- Submit focused pull requests with explanations of behavior and any risks.
- Reuse compatible upstream code where practical, while retaining Andesk's session ownership.
- Prefer simple Java or Kotlin implementations and avoid new dependencies without a clear reason.
- Preserve accessibility, overlay-permission onboarding, widget binding, and freeform-helper behavior.
- Update tests for changed behavior. Do not remove or skip tests to make CI pass.
- Maintain formatting with the existing Spotless and ktlint configuration; avoid unrelated formatting churn.

## Tests and CI

All PRs should pass the workflow in [`.github/workflows/gradle.yml`](.github/workflows/gradle.yml), including:

```text
./gradlew spotlessCheck
./gradlew assembleFreeDebug
./gradlew testFreeDebug
./gradlew lintFreeDebug
```

GitHub Actions uses JDK 21 and a temporary Android build environment. You do **not** need to install an Android SDK on your personal computer to contribute or to use the CI-generated APK artifact. If you build locally, you must supply your own compatible Android SDK; local installation is optional.

Do not merge a known-red PR. If a toolchain or testing-library upgrade breaks Robolectric/PowerMock tests, report the incompatibility instead of weakening the test suite.

### Translations

Translate against the source strings in `app/src/main/res/values/strings.xml` and relevant resource qualifiers. Preserve shell commands, format placeholders, and XML escaping. If adding a locale, update `androidResources.localeFilters` in `app/build.gradle` where necessary, and explain the change in your PR.

### Device-specific behavior

CI cannot verify device-specific Android edge-to-edge layout, background activity launch restrictions, freeform windows, widgets, navigation gestures, and overlay permissions. State clearly what you tested on a physical device and what still needs testing.

See the [README device checklist](README.md#device-verification-checklist) and [privacy policy](Privacy-Policy.md).

## Licensing

By contributing, you agree to make your contribution available under this project's Apache License 2.0 terms. Retain upstream copyright and licensing notices when copying or modifying code derived from [farmerbb/Taskbar](https://github.com/farmerbb/Taskbar).
