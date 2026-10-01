# Building a public preview

Public releases use package `ca.ilianokokoro.umihi.music.keypad`, distinct from upstream Umihi. Use the **store release** build; the inherited updater is disabled in both flavors until a fork-specific update feed is configured.

## 1. Create and protect a signing key

On your Windows computer, run this in PowerShell:

```powershell
keytool -genkeypair -v -keystore "$env:USERPROFILE\umihi-keypad-release.jks" -alias umihi-keypad -keyalg RSA -keysize 2048 -validity 10000
```

Choose a strong password and answer the certificate prompts. Keep a private backup of the keystore and its password; every future update needs the same signing key. Do not overwrite an existing release keystore or upload it to GitHub.

## 2. Configure local signing

Copy `keystore.properties.example` to `keystore.properties` in the repository root. Replace the example username and passwords. Use an absolute path with forward slashes, such as `C:/Users/alsid/umihi-keypad-release.jks`.

For the default PKCS12 keystore created by modern keytool, use the same store and key password. Java properties files interpret backslashes as escapes; encode any literal backslashes in property values as `\\`.

Check that Git ignores your local credentials and keystore before committing:

```powershell
git check-ignore keystore.properties
git status --short
```

The first command should print `keystore.properties`. `.jks` and `.keystore` files are also ignored. Keep actual credentials outside tracked example files and outside build logs.

## 3. Build

From the repository root:

```powershell
.\gradlew.bat :app:assembleStoreRelease
```

The configured output is:

```text
app/build/outputs/apk/store/release/UmihiMusic-store.apk
```

Release builds enable code and resource shrinking. The filename is retained from upstream; inspect the APK itself to verify its identity. Without `keystore.properties`, this configuration can produce an unsigned APK; do not publish it. `BUILD SUCCESSFUL` alone does not prove that an APK is signed.

## 4. Verify the APK

Use `apksigner` and `aapt` from your Android SDK's `build-tools/37.0.0` folder (adjust the path if the repository changes build-tools versions):

```powershell
& "C:\path\to\Android\Sdk\build-tools\37.0.0\apksigner.bat" verify --verbose --print-certs ".\app\build\outputs\apk\store\release\UmihiMusic-store.apk"
& "C:\path\to\Android\Sdk\build-tools\37.0.0\aapt.exe" dump badging ".\app\build\outputs\apk\store\release\UmihiMusic-store.apk"
Get-FileHash ".\app\build\outputs\apk\store\release\UmihiMusic-store.apk" -Algorithm SHA256
```

Check that signature verification succeeds, the certificate is your intended release certificate, the package is `ca.ilianokokoro.umihi.music.keypad`, and the APK includes `armeabi-v7a`. Record the file SHA-256 for the release notes and the signing certificate fingerprint privately for future comparisons.

## 5. Test before uploading

Use a spare handset or emulator initially. Your existing debug installation has the same package name but a different signature, so the signed release cannot replace it with `adb install -r`. Keep your working debug installation; do not uninstall it just to test a public build. Moving it to release signing requires a deliberate backup/migration plan.

On a device without an existing conflicting installation, check:

- Launch, search, login if used, and T9 typing.
- Playlist and queue navigation, including Play next and Left/Back.
- Streaming and offline downloaded playback.
- Accessibility setup, short-tap volume, held-volume track changes, and play/pause chord.
- Restart behavior and closed-lid controls on supported hardware.

The minified release needs its own device check even if the debug build worked. Keep the first release marked as a preview; other handsets are untested.

## 6. Publish a GitHub prerelease

Open this fork's Releases page and draft a release targeting the tested commit. For the current app version, a suitable first tag is `v1.15.1-keypad-preview.1`.

- Title: **Umihi Keypad — first public preview**.
- Mark it as a prerelease.
- Attach the verified signed APK; a clear download filename such as `UmihiKeypad-1.15.1-preview.1.apk` is fine.
- Include its SHA-256, tested model/firmware, Android requirement, installation instructions, accessibility setup, and limitations.
- Explain that native external-screen song information depends on the handset and that the optional custom-display probe does not work on the tested ZTE firmware.
- Link the corresponding source tag and preserve upstream license/credits.

Then update the README's project-status/download section to link to this fork's release. Do not imply that upstream store listings distribute this fork.

For later APK updates, keep package identity and signing key unchanged and increase `versionCode` in `app/build.gradle.kts`. A Git tag alone does not change Android's version number.
