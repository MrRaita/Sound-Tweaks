# Text Selection Toolbar Customizer

An experimental LSPosed module for customizing Android's standard floating text-selection toolbar.

> **Status: prototype / not yet device-validated.** The project has not been confirmed to build or run successfully on a physical device. Treat it as an experimental development build, not a stable release.

## What it aims to do

- Prioritize matching text-selection actions.
- Request that matching actions stay in the main toolbar row.
- Request that matching actions appear in the overflow (three-dot) menu.
- Hide matching actions from the standard floating toolbar.
- Apply rules globally or only to selected apps.
- Prioritize DeepL by default as the initial use case.

The module does not patch `SystemUI.apk`, modify the framework on disk, or alter the DeepL APK. It hooks the framework floating-toolbar implementation inside app processes.

## Compatibility

- Android API 26+ is declared as the minimum; actual compatibility is not yet verified across Android versions or OEM builds.
- LSPosed with the legacy Xposed API is required.
- The initial investigation used a Nothing Phone (4a) running Android 16 / SDK 36. This is a development target, not a compatibility guarantee.
- Apps that provide their own custom text-selection UI may not use Android's standard floating toolbar and may not be affected.

## Build

### Requirements

- JDK 17 or newer
- Android SDK Platform 36
- Gradle 9.1 or newer compatible with Android Gradle Plugin 9.0.0

From the repository root:

```bash
gradle assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

A GitHub Actions workflow builds the debug APK on pushes and pull requests and uploads it as a workflow artifact.

## Install and test

1. Build the debug APK or download the artifact from a successful GitHub Actions run.
2. Install the APK on the test device.
3. Enable **Text Selection Toolbar Customizer** in LSPosed.
4. Include the apps in which you want the hook to run.
5. Open the module's settings and review the rules.
6. Force-stop and reopen a target app before testing changes.
7. Test both short and long selections, including the overflow menu.

Keep a way to disable the module in LSPosed if a target app crashes or its toolbar behaves unexpectedly.

## Rule syntax

Enter one rule per line. Matching is case-insensitive where applicable.

| Rule | Meaning |
| --- | --- |
| `package:com.deepl.mobiletranslator` | Match an action whose intent targets this package |
| `activity:com.deepl.mobiletranslator/.MiniTranslatorActivity` | Match an action targeting this activity |
| `title:DeepL` | Match an action whose visible title contains `DeepL` |
| `action:android.intent.action.PROCESS_TEXT` | Match an action with this intent action |
| `id:16908321` | Match an action by menu item ID |
| `DeepL` | Search the visible title for this text |

Lines beginning with `#` are ignored. The priority list is ordered: earlier matching rules are given higher priority. If an action matches both the “main row” and “overflow” lists, the overflow rule takes precedence.

### Settings overview

- **Priority order** — reorder matching actions.
- **Pin to main row** — request that matching actions be displayed in the main row.
- **Move to overflow** — request that matching actions be placed in the three-dot menu.
- **Hide actions** — omit matching actions from the standard toolbar.
- **Include apps** — optional package allowlist. Empty means all apps except those excluded.
- **Excluded apps** — packages that should not be affected.

## Known limitations

- `SHOW_AS_ACTION_ALWAYS` cannot create additional screen space. The framework may still be constrained by available width, labels, or OEM layout behavior.
- Reordering and visibility depend on the framework's internal implementation and may differ across Android releases.
- This prototype has not yet been verified on-device. Issues and pull requests are welcome, but reports should include Android version, device/OEM, LSPosed version, affected app, and reproducible steps. Avoid posting personal data or full device identifiers.
- A true second row inside the existing popup is not implemented. It requires coordinated changes to popup measurement, height, positioning, and touchable insets.

## Technical notes

See [`docs/TECHNICAL_NOTES.md`](docs/TECHNICAL_NOTES.md) for the initial framework investigation and the rationale for the chosen hook point.

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md). Please keep changes focused and test both short and long selections when possible.

## License

No license has been selected for this repository yet. Until a license is added, do not assume that the source is available for unrestricted reuse, redistribution, or modification.
