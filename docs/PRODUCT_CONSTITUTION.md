# Tooliva — Product Constitution

Status: AUTHORITATIVE  
Revision: 2026-08-18

This document is the highest-level product and engineering source of truth for Tooliva. If an older document, TODO item, comment, prototype, or agent suggestion conflicts with this file, follow this file and record the conflict in `docs/DECISION_LOG.md`.

## 1. North star

**Tooliva — Cleaner, File Manager & Device Tools**

Tooliva must first be an excellent Android Cleaner and File Manager. Device diagnostics and recurring utilities come after that core is strong.

The primary user promise is simple:

> Find what is taking space, review it quickly, remove it safely, and understand exactly what happened.

Tooliva is not an architecture demo, benchmark project, fake optimizer, or wall of shallow tools.

## 2. Market model

Tooliva is built from proven consumer expectations seen in successful Android utilities such as CCleaner, AVG Cleaner, Files by Google, Phone Cleaner / AI Cleaner, 1Tap Cleaner, SD Maid 2/SE, Storage Analyzer, and established file managers.

We borrow product principles, not proprietary code, assets, strings, branding, or copied layouts.

The proven market workflow is:

1. user grants the storage access needed for the feature;
2. user opens Cleaner or taps one clear Scan/Analyze action;
3. useful categories/results appear progressively;
4. user reviews concrete files/categories;
5. user selects what to remove;
6. Tooliva performs a safe action;
7. Tooliva verifies the result and shows a Cleanup Receipt.

The user must never be required to understand internal terms such as database generation, active snapshot, storage index generation, fast index, deep index, Room scope, or coordinator state.

## 3. Product priorities

Until Cleaner + File Manager Beta is genuinely good, priority is:

1. Storage access UX
2. Cleaner scan and results
3. Large Files
4. Downloads / APK / Archives / Documents / Old Files
5. Screenshot Cleaner
6. Explainable junk rules
7. Cache-cleaning system flow
8. File Manager
9. Exact duplicates
10. Cleanup Swipe
11. App Manager
12. Storage Map
13. Phone Doctor / Check My Phone
14. Notification History
15. Vault / App Lock / content tools / smaller utilities
16. Monetization polish

Do not jump ahead merely because a later feature is easier or more interesting.

## 4. Known-good baseline

The important known-good product baseline is commit:

`b767aa8` — `Fix cleanup result and home navigation`

On Xiaomi, the user manually confirmed the working Full Storage flow around this baseline:

- All Files Access could be granted;
- Large Files found synthetic APK, ZIP, PDF, DOC, PNG and MP4 files;
- filters/selection worked;
- files could be opened;
- selected files could be deleted;
- Cleanup Result worked after the timing fix;
- Screenshot Cleaner worked;
- Home navigation from Screenshot Cleaner worked.

This baseline is important because it proves the direct `StorageProvider -> Flow<StorageScanEvent> -> UI` approach works on a real phone.

## 5. Rejected experiment: mandatory Storage Index

Commits:

- `7836ea` — Storage Index v1
- `71f35ca` — fast-first/non-blocking index attempt

These commits introduced Room index tables, generations, active scopes, index coordinator, index UI, and made Large Files depend on indexed snapshots.

The user manually tested the result and reported a major product regression:

- first scan took minutes and felt like the phone was hanging;
- Large Files stopped working reliably;
- Cleaner gained technical clutter;
- automatic scans happened without clear user intent;
- user faced confusing duplicate storage/media permission flows;
- the experience was worse than the already-working pre-index version.

**Decision:** mandatory/index-first Cleaner architecture is rejected.

Do not attempt to fix this rejected direction by adding another scheduling/cache/index abstraction on top of it.

Room is not forbidden. Room is allowed only where persistent state has a demonstrated product need, e.g. notification history, duplicate fingerprint cache, saved cleanup decisions/history, settings-related structured data, or a small optional cache proven by measurements to improve UX.

