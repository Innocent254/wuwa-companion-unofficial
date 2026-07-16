# WuWa Companion Unofficial

Offline-first Android companion for Wuthering Waves, distributed through GitHub rather than Google Play.

> Unofficial community project. Not affiliated with, endorsed by, or sponsored by Kuro Games.

## Repository responsibility

This repository contains the Android application only. The manually triggered Python data builder and hosted update feed live in:

```text
Innocent254/wuwa-database-server
```

## Update channels

The app checks two independent manifests when internet is available:

1. **App update manifest**  
   `updates/app-update.json` in this repository.  
   A newer APK requires normal Android Package Installer confirmation.

2. **Database update manifest**  
   `public/latest/version.json` in `wuwa-database-server`.  
   The app notifies the user, downloads the selected package, verifies SHA-256, and imports it.

App and database versions are deliberately independent.

## Offline image protection

Images are not written to shared storage such as Downloads, Pictures, or `Android/media`.

Downloaded and bundled assets are imported into:

```text
context.noBackupFilesDir/secure_assets/
```

Each asset:

- has an opaque SHA-256-derived filename;
- is encrypted with AES-256-GCM;
- uses a key generated inside Android Keystore;
- is decrypted as a stream in memory;
- is never written back as a normal PNG/JPEG/WebP file.

This prevents casual discovery through ordinary file managers. It is not DRM: a rooted device, debugger, instrumentation framework, or modified app can still extract content.

## Build

Open the project in Android Studio and use JDK 17.

The GitHub workflow can build and publish signed APKs after these repository secrets are configured:

- `ANDROID_KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

Never commit the keystore or passwords.
