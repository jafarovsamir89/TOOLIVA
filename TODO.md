# Tooliva — TODO / Product Roadmap

Revision: 2026-08-18  
Authoritative product source: `docs/PRODUCT_CONSTITUTION.md`

Legend:
- `[x]` implemented and verified for stated scope
- `[~]` implemented/partially verified or needs current retest
- `[ ]` not complete
- `[!]` rejected/deprecated direction; do not continue
- `[P0]` current/release-blocking priority
- `[P1]` important after P0
- `[P2]` later

# Product definition

**Tooliva — Cleaner, File Manager & Device Tools**

Core order:
1. Cleaner
2. File Manager
3. Apps / maintenance
4. Phone Doctor / Checkup
5. Notification History
6. later Protect/content/everyday tools

---

# Phase R0 — RECOVERY FROM INDEX REGRESSION

This is the current highest priority. Do not start new product modules until R0 receives human Xiaomi PASS.

## Known-good reference

- [x] [P0] Reference baseline identified: `b767aa8`
- [x] [P0] Xiaomi previously confirmed Full Storage discovery of APK/ZIP/PDF/DOC/PNG/MP4
- [x] [P0] Xiaomi previously confirmed select/open/delete Large Files flow
- [x] [P0] Xiaomi previously confirmed immediate Cleanup Result after timing fix
- [x] [P0] Xiaomi previously confirmed Screenshot Cleaner Trash flow
- [x] [P0] Xiaomi previously confirmed Home navigation fix

## Rejected experiment

- [!] Mandatory Room Storage Index from `7836ea`
- [!] Fast/deep index-first Cleaner from `71f35ca`
- [!] `StorageIndexCard` as consumer-facing Cleaner UI
- [!] Large Files depending on active Room generation/snapshot
- [!] automatic heavy scan simply because Large Files/Clean screen opened

## Recovery implementation

- [x] [P0] Revert/remove mandatory index changes introduced by `7836ea` and `71f35ca` while preserving unrelated good work/docs
- [x] [P0] Restore direct progressive `StorageProvider -> StorageScanEvent -> LargeFilesViewModel/UI` flow based on known-good baseline
- [x] [P0] Remove Storage Index technical card/status from Clean UI
- [x] [P0] Remove automatic heavy Large Files scan on navigation
- [x] [P0] Keep explicit user-controlled Scan/Refresh action
- [x] [P0] Preserve FullStorageProvider / StorageProvider abstraction
- [x] [P0] Preserve centralized cleanup/delete coordinator
- [x] [P0] Preserve Cleanup Receipt accounting
- [x] [P0] Preserve filters/search/sort/select/open/delete behavior
- [x] [P0] Remove Room/KSP/storage-index dependencies/files if no current feature uses them after recovery; do not keep dead architecture “for later”
- [x] [P0] Automated regression tests/build green
- [x] [P0] Install fresh debug APK on Xiaomi
- [ ] [P0] Human manual regression PASS before Phase R1

---

# Phase R1 — STORAGE PERMISSION UX

Goal: one understandable primary Full Mode flow on Android 11+.

- [~] [P0] `MANAGE_EXTERNAL_STORAGE` manifest/config exists
- [~] [P0] All Files Access disclosure/settings flow exists
- [~] [P0] `Environment.isExternalStorageManager()` state detection exists
- [ ] [P0] Full Mode: Large Files works without requesting separate Photos/Videos permission
- [ ] [P0] Full Mode: Screenshot Cleaner works without redundant `READ_MEDIA_IMAGES` request when broad storage access is sufficient
- [ ] [P0] Full Mode: future Downloads/APK/Archives/Documents use the same storage access
- [ ] [P0] Limited Mode: MediaStore/granular media permission used only when Full Mode is absent and the feature needs it
- [ ] [P0] Limited Mode coverage explained honestly
- [ ] [P0] Grant/deny/revoke tested manually on Xiaomi
- [ ] [P0] Draft final in-app All Files disclosure copy
- [ ] [P0] Draft Google Play All Files Access declaration package/text

STOP after R1 until human PASS.

---

# Phase 1 — FOUNDATION THAT ALREADY WORKS

- [x] Kotlin + Jetpack Compose project
- [x] package namespace `az.simplesoft.tooliva`
- [x] compileSdk/targetSdk 36
- [x] minSdk 26
- [x] Material 3
- [x] Navigation Compose
- [x] Tooliva light/dark design direction
- [x] unit/UI test foundation
- [x] CI build foundation
- [x] Home screen baseline
- [x] storage summary
- [x] battery/device summary baseline
- [x] module cards
- [x] `CHECK MY PHONE` shell

Deferred infrastructure:

- [ ] [P1] DataStore when real persistent settings need it
- [ ] [P1] Room only when a real persistent feature needs it (duplicates fingerprints, Notification History, etc.)
- [ ] [P1] Hilt only when dependency graph complexity justifies it
- [ ] [P1] WorkManager only when a real deferred job needs it
- [ ] [P1] common reusable Tooliva components as repetition appears