Room must never become a mandatory gateway between a newly discovered file and the Cleaner UI unless a new measured problem and explicit human decision justify it.

## 6. Cleaner engine contract

The core Cleaner path is intentionally simple:

```text
User action
   ↓
StorageProvider
   ↓
progressive filesystem / platform scan
   ↓
classifier pipeline
   ├─ Large Files
   ├─ Downloads
   ├─ APK
   ├─ Archives
   ├─ Documents
   ├─ Old Files
   └─ explainable cleanup candidates
   ↓
UI updates progressively
```

Requirements:

- never scan on the main thread;
- emit useful results as they are found;
- cancellation must work;
- no full-tree-in-memory requirement;
- no hashing during ordinary Cleaner scan;
- no thumbnail generation for every file;
- no database write for every discovered file unless explicitly justified;
- one file failure must not abort the scan;
- no protected-path bypasses;
- no automatic heavy scan simply because a screen opened;
- user control is preferred over hidden heavy work.

A single traversal may feed multiple lightweight classifiers. The goal is useful cleanup results, not a perfect database mirror of the entire phone.

## 7. Performance rule

**Optimize measured problems, not hypothetical future problems.**

Before adding a cache, index, background worker, new database, multi-stage scan, or concurrency framework, answer:

1. What measured user-visible problem exists now?
2. What is the simplest change that fixes it?
3. Does the new design improve time-to-useful-result on the real Xiaomi test device?
4. Does it preserve the already-working flow?

If the problem is only hypothetical, do not add the complexity.

No regression is acceptable merely because the architecture is theoretically more scalable.

## 8. Permission contract

### Android 11+ Full Mode

For the core Cleaner/File Manager flow, `MANAGE_EXTERNAL_STORAGE` is the preferred broad shared-storage access when granted.

If Full Storage Access is granted:

- Large Files must use it;
- Downloads/APK/Archives/Documents must use it;
- File Manager must use it;
- Screenshot Cleaner should not ask for an additional broad photo permission solely to inspect shared-storage screenshots when Full Mode can perform that job.

Do not create a confusing permission wall where the user grants All Files Access and is immediately asked for Photos/Videos for the same Cleaner job.

### Limited Mode

If Full Storage Access is denied/not supported, MediaStore and SAF may provide a reduced feature set. In Limited Mode, request granular media access only when the user enters a media feature that genuinely needs it.

The UI must clearly state that coverage is limited.

### Other restricted access

- `QUERY_ALL_PACKAGES`: not approved automatically; add only after a real App Manager need is demonstrated and current Play policy is re-reviewed.
- AccessibilityService: not approved for Cleaner/App Lock without a separate explicit human decision and policy review.
- Usage Access: only for real app-usage/unused-app features after user opt-in.
- Notification Access: only for Notification History after prominent disclosure.

## 9. Cleaner UX contract

The Cleaner screen is consumer-facing, not engineering-facing.

Allowed primary concepts:

- storage used/free;
- Scan / Analyze;
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
- recoverable/reviewable bytes;
- Cleanup Receipt.

Do not expose as primary UI:

- Storage Index;
- Fast generation;
- Deep generation;
- Room state;
- indexed scope;
- database progress;
- coordinator status.

Do not auto-start a heavy scan merely by navigating to Large Files or Clean unless explicit product research/measurement proves that behavior is superior and the user can still interact normally.

## 10. Explainable cleanup

Tooliva never invents magic junk totals.

Each cleanup group must explain why it is shown, e.g.:

- Old APK installers
- Archives in Downloads
- Screenshots older than 90 days
- Exact duplicate files
- Temporary files matched by a deterministic rule
- Empty writable folders
- Old downloads selected by user-defined age

Normal documents are never called junk simply because they are old or large.

Ambiguous candidates are not preselected.

## 11. Destructive-operation contract

The centralized cleanup/delete infrastructure is a permanent product asset.

Required flow:

