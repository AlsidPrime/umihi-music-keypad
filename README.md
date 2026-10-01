# Umihi Keypad

**YouTube Music for Android flip phones with physical keypads.**

Umihi Keypad is a fork of [Umihi Music](https://github.com/ilianoKokoro/umihi-music), adapted for small screens, D-pads, and number keys. It started with a ZTE/nubia Cymbal 2: music played well, but touchscreen controls were hard to reach without a mouse. This fork adds compact screens and physical-key controls for everyday listening.

Built with Kotlin and Jetpack Compose. An unofficial client, independent of Google and YouTube.

## What changes in this fork?

- Compact Home, Search, playlists, player, lyrics, queue, and Settings screens.
- Visible selection, predictable D-pad navigation, and quick Back actions without scrolling through an entire playlist.
- Previous, play/pause, and next shortcuts on the number keys while the app is open.
- Optional volume-button playback shortcuts that work with the lid closed on the tested Cymbal 2.
- Queue actions: Play, Play next, Move earlier, Move later, and Remove.
- Separate streaming and download quality choices, storage controls, sleep timer, and playback speed controls.
- Traditional T9 text entry, with number shortcuts kept out of text editors.
- A distinct keypad-and-music-note icon and startup logo.

Upstream features include searching and streaming music, YouTube Music playlists, playlist management, and downloads for offline playback. Larger screens retain the existing touchscreen layouts.

## Project status and compatibility

This is an experimental fork with working handset testing, not a broadly tested release. **There is no published APK release for this fork yet.** Build from source using the instructions below. APKs on upstream Umihi's download pages do not contain these keypad changes.

| Device | Testing |
| --- | --- |
| ZTE/nubia Cymbal 2, model Z2335L | Main keypad screens and Settings tested; closed-lid volume shortcuts and native front-screen track updates confirmed |
| Other Android keypad phones | Not yet tested; device reports welcome |

Tested firmware: Z2335LGV1.0.0B38, Android 10, 32-bit ARM, 240 × 320 main screen. The app requires Android 7.0 or newer (API 24) and includes armeabi-v7a support. Your phone must allow installing Android APKs.

Compact layouts are selected by screen size: the shorter dimension must be at most 360 dp and the longer at most 480 dp. A phone having a keypad does not automatically mean it will select this layout or support all background shortcuts.

## Keypad controls

### While using the app

| Key | Action |
| --- | --- |
| Up / Down | Select and scroll through list entries |
| OK / centre key | Activate the selected entry |
| Right on a song with options | Open its action menu |
| Left / Back | Return or dismiss, according to the screen |
| 4 | Previous track |
| 5 | Play / Pause |
| 6 | Next track |

The 4/5/6 shortcuts apply on noneditable controls. In text editors those keys remain available for typing with T9.

In the player, Left moves between controls. Press Left again when already at the leftmost control to close the player. In lyrics, Left returns to the player. The bottom navigation is **Home · Find · Setup · Now**; Now opens the player.

In the queue, **Play** starts the selected song immediately. **Play next** moves that existing queue entry immediately after the current song, without interrupting playback or adding a duplicate.

### Background and closed-lid controls

Background shortcuts are optional and off by default:

1. Open **Setup → General → Background playback shortcuts**.
2. Turn the app option on.
3. Open Accessibility settings from that dialog and enable Umihi Keypad's accessibility service.
4. Disable competing key-filtering services, such as KeyMapper, while testing these shortcuts.
5. Start playback in Umihi Keypad, then leave the app or close the lid.

| Gesture | Action |
| --- | --- |
| Tap Volume Up / Down | Change music volume on release |
| Hold Volume Up | Next track |
| Hold Volume Down | Previous track |
| Press both volume keys together | Play / Pause |

The service handles volume-key gestures and controls this app's playback session. It does not require root or notification-listener access. These gestures were confirmed with the lid closed on Z2335L; other phones may deliver keys differently. Check service enablement and shortcut behavior after restarting your phone.

## Typing and the front screen

[Traditional T9](https://github.com/sspanak/tt9) works for text entry on the tested phone. Install and configure it separately; it is not bundled with Umihi Keypad.

The Cymbal 2's built-in front-screen interface already shows the current song title and updates when the track changes. This is the phone's native media integration, not a custom Umihi display.

**Front display test** in App info is an optional diagnostic. Its custom drawing attempt is rejected by the tested firmware. It is not needed for native song titles or closed-lid controls; custom graphics remain an experiment.

## Build and install

### Prerequisites

- Git and an Android development environment, including the Android SDK.
- Access to the SDK platform/build tools required by [app/build.gradle.kts](app/build.gradle.kts), currently API 37 and build-tools 37.0.0.
- A Java environment suitable for the Gradle wrapper. The repository's daemon configuration requests JetBrains Java 21; Gradle may provision it on the first run.
- Android Platform Tools (ADB), USB debugging enabled on the phone, and authorization for your computer.

Clone the fork:

```sh
git clone https://github.com/AlsidPrime/umihi-music-keypad.git
cd umihi-music-keypad
```

Build on Windows PowerShell:

```powershell
.\gradlew.bat :app:assembleStoreDebug
```

Or on Linux/macOS:

```sh
./gradlew :app:assembleStoreDebug
```

The APK is written to:

```text
app/build/outputs/apk/store/debug/UmihiMusic-store.apk
```

With ADB on your PATH, install or update it from the repository root:

```sh
adb devices
adb install -r app/build/outputs/apk/store/debug/UmihiMusic-store.apk
```

If ADB is not on PATH, use its full executable path. In PowerShell, a path containing spaces needs the call operator, for example:

```powershell
& "C:\path\to\platform-tools\adb.exe" install -r ".\app\build\outputs\apk\store\debug\UmihiMusic-store.apk"
```

### Updating without losing downloads

Pull changes and rebuild using the same computer and signing key, then install with `adb install -r`. Do not uninstall just to update: uninstalling removes app data and downloads. A debug APK built on another computer may have a different signature and cannot replace your existing installation directly.

The documented debug build uses package `ca.ilianokokoro.umihi.music.keypad`, so it can coexist with original Umihi. The **store** flavor disables the inherited updater; the name does not mean this fork is listed in an app store.

Public release packaging is still pending. Release builds currently retain upstream's package name; fork package identity and a permanent release signing key need to be settled before distributing those builds.

## Downloads, quality, and storage

Streaming and download quality are configured separately:

| Quality | Selection |
| --- | --- |
| Best | Best available stream |
| Balanced | Available stream at or below a 128 kbps target |
| Data saver | Available stream at or below a 64 kbps target |

These are selection targets, not guarantees that every track has that bitrate. If no stream fits a limited target, the lowest known bitrate is used. Existing downloaded tracks keep their original quality; changing the preference affects new downloads. Cache-limit changes take effect after restarting the app.

## Contributing and reporting problems

Read [AGENTS.md](AGENTS.md) for the development handoff, navigation rules, source map, device findings, and signing constraints. Keep changes focused and preserve both keypad navigation and T9 text entry.

When reporting a problem, include the phone model, Android version, firmware, app version, screen size, and exact steps. For shortcut problems, include whether the lid was closed, the accessibility service was enabled, and another key-filtering service was running. Redact credentials and account information from logs.

Run the store debug build and unit tests for relevant code changes:

```powershell
.\gradlew.bat :app:assembleStoreDebug :app:testStoreDebugUnitTest
```

Physical-device checks remain necessary for focus, text entry, audio, and closed-lid key delivery. YouTube service changes can also affect playback independently of keypad controls.

## Credits and license

Based on [Umihi Music by ilianoKokoro](https://github.com/ilianoKokoro/umihi-music) and its contributors. Thank you to the upstream developers and translators for the music player this fork builds on. See the [upstream project](https://github.com/ilianoKokoro/umihi-music) and [translation project](https://crowdin.com/project/umihi-music) for their credits and work.

The original Umihi logo was made by [Apelleru](https://www.twitch.tv/apelleru); inherited original logo assets remain in the source. This fork uses a separate keypad-and-music-note launcher design.

Licensed under the GNU General Public License v3; see [LICENSE](LICENSE). Existing upstream copyright and license notices remain applicable.
