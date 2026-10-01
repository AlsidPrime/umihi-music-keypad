# Umihi Keypad: instructions for future contributors and agents

## Purpose and baseline

This is AlsidPrime's keypad-friendly fork of Umihi Music, built for a child's ZTE/nubia Cymbal 2. Prioritize dependable music playback and complete physical-key navigation on a tiny screen. Keep touchscreen layouts working on larger devices. Avoid adding complexity just to expand the feature list.

Repository: https://github.com/AlsidPrime/umihi-music-keypad
Upstream: https://github.com/ilianoKokoro/umihi-music

As of 2026-10-01 UTC, PR #1 is merged into main (merge e69ecd3aee6a88f1b9768d51eab0749ab9bbbe87). The tested feature commit is 8da0e3d579b7c8ae0af8b382e860017b929929a9. Version: 1.15.1-keypad-beta-store, versionCode 11501. Treat these as historical checkpoints; read current source and refs before editing.

## Working agreement

- Read current code and git status first. Preserve unrelated user changes.
- Use focused commits. For future feature work, start a branch from current main and prepare a reviewable PR. Do not assume the former feature branch is still the destination.
- The user prefers autonomous implementation, working commits, and a build they can pull and test. Avoid repeated approval requests for ordinary implementation. Merge/publish only when authorized for that task; PR #1's merge authorization is already fulfilled.
- Report what changed, build/test results, and what still requires physical-device testing. A successful APK build does not prove key delivery, display rendering, or audio behavior on the handset.
- Never expose account tokens, private device data, signing passwords, or keystores in commits/logs. Do not add agent attribution to commits.

## Target handset and verified constraints

| Property | Value |
| --- | --- |
| Model | Z2335L, marketed as ZTE/nubia Cymbal 2 |
| Firmware | Z2335LGV1.0.0B38 |
| Android | 10, SDK 29 |
| ABIs | armeabi-v7a, armeabi (32-bit ARM) |
| Main screen | 240 × 320 px, 160 dpi |
| Front screen | Display ID 1, name HDMI Screen, 128 × 160 px, reported density 37 dpi |
| Low RAM | ro.config.low_ram=true |
| Root | adbd refuses root on this production build |

Keep minSdk compatibility (currently 24) and armeabi-v7a support. Never replace builds with arm64-only artifacts. Do not assume touch input, Google Play Services, notification-listener availability, or root.

Notification-listener access remained unavailable on this low-RAM Android 10 firmware despite attempted ADB grants. Do not revisit that as the background-control solution. The app now controls its own MediaSession directly.

Traditional T9 is installed and works (io.github.sspanak.tt9/.ime.TraditionalT9). Preserve text entry with it. KeyMapper was useful during prototyping but is not required for the implemented background shortcuts; competing accessibility key filtering can interfere.

## Build, installation, and signing

Read app/build.gradle.kts, gradle/libs.versions.toml, wrapper properties, and daemon JVM criteria for current tool versions. The wrapper currently uses Gradle 9.7.1; daemon criteria request JetBrains Java 21. The user's Java 23 launcher worked because daemon provisioning is separate. Do not change toolchain requirements simply because the installed launcher is newer.

Build and meaningful unit tests on Windows from the repository root:

    .\gradlew.bat :app:assembleStoreDebug :app:testStoreDebugUnitTest

Current APK output:

    app\build\outputs\apk\store\debug\UmihiMusic-store.apk

Install with the user's Windows ADB path:

    & "C:\Users\alsid\Downloads\platform-tools-latest-windows\platform-tools\adb.exe" install -r ".\app\build\outputs\apk\store\debug\UmihiMusic-store.apk"

The user's working folder is C:\Dev\umihi-music-keypad\umihi-music-keypad. These paths are convenience examples, not portable build configuration.

Debug package: ca.ilianokokoro.umihi.music.keypad. Original package: ca.ilianokokoro.umihi.music. IMPORTANT: release currently retains the original applicationId because only debug adds .keypad. Before distributing a release, explicitly resolve fork package identity and signing rather than accidentally conflicting with upstream.