1. user reviews/selects;
2. selected count and bytes are visible;
3. user/system confirmation where required;
4. operation runs;
5. result is verified;
6. Cleanup Receipt is shown immediately;
7. UI list is reconciled without forcing an unnecessary full-device rescan.

Cleanup Receipt distinguishes:

- requested;
- already missing;
- moved to Trash;
- physically freed;
- unchanged/failed;
- canceled;
- permission revoked.

Trash bytes are never reported as physically freed bytes.

No ads interrupt selection -> confirmation -> action -> receipt.

## 12. File Manager contract

File Manager is a genuine core feature, not a permission-justification placeholder.

P0 eventually includes:

- browse shared storage/volumes;
- Downloads/Documents/APKs/Archives/Images/Videos/Audio shortcuts;
- search;
- sort by name/size/date;
- details;
- open/share;
- rename;
- copy/move;
- delete/trash;
- create folder;
- collision handling;
- long-operation progress/cancel.

It should reuse the same storage/file-operation domain used by Cleaner, but it must not be blocked on a whole-device database index.

## 13. Cache cleaning

For V1, prefer the official Android system-mediated cache action where supported.

Do not claim direct silent access to private cache of every other app.

Advanced Accessibility-based cache automation is a later, separately approved experiment only.

## 14. Testing ownership

Automated testing belongs to the coding agent:

- unit tests;
- compile checks;
- instrumentation/connected tests where safe and deterministic;
- build APK;
- install fresh debug APK;
- crash smoke-check.

**Manual functional testing on the phone belongs to the human user.**

The agent must not claim a physical flow PASS simply because ADB, shell inspection, an instrumentation test, or automatic input succeeded.

After each device-dependent vertical slice the agent must:

1. install the updated debug APK;
2. stop development of the next major feature;
3. print `MANUAL TEST REQUIRED — <feature>`;
4. provide a short numbered checklist;
5. wait for the user's PASS/FAIL report;
6. fix FAIL results before moving on;
7. only then update device-dependent TODO items to `[x]`.

## 15. Product regression gate

Every architecture/refactor must preserve these user-visible invariants unless a human explicitly approves a product change:

- Full Storage can find APK/ZIP/PDF/document/image/video files;
- Large Files can display results progressively;
- user can select/open/delete files;
- Cleanup Receipt appears immediately after confirmed operation;
- Screenshot Cleaner remains usable;
- Home navigation works;
- no extra redundant permission request appears in Full Mode;
- no automatic heavy scan makes navigation feel frozen.

If a refactor breaks one of these, stop and fix/revert before adding features.

## 16. Development decision test

Before implementing a new technical idea ask:

1. Is this behavior proven useful by successful products or our own user testing?
2. Does it make Tooliva faster, simpler, safer, or more useful?
3. Is there a current measured problem it solves?
4. Can it be implemented without replacing a working path?
5. Can the user explain the benefit without knowing Android internals?

If the answers are weak, do not build it.

## 17. What “best in market” means for Tooliva

Tooliva does not win by having the most checkboxes.

It wins by combining:

- breadth of a serious Cleaner/File Manager;
- simplicity/trust similar to Files by Google;
- depth inspired by mature tools such as SD Maid;
- truthful device information;
- explainable cleanup candidates;
- excellent review/delete UX;
- verified Cleanup Receipts;
- local-first privacy;
- restrained advertising;
- no fake boost/cooling/virus theater.

## 18. Source-of-truth precedence

For product/engineering decisions, use this order:

1. `docs/PRODUCT_CONSTITUTION.md`
2. `docs/DECISION_LOG.md`
3. `AGENTS.md`
4. `TECH_SPEC.md`
5. `ARCHITECTURE.md`
6. `TODO.md`
7. `docs/PLAY_POLICY.md`
8. `docs/QA_PLAN.md`
9. `docs/MARKET_RESEARCH_2026.md`
10. older comments/commits/prototypes

When a lower-priority source conflicts with a higher-priority source, update the lower-priority source instead of inventing a compromise.