# Contributing

Thanks for helping improve Text Selection Toolbar Customizer. The project is experimental, so clear reproduction steps and compatibility details are especially useful.

## Bug reports

Include:

- Device manufacturer/model and Android version
- Android SDK level and OEM build, if known
- LSPosed version and framework variant
- Target app and whether its standard or custom selection toolbar is used
- Exact steps to reproduce
- Expected and actual behavior
- Relevant filtered logs, with personal information removed

Do not attach full device dumps, account identifiers, private messages, or unrelated application data.

## Pull requests

- Keep changes focused and explain the behavior being changed.
- Avoid hard-coding device-specific paths or build fingerprints.
- Preserve compatibility with the project's declared Java and Android SDK targets unless the change explicitly updates them.
- Run `gradle assembleDebug` before submitting when possible.
- Describe any behavior that has not been tested on a physical device.

## Testing checklist

- Short text selection
- Long text selection and overflow menu
- At least one app using the standard Android selection toolbar
- App excluded by the package filter
- Invalid or empty rules
- Module disabled in LSPosed
