# Tooliva — Context Recovery Guide

Revision: 2026-08-18

Use this file when a new chat/agent starts, context is lost, or project direction feels ambiguous.

## 1. What is Tooliva?

**Tooliva — Cleaner, File Manager & Device Tools**

Primary goal: become an excellent Android Cleaner + real File Manager before expanding into many secondary utilities.

## 2. Read these files first

In this order:

1. `docs/PRODUCT_CONSTITUTION.md`
2. `docs/DECISION_LOG.md`
3. `AGENTS.md`
4. `docs/CLEANER_BLUEPRINT.md`
5. `TECH_SPEC.md`
6. `ARCHITECTURE.md`
7. `TODO.md`
8. `docs/PLAY_POLICY.md`
9. `docs/QA_PLAN.md`
10. `docs/MARKET_RESEARCH_2026.md`
11. `docs/design/README.md` + both design WebP files for UI work

Do not infer product direction from code alone because the current branch contains a known rejected Storage Index experiment that has not yet been recovered.

## 3. Known-good product point

Reference commit:

`b767aa8` — `Fix cleanup result and home navigation`

Human Xiaomi validation confirmed around this point:

- Full Storage Access works;
- Large Files found APK/ZIP/PDF/DOC/PNG/MP4 synthetic fixtures;
- filters/selection/open worked;
- delete worked;
- Cleanup Result worked;
- Screenshot Cleaner worked;
- Home navigation worked.

## 4. Known bad/rejected experiment

Commits:

- `7836ea` — mandatory Room Storage Index
- `71f35ca` — fast-first/deep-index follow-up

Human Xiaomi result:

- scan felt extremely slow/heavy;
- Large Files regressed;
- technical clutter appeared on Clean screen;
- auto-scans felt like the phone was hanging;
- permission UX became confusing;
- product was worse than the known-good baseline.

Decision: mandatory index-first Cleaner architecture is rejected.

## 5. Correct architecture now

Primary Cleaner path:

```text
explicit user action
 -> StorageProvider
 -> progressive cancellable direct scan
 -> lightweight classifiers
 -> progressive UI results
 -> review/select
 -> central safe delete/trash
 -> verified Cleanup Receipt
```

Room is not the gateway for ordinary Cleaner discovery.

Room is allowed later only for real persistent data such as duplicate fingerprints or Notification History.

## 6. Permission model

Android 11+ Full Mode:

`MANAGE_EXTERNAL_STORAGE` is the preferred primary shared-storage access for Cleaner + File Manager when granted.

If Full Mode is granted, do not immediately ask for redundant Photos/Videos permission for the same Cleaner job.

Limited Mode:

MediaStore / granular media permission / SAF only when Full Mode is absent and the specific feature needs it.

## 7. Current branch state warning

The branch `agent/android-bootstrap` currently contains the rejected index implementation in code.

The authoritative documents have already been rewritten to describe the recovery direction.

**Do not treat current index code as desired architecture just because it is the newest code.**

## 8. Current immediate task

`RECOVERY v1`

1. revert/remove mandatory index changes from `7836ea` and `71f35ca` while preserving unrelated good documentation/work;
2. restore direct progressive Large Files scanner based on `b767aa8` behavior;
3. remove Storage Index UI from Clean;
4. remove automatic heavy scan on navigation;
5. preserve FullStorageProvider, StorageProvider abstraction, cleanup coordinator, Cleanup Receipt, filters/search/sort/select/open/delete;
6. remove dead Room/KSP/index code/dependencies if nothing active requires them after recovery;
7. unify storage permission UX so Full Mode does not redundantly request broad media permission;
8. automated tests/build;
9. install fresh debug APK on Xiaomi;
10. stop and give human manual checklist;
11. proceed only after human PASS.

## 9. Manual testing ownership

Coding agent does not self-certify phone UX.

Agent:
- code/tests/build;
- install fresh debug APK;
- crash smoke-check;
- give numbered `MANUAL TEST REQUIRED` checklist;
- stop.

Human user:
- runs functional tests;
- reports PASS/FAIL;
- device-dependent TODO `[x]` only after human PASS.

## 10. What comes after recovery

After Recovery + Permission UX manual PASS:

1. Downloads
2. APK installers
3. Archives
4. Documents
5. Old Files
6. Explainable Junk rules
7. Cache Cleaner v2 manual App Info flow + Phone Optimizer
8. File Manager
9. Exact Duplicates
10. Cleanup Swipe
11. App Manager
12. Storage Map
13. Phone Doctor / Check My Phone
14. Notification History
15. later Vault/App Lock/content tools/monetization

The current approved work slice includes App Manager v1, Cache Cleaner v2 + Phone Optimizer, File Manager v1, Exact Duplicates v1 with its measured fingerprint cache, and the Phone Doctor + Hardware Tests + Check My Phone v1 vertical slice. App Manager uses narrow package visibility and optional Usage Access; Xiaomi package-gap measurement and physical App Manager validation remain human-owned gates. Human Xiaomi validation has passed for the duplicate flow and the repeat-analysis fingerprint cache. Phone Doctor and physical hardware behavior remain human-owned manual gates. Do not restore the rejected Storage Index or Accessibility automation. The duplicate cache is not a Room index or background crawler; Check My Phone is not a background analyzer.

## 11. Product invariant

Do not make Tooliva worse in order to make its architecture “better.”

Every major change must answer:

- what real measured problem does this solve?
- is there a simpler solution?
- does it preserve the known-good user flow?
- did the human Xiaomi test improve or regress?

If a refactor regresses the product, fix/revert before adding features.