Preserve the user's local Windows debug signing key for updates. A Linux-built debug APK is not a routine replacement: it can have a different signature and require uninstalling, losing app data/downloads. Never recommend uninstalling as the first response to a signature mismatch. Back up and agree on migration if necessary. For public releases, use a durable dedicated release key, keep it private/backed up, and maintain it across updates.

Use the store flavor for development: it disables the upstream updater. Audit upstream repository/download references before enabling standalone updates or publishing releases. TinyWall previously blocked Java's network access; investigate firewall/proxy failures before rewriting Gradle versions.

Transient verification environments may need local proxy/trust-store/SDK configuration. Do not commit environment-specific local.properties, daemon-criteria removals, aapt2 overrides, or scratch paths. A prior Linux session used SDK 37 aapt2 because the Maven artifact download failed. Incremental compiler caches also produced duplicate declarations in that transient checkout; a clean build succeeded. Treat this as environment troubleshooting, not a reason to alter app logic.

## Keypad behavior to preserve

- Compact layouts currently activate when min(screenWidthDp, screenHeightDp) <= 360 and max(...) <= 480. Read isKeypadScreen in KeypadList.kt before changing this; other handset sizes may need explicit mode selection later.
- D-pad Up/Down selects entries, OK activates, and options open the relevant song actions. Selection must remain visible and scroll into view.
- Left/Back should exit screens promptly, without scrolling through a long playlist to a bottom Close row. Back in a filter closes the filter first.
- In the player, Left moves across controls normally. A fresh Left press at the left edge closes the player. A held Left must not close it accidentally. In lyrics, Left hides lyrics.
- Foreground 4/5/6 are Previous / Play-Pause / Next on noneditable controls. Never intercept those digits in TT9 text editors.
- Focus should recover predictably after dialogs/player close, reorders, removals, and asynchronous list updates.
- Keep long text independently scrollable with dialog actions always reachable. Avoid large lyrics or dialogs trapping the user or hiding controls.
- Bottom navigation labels are Home / Find / Setup / Now, with compact single-line buttons.
- Queue Play plays the chosen entry now. Play next moves the existing selected entry directly after the currently playing one without interrupting playback or duplicating it. Resolve identities against the live timeline, accounting for removal shifting the current index; never fall back to another entry when identity is missing/ambiguous.

## Background playback shortcuts

Opt-in at Settings > General > Background playback shortcuts. Enable both the app preference and its Android accessibility service. Default is off.

| Gesture | Action |
| --- | --- |
| Short Volume Up/Down tap | Music volume changes on release |
| Hold Volume Up | Next track |
| Hold Volume Down | Previous track |
| Both volume keys together | Play/Pause |

KeypadAccessibilityService connects a MediaController explicitly to this app's PlaybackService; generic global media keys can target another app. Maintain complete captured down/up pairs, suppress repeats, cancel pending actions on interruption/disable, and release controllers correctly. Keep numeric keys untouched. Foreground app input and call audio modes are excluded. Main display state is used because ZTE may leave the Activity visible with the lid closed.

The user confirmed all closed-lid gestures worked flawlessly on Z2335L. Persistence/reconnection after reboot and behavior on other devices are not established by that report. Service-bound status alone is not proof that it wins key filtering or has a connected playback controller.

## Front-screen findings: do not repeat unsuccessful assumptions

Native ZTE front-screen UI already displays Umihi Keypad's track title and updates when the song changes; the user confirmed this. Keep normal MediaSession metadata working. Custom graphics are optional future exploration, not a requirement for usable now-playing information.

The manual Settings > App info > Front display test tries an Android Presentation, with white “Umihi / Display test” on black for 60 seconds. It does NOT show real track titles. The phone enumerates display 1 but rejects Presentation.show with WindowManager.InvalidDisplayException: “the specified display can not be found”. Enumeration is not proof of draw permission or presentation eligibility. Do not claim custom rendering works.

