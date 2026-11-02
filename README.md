# Sound Tweaks

🇬🇧 English · 🇹🇷 [Türkçe](README.tr.md)

Sound Tweaks is an LSPosed module that changes Android system sounds (keyboard, touch, screenshot, lock screen, charging, dock).

No system files are modified. The chosen sounds are kept in a separate store and only the sound path is redirected inside the relevant system processes; when the module is disabled or the sounds are reset, the system returns to its own defaults.

## Features

- Pick sounds inside the app instead of by file path: from system sounds, from your own file, or by muting.
- A playable library that scans and lists the system sound directories.
- Per-sound normalization (global, off, or a custom target level); peaks are limited with a soft limiter.
- Show and change the state of the system switches for touch, lock screen and charging sounds (including the ringer mode and volume levels).
- Optional "force play" mode: lock sounds are played on the media stream so they are audible in silent/vibrate mode.
- An option to turn off the loud-volume (hearing protection) warning shown with headphones; no hook is added to the system, a tiny script that runs at boot is used instead.
- A diagnostic report (file permissions, settings, hook status, SELinux denials).

## Supported sounds

| Group | Sounds | Method |
|---|---|---|
| Keyboard | Key, space, delete, enter, invalid key | `system_server` hook |
| Touch and screen | Touch click, screenshot | `system_server` hook, SystemUI hook |
| Lock screen | Lock, unlock, trusted device | `Settings.Global` path (default), optional SystemUI hook |
| Charging and battery | Low battery, charging, wireless charging | `Settings.Global` path |
| Dock | Desk/car dock, undock | `Settings.Global` path |

