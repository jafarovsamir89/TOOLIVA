# Tooliva — TODO / Product Roadmap

Revision: 2026-08-20
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
- [x] [P0] Keep Room/KSP out of the Cleaner/storage-index path; the only active Room feature is Notification History, a valid persistence use
- [x] [P0] Automated regression tests/build green
- [x] [P0] Install fresh debug APK on Xiaomi
- [x] [P0] Human manual regression PASS before Phase R1

---

# Phase R1 — STORAGE PERMISSION UX

Goal: one understandable primary Full Mode flow on Android 11+.

- [~] [P0] `MANAGE_EXTERNAL_STORAGE` manifest/config exists
- [~] [P0] All Files Access disclosure/settings flow exists
- [~] [P0] `Environment.isExternalStorageManager()` state detection exists
- [x] [P0] Full Mode: Large Files works without requesting separate Photos/Videos permission
- [x] [P0] Full Mode: Screenshot Cleaner works without redundant `READ_MEDIA_IMAGES` request when broad storage access is sufficient
- [x] [P0] Full Mode: Downloads/APK/Archives/Documents use the same storage access — Xiaomi PASS
- [x] [P0] Limited Mode: MediaStore/granular media permission used only when Full Mode is absent and the feature needs it
- [x] [P0] Limited Mode coverage explained honestly
- [x] [P0] Grant/deny/revoke tested manually on Xiaomi
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
- [x] [P1] Room only when a real persistent feature needs it — Notification History is the first active persistent feature
- [ ] [P1] Hilt only when dependency graph complexity justifies it
- [ ] [P1] WorkManager only when a real deferred job needs it
- [ ] [P1] common reusable Tooliva components as repetition appears

Do not add these solely to satisfy architecture style.

---

# Phase 2 — CLEANER CORE

## Cleaner shell

- [x] [P0] Clean screen contains only user-facing storage/cleanup concepts
- [x] [P0] Storage used/total/free card
- [x] [P0] one clear Scan/Analyze action when required
- [x] [P0] progressive category/result cards
- [x] [P0] no index/database/generation terminology in production UI
- [x] [P0] scan can be canceled
- [x] [P0] no heavy auto-scan on navigation
- [ ] [P0] Human Xiaomi Cleaner One Scan / Action Plan PASS

## Large Files

- [x] [P0] Full Mode discovers accessible shared-storage file types — human-confirmed on Xiaomi after recovery
- [~] [P0] categories All/Video/Image/Audio/APK/Archive/Document/Download/Other
- [~] [P0] thresholds 100 MB / 500 MB / 1 GB
- [~] [P0] sort size/newest/oldest/name
- [~] [P0] search name/path
- [x] [P0] multi-select / Select all
- [x] [P0] open
- [x] [P0] share single and multiple selected files through content:// / FileProvider
- [x] [P0] details
- [x] [P0] show selected file parent in Files
- [x] [P0] largest-first label and safe type icons/actions
- [x] [P0] safe delete/trash
- [x] [P0] Cleanup Receipt model distinguishes Trash vs Physically Freed
- [x] [P0] current Xiaomi regression PASS after recovery
- [ ] [P1] Samsung physical test
- [ ] [P1] Pixel physical test

## Downloads

- [x] [P0] automatic Full Mode Downloads scan — explicit user action, scoped to Download/Downloads roots; Xiaomi PASS
- [x] [P0] APK/installers group — classification and real-file summary human-verified
- [x] [P0] archives group — ZIP/RAR/7Z/TAR/GZ classification human-verified
- [x] [P0] documents group — PDF/Office/text classification human-verified
- [x] [P0] media group — image/video/audio classification human-verified
- [x] [P0] large downloads filter — 100/500/1024 MB thresholds human-verified
- [x] [P0] old downloads 30/90/180/365 — user-controlled filters human-verified
- [ ] [P1] Limited Mode SAF fallback where useful

## APK installers

