# Tooliva

**Tooliva — Cleaner, File Manager & Device Tools**

> Find what is taking space. Review it quickly. Remove it safely. Understand exactly what happened.

## Source of truth

Before changing product direction or storage architecture, read:

1. `docs/PRODUCT_CONSTITUTION.md`
2. `docs/DECISION_LOG.md`
3. `AGENTS.md`
4. `TECH_SPEC.md`
5. `ARCHITECTURE.md`
6. `TODO.md`
7. `docs/PLAY_POLICY.md`
8. `docs/QA_PLAN.md`
9. `docs/MARKET_RESEARCH_2026.md`

The Constitution has precedence when older code/comments/docs disagree.

## Product direction

Tooliva is not a shallow all-in-one toolbox and not an architecture experiment.

Its core jobs are:

1. **Clean storage** — useful, progressive, explainable cleanup results.
2. **Manage files** — real File Manager/search/actions.
3. **Maintain the phone** — apps and trustworthy diagnostics.
4. **Stay useful after cleanup** — Notification History and later selected tools.

## Product principles

- real Cleaner, not gallery-only in Full Mode;
- one understandable storage-access model;
- no fake RAM boost/CPU cooling/virus theater;
- no mysterious junk totals;
- no silent deletion;
- progressive real results;
- explicit user control over heavy scan actions;
- measured optimization only;
- local-first privacy;
- Cleanup Receipt after destructive operations;
- no ad spam in sensitive/core flows.

## Cleaner architecture

Primary Cleaner path:

```text
User action
 -> StorageProvider
 -> progressive cancellable scan
 -> lightweight classifiers
 -> live UI results
 -> review/select
 -> safe operation
 -> verified Cleanup Receipt
```

A mandatory whole-device Room index is **not** the primary Cleaner architecture.

See `docs/DECISION_LOG.md` for the rejected Storage Index experiment.

## Storage access

### Full Mode — Android 11+

When `MANAGE_EXTERNAL_STORAGE` is granted, it is the primary shared-storage Cleaner/File Manager path.

It powers, where Android permits:

- Large Files;
- Downloads;
- APKs;
- Archives;
- Documents;
- media/screenshot cleanup;
- File Manager/search.

A Full Mode user should not immediately be asked for a redundant Photos/Videos permission for the same Cleaner purpose.

### Limited Mode

When Full Mode is denied/unavailable:

- MediaStore;
- granular media permission when genuinely needed;
- SAF where user-mediated selection is appropriate.

Limited Mode never pretends to cover the whole phone.

## Cleaner V1 target

- Storage overview
- Large Files
- Downloads
- APK installers
- Archives
- Documents
- Old Files
- Screenshot Cleaner
- Explainable junk candidates
- official system-mediated Cache Cleanup
- Cache Cleaner for installed browsers and YouTube using Android-provided cache statistics
- manual App Info fallback for each selected app; the user controls Clear cache in Android
- Phone Optimizer with real memory metrics and the system-mediated temporary-cache action
- Phone Doctor v1 with factual device, memory, storage, battery, thermal, display and sensor data
- Hardware Tests v1 with user-confirmed display, touch, vibration, flashlight, speaker, microphone and sensor checks
- Check My Phone v1 as a lightweight factual dashboard with explicit review links and no health score
- App Manager v1 with a fast visible-app inventory, search/filter/sort, honest Android storage/usage enrichment, app details, Open/App info and system-mediated uninstall
- Exact Duplicates
- Cleanup Swipe
- Notification History with explicit Notification Access, local retention and app exclusions
- Storage Map with explicit folder aggregation and map/list views
- verified Cleanup Receipt

Exact Duplicates v1 is an explicit, local-only analysis: direct metadata traversal groups non-empty regular files by exact size, SHA-256 is calculated only for candidate groups, and hash matches receive streaming byte verification. A bounded private fingerprint cache reuses a hash only when the same path, size and modified time are unchanged; it stores no file contents and is not a whole-device index. Review starts with an empty selection and offers Keep this copy, filters, search, sort, open/details/show-in-Files, and the existing verified cleanup receipt flow. Xiaomi end-to-end validation remains a human-owned TODO item.

Notification History is opt-in. A prominent disclosure precedes Android Notification Access; captured fields are normalized to local Room rows, raw notification bundles/images/PendingIntents are not stored, Tooliva's own notifications are excluded, and ongoing notifications are excluded by default. Users can search/filter, pin, delete, pause, set retention, exclude apps and clear history. The notification database and preferences are excluded from backup.

Storage Map and Cleanup Swipe are explicit user-started tools. Storage Map aggregates folder totals directly from `FullStorageProvider` without a whole-device Room index, content reads, hashes or thumbnails. Cleanup Swipe keeps its review session in memory, supports Keep/Delete/Skip, undo and final review, and uses the existing central file operation and Cleanup Receipt only after explicit confirmation. Neither tool auto-deletes or auto-scans on navigation.

