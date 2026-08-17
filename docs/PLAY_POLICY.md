# Tooliva — Google Play / Android Policy Notes

Last reviewed: 2026-08-16

This is an engineering/product checklist, not legal advice.

Primary official policy sources:
- Target API: https://developer.android.com/google/play/requirements/target-sdk
- All Files Access: https://support.google.com/googleplay/android-developer/answer/10467955
- Package visibility: https://support.google.com/googleplay/android-developer/answer/10158779
- Sensitive permissions/API overview: https://support.google.com/googleplay/android-developer/answer/16558241
- Android All Files Access docs: https://developer.android.com/training/data-storage/manage-all-files
- StorageManager cache action: https://developer.android.com/reference/android/os/storage/StorageManager#ACTION_CLEAR_APP_CACHE

---

# 1. Target API

Project baseline:
- compileSdk 36
- targetSdk 36

Starting 2026-08-31, new apps and updates submitted to Google Play must target Android 16 / API 36 or higher (with platform-category exceptions not relevant to Tooliva's phone app).

---

# 2. Product purpose matters to permission eligibility

Tooliva's product definition is now intentionally:

**Cleaner + File Manager + Device Tools**

Core user-facing purposes:
1. access/manage shared-storage files and folders;
2. on-device file search and storage analysis;
3. device-storage maintenance/cleanup.

This positioning is not cosmetic. Restricted permissions must be genuinely required by and prominently tied to core functionality in the app and store listing.

Do not later rewrite the Play listing as merely `photo cleaner + random utilities` while still requesting broad file access.

---

# 3. `MANAGE_EXTERNAL_STORAGE` / All Files Access

Google Play restricts this high-risk permission and requires a Permissions Declaration Form and approval.

Current policy lists eligible core uses including:
- file management;
- backup/restore;
- antivirus;
- document management;
- on-device search;
- disk/folder encryption;
- device migration.

Tooliva's intended justification is **file management + on-device search + storage maintenance as core functionality**.

## Tooliva decision

`MANAGE_EXTERNAL_STORAGE` is **approved for implementation/prototyping**.

Production Play release remains contingent on:
- current-policy re-check;
- complete Play permission declaration;
- app/store UX accurately showing the eligible core functionality;
- review approval.

## Required implementation behavior

Before sending user to Special App Access:
- explain what Full Storage Mode enables;
- explain that it scans shared storage to find/manage files;
- do not imply access to private/protected data that Android still blocks;
- explain Limited Mode fallback;
- no ad on this screen.

State detection:
- use `Environment.isExternalStorageManager()` where applicable;
- handle grant/deny/revoke;
- do not infer grant from manifest presence.

Fallback:
- MediaStore / SAF remain available as Limited Mode;
- denial must not crash the app;
- limited scan results must be labeled truthfully.

## Prohibited use

Do not use broad access for unrelated hidden purposes, ad targeting, analytics collection or server upload of user file inventory.

Do not use it to bypass Android restrictions on protected app-private areas.

---

# 4. `QUERY_ALL_PACKAGES`

Google Play treats installed-app inventory as sensitive data.

Current policy requires broad visibility to be directly necessary for core functionality. Listed permitted categories include file managers and device-search-style apps, but approval is still required.

## Tooliva decision

Not automatically approved merely because competitors use it.

Workflow before adding:
1. implement/prototype App Manager using narrower visibility;
2. document which core user-facing behavior is incomplete;
3. verify current policy;
4. obtain explicit product approval;
5. add permission only if justified;
6. prepare Play declaration;
7. never share app inventory with ad/analytics providers.

Potential eligible uses:
- App Manager requiring broad app discovery;
- mapping accessible leftover files to installed/removed packages when this is a genuine core maintenance feature.

Never request it only for statistics or convenience.

---

# 5. Media permissions

On Android 13+, media access uses granular permissions such as `READ_MEDIA_IMAGES` and `READ_MEDIA_VIDEO`.

Tooliva may still use MediaStore in Limited Mode and for optimized media-specific flows even when Full Storage Mode exists.

Do not request broad media permissions at first launch unless the user enters a feature that requires them.

Full Storage Mode and media permissions must not be mixed into a confusing permission wall.

---

# 6. Cache cleanup

`android.permission.CLEAR_APP_CACHE` itself has `signature|privileged` protection and is not a normal permission Tooliva can rely on as a third-party Play app.

Android 11+ provides:
`StorageManager.ACTION_CLEAR_APP_CACHE`

It:
- requires `MANAGE_EXTERNAL_STORAGE`;
- launches a system/user-mediated cache-clearing flow;
- does not silently clear caches by itself;
- can clear external app cache directories after user/system confirmation.

Tooliva V1 should prefer this official mechanism.

Do not claim Tooliva can directly read/wipe every other app's private internal cache.

---

# 7. Accessibility API

Market competitors use Accessibility for features such as automating repeated Settings/cache actions or background-app stopping. This does **not** automatically make Accessibility approved for Tooliva.

Google Play allows AccessibilityService use beyond accessibility tools only under strict policy/disclosure conditions.

Tooliva rule:
- no AccessibilityService in Cleaner V1;
- no AccessibilityService for App Lock without explicit human approval;
- if considered later, perform a fresh policy review and write a prominent disclosure before implementation.

Official reference:
- https://support.google.com/googleplay/android-developer/answer/10964491

---

# 8. App usage access

`PACKAGE_USAGE_STATS` requires user-granted Usage Access through system settings.

Allowed Tooliva purposes may include:
- last-used app facts;
- app usage duration;
- identifying rarely used apps for user review.

Requirements:
- just-in-time disclosure;
- no judgmental/scare language;
- no app usage inventory sent to advertising analytics.

---

# 9. Notification access

Notification History requires explicit Notification Access.

Requirements:
- prominent disclosure;
- local-first storage;
- exclude-app controls;
- retention controls;
- no notification content sent to analytics/ads;
- handle Android restrictions/redaction honestly.

---

# 10. Deletion / Trash

Use the centralized Tooliva cleanup architecture.

Requirements:
- explicit selection/review;
- show selected count and bytes;
- system confirmation where required;
- verify after action;
- distinguish Trash from physical deletion;
- partial/canceled/permission-changed states;
- no advertisement between selection and Cleanup Receipt.

Do not market `Moved to Trash` bytes as physically freed storage.

---

# 11. File Manager claims

The store listing must visibly promote real file-management/on-device-search functions if the production build requests All Files Access.

Expected core capabilities before permission declaration submission:
- browse shared storage;
- search;
- sort/filter;
- open/share;
- rename;
- copy/move;
- delete/trash;
- folder management;
- categories such as APK/archive/document/download/media;
- storage analysis.

A fake/placeholder file manager is not sufficient justification.

---

# 12. Ads

Rules:
- no full-screen ad at app startup before useful content;
- no ad before scan results;
- no interstitial after every tap;
- no ad triggered by Back/exit;
- no ad styled as a system/virus/security warning;
- no ads on permission disclosure;
- no ads on destructive confirmation;
- no ads on Cleanup Receipt;
- no ads in Vault/PIN/biometric screens;
- frequency cap interstitials;
- paid/no-ads state must actually remove ads.

The reviewed cleaner market shows that intrusive/scare ads directly damage trust. Tooliva must compete by being calmer.

Official policy references:
- https://support.google.com/googleplay/android-developer/answer/9857753
- https://support.google.com/googleplay/android-developer/answer/12271244

---

# 13. Marketing claims

Forbidden unless genuinely implemented and supportable:
- `Boost RAM 300%`
- `Cool CPU`
- `Your phone is damaged`
- `Virus detected` without a legitimate antivirus engine
- fake battery-health percentage
- fake reclaimable-space totals
- calling normal user documents `junk` without explanation

Preferred claims:
- `Find files larger than 1 GB`
- `Review old APK installers`
- `See which folders use the most storage`
- `Find exact duplicate files`
- `Review screenshots older than 90 days`
- `Moved 2.4 GB to Trash`
- `Physically freed 620 MB`

---

# 14. Pre-submission restricted-permission package

Before production submission prepare:

## All Files Access
- screen recording/demo showing core file manager + cleaner workflow;
- store description matching core purpose;
- written explanation why MediaStore/SAF alone substantially harm the core automatic file-management/search experience;
- privacy policy explanation;
- test instructions.

## QUERY_ALL_PACKAGES — only if used
- exact App Manager use case;
- why narrower visibility is insufficient;
- proof it is core to the user-facing product;
- no advertising/analytics use.

---

# 15. Pre-release audit

Before every production release:
- inspect merged manifest;
- inspect all restricted/special permissions;
- review current Play policy again;
- audit dependency-added permissions;
- audit SDK data collection;
- update Data Safety;
- verify privacy policy;
- test ads;
- test grant/deny/revoke for special access;
- verify store claims against actual build;
- document App Lock/Accessibility go/no-go;
- verify targetSdk requirement;
- verify physical-device destructive flows.
