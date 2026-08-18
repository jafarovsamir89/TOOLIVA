# Tooliva — Google Play / Android Policy Notes

Revision: 2026-08-18

This is an engineering/product checklist, not legal advice.

Authoritative product source:
- `docs/PRODUCT_CONSTITUTION.md`

Primary official references:
- Target API: https://developer.android.com/google/play/requirements/target-sdk
- All Files Access: https://support.google.com/googleplay/android-developer/answer/10467955
- Android All Files Access: https://developer.android.com/training/data-storage/manage-all-files
- Package visibility: https://support.google.com/googleplay/android-developer/answer/10158779
- Accessibility: https://support.google.com/googleplay/android-developer/answer/10964491
- StorageManager cache action: https://developer.android.com/reference/android/os/storage/StorageManager#ACTION_CLEAR_APP_CACHE

## 1. Target API

Project baseline:

- compileSdk 36
- targetSdk 36
- minSdk 26

Before release, re-check the current Google Play target API requirement.

## 2. Product purpose and restricted permissions

Tooliva is genuinely defined as:

**Cleaner + File Manager + Device Tools**

Core functions include:

- shared-storage file management;
- on-device file search;
- storage analysis/cleanup.

Restricted permissions must remain directly tied to visible core functionality. The store listing, in-app UX, permission explanation, and actual build must tell the same story.

## 3. `MANAGE_EXTERNAL_STORAGE` / All Files Access

This is a restricted permission requiring Google Play declaration/review.

Tooliva decision:

**approved for development/prototype and intended Full Mode**, because Cleaner + File Manager + on-device search are actual core functionality.

Production submission is contingent on current-policy review and approval.

### Required Full Mode implementation

- explain why shared-storage access is needed before Special App Access;
- user explicitly enables it;
- detect state with `Environment.isExternalStorageManager()` where applicable;
- handle deny/revoke;
- no protected/private path bypass;
- no hidden analytics/ad use of file inventory;
- no ad on disclosure screen.

### Full Mode UX rule

When Full Storage Access is granted on Android 11+, the core Cleaner/File Manager flow should use that access directly.

**Do not immediately ask the user for broad Photos/Videos permission for the same storage-cleaning purpose.**

This applies to Full Mode Large Files, Downloads, APK/Archives/Documents, File Manager, and Screenshot Cleaner where Full Mode access is sufficient.

## 4. Limited Mode / media permissions

If Full Mode is denied/not supported, Tooliva may use:

- MediaStore;
- granular `READ_MEDIA_*` permissions on Android versions where required;
- Storage Access Framework.

Granular media permission must be requested just-in-time for a Limited Mode feature that genuinely needs it.

Do not request media permission during onboarding “just in case.”

Do not present Full All Files Access plus Photos/Videos as two mandatory permissions for one Cleaner job.

Limited Mode must be labelled as limited coverage.

## 5. Manifest discipline

A permission may remain declared for a valid Limited Mode feature, but declaration does not mean it should be prompted in Full Mode.

Before release inspect the merged manifest and confirm:

- every permission has a current user-facing purpose;
- no SDK pulled in unrelated sensitive access;
- runtime/special-access prompts match actual feature flow;
- denied access does not crash or trap the user.

## 6. `QUERY_ALL_PACKAGES`

Installed-app inventory is sensitive and broad visibility is restricted.

Tooliva decision:

- not pre-approved;
- first prototype App Manager with narrower PackageManager visibility;
- document exactly which core behavior is missing;
- re-check current policy;
- obtain explicit human approval;
- only then add/declaration if justified.

Never use installed-app inventory for ad targeting/analytics.

## 7. Usage Access

`PACKAGE_USAGE_STATS` is only for real user-visible usage features, such as last-used or rarely-used app review.

Requirements:

- just-in-time explanation;
- explicit system Settings grant;
- deny/revoke handling;
- no judgemental/scare language;
- no usage inventory sent to ads.

## 8. Cache cleanup

`CLEAR_APP_CACHE` is not a normal third-party permission Tooliva can rely on.

