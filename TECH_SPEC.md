# Tooliva — Technical Specification

Version: 0.2 — market-driven rewrite  
Platform: Android  
Product: Cleaner + File Manager + Device Toolbox  
Research baseline: `docs/MARKET_RESEARCH_2026.md`

## 1. Product goal

Build a best-in-class Android device-maintenance utility whose **core purposes are storage cleaning and file management**, then retain users with diagnostics, privacy and everyday tools.

Tooliva must be useful in the way mainstream users expect a real cleaner to be useful:

- scan shared storage beyond the photo gallery;
- explain what occupies space;
- find large/old/duplicate/unnecessary candidates;
- surface APKs, archives, documents, downloads, media and other accessible shared-storage files;
- manage files safely;
- analyze installed applications where policy-approved;
- provide a system-mediated cache-cleaning flow;
- verify exactly what happened after cleanup.

The product must never sacrifice trust for fake optimization claims.

## 2. Product pillars

Priority order:

1. **CLEAN** — deep storage analysis and cleanup
2. **FILES** — full shared-storage file management/search
3. **DIAGNOSE / APPS** — device and application maintenance
4. **PROTECT** — notification history, Vault, App Lock if reliable
5. **TOOLS** — image/PDF/QR/network/sensor utilities

Cleaner and File Manager are core functionality, not side tools.

---

# 3. Non-goals / prohibited behavior

V1 must not implement or claim:

- fake RAM booster;
- fake CPU cooler;
- invented battery health percentage;
- fake virus alerts;
- fake `phone is damaged` warnings;
- silent deletion of user files;
- silent clearing of private app data;
- root-required behavior in the normal Play build;
- hidden Accessibility automation;
- hidden package/app inventory collection;
- server upload of scanned filenames/content;
- mandatory account creation;
- ad gating before showing a requested scan/result;
- manipulative weekly-subscription traps.

Every number displayed as reclaimable space must be explainable.

---

# 4. Storage access architecture

Tooliva uses two explicit modes.

## 4.1 Full Storage Mode — preferred cleaner mode

Target capability:
`MANAGE_EXTERNAL_STORAGE`

Reason:
Tooliva's core purpose includes file management, on-device file search and shared-storage maintenance. This mode is necessary to provide the full consumer cleaner experience across shared storage.

Capabilities where Android permits:
- enumerate shared-storage files/folders;
- APK/archive/document discovery;
- global file search;
- full-storage size/date/type indexing;
- file manager actions;
- deep storage visualization;
- official cache-clearing intent.

Requirements:
- prominent explanation before opening Special App Access;
- user explicitly enables All Files Access;
- app remains useful if denied;
- policy/declaration review before production Play submission;
- no attempt to bypass Android protected/private areas.

## 4.2 Limited Mode — mandatory fallback

Use:
- MediaStore;
- Storage Access Framework;
- user-granted document/tree URIs where appropriate.

Limited Mode must never pretend to be a full-device scan.

UI must clearly label when results are limited by Android permissions.

## 4.3 StorageProvider abstraction

Do not couple cleaner UI directly to one Android storage API.

Suggested domain interface:

```kotlin
interface StorageProvider {
    val accessMode: StorageAccessMode
    fun scan(request: StorageScanRequest): Flow<StorageEntry>
    suspend fun search(query: String, filters: FileFilters): List<StorageEntry>
    suspend fun resolve(uriOrPath: StorageRef): StorageEntry?
}
```

Implementations can include:
- `FullStorageProvider`
- `MediaStoreStorageProvider`
- `SafStorageProvider`

Existing MediaStore work must be preserved as fallback/reusable infrastructure.

---

# 5. CLEAN — primary product module

## 5.1 Deep Storage Scan

Primary CTA:
`SCAN STORAGE`

Scan output must include real accessible categories:

- reclaimable candidates;
- large files;
- downloads;
- APKs;
- archives: ZIP/RAR/7Z where identifiable;
- documents: PDF/DOC/DOCX/PPT/PPTX/XLS/XLSX/TXT etc.;
- videos;
- images;
- audio;
- screenshots;
- duplicate candidates;
- old files;
- empty folders;
- installed/unused applications when App Manager access is enabled;
- leftover candidates only when classification is defensible.

Scan requirements:
- coroutine/IO execution;
- progressive results;
- cancellation;
- resumable/cached index where beneficial;
- handle disappearing files;
- no unbounded memory accumulation;
- exclude Tooliva's own working/temp files from cleanup suggestions;
- never scan Android-protected data by bypass methods.

## 5.2 Storage dashboard

Show:
- total / used / available;
- indexed bytes;
- access mode: Full / Limited;
- category breakdown;
- top space consumers;
- highest-value cleanup actions.

