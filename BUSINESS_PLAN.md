# Tooliva — Business Plan

Market-driven revision: 2026-08-16

## Executive summary

Tooliva is an Android **Cleaner + File Manager + Device Tools** product.

The acquisition engine is storage pressure: users install Tooliva because they need to understand and free space. The retention engine is broader device utility: file management, app/device diagnostics, Notification History and practical local tools.

Business model:
1. acquire users through high-intent Google Play searches such as cleaner, large files, file manager, storage analyzer and duplicate files;
2. give free users a genuinely useful cleaner/file manager;
3. monetize with restrained advertising that never interrupts destructive/storage-result flows;
4. offer a one-time Pro upgrade rather than relying on a weekly-subscription trap;
5. keep infrastructure cost near zero through local processing.

## Market thesis

Current Google Play leaders prove demand for:
- deep storage cleaners;
- file managers/search;
- large-file analysis;
- APK/archive/document discovery;
- duplicate/screenshot cleanup;
- app management;
- storage visualization;
- cache-maintenance workflows;
- device diagnostics.

The category also has persistent trust problems: excessive ads, scare creatives, confusing subscriptions and fears of wrong-file deletion.

Tooliva's opportunity is **depth + trust**.

Competitive research is documented in `docs/MARKET_RESEARCH_2026.md`.

## Primary customer

Android users who:
- are running out of storage;
- cannot understand what occupies tens of gigabytes;
- have old Downloads/APKs/archives/documents/media;
- need to find large files/folders quickly;
- want a better file browser/search experience;
- want duplicates/screenshots cleaned safely;
- want one trusted utility instead of many ad-heavy apps.

Secondary users:
- power users;
- technicians;
- used-phone buyers;
- users with low/mid-range storage devices;
- users who later value Notification History, diagnostics and image/PDF/QR tools.

## Main promise

**Find what takes your storage. Clean it safely. Manage your Android from one trusted toolbox.**

Trust promises:
- no fake boost;
- no fake cooling;
- no fake virus panic;
- no unexplained junk number;
- no silent deletion;
- no pretending Trash bytes are physically freed;
- no upload of file inventory/content for cloud analysis.

## Competitive wedge

Tooliva should combine:
- deep storage understanding comparable to serious cleaners/analyzers;
- file-management/search capability that justifies broad-storage access;
- a Files-by-Google-like review-before-delete trust model;
- a polished mainstream UI instead of power-user complexity;
- verified Cleanup Receipts;
- strong recurring utility modules.

Required differentiators:
1. Verified Cleanup Receipt
2. Trash vs Physically Freed accounting
3. Storage Map
4. Explainable Junk
5. Full Storage Mode + Limited fallback
6. One Scan → Action Plan
7. local-first privacy
8. respectful monetization

## Product hierarchy

### Acquisition / hero features
- Deep Storage Scan
- Large Files
- Downloads/APKs/Archives/Documents
- File Manager / Search
- Storage Map
- Exact Duplicates
- Screenshot Cleaner

### Retention features
- App Manager
- Phone Doctor / Check My Phone
- Notification History
- image/EXIF/PDF tools
- QR/network/sensor tools

### Later high-risk retention
- Vault
- App Lock

## Monetization

### Free
Must remain useful enough to earn ratings and retention:
- full core scan/review capability;
- file browser/search;
- basic cleanup categories;
- basic utilities.

Advertising rules:
- no startup interstitial before useful content;
- no ad before scan result;
- no ad between file selection and Cleanup Receipt;
- no ads on permissions/destructive confirmation;
- no fake system/virus ad placements;
- frequency caps;
- paid/no-ads state actually removes ads.

### Pro Lifetime
Initial strategy: one-time purchase with regional pricing tests.

Possible Pro value:
- remove ads;
- advanced filters/sorting;
- advanced Storage Map/history;
- similar-photo tools;
- bulk/advanced file operations where appropriate;
- extra customization/premium local tools.

Do not cripple basic cleaning so badly that the free app becomes an advertisement for Pro.

A subscription should be introduced only if a future feature has genuine ongoing service/cost/value.

## Cost structure

Core V1 backend cost: approximately zero by design.

Potential costs:
- Play developer/account fees;
- domain/privacy hosting;
- analytics/crash tooling depending tier;
- ad consent/mediation tooling;
- test devices;
- creative production/localization.

No per-scan AI/cloud inference cost is required for core cleaning.

## Acquisition / ASO clusters

Primary:
- phone cleaner
- storage cleaner
- file manager
- file explorer
- storage analyzer
- disk usage
- large files
- find large files
- duplicate files
- duplicate photos
- screenshot cleaner
- cache cleaner
- APK cleaner
- downloads cleaner

Secondary:
- device info
- phone test
- battery temperature
- notification history
- app manager
- image compressor
- remove EXIF/GPS
- image to PDF
- QR scanner/generator

Store text must remain natural and match the shipped build.

## Creative / marketing concepts

- `What is using 38 GB on your phone?`
- `Find every file larger than 1 GB`
- `You still have 14 old APK installers`
- `See your storage as a map`
- `2.4 GB moved to Trash — 620 MB physically freed`
- `824 MB of screenshots older than 90 days`
- `One app replaced my cleaner + file manager + phone checker`

Do not market features that are still placeholders.

## Retention thesis

A cleaner alone is episodic. Tooliva stays installed because:
- File Manager/Search is recurrent;
- Phone Checkup gives ongoing maintenance value;
- App Manager helps monitor large/unused apps;
- Notification History is continuously useful;
- image/PDF/QR/network tools create repeated utility.

Avoid spam notifications as a retention hack.

Useful optional reminders may later include:
- storage above a user-selected threshold;
- user-enabled periodic storage review;
- notification retention reminder.

## Product metrics

Acquisition:
- store listing conversion;
- installs by country/source;
- keyword/source performance.

Activation:
- Full Storage Mode grant rate;
- first scan completion;
- first useful recommendation opened;
- first File Manager search;
- first cleanup completed.

Cleaner quality:
- scan duration;
- indexed file count;
- cancellation rate;
- cleanup verification success;
- partial/failure rate;
- bytes moved to Trash;
- bytes physically freed;
- duplicate detection duration.

Retention:
- DAU/MAU;
- File Manager opens/user;
- searches/user;
- checkups/user;
- Notification History usage when enabled.

Monetization:
- ads/user without rating decline;
- Pro conversion;
- refund rate;
- revenue by market.

Trust/quality:
- crash-free users;
- ANR;
- rating;
- review sentiment around deletion, permissions and ads.

## Major risks

### Google Play restricted-permission approval
Mitigation:
- File Manager + on-device search are genuine core functionality;
- Full Storage Mode has a transparent user flow;
- Limited Mode exists;
- prepare the All Files Access declaration before production;
- store listing prominently reflects the file-management purpose.

### Wrong-file deletion
Mitigation:
- preview/review;
- conservative default selections;
- explainable categories;
- central delete/trash coordinator;
- post-action verification;
- Cleanup Receipt;
- destructive regression suite.

### Category is crowded
Mitigation:
- build deeper than shallow one-tap cleaners;
- simplify more than power-user cleaners;
- differentiate through Storage Map, receipts and trust.

### Ads damage ratings
Mitigation:
- strict placement/frequency rules;
- lifetime Pro;
- no ads in critical workflows.

### Feature bloat
Mitigation:
- Cleaner/File Manager Beta before dozens of micro-tools;
- product pillars, not a wall of random icons;
- measure actual usage after launch.

## Launch sequence

### Stage 1 — Cleaner/File Manager Beta
Prove:
- Full Storage Mode;
- all-type Large Files;
- Downloads/APK/archive/document categories;
- File Manager/Search;
- Storage Map;
- duplicates/screenshots;
- cache system flow;
- permission/deletion reliability.

### Stage 2 — Tooliva 1.0
Add recurring value:
- App Manager;
- Phone Doctor / Check My Phone;
- Notification History;
- essential image/PDF/QR/network tools;
- ads + Pro;
- Play declarations/privacy.

### Stage 3
Only after data/reliability:
- Vault;
- App Lock;
- similar-photo intelligence;
- additional power-user tools.

## Product thesis

> A user discovers Tooliva because storage is full, trusts it because every cleanup is explainable and verified, and keeps it because it becomes the phone's file and maintenance toolbox.
