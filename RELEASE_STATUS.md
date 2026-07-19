# Universal release status

This file is generated from the tracked Android and database source content.
Generated manifests, release state and public release output are excluded from change detection.

| Item | Value |
|---|---|
| Current visible version | `0.2.2` |
| Current Android version code | `4` |
| Current channel | `unreleased` |
| Source changes detected | **Yes** |
| Android source changed | Yes |
| Database source changed | Yes |
| Planned visible version | **`0.3.0`** |
| Planned Android version code | `5` |

The next visible version will be **0.3.0** (a +0.1 minor bump from 0.2.2).

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
