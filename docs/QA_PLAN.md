# Tooliva — QA Plan

Revision: 2026-08-18

Authoritative product source: `docs/PRODUCT_CONSTITUTION.md`

## 1. QA principle

Tooliva touches user files. Wrong-file deletion is unacceptable, and a technically correct architecture that makes the product feel frozen is also a failure.

QA therefore covers both:

- correctness/safety;
- real human usability/perceived responsiveness.

## 2. Testing ownership

### Coding agent owns

- unit tests;
- compile/build checks;
- instrumentation/connected tests where deterministic and safe;
- synthetic datasets;
- regression tests;
- debug APK build;
- installation of the fresh debug APK on the connected Xiaomi;
- crash smoke-check.

### Human user owns

- permission UX;
- system/OEM dialogs;
- perceived speed/responsiveness;
- navigation;
- real scan results;
- file open/select/delete behavior;
- subjective product quality.

**ADB/shell/automated input never substitutes for human device PASS.**

After every device-dependent vertical slice the agent must output:

`MANUAL TEST REQUIRED — <feature>`

with a short numbered checklist and stop before the next major slice.

## 3. Device matrix

Required before Cleaner/File Manager Beta:

- Xiaomi/MIUI or HyperOS — primary active development device
- Samsung/One UI
- Pixel/AOSP-like Android

Android coverage before release:

- Android 11
- Android 12
- Android 13
- Android 14
- Android 15
- Android 16

Legacy compatibility smoke checks where minSdk remains 26:

- Android 8–10

## 4. Known-good regression suite

Every storage refactor must preserve these user-validated behaviors from the `b767aa8` reference baseline:

1. Full Mode can discover synthetic APK.
2. Full Mode can discover ZIP/archive.
3. Full Mode can discover PDF/document.
4. Full Mode can discover image.
5. Full Mode can discover video.
6. Large Files can select multiple items.
7. Large Files can open a selected file via safe URI/FileProvider path.
8. Delete/trash flow works.
9. Cleanup Receipt appears immediately after confirmed operation.
10. Cleanup Receipt distinguishes Trash from physically freed bytes.
11. Screenshot Cleaner works.
12. Screenshot Cleaner -> Home navigation works.
13. Full Mode does not ask for redundant broad media permission for the same Cleaner purpose after permission unification is implemented.
14. Opening Clean/Large Files does not start a heavy scan that makes the app feel frozen.

A refactor that fails this suite is not accepted even if automated architecture tests pass.

## 5. Storage permission matrix

### Android 11+ Full Mode

Test manually:

- access absent;
- disclosure understandable;
- open All Files Access settings;
- grant;
- return to Tooliva;
- state refreshes;
- Large Files uses Full Mode;
- Screenshot Cleaner does not ask for a second redundant Photos permission for the same task;
- revoke Full Mode;
- return to Tooliva;
- no crash;
- feature switches to truthful Limited Mode behavior.

### Limited Mode

- deny Full Mode;
- enter a media feature;
- granular media access is requested only when genuinely required;
- deny media permission;
- app remains usable;
- coverage is labelled Limited;
- SAF cancellation does not dead-end the UI.

## 6. Synthetic storage dataset

Maintain disposable fixtures only.

Minimum:

- APK;
- ZIP;
- RAR/7Z when supported;
- PDF;
- DOC/DOCX;
- XLS/XLSX;
- PPT/PPTX;
- TXT;
- images;
- videos;
- audio;
- screenshots old/new;
- unknown extension;
- zero-byte file;
- nested folders;
- Unicode/symbol names;
- same filename in different folders;
- exact duplicate bytes;
- same-size different-content files;
- >100 MB files;
- >500 MB files where practical;
- >1 GB fixture only where storage/time safely allows.

Never use irreplaceable personal files for destructive tests.

## 7. Direct scan tests

Automated where possible:

- no main-thread filesystem traversal;
- progressive `EntryFound` behavior;
- cancellation;
- restart after cancel;
- one unreadable entry does not fail the scan;
- file disappears while scanning;
- file metadata changes;
- protected path excluded;
- malformed/unreadable entry handled;
- no unbounded coroutine-per-file fan-out;
- no ordinary scan hashing/file-content reads;
- no mandatory Room persistence requirement.

Manual Xiaomi:

- tap explicit Scan/Refresh;
- first useful result appears without long frozen state;
- files visibly appear progressively where expected;
- navigation remains responsive;
- Cancel reacts promptly;
- repeated use does not require mysterious index-building step.

Measure, but do not make brittle CI thresholds:

- tap -> first useful result;
- scan completion time;
- cancellation latency;
- memory/jank/ANR observations.

## 8. Large Files tests

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

Thresholds:

- 100 MB+
- 500 MB+
- 1 GB+

Sorting:

- size
- newest
- oldest
- name

Actions:

- search name/path;
- select/deselect;
- Select all visible;
- open;
- share when implemented;
- details when implemented;
- delete/trash;
- cancel operation;
- Cleanup Receipt;
- visible list reconciles without forcing whole-device rescan.

## 9. Downloads/APK/Archive/Document tests

Downloads:

- installers;
- archives;
- documents;
- media;
- old-age boundaries;
- large-size thresholds.

APK:

- valid APK metadata;
- malformed APK;
- old APK never auto-selected solely by age;
- open/share/delete.

Archives:

- ZIP/RAR/7Z classification;
- unknown/misleading extension;
- open/share/delete.

