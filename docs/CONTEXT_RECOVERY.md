# Tooliva — Context Recovery Guide

Revision: 2026-08-19

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

Do not infer product direction from code alone; the known Storage Index experiment is rejected and the direct Cleaner architecture is authoritative.

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

## 7. Current branch state

The direct Cleaner architecture is restored. Room is now present only because Notification History is an active persistent feature; it is not a gateway for Large Files, Storage Map or ordinary Cleaner discovery.

The current development batch adds Notification History v1, Storage Map v1 and Cleanup Swipe v1. All three require human Xiaomi validation before their device-dependent TODO items become `[x]`.

**Do not treat rejected index code as desired architecture just because it is newer.**

## 8. Current immediate task

`NOTIFICATION HISTORY v1 + STORAGE MAP v1 + CLEANUP SWIPE v1`

Implementation, tests and documentation are complete for the current batch. The remaining gate is the combined human Xiaomi checklist in `docs/QA_PLAN.md`; do not mark device-dependent items complete until the user reports PASS.

## 8.1. Current three-feature batch

- Notification History: explicit disclosure/Notification Access, local Room rows, dedupe by active notification key, retention, filters, exclusion and backup exclusion.
- Storage Map: explicit direct aggregation, progress/cancel, map/list, drill-down and file-operation integration; no auto-scan.
- Cleanup Swipe: explicit category loading, Keep/Delete/Skip, undo, final review and central Cleanup Receipt; no immediate card deletion.

Required next step is one combined Xiaomi manual checklist covering permissions, notifications, Storage Map navigation and Cleanup Swipe review/deletion. Do not start Vault, App Lock, similar photos, permission manager, network tools, ads or billing before that validation.

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

## 10. What comes after this batch

Do not start another module until the three-feature human validation is complete. After that gate, the next priority is determined by the authoritative TODO and a new explicit task; Vault, App Lock, Similar Photos, Permission Manager, network tools, ads and Billing remain out of scope.

The current approved work slice includes App Manager v1, Cache Cleaner v2 + Phone Optimizer, File Manager v1, Exact Duplicates v1 with its measured fingerprint cache, and the Phone Doctor + Hardware Tests + Check My Phone v1 vertical slice. App Manager uses narrow package visibility and optional Usage Access; Xiaomi package-gap measurement and physical App Manager validation remain human-owned gates. Human Xiaomi validation has passed for the duplicate flow and the repeat-analysis fingerprint cache. Phone Doctor and physical hardware behavior remain human-owned manual gates. Do not restore the rejected Storage Index or Accessibility automation. The duplicate cache is not a Room index or background crawler; Check My Phone is not a background analyzer.

## 11. Product invariant

Do not make Tooliva worse in order to make its architecture “better.”

Every major change must answer:

- what real measured problem does this solve?
- is there a simpler solution?
- does it preserve the known-good user flow?
- did the human Xiaomi test improve or regress?

If a refactor regresses the product, fix/revert before adding features.
