# Tooliva — Architecture

Revision: 2026-08-18

Authoritative product context:
- `docs/PRODUCT_CONSTITUTION.md`
- `docs/DECISION_LOG.md`

## 1. Architecture goal

Use the simplest architecture that safely supports the current Tooliva product.

Priorities:

- direct user value;
- progressive Cleaner results;
- safe file operations;
- Full/Limited storage modes;
- no mandatory whole-device index;
- offline-first;
- easy manual verification on real devices;
- policy/permission isolation;
- low regression risk.

## 2. Package direction

```text
app/
core/
  common/
  ui/
  designsystem/
  storage/
    access/
    model/
    full/
    mediastore/
    saf/
    operations/
    cleanup/
  database/        # only for features with real persistence needs
  security/
  analytics/
  ads/
  billing/
  testing/
feature/
  home/
  cleaner/
    largefiles/
    downloads/
    installers/
    archives/
    documents/
    oldfiles/
    screenshots/
    duplicates/
    cleanupswipe/
    result/
  files/
  appmanager/
  apps/
  doctor/
  checkup/
  notifications/
  vault/
  applock/
  tools/
```

Do not create many Gradle modules before build/test/ownership boundaries justify them.

## 3. UI architecture

- Jetpack Compose + Material 3
- unidirectional data flow
- immutable UI state
- ViewModel per coherent flow
- business/platform logic outside Composables
- explicit user actions
- no hidden heavy scan triggered by navigation unless explicitly approved
- progressive UI updates when scan results arrive

## 4. Storage access state

Central `StorageAccessCoordinator` owns special-access detection/navigation.

Modern Android model:

### Full Mode

`MANAGE_EXTERNAL_STORAGE` granted.

This is the primary Cleaner/File Manager storage mode on Android 11+.

### Limited Mode

Full access denied/not available.

Use MediaStore/SAF and granular permissions only for the feature that needs them.

Critical rule:

If Full Mode is already granted, a Cleaner submodule must not independently force a second broad Photos/Videos permission for the same shared-storage job.

## 5. Core storage models

Example direction:

```kotlin
enum class StorageAccessMode { FULL, LIMITED }

data class StorageEntry(
    val ref: StorageRef,
    val name: String,
    val path: String?,
    val sizeBytes: Long,
    val modifiedAtMillis: Long?,
    val mimeType: String?,
    val extension: String?,
    val category: StorageCategory,
    val isDirectory: Boolean,
    val volumeId: String?,
)
```

Use provider-neutral identity where practical.

## 6. StorageProvider

Primary abstraction:

```kotlin
interface StorageProvider {
    val accessMode: StorageAccessMode
    fun scan(request: StorageScanRequest): Flow<StorageScanEvent>
    suspend fun stat(ref: StorageRef): StorageEntry?
}
```

Implementations:

- `FullStorageProvider`
- `MediaStoreStorageProvider`
- `SafStorageProvider` only where needed

Provider scan requirements:

- IO dispatcher/background work;
- progressive events;
- cancellation;
- no full-tree accumulation before emission;
- isolate unreadable/disappearing files;
- avoid protected paths and loop hazards;
- cheap metadata first.

## 7. Cleaner pipeline

Target architecture:

```text
User taps Scan
      ↓
StorageProvider.scan()
      ↓
StorageScanEvent.EntryFound
      ↓
ClassifierPipeline
  ├─ LargeFileClassifier
  ├─ DownloadClassifier
  ├─ ApkClassifier
  ├─ ArchiveClassifier
  ├─ DocumentClassifier
  ├─ OldFileClassifier
  └─ ExplainableRuleClassifier
      ↓
Feature/UI state updates progressively
```

A classifier should do the minimum work needed for its result.

Examples:

- Large File: size threshold only
- APK: extension/MIME + optional safe metadata parsing later
- Archive: extension/MIME
- Old File: metadata age + user-defined scope
- Exact Duplicate: NOT part of ordinary scan; separate candidate/hash pipeline

## 8. Rejected index-first pipeline

Do not restore this as primary flow:

```text
filesystem
 -> full traversal
 -> Room index generation
 -> active scope promotion
 -> Room query
 -> Large Files UI
```

This architecture was tested on Xiaomi and caused product regressions.

The code introduced by `7836ea` and `71f35ca` may be reverted/removed or salvaged only for independent reusable pieces that do not reintroduce the mandatory gateway.

## 9. Persistence policy

Room exists only for concrete persistent-state needs.

Good candidates:

- duplicate fingerprints;
- Notification History;
- saved cleanup receipts/history if product value is chosen;
- app preferences requiring structured data;
- a small optional cache after measured evidence.

Bad reason:

> “We may need a full phone index later.”

Do not write every ordinary scan entry to Room by default.

## 10. Large Files

Large Files should consume progressive direct scan results.

Flow:

```text
LargeFilesViewModel
 -> choose provider from access state
 -> provider.scan(minSize=threshold floor)
 -> append/update matching entries as found
 -> user filter/sort/search
```

No mandatory database snapshot.

Opening the screen must not automatically start expensive whole-storage work unless explicit product decision changes this later.

## 11. Cleaner multi-classifier scan

When implementing the main Cleaner scan, prefer one traversal feeding multiple cheap classifiers rather than N independent full traversals.

However, do not build a complex event bus/framework before at least two/three active classifiers need it.

A simple coordinator can own:

- current scan Job;
- provider;
- cancellation;
- classifier list;
- aggregate counts;
- progressive category summaries.

No persistence requirement.

## 12. Screenshot Cleaner

Access selection:

