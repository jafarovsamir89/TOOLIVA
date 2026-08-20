# Tooliva — Technical Specification

Version: 0.3 — direct-cleaner architecture  
Revision: 2026-08-18  
Platform: Android  
Product: Cleaner + File Manager + Device Tools

Authoritative context:
- `docs/PRODUCT_CONSTITUTION.md`
- `docs/DECISION_LOG.md`
- `AGENTS.md`

## 1. Product goal

Build a best-in-class Android storage Cleaner and real File Manager first, then add trustworthy device-maintenance and retention utilities.

The core Cleaner must:

- scan accessible shared storage beyond the gallery;
- progressively surface useful results;
- find large files, downloads, APKs, archives, documents, media, screenshots and old-file candidates;
- explain why each cleanup candidate is shown;
- allow safe review/open/select/delete flows;
- verify what changed;
- show a truthful Cleanup Receipt;
- remain simple enough that a normal user never needs to understand internal Android/database architecture.

## 2. Non-goals

Do not implement/claim:

- fake RAM boost;
- fake CPU cooling;
- fake antivirus warnings;
- invented battery-health score;
- mysterious fake junk totals;
- silent deletion;
- hidden root/exploit behavior;
- automatic private-cache wiping claims;
- hidden Accessibility automation;
- mandatory account/backend for core tools;
- ad-gated scan results;
- architecture whose only justification is hypothetical future scale.

## 3. Product modules and order

### P0 core

1. CLEAN
2. FILES
3. APPS / maintenance
4. DIAGNOSE / CHECKUP
5. NOTIFICATION HISTORY

### Later

- Vault
- App Lock
- image/PDF tools
- QR/network/sensor mini-tools
- monetization polish

Do not build later modules while the Cleaner/File Manager regression/recovery work remains unresolved.

# 4. Storage access model

## 4.1 Full Mode — preferred modern Cleaner/File Manager path

On Android 11+ when granted:

`MANAGE_EXTERNAL_STORAGE`

Use Full Mode for the shared-storage functions it enables:

- Large Files;
- Downloads;
- APK/archives/documents;
- media cleanup;
- Screenshot Cleaner;
- File Manager;
- on-device file search;
- storage analysis.

Implementation requirements:

- truthful pre-permission disclosure;
- open correct Special App Access page;
- `Environment.isExternalStorageManager()` state detection;
- handle grant/deny/revoke;
- no attempt to enter Android-protected paths through bypasses;
- production remains contingent on current Google Play declaration approval.

### Critical UX rule

If Full Storage Access is already granted, Cleaner submodules must not ask for redundant broad Photos/Videos permission for the same storage-cleaning job.

## 4.2 Limited Mode

When Full Mode is denied/not supported:

- MediaStore for media-focused flows;
- SAF for user-mediated files/folders;
- granular media permissions only when a Limited Mode media feature genuinely needs them.

Limited Mode UI explicitly states reduced coverage.

# 5. Storage domain

Retain simple provider abstraction:

```kotlin
interface StorageProvider {
    val accessMode: StorageAccessMode
    fun scan(request: StorageScanRequest): Flow<StorageScanEvent>
    suspend fun stat(ref: StorageRef): StorageEntry?
}
```

Core models describe:

- ref/path identity;
- name;
- extension/MIME;
- size;
- modified time;
- category;
- parent/volume where useful;
- file/directory status.

Providers may include:

- `FullStorageProvider`
- `MediaStoreStorageProvider`
- `SafStorageProvider` when needed

Features consume domain models, not Android cursor/filesystem details directly.

# 6. Cleaner engine

## 6.1 Main architecture

The primary Cleaner path is direct and progressive:

```text
User taps Scan / opens explicit scan action
        ↓
StorageProvider
        ↓
progressive cancellable traversal/query
        ↓
lightweight classifier pipeline
        ├─ Large Files
        ├─ Downloads
        ├─ APK
        ├─ Archives
        ├─ Documents
        ├─ Media
        ├─ Old Files
        └─ Explainable candidate rules
        ↓
UI updates as matches are discovered
```

