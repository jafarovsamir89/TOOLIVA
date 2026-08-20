# Tooliva — Decision Log

Status: AUTHORITATIVE  
Revision: 2026-08-19

This file records decisions that must survive context loss, agent changes, long pauses, and future refactors.

## D-001 — Product identity

**Decision:** Tooliva is primarily a Cleaner + File Manager, with Device Tools around it.

**Why:** Google Play leaders demonstrate strong demand for storage cleanup, file management, app/storage insight, and simple maintenance workflows. Tooliva must first be excellent at these jobs before expanding into many small tools.

**Consequence:** Cleaner/File Manager P0 work outranks Vault, App Lock, QR, PDF micro-tools, ads, and secondary utilities.

---

## D-002 — No fake optimizer behavior

**Decision:** Tooliva does not implement fake RAM boost, CPU cooling, fake antivirus alerts, invented health scores, fake reclaimable bytes, or scareware wording.

**Consequence:** Every cleanup result and device metric must be measurable or explicitly marked unavailable.

---

## D-003 — All Files Access direction

**Decision:** `MANAGE_EXTERNAL_STORAGE` is approved for development/prototype because file management, on-device search, and storage maintenance are genuine core functions.

**Production condition:** current Google Play restricted-permission review and declaration must be completed before release.

**Consequence:** Full Mode is the primary modern Android Cleaner/File Manager path when granted. Limited Mode remains a truthful fallback.

---

## D-004 — Permission UX on Android 11+

**Decision:** When Full Storage Access is granted, the core Cleaner must not immediately request separate photo/video permission for the same shared-storage job.

**Why:** This confused the real Xiaomi test and duplicates user consent for one product purpose.

**Consequence:** Screenshot Cleaner and other storage cleanup modules should use Full Mode when available. Granular media permission is reserved for Limited Mode or a genuinely separate media-specific capability that requires it.

---

## D-005 — Known-good baseline

**Decision:** commit `b767aa8` is the reference user-validated baseline before the index regression.

**Manual Xiaomi evidence:**
- synthetic APK/ZIP/PDF/DOC/PNG/MP4 discovered;
- selection and deletion worked;
- Cleanup Result timing fix worked;
- Screenshot Cleaner worked;
- Home navigation worked.

**Consequence:** future refactors must preserve these flows.

---

## D-006 — Storage Index experiment rejected as primary Cleaner architecture

**Affected commits:**
- `7836ea` — Room-backed Storage Index v1
- `71f35ca` — fast-first/non-blocking index revision

**Decision:** do not use mandatory/index-first architecture as the primary Cleaner data path.

**Observed regression:**
- long first scan;
- UI felt frozen/heavy;
- Large Files became unreliable;
- technical index concepts cluttered the Cleaner screen;
- automatic scans reduced user control;
- duplicated permission UX remained/confused users.

**Root technical mistake:** a persistent Room snapshot became a gateway between direct storage discovery and the Large Files UI.

**Consequence:** restore/directly preserve the progressive `StorageProvider -> StorageScanEvent -> UI` path. Room may later be used only for proven persistence/caching needs.

---

## D-007 — Do not optimize hypothetical scale before measuring real UX

**Decision:** no cache/index/worker/concurrency framework is added solely because future phones may have 50k/100k files.

**Required before optimization:**
1. measure a real user-visible problem;
2. reproduce it on the physical test phone or a realistic benchmark;
3. choose the smallest improvement;
4. verify the improvement does not regress the known-good flow.

---

## D-008 — Cleaner scan semantics

**Decision:** Cleaner Scan exists to find useful cleanup categories, not to build a perfect mirror of every file on the phone.

**Consequence:** one progressive traversal may feed lightweight classifiers. A file that does not matter to an active classifier does not need expensive processing or persistence.

No ordinary Cleaner scan should hash all files, read full contents, generate all thumbnails, or insert every file into a database without a demonstrated need.

---

## D-009 — No automatic heavy scan on navigation

**Decision:** opening Clean or Large Files must not automatically start expensive whole-storage work by default.

**Why:** real user test interpreted this as the phone hanging and loss of control.

**Consequence:** expensive work starts from an explicit user action unless a future measured UX study and explicit human decision approve otherwise.

---

## D-010 — Progressive results are a core UX requirement

**Decision:** when a user starts a scan, matching useful files should appear progressively where practical.

**Consequence:** Large Files must not wait for a complete full-device pass or a database-generation promotion before showing known matches.

---

## D-011 — Cleanup Receipt is a permanent differentiator

**Decision:** retain and expand the verified Cleanup Receipt model.

It must distinguish requested, missing, Trash, physically freed, failed, canceled, and permission-revoked outcomes.

**Consequence:** no future refactor may replace it with a generic `Cleaned X GB` toast.

---

## D-012 — Explainable Junk

**Decision:** no mystery aggregate `Junk = X GB` without drill-down.

**Consequence:** every cleanup group has a deterministic reason and user-review path. Ambiguous normal documents are not preselected.

---

