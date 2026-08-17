# Tooliva

**Tooliva — Cleaner, File Manager & Device Tools**

> A serious Android storage cleaner and file manager first, with diagnostics, privacy and everyday utilities around it.

## Product direction

Tooliva is no longer defined as a shallow `all-in-one app with many icons`.

Its core jobs are:

1. **Understand storage** — what is actually taking space?
2. **Clean safely** — what can the user review and remove?
3. **Manage files** — browse, search, sort, move, copy, rename and delete shared-storage files.
4. **Maintain the phone** — app/storage/device diagnostics.
5. **Stay useful after cleanup** — Notification History, image/PDF/QR/network tools, and later Vault/App Lock.

The market basis for this direction is documented in:

- `docs/MARKET_RESEARCH_2026.md`

## Product promise

**Find what is taking your storage. Clean it safely. Manage your phone with one trusted toolbox.**

Tooliva must never use:
- fake RAM boost;
- fake CPU cooling;
- fake antivirus warnings;
- invented battery-health scores;
- unexplained fake `junk` totals;
- silent deletion.

## Core V1 product

### CLEAN
- Deep Storage Scan
- Explainable junk candidates
- Large Files across accessible shared storage
- Downloads
- APK installers
- Archives
- Documents
- Images / Videos / Audio
- Old Files
- Screenshot Cleaner
- Exact duplicate files
- Cleanup Swipe
- Empty folders
- system-mediated Cache Cleanup
- verified Cleanup Receipt

### FILES
- Internal/shared storage browser
- SD/USB where available
- Downloads/Documents/APKs/Archives/Images/Videos/Audio shortcuts
- global search
- sort/filter by name/size/date/type
- open/share/details
- rename
- copy/move
- delete/trash
- create folder
- Storage Map
- ZIP/unZIP later in the same file subsystem

### DIAGNOSE / APPS
- App Manager
- large/unused app review
- Phone Doctor
- battery/thermal/device facts
- sensor inventory
- hardware tests

### PROTECT
- Notification History — high priority after cleaner core
- Private Vault — later
- App Lock — only after reliability/policy validation

### TOOLS
- Image Compress / Resize / Convert
- EXIF Privacy Clean
- Images → PDF
- QR / barcode
- Network tools
- Compass / Level / Flashlight
- Magnifier and small tools later

## Storage access model

Tooliva supports two modes.

### Full Storage Mode

Uses Android All Files Access (`MANAGE_EXTERNAL_STORAGE`) for the product's core file-management, on-device-search and storage-maintenance experience.

Production use is subject to Google Play restricted-permission declaration and approval.

### Limited Mode

Uses MediaStore and Storage Access Framework when Full Storage Mode is not granted.

Limited Mode remains useful, but the UI must never pretend it scanned every shared-storage file.

The existing MediaStore work remains valuable fallback infrastructure.

## Why Tooliva can win

Research of current Google Play leaders shows a split:

- serious cleaners/storage analyzers can be powerful but complex or ad-heavy;
- Files by Google is trusted and simple but not trying to be a deep system toolbox;
- device-info apps are technically strong but do not solve cleanup;
- AppLock/notification-history apps solve one recurring problem each;
- giant all-in-one toolboxes often become walls of shallow features.

Tooliva's target is:

**deep cleaner + real file manager + trustworthy UX + strong recurring utilities.**

Key differentiators:
- Verified Cleanup Receipt
- Trash vs Physically Freed accounting
- Storage Map
- explainable cleanup candidates
- Full/Limited access modes
- one scan → action plan
- local-first privacy
- respectful monetization

## Design

Visual source of truth:
- `docs/design/tooliva-ui-showcase.webp`
- `docs/design/tooliva-ui-system.webp`
- `docs/design/README.md`

The new cleaner/file-manager screens extend the same dark graphite + teal Material 3 language.

## Technology

- Kotlin
- Jetpack Compose
- Material 3
- Coroutines / Flow
- Room
- DataStore
- WorkManager only where justified
- MediaStore
- Storage Access Framework
- full shared-storage provider for Full Storage Mode
- UsageStatsManager
- NotificationListenerService
- SensorManager
- ConnectivityManager
- Android Keystore
- AndroidX Biometric
- CameraX where needed

### Android targets

- `compileSdk`: 36
- `targetSdk`: 36
- `minSdk`: 26

## Backend

No backend is required for core Tooliva functionality.

Storage scans, fingerprints, Notification History and Vault data remain local.

## Monetization

### Free
- real useful cleaner/file manager
- restrained ads only on non-sensitive screens

### Pro Lifetime
Initial hypothesis remains a one-time purchase instead of a costly weekly subscription.

Possible Pro value:
- remove ads
- advanced filters
- similar-photo tools
- advanced Storage Map/history
- additional premium local utilities

Never block a requested cleanup result behind an ad.

## Repository docs

- `TECH_SPEC.md` — product/engineering specification
- `TODO.md` — current implementation roadmap
- `AGENTS.md` — mandatory AI-agent rules
- `docs/MARKET_RESEARCH_2026.md` — competitive analysis and product conclusions
- `docs/PLAY_POLICY.md` — restricted-permission / Play strategy
- `docs/FEATURE_MATRIX.md` — feature/access priorities
- `docs/PRIVACY_SECURITY.md` — privacy/security model
- `docs/QA_PLAN.md` — QA strategy
- `docs/design/` — visual references

## Current status

Already implemented in `agent/android-bootstrap`:
- Android/Compose foundation
- Home baseline
- MediaStore scanner foundation
- Large Files media fallback flow
- Screenshot Cleaner flow
- centralized Trash/delete architecture
- verified Cleanup Result model
- Xiaomi physical-device baseline

Current highest-priority milestone:

**Full Storage Mode → StorageProvider/index → Deep Cleaner categories → real File Manager → Storage Map.**