### Requirements

- no filesystem work on main thread;
- results appear progressively;
- user can cancel;
- single unreadable/disappearing file does not abort the scan;
- avoid symlink/path loops where applicable;
- do not materialize the whole phone tree before producing results;
- do not hash file contents during normal scan;
- do not decode/generate thumbnails for all files;
- do not persist every discovered file to Room as a mandatory step;
- do not auto-start heavy whole-storage traversal simply by navigation.

## 6.2 Persistence rule

Room is optional infrastructure, not the primary Cleaner gateway.

Acceptable persistence cases:

- duplicate fingerprint cache;
- Notification History;
- saved cleanup decisions/history;
- explicit scan history/receipts if product value is demonstrated;
- a small measured cache only after real performance evidence.

Rejected:

```text
filesystem -> mandatory whole-device Room index -> active generation -> UI
```

for ordinary Large Files/Cleaner discovery.

# 7. Cleaner screen UX

The Clean screen should expose consumer concepts only.

Top:

- storage used / total / available;
- one clear `SCAN` or `ANALYZE` action when a scan is required;
- access status only when useful/actionable.

Result/category cards:

- Junk & leftovers (only explainable rules)
- Large Files
- Downloads
- APK installers
- Archives
- Documents
- Screenshots
- Old Files
- Exact Duplicates
- Cache
- Apps

Cards display real item count and/or bytes when known.

Do not expose:

- Room/index terminology;
- generations/scopes;
- database progress;
- coordinator internals.

# 8. Large Files

Full Mode target: every accessible shared-storage file type.

Default size thresholds:

- 100 MB+
- 500 MB+
- 1 GB+

Categories:

- All
- Video
- Image
- Audio
- APK
- Archive
- Document
- Download
- Other

Actions:

- search by name/path;
- sort size/newest/oldest/name;
- select/multi-select/select-all-visible;
- open;
- share;
- details;
- delete/trash;
- later locate in File Manager.

### UX behavior

A user-initiated scan progressively appends matching files.

The screen must not wait for completion of a whole-device database generation.

Opening Large Files must not automatically trigger a heavy full scan unless explicitly approved later after measurements.

# 9. Downloads / APK / Archives / Documents

## 9.1 Downloads

Automatic Full Mode analysis of the Downloads area.

Subcategories:

- APK installers;
- archives;
- documents;
- media;
- old downloads;
- large downloads.

## 9.2 APK installers

Show where safely available:

- filename;
- size;
- modified time;
- label/package/version via safe APK parsing;
- installed relation only when package visibility supports it.

Never preselect an APK solely because it is old.

## 9.3 Archives

Classify common archive extensions/MIME such as ZIP/RAR/7Z when identifiable.

Show size/date/path + open/share/details/delete.

## 9.4 Documents

Classify PDF/Office/text documents.

Never call a normal document junk merely because it is old/large.

# 10. Screenshot Cleaner

Requirements:

- use Full Mode storage access when already granted and sufficient;
- use MediaStore/granular media permission only as Limited Mode fallback where required;
- detect screenshot buckets/path patterns;
- filters 30/90/365 days;
- real thumbnails;
- multi-select;
- Select all;
- central delete/trash pipeline;
- immediate Cleanup Receipt.

Do not require the user to grant Full Storage Access and then separately grant photos just to use the Full Mode screenshot cleanup path.

# 11. Old Files

User-controlled age filters:

- 30
- 90
- 180
- 365 days

Prioritize categories where age is meaningful for review:

- Downloads;
- APK installers;
- screenshots;
- archives;
- user-selected folders.

Do not call a file `unused` without real usage evidence.

# 12. Explainable Junk

Junk is a grouping of deterministic cleanup reasons, not a magic scanner.

Every candidate includes:

- reason id;
- user-facing explanation;
- risk/confidence;
- whether default selection is allowed.

Examples:

