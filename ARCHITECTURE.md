# Tooliva — Architecture

Revision: 2026-08-16

## Goals

- Cleaner + File Manager share one coherent storage domain
- Full Storage Mode and Limited Mode are first-class, testable states
- offline-first
- progressive/cancellable storage work
- safe destructive operations with verified receipts
- scalable local index
- tolerant of OEM/Android-version differences
- easy for AI agents to navigate
- restricted permissions isolated and auditable

## Suggested package structure

```text
app/
core/
  common/
  ui/
  designsystem/
  database/
  datastore/
  access/
  storage/
    model/
    index/
    full/
    mediastore/
    saf/
    operations/
    cleanup/
  packages/
  security/
  analytics/
  ads/
  billing/
  testing/
feature/
  home/
  cleaner/
    dashboard/
    largefiles/
    downloads/
    installers/
    oldfiles/
    screenshots/
    duplicates/
    cleanupswipe/
    result/
  files/
    browser/
    search/
    storagemap/
  apps/
  checkup/
  doctor/
  notifications/
  vault/
  applock/
  imagetools/
  pdftools/
  qr/
  network/
  sensors/
```

Start package-by-feature in a small number of Gradle modules. Split modules only when ownership/build/test boundaries justify the cost.

---

# UI architecture

- Jetpack Compose + Material 3
- unidirectional data flow
- immutable screen state
- explicit user actions/events
- ViewModel per coherent flow
- platform/file operations behind repositories/coordinators
- no filesystem traversal in Composables

For long lists, destructive actions may use a fixed bottom action area, matching the current Cleaner UX.

---

# Storage domain

## Core models

Suggested concepts:

```kotlin
enum class StorageAccessMode {
    FULL,
    LIMITED,
}

data class StorageEntry(
    val ref: StorageRef,
    val name: String,
    val pathLabel: String?,
    val kind: StorageKind,
    val sizeBytes: Long,
    val modifiedAtMillis: Long?,
    val mimeType: String?,
    val extension: String?,
    val isDirectory: Boolean,
    val volumeId: String?,
)
```

`StorageRef` must support provider-specific identities without forcing every feature to depend on a raw filesystem path.

## Provider interface

```kotlin
interface StorageProvider {
    val accessMode: StorageAccessMode
    fun scan(request: StorageScanRequest): Flow<StorageScanEvent>
    suspend fun search(request: StorageSearchRequest): List<StorageEntry>
    suspend fun children(directory: StorageRef): List<StorageEntry>
    suspend fun stat(ref: StorageRef): StorageEntry?
}
```

Providers:
- `FullStorageProvider` — broad accessible shared-storage filesystem
- `MediaStoreStorageProvider` — Limited Mode/media-optimized flows
- `SafStorageProvider` — user-mediated file/tree access

Features consume domain models, not provider-specific cursor/path logic.

---

# Access state

Central `StorageAccessCoordinator` owns:
- Full Storage permission/special-access state
- Media permissions
- SAF grants
- current access mode
- disclosure/settings navigation
- revoke detection

State example:

```kotlin
data class StorageAccessState(
    val mode: StorageAccessMode,
    val allFilesAccessGranted: Boolean,
    val mediaAccess: MediaAccessState,
    val persistedSafGrants: List<SafGrant>,
)
```

Do not scatter `Environment.isExternalStorageManager()` checks across feature screens.

---

# Full Storage Provider

Responsibilities:
- enumerate accessible shared-storage roots/volumes
- traverse files incrementally
- normalize metadata
- respect exclusions/protected paths
- emit progressive scan events
- support cancellation
- avoid symlink/loop problems where applicable
- avoid following paths Tooliva cannot legitimately access

Use bounded IO concurrency. Do not recursively build the whole tree in memory before emitting results.

---

# Limited providers

The existing MediaStore scanner remains production code, not throwaway prototype.

Use it for:
- Limited Mode
- screenshot/media-specific optimized queries
- Android Trash requests where appropriate

SAF remains for:
- user-selected files/folders
- document operations where provider semantics are required
- fallback workflows.

The UI must accurately describe limited scan coverage.

---

# Local storage index

Room-backed index is recommended for deep scan/search.

Possible entities:
- `StorageIndexEntryEntity`
- `ScanSessionEntity`
- `FileFingerprintEntity`
- `CleanupReceiptEntity`

Index fields may include local-only:
- stable provider/ref identity
- normalized path/name
- type/category
- size/date
- volume
- last indexed metadata

Privacy rules:
- index stays local
- never send names/paths to analytics/ads
- clear/rebuild when storage access changes materially

## Index pipeline

```text
StorageProvider
      ↓
ScanCoordinator
      ↓
Normalizer / Exclusion Rules
      ↓
Room Index  ←→  Incremental Change Strategy
      ↓
Analysis Pipeline
  ├─ category aggregation
  ├─ large files
  ├─ old files
  ├─ candidate rules
  └─ duplicate pre-groups
      ↓
UI Flows / Search / Storage Map
```

Do not perform expensive hashing during the initial cheap index pass unless needed.

## Storage Index v1 implementation contract

The first Room index uses three local tables: indexed entries, active access-mode/volume
scopes, and scan generations. The first run has two ordered generations: a small priority
snapshot for high-value shared-storage directories, followed by the complete deep generation.
The priority snapshot is promoted only after its selected roots report successful completion,
so Large Files can show useful rows while the complete generation is still running. The deep
generation later replaces that snapshot and performs stale cleanup for each successfully
completed full root. Cancelled or failed generations therefore cannot replace the last known-
good scope.

Full and Limited entries are scoped separately by `StorageAccessMode`. If Full Storage Access is
revoked, Large Files queries use only the Limited scope after a Limited scan; old Full entries
remain stale informational data and are not used for file actions. Index rows contain metadata
and local references only—never file contents or thumbnails.

Indexing uses a single cancellable filesystem traversal and bounded Room batches. Unchanged
metadata (`stable key`, path/ref, size and modified time plus normalized fields) reuses the
existing row and only advances its generation marker. No hashing or file-content reads are
performed. A process-scoped `StorageIndexCoordinator` owns the fast/deep sequence so Clean and
Large Files cannot start parallel walks. Large Files reads the active Room snapshot immediately
and refreshes as the deep generation enriches it. Feature queries are Room-filtered and bounded
rather than loading the whole table into Compose.

---

# ScanCoordinator

Responsibilities:
- start/cancel scan
- report progress/phase
- aggregate byte/file counts
- choose provider based on access state
- persist session metadata
- isolate errors per path/volume where possible

Suggested events:
- Started
- RootDiscovered
- EntryIndexed
- CategoryUpdated
- Progress
- Warning
- Completed
- Canceled

A single unreadable file/folder must not abort the entire scan.

---

# Cleanup rule engine

`Junk` must be explainable.

Suggested contract:

```kotlin
interface CleanupRule {
    val id: String
    val risk: CleanupRisk
    suspend fun evaluate(entry: StorageEntry, context: RuleContext): CleanupCandidate?
}

data class CleanupCandidate(
    val entry: StorageEntry,
    val reason: CleanupReason,
    val defaultSelected: Boolean,
    val confidence: CleanupConfidence,
)
```

Rules are deterministic/testable.

Examples:
- old APK installer
- old screenshot
- exact duplicate
- accessible temp artifact
- empty accessible folder

Normal documents are not generic junk.

---

# Duplicate architecture

Pipeline:

```text
Index entries
  ↓ group by size
Candidate groups
  ↓ optional cheap metadata
HashQueue
  ↓ bounded IO hashing
Fingerprint cache
  ↓
Exact duplicate groups
```