Camera, video recording, NFC and reverse-charging sounds are played in processes other than `system_server` and SystemUI, so they are out of scope (see [Known limitations](#known-limitations)).

## Requirements

- Android 9 (API 28) or newer
- Root (KernelSU, Magisk or APatch); no extra module is needed. For the headphone warning option, the root manager must run the scripts in `/data/adb/post-fs-data.d` (all three do)
- An LSPosed framework that supports libxposed API 101 (for example Vector)

Development and test environment: Nothing Phone (4a), Nothing OS 4.1, Android 16, KernelSU, Vector 2.2.

## Installation

1. Download the APK from the repository's **Releases** page and install it.
2. Open the app and grant Sound Tweaks superuser access in your root manager.
3. Enable Sound Tweaks in the Vector (LSPosed) manager and add **System Framework** and **System UI** to its scope.
4. Reboot the device.
5. The Vector card on the app's **Settings** tab should show that the hooks are active.

## Usage

- **Sounds:** Tapping a sound opens the options: choose from system sounds, choose your own file, mute, or reset to default. Files are decoded and written to the store as WAV.
- **Library:** The system sounds that were found can be filtered, searched with the search icon, and played.
- **Advanced:** System sound switches, audio processing (global and per-sound normalization), force-playing lock sounds, and turning off the headphone warning.
- **Settings:** Status cards (Root and Vector module), theme and colors, bottom bar layout, tools (SystemUI refresh, diagnostic report).

## How it works

### Sound store

The chosen sounds are written under `/data/system/soundtweaks` through root and labeled with the world-readable `system_file` SELinux context.

- `override/<StockName>.<extension>`: files the hooks redirect to
- `files/<settingKey>.<extension>`: files that `Settings.Global` paths point to

The context is re-applied every time the app opens and every time a sound is written. If a system update rewrites the file contexts, opening the app once is enough.

### Hooks

Written against libxposed API 101 (`minApiVersion = targetApiVersion = 101`). The entry class is `com.mrraita.soundtwks.hook.SoundTweaksModule`; the metadata lives under `app/src/main/resources/META-INF/xposed/`.

| Process | Class / method | Purpose |
|---|---|---|
| `system_server` | `SoundEffectsHelper.getResourceFilePath` | Redirects keyboard and touch sound paths |
| SystemUI | `ScreenshotSoundProviderImpl.getScreenshotSound` | Replaces the screenshot sound |
| SystemUI | `KeyguardViewMediator.playSounds`, `playTrustedSound` | Plays lock sounds, only in "force play" mode |

On the Vector card, the "Framework" row shows Vector's own API version and the "Module API" row shows the API Sound Tweaks was built against (101); it is normal for them to differ. Every hook sits in its own `try/catch`; on failure the stock behavior continues.

Lock sounds play through the stock mechanism by default: the system loads the path in the `lock_sound`, `unlock_sound` and `trusted_sound` settings once when SystemUI starts. While "force play" mode is off, no hook is added to SystemUI.

### Headphone volume warning

After a reboot, Android shows a warning when headphone volume is raised above a certain level. Three system properties read at boot control that warning. When the option is turned on, Sound Tweaks:

- writes the `/data/adb/soundtweaks/hp_warning.sh` script, which sets `audio.safemedia.bypass=true`, `audio.safemedia.force=false` and `audio.safemedia.csd.force=false` (with `resetprop`, or `setprop` if it is missing);
- puts a single line that only calls the script into `/data/adb/post-fs-data.d/soundtweaks_hp_warning.sh`, so that it runs before the system starts;
- also sets the properties once right away, so there is no need to wait for a reboot.

When the option is turned off, both files are deleted and the warning returns from the next boot. No hook is added to any system process. Turn the option off before uninstalling the app; otherwise the script stays under `/data/adb`. To remove it by hand:

```sh
su -c "rm -rf /data/adb/soundtweaks /data/adb/post-fs-data.d/soundtweaks_hp_warning.sh"
```

### Audio processing

Files are decoded with `MediaExtractor` and `MediaCodec` and written as 16-bit PCM WAV. The normalization metric is the mean (RMS) level of the whole file (the same as ffmpeg `volumedetect` "mean_volume"); if the gain needed to reach the target pushes peaks over full scale, a soft limiter is applied.

## Resource usage

- The app has no service, boot receiver or background job.
- The ringer mode and volume listeners are only active while the app is on screen.
- Root queries are collected into a single `su` call at startup; the library is scanned when first needed.
- The hooks are event-based; there is no polling or always-running thread. The override directory is kept in memory and only the directory's modification time is queried.
- Only the methods of the relevant classes (`SoundEffectsHelper` and `KeyguardViewMediator`) are removed from compiled code (deoptimized).
- The headphone warning script runs once at boot, sets three properties and exits.
- The verbose event log exists only in pre-releases and debug builds and is off by default; in release builds that code never runs.

## Troubleshooting

- **Sounds do not play:** In the "System sound switches" card on the Advanced tab, touch, lock screen and charging sounds must be on and the ringer mode must be normal. Lock and touch sounds do not play while the phone is silent or vibrating; "force play" mode bypasses this for lock sounds.
- **The lock sound did not change:** Lock sounds are loaded when SystemUI starts, so SystemUI must be refreshed after a change (the app asks).
- **The Vector card is red:** The module is not enabled or the scope is missing; check the scope and reboot the device.
- **Diagnostics:** Settings → Tools → Generate diagnostic report. In pre-releases and debug builds, while "Verbose event log" is on, the last SystemUI event is also written to the report.
- **The headphone warning still appears:** Your root manager may not run `post-fs-data.d` scripts, or the properties that the warning reads at boot may be overridden by the vendor; turn the option on, reboot, and check the diagnostic report.

## Building

Requirement: JDK 17 or newer (CI uses JDK 21). The Gradle wrapper jar is not in the repository because it is a binary file; the version and the SHA-256 checksum are read from `gradle/wrapper/gradle-wrapper.properties`, and CI verifies the distribution against that checksum.

```sh
gradle wrapper --gradle-version <version in gradle-wrapper.properties>   # once
./gradlew assembleRelease
```

`versionName` is in `app/build.gradle.kts`; `versionCode` comes from the CI run number. If no signing key is provided in a local build, the release APK is signed with the debug key. The verbose event log is included in the APK only in the CI pre-build (`SOUNDTWEAKS_VERBOSE=true`) and in debug builds; it is absent from release builds.

## Project structure

```
app/src/main/
├── kotlin/com/mrraita/soundtwks/
│   ├── MainActivity.kt
│   ├── core/    # catalog, scanner, converter, root, store
│   ├── hook/    # libxposed entry class and hooks
│   └── ui/      # Compose screens, theme, palettes, ViewModel
├── resources/META-INF/xposed/   # module.prop, scope.list, java_init.list
└── res/
```

## Known limitations

- Camera, video recording, NFC, reverse-charging and call notification sounds are not played in processes within Sound Tweaks' scope. Changing them would need the relevant apps to be added to the Vector scope and the sound files to be delivered into those app processes (Vector remote file support); this is not implemented yet.
- The shutter sound that the old camera API plays in native code inside `cameraserver` cannot be hooked.
- The headphone warning option depends on the root manager running the `/data/adb/post-fs-data.d` scripts.
- The system sound library searches `/<partition>/media/audio` on every partition at the root; vendor-specific sound directories outside that pattern are not listed. The "sound directories" section of the diagnostic report shows what exists on a device.
- The hook points rely on AOSP class names; on vendor skins where the class names differ, the related hook does not work and shows up as an error on the status card.

## License

Sound Tweaks is licensed under the [GNU General Public License version 3](LICENSE) (GPL-3.0-or-later). You can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