## D-013 — File Manager is real core functionality

**Decision:** File Manager will be a real product module, not a placeholder to justify storage permission.

**P0 direction:** browse/search/sort/open/share/rename/copy/move/delete/create-folder/category views.

**Consequence:** Cleaner and File Manager share simple storage/file-operation primitives where practical, but neither is blocked on a mandatory whole-device index.

---

## D-014 — Cache cleanup V1

**Decision:** use the official system-mediated cache-clearing flow where supported.

**Consequence:** no claim of silent private-cache deletion from V1. The V2 analyzer may use Android-provided `StorageStats.cacheBytes` after Usage Access, while the V1 system action is moved to Phone Optimizer.

---

## D-018 — Selected-app cache automation experiment (superseded)

**Decision:** the human explicitly approved AccessibilityService for one narrow experiment: cleaning caches of apps selected by the user.

**Scope:** installed browsers discovered through browser intent resolution plus the explicitly visible YouTube package. The flow is user-started, local-only and deterministic: open App Info, navigate to Storage/cache and click only an exact `Clear cache` control.

**Hard exclusions:** App Lock, RAM killing, force-stop, ad clicking, background autonomous actions, arbitrary Settings control, browser/page inspection, OCR, screenshots, gestures, network and collection of accessibility text.

**Safety:** the service is disabled by default, requires prominent disclosure and affirmative consent, filters to expected Settings packages, has a finite timeout, and fails safely when a target or safe node cannot be confirmed. `isAccessibilityTool` remains false because Tooliva is not an accessibility tool for people with disabilities.

**Outcome:** the Xiaomi manual test showed that the automation did not reliably complete the intended flow. The experiment was removed from the current build and is not a product fallback.

---

## D-019 — Manual App Info cache cleanup

**Decision:** Cache Cleaner uses the reliable manual path. It measures browser/YouTube cache sizes, lets the user select apps, and opens each selected app's Android App Info page. The user presses Clear cache themselves.

**Why:** this is understandable, visible on the device and avoids unreliable OEM-specific Accessibility automation. Tooliva does not claim that the cache was cleared or report a reduction unless a future supported verification path is explicitly implemented.

**Consequence:** no AccessibilityService is declared for Cache Cleaner. Any future automation requires a new explicit human decision, policy review and Xiaomi validation.

---

## D-015 — App visibility

**Decision:** `QUERY_ALL_PACKAGES` is not pre-approved.

**Consequence:** prototype App Manager with narrower visibility first. Add broad package visibility only after a real missing core behavior is documented, current Play policy is rechecked, and explicit approval is recorded.

---

## D-016 — Manual phone testing ownership

**Decision:** the human user performs manual functional phone testing.

The coding agent:
- writes code/tests;
- runs automated checks;
- builds APK;
- installs the fresh debug APK;
- performs only crash/smoke confirmation;
- supplies a numbered manual test checklist;
- stops before the next major slice.

The agent must not self-certify device UX PASS through ADB automation.

---

## D-017 — Regression-first development

**Decision:** preserving known-good user flows is more important than completing architectural TODO items.

Before a major refactor, capture which working flows are at risk. After the refactor, the user must manually retest those flows before new modules begin.

---

## D-018 — Market references are behavioral, not source-code templates

**Decision:** CCleaner, AVG Cleaner, Files by Google, AI Cleaner, 1Tap Cleaner, SD Maid, Storage Analyzer and other competitors may inform product requirements and UX principles.

**Prohibited:** copying proprietary code, assets, strings, branding, or screen layouts.

Open-source projects may be studied under their licenses, but Tooliva should implement its own architecture and UI.

---

## D-019 — Architecture simplicity rule

**Decision:** choose the simplest architecture that supports the currently implemented product safely.

A new abstraction/database/service/worker must answer a concrete current need. `It may be useful later` is not enough.

---

## D-020 — Next recovery step after this documentation pass

**Decision:** before adding any new Cleaner feature, restore the user-validated direct-scan behavior by reverting/removing the two mandatory-index experiments while preserving unrelated good work.

Then fix the permission model so Full Mode does not redundantly request media access for the same Cleaner purpose.

Only after manual Xiaomi PASS should development continue to the next Cleaner categories.

---

## D-021 — Three explicit v1 tools after recovery

**Decision:** Notification History, Storage Map and Cleanup Swipe may be implemented as separate vertical slices in one development batch without changing the direct Cleaner architecture.

**Notification History:** Room is permitted only for the persistent notification-history feature. The listener stores normalized local rows, not raw Android objects, and excludes its database/preferences from backup.

**Storage Map:** direct, explicit, cancellable folder aggregation is the source of truth. It is not a prerequisite for Cleaner/File Manager and does not become a whole-device index.

**Cleanup Swipe:** the review session is in memory and destructive work is deferred until final confirmation. It reuses existing scanners, file operations and Cleanup Receipt.

**Consequence:** no new permissions, WorkManager, global storage index, background crawler, ad SDK, Vault, App Lock or unrelated module is introduced by this batch.