Requirements:
- cancellable
- cache-aware
- file mutation validation before trusting a cached fingerprint
- no full-file hashing on main thread
- similar-photo analysis is a separate feature/model

---

# File operations

Cleaner and File Manager share operation infrastructure.

Suggested interfaces:
- `FileOperationCoordinator`
- `CopyOperation`
- `MoveOperation`
- `RenameOperation`
- `CreateDirectoryOperation`
- `DeleteCoordinator`

## Destructive pipeline

```text
Selection
  ↓
Prepare / re-stat
  ↓
User/system confirmation
  ↓
Execute
  ↓
Verify/re-stat/re-index
  ↓
CleanupReceipt
```

The existing MediaStore cleanup result accounting must be preserved and generalized carefully.

Receipt fields distinguish:
- requested
- missing before
- moved to Trash
- physically freed
- unchanged/failed
- canceled
- permission revoked

## Copy/move

Must support:
- collision policy: ask/skip/rename/replace where safe
- progress
- cancel
- partial result
- verification before deleting source during move fallback/copy-delete flows
- low-storage errors

---

# File Manager

Browser, global search and Cleaner use the same index/provider models.

Do not create a second independent file discovery implementation.

Browser state includes:
- current directory/category
- children
- selection
- sort/filter
- operation progress
- access mode

Global search primarily queries the local index, with refresh/revalidation when opening/actioning an item.

---

# Storage Map

Storage Map consumes aggregate/index data.

Represent folder/category hierarchy as a domain tree independent of visualization.

UI can render treemap/sunburst-like layout plus an accessible list fallback.

Never make visualization the only way to locate a file.

---

# App Manager

Keep package/application data in a separate domain from files, linked only where needed.

Interfaces may include:
- `InstalledAppRepository`
- `AppUsageRepository`
- `PackageVisibilityState`

Do not add `QUERY_ALL_PACKAGES` until the narrower prototype proves insufficient and docs/approval are updated.

Installed-app inventory stays local.

---

# Cache cleanup

Use an isolated system-action adapter around `StorageManager.ACTION_CLEAR_APP_CACHE`.

The adapter reports:
- supported/unsupported
- permission required
- launched
- canceled/failed where observable

Do not pretend this action gives Tooliva direct visibility into all private app caches.

---

# Phone Doctor / Checkup

Device information repositories remain separate platform adapters:
- battery
- thermal
- memory
- sensors
- network

`Check My Phone` orchestrates already-existing repositories and cleaner insights; it must not reimplement storage scanning.

---

# Notification History

`NotificationListenerService` performs minimal callback work:
- normalize
- persist/enqueue
- return

Retention through local jobs when needed.

No notification content in analytics/logging.

---

# Vault / App Lock

Vault:
- Keystore-backed authenticated encryption
- verify encrypted copy before source deletion
- isolated encrypted metadata

App Lock:
- isolated feature flag/subsystem
- no Accessibility implementation without explicit approval
- design for possible removal from Play build without affecting Cleaner/File Manager.

---

# Ads / billing

Feature packages do not directly depend on ad SDK.

No ad is allowed inside:
- access disclosure
- selection→delete flow
- system confirmation transition
- Cleanup Receipt
- Vault/authentication

Entitlements remain central (`FREE`, `PRO_LIFETIME`).

---

# Analytics

Allow-list events only.

Never include:
- filename/path
- extension tied to path/user content when unnecessary
- notification text
- file hash/fingerprint
- installed package inventory
- Vault/QR content.

Useful aggregate events can include:
- scan completed
- access mode
- duration bucket
- category opened
- cleanup completed
- operation error code

---

# Dependency rule

Before adding a library check:
1. why needed;
2. platform/AndroidX alternative;
3. permissions/manifests;
4. collected data;
5. Data Safety impact;
6. maintenance/security;
7. license;
8. binary-size cost.

Market competitors are product references, not a reason to import arbitrary cleaner SDKs.
