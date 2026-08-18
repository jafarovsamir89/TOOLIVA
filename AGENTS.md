# Tooliva — Instructions for AI Coding Agents

Status: AUTHORITATIVE  
Revision: 2026-08-18

This file governs Codex/Claude/Gemini and other coding agents working in this repository.

## 0. Required reading and precedence

Before any major task, read in this order:

1. `docs/PRODUCT_CONSTITUTION.md`
2. `docs/DECISION_LOG.md`
3. `AGENTS.md`
4. `TECH_SPEC.md`
5. `ARCHITECTURE.md`
6. `TODO.md`
7. `docs/PLAY_POLICY.md`
8. `docs/QA_PLAN.md`
9. `docs/MARKET_RESEARCH_2026.md`
10. `docs/design/README.md` and both WebP references for UI work

If an older comment, TODO item, implementation experiment, or lower-priority document conflicts with a higher-priority source, follow the higher-priority source and update the stale documentation.

## 1. Mission

Build **Tooliva — Cleaner, File Manager & Device Tools**.

Until Cleaner + File Manager Beta is strong, optimize for:

- immediate user value;
- simple predictable UX;
- safe deletion;
- fast progressive results;
- privacy;
- truthful behavior;
- Google Play compliance;
- maintainable code;
- regression prevention.

Do not optimize for architecture elegance at the cost of product usability.

## 2. Hard rules

1. Kotlin + Jetpack Compose for app implementation unless a dependency requires otherwise.
2. Target API 36; minSdk 26 unless explicitly changed.
3. Cleaner must not be gallery-only in Full Mode.
4. `MANAGE_EXTERNAL_STORAGE` is approved for Cleaner/File Manager prototype/full-mode development, subject to final Play declaration/review.
5. When Full Storage Access is granted, do not request redundant broad image/video permission for the same Cleaner storage job.
6. MediaStore/SAF are Limited Mode and specialized platform tools, not a second permission wall after Full Mode.
7. Never add `QUERY_ALL_PACKAGES` without a demonstrated App Manager need, current policy review, documentation, and explicit approval.
8. Never add AccessibilityService without a separate explicit human decision and policy review.
9. Never add permissions “just in case.”
10. Never bypass Android protected/private storage using root, exploits, hidden APIs, or deceptive flows in the normal Play build.
11. Never delete a user file without explicit user selection/review/confirmation appropriate to the platform.
12. Never implement fake RAM boost, CPU cooling, fake antivirus alerts, fake phone-health scores, or fake reclaimable bytes.
13. Never upload filenames, file contents, hashes, notification text, Vault content, or installed-app inventory to analytics/ads/server unless a future feature explicitly requires and documents user-controlled transfer.
14. Core Cleaner/File Manager functions work offline.
15. No ads on permission disclosure, destructive confirmation, operation progress, or Cleanup Receipt.
16. Do not copy proprietary competitor code, assets, strings, branding, or UI artwork.
17. Do not mark placeholders/mock values as completed functionality.
18. Do not start a new major module while the current device-dependent slice is waiting for human manual PASS/FAIL.

## 3. Known-good baseline and rejected index experiment

Reference known-good baseline:

`b767aa8` — direct progressive Full Storage Large Files flow + Cleanup Result/navigation fixes.

The user manually confirmed on Xiaomi that this path could find/delete APK, ZIP, PDF, DOC, PNG and MP4 fixtures and that Screenshot Cleaner/navigation worked.

Rejected as primary Cleaner architecture:

- `7836ea` — mandatory Room Storage Index
- `71f35ca` — fast-first/non-blocking index attempt

Do not build more layers on top of this rejected index-first approach.

The primary Cleaner path must return to/directly preserve:

```text
StorageProvider -> Flow<StorageScanEvent> -> classifiers -> progressive UI
```

Room may be used later only for demonstrated persistence needs such as duplicate fingerprints, Notification History, saved decisions/history, or a small measured cache that is not a mandatory gateway.

## 4. Simplicity / anti-overengineering gate

Before adding a database, cache, index, worker, background service, coordinator, queue, new abstraction layer, or concurrency framework, answer:

1. What current measured user-visible problem does it solve?
2. Can the problem be fixed with a smaller change?
3. Will it preserve the known-good flow?
4. How will we prove on the Xiaomi device that UX improved?

If the problem is hypothetical, do not add the complexity.

Never justify a regression with “more scalable architecture.”

## 5. Cleaner architecture

The ordinary Cleaner scan is a progressive discovery/classification task, not a requirement to mirror the entire phone into a database.

Preferred flow:

```text
explicit user action
  -> StorageProvider
  -> cancellable IO traversal/platform query
  -> lightweight classifiers
  -> progressive real results
  -> review/select
  -> safe operation
  -> verified Cleanup Receipt
```

Rules:

- never scan on main thread;
- emit matching useful results as they are found;
- no required full scan before showing Large Files;
- no automatic heavy whole-storage scan merely because a screen opened;
- no full-file hashing in ordinary Cleaner scan;
- no thumbnail generation for every file;
- no requirement to retain every small file in memory/Room;
- isolate unreadable/disappearing files instead of failing the whole scan;
- cancellation must work;
- protected directories remain excluded.

One traversal may feed multiple cheap classifiers when this reduces work without complicating UX.

## 6. Storage access model

### Full Mode — Android 11+

When `Environment.isExternalStorageManager()` is true, the primary shared-storage Cleaner/File Manager path uses Full Mode.

Full Mode covers, where Android permits:

