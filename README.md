# SoundMod

🇬🇧 English · 🇹🇷 [Türkçe](README.tr.md)

An LSPosed/Vector module and management app that changes Android system sounds (keyboard, touch, screenshot,
lock screen, charging, dock). The UI is written with Material 3 Expressive.

No system files are modified. The chosen sounds are kept in a separate store and only the sound path is redirected
inside the relevant system processes; when the module is disabled or the sounds are reset, the system returns to its
own defaults.

## Features

- Pick sounds inside the app instead of by file path: from system sounds, from your own file, or by muting.
- A playable library that scans and lists the system sound directories (play/pause, a wavy progress bar tied to the
  duration).
- Per-sound normalization (global, off, or a custom target level); peaks are limited with a soft limiter.
- Show and change the state of the system switches for touch, lock screen and charging sounds (including the ringer
  mode and volume levels).
- Optional "force play" mode: lock sounds are played on the media stream so they are audible in silent/vibrate mode.
- A SystemUI restart button (it shows a warning on first use, which can be dismissed).
- Theme: system, light, dark; a separate OLED (pure black) switch makes the dark theme black (also when the system
  switches to dark). Material You colors or 12 ready-made palettes.
- A floating bottom navigation bar in two widths (wide: the selected tab expands into a pill with icon and label;
  compact: icons only with a sliding highlight), tab reordering by drag and drop, and hideable tabs.
- The top header collapses when scrolling down and the tab name replaces the app name; the header overlays the list so
  the list shows behind its corners. In the Library tab the search row and filter chips are attached to the header and
  slide up with it.
- An option to turn off the loud-volume (hearing protection) warning shown with headphones; no hook is added to the
  system, a tiny script that runs at boot is used.
- A diagnostic report (file permissions, settings, hook status, SELinux denials).

## Supported sounds

| Group | Sounds | Method |
|---|---|---|
| Keyboard | Key, space, delete, enter, invalid key | `system_server` hook |
| Touch and screen | Touch click, screenshot | `system_server` hook, SystemUI hook |
| Lock screen | Lock, unlock, trusted device | `Settings.Global` path (default), optional SystemUI hook |
| Charging and battery | Low battery, charging, wireless charging | `Settings.Global` path |
| Dock | Desk/car dock, undock | `Settings.Global` path |

Camera, video recording, NFC and reverse-charging sounds are played in processes other than `system_server` and
SystemUI, so they are out of scope (see [Known limitations](#known-limitations)).

## Requirements

- Android 9 (API 28) or newer
- Root (KernelSU, Magisk or APatch); no extra module is needed. For the headphone warning option the root manager
  must run the `/data/adb/post-fs-data.d` directory (all three do)
- Vector (LSPosed) 2.1 or newer, with libxposed API 101 support

Development and test environment: Nothing Phone (4a), Nothing OS 4.1, Android 16, KernelSU, Vector 2.2.

## Installation

1. Download the APK from the repository's **Releases** page (or from the **Actions** output) and install it.
2. Open the app and grant root access.
3. Enable SoundMod in the Vector manager. **System Framework** and **System UI** must be selected in its scope.
4. Reboot the device.
5. In the app, the Vector card on the **Settings** tab should show that the hooks are active.

## Usage

- **Sounds:** Tapping a sound opens the options: choose from system sounds, choose your own file, mute, or reset to
  default. Files are decoded and written to the store as WAV.
- **Library:** The system sounds that were found can be filtered, searched with the search icon, and played.
- **Advanced:** System sound switches, audio processing (global and per-sound normalization), force-play lock sounds,
  and turning off the headphone warning.
- **Settings:** Status cards (Root and Vector module), theme and colors, bottom bar layout, tools (SystemUI refresh,
  diagnostic report).

## How it works

### Sound store

The chosen sounds are written under `/data/system/soundmod` through root and labeled with the world-readable
`system_file` SELinux context. Directories are mode 755 and files are mode 644 (never executable).

- `override/<StockName>.<extension>`: files the hooks redirect to
- `files/<settingKey>.<extension>`: files that `Settings.Global` paths point to

The context is re-applied every time the app opens and every time a sound is written. If a system update rewrites the
file contexts, opening the app once is enough. The store is only treated as ready when preparing it (directories,
ownership, mode, SELinux context) actually succeeded, and it is retried otherwise.

### Interface

The top header has two states (wide/narrow); once the scroll threshold is passed it collapses with a springy
transition, its background shrinks with the height, and it overlays the list. No effect is applied to list rows while
scrolling; the rows only appear one after another when the list first opens. With "Reduce animations" on, transitions
are simplified.

### Hooks

Written against libxposed API 101 (`minApiVersion = targetApiVersion = 101`). The entry class is
`dev.soundmod.nothing.hook.SoundModule`; the metadata lives under `app/src/main/resources/META-INF/xposed/`.

| Process | Class / method | Purpose |
|---|---|---|
| `system_server` | `SoundEffectsHelper.getResourceFilePath` | Redirects keyboard and touch sound paths |
| SystemUI | `ScreenshotSoundProviderImpl.getScreenshotSound` | Replaces the screenshot sound |
| SystemUI | `KeyguardViewMediator.playSounds`, `playTrustedSound` | Plays lock sounds, only in "force play" mode |

On the Vector card the "Framework" row shows Vector's own API version and the "Module API" row shows the API
SoundMod was built against (101); it is normal for them to differ. Every hook sits in its own `try/catch`; on failure
the stock behavior continues. Lock sounds play through the stock mechanism by default: the system loads the path in
the `lock_sound`, `unlock_sound` and `trusted_sound` settings once when SystemUI starts. While "force play" mode is off,
no hook is added to SystemUI.

### Headphone volume warning

After a reboot, Android shows a warning when headphone volume is raised above a certain level. Three system
properties read at boot control that warning. When the option is turned on, SoundMod:

- writes the `/data/adb/soundmod/hp_warning.sh` script, which sets `audio.safemedia.bypass=true`,
  `audio.safemedia.force=false` and `audio.safemedia.csd.force=false` (with `resetprop`, or `setprop` if missing);
- puts a single line that only calls the script into `/data/adb/post-fs-data.d/soundmod_hp_warning.sh`, so that it
  runs before the system starts;
- also sets the properties once right away, so there is no need to wait for a reboot.

When the option is turned off both files are deleted and the warning returns from the next boot. No hook is added to
any system process. Turn the option off before uninstalling the app; otherwise the script stays under `/data/adb`.
To remove it by hand:

```sh
su -c "rm -rf /data/adb/soundmod /data/adb/post-fs-data.d/soundmod_hp_warning.sh"
```

Note that listening at high volume for a long time can damage your hearing; this option is deliberate and user-enabled.

### Audio processing

Files are decoded with `MediaExtractor` and `MediaCodec` and written as 16-bit PCM WAV. The normalization metric is
the mean (RMS) level of the whole file (the same as ffmpeg `volumedetect` "mean_volume"); if the gain needed to reach
the target pushes peaks over full scale, a soft limiter is applied. Decoding has a 30-second timeout, and the
decoder is always released even if preparing it fails.

## Resource usage

- The app has no service, boot receiver or background job.
- The ringer mode and volume listeners are only active while the app is on screen.
- Root queries are collected into a single `su` call at startup; the library is scanned when first needed. Root
  commands have a timeout and a capped output size.
- The hooks are event-based; there is no polling or always-running thread. The override directory is kept in memory
  and only the directory's modification time is queried.
- Only the methods in the sound flow are removed from compiled code (deoptimized).
- The headphone warning script runs once at boot, sets three properties and exits.
- The verbose event log exists only in pre-releases and is off by default; in stable releases that code never runs.

## Troubleshooting

- **Sounds do not play:** In the "System sound switches" card on the Advanced tab, touch, lock screen and charging
  sounds must be on and the ringer mode must be normal. Lock and touch sounds do not play while the phone is silent or
  vibrating; "force play" mode bypasses this for lock sounds.
- **The lock sound did not change:** Lock sounds are loaded when SystemUI starts, so SystemUI must be refreshed after
  a change (the app asks).
- **The Vector card is red:** The module is not enabled or the scope is missing; check the scope and reboot.
- **Diagnostics:** Settings → Tools → Generate diagnostic report. In pre-releases, while "Verbose event log" is on,
  the last SystemUI event is also written to the report.
- **The headphone warning still appears:** The root manager may not run `post-fs-data.d` scripts, or the properties
  that the warning reads at boot may be overridden by the vendor; turn the option on, reboot, and check the
  diagnostic report.

## Building

Requirements: JDK 17 or newer (CI uses JDK 21). The Gradle wrapper jar is not in the repository because it is a binary
file; the version and the SHA-256 checksum are read from `gradle/wrapper/gradle-wrapper.properties`.

```sh
gradle wrapper --gradle-version <version in gradle-wrapper.properties> \
  --gradle-distribution-sha256-sum <checksum in gradle-wrapper.properties>   # once
./gradlew assembleRelease
```

`versionName` is in `app/build.gradle.kts`; `versionCode` comes from the CI run number. If no signing key is provided
in a local build, the release APK is signed with the debug key. The verbose event log is included in the APK only in
the CI pre-build (`SOUNDMOD_VERBOSE=true`); it is absent from local and stable builds.

## CI/CD

| Workflow | Trigger | Output |
|---|---|---|
| `prebuild.yml` (Pre-build) | Every push, PR and manual | A signed APK (artifact, including the verbose event log) and, on the default branch, a pre-release tagged `pre-<run number>` (the last 10 are kept) |
| `release.yml` (Stable release) | Manual only | A stable release tagged `v<versionName>` (no verbose event log); fails if there is no signing key and stops if that version already exists |
| `build-apk.yml` | Called by the two above | Shared build and signing steps |
| `security.yml` (Security and quality scan) | Push, PR, weekly, manual | Android Lint, Semgrep, CodeQL (Java/Kotlin and GitHub Actions, `security-and-quality`), Gitleaks (secrets in the git history and signing key files), zizmor (workflow security), dependency review on PRs |
| `dependabot.yml` | Weekly | Gradle and GitHub Actions version updates (grouped, with a 7-day cooldown); major bumps of the libxposed API are ignored |

The job fails if Android Lint finds an error, if Semgrep reports an ERROR-severity finding, or if zizmor reports a
high/critical finding; findings of lower severity are only reported (job summary and the `lint-raporu`,
`semgrep-raporu` and `zizmor-raporu` artifacts). Gitleaks, CodeQL and the dependency review fail the job on any
finding. The pre-build does not give signing secrets to PR builds (PR builds are signed with the debug key), and only
the four signing secrets are passed to the build workflow. Release pages are created with the GitHub CLI
(`gh release create`); no third-party release action is used. `gradle/actions/setup-gradle` is pinned to a full commit
SHA, whose version comment Dependabot keeps up to date, and the Gradle distribution is verified against the pinned
SHA-256.

### Signing key

Pre-releases and stable releases are signed with the same key so one can be installed over the other. The key is not
put in the repository; it is added as four secrets under **Settings → Secrets and variables → Actions**:

| Secret | Content |
|---|---|
| `SIGNING_KEYSTORE_BASE64` | The keystore file, base64-encoded |
| `SIGNING_KEYSTORE_PASSWORD` | The keystore password |
| `SIGNING_KEY_ALIAS` | The key alias |
| `SIGNING_KEY_PASSWORD` | The key password |

Generating the key and converting it to base64:

```sh
keytool -genkeypair -v -keystore soundmod.jks -alias soundmod \
  -keyalg RSA -keysize 4096 -validity 10000
base64 -w0 soundmod.jks    # save the output as SIGNING_KEYSTORE_BASE64
```

Back up the keystore file and the passwords; if they are lost, existing installs cannot be updated.
If `SIGNING_KEYSTORE_BASE64` is not set, the pre-build is signed with the debug key (a different signature on every
build) and prints a warning. At the end of a build the `apksigner` certificate digest is written to the log; that
digest can be used to verify two releases were signed with the same key.

### Making a stable release

1. Bump `versionName` in `app/build.gradle.kts` and push.
2. Run **Actions → Stable release → Run workflow** (optional release notes can be entered).
3. The APK is published on the **Releases** page as a stable release tagged `v<versionName>`.

## Project structure

```
app/src/main/
├── kotlin/dev/soundmod/nothing/
│   ├── MainActivity.kt
│   ├── core/    # catalog, scanner, converter, root, store
│   ├── hook/    # libxposed entry class and hooks
│   └── ui/      # Compose screens, theme, palettes, ViewModel
├── resources/META-INF/xposed/   # module.prop, scope.list, java_init.list
└── res/
```

## Known limitations

- Camera, video recording, NFC, reverse-charging and call notification sounds are not played in processes within
  SoundMod's scope. Changing them would need the relevant apps to be added to the Vector scope and the sound files to
  be delivered into those app processes (Vector remote file support); this is not implemented yet.
- The shutter sound that the old camera API plays in native code inside `cameraserver` cannot be hooked.
- The headphone warning option depends on the root manager running `/data/adb/post-fs-data.d` scripts.
- The system sound library searches `/<partition>/media/audio` on every partition at the root; vendor-specific sound
  directories outside that pattern are not listed. The "sound directories" section of the diagnostic report shows
  what exists on a device.
- The hook points rely on AOSP class names; on vendor skins where the class names differ, the related hook does not
  work and shows up as an error on the status card.
- The app UI is currently English only.

## License

SoundMod is licensed under the [GNU General Public License version 3](LICENSE) (GPL-3.0-or-later). You can
redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in
the hope that it will be useful, but WITHOUT ANY WARRANTY.
