# WuWa Companion Unofficial

Offline-first Android companion for Wuthering Waves, distributed through GitHub
rather than Google Play.

> [!IMPORTANT]
> This is an unofficial, non-commercial fan project. It is not affiliated with,
> endorsed by, sponsored by, or approved by Kuro Games. Wuthering Waves and all
> associated visual and audio assets are trademarks, copyrights, or other
> intellectual property of Kuro Games and/or its licensors. The MIT
> License covers original project code only—not official game assets or
> separately licensed data. Takedown or licensing requests may be sent to
> **chengoleinnocent@gmail.com**. See [LEGAL.md](LEGAL.md).

## Repository responsibility

This repository contains the Android application only. The manually triggered
Python data builder and hosted update feed live in:

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
   The app notifies the user, downloads the selected package, verifies SHA-256,
   and imports it.

App and database versions are deliberately independent.

## Image permission status

Written permission has been requested from Kuro Games for limited use of
selected official game images in this free, ad-free application. The request,
silence, or customer-support correspondence must not be treated as approval.

Until express written permission covering repository, APK, and data-package
distribution is received, public releases must use original placeholders or
other assets with verified redistribution rights. Official game images are not
covered by the project's MIT License.

The project will promptly review a credible rights-holder request and may
remove, replace, or disable affected material. That cooperation policy does not
create permission to use proprietary assets. See [LEGAL.md](LEGAL.md),
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md), and the notices inside Android
asset directories.

## Offline image protection

Images that the project is legally permitted to distribute are not written to
shared storage such as Downloads, Pictures, or `Android/media`.

Downloaded and bundled assets are imported into:

```text
context.noBackupFilesDir/secure_assets/
```

Each asset:

- has an opaque SHA-256-derived filename;
- is encrypted with AES-256-GCM;
- uses a key generated inside Android Keystore;
- is decrypted as a stream in memory; and
- is never written back as a normal PNG, JPEG, or WebP file.

This prevents casual discovery through ordinary file managers. It is not DRM:
a rooted device, debugger, instrumentation framework, or modified app can still
extract content. Encryption does not grant or replace content-distribution
rights.

## Build

Open the project in Android Studio and use JDK 17.

The GitHub workflow can build and publish signed APKs after these repository
secrets are configured:

- `ANDROID_KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

Never commit the keystore or passwords.

## Licensing

- Original application code: [MIT License](LICENSE)
- Legal disclaimer and rights-holder process: [LEGAL.md](LEGAL.md)
- Third-party and data notices:
  [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)
