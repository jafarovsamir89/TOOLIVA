# Tooliva — Feature Matrix

Revision: 2026-08-18

| Feature | Priority | Primary access / API | Milestone |
|---|---:|---|---|
| Full Storage Mode | P0 | `MANAGE_EXTERNAL_STORAGE` | Cleaner Recovery/Beta |
| Limited Storage fallback | P0 | MediaStore / SAF | Cleaner Beta |
| Direct progressive Cleaner scan | P0 | `StorageProvider` + Flow | Cleaner Recovery |
| Storage overview | P0 | filesystem/volume APIs | Cleaner Beta |
| Large Files — all accessible types | P0 | Full Mode; Limited fallback | Cleaner Recovery/Beta |
| Downloads analyzer | P0 | Full Mode / SAF fallback | Cleaner Beta |
| APK/installers analyzer | P0 | filesystem + safe PackageManager parsing | Cleaner Beta |
| Archives/Documents categories | P0 | filesystem metadata | Cleaner Beta |
| Old files | P0 | filesystem metadata | Cleaner Beta |
| Screenshot Cleaner | P0 | Full Mode first; MediaStore Limited fallback | Cleaner Beta |
| Explainable junk candidates | P0 | deterministic rules | Cleaner Beta |
| System cache cleanup entry | P0 | `ACTION_CLEAR_APP_CACHE` + required access | Cleaner Beta |
| Cleanup Receipt | P0 | central cleanup coordinator | Cleaner Recovery/Beta |
| Exact duplicate files | P0 | size pre-group + candidate hashing + optional Room fingerprint cache | Cleaner Beta |
| Cleanup Swipe | P1 | local review state | 1.0 |
| Empty folders | P1 | filesystem | 1.0 |
| File Manager browser | P0 | Full Mode / SAF | Cleaner Beta |
| File search | P0 | direct/targeted search first; optional cache only if measured need | Cleaner Beta |
| Copy/Move/Rename/Delete | P0 | shared file-operation layer | Cleaner Beta |
| Storage Map | P1 | explicit folder-size analysis | 1.0 |
| App Manager basic | P0/P1 | PackageManager | 1.0 |
| Broad app visibility | Conditional | `QUERY_ALL_PACKAGES` | only if justified |
| App usage / unused apps | P1 | Usage Access | 1.0 |
| Phone Doctor | P0 | device/system APIs | 1.0 |
| Hardware tests | P1 | sensors/camera/mic | 1.0 |
| Notification History | P0 | Notification Access + Room | 1.0 |
| Image/PDF tools | P1 | local files | 1.0/later |
| QR/network/sensor tools | P1 | relevant platform APIs | 1.0/later |
| Private Vault | P1 | Keystore/biometric/local crypto | 1.1 if needed |
| App Lock | P1 experimental | Usage/overlay; Accessibility not approved | later/conditional |
| Similar-photo detection | P2 | local perceptual matching | Later |

## Architecture decisions

### Primary Cleaner data path

Approved:

`StorageProvider -> progressive StorageScanEvent -> classifiers -> UI`

Rejected as primary path:

`whole-device scan -> mandatory Room index/generation -> UI`

Room is allowed only for demonstrated persistent-state needs such as duplicate fingerprint cache or Notification History.

## Permission decisions

### Approved for prototype/product implementation

`MANAGE_EXTERNAL_STORAGE`

Reason: Cleaner + real File Manager + on-device shared-storage search/management are core functions.

Production still requires current Google Play declaration/review.

### Full Mode UX rule

When Full Mode is granted, Cleaner submodules should not request redundant broad Photos/Videos permission for the same storage-cleaning purpose.

### Conditional

`QUERY_ALL_PACKAGES`

Only after narrower App Manager visibility proves insufficient for a genuine core user-facing feature, followed by current policy review and explicit human approval.

`PACKAGE_USAGE_STATS`

Only for user-enabled usage/unused-app features.

Notification Access

Only for Notification History.

AccessibilityService

Not approved for Cleaner/App Lock V1 without a separate explicit human + policy decision.

## Deferred infrastructure

Do not add merely to satisfy architecture style:

- Room global storage index;
- WorkManager for speculative background indexing;
- Hilt before dependency complexity requires it;
- DataStore before persistent settings need it.

## Product gates

Suggested gates:

- `FULL_STORAGE_MODE_ENABLED`
- `APP_MANAGER_ENABLED`
- `BROAD_PACKAGE_VISIBILITY_ENABLED`
- `NOTIFICATION_HISTORY_ENABLED`
- `VAULT_ENABLED`
- `APP_LOCK_ENABLED`
- `ADS_INTERSTITIAL_ENABLED`

Restricted/experimental features must be removable without destabilizing Cleaner/File Manager.