- known accessible temp artifact;
- obsolete installer candidate with clear reason;
- empty writable folder;
- exact duplicate;
- old screenshot;
- old download under user-selected rule.

Ambiguous files are not preselected.

# 13. Exact Duplicates

Do not hash the entire phone blindly.

Pipeline:

1. pre-group by size;
2. optional cheap metadata reduction;
3. hash only candidate groups;
4. verify exact content;
5. cache fingerprints in Room where metadata proves cache validity;
6. group exact duplicates;
7. review/delete through central cleanup pipeline.

Support accessible file types, not only photos.

Similar photos are a separate later feature and must never be labelled duplicates.

# 14. Cache cleanup

V1 target on Android 11+:

`StorageManager.ACTION_CLEAR_APP_CACHE`

Requirements:

- Full Mode/access prerequisites handled truthfully;
- launch official system-mediated flow;
- explain Android performs/controls the action;
- no fake cache-size promises;
- handle cancel/error/unsupported OEM behavior.

Cache Cleaner v2 may use `StorageStatsManager.queryStatsForPackage` after explicit Usage Access to show Android-provided `StorageStats.cacheBytes` for discovered browsers and installed YouTube. It must remain user-started, off the main thread, tolerant of per-package failures, and must show unavailable rather than fake zero values.

The selected-app AccessibilityService experiment was removed after Xiaomi validation showed unreliable behavior. Cache Cleaner opens Android App Info manually for the user; it does not automate Settings, press cleanup controls or claim a cleanup result.

# 15. File Manager

File Manager is genuine core functionality.

P0:

- shared/internal accessible volumes;
- folder browser;
- breadcrumbs;
- category shortcuts: Downloads/Documents/APKs/Archives/Images/Videos/Audio/Recent/Large;
- search;
- sort name/size/date;
- details;
- open/share;
- rename;
- create folder;
- copy;
- move;
- delete/trash;
- collision handling;
- long-operation progress/cancel.

File Manager should reuse `StorageProvider` and shared file-operation code where practical.

Implementation note (2026-08-19): File Manager v1 now uses `FullStorageProvider` direct volume/child browsing and explicit cancellable recursive searches. It does not create a Room index or scan the device on entry. File operations use streaming temp-file finalization; same-volume move attempts rename first and only deletes the source after a successful verified copy fallback. `FileProvider` is restricted to public user-facing shared-storage collections rather than exposing the storage root. Xiaomi end-to-end validation remains pending human PASS.

## 15.1 Exact Duplicates v1 implementation

Exact Duplicates is an explicit user-started operation only. It traverses accessible shared storage directly, ignores directories, zero-byte files, protected paths and Tooliva internal/temp files, groups regular files by exact byte size, hashes only groups with at least two files using sequential streaming SHA-256, and verifies hash matches byte by byte. After the Xiaomi user reported repeat-analysis latency, v1 now has a bounded private fingerprint cache keyed by path + size + modified time. It stores hashes and metadata only, never file contents, and reuses a hash only when that metadata is unchanged. Room, WorkManager, background crawlers and startup hashing remain excluded.

The result model separates potential recoverable bytes from the user's selected bytes. Selection starts empty, Keep this copy selects only the other copies, and normal duplicate cleanup refuses to remove the last current copy in a group. Selected files are revalidated for existence, size and modification time before the existing central delete coordinator and Cleanup Receipt are used.

It must not require completion of a whole-device Room index before basic browsing.

# 16. Storage Map — later Cleaner/File Manager differentiator

Storage Map may aggregate folder sizes during an explicit analysis and render a treemap/sunburst-like view plus accessible list fallback.

Do not make Storage Map a prerequisite for basic Cleaner/File Manager usage.

# 17. Cleanup safety / Receipt

The centralized cleanup architecture is authoritative.

Flow:

1. user review/select;
2. selected count + bytes;
3. system/user confirmation;
4. execute;
5. verify/re-stat;
6. show Cleanup Receipt immediately;
7. update visible results without an unnecessary full-device rescan.

