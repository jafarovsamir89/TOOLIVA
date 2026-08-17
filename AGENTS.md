# Tooliva — Instructions for AI Coding Agents

This file is authoritative for Codex/Claude/Gemini or other coding agents working in this repository.

## Mission

Build a production-quality Android utility whose core strengths are:

1. deep storage cleaning;
2. file management / on-device file search;
3. trustworthy device maintenance;
4. retention utilities that remain useful after cleanup.

Optimize for:
- real user value;
- reliability;
- deletion safety;
- privacy;
- truthful behavior;
- fast UX;
- maintainable code;
- Google Play compliance;
- visual consistency with the approved Tooliva design reference.

The product strategy is based on the current-market research in `docs/MARKET_RESEARCH_2026.md`. Read it before changing cleaner/file-manager scope.

---

# Hard product rules

1. Kotlin only unless an external library requires otherwise.
2. Jetpack Compose for app UI.
3. Target Android API 36.
4. **Do not downgrade the Cleaner to gallery/media-only behavior.** Tooliva is intended to be a real Cleaner + File Manager.
5. `MANAGE_EXTERNAL_STORAGE` is explicitly approved for implementation/prototyping because file management/on-device file search/storage maintenance are core purposes. It still requires a truthful disclosure, fallback mode and Play declaration before production.
6. Preserve the existing MediaStore implementation as Limited Mode/fallback; do not delete working scoped-storage code merely because Full Storage Mode is added.
7. Never add `QUERY_ALL_PACKAGES` merely for convenience. Prototype the App Manager first; add broad visibility only if the product need is demonstrated and separately documented.
8. Never add AccessibilityService without explicit human approval. Market competitors using Accessibility does not automatically approve it for Tooliva.
9. Never add permissions `just in case`.
10. Never bypass Android protected/private storage using exploits, root, hidden APIs or deceptive flows in the normal Play build.
11. Never delete a user file without explicit user action/review/confirmation appropriate to the platform.
12. Never implement fake RAM boost, CPU cooling, fake antivirus warnings or invented health scores.
13. Never upload scanned filenames, file contents, notification text, Vault content or installed-app inventory to a server/analytics provider.
14. Never log filenames, notification content, secrets or Vault contents in production logs.
15. Never add an ad SDK directly inside a feature package.
16. Never show ads on permission explanations, delete/trash confirmation, Cleanup Result, Vault or biometric/PIN screens.
17. Do not create a backend unless a task explicitly requires one.
18. Core storage/file utilities must work offline.
19. Do not redesign Tooliva from scratch. Use `docs/design/` as visual source of truth unless product requirements now require an additional screen/component.
20. Do not mark placeholders/mock values/fake scan results as completed functionality.
21. Do not copy proprietary source code, assets, strings or UI artwork from competitors. Market research defines requirements, not implementation copying.

---

# Product source of truth

Before major work, read:

- `README.md`
- `TECH_SPEC.md`
- `TODO.md`
- `docs/MARKET_RESEARCH_2026.md`
- `docs/PLAY_POLICY.md`
- `docs/PRIVACY_SECURITY.md`
- `docs/FEATURE_MATRIX.md`
- `docs/design/README.md`

For UI work also inspect:
- `docs/design/tooliva-ui-showcase.webp`
- `docs/design/tooliva-ui-system.webp`

If old comments/code conflict with the rewritten `TECH_SPEC.md` and `TODO.md`, follow the newer market-driven specification, while preserving already working code where it can become a fallback/component.

---

# Current engineering priority

Until the Cleaner/File Manager Beta gate is reached, priority is:

1. Full Storage Mode + Limited fallback
2. storage abstraction/index
3. deep cleaner categories
4. real file manager/search
5. storage map
6. exact duplicates/screenshots/cleanup swipe
7. app manager/cache workflow

Do not jump to Vault, App Lock, PDF micro-tools or monetization while these P0 items remain open unless a human explicitly reprioritizes.

---

# Full Storage Mode rules

`MANAGE_EXTERNAL_STORAGE` is approved for the current prototype/product direction.

Implementation must include:
- `Environment.isExternalStorageManager()` state check;
- clear pre-permission explanation;
- correct Special App Access intent/settings flow;
- grant, deny and revoke handling;
- visible `Full Storage Mode` vs `Limited Mode` state where relevant;
- no crash or dead-end when access is denied;
- tests/manual validation notes;
- no attempt to enter protected directories Android still disallows.

Do not pretend the permission has been granted merely because it exists in the manifest.

---

# Storage architecture

Cleaner UI must not depend directly on `MediaStore.Images`/`MediaStore.Video` as the sole storage source.

Prefer a domain abstraction such as:
- `StorageProvider`
- `StorageEntry`
- `StorageScanRequest`
- `StorageAccessMode`

Expected providers:
- Full Storage provider
- MediaStore Limited provider
- SAF provider when user-mediated access is appropriate

Preserve the existing central cleanup/delete coordinator.

Storage scanning requirements:
- never scan on main thread;
- progressive results;
- cancellation;
- bounded concurrency;
- handle disappearing/mutating files;
- exclude Tooliva temp/internal data;
- cache/index expensive data when appropriate;
- test large file counts;
- never call an ambiguous normal user file `junk` without an explainable rule.

