# Tooliva — Google Play Market Research 2026

Last researched: 2026-08-16

Purpose: define Tooliva from evidence in the current Android market, not assumptions.

This document is product research, not legal advice. Google Play install counts are public thresholds such as `50M+`, not exact totals. Permission details from third-party APK/manifest analyzers are treated as supporting evidence and must be re-verified before release.

## Executive conclusion

The market proves that a useful Android cleaner is **not** limited to gallery media.

Popular Play-distributed cleaners and storage tools combine several of these capabilities:

- broad shared-storage scanning;
- large files across multiple types;
- APK/archive/document discovery;
- junk/residual-file candidates;
- duplicate detection;
- screenshot/photo cleanup;
- app manager / unused-app analysis;
- file manager / device search;
- cache-cleaning workflows;
- storage visualization;
- system/battery/device information.

Representative Play apps also use restricted access such as `MANAGE_EXTERNAL_STORAGE`, subject to Google Play declaration and approval. Therefore Tooliva must not cripple its cleaner to media-only scanning merely to avoid a permission review. Instead, **Cleaner + File Management must be a genuine core product purpose**, with a truthful permission declaration and a privacy-safe fallback when broad access is not granted.

Tooliva's opportunity is to combine the depth of serious cleaners/storage analyzers with the trust and usability of Files by Google, then add high-retention utility modules without adopting the category's worst behaviors: fake boost claims, scary warnings, accidental deletion, ad spam and expensive subscription traps.

---

# 1. Competitive set

## 1.1 Phone Cleaner — AI Cleaner (Brain Trust)

Google Play package: `myfiles.filemanager.fileexplorer.cleaner`

Public Play signal at research time:
- 50M+ downloads
- ~4.8 rating
- ads + in-app purchases

Promoted functions:
- junk cleaner;
- large files and folders;
- old/residual APK and temporary files;
- duplicates;
- old screenshots;
- app manager / large installed apps;
- file manager for multiple file categories.

This is especially important because it matches the exact consumer expectation that triggered this research: launch, grant access, scan the phone, then show large files/junk/old files instead of only gallery media.

Supporting manifest analysis from Chrome-Stats reports:
- `MANAGE_EXTERNAL_STORAGE`
- `PACKAGE_USAGE_STATS`
- `REQUEST_DELETE_PACKAGES`
- `REQUEST_INSTALL_PACKAGES`
- `READ_EXTERNAL_STORAGE`

Product lesson:
**Consumers already expect a Play cleaner to understand the broader storage, not just photos and videos.**

Weaknesses visible in Play reviews:
- complaints about very frequent advertising;
- complaints about alarming/deceptive ad creatives from ad networks;
- localization quality complaints.

Tooliva response:
- broad storage capability, but calmer UX;
- no fake virus/scare messages;
- strict ad frequency limits;
- high-quality localization.

Sources:
- https://play.google.com/store/apps/details?id=myfiles.filemanager.fileexplorer.cleaner
- https://chrome-stats.com/d/myfiles.filemanager.fileexplorer.cleaner

---

## 1.2 CCleaner

Google Play package: `com.piriform.ccleaner`

Public Play signal:
- 100M+ downloads
- ~4.6 rating
- millions of reviews

Promoted functions:
- junk/old/residual data cleanup;
- uninstall unused apps;
- application impact analysis;
- network/battery/storage/system monitoring.

Manifest-analysis services report `MANAGE_EXTERNAL_STORAGE` and `PACKAGE_USAGE_STATS` among its permissions.

Strength:
- mature brand and broad maintenance story.

Weaknesses visible in Play reviews:
- premium/ad flow can interfere with cleaning;
- severe trust damage when users believe wrong files were removed;
- photo-compression workflow complaints.

Tooliva response:
- deletion safety is a primary product feature;
- keep the existing verified Cleanup Result architecture;
- separate `Moved to Trash` from `Physically freed`;
- never block a cleanup result behind an advertisement.

