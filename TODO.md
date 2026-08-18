# Tooliva — TODO / Product Roadmap

Market-driven rewrite: 2026-08-16  
Research source: `docs/MARKET_RESEARCH_2026.md`

Legend:
- `[x]` implemented and sufficiently verified for the stated scope
- `[~]` implemented/partially verified; do not treat as release-complete
- `[ ]` not complete
- `[P0]` required for current milestone
- `[P1]` important after P0
- `[P2]` later

## Current product definition

**Tooliva — Cleaner, File Manager & Device Tools**

Core purposes:
1. storage cleaning;
2. file management / on-device file search;
3. device maintenance.

Do not prioritize unrelated micro-tools until Cleaner + File Manager are strong.

---

# Phase 0 — Product / policy foundation

- [x] [P0] Working brand: Tooliva
- [x] [P0] Create GitHub repository
- [x] [P0] Add README / agent rules / architecture docs
- [x] [P0] Add 2026 competitive market research
- [x] [P0] Confirm minSdk 26
- [x] [P0] Set compileSdk/targetSdk 36
- [ ] [P0] Reserve/final-check package id `az.simplesoft.tooliva`
- [x] [P0] Product decision: Cleaner + File Manager are core purposes
- [x] [P0] Product decision: `MANAGE_EXTERNAL_STORAGE` approved for implementation/prototype
- [~] [P0] Write final All Files Access in-app disclosure copy — prototype disclosure is implemented; final product copy still needs review
- [ ] [P0] Draft Google Play `MANAGE_EXTERNAL_STORAGE` declaration text
- [ ] [P0] Decide `QUERY_ALL_PACKAGES` only after App Manager implementation proves broad visibility is necessary
- [ ] [P0] Choose ads SDK later; do not block current storage work

---

# Phase 1 — Android foundation

- [x] [P0] Kotlin + Jetpack Compose project
- [~] [P0] Gradle setup / wrapper / dependency organization
- [x] [P0] Package namespace
- [x] [P0] Material 3
- [x] [P0] Navigation Compose
- [x] [P0] Light/dark Tooliva design direction
- [x] [P0] Unit/UI test foundation
- [x] [P0] CI build/test workflow
- [ ] [P1] Introduce version catalog if it improves maintainability without churn
- [~] [P0] Add Room for storage index / fingerprints / receipts — Storage Index v1 schema and repository added; fingerprints/receipts remain separate future work
- [ ] [P0] Add DataStore for settings/access state
- [ ] [P1] Add Hilt when dependency graph justifies it
- [ ] [P1] Add WorkManager only for justified deferred/index work
- [ ] [P1] Extract common Tooliva cards/buttons/permission components

---

# Phase 2 — Existing UI / Cleaner baseline

- [x] [P0] Home screen
- [x] [P0] Storage summary card
- [x] [P0] Battery/device summary
- [x] [P0] Module cards
- [x] [P0] `CHECK MY PHONE` CTA shell

## Existing scoped-storage cleaner work

- [x] [P0] MediaStore scanning foundation
- [x] [P0] Progressive scan / cancellation foundation
- [x] [P0] Centralized cleanup/delete coordinator
- [x] [P0] Cleanup Result accounting model
- [x] [P0] Distinguish Trash bytes vs physically freed bytes
- [~] [P0] Large Files media implementation — useful fallback but not full product scope
- [~] [P0] Screenshot Cleaner with 30/90/365, thumbnails, multi-select, Select all
- [~] [P0] Physical-device validation on Xiaomi — manual PASS confirmed for immediate Large Files Cleanup Result, Screenshot Cleaner Trash result, and Home navigation; Storage Index v1 still needs manual validation

Do **not** delete the existing MediaStore implementation. It becomes Limited Mode/fallback infrastructure.

---

# Phase 3 — Full Storage Mode / storage engine

This is now the highest-priority engineering phase.

## All Files Access

