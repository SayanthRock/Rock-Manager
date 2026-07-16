# Rock Manager

[![Android APK Release](https://github.com/SayanthRock/Rock-Manager/actions/workflows/android-release.yml/badge.svg)](https://github.com/SayanthRock/Rock-Manager/actions/workflows/android-release.yml)
[![Latest release](https://img.shields.io/github/v/release/SayanthRock/Rock-Manager)](https://github.com/SayanthRock/Rock-Manager/releases/latest)

Rock Manager is an Android app inspector for viewing installed-app metadata, reviewing permissions, extracting APK files, and exploring PNG assets packaged inside APKs.

## Features

- Inspect user and system apps, versions, SDK levels, sizes, and permissions
- Search and filter apps, including permission-based filtering
- Extract a base APK or package split APKs into a ZIP archive
- Select multiple apps for batch extraction
- Preview, save, and share PNG assets found inside APK files
- Keep local extraction and activity logs with light, dark, and dynamic themes

## Download

Download the latest verified APK and SHA-256 checksum from [GitHub Releases](https://github.com/SayanthRock/Rock-Manager/releases/latest).

Rock Manager supports Android 7.0 and newer (API 24+).

## Build locally

Requirements:

- JDK 17
- Android SDK platform 36.1
- Android SDK Build Tools 36.1.0

```bash
./gradlew :app:testDebugUnitTest :app:lintRelease :app:assembleRelease
```

The APK is written to `app/build/outputs/apk/release/`.

## Release signing

The release workflow supports a private production keystore through these repository secrets:

| Secret | Purpose |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | Base64-encoded JKS/PKCS12 keystore |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Signing key alias |
| `ANDROID_KEY_PASSWORD` | Signing key password |

If none of the four secrets exists, CI produces an installable debug-signed APK. If only some are configured, the workflow stops instead of creating an incorrectly signed release.

## Privacy and permissions

App inspection happens on-device. Extracted APKs are stored in the app's private storage until you explicitly share them. `QUERY_ALL_PACKAGES` is required because listing installed applications is Rock Manager's core function.

## License

No license has been declared yet. All rights remain with the repository owner unless a license file is added.