Sources:
- https://play.google.com/store/apps/details?id=com.piriform.ccleaner
- https://chrome-stats.com/d/com.piriform.ccleaner

---

## 1.3 AVG Cleaner

Google Play package: `com.avg.cleaner`

Public Play signal:
- 100M+ downloads
- ~4.6 rating
- ~2M reviews

Promoted functions:
- old-file cleanup;
- unwanted photo/video cleanup;
- file management/storage analysis;
- unused app analysis;
- battery/data/storage app impact.

Manifest-analysis services report `MANAGE_EXTERNAL_STORAGE`, `PACKAGE_USAGE_STATS` and package deletion capability.

Product lesson:
Cleaner users value an **App Manager** alongside file cleanup.

Sources:
- https://play.google.com/store/apps/details?id=com.avg.cleaner
- https://chrome-stats.com/d/com.avg.cleaner

---

## 1.4 Avast Cleanup

Google Play package: `com.avast.android.cleaner`

Public Play signal:
- 50M+ downloads
- ~4.6 rating
- >1M reviews

Promoted functions:
- storage analysis and junk cleanup;
- photo organization;
- unused app removal;
- biggest-file/media/app discovery;
- leftover/temporary file cleanup.

The app also declares Accessibility usage for a one-tap background-app workflow. Manifest-analysis services report broad storage permissions in current/recent builds.

Weakness visible in reviews:
- monetization/ad failures can interfere with requested actions.

Tooliva response:
Monetization must be downstream of utility, never a gate in a destructive/storage workflow.

Sources:
- https://play.google.com/store/apps/details?id=com.avast.android.cleaner
- https://chrome-stats.com/d/com.avast.android.cleaner

---

## 1.5 SD Maid 2/SE

Google Play package: `eu.darken.sdmse`

Public Play signal:
- 1M+ downloads
- actively maintained in 2026
- open source
- no ads; some features paid

Promoted functions:
- leftovers from removed apps (`CorpseFinder` concept);
- system-cleaner rules;
- app cleaner;
- duplicate finder;
- file manager;
- identify which apps created files;
- swipe-based file review (`Swiper`);
- media compression (`Media Squeeze`);
- optional Accessibility automation for tedious cache/settings actions.

The current open-source build declares:
- `MANAGE_EXTERNAL_STORAGE`
- `QUERY_ALL_PACKAGES`
- `PACKAGE_USAGE_STATS`
- `REQUEST_DELETE_PACKAGES`
- optional Accessibility-related functionality.

This is the strongest technical proof that a serious Play-facing Cleaner + File Manager can be built around broad storage and package visibility while still being privacy-conscious.

Weakness:
- specialist/power-user feel can be harder for mainstream users.

Tooliva response:
Take the depth, not the complexity.

Sources:
- https://play.google.com/store/apps/details?id=eu.darken.sdmse
- https://github.com/d4rken-org/sdmaid-se
- https://apt.izzysoft.de/fdroid/index/apk/eu.darken.sdmse

---

## 1.6 Files by Google

Google Play package: `com.google.android.apps.nbu.files`

Public Play signal:
- 5B+ downloads
- ~4.5 rating
- no ads
- very small footprint relative to the category

Promoted functions:
- cleaning recommendations;
- old chat photos;
- duplicate files;
- cache cleanup recommendations;
- file browsing/search;
- sort by size;
- Secure Folder;
- Quick Share;
- cloud/SD backup.

Product lesson:
The strongest trust model in this category is **recommend, explain, let the user decide**.

Tooliva response:
Use Files by Google as the benchmark for clarity/trust, but go deeper on storage analysis and device utilities.

Source:
- https://play.google.com/store/apps/details?id=com.google.android.apps.nbu.files

---

## 1.7 Storage Analyzer & Disk Usage

Google Play package: `com.mobile_infographics_tools.mydrive`