Receipt states:

- requested;
- missing before;
- moved to Trash;
- physically freed;
- unchanged/failed;
- canceled;
- permission revoked.

Trash bytes are not physically freed bytes.

# 18. App Manager

App Manager v1 is a fast, user-started review screen for the apps Android makes visible to the package manager. It renders the basic inventory before enrichment and never performs a whole-device storage scan.

The inventory shows the label, icon, package name, version, install/update dates, enabled/launchable state and a flag-based user/system classification. Search matches label or package name. Filters are All/User/System/Rarely used; rarely-used review supports 30/90/180 days and only uses a real Android `lastTimeUsed` timestamp. Missing usage is unknown, not “old”. Sorts include name, install/update date, storage and usage, with unavailable values kept at the end.

Storage enrichment uses `StorageStatsManager` off the main thread only after Usage Access. The screen labels `appBytes`, `dataBytes` (which includes cache) and `cacheBytes` (a subset of data), and reports total only as App + Data. It never adds cache a second time or replaces an unavailable value with zero. Usage enrichment uses `UsageStatsManager` and remains local; the app content is never read.

Details provides Open, Android App info and, for removable user apps only, the normal Android uninstall confirmation. System apps and Tooliva itself are not offered as normal uninstall candidates. Bulk review requests each uninstall through Android sequentially; canceling one leaves it installed and the list refreshes after the queue completes.

The first implementation deliberately uses narrower package visibility and does not add `QUERY_ALL_PACKAGES`. A Xiaomi comparison of visible apps versus Android Settings is a separate human measurement gate. Only a documented core gap plus explicit human approval can change that decision.

# 19. Phone Optimizer / Phone Doctor / Check My Phone

Phone Optimizer owns the existing `StorageManager.ACTION_CLEAR_APP_CACHE` system-mediated action. It also shows real `ActivityManager.MemoryInfo` values: total RAM, available memory, a non-negative used estimate and low-memory pressure. Before/after readings are presented as device readings, never as RAM freed by Tooliva. No process killing, force-stop, Settings automation or fake boost is allowed.

## 19.1 Phone Doctor / Check My Phone

Show real platform facts:

- device/model/Android/security patch;
- ABI/CPU facts through public APIs;
- RAM/storage;
- battery level/state/source/voltage/temperature/current where exposed;
- thermal state;
- sensors;
- display facts;
- guided hardware tests.

Phone Doctor reads public local Android APIs only. Unavailable values are shown as unavailable. Battery health is the Android-reported health constant when exposed, never a computed percentage. Sensor listeners exist only while live values or a sensor test is visible and are unregistered when leaving the screen.

Hardware Tests are explicit, user-started checks. Physical results are never auto-passed: display and touch require user confirmation; vibration, flashlight, speaker and microphone have explicit controls; missing hardware is `Not supported`; microphone audio is an in-memory live level only and is not saved or uploaded.

Check My Phone is a lightweight aggregator. `Run checkup` collects quick public facts and known local hardware-test results. It does not run duplicate hashing, recursive scans, thumbnail generation or a startup/background index. It provides links to existing Cleaner/File Manager modules instead.

`CHECK MY PHONE` becomes an action plan composed from existing modules, not a fake health score.

# 20. Notification History

Notification History v1 is an explicit opt-in local feature. The UI shows a prominent disclosure before opening API-aware Notification Access settings. On Android 11+ it prefers the component detail settings intent and falls back to the listener settings list when needed.

The manifest listener uses `BIND_NOTIFICATION_LISTENER_SERVICE` and the platform service interface. Android callbacks are normalized immediately and persisted on a single IO coroutine lane. The Room database is limited to notification history; it stores safe text fields and metadata only, never raw `Parcelable` extras, images, `PendingIntent` objects or secrets. Tooliva's own package is excluded, ongoing notifications are excluded by default, and the same active `StatusBarNotification.key` updates one active row until removal instead of creating duplicates.