```text
if Full Mode granted
 -> use Full Mode storage/path discovery suitable for screenshots
else
 -> MediaStore Limited Mode
 -> request granular photo/media permission only if needed
```

Thumbnail loading may still use platform APIs/FileProvider/content URIs as appropriate.

Deletion uses central cleanup infrastructure.

## 13. Duplicate architecture

Separate from ordinary Cleaner scan:

```text
cheap candidate discovery
 -> group by size
 -> hash only groups with 2+ candidates
 -> verify exact equality
 -> persist fingerprint cache only where useful
 -> duplicate groups UI
```

Hash work:

- bounded;
- cancellable;
- never main thread;
- cached fingerprint invalidated when size/modified identity changes.

## 14. File operations

Cleaner and File Manager share operation primitives where practical.

Core operations:

- stat/open/share;
- rename;
- create folder;
- copy;
- move;
- delete/trash.

Copy/move later require:

- progress;
- cancel;
- collision strategy;
- insufficient-space handling;
- source verification before destructive move fallback.

## 15. Delete/CleanupCoordinator

The existing central cleanup pipeline is permanent.

```text
Selection
  ↓
Prepare / re-stat
  ↓
User/system confirmation
  ↓
Execute
  ↓
Verify
  ↓
Cleanup Receipt
  ↓
Reconcile current visible results only
```

Do not force a full-device rescan after every cleanup solely to update the current list.

## 16. Cleanup Receipt model

Must distinguish:

- requested;
- missing before;
- moved to Trash;
- physically freed;
- unchanged/failed;
- canceled;
- permission revoked.

This is a product-level invariant.

## 17. File Manager

Basic browser should be direct:

```text
current folder/category
 -> StorageProvider / filesystem operation
 -> children/results
 -> UI
```

Do not require a global database index for simple folder browsing.

Global search can initially perform targeted/direct search. If measured performance later justifies a cache/index, design it as optional optimization with explicit decision.

## 18. Explainable rule engine

Only introduce a shared rule interface when multiple real rules exist.

Possible contract:

```kotlin
interface CleanupRule {
    fun evaluate(entry: StorageEntry, context: RuleContext): CleanupCandidate?
}
```

Candidate contains:

- reason;
- risk/confidence;
- default-selected policy;
- source rule.

Do not create a large generic rules framework before real rules exist.

## 19. App Manager

Separate domain from files. `core/appmanager` owns the visible `PackageManager` inventory, Usage Access/`UsageStatsManager` and off-main-thread `StorageStatsManager` adapters. `feature/appmanager` owns immutable Compose state, progressive enrichment, selection and the sequential Android uninstall request queue.

The list renders basic metadata first; storage and usage are progressively enriched with one storage query at a time. Icons are loaded by package name in the UI with the platform default fallback. App details actions use Android intents rather than private Settings automation.

Only add `QUERY_ALL_PACKAGES` after:

- a real core feature gap is demonstrated;
- policy is rechecked;
- explicit decision recorded.

## 20. Cache cleanup adapter

Isolate official system action behind a tiny adapter and expose it through Phone Optimizer, not the per-app Cache Cleaner.

Return supported/launched/error/canceled status where observable.

Cache Cleaner v2 separately uses browser intent discovery, Usage Access and a background `StorageStatsManager` reader. Cleanup uses direct Android App Info intents and leaves the final Clear cache action to the user; there is no Settings automation/session service in the current build.

No fake direct-private-cache access abstraction and no `QUERY_ALL_PACKAGES`.

## 21. Phone Doctor

Platform adapters remain small:

- battery;
- memory;
- thermal;
- sensors;
- network;
- device facts.

`Check My Phone` orchestrates existing module outputs instead of creating a second scanning system.

## 22. Notification History

Room is appropriate here because persistence is the feature itself, not because the Cleaner needs an index. `NotificationListenerService` performs only safe extraction on callback, then persists on a serialized IO scope. The repository owns access detection, API-aware settings intents, active-key deduplication, retention and exclusion rules. UI observes DAO flows and never touches raw notification objects.

Only normalized local notification rows are stored. Raw extras/images/PendingIntents are discarded, Tooliva's package and ongoing rows are filtered according to settings, and backup rules exclude the database/preferences. Access revoke leaves existing rows visible but stops new capture.

## 23. Storage Map and Cleanup Swipe

Storage Map is a direct explicit `FullStorageProvider` aggregation that retains folder totals rather than file records. It is cancellable, progressive and independent of Room/WorkManager. Its UI has map and accessible list representations with drill-down and explicit file actions.

Cleanup Swipe is an in-memory review state machine over existing `StorageEntry` sources. Decisions are reversible during review; final deletion is a separate confirmation and delegates to `FileOperationCoordinator`, so missing files, failures and verified Cleanup Receipt semantics stay centralized.

## 24. Analytics/privacy

Allow-list aggregate events only.

Never include:

- filename/path;
- file content;
- hashes tied to user content;
- installed-app inventory;
- notification text;
- Vault contents.

## 25. Performance discipline

Measure:

- explicit scan tap -> first useful result;
- scan duration for targeted feature;
- memory;
- ANR/jank;
- cancellation latency;
- delete verification latency.

Only optimize after a real bottleneck is measured.

## 26. Testing boundary

Automated testing verifies deterministic technical behavior.

Human Xiaomi testing verifies:

- perceived responsiveness;
- permission UX;
- OEM/system dialogs;
- scan correctness;
- navigation;
- file open/delete behavior.

The agent installs APK and stops for manual checklist before proceeding to the next major slice.