Public Play signal:
- 10M+ downloads
- ~4.2 rating

Promoted functions:
- internal/external/SD/USB storage analysis;
- full file metadata index;
- sunburst visualization;
- global search;
- categories by type, size and date;
- installed-app size/cache/last-used analysis;
- open/delete/share;
- cache and app management.

Its Play description explicitly documents use of:
- `MANAGE_EXTERNAL_STORAGE`
- `QUERY_ALL_PACKAGES`
- `PACKAGE_USAGE_STATS`
- `REQUEST_DELETE_PACKAGES`

Product lesson:
A **Storage Map / disk-usage visualization** is a proven feature with real user value, especially for finding hidden space consumers.

Source:
- https://play.google.com/store/apps/details?id=com.mobile_infographics_tools.mydrive

---

## 1.8 Phone Cleaner — JonDev Studio

Google Play package: `phonecleaner.junkfiles.appmanager.duplicatefileremover.applock`

Public Play signal:
- 10M+ downloads
- ~4.3 rating

Promoted functions:
- junk cleaner;
- app manager;
- battery monitor;
- file manager;
- CPU monitor;
- image compression;
- RAM info;
- duplicate remover.

Product lesson:
Cleaner + diagnostics + utility tools is already a validated consumer bundle.

Source:
- https://play.google.com/store/apps/details?id=phonecleaner.junkfiles.appmanager.duplicatefileremover.applock

---

## 1.9 HyperClean AI

Public Play signal:
- 1M+ downloads
- ~4.4 rating

Promoted functions:
- duplicate photos/videos;
- screenshots;
- photo/video compression;
- private vault.

Weakness in reviews:
- high subscription pricing;
- users complain that advertising implies free use while meaningful actions require payment/trial conversion.

Tooliva response:
- no weekly-subscription trap;
- core cleanup must remain genuinely useful for free users;
- lifetime Pro is preferred for V1.

Source:
- https://play.google.com/store/apps/details?id=com.hyperclean.ai

---

## 1.10 Nox Cleaner

Public Play signal:
- 5M+ downloads

Promoted functions:
- junk cleanup;
- app uninstall;
- photo/video cleanup;
- antivirus/security positioning.

Manifest-analysis services report `MANAGE_EXTERNAL_STORAGE`, `PACKAGE_USAGE_STATS`, `QUERY_ALL_PACKAGES`, overlay and other sensitive access.

Weakness in reviews:
- paid users reporting ads;
- removal of previously useful one-tap behavior.

Tooliva response:
Do not sell fake safety/performance. Preserve user trust after purchase.

Sources:
- https://play.google.com/store/apps/details?id=com.nox.app.cleaner
- https://chrome-stats.com/d/com.nox.app.cleaner

---

# 2. Adjacent utility leaders

## 2.1 DevCheck Device & System Info

Public Play signal:
- 10M+ downloads
- ~4.4 rating
- no ads

Strong capabilities:
- CPU/GPU/RAM/storage/system details;
- battery temperature/voltage/current/power where available;
- network details;
- sensors with live values;
- hardware tests;
- app management;
- optional Shizuku/root enhancements;
- widgets and floating monitors.

Product lesson:
Tooliva's `Phone Doctor` must be a real diagnostic module, not three battery numbers and a fake health percentage.

Source:
- https://play.google.com/store/apps/details?id=flar2.devcheck

## 2.2 AppLock leaders

Representative app `com.alpha.applock`:
- 100M+ downloads;
- PIN/pattern;
- vault;
- privacy locking.

Other popular AppLock products add:
- fingerprint;
- random PIN keyboard;
- invisible pattern;
- intruder selfie;
- lock recent apps;
- fake crash screen;
- notification privacy.

Product lesson:
If Tooliva ships App Lock, a basic PIN overlay is not enough. Reliability on Xiaomi/Samsung/Pixel and bypass-resistance matter more than feature count.