The timeout currently replaces the last useful Result with “Test stopped after 60 seconds”; a future cleanup could preserve outcome separately from running state. Failure details can be retrieved with:

    .\adb.exe logcat -d -s UmihiFrontDisplay

Vendor front-screen package: com.zte.featurephone.seconddisplay. Closed-lid dumps show its window on display 1. The main display turns off and the front display turns on; power remained Awake and window policy reported LID_ABSENT. No vendor APK has been analyzed here and no vendor control intent has been confirmed. Do not send guessed broadcasts, add root/overlay permissions, or implement automatic probes without evidence. If customization is requested later, inspect the vendor APK/interfaces first and account for 128 × 160 pixels and unusual density.

## Source map (under app/src/main/java/ca/ilianokokoro/umihi/music)

- ui/components/keypad/: KeypadList, selection helpers, buttons, dialogs, text dialogs, Back and playback key handling.
- ui/navigation/KeypadNavigationBar.kt: compact bottom navigation and focus routing.
- ui/screens/{home,playlist,search,player,settings}/: compact screens alongside existing layouts.
- ui/components/bottomsheet/: KeypadQueue, playlist creation, playback options, addtoplaylist/KeypadAddToPlaylist.
- services/KeypadAccessibilityService.kt and VolumeShortcutGesture.kt: opt-in volume gestures.
- core/managers/PlayerManager.kt and services/PlaybackService.kt: media session, queue, playback.
- core/managers/FrontDisplayManager.kt and ui/screens/diagnostics/FrontScreenDiagnostics.kt: optional manual probe.
- data/repositories/DatastoreRepository.kt and Settings models/view models: persisted preferences.
- res/drawable/keypad_brand.xml and res/mipmap-anydpi*/keypad_icon.xml: native vector branding; manifest and themes reference it.

## Quality, downloads, and storage

Streaming and download quality are separate persisted preferences: Best (default), Balanced (128 kbps target), Data saver (64 kbps target). Both extraction routes choose an available bitrate <= target, falling back to the lowest known bitrate. NewPipe units require conversion from kbps. Limited quality bypasses the legacy highest-quality URL cache. Existing downloaded files do not change quality retroactively.

Cache clearing uses the live SimpleCache removeResource path, not directory deletion while in use. Cache work runs on IO. Cache-limit changes apply after restart. Preserve these behaviors and explicit Save/Cancel.

## Verification and remaining scope

The last app change passed a clean store debug APK build and all 20 unit tests: selection (5), queue (3), audio quality (6), volume gesture (6). Test reorder cases both before and after the current track, missing identities, held-key suppression, and text-input preservation when changing related code. Do not write tests that merely repeat static labels/resources. Documentation-only changes do not need an APK rebuild.

For UI/control changes, give the user focused physical-phone checks. Preserve their confirmation that existing screens/Settings are satisfactory instead of reopening a broad overhaul without a reported problem. The last new visual polish/Play next action did not receive a separate detailed handset report; merge was explicitly authorized.

Remaining optional work: user-facing controls/setup guide; clearer background setup/reboot status if a problem is found; diagnostic result retention; public-release packaging/documentation; custom front-screen graphics only if requested. No public signed APK release or stable release tag has been created as part of this handoff.

## Public release considerations

The GitHub repository is already public, but the inherited README/badges/download links advertise upstream, not this fork. Before promoting it, write an honest fork-specific README with screenshots, controls, tested-device list, installation/accessibility setup, limitations, and upstream attribution. Label other Android keypad phones as untested, not compatible by assumption. This is an unofficial YouTube Music client, not an official Google product or a promise of perpetual service compatibility.

Inspect LICENSE and retain upstream copyright/license notices. Choose a distinct durable package identity, signing key, versioning/update source, and reproducible release workflow before posting installable APKs. Never publish the user's personal debug APK/signing material as the public release strategy. Keep credentials out of the repository. Prefer a small experimental GitHub release and voluntary device reports before wider distribution. Do not post to communities or contact people without explicit authorization.
