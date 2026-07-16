# Architecture

```text
┌──────────────────────────────────────────────────────────────┐
│ wuwa-companion-unofficial                                    │
│ Android APK + app-update.json + APK GitHub Releases          │
└───────────────┬──────────────────────────────────────────────┘
                │ checks when online
                │
                ├──────────── app update ────────────────┐
                │                                        │
                ▼                                        ▼
updates/app-update.json                         normal Android installer
                │
                │
                └──────── database update ───────────────┐
                                                         ▼
┌──────────────────────────────────────────────────────────────┐
│ wuwa-database-server                                         │
│ manual GitHub Action -> scrape -> validate -> build -> publish│
│ public/latest/version.json + immutable data GitHub Releases  │
└──────────────────────────────────────────────────────────────┘
```

## Separation

The Android app never contains the scraper, Python runtime, source credentials, or publishing permissions. The backend never needs to run on a permanent server. GitHub Actions supplies the temporary build environment only when the owner clicks **Run workflow**.

## App data flow

1. WorkManager checks the two manifests when a network is available.
2. The user is notified when either version is newer.
3. APK updates are handed to Android's Package Installer; silent installation is not attempted.
4. Database/assets packages are downloaded into app-private staging.
5. SHA-256 is checked before import.
6. Database import should use a staging database and atomic replacement.
7. Images are encrypted into `SecureAssetStore`; plaintext image files are not retained.