Source:
- https://play.google.com/store/apps/details?id=com.alpha.applock

## 2.3 Notification History category

Representative modern products offer:
- local notification archive;
- grouped-by-app history;
- search;
- filters/date/channel;
- favorite/pin;
- excluded apps;
- retention controls;
- export;
- notification-volume insights.

Product lesson:
Tooliva can beat many standalone notification-history apps through a better interface and true local-first privacy, while using the same module for `Notification Doctor` insights.

Representative source:
- https://play.google.com/store/apps/details?id=com.notificationhistorylog

## 2.4 3C All-in-One Toolbox

Public Play signal:
- 1M+ downloads

This represents the power-user `everything in one app` side of the market: monitoring, app/device management, widgets, system controls and many advanced tools.

Product lesson:
Breadth has demand, but a giant wall of controls is not the UX target. Tooliva should keep five understandable product pillars and progressively reveal advanced tools.

Source:
- https://play.google.com/store/apps/details?id=ccc71.at.free

---

# 3. What users repeatedly value

Across the reviewed products, the repeat winners are:

1. **One scan that immediately explains storage usage.**
2. **Large files across real storage, not only media.**
3. **Junk/residual/old-file candidates.**
4. **Duplicates.**
5. **Screenshots and photo cleanup.**
6. **Unused/large app management.**
7. **File browser/search/sort.**
8. **Cache-cleaning entry point.**
9. **Clear amount of recoverable space.**
10. **Fast review before delete.**
11. **Device/battery diagnostics.**
12. **Privacy tools that justify keeping the app installed.**

---

# 4. What users repeatedly hate

The market creates an unusually clear opportunity by repeatedly damaging user trust.

Avoid:

- interstitial ads every few taps;
- alarming virus/sexual-content ad creatives;
- advertising `free` then forcing a costly weekly plan;
- cleanup locked behind watching ads;
- wrong-file deletion;
- vague `junk` totals without explaining categories;
- claiming Trash bytes are physically freed;
- fake CPU/RAM/temperature optimization;
- one-tap destructive behavior with no review;
- unclear translations;
- subscription/payment state that still shows ads.

Tooliva should treat **trust as a feature**, not merely a privacy statement.

---

# 5. Permission reality

## MANAGE_EXTERNAL_STORAGE

Current Google Play policy allows All Files Access when it is directly required for eligible core purposes including:
- file management;
- backup/restore;
- antivirus;
- document management;
- on-device search;
- disk/folder encryption;
- device migration.

It requires a Permissions Declaration and Play approval.

Official policy:
- https://support.google.com/googleplay/android-developer/answer/10467955

Tooliva decision:
**Approved for implementation/prototyping because file management and device-storage maintenance are now explicit core purposes.**

Requirements:
- prominent explanation;
- special-access settings flow;
- limited-mode fallback;
- no hidden/undeclared use;
- re-review before Play submission.

## QUERY_ALL_PACKAGES

Current Play policy permits broad package visibility only when it is genuinely required by core functionality; file managers are one listed eligible category.

Official policy:
- https://support.google.com/googleplay/android-developer/answer/10158779

Tooliva decision:
Do not add it merely for convenience. Add only when the App Manager / file-to-app attribution implementation demonstrably requires broad visibility, and then document/declaration-test it separately.

## Cache cleanup

Android exposes `StorageManager.ACTION_CLEAR_APP_CACHE` on API 30+, which launches a **system/user-mediated** cache-clearing flow and requires `MANAGE_EXTERNAL_STORAGE`.

Tooliva V1 should use this official path instead of pretending it can silently wipe every app's private internal cache.

Official reference:
- https://developer.android.com/reference/android/os/storage/StorageManager#ACTION_CLEAR_APP_CACHE

`android.permission.CLEAR_APP_CACHE` itself is signature/privileged and is not a normal third-party-app permission.

---

# 6. New Tooliva product definition

## Positioning