For Android 11+ V1, use the official system-mediated `StorageManager.ACTION_CLEAR_APP_CACHE` where supported/appropriate.

Tooliva must not claim direct silent deletion of every other app's private internal cache.

Handle unsupported/cancel/error honestly.

## 9. Accessibility

AccessibilityService is **not approved by default** for Cleaner or App Lock.

Competitor use does not automatically make it appropriate for Tooliva.

If considered later:

1. define exact user-facing need;
2. verify current Google Play Accessibility policy;
3. obtain explicit human approval;
4. implement prominent disclosure;
5. avoid autonomous/deceptive behavior prohibited by policy.

## 10. Notification Access

Notification History requires explicit Notification Access.

Requirements:

- prominent disclosure;
- local storage by default;
- retention/exclusion controls;
- no notification content in analytics/ads;
- deny/revoke handling.

## 11. File deletion / Trash

Tooliva deletion rules:

- explicit user selection/review;
- selected count and bytes visible;
- platform/system confirmation where required;
- verify after operation;
- distinguish Trash from physical deletion;
- report partial/missing/canceled/revoked states;
- no ads during selection -> confirmation -> operation -> Cleanup Receipt.

Never market bytes moved to Trash as physically freed space.

## 12. Cleaner claims

Forbidden unless actually supported:

- `Boost RAM 300%`
- `Cool CPU`
- `Phone damaged`
- fake virus detection
- fake battery health
- unexplained fake reclaimable bytes
- calling ordinary documents junk without a rule/review context

Preferred factual claims:

- `Find files larger than 1 GB`
- `Review APK installers`
- `Review screenshots older than 90 days`
- `Find exact duplicate files`
- `Moved 2.4 GB to Trash`
- `Physically freed 620 MB`

## 13. File Manager requirement for All Files strategy

If Tooliva requests All Files Access in the production Play build, File Manager/on-device-search functionality must be real and visible, not a placeholder.

Expected product capabilities before restricted-permission submission:

- browse accessible shared storage;
- search;
- sort/filter;
- open/share;
- rename;
- copy/move;
- create folders;
- delete/trash;
- category views such as Downloads/APK/Archives/Documents/media;
- storage analysis.

## 14. Ads

Rules:

- no startup full-screen ad before useful content;
- no ad before showing a requested scan result;
- no repeated interstitial after every action;
- no fake system/virus-style ad;
- no ad on permission disclosure;
- no ad on destructive confirmation;
- no ad during file operation;
- no ad on Cleanup Receipt;
- no ads in Vault/PIN/biometric screens;
- frequency cap interstitials;
- paid/no-ads entitlement must actually suppress ads.

Tooliva should differentiate from ad-heavy cleaners through calmer monetization.

## 15. Data/privacy implications

Scanned file inventory is sensitive from a trust perspective.

Do not send to analytics/ads:

- filenames;
- paths;
- file contents;
- duplicate hashes tied to user files;
- installed-app inventory;
- notification content;
- Vault content.

Use aggregate allow-listed analytics only if introduced later.

## 16. Production All Files Access declaration package

Before Play submission prepare:

- store listing clearly presenting Cleaner + real File Manager/on-device search;
- video/demo showing the user-facing file management/search/cleanup workflow;
- in-app permission disclosure;
- explanation why MediaStore/SAF alone cannot deliver the core automatic shared-storage experience;
- privacy policy language;
- test instructions;
- current policy review notes.

Approval is not guaranteed and must be treated as a release gate.

## 17. Pre-release audit

Before every production release:

- inspect merged manifest;
- re-check target API requirement;
- re-check restricted permissions;
- audit transitive SDK permissions/data collection;
- update Data Safety;
- update privacy policy;
- test all permission grant/deny/revoke flows;
- verify Full Mode does not trigger redundant media prompts;
- verify store claims against actual build;
- verify ads;
- verify destructive flows on physical devices;
- document `QUERY_ALL_PACKAGES` / Accessibility go-no-go decisions if relevant.