- [~] [P0] Add `MANAGE_EXTERNAL_STORAGE` to prototype manifest/config — implemented; production Play review remains
- [~] [P0] Add `Environment.isExternalStorageManager()` access-state detection — implemented
- [~] [P0] Add truthful pre-permission disclosure screen — implemented in Clean/Large Files access card
- [~] [P0] Open correct Special App Access settings flow — implemented; OEM/manual validation pending
- [~] [P0] Handle grant / deny / revoke cleanly — implemented in state refresh/fallback; physical validation pending
- [~] [P0] Clearly display `Full Storage Mode` vs `Limited Mode` — implemented
- [~] [P0] Ensure app remains functional in Limited Mode — existing MediaStore fallback preserved; physical validation pending
- [x] [P0] Add tests for access-state transitions where possible

## Storage abstraction

- [~] [P0] Introduce `StorageProvider`/domain abstraction — implemented for the first Large Files slice
- [x] [P0] Keep MediaStore provider as Limited Mode implementation
- [~] [P0] Implement Full Storage provider for accessible shared storage — implemented; physical scan pending
- [~] [P0] Normalize file model: path/ref/name/extension/MIME/size/date/category/volume — implemented for Large Files
- [~] [P0] Exclusion rules for Tooliva temp/internal files — protected shared-storage paths are excluded; index exclusions remain
- [~] [P0] Protected-path handling; never attempt Android restriction bypasses — implemented and build-verified; device behavior pending

## Index

- [x] [P0] Room-backed storage index — entries, scopes, generations, exported schema
- [~] [P0] Progressive full-storage indexing — real counters/progress and Clean UI implemented; physical UX validation pending
- [x] [P0] Cancellation — automated connected test confirms canceled generation preserves the last successful index
- [x] [P0] Bounded concurrency — single cancellable traversal with bounded Room batches; no per-file coroutine fan-out
- [~] [P0] Handle files disappearing/changing during scan — per-entry failures are isolated and changed metadata is tested; physical revoke/disappearance validation pending
- [x] [P0] Incremental/reuse strategy so unchanged storage is not fully rescanned every time — metadata reuse and stale cleanup covered by 50k test
- [x] [P0] Stress test 50k files — connected Room stress test passed on Xiaomi
- [ ] [P1] Stress test 100k+ files

---

# Phase 4 — Deep Cleaner categories

## Cleaner dashboard

- [ ] [P0] Rework Clean screen around one `SCAN STORAGE` flow
- [ ] [P0] Show indexed/used/free bytes
- [ ] [P0] Show Full/Limited access status
- [ ] [P0] Show highest-value cleanup recommendations
- [ ] [P0] Category cards use real item counts + bytes

## Large Files — full scope

- [x] [P0] Scan all accessible shared-storage file types in Full Mode — user confirmed APK/ZIP/PDF/media fixtures were found on Xiaomi
- [~] [P0] Filters: All / Video / Image / Audio / APK / Archive / Document / Download / Other
- [~] [P0] Thresholds 100 MB / 500 MB / 1 GB — implemented; manual UI verification pending
- [~] [P0] Sort by size/date/name — implemented; manual UI verification pending
- [~] [P0] Open/share/details — open implemented; share/details remain
- [~] [P0] Multi-select / Select all — implemented; manual UI verification pending
- [~] [P0] Safe delete/trash + Cleanup Receipt — direct Full Mode deletion and existing Limited Trash flow implemented; physical verification pending
- [~] [P0] Xiaomi physical test with APK + ZIP + PDF + media — user confirmed discovery, selection and deletion; cleanup receipt timing fix awaits retest
- [ ] [P0] Samsung physical test
- [ ] [P0] Pixel physical test

## Downloads

- [ ] [P0] Automatic Downloads analysis in Full Mode
- [ ] [P0] APK/installers category
- [ ] [P0] Archives category
- [ ] [P0] Documents category
- [ ] [P0] Media category
- [ ] [P0] Old downloads filter
- [ ] [P0] Large downloads filter
- [ ] [P0] Limited Mode SAF fallback with honest UX

## APK / installers

- [ ] [P0] Detect APK files
- [ ] [P0] Parse safe APK metadata where possible
- [ ] [P1] Relate APK to installed package when package visibility allows
- [ ] [P1] Detect duplicate/old APK versions
- [ ] [P0] Never preselect APK deletion solely because file is old