Documents:

- PDF/Office/text;
- normal document never generically labelled junk.

## 10. Screenshot Cleaner tests

- Full Mode path when All Files Access is granted;
- Limited MediaStore path when Full Mode absent;
- no duplicate permission wall;
- screenshot bucket/path variants;
- 30/90/365 filters;
- thumbnails;
- missing/corrupt thumbnail;
- multi-select;
- Select all;
- Trash cancel;
- Trash success;
- Cleanup Receipt;
- Home navigation.

## 11. Explainable Junk tests

Every rule requires:

- positive fixture;
- negative fixture;
- ambiguous fixture;
- reason text/domain reason;
- default-selected policy;
- byte total reconciliation.

No unexplained aggregate `Junk = X GB`.

## 12. Exact duplicate tests

- exact bytes/different names;
- exact bytes/different folders;
- same size/different content;
- modified file invalidates fingerprint;
- candidate size grouping avoids hashing unique sizes;
- hashing cancellation;
- keep-one never selects every copy;
- recoverable byte total correct;
- Cleanup Receipt correct.

Room fingerprint cache is valid here because persistent hashing work has demonstrated value.

## 13. File Manager tests

Navigation:

- volume/root;
- nested folders;
- breadcrumbs;
- category shortcuts.

Operations:

- open;
- share;
- rename;
- create folder;
- copy;
- move;
- delete/trash.

Failure/collision:

- destination exists;
- insufficient space;
- source disappears;
- permission revoked;
- volume removed;
- cancel mid-copy/move;
- move fallback verifies destination before deleting source.

Basic browsing must work without completion of a whole-device database index.

## 14. Destructive operation suite

For image/video/APK/archive/PDF/document/audio/unknown:

- one selected;
- many selected;
- cancel before action;
- cancel platform action;
- success;
- partial failure;
- already missing;
- permission revoke;
- visible-list reconciliation.

Receipt must correctly report:

- requested;
- missing;
- Trash;
- physically freed;
- unchanged/failed;
- canceled;
- permission revoked.

## 15. Cache cleanup tests

On supported Android:

- Full Mode missing;
- official cache action launch;
- user cancels;
- OEM unsupported/unexpected behavior;
- no fake cache byte count;
- no claim of direct private-cache access.

Cache Cleaner v2 automated tests:

- browser discovery is intent-based, deduplicated and excludes unavailable packages;
- YouTube appears only when installed;
- Usage Access denied/granted/revoked states are truthful;
- cache stats measured/zero/unavailable/security/io/package-disappeared cases are isolated;
- selection starts empty, Select all excludes unavailable values, selected totals are exact;
- before/after reduction clamps negative values to zero and never fabricates an unavailable result;
- manual App Info intent opens the selected package;
- Tooliva does not declare or enable AccessibilityService for this flow;
- unavailable/zero values remain honest and the user can clear cache directly in Android settings.

Manual Xiaomi cache slice remains human-owned: verify browser/YouTube values, selected totals, App Info opening for each selected app and the user's manual Clear cache action.

## 16. App Manager tests

Before broad package visibility:

- test actual visible apps;
- record which core user behavior is missing.

Only if `QUERY_ALL_PACKAGES` is later explicitly approved:

- user/system apps;
- launch;
- App Info;
- uninstall request;
- no package inventory in logs/analytics.

Usage Access:

- deny/grant/revoke;
- no `unused` label without evidence.

## 17. Phone Doctor tests

Cross-check against system/ADB where reasonable:

- battery level/temp/voltage;
- RAM/storage;
- device/build info;
- thermal state;
- sensors.

Unsupported values show `Unavailable`, never fabricated values.

Phone Optimizer tests additionally cover:

- real total/available memory, non-negative used estimate and Normal/High pressure mapping;
- system cache action launch/cancel/unsupported/error;
- before/after memory/storage readings without claiming RAM freed;
- Xiaomi manual navigation and system-dialog behavior.

## 18. Notification History tests

- access deny/grant/revoke;
- received/removed notifications;
- excluded apps;
- retention;
- search/filter;
- large local dataset;
- no notification content in logs/network analytics.

## 19. Ads/privacy regression

No ad:

- before useful scan result;
- on storage permission explanation;
- in select -> confirm -> operation -> Receipt flow;
- in Vault/authentication.

Network/privacy audit during storage scan:

- no filename/path/hash/content upload;
- no app inventory upload;
- no notification text upload.

## 20. Release gates

### Cleaner Recovery Gate

Must pass before new Cleaner modules:

- mandatory index experiment removed from primary flow;
- direct progressive Large Files restored;
- no index UI/autoscan regression;
- Full Mode permission UX unified;
- automated build/tests green;
- fresh APK installed;
- human Xiaomi PASS.

### Cleaner/File Manager Beta

Must additionally have:

- Large Files current Xiaomi PASS;
- Downloads/APK/Archives/Documents/Old Files;
- explainable cleanup rules;
- Screenshot Cleaner;
- Cache v1;
- File Manager critical operations;
- Exact Duplicates;
- destructive regression suite;
- Samsung + Pixel smoke coverage;
- CI green.

### Tooliva 1.0

Additionally:

- App Manager;
- Phone Doctor/Checkup;
- Notification History;
- privacy/Data Safety/restricted-permission review;
- monetization regression;
- closed test with no P0 file-loss or major UX regression.
