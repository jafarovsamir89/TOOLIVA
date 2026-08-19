# Tooliva — Cleaner Blueprint

Status: AUTHORITATIVE PRODUCT UX REFERENCE  
Revision: 2026-08-18

Read after `docs/PRODUCT_CONSTITUTION.md` and `docs/DECISION_LOG.md`.

This document translates competitive research into Tooliva behavior. It defines what the user should experience, not how many internal classes/services exist.

## 1. Reference products and what to learn

### CCleaner

Learn:
- broad cleanup/maintenance story;
- app/storage analysis;
- simple consumer language.

Do not copy:
- ad/subscription friction;
- generic optimization theater.

### Phone Cleaner — AI Cleaner

Learn:
- broad storage categories;
- large files/folders;
- APK/temp/residual candidates;
- App Manager + File Manager bundle;
- immediate consumer expectation: grant access -> scan -> actionable list.

Do not copy:
- aggressive ads/scare-style monetization.

### AVG Cleaner

Learn:
- one clear Analyze action;
- combined files/apps cleanup;
- simple recommendations.

### Files by Google

Learn:
- trust;
- recommendation cards;
- explain/review/delete;
- simple file browsing/search;
- no fake danger language.

### 1Tap Cleaner

Learn:
- one problem -> one obvious action;
- visible system/user progress;
- do not make the user understand internals.

### SD Maid 2/SE

Learn:
- technical depth;
- All Files Access setup on modern Android;
- leftovers/system-cleaner concepts;
- duplicates;
- file manager;
- Swiper/review mechanics;
- app/file attribution.

Do not copy:
- power-user complexity into mainstream surfaces.

### Storage Analyzer

Learn:
- finding hidden storage consumers;
- folder/category size analysis;
- visual Storage Map as a later differentiator.

### Cleaner + Antivirus + VPN bundles

Learn:
- broad bundles can acquire users.

Tooliva decision:
- do not add antivirus/VPN/booster theater merely to increase feature count;
- Cleaner/File Manager quality remains the acquisition core.

## 2. First-run storage access

### Android 11+ preferred flow

When user first enters a feature that needs broad storage:

1. show one Tooliva disclosure card/screen;
2. explain: Tooliva needs shared-storage access to find large files, Downloads, APKs, archives, documents, screenshots and to manage files;
3. user taps `Enable storage access`;
4. Android All Files Access settings opens;
5. user grants;
6. return to Tooliva;
7. Tooliva shows Full Mode ready.

Do not immediately show a second `Allow Photos` request for the same Cleaner purpose.

If Full Mode denied:
- continue in Limited Mode;
- explain reduced coverage;
- request granular media permission only inside a Limited Mode media feature that genuinely needs it.

## 3. Home screen

Home is not the Cleaner result screen.

Core cards:
- Clean Storage
- Files
- Phone Doctor
- Notification History later
- Apps later

Primary CTA may remain `CHECK MY PHONE`, but Cleaner must be reachable in one tap.

No auto heavy storage scan just because Home opened.

## 4. Clean screen before scan

Show:

- `Clean` title;
- storage used / total / free;
- Full/Limited access state only if useful;
- one primary `SCAN` / `ANALYZE` action;
- direct shortcuts such as Large Files and Screenshots may remain accessible independently.

Do not show:

- Storage Index;
- Fast/Deep generation;
- Room status;
- database counts;
- developer/debug progress.

## 5. Scan behavior

Scan is explicit user action.

Once tapped:

- button becomes Cancel/Scanning state;
- UI remains responsive;
- matching category cards/results update progressively;
- user can navigate away if architecture safely supports it, but no hidden permanent scan is required;
- do not claim fake percentage if total work is unknown.

Useful progress examples:

- `Checking shared storage…`
- `Found 12 large files`
- `APK installers: 840 MB`
- `Archives: 2.1 GB`

Bad progress examples:

- `Index generation 4`
- `25 GB indexed` when 25 GB is not actual completion percentage;
- `Deep snapshot activating`.

## 6. Scan result cards

Target cards:

### Large Files
Example:
`12.4 GB · 34 files`

### Downloads
Example:
`4.7 GB · 183 files`

### APK Installers
Example:
`1.2 GB · 16 files`

### Archives
Example:
`3.1 GB · 21 files`

### Documents
Show reviewable bytes/count, never call generic documents junk.

### Screenshots
Example:
`1.4 GB · 839 screenshots`

### Old Files
Only under clear age/scope rules.

### Exact Duplicates
Only after exact verification.

### Cache
System-mediated action; no fake private-cache byte claim unless defensibly measured.

### Apps
Later: large/unused apps with separate access where needed.

## 7. Large Files screen

User enters Large Files.

If results are not already available, show a clear explicit `Scan large files` action rather than silently starting a heavy full-storage task.