## Archives / documents / other

- [ ] [P0] ZIP/RAR/7Z classification where identifiable
- [ ] [P0] PDF/Office/text document classification
- [ ] [P0] Open/share/details/delete flows
- [ ] [P0] Never call normal user documents `junk` without explainable rule/user filter

## Old files

- [ ] [P0] Filters 30/90/180/365 days
- [ ] [P0] Prioritize downloads/APKs/screenshots for cleanup suggestions
- [ ] [P0] Do not use `unused` language without actual usage evidence

## Explainable junk

- [ ] [P0] Define deterministic cleanup-candidate rule registry
- [ ] [P0] Every rule provides user-facing reason
- [ ] [P0] Ambiguous candidates are not preselected
- [ ] [P0] No mystery `Junk = X GB` aggregate without drill-down

## Empty folders

- [ ] [P1] Detect writable empty shared-storage folders
- [ ] [P1] Safe exclusions
- [ ] [P1] Review before delete

---

# Phase 5 — File Manager / on-device search

File Manager is core, not optional polish.

## Browser

- [ ] [P0] Internal/shared storage browser
- [ ] [P0] SD card / USB volume handling when available
- [ ] [P0] Breadcrumb/path navigation
- [ ] [P0] List/grid mode
- [ ] [P0] Category shortcuts: Downloads/Documents/APKs/Archives/Images/Videos/Audio/Recent/Large
- [ ] [P0] Sort name/size/date
- [ ] [P0] File details
- [ ] [P0] Open
- [ ] [P0] Share
- [ ] [P0] Rename
- [ ] [P0] Copy
- [ ] [P0] Move
- [ ] [P0] Delete/trash using central coordinator
- [ ] [P0] Create folder
- [ ] [P0] Collision handling
- [ ] [P0] Long-operation progress + cancel

## Global search

- [ ] [P0] Search by name
- [ ] [P0] Extension/type filters
- [ ] [P0] Size filters
- [ ] [P0] Date filters
- [ ] [P0] Folder/path filter
- [ ] [P0] Offline indexed results

## Storage Map

- [ ] [P0] Prototype treemap/sunburst-style disk usage view
- [ ] [P0] Drill-down folder → child folders/files
- [ ] [P0] Accessible list alternative
- [ ] [P1] Open/delete/details from map

---

# Phase 6 — Duplicates / smart cleanup

## Exact duplicate files

- [ ] [P0] Size pre-grouping
- [ ] [P0] Incremental hash worker
- [ ] [P0] Fingerprint cache in Room
- [ ] [P0] Exact duplicate groups across accessible file types
- [ ] [P0] Total recoverable duplicate bytes
- [ ] [P0] Keep-one helper
- [ ] [P0] Preview/open/location
- [ ] [P0] Safe cleanup + Cleanup Receipt
- [ ] [P0] Cancellation / large-data tests

## Similar photos

- [ ] [P1] Local perceptual similarity prototype
- [ ] [P1] Similarity groups UI
- [ ] [P1] Never present similar as identical

## Screenshot Cleaner

- [~] [P0] Existing screenshot detection + filters + thumbnails + selection
- [ ] [P0] Revalidate through new StorageProvider architecture
- [ ] [P0] End-to-end destructive test with synthetic screenshots

## Cleanup Swipe

- [ ] [P0] Swipe keep/delete/skip
- [ ] [P0] Undo
- [ ] [P0] Saved review session
- [ ] [P0] Support screenshots / old downloads / user-selected category
- [ ] [P0] Final review before changes
- [ ] [P0] Cleanup Receipt

---

# Phase 7 — App Manager / cache maintenance

## App Manager

- [ ] [P0] Define exact PackageManager visibility needs
- [ ] [P0] Prototype app list without `QUERY_ALL_PACKAGES`
- [ ] [P0] If incomplete for core UX, document evidence and approve `QUERY_ALL_PACKAGES`
- [ ] [P0] Prepare separate Play declaration if added
- [ ] [P0] App label/icon/package
- [ ] [P0] Install/update date
- [ ] [P0] System/user app distinction
- [ ] [P0] Open App Info
- [ ] [P0] Launch app
- [ ] [P0] Request uninstall
- [ ] [P1] App storage size where Android exposes defensible values

