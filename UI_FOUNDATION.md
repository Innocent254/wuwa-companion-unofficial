# WuWa Companion UI foundation

This patch replaces the placeholder Compose screen with the first production-oriented UI shell.

## Included

- Responsive phone and large-screen navigation.
- Home, Library, Updates, and Settings destinations.
- Image-rich and text-only catalogue layouts.
- Automatic, forced-image, and forced-text display modes.
- Persisted System, Light, and Dark appearance modes.
- Optional Coil image loading with safe placeholders.
- All visible interface text in `res/values/strings.xml`.
- RTL support retained in the manifest.
- Unicode-safe catalogue presentation with no language whitelist.

## Current data state

The screen uses `DemoUiStateFactory` while the real database importer and repository layer are connected. The UI models are intentionally shaped for the backend catalog and optional image package.

## Recommended next UI milestone

Connect `CompanionUiState` to the installed `catalog.json`, then replace `DemoUiStateFactory` with a ViewModel-backed repository.