Do not show one unexplained `Junk = X GB` total.

## 5.3 Explainable Junk Candidates

`Junk` is a presentation group, not a magic classifier.

Every item/group must have a reason such as:
- obsolete APK installer;
- temporary/download residue;
- empty file/folder;
- known thumbnail/cache artifact in accessible shared storage;
- duplicate exact file;
- old screenshot;
- user-selected old download;
- leftover candidate linked to removed app with confidence/explanation.

Risky/ambiguous items require review and are not preselected by default.

## 5.4 Large Files

Must work across every file type accessible in Full Storage Mode.

Default thresholds:
- >100 MB
- >500 MB
- >1 GB

Filters:
- All
- Video
- Image
- Audio
- APK
- Archive
- Document
- Download
- Other

Sort:
- size
- modified date
- name
- path/folder

Actions:
- preview/open;
- share;
- file details;
- multi-select;
- select all in current filter;
- delete/trash;
- locate in Files.

## 5.5 Downloads cleanup

Automatic category in Full Storage Mode.

Subcategories:
- installers/APK;
- archives;
- documents;
- media;
- old downloads;
- very large downloads.

Limited Mode may use user-mediated SAF and must state its limitation.

## 5.6 APK / Installer cleanup

Show:
- filename;
- size;
- modified date;
- APK package/app label/version where safely parseable;
- installed/not-installed relation where package visibility permits;
- duplicate APK versions;
- old installer candidates.

Never auto-delete an APK solely because it is old.

## 5.7 Archives/Documents

Show size/date/path and open/share/delete actions.

Do not classify documents as junk without explicit user-driven filters/review.

## 5.8 Screenshot Cleaner

Keep the already built architecture.

Required:
- screenshot bucket/path detection;
- 30+/90+/365+ filters;
- thumbnails;
- multi-select;
- Select all;
- shared central delete/trash flow;
- Cleanup Result.

## 5.9 Old Files

User-controlled filters:
- 30 days
- 90 days
- 180 days
- 365 days

Default category should focus on downloads/installers/screenshots first.

Do not claim `unused` unless there is reliable usage evidence.

## 5.10 Exact Duplicate Finder

Scope: files, not only photos.

Pipeline:
1. group by size;
2. optional lightweight metadata grouping;
3. hash candidates;
4. confirm exact byte/content match;
5. persist fingerprint cache keyed by stable metadata where possible.

UI:
- groups;
- total duplicate bytes;
- keep-one helper;
- preview/open;
- selection review;
- safe cleanup.

Hashing must be cancellable and incremental.

## 5.11 Similar Photos — P1/P2

Separate from exact duplicates.

Use local perceptual techniques only.

Must never imply two visually similar photos are identical.

## 5.12 Cleanup Swipe

General review mode inspired by proven market behavior.

Support selected queues:
- screenshots;
- old downloads;
- old photos/videos;
- user-selected folder/category.

Actions:
- swipe keep;
- swipe mark delete;
- skip;
- undo;
- save/continue session;
- final review before cleanup.

No file changes until final confirmation.

## 5.13 Empty folders

Detect only writable/accessible shared-storage folders.

Exclude:
- protected/system paths;
- folders with semantic placeholder files unless user reviews them;
- Tooliva internal storage.

## 5.14 Cache cleanup

On Android 11+ use the official user-mediated:
`StorageManager.ACTION_CLEAR_APP_CACHE`

Requirements:
- requires Full Storage Mode permission;
- explain that Android/system performs the action;
- do not claim Tooliva can directly read/delete every app's private internal cache;
- handle cancel/error/result honestly.

Optional Accessibility automation of per-app Settings screens is **not approved for V1** and requires separate policy/product decision.

---

# 6. Cleanup safety architecture

The existing centralized cleanup architecture is a product differentiator and must remain authoritative.

## 6.1 Cleanup Receipt / Result

Always distinguish:
- requested files/bytes;
- already missing;
- moved to Trash;
- physically freed;
- still present / failed;
- canceled;
- permission changed.

If a file is moved to Android Trash, do not report its bytes as physically freed.

## 6.2 Destructive flow

1. scan/index
2. user selects
3. show selected count + bytes
4. system/user confirmation as required
5. perform action
6. re-query/re-scan
7. verify result
8. show Cleanup Receipt

No ad between steps 3–8.

---

# 7. FILES — core file manager

Tooliva needs a real file manager because it is both useful and strategically aligned with the deep-cleaner permission model.

## 7.1 Main file views

- Internal storage
- SD card / USB if present and accessible
- Downloads
- Documents
- APKs
- Archives
- Images
- Videos
- Audio
- Recent
- Large
- Favorites