Do not add these solely to satisfy architecture style.

---

# Phase 2 — CLEANER CORE

## Cleaner shell

- [ ] [P0] Clean screen contains only user-facing storage/cleanup concepts
- [ ] [P0] Storage used/total/free card
- [ ] [P0] one clear Scan/Analyze action when required
- [ ] [P0] progressive category/result cards
- [ ] [P0] no index/database/generation terminology in production UI
- [ ] [P0] scan can be canceled
- [ ] [P0] no heavy auto-scan on navigation

## Large Files

- [~] [P0] Full Mode discovers accessible shared-storage file types — previously Xiaomi PASS, must re-pass after recovery
- [~] [P0] categories All/Video/Image/Audio/APK/Archive/Document/Download/Other
- [~] [P0] thresholds 100 MB / 500 MB / 1 GB
- [~] [P0] sort size/newest/oldest/name
- [~] [P0] search name/path
- [~] [P0] multi-select / Select all
- [~] [P0] open
- [ ] [P0] share
- [ ] [P0] details
- [~] [P0] safe delete/trash
- [x] [P0] Cleanup Receipt model distinguishes Trash vs Physically Freed
- [ ] [P0] current Xiaomi regression PASS after recovery
- [ ] [P1] Samsung physical test
- [ ] [P1] Pixel physical test

## Downloads

- [ ] [P0] automatic Full Mode Downloads scan
- [ ] [P0] APK/installers group
- [ ] [P0] archives group
- [ ] [P0] documents group
- [ ] [P0] media group
- [ ] [P0] large downloads filter
- [ ] [P0] old downloads 30/90/180/365
- [ ] [P1] Limited Mode SAF fallback where useful

## APK installers

- [ ] [P0] detect APK files
- [ ] [P0] safe app/package/version metadata parsing where possible
- [ ] [P0] size/date/path/open/share/details/delete
- [ ] [P0] never preselect solely because old
- [ ] [P1] relation to installed package only when visibility permits
- [ ] [P1] duplicate/old-version installer analysis

## Archives

- [ ] [P0] ZIP/RAR/7Z classification where identifiable
- [ ] [P0] size/date/path/open/share/details/delete
- [ ] [P0] Downloads integration

## Documents

- [ ] [P0] PDF/Office/text classification
- [ ] [P0] size/date/path/open/share/details/delete
- [ ] [P0] normal documents never generic junk

## Old Files

- [ ] [P0] age filters 30/90/180/365
- [ ] [P0] prioritize Downloads/APKs/Archives/Screenshots/user-selected scope
- [ ] [P0] do not label `unused` without actual usage evidence

---

# Phase 3 — SCREENSHOTS / EXPLAINABLE CLEANUP

## Screenshot Cleaner

- [~] [P0] screenshot detection
- [~] [P0] 30/90/365 filters
- [~] [P0] thumbnails
- [~] [P0] multi-select / Select all
- [~] [P0] central Trash/delete flow
- [x] [P0] immediate Cleanup Receipt behavior previously human-verified
- [ ] [P0] Full Mode permission unification
- [ ] [P0] re-run synthetic screenshot destructive test after permission rewrite

## Explainable Junk

- [ ] [P0] implement first real deterministic rule set
- [ ] [P0] every group exposes `why shown`
- [ ] [P0] ambiguous candidates not preselected
- [ ] [P0] no mystery `Junk = X GB` without drill-down
- [ ] [P0] totals derived only from actual listed candidates

Candidate rules to research/implement one by one:

- [ ] old APK installer candidate
- [ ] old Downloads candidate
- [ ] exact duplicate candidate
- [ ] accessible deterministic temp/residual artifacts
- [ ] empty writable folders [P1]

---

# Phase 4 — CACHE / APPS

## Cache cleanup v1

- [ ] [P0] implement official `StorageManager.ACTION_CLEAR_APP_CACHE` flow where supported
- [ ] [P0] explain system-mediated behavior
- [ ] [P0] handle supported/unsupported/cancel/error honestly
- [ ] [P0] no fake per-app private-cache visibility
- [ ] [P0] Xiaomi manual test
- [ ] [P2] Accessibility-based automation only after separate explicit approval/policy review

## App Manager

- [ ] [P0] prototype visible app list without `QUERY_ALL_PACKAGES`
- [ ] [P0] label/icon/package/install/update date
- [ ] [P0] user/system distinction where possible
- [ ] [P0] launch
- [ ] [P0] App Info
- [ ] [P0] uninstall request
- [ ] [P1] defensible storage size where available
- [ ] [P1] Usage Access + last used / rarely used recommendations
- [ ] [P1] if narrow visibility is insufficient, document exact gap and re-review `QUERY_ALL_PACKAGES`

---