## File Manager V1 target

- browse accessible shared storage/volumes
- category shortcuts
- search
- sort by name/size/date
- details/open/share

Current implementation status: the first direct-browser vertical slice is wired from Home → Files. It reads real accessible volumes and direct children without a whole-device index, supports explicit category/search actions, selection, details, rename/create-folder, copy/move/delete operations, collision choices and cancellable progress. The APK/build and unit tests are verified; Xiaomi device end-to-end validation remains a human-owned TODO item.
- rename
- create folder
- copy/move
- delete/trash
- collision handling
- progress/cancel

File Manager is real core functionality, not a placeholder for permissions.

## Known-good baseline

Reference baseline before the index regression:

`b767aa8` — `Fix cleanup result and home navigation`

Manual Xiaomi validation around this baseline confirmed:

- APK/ZIP/PDF/DOC/PNG/MP4 discovery in Full Mode;
- selection/open/delete;
- Cleanup Result;
- Screenshot Cleaner;
- Home navigation.

## Rejected Storage Index experiment

Rejected as primary Cleaner architecture:

- `7836ea` — Room Storage Index v1
- `71f35ca` — fast-first/deep-index attempt

Real Xiaomi testing showed worse UX: long/heavy first scan, broken/unreliable Large Files behavior, technical clutter, hidden auto-scan, and confusing permission flow.

Do not add more layers to rescue that architecture. Recover the direct progressive scan path first.

## Current milestone

**Tooliva UI/UX 2.0 — Product Shell and Design System**

Cleaner 2.0 is human-confirmed responsive on Xiaomi after `6d5fccc`. This slice adds a dark-first Tooliva design system, responsive Home dashboard, real Home/Clean/Files/Tools/More navigation, a real Tools hub, appearance persistence and Settings access-status surfaces. The audit hardening pass also throttles scan/search UI publication for large libraries, aligns Action Plan categories with their review screens, and routes File Manager folder deletion through the shared Cleanup Result. Stable Cleaner/File Manager/device logic remains behind the presentation layer; Vault, App Lock, QR, PDF tools, ads, billing and fake Pro features are not exposed.

See `TODO.md` for exact tasks.

## Testing ownership

Coding agent:

- writes code/tests;
- runs automated checks;
- builds debug APK;
- installs APK on connected Xiaomi;
- performs crash smoke-check;
- gives numbered manual checklist.

Human user:

- performs functional phone testing;
- reports PASS/FAIL;
- is the authority for device-dependent UX completion.

The agent must stop before the next major slice until human PASS.

## Design

Visual source of truth:

- `docs/design/tooliva-ui-showcase.webp`
- `docs/design/tooliva-ui-system.webp`
- `docs/design/README.md`

Direction:

- dark graphite;
- teal/cyan primary actions;
- Material 3;
- rounded cards;
- clean hierarchy;
- no fake red danger theater;
- technical backend concepts stay out of consumer UI.

## Technology

- Kotlin
- Jetpack Compose
- Material 3
- Coroutines / Flow
- MediaStore
- Storage Access Framework
- Full shared-storage provider for Full Mode
- Room only for features with demonstrated persistence needs
- DataStore/WorkManager/Hilt only when justified by active features
- AndroidX/platform APIs preferred

Targets:

- compileSdk 36
- targetSdk 36
- minSdk 26

## Backend

No backend required for core Tooliva functionality.

User file inventory, fingerprints, notification history and Vault content remain local unless a future explicit user-controlled transfer feature is designed.

## Competitive strategy

Market references include:

- CCleaner
- AVG Cleaner
- Files by Google
- Phone Cleaner / AI Cleaner
- 1Tap Cleaner
- SD Maid 2/SE
- Storage Analyzer
- established File Managers

Tooliva should combine market-proven breadth with calmer UX, explainable cleanup and verified results.

Do not copy proprietary code/assets/layouts.

## Differentiators

- Cleanup Receipt
- Trash vs Physically Freed accounting
- Explainable Junk
- real Full Mode Cleaner beyond gallery
- real File Manager
- local-first privacy
- truthful Phone Doctor, Hardware Tests and Check My Phone
- restrained monetization

## Repository documents

- `docs/PRODUCT_CONSTITUTION.md` — highest-level permanent rules
- `docs/DECISION_LOG.md` — decisions/rejected experiments
- `AGENTS.md` — mandatory coding-agent operating rules
- `TECH_SPEC.md` — product/technical specification
- `ARCHITECTURE.md` — current architecture
- `TODO.md` — active roadmap
- `docs/MARKET_RESEARCH_2026.md` — competitive research
- `docs/PLAY_POLICY.md` — Android/Google Play permission strategy
- `docs/QA_PLAN.md` — testing strategy
- `docs/PRIVACY_SECURITY.md` — privacy/security
- `docs/FEATURE_MATRIX.md` — feature/access matrix
- `docs/design/` — visual source of truth