## 7.2 Browser actions

P0:
- folder navigation;
- breadcrumbs/path;
- list/grid;
- search;
- sort name/size/date;
- file details;
- open;
- share;
- rename;
- copy;
- move;
- delete/trash;
- create folder.

P1:
- ZIP;
- unzip;
- hashes;
- favorites;
- multi-pane/tablet improvements.

All copy/move operations require collision handling and progress/cancel states.

## 7.3 Global Search

Search indexed accessible shared storage by:
- name;
- extension/type;
- minimum/maximum size;
- modified date;
- folder.

Search must work offline.

## 7.4 Storage Map

P0/P1 differentiator.

Provide visual drill-down of storage usage using a treemap/sunburst-like representation.

Must also provide an accessible list fallback because visual sectors can be hard to select on small screens.

---

# 8. APP MANAGER

Purpose:
- understand app storage impact;
- find large/unused apps;
- uninstall user-selected apps;
- connect accessible leftover files to applications where defensible.

Potential data:
- app label/icon;
- package;
- installed size where Android exposes a defensible value;
- last used through UsageStats after explicit Usage Access;
- install/update dates;
- system/user app;
- launch;
- open system App Info;
- request uninstall.

`QUERY_ALL_PACKAGES` is restricted. Do not add until the broad-visibility need is implemented and documented. When required for the core App Manager/File Manager, perform a separate Play declaration review.

No installed-app inventory may be sent to ad analytics.

---

# 9. DIAGNOSE / PHONE DOCTOR

Benchmark against serious device-information apps, not fake cleaner dashboards.

## 9.1 Device
- manufacturer/model;
- Android/API/security patch;
- ABI;
- display/resolution/density;
- storage volumes.

## 9.2 Battery
- level;
- charging state/source;
- voltage;
- temperature;
- current/power when exposed;
- technology/status.

Never fabricate true battery capacity/health when Android/OEM data is not trustworthy.

## 9.3 Memory
- total/available RAM;
- low-memory state;
- current process/device memory facts where supported.

Do not expose `Free RAM` as a performance problem that needs boosting.

## 9.4 Network
- connection type;
- local IPv4/IPv6;
- DNS/link properties;
- Wi-Fi facts allowed by current permissions;
- mobile network/SIM facts where safely available;
- public IP only via explicit network request.

## 9.5 Sensors/tests
- sensor inventory + live values;
- display/dead-pixel test;
- touch/multitouch;
- vibration;
- flashlight;
- speaker;
- microphone;
- proximity;
- accelerometer;
- compass.

Label manual vs automatic test results.

---

# 10. PROTECT / retention modules

## 10.1 Notification History — high priority after cleaner core

After explicit Notification Access:
- local archive;
- grouped by app;
- search;
- date/channel filters;
- exclude apps;
- retention 1/7/30/90 days;
- clear all;
- pin/favorite;
- noisy-app insights;
- optional export later.

Notification content stays local and never enters ads/analytics.

## 10.2 Private Vault — later

- PIN + biometric;
- Android Keystore;
- authenticated encryption;
- secure import/export;
- auto-lock;
- uninstall/data-loss warning.

## 10.3 App Lock — later / experimental

Must compete on reliability, not checkbox presence.

Candidate functionality if technically/policy viable:
- PIN/pattern/biometric;
- random PIN keyboard;
- relock rules;
- notification privacy;
- intruder selfie later;
- recent-app protection if Android permits reliably.

AccessibilityService remains prohibited until explicit approval after prototype/policy research.

---

# 11. FILE / CONTENT TOOLS

P1 after the cleaner/file-manager core:

## Images
- compress;
- resize;
- JPEG/PNG/WebP convert;
- batch processing;
- metadata/EXIF viewer;
- remove GPS/private metadata.

## PDF
- images → PDF;
- reorder pages;
- page size/margins;
- compression;
- save/share.

## Archives
- ZIP/unzip;
- zip-slip/path traversal protection.

## Hash
- SHA-256;
- SHA-512;
- MD5 compatibility.

---

# 12. EVERYDAY TOOLS

- QR/barcode scan;
- QR generator: text/URL/contact/Wi-Fi;
- network info;
- ping;
- DNS lookup;
- compass;
- bubble level;
- flashlight;
- magnifier later;
- small offline converters/generators later.

Do not build 100 shallow tools before the cleaner/file manager is excellent.

---

# 13. PHONE CHECKUP

`CHECK MY PHONE` becomes an action plan, not a fake score.

Pipeline:
1. access mode and storage state;
2. deep storage scan summary;
3. reclaimable explained categories;
4. biggest files/folders;
5. duplicates/screenshots;
6. unused/large apps if access enabled;
7. battery/thermal facts;
8. sensor availability;
9. notification noise if enabled;
10. usage facts if enabled.