The screen supports local search, All/Today/7 days/30 days/Pinned and app filters, details, pin/delete, pause, include-ongoing, retention (1/7/30/90 days or until deleted), excluded apps with keep/delete-existing choice, per-app/all clear and truthful access-revoked behavior. Pinned rows survive retention pruning. Notification content never enters logs, analytics, ads, backend or backup; the database and preferences are excluded from Android backup.

# 21. Storage Map v1

Storage Map starts only from an explicit Analyze action and uses the existing `FullStorageProvider` scan. It aggregates folder totals, direct bytes, file counts and warnings in memory; it does not create a whole-device Room index, read file contents, hash files or generate thumbnails. Progress reports files checked, folders found, bytes counted and skipped warnings. The UI provides real map/list views, drill-down, breadcrumbs/parent/system Back, folder details, Open in Files and explicit delete through the existing file operation/Cleanup Receipt path. A deletion marks the visible map stale and never launches an automatic rescan.

# 22. Cleanup Swipe v1

Cleanup Swipe has an explicit picker for screenshots, images, videos, Downloads and files at least 100 MiB. Each category loads only after the user taps it and reuses existing direct scanners; no new storage permission is introduced. The in-memory session is ordered newest/oldest/largest/smallest and offers Keep, Delete and Skip buttons, horizontal Keep/Delete gestures, undo, selected count/bytes, final review, unselect, details and Open in Files. No card action deletes immediately. After an explicit final confirmation, selected files go through the central file operation coordinator and the same verified Cleanup Receipt, including missing-file, partial-failure and permission-revoked outcomes.

# 21. Vault / App Lock / Tools

Later unless explicitly reprioritized.

Vault:

- Keystore-backed authenticated encryption;
- PIN/biometric;
- verify copy before source deletion;
- safe export;
- auto-lock.

App Lock:

- only after reliability and Play path validated;
- Accessibility not approved by default.

Tools later:

- image compress/resize/convert;
- EXIF privacy clean;
- images-to-PDF;
- QR;
- network tools;
- compass/level/flashlight.

# 22. Performance rules

Measure before optimizing.

Key user metrics:

- time from explicit scan tap to first useful result;
- time to complete targeted scan;
- UI responsiveness;
- cancellation latency;
- memory use;
- deletion/verification latency.

No architecture change is considered successful if it worsens user-perceived speed/control on the physical test device.

# 23. Testing process

Automated by agent:

- unit tests;
- compilation;
- debug build;
- instrumentation/connected tests where appropriate;
- static regression checks;
- install fresh debug APK;
- crash smoke-check.

Manual by human user:

- permissions;
- scan UX/speed;
- result correctness;
- navigation;
- open/share/delete flows;
- OEM/system dialogs;
- subjective responsiveness.

After device-dependent implementation the agent must stop and provide `MANUAL TEST REQUIRED` checklist before new major work.

# 24. Current recovery milestone

Before new Cleaner categories:

1. remove/revert the mandatory Storage Index experiment introduced by `7836ea` and `71f35ca` without losing unrelated good work;
2. restore the direct progressive scan behavior represented by `b767aa8`;
3. remove technical index UI from Clean;
4. stop automatic heavy scan on Large Files navigation;
5. make Full Mode the single primary storage permission path for Cleaner submodules on Android 11+;
6. make Screenshot Cleaner use Full Mode when granted instead of immediately requiring separate media permission;
7. build/install;
8. human Xiaomi regression test;
9. only after PASS proceed to Downloads/APK/Archives/Documents/Old Files/Cache.

# 25. Release principles

Do not ship until:

- no P0 destructive bug;
- Full Mode works on modern Android;
- Limited Mode is honest/useful;
- Cleaner/File Manager core is fast and understandable;
- Play restricted-permission declaration package is prepared;
- privacy/Data Safety match the build;
- ads do not interrupt core/sensitive flows;
- Xiaomi/Samsung/Pixel coverage reaches required gate;
- closed test shows no critical file-loss/regression issues.