---

# Cleaner classification rules

Every cleanup candidate must have a reason.

Examples of acceptable categories:
- large file;
- old screenshot;
- exact duplicate;
- old APK installer;
- old download;
- accessible temp/residual candidate from an explicit rule;
- empty accessible folder;
- user-selected old file.

Rules:
- ambiguous items are not preselected;
- normal documents are not automatically `junk`;
- age alone does not make a document/APK safe to delete;
- `unused` requires real usage evidence;
- duplicate means verified exact match unless UI explicitly says `similar`.

---

# Destructive operations

The existing verified Cleanup Receipt is a key product feature.

Required flow:
1. scan/index;
2. user review/select;
3. show count + bytes;
4. platform/system confirmation when required;
5. execute;
6. re-query/re-scan;
7. verify;
8. show Cleanup Receipt.

Receipt must distinguish:
- requested;
- already missing;
- moved to Trash;
- physically freed;
- unchanged/failed;
- canceled;
- permission revoked.

Never count Android Trash bytes as physically freed.

No advertisement may interrupt this flow.

---

# File Manager rules

File Manager is core functionality.

P0 capabilities:
- browse shared storage;
- volume handling;
- breadcrumbs/path;
- search;
- type/size/date filters;
- sort;
- open/share/details;
- rename;
- copy;
- move;
- create folder;
- delete/trash;
- collision handling;
- long-operation progress/cancel.

File actions must share domain/storage abstractions with Cleaner where practical instead of implementing duplicate filesystem logic.

---

# App Manager / package visibility

Do not add `QUERY_ALL_PACKAGES` before testing the narrower implementation.

If broader visibility is necessary:
1. document the exact user-facing feature that fails without it;
2. verify the current Google Play policy;
3. update `docs/PLAY_POLICY.md` and `TECH_SPEC.md` if needed;
4. obtain explicit human approval;
5. add the permission;
6. add disclosure/declaration notes;
7. ensure installed-app inventory is never used for ads/analytics.

`PACKAGE_USAGE_STATS` requires explicit Usage Access by the user and is only for features that genuinely use it.

---

# Cache cleaning

For Android 11+ use official system-mediated `StorageManager.ACTION_CLEAR_APP_CACHE` where supported and appropriate.

Do not claim Tooliva can directly silently wipe every other app's private internal cache.

`CLEAR_APP_CACHE` is not a normal third-party permission; do not attempt privilege tricks.

Accessibility automation of app Settings remains unapproved for V1.

---

# Visual source of truth

The approved visual language:
- dark graphite surfaces;
- teal/cyan primary actions;
- restrained blue/green/orange secondary accents;
- large rounded Material 3 cards;
- strong hierarchy and readable numeric storage data;
- 4dp spacing grid;
- minimum 48dp touch targets;
- dark theme first, light theme supported;
- no fake danger colors/manipulative warnings.

Mockup values are examples only. Production UI uses real local data.

New cleaner/file-manager screens should extend this design system, not invent a second style.

---

# Before coding a task

1. Read the relevant source-of-truth docs.
2. Inspect existing code before proposing replacement.
3. Identify exact TODO items.
4. State the smallest coherent vertical slice.
5. Implement.
6. Add/update tests.
7. Run build/tests.
8. Validate on a physical device when the feature depends on OEM/storage behavior.
9. Inspect UI against design reference.
10. Update TODO honestly using `[x]`, `[~]`, `[ ]` semantics.
11. Commit and push focused changes.
12. Report changed files, tests, physical-device coverage and remaining blockers.

Do not mark a feature complete because the UI compiles.

---

# Architecture

Prefer:
- package-by-feature;
- unidirectional data flow;
- immutable UI state;
- repository/platform abstractions;
- coroutines/Flow;
- centralized storage access;
- centralized permission/special-access handling;
- centralized destructive-operation handling;
- centralized ads/billing.

Avoid:
- god classes;
- global mutable state;
- business logic in Composables;
- direct database access from UI;
- separate ad SDK calls inside features;
- duplicate file-operation implementations in Cleaner and File Manager.

Do not create abstraction layers that have no concrete purpose.

---

# Testing minimum

For each relevant feature verify:
- happy path;
- permission denied;
- permission revoked after grant;
- empty data;
- large data set;
- file disappears/changes during operation;
- process recreation where meaningful;
- Android version differences;
- no main-thread blocking;
- no unexpected network use.

For storage/destructive functionality also verify:
- APK;
- ZIP/archive;
- PDF/document;
- image;
- video;
- nested folders;
- duplicate names;
- copy/move collisions;
- read-only/inaccessible path behavior;
- low-storage state;
- Trash vs physical deletion accounting.

---

# Definition of done

A task is done only when:
- it works end-to-end for its stated scope;
- loading/error/empty/access-denied states exist;
- tests are added where appropriate;
- build passes;
- physical-device validation is performed when required;
- no policy/privacy issue was silently introduced;
- UI follows `docs/design/`;
- no mock/fake data remains in production path;
- docs are updated if behavior/scope changed;
- TODO reflects verified reality.