- [x] [P0] detect APK files — human-verified on Xiaomi
- [x] [P0] safe app/package/version metadata parsing where possible — details flow human-verified
- [x] [P0] size/date/path/open/share/details/delete — Downloads flow human-verified
- [x] [P0] never preselect solely because old
- [ ] [P1] relation to installed package only when visibility permits
- [ ] [P1] duplicate/old-version installer analysis

## Archives

- [x] [P0] ZIP/RAR/7Z classification where identifiable — human-verified on Xiaomi
- [x] [P0] size/date/path/open/share/details/delete — Downloads flow human-verified
- [x] [P0] Downloads integration

## Documents

- [x] [P0] PDF/Office/text classification — human-verified on Xiaomi
- [x] [P0] size/date/path/open/share/details/delete — Downloads flow human-verified
- [x] [P0] normal documents never generic junk

## Old Files

- [x] [P0] age filters 30/90/180/365 — Downloads implementation human-verified
- [x] [P0] prioritize Downloads/APKs/Archives/Screenshots scopes
- [ ] [P1] user-selected folder scope
- [ ] [P0] do not label `unused` without actual usage evidence
- [x] [P0] reasons, search, sort, empty initial selection and central verified cleanup result
- [ ] [P0] Human Xiaomi Old Files PASS

---

# Phase 3 — SCREENSHOTS / EXPLAINABLE CLEANUP

## Screenshot Cleaner

- [x] [P0] screenshot detection
- [x] [P0] 30/90/365 filters
- [x] [P0] thumbnails
- [x] [P0] multi-select / Select all
- [x] [P0] central Trash/delete flow
- [x] [P0] immediate Cleanup Receipt behavior previously human-verified
- [x] [P0] Full Mode permission unification
- [x] [P0] re-run synthetic screenshot destructive test after permission rewrite

## Explainable Junk

- [x] [P0] implement first real deterministic rule set — Old APK installers and Old Downloads only
- [x] [P0] every group exposes `why shown` — Xiaomi PASS
- [x] [P0] ambiguous candidates not preselected
- [x] [P0] no mystery `Junk = X GB` without drill-down
- [x] [P0] totals derived only from actual listed candidates

Candidate rules to research/implement one by one:

- [x] old APK installer candidate — deterministic rule, reason mapping and tests
- [x] old Downloads candidate — deterministic rule, reason mapping and tests
- [x] [P0] Explainable Cleanup Xiaomi review/select/delete/Cleanup Receipt test
- [ ] exact duplicate candidate
- [x] [P0] accessible deterministic temp/residual artifacts — old fragments in Downloads only
- [x] [P1] safe empty writable-folder candidate scan, revalidation and verified result
- [ ] [P1] Human Xiaomi Empty Folders PASS

## Cleaner 2.0 manual gates

- [ ] [P0] Human Xiaomi Large Files v2 PASS (share/details/show in Files/scan regression)
- [ ] [P0] Human Xiaomi Screenshot Cleaner regression PASS after shared Action Plan integration
- [ ] [P0] Samsung/Pixel/SD/USB validation — not claimed by Xiaomi testing

---

# Phase 4 — CACHE / APPS

## Cache cleanup v1

- [x] [P0] implement official `StorageManager.ACTION_CLEAR_APP_CACHE` flow where supported
- [x] [P0] explain system-mediated behavior
- [x] [P0] handle supported/unsupported/cancel/error honestly
- [x] [P0] no fake per-app private-cache visibility or cache amount
- [x] [P0] Xiaomi manual test — user confirmed the v1 system cache flow works

## Cache Cleaner v2 + manual app-settings cleanup