- Large Files;
- Downloads;
- APKs;
- Archives;
- Documents;
- media;
- Screenshot Cleaner;
- File Manager.

Do not ask for `READ_MEDIA_IMAGES/VIDEO` solely because a Full Mode Cleaner sub-screen was opened.

### Limited Mode

When Full Mode is denied/not supported:

- use MediaStore/SAF where appropriate;
- request granular media permission only for a feature that genuinely needs it;
- label limited coverage honestly;
- never pretend the whole storage was scanned.

## 7. Cleaner UX

User-facing primary concepts:

- storage used/free;
- Scan/Analyze;
- Large Files;
- Downloads;
- APK installers;
- Archives;
- Documents;
- Screenshots;
- Old Files;
- Duplicates;
- Cache;
- Apps;
- reviewable/reclaimable bytes;
- Cleanup Receipt.

Never make engineering concepts primary UI:

- Storage Index;
- generations/scopes;
- Room progress;
- fast/deep index;
- coordinator state.

If scan work takes time, show meaningful result/progress concepts, not internal database state.

## 8. Explainable cleanup

Every cleanup candidate/group has a reason.

Acceptable examples:

- file larger than chosen threshold;
- old APK installer;
- archive in Downloads;
- screenshot older than user-selected age;
- exact duplicate;
- deterministic accessible temp/residual rule;
- empty writable folder;
- old download selected through an explicit age filter.

Rules:

- ambiguous candidates are not preselected;
- normal documents are not generic junk;
- age alone does not make an APK/document safe to delete;
- `unused` requires real Usage evidence;
- `duplicate` means exact verified match unless UI explicitly says `similar`.

## 9. Destructive operations / Cleanup Receipt

Centralized cleanup/delete code is authoritative.

Required flow:

1. review/select;
2. show selected count + bytes;
3. user/system confirmation;
4. execute;
5. verify/re-stat;
6. show Cleanup Receipt immediately;
7. reconcile visible results without an unnecessary full-device rescan.

Receipt distinguishes:

- requested;
- missing before action;
- moved to Trash;
- physically freed;
- unchanged/failed;
- canceled;
- permission revoked.

Trash bytes are never reported as physically freed.

Do not replace the receipt with a generic `Cleaned X GB` message.

## 10. File Manager rules

File Manager is real core functionality.

Eventually P0:

- shared-storage/volume browsing;
- category shortcuts;
- name/size/date sorting;
- search;
- details/open/share;
- rename;
- copy/move;
- create folder;
- delete/trash;
- collision handling;
- long-operation progress/cancel.

Reuse storage/file-operation primitives from Cleaner where practical, but never require a complete whole-device database index before browsing/searching basic files.

## 11. Cache cleaning

V1 uses official system-mediated cache clearing where supported.

Do not claim Tooliva can silently delete every other app's private cache.

Accessibility automation is deferred until a separate approval/policy decision.

## 12. Visual rules

Read:

- `docs/design/tooliva-ui-showcase.webp`
- `docs/design/tooliva-ui-system.webp`
- `docs/design/README.md`

Preserve:

- dark graphite surfaces;
- teal/cyan primary actions;
- restrained secondary accents;
- rounded Material 3 cards;
- strong readable hierarchy;
- 4dp grid;
- minimum 48dp touch targets;
- dark-first + supported light theme;
- no fake danger/scare styling.

Do not expose backend/debug concepts because they happen to exist in code.

## 13. Testing ownership — mandatory workflow

The agent performs:

- unit tests;
- compile/build checks;
- instrumentation/connected tests when safe and deterministic;
- fresh debug APK build;
- APK installation on the connected Xiaomi;
- crash smoke-check only.

**The human user performs manual functional phone testing.**

ADB/shell/UI automation does not substitute for user PASS.

After every device-dependent vertical slice:

1. build/test;
2. install fresh debug APK;
3. launch only for crash smoke-check;
4. stop coding the next major feature;
5. output `MANUAL TEST REQUIRED — <feature>`;
6. give a short numbered checklist;
7. wait for the user's PASS/FAIL;
8. fix failures;
9. only after human PASS mark device-dependent TODO items `[x]`.

## 14. Regression gate

Every refactor must preserve, unless explicitly changed by the human owner:

- Full Mode discovers APK/ZIP/PDF/document/image/video fixtures;
- Large Files shows progressive useful results;
- select/open/delete works;
- Cleanup Receipt appears immediately after confirmed action;
- Screenshot Cleaner remains usable;
- Home navigation works;
- Full Mode does not trigger a redundant media permission request;
- opening a screen does not start a heavy scan that makes the app feel frozen.

If any invariant breaks, stop new work and fix/revert the regression first.

## 15. Coding discipline

Before coding:

1. read authoritative docs;
2. inspect existing implementation and known-good history;
3. identify exact TODO slice;
4. explain the smallest coherent change;
5. preserve working code where possible;
6. implement;
7. add tests;
8. build;
9. install APK if device-dependent;
10. provide manual checklist;
11. update TODO only to verified reality;
12. commit/push focused changes.

Avoid broad refactors during feature work.

Do not create speculative abstractions with no current consumer.

## 16. Definition of done

A task is complete only when:

- stated behavior works end-to-end;
- loading/error/empty/permission-denied states exist where relevant;
- automated tests/build pass;
- human device PASS exists when required;
- known-good regressions are not introduced;
- privacy/Play rules remain valid;
- design reference is respected;
- TODO/docs reflect reality;
- no fake/mock production data remains.