Output examples:
- `Downloads: 4.2 GB, including 8 APK installers`
- `Largest folder: Movies — 12.8 GB`
- `Exact duplicates: 1.1 GB`
- `Screenshots older than 90 days: 824 MB`
- `3 apps larger than 2 GB`

Actions deep-link into the exact review screen.

---

# 14. Permissions strategy

Permissions/special access are requested only when the relevant feature is used.

## Approved for implementation

### `MANAGE_EXTERNAL_STORAGE`
Approved for prototype/product implementation because Cleaner + File Manager + on-device file search are now explicit core purposes.

Production release remains contingent on Google Play Permissions Declaration approval.

Must provide:
- prominent in-app disclosure;
- clear Special App Access flow;
- Limited Mode fallback;
- policy documentation.

## Conditional / later

### `QUERY_ALL_PACKAGES`
Only when App Manager/file attribution demonstrably requires broad visibility. Requires separate declaration review.

### `PACKAGE_USAGE_STATS`
Only after explicit user opt-in for unused-app/app-usage features.

### Notification Listener
Only for Notification History, after disclosure.

### AccessibilityService
Not approved for Cleaner or App Lock in V1. Requires explicit human approval and current Play-policy review.

---

# 15. Persistence and indexing

Use Room for structured local index/cache where justified.

Candidates:
- `StorageIndexEntry`
- `ScanSnapshot`
- `FileFingerprint`
- `DuplicateGroupCache`
- `CleanupReceipt`
- `NotificationRecord`
- `NotificationAppRule`
- `UsageSnapshot`
- `ToolFavorite`

DataStore:
- preferences;
- onboarding/access state;
- UI settings.

Do not persist full filenames/paths in analytics.

---

# 16. Performance targets

- no filesystem scan on main thread;
- progressive first results;
- scan cancellation;
- incremental index updates where practical;
- duplicate hashing only after cheap pre-grouping;
- bounded concurrency;
- handle 100k+ file indexes without OOM;
- avoid rescanning unchanged volumes unnecessarily;
- background work only when user benefit justifies it;
- no permanent foreground service for normal browsing.

---

# 17. UI principles

Visual source remains `docs/design/`.

Product UX additions:
- Home must prominently expose `Clean` and `Files`;
- Cleaner top screen is action-oriented, not a list of random tools;
- always show Full vs Limited access when relevant;
- large numerical storage values are readable;
- category cards show real bytes/item counts;
- destructive controls stay reachable with fixed bottom actions for long lists;
- empty/loading/error/permission states are mandatory;
- no red panic color for normal cleanup opportunities.

---

# 18. Monetization

Free version must be genuinely useful.

Allowed:
- restrained banner/native ads on non-sensitive screens;
- occasional interstitial only after a completed non-sensitive workflow, after result is visible;
- Lifetime Pro purchase.

Never:
- show interstitial before scan result;
- gate cleanup behind an ad video;
- place ads on delete/trash confirmation;
- place ads on All Files Access disclosure;
- place ads in Vault/App Lock auth;
- claim free and immediately force a weekly subscription.

Initial Pro hypothesis remains lifetime rather than weekly subscription.

---

# 19. Release order

## Cleaner/File Manager Beta
Must include:
- Full Storage Mode + Limited fallback;
- Deep Storage Scan;
- real Large Files across shared storage;
- Downloads/APK/Archives/Documents categories;
- Screenshots;
- exact duplicates;
- Cleanup Receipt;
- File Manager browse/search/sort;
- basic Storage Map;
- cache-clear system action;
- privacy/permission disclosures;
- Xiaomi/Samsung/Pixel validation.

## Tooliva 1.0
Add:
- Cleanup Swipe;
- App Manager / unused apps;
- Phone Checkup / Phone Doctor;
- Notification History;
- image/EXIF/PDF basics;
- QR/network/compass/level/flashlight;
- monetization + Pro;
- Play declarations/Data Safety.

Vault/App Lock may ship later if they are not yet production-grade.

---

# 20. Release criteria

Do not publish until:
- targetSdk 36;
- deep scan behavior validated on Android 11–16;
- fallback behavior validated without All Files Access;
- All Files Access declaration is prepared and reviewed;
- any package visibility declaration is justified separately;
- destructive operations are verified on physical devices;
- Trash vs physically freed accounting is correct;
- large indexes do not ANR/OOM;
- privacy policy/Data Safety match the actual build;
- merged manifest and SDKs are audited;
- no fake optimization language exists in UI/store listing;
- monetization cannot interrupt a cleanup result.