# Phase 5 — FILE MANAGER

File Manager is core functionality.

- [ ] [P0] browse accessible shared storage/volumes
- [ ] [P0] breadcrumbs/folder navigation
- [ ] [P0] category shortcuts Downloads/Documents/APKs/Archives/Images/Videos/Audio/Recent/Large
- [ ] [P0] sort name/size/date
- [ ] [P0] search by name
- [ ] [P0] details
- [ ] [P0] open
- [ ] [P0] share
- [ ] [P0] rename
- [ ] [P0] create folder
- [ ] [P0] copy
- [ ] [P0] move
- [ ] [P0] delete/trash through central coordinator
- [ ] [P0] collision handling
- [ ] [P0] long-operation progress/cancel

Do not make basic browser depend on a completed whole-device Room index.

---

# Phase 6 — EXACT DUPLICATES / CLEANUP SWIPE

## Exact duplicates

- [ ] [P0] cheap size pre-grouping
- [ ] [P0] hash only groups with 2+ candidates
- [ ] [P0] exact verification
- [ ] [P0] Room fingerprint cache only here if useful
- [ ] [P0] file mutation invalidates cached fingerprint
- [ ] [P0] groups UI
- [ ] [P0] recoverable bytes
- [ ] [P0] keep-one helper
- [ ] [P0] safe cleanup + Receipt
- [ ] [P0] cancellation

## Cleanup Swipe

- [ ] [P1] keep/delete/skip
- [ ] [P1] undo
- [ ] [P1] selected category/session
- [ ] [P1] final review
- [ ] [P1] Cleanup Receipt

---

# Phase 7 — STORAGE MAP

- [ ] [P1] folder-size aggregation during explicit analysis
- [ ] [P1] treemap/sunburst-style prototype
- [ ] [P1] drill-down
- [ ] [P1] accessible list alternative
- [ ] [P1] open/details/delete integration

Storage Map is not allowed to become a prerequisite for basic Cleaner/File Manager usage.

---

# Phase 8 — PHONE DOCTOR / CHECK MY PHONE

## Phone Doctor

- [ ] [P0] device/model/Android/security patch
- [ ] [P0] CPU/ABI public facts
- [ ] [P0] RAM/storage facts
- [ ] [P0] battery level/state/source/voltage/temperature
- [ ] [P1] current/power where exposed
- [ ] [P0] thermal state
- [ ] [P0] sensor inventory
- [ ] [P1] network details
- [ ] [P1] guided hardware tests

## Check My Phone

- [ ] [P0] aggregate existing Cleaner + Doctor insights
- [ ] [P0] biggest actionable storage categories
- [ ] [P0] deep-links to exact review screens
- [ ] [P0] no fake health score

---

# Phase 9 — NOTIFICATION HISTORY

- [ ] [P0] prominent disclosure
- [ ] [P0] Notification Access flow
- [ ] [P0] `NotificationListenerService`
- [ ] [P0] Room persistence (valid persistence use)
- [ ] [P0] group/search/filter
- [ ] [P0] retention controls
- [ ] [P0] excluded apps
- [ ] [P0] clear history
- [ ] [P1] noisy-app insights

---

# Phase 10 — LATER MODULES

## Vault [P1]

- threat model
- PIN/biometric
- Keystore
- authenticated encryption
- secure import/export
- auto-lock

## App Lock [P1/P2]

- reliability prototype
- Play-policy decision
- no Accessibility without explicit approval

## Content/everyday tools [P1/P2]

- image compress/resize/convert
- EXIF privacy clean
- images to PDF
- QR scanner/generator
- network tools
- compass/level/flashlight

Do not prioritize these before Cleaner/File Manager core quality.

---

# Phase 11 — MONETIZATION / RELEASE

- [ ] choose ad SDK after core UX stabilizes
- [ ] consent flow
- [ ] restrained ad placements
- [ ] no ads in permission/destructive/receipt/sensitive flows
- [ ] Play Billing
- [ ] Pro Lifetime / restore
- [ ] privacy policy final
- [ ] Data Safety inventory
- [ ] merged-manifest permission audit
- [ ] All Files Access declaration package
- [ ] target audience/content rating
- [ ] closed test

---

# Manual testing process — mandatory

For every device-dependent vertical slice:

1. agent runs automated tests/build;
2. agent installs fresh debug APK on connected Xiaomi;
3. agent performs crash smoke-check only;
4. agent outputs `MANUAL TEST REQUIRED — <feature>`;
5. human user performs numbered manual tests;
6. user reports PASS/FAIL;
7. agent fixes failures;
8. only human-confirmed device behavior becomes `[x]`;
9. next major slice begins only after PASS.

---

# Immediate next milestone

**RECOVERY v1**

`revert index-first experiment -> restore direct Large Files scan -> remove index UI/autoscan -> unify Full Mode permission behavior -> build/install -> human Xiaomi regression test -> STOP`
