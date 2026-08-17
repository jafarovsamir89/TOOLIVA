# Tooliva — Feature Matrix

Market-driven revision: 2026-08-16

| Feature | Priority | Access / API | Milestone |
|---|---:|---|---|
| Full Storage Mode | P0 | `MANAGE_EXTERNAL_STORAGE` | Cleaner Beta |
| Limited Storage fallback | P0 | MediaStore / SAF | Cleaner Beta |
| Deep Storage Scan | P0 | StorageProvider + local index | Cleaner Beta |
| Storage overview/categories | P0 | Filesystem / volumes | Cleaner Beta |
| Large Files — all accessible types | P0 | Full Storage Mode | Cleaner Beta |
| Downloads analyzer | P0 | Full Storage / SAF fallback | Cleaner Beta |
| APK/installers analyzer | P0 | Filesystem; PackageManager where safe | Cleaner Beta |
| Archives/Documents categories | P0 | Filesystem | Cleaner Beta |
| Explainable junk candidates | P0 | Rule engine | Cleaner Beta |
| Old files | P0 | Filesystem metadata | Cleaner Beta |
| Screenshot Cleaner | P0 | MediaStore / StorageProvider | Cleaner Beta |
| Exact duplicate files | P0 | Storage index + hashing | Cleaner Beta |
| Empty folders | P1 | Filesystem | 1.0 |
| Cleanup Receipt | P0 | Central coordinator | Cleaner Beta |
| Cleanup Swipe | P0 | Local file review | 1.0 |
| System cache cleanup entry | P0 | `ACTION_CLEAR_APP_CACHE` + Full Storage Mode | Cleaner Beta |
| File Manager browser | P0 | Full Storage / SAF | Cleaner Beta |
| Global file search | P0 | Local storage index | Cleaner Beta |
| Copy/Move/Rename/Delete | P0 | Filesystem / central coordinator | Cleaner Beta |
| Storage Map | P0/P1 | Local index | Cleaner Beta |
| ZIP/unZIP | P1 | Local files | 1.0 |
| File hashes | P1 | Local files | 1.0 |
| App Manager basic | P0/P1 | PackageManager | 1.0 |
| Broad app visibility | Conditional | `QUERY_ALL_PACKAGES` | Only if justified |
| App usage / unused apps | P1 | Usage Access | 1.0 |
| Phone Doctor | P0 | Device/system APIs | 1.0 |
| Hardware tests | P1 | Sensors/camera/mic | 1.0 |
| Notification History | P0 | Notification Access | 1.0 |
| Noisy notification insights | P1 | Local notification DB | 1.0 |
| Image Compress/Resize/Convert | P1 | Local files | 1.0 |
| EXIF Privacy Clean | P1 | ExifInterface | 1.0 |
| Images → PDF | P1 | Local files | 1.0 |
| QR scan/generate | P1 | Camera / local | 1.0 |
| Network tools | P1 | Network APIs | 1.0 |
| Compass/Level/Flashlight | P1 | Sensors/camera | 1.0 |
| Private Vault | P1 | Keystore/biometric/local crypto | 1.1 if needed |
| App Lock | P1 experimental | Usage/overlay; Accessibility not approved | 1.1 if needed |
| Similar-photo detection | P2 | Local perceptual matching | Later |
| Magnifier/micro-tools | P2 | Camera/local | Later |

## Restricted-access decisions

### Approved for prototype/product implementation
- `MANAGE_EXTERNAL_STORAGE`
  - because file management, storage search and deep storage maintenance are now explicit core purposes;
  - requires Limited Mode fallback and Google Play declaration/approval before production.

### Conditional — not automatically approved
- `QUERY_ALL_PACKAGES`
  - add only if App Manager/file attribution cannot meet its core user-facing purpose with narrower visibility;
  - requires explicit product decision and Play declaration review.
- `PACKAGE_USAGE_STATS`
  - only for user-enabled usage/unused-app features.
- Notification Access
  - only for Notification History after disclosure.
- AccessibilityService
  - not approved for Cleaner or App Lock V1 without a separate human + policy decision.

## Product feature flags / gates

Suggested gates:
- `FULL_STORAGE_MODE_ENABLED`
- `STORAGE_MAP_ENABLED`
- `APP_MANAGER_ENABLED`
- `BROAD_PACKAGE_VISIBILITY_ENABLED`
- `NOTIFICATION_HISTORY_ENABLED`
- `VAULT_ENABLED`
- `APP_LOCK_ENABLED`
- `ADS_INTERSTITIAL_ENABLED`

Restricted or experimental features must be removable/disableable without destabilizing the core Cleaner/File Manager.
