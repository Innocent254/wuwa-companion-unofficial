# Universal release status

This file is generated from the tracked Android and database source content.
Generated manifests, release state and public release output are excluded from change detection.

| Item | Value |
|---|---|
| Current visible version | `0.4.0` |
| Current Android version code | `6` |
| Current channel | `prerelease` |
| Source changes detected | **No** |
| Android source changed | No |
| Database source changed | No |
| Planned visible version | **`0.4.0`** |
| Planned Android version code | `7` |

No source change was detected. Stable publish will promote **0.4.0** without changing the visible version.

## One universal application

The APK always supports both user data modes:

- **Minimalist** downloads and displays the text database only.
- **Images** downloads the same text database plus the optional verified image package.

Theme selection remains independent from the data mode.

## Manual workflow choices

- **build-only** builds the universal debug APK and database artifacts without reserving the version.
- **prerelease** publishes the paired APK and database as GitHub prereleases when source changes exist.
- **publish** publishes a stable pair when source changes exist, or promotes the current prerelease without a visible-version bump.

The Android version code can increase during prerelease-to-stable promotion because Android requires a higher internal build number for an installable update. The user-facing version name increases only when tracked source content changes.
