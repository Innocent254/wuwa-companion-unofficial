# Secure offline assets

## Goal

Prevent a normal user from opening a file manager and browsing the app's offline images.

## Storage design

- No `READ_MEDIA_IMAGES`, legacy storage, or broad file permissions.
- No output under Downloads, Pictures, `Android/media`, or shared external storage.
- Asset directory: `context.noBackupFilesDir/secure_assets`.
- Opaque file names: SHA-256 of the internal asset ID.
- File contents: AES-256-GCM encrypted.
- Key: generated in Android Keystore and marked non-exportable.
- Decryption: streaming only; no plaintext temporary files.

## Package import

The GitHub Release asset package is public and therefore not encrypted for a particular phone. After download and checksum verification, the importer must:

1. open each archive entry;
2. reject absolute paths and `..` traversal;
3. cap uncompressed size;
4. validate the content type;
5. stream the entry directly into `SecureAssetStore.put`;
6. delete the downloaded package after a successful import.

## Limits

This design is protection against casual discovery, not absolute extraction prevention. Root access, a compromised OS, runtime hooks, screenshots, memory dumps, or a modified APK can still expose content.