- [x] [P0] discover installed browsers through intent-based package visibility without `QUERY_ALL_PACKAGES`
- [x] [P0] include YouTube only when `com.google.android.youtube` is installed
- [x] [P0] detect Usage Access grant/revoke and provide a just-in-time disclosure/settings flow
- [x] [P0] measure Android-provided `StorageStats.cacheBytes` off the main thread
- [x] [P0] show unavailable per-app stats honestly and isolate per-package failures
- [x] [P0] browser/video grouping, cache totals, largest-first ordering and empty initial selection
- [x] [P0] select/unselect, Select all, selected total and manual App Info fallback
- [x] [P0] open Android App Info for each selected app as the safe manual cleanup path
- [x] [P0] do not automate Android Settings or claim that cache was cleared by Tooliva
- [x] [P0] remove the failed Xiaomi Accessibility automation experiment from the production path
- [~] [P0] Xiaomi real browser/YouTube cache measurements and Usage Access flow — manual test required

## Phone Optimizer / Memory v1

- [x] [P0] move the official system-mediated cache action into Phone Optimizer
- [x] [P0] show real total RAM, available memory, used estimate and memory pressure
- [x] [P0] show before/after device readings without fake RAM-freed claims
- [x] [P0] handle Full Storage Access, cancel, unsupported and launch failure honestly
- [~] [P0] Xiaomi system action, RAM UI and Home/Clean navigation — manual test required

## App Manager

- [x] [P0] prototype visible app list without `QUERY_ALL_PACKAGES` — Xiaomi PASS
- [x] [P0] label/icon/package/install/update date — Xiaomi PASS
- [x] [P0] user/system distinction where possible — Xiaomi PASS
- [x] [P0] launch — Xiaomi PASS
- [x] [P0] App Info — Xiaomi PASS
- [x] [P0] uninstall request — Xiaomi PASS
- [x] [P1] defensible storage size where available — Xiaomi PASS
- [x] [P1] Usage Access + last used / rarely used recommendations — Xiaomi PASS
- [x] [P0] Human Xiaomi App Manager v1 PASS
- [x] [P1] App Manager rules, progressive enrichment and uninstall queue unit tests
- [x] [P1] App Manager documentation and QA checklist
- [ ] [P1] Xiaomi package visibility measurement and exact gap report
- [ ] [P1] if narrow visibility is insufficient, document exact gap and re-review `QUERY_ALL_PACKAGES`
- [ ] [P1] Evaluate `QUERY_ALL_PACKAGES` only after Xiaomi package visibility measurement and explicit human approval

---

# Phase 5 — FILE MANAGER

File Manager is core functionality.

- [x] [P0] browse accessible shared storage/volumes — Xiaomi manual PASS
- [x] [P0] breadcrumbs/folder navigation — Xiaomi manual PASS
- [x] [P0] category shortcuts Downloads/Documents/APKs/Archives/Images/Videos/Audio/Recent/Large — Xiaomi manual PASS
- [x] [P0] sort name/size/date — Xiaomi manual PASS
- [x] [P0] search by name — Xiaomi manual PASS
- [x] [P0] details — Xiaomi manual PASS
- [x] [P0] open — Xiaomi manual PASS
- [x] [P0] share — Xiaomi manual PASS
- [x] [P0] rename — Xiaomi manual PASS
- [x] [P0] create folder — Xiaomi manual PASS
- [x] [P0] copy — Xiaomi manual PASS
- [x] [P0] move — Xiaomi manual PASS
- [x] [P0] delete/trash through central coordinator — Xiaomi manual PASS
- [x] [P0] collision handling — Xiaomi manual PASS
- [x] [P0] long-operation progress/cancel — Xiaomi manual PASS
- [x] [P0] Human Xiaomi File Manager v1 end-to-end PASS

Samsung/Pixel/SD/USB volume coverage remains unverified.

Do not make basic browser depend on a completed whole-device Room index.

---

# Phase 6 — EXACT DUPLICATES / CLEANUP SWIPE

## Exact duplicates

