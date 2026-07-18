# Universal APK and user-selectable data modes

WuWa Companion now ships as one Android APK. The APK understands both the text database and the optional image package.

## First launch

Android's package installer cannot display application-specific setup fields. Immediately after installation, WuWa Companion presents its own first-launch setup where the user chooses:

- Theme: System, Light, or Dark.
- Data mode: Minimalist or Images.

These settings are independent and may be changed later.

## Minimalist

The application downloads and installs `database-full.wupack`. It does not request `assets-full.wupack`, and card layouts remain text-focused.

## Images

The application installs the same `database-full.wupack` and downloads `assets-full.wupack` when the manifest says that package is available. Downloads remain SHA-256 verified and assets remain in app-private encrypted storage.

Switching back to Minimalist hides images and stops future image downloads. Existing encrypted assets are retained so switching back does not necessarily require another download. A future storage-management screen may add an explicit remove-images action.

## Releases

The synchronized workflow publishes one APK and one database release. The database release may contain both:

- `database-full.wupack` for every user.
- `assets-full.wupack` for users who select Images mode.

The workflow offers build-only, prerelease, and publish. Visible versions increase by one minor step only when tracked app or database source changes.
