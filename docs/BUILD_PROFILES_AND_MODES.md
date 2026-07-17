# Build profiles and future experience modes

## Android build profile

The GitHub Actions input `supports_images` is a developer-controlled boolean.
It is compiled into the APK as `BuildConfig.SUPPORTS_IMAGES`.

- `true` creates the `images` update channel and permits image-package downloads.
- `false` creates the `minimal` update channel and ignores image packages.

The Android user cannot change this value. The Settings screen displays the
installed profile as read-only information.

## Database server image selection

The database server's `include_images` input determines whether the generated
data release contains an image package. The Android image-support profile only
downloads that package when it exists. A minimal Android build always ignores
it.

## Theme

System, Light, and Dark remain runtime user preferences. They work with either
Android build profile.

## Future experience/data modes

Modes such as Minimalist, Waves, or other interaction styles are a separate
runtime concept. They must not reuse `SUPPORTS_IMAGES` or theme preferences.
They can later be represented by their own persisted mode identifier and can
work together with System, Light, or Dark appearance.

## Profile-specific app updates

App releases use independent manifests:

- `updates/app-update-images.json`
- `updates/app-update-minimal.json`

This prevents an image-support installation from silently replacing itself
with a minimal APK, or the reverse.
