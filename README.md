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
- Exact Duplicates
- Cleanup Swipe
- verified Cleanup Receipt

Exact Duplicates v1 is an explicit, local-only analysis: direct metadata traversal groups non-empty regular files by exact size, SHA-256 is calculated only for candidate groups, and hash matches receive streaming byte verification. Hashes are kept only in the current session; no Room index or background crawler is used. Review starts with an empty selection and offers Keep this copy, filters, search, sort, open/details/show-in-Files, and the existing verified cleanup receipt flow. Xiaomi end-to-end validation remains a human-owned TODO item.

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

**CACHE CLEANER v2 + PHONE OPTIMIZER / MEMORY v1**

1. Cache Cleaner measures installed browsers and YouTube only after Usage Access;
2. the user selects apps and opens Android App Info to clear cache manually;
3. Phone Optimizer owns the official system-mediated temporary-cache action and real memory readings;
4. device-dependent automation and Xiaomi regression remain manual-test gates.

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
- truthful Phone Doctor later
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