## Usage / unused apps

- [ ] [P1] Usage Access disclosure
- [ ] [P1] `PACKAGE_USAGE_STATS` settings flow
- [ ] [P1] Last-used / today / 7d / 30d
- [ ] [P1] Large + rarely used recommendations

## Cache cleanup

- [ ] [P0] Implement `StorageManager.ACTION_CLEAR_APP_CACHE` on API 30+
- [ ] [P0] Explain system-mediated behavior
- [ ] [P0] Handle success/cancel/error
- [ ] [P0] Never claim silent private-cache access
- [ ] [P2] Accessibility automation only after separate human + Play policy approval

---

# Phase 8 — Phone Doctor / Checkup

## Phone Doctor

- [ ] [P0] Device/manufacturer/model/Android/security patch
- [ ] [P0] CPU/ABI facts available through public APIs
- [ ] [P0] Memory/storage facts
- [ ] [P0] Battery level/state/source/voltage/temperature
- [ ] [P1] Battery current/power when exposed
- [ ] [P0] Thermal state
- [ ] [P0] Sensor inventory
- [ ] [P1] Live sensor values
- [ ] [P1] Network details

## Hardware tests

- [ ] [P1] Display/dead-pixel test
- [ ] [P1] Touch/multitouch test
- [ ] [P1] Vibration
- [ ] [P1] Flashlight
- [ ] [P1] Speaker
- [ ] [P1] Microphone
- [ ] [P1] Proximity
- [ ] [P1] Accelerometer
- [ ] [P1] Compass

## Check My Phone

- [ ] [P0] Orchestrate storage/action-plan scan
- [ ] [P0] Biggest files/folders insight
- [ ] [P0] Cleanup categories insight
- [ ] [P0] Duplicate/screenshot insight
- [ ] [P0] Battery/thermal facts
- [ ] [P0] Deep-links into review screens
- [ ] [P0] No fake health score
- [ ] [P1] App/notification insights when access enabled

---

# Phase 9 — Notification History / Protect

## Notification History

- [ ] [P0] Prominent disclosure
- [ ] [P0] Notification Access flow
- [ ] [P0] `NotificationListenerService`
- [ ] [P0] Local Room persistence
- [ ] [P0] Group by app
- [ ] [P0] Search
- [ ] [P0] Date/channel filters
- [ ] [P0] Excluded apps
- [ ] [P0] Retention 1/7/30/90 days
- [ ] [P0] Clear history
- [ ] [P1] Pin/favorite
- [ ] [P1] Noisy-app insights
- [ ] [P2] Export

## Private Vault

- [ ] [P1] Threat model
- [ ] [P1] PIN + biometric
- [ ] [P1] Android Keystore master key
- [ ] [P1] Authenticated encrypted import
- [ ] [P1] Encrypted metadata
- [ ] [P1] Secure export
- [ ] [P1] Auto-lock
- [ ] [P1] Uninstall/data-loss warning

## App Lock

- [ ] [P1] Prototype on Xiaomi/Samsung/Pixel
- [ ] [P1] Verify Play-policy path
- [ ] [P1] Foreground-app detection prototype
- [ ] [P1] PIN/pattern/biometric lock screen
- [ ] [P1] Random PIN keyboard
- [ ] [P1] Timeout/relock/reboot tests
- [ ] [P1] Decide GO / NO-GO
- [ ] [P2] Intruder selfie if privacy/policy validated

---

# Phase 10 — File/content tools

- [ ] [P1] Image compress
- [ ] [P1] Image resize
- [ ] [P1] JPEG/PNG/WebP conversion
- [ ] [P1] Batch image processing
- [ ] [P1] EXIF viewer
- [ ] [P1] Remove GPS/private metadata
- [ ] [P1] Images → PDF
- [ ] [P1] ZIP
- [ ] [P1] Unzip + zip-slip protection
- [ ] [P1] SHA-256/SHA-512/MD5

---

# Phase 11 — Everyday tools

- [ ] [P1] QR/barcode scanner
- [ ] [P1] QR generator
- [ ] [P1] Wi-Fi QR
- [ ] [P1] Network info
- [ ] [P1] Ping / DNS lookup
- [ ] [P1] Compass
- [ ] [P1] Bubble level
- [ ] [P1] Flashlight
- [ ] [P2] Magnifier
- [ ] [P2] Unit converter / password generator / color picker

---

# Phase 12 — Monetization

Do this after the core utility proves valuable.

- [ ] [P0] Ads abstraction
- [ ] [P0] Consent flow where required
- [ ] [P0] Restrained banner/native placements
- [ ] [P0] Interstitial frequency cap
- [ ] [P0] Guarantee no ad between destructive selection and Cleanup Receipt
- [ ] [P0] Google Play Billing
- [ ] [P0] Pro Lifetime purchase
- [ ] [P0] Restore purchases
- [ ] [P0] Paid state never shows ads
- [ ] [P1] Regional pricing experiment

---

# Phase 13 — Privacy / Google Play

- [ ] [P0] Privacy policy reflecting full-storage access
- [ ] [P0] Data Safety inventory
- [ ] [P0] SDK data audit
- [ ] [P0] All Files Access declaration
- [ ] [P0] `QUERY_ALL_PACKAGES` declaration only if ultimately used
- [ ] [P0] Usage Access disclosure
- [ ] [P0] Notification Access disclosure
- [ ] [P0] Verify merged manifest
- [ ] [P0] Ad declarations
- [ ] [P0] Content rating / target audience
- [ ] [P0] Store description accurately promotes File Manager/Storage Search as core functionality

---

# Phase 14 — QA matrix

- [ ] [P0] Android 8–10 Limited/legacy behavior
- [ ] [P0] Android 11
- [ ] [P0] Android 12
- [ ] [P0] Android 13
- [ ] [P0] Android 14
- [ ] [P0] Android 15
- [ ] [P0] Android 16
- [~] [P0] Xiaomi physical-device baseline — latest manual PASS covers Cleaner result timing and Home navigation; broader index/access regression remains
- [ ] [P0] Samsung physical device
- [ ] [P0] Pixel physical device
- [ ] [P1] Oppo/Realme if available
- [ ] [P0] Full Storage Mode grant/deny/revoke
- [ ] [P0] 50k-file stress
- [ ] [P1] 100k-file stress
- [ ] [P0] Destructive-operation regression suite
- [ ] [P0] APK/ZIP/PDF/media deletion tests
- [ ] [P0] Copy/move collision tests
- [ ] [P0] Low-storage behavior
- [ ] [P0] Process death during long operations
- [ ] [P0] No unexpected network upload during scan

---

# Cleaner / File Manager Beta release gate

- [ ] Full Storage Mode + Limited fallback
- [ ] Deep Storage Scan
- [ ] Large Files across APK/archives/docs/media/other
- [ ] Downloads analyzer
- [ ] Screenshot Cleaner verified under final architecture
- [ ] Exact duplicate files
- [ ] Cleanup Receipt verified
- [ ] File Manager browse/search/sort/open/share/rename/copy/move/delete
- [ ] Storage Map basic
- [ ] System-mediated Cache Cleanup
- [ ] All Files Access disclosure + Play declaration draft
- [ ] Xiaomi/Samsung/Pixel smoke validation
- [ ] CI green

# Tooliva 1.0 release gate

Everything in Cleaner/File Manager Beta plus:

- [ ] Cleanup Swipe
- [ ] App Manager basic
- [ ] Phone Doctor
- [ ] Check My Phone action plan
- [ ] Notification History
- [ ] Image Compress
- [ ] EXIF Privacy Clean
- [ ] Images to PDF
- [ ] QR Scanner/Generator
- [ ] Network Info
- [ ] Compass / Level / Flashlight
- [ ] Ads implemented respectfully
- [ ] Pro Lifetime
- [ ] Privacy/Data Safety complete
- [ ] Play restricted-permission review complete
- [ ] Crash-free closed test

Vault and App Lock are **not allowed to delay 1.0** if they are not yet production-grade.