**Tooliva — Cleaner, File Manager & Device Tools**

Core purpose:

> Find what occupies Android storage, safely clean what the user chooses, manage files and apps, and provide trustworthy device-maintenance tools in one application.

The product hierarchy is now:

### 1. CLEAN — primary acquisition engine
Deep Storage Scan, Junk Candidates, Large Files, Downloads, APKs, Archives, Documents, Old Files, Duplicates, Screenshots, Empty Folders, App leftovers where defensible, Cache Cleanup, Cleanup Swipe.

### 2. FILES — core functionality
Full shared-storage browser, category browser, search, sort, copy, move, rename, delete/trash, share, ZIP/unZIP, hashes, storage map.

### 3. APPS / DIAGNOSE — recurring device maintenance
App Manager, unused/large apps, usage facts, Phone Doctor, sensors/hardware tests.

### 4. PROTECT — retention
Notification History first; Vault and App Lock after separate security/policy validation.

### 5. TOOLS — retention and ASO breadth
Image/PDF/EXIF, QR, network, compass, level, flashlight, magnifier and small local tools.

---

# 7. Best-of-market strategy

Tooliva should combine:

- **SD Maid depth** — real filesystem thinking, leftovers, duplicates, safe review;
- **Files by Google trust** — recommendations, clarity, no scare language;
- **Storage Analyzer visibility** — map/search/category understanding;
- **AI Cleaner mainstream simplicity** — one obvious scan and broad categories;
- **DevCheck diagnostics** — real technical device data;
- **AppLock retention** — only if we can make it reliable;
- **modern toolbox breadth** — without turning the home screen into 100 random icons.

This is an inference from the representative leaders above, not a claim that no other Android app has any overlapping combination.

---

# 8. Tooliva differentiators

Required differentiators, not optional marketing copy:

1. **Verified Cleanup Receipt**
   - requested bytes;
   - moved to Trash;
   - physically freed;
   - failed/missing/canceled.

2. **Storage Map**
   - visual tree/treemap/sunburst-style view;
   - drill into biggest folders/files.

3. **Explainable Junk**
   Every candidate has a reason/category. No mysterious `8 GB junk` number.

4. **Dual access model**
   - Full Storage Mode with approved All Files Access;
   - Limited Mode fallback when permission is denied/unavailable.

5. **One Scan → Action Plan**
   A scan returns the highest-value actions instead of dumping a technical list.

6. **No fake optimization**
   No RAM boost, CPU cooling, fake antivirus or invented health percentages.

7. **Local-first privacy**
   Storage scans, duplicate fingerprints, notification content and Vault data stay local.

8. **Respectful monetization**
   No ad before the requested result and no weekly-subscription trap for basic cleaning.

---

# 9. Product priority after research

## 9.1 Expansion validation notes

The latest review compared the planned slice against current public descriptions and documentation for Files by Google, SD Maid 2/SE, Cx File Explorer and Solid Explorer. The common expectations are: real large/duplicate/old-photo suggestions, favorites and recent access, app/usage visibility, storage visualization, Trash/restore, archives, and local/cloud/removable storage.

Tooliva's implementation deliberately keeps the safer differentiators from that comparison: explicit review before deletion, verified Cleanup Receipts, no fake optimization claims, local-only photo heuristics, and SAF-based user-selected external/cloud roots. This avoids broad package visibility, hidden Accessibility automation and a new network backend merely to imitate a competitor.

The old roadmap was too broad too early.

New order:

1. Full Storage Access + storage index
2. Deep Cleaner categories
3. Real File Manager + global search
4. Storage Map
5. Exact duplicates + screenshots + Cleanup Swipe
6. App Manager + cache-clean entry point
7. Phone Checkup / diagnostics
8. Notification History
9. File/image/PDF/QR/network utility tools
10. Vault
11. App Lock after reliability/policy prototype

Cleaner/File Manager depth comes before adding dozens of unrelated utilities.