- [x] [P0] cheap size pre-grouping — non-empty regular files only
- [x] [P0] hash only groups with 2+ candidates — unique sizes never reach the hasher
- [x] [P0] SHA-256 streaming hash with bounded sequential IO
- [x] [P0] exact verification — matching hashes are verified byte by byte
- [x] [P0] no Room/index/background crawler in v1
- [x] [P0] measured bounded local fingerprint cache keyed by path/size/modified time — no file contents persisted
- [x] [P0] file mutation invalidates a hash result
- [x] [P0] groups UI — Xiaomi manual PASS
- [x] [P0] recoverable bytes math
- [x] [P0] keep-one helper and last-copy safety guard
- [x] [P0] safe cleanup + Cleanup Receipt — Xiaomi manual PASS
- [x] [P0] cancellation for metadata traversal, hashing and verification
- [x] [P0] filters/search/sort/open/details/show in Files — Xiaomi manual PASS
- [x] [P0] previews/type icons — Xiaomi manual PASS
- [x] [P0] Human Xiaomi Exact Duplicates v1 PASS
- [x] [P0] repeat-analysis fingerprint cache — Xiaomi manual PASS

## Cleanup Swipe

- [x] [P1] explicit category picker and user-started scan
- [x] [P1] keep/delete/skip buttons and horizontal Keep/Delete gestures
- [x] [P1] undo
- [x] [P1] selected category/session held in memory only
- [x] [P1] selected bytes/count and final review/unselect
- [x] [P1] central file operation + verified Cleanup Receipt integration
- [x] [P1] missing-file/permission-revoked safe fallback and no automatic deletion
- [x] [P1] unit/instrumented rule coverage
- [x] [P1] Human Xiaomi Cleanup Swipe v1 PASS

---

# Phase 7 — STORAGE MAP

- [x] [P1] folder-size aggregation during explicit, cancellable analysis
- [x] [P1] treemap-style prototype using real aggregated bytes
- [x] [P1] drill-down, parent navigation and system Back handling
- [x] [P1] accessible list alternative
- [x] [P1] open/details/delete integration through existing file operations and Cleanup Receipt
- [x] [P1] synthetic Storage Map aggregator tests
- [x] [P1] Human Xiaomi Storage Map v1 PASS

Storage Map is not allowed to become a prerequisite for basic Cleaner/File Manager usage.

---

# Phase 8 — PHONE DOCTOR / CHECK MY PHONE

## Phone Doctor

- [x] [P0] Phone Optimizer Xiaomi manual regression PASS
- [x] [P0] device/model/Android/security patch
- [x] [P0] CPU/ABI public facts
- [x] [P0] RAM/storage facts
- [x] [P0] battery level/state/source/voltage/temperature
- [x] [P1] current/power where exposed
- [x] [P0] thermal state
- [x] [P0] display facts
- [x] [P0] sensor inventory and live values
- [ ] [P1] network details
- [x] [P0] guided hardware tests: display, touch, vibration, flashlight, speaker, microphone and sensors
- [x] [P0] Xiaomi physical Phone Doctor and Hardware Tests PASS

## Check My Phone

- [x] [P0] aggregate existing Cleaner + Doctor insights
- [x] [P0] biggest actionable storage categories as explicit review links
- [x] [P0] deep-links to exact review screens
- [x] [P0] no fake health score
- [x] [P0] Xiaomi Check My Phone flow and Back navigation PASS

---

# Phase 9 — NOTIFICATION HISTORY

- [x] [P0] prominent disclosure
- [x] [P0] API-aware Notification Access detail/settings fallback flow
- [x] [P0] `NotificationListenerService` with off-main persistence and own-package/ongoing filters
- [x] [P0] Room persistence (valid persistence use; notification content only)
- [x] [P0] active-key deduplication/update and removed-state tracking
- [x] [P0] local search, All/Today/7 days/30 days/Pinned and app filters
- [x] [P0] retention controls with pinned-row preservation
- [x] [P0] excluded apps with keep/delete-existing choice
- [x] [P0] pause/include-ongoing settings, per-entry pin/delete/details, clear all
- [x] [P0] revoke handling preserves existing history and stops new capture
- [x] [P0] backup exclusion for notification database/preferences
- [x] [P0] extraction/deduplication/retention/DAO tests
- [x] [P0] Human Xiaomi Notification History v1 PASS
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