During scan:

- files appear progressively;
- filter/sort/search stays responsive;
- user can cancel.

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

Thresholds:
- 100 MB+
- 500 MB+
- 1 GB+

Sort:
- largest
- newest
- oldest
- name

Actions:
- select;
- select all visible;
- open;
- share;
- details;
- delete/trash;
- later locate in Files.

## 8. Screenshot Cleaner

If Full Mode is granted:
- use Full Mode-compatible discovery/access;
- no second broad Photos permission solely for this feature.

If Limited Mode:
- use MediaStore;
- request media permission just-in-time if needed.

UI:
- thumbnail list/grid;
- 30/90/365 days;
- multi-select;
- Select all;
- bottom Trash action;
- Cleanup Receipt.

## 9. Cleanup interaction

Before deletion:

- selected count;
- selected bytes;
- clear action label;
- platform confirmation when required.

During preparation/action:
- show meaningful progress (`Preparing cleanup…`);
- do not simply gray out the button without explanation.

After confirmed operation:
- Cleanup Receipt appears immediately;
- visible list reconciles afterward;
- no forced whole-device rescan before showing receipt.

## 10. Cleanup Receipt

Required fields/states:

- selected/requested;
- deleted/physically freed;
- moved to Trash;
- already missing;
- failed/unchanged;
- canceled;
- permission revoked.

Example:

> Selected: 5.8 GB  
> Physically freed: 1.4 GB  
> Moved to Trash: 4.4 GB  
> Failed: 0

Add explanatory copy when Trash still occupies storage.

## 11. Explainable Junk

Tooliva should say `Ready for review` rather than pretending everything is safe junk.

Example drill-down:

- Old APK installers — 1.3 GB
- Old Downloads — 2.4 GB
- Exact duplicates — 1.1 GB
- Old screenshots — 824 MB
- Accessible temp artifacts — 620 MB

Each group has `Why shown?` explanation.

Never preselect ambiguous documents/user files.

## 12. Cache UX

V1:

- card: `App cache`
- explain Android/system will handle the action;
- tap opens official system-mediated cache flow;
- return state handled honestly.

Do not promise:
- silent one-tap private-cache wipe;
- exact cache bytes if Tooliva cannot defensibly know them.

## 13. File Manager UX

File Manager later but core:

Main categories:
- Internal storage
- Downloads
- Documents
- APKs
- Archives
- Images
- Videos
- Audio
- Recent
- Large

Folder browser:
- breadcrumbs;
- list/grid;
- search;
- sort;
- selection;
- open/share/details;
- rename/copy/move/delete/create folder.

Basic browsing must not wait for a global phone index.

## 14. Navigation rules

Bottom nav actions should be deterministic:

- Home always goes Home;
- Clean always goes Clean root;
- nested Clean screen must not swallow Home navigation;
- Back follows screen hierarchy;
- no automatic scan caused solely by tab re-selection.

## 15. Performance acceptance

A product flow fails if the user feels compelled to close the app because it appears frozen, even if background code is technically progressing.

Acceptance questions:

- Does the user retain control?
- Do useful results appear without an unnecessary multi-minute wait?
- Is every waiting state understandable?
- Can the user cancel?
- Did a refactor make a previously working screen slower or less reliable?

If yes to the last question, fix/revert before continuing.

## 16. Monetization UX

Do not imitate ad-heavy cleaners.

No ads:
- before first useful result;
- in permission flow;
- in select/delete/receipt flow;
- in sensitive screens.

Later free monetization can use restrained placements at natural non-sensitive points.

## 17. Definition of a successful Cleaner Beta

A normal user can:

1. install Tooliva;
2. understand/enable storage access;
3. enter Cleaner;
4. start one understandable scan/action;
5. quickly see useful categories;
6. open Large Files and see APK/archive/document/media results;
7. review/select/open/delete;
8. understand exactly what was removed;
9. browse/manage files;
10. never encounter fake optimization/scareware behavior.

## 18. App cache analysis and selected cleanup

Cache Cleaner v2 is a focused app-cache review, not a whole App Manager. It discovers installed browsers through an `ACTION_VIEW`/`BROWSABLE` web intent and adds YouTube only when that exact package is installed. After explicit Usage Access consent, `StorageStatsManager` supplies Android-provided `StorageStats.cacheBytes` off the main thread.

Nothing is selected or cleaned automatically. Unavailable statistics remain unavailable, and totals include only measured cache bytes. Users can select apps, see the exact selected total, refresh, or open App Info for manual Clear cache.

Cleanup is manual and user-controlled. Tooliva opens Android App Info for each selected app; the user reviews the page and presses Clear cache. Tooliva does not automate Settings, inspect browser content or claim a cleanup result that Android has not directly exposed.
