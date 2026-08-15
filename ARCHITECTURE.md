# Tooliva — Architecture

## Goals

- modular and offline-first
- safe destructive operations
- easy to test
- tolerant of OEM differences
- no feature may silently gain sensitive permissions
- easy for AI coding agents to navigate

## Suggested structure

```text
app/
core/
  common/
  ui/
  designsystem/
  database/
  datastore/
  permissions/
  files/
  media/
  security/
  analytics/
  ads/
  billing/
  testing/
feature/
  home/
  checkup/
  cleaner/
  largefiles/
  screenshots/
  duplicates/
  cleanupswipe/
  downloads/
  applock/
  vault/
  notifications/
  doctor/
  hardwaretests/
  appusage/
  imagetools/
  pdftools/
  archives/
  qr/
  network/
  compass/
  level/
  flashlight/
  magnifier/
```

Start with package-by-feature inside a small number of Gradle modules. Do not create dozens of Gradle modules before build-time/ownership benefits justify them.

## UI architecture

- Jetpack Compose + Material 3
- unidirectional data flow
- ViewModel per screen/flow
- immutable UI state
- explicit actions/events
- no business logic in Composables

Example:

```kotlin
data class LargeFilesUiState(
    val isLoading: Boolean = false,
    val files: List<LargeFileItem> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val totalSelectedBytes: Long = 0,
    val error: String? = null,
)

sealed interface LargeFilesAction {
    data object Scan : LargeFilesAction
    data class Toggle(val id: String) : LargeFilesAction
    data object DeleteSelected : LargeFilesAction
}
```

## Data layers

Platform sources:
- MediaStore
- Storage Access Framework
- UsageStatsManager
- NotificationListenerService
- BatteryManager
- ActivityManager
- SensorManager
- ConnectivityManager

Repositories translate raw Android APIs into stable domain models.

Persistence:
- Room for cache/history
- DataStore for preferences
- app-private encrypted storage for Vault

Do not create meaningless one-line use cases just to imitate “clean architecture.”

## Storage scanner

```text
UI
 ↓
ScanCoordinator
 ↓
MediaRepository ──→ MediaStore
 ↓
AnalysisPipeline
 ├─ large-file classifier
 ├─ screenshot classifier
 └─ duplicate fingerprint queue
 ↓
Room cache
 ↓
Flow<Result>
```

Requirements:
- cancellation
- progress
- incremental results
- no UI-thread scanning
- cached expensive fingerprints
- bounded memory
- handle files disappearing during scan

## Deletion

All destructive operations go through one `DeleteCoordinator`.

```text
DeleteRequest
  uris
  reason
  sourceFeature
       ↓
DeleteCoordinator
  validate
  build platform request
  request user approval
  execute
  verify
  emit result
```

Feature code must not invent its own unsafe deletion path.

## Permissions

Central `PermissionCoordinator` handles permissions and special access.

Every access request contains:
- feature
- purpose
- disclosure
- current state
- request/settings action
- fallback behavior

Never ask for unrelated permissions together.

## Vault

```text
Select source
 ↓
Validate
 ↓
Encrypt stream (AES-GCM)
 ↓
App-private destination
 ↓
Verify integrity/decryption
 ↓
Commit encrypted metadata
 ↓
Optionally request source deletion
```

Rules:
- source is never deleted before verified encrypted copy
- Keystore-backed master secret
- unique nonce per encrypted object
- versioned encrypted file format
- re-authenticate before export

## Notification history

`NotificationListenerService` performs minimal work:
- normalize event
- persist/enqueue
- return

Retention cleanup runs through WorkManager.

Never send notification content to analytics/logging.

## App Lock

Treat App Lock as an isolated subsystem because it is OEM- and policy-sensitive.

Interfaces:
- `ForegroundAppObserver`
- `AppLockPolicy`
- `LockSessionManager`
- `LockScreenController`

Feature flag:
`APP_LOCK_ENABLED`

This lets us disable App Lock from a Play build if review/reliability is unacceptable without rewriting the rest of Tooliva.

## Ads

Feature screens never import the ad SDK directly.

```kotlin
interface AdController {
    fun canShowInterstitial(placement: AdPlacement): Boolean
    suspend fun showInterstitial(placement: AdPlacement): AdResult
}
```

No ads in Vault, PIN/biometric, permission explanations or destructive confirmations.

## Billing

Entitlements:
- `FREE`
- `PRO_LIFETIME`

Features query `EntitlementRepository`, not Google Billing directly.

## Analytics

Only allow-listed events.

Never include:
- filename/path
- notification text
- vault data
- photo fingerprints
- QR contents

## Dependency rule

Before adding a library, check:
1. why it is needed;
2. whether AndroidX/platform is enough;
3. manifest permissions;
4. collected data;
5. Data Safety impact;
6. maintenance;
7. license;
8. binary-size impact.
