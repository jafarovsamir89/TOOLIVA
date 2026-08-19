# Tooliva — Privacy & Security Model

Revision: 2026-08-16

## Product promise

> Tooliva may receive broad local storage access to do its job, but the user's file inventory and content stay on the user's device by default.

A powerful Cleaner/File Manager has more responsibility, not less. Full Storage Mode must never become an excuse to collect or transmit personal file information.

---

# 1. Sensitive local data

Treat these as local-only sensitive data:
- file/folder names and paths;
- shared-storage index;
- file metadata;
- duplicate fingerprints/hashes;
- cleanup candidate history/receipts when it can reveal filenames;
- installed-app inventory;
- app usage summaries;
- Notification History/content;
- Vault files/metadata;
- App Lock settings.

Do not transmit these to:
- ad networks;
- analytics providers;
- backend APIs;
- crash logs;
- remote AI services.

If a future feature genuinely requires cloud transfer, it needs a separate explicit product/privacy decision and user action.

---

# 2. Full Storage Mode

`MANAGE_EXTERNAL_STORAGE` is approved because file management, on-device search and storage maintenance are core product purposes.

Privacy requirements:
- prominent explanation before Special App Access;
- explain the scope: shared-storage files/folders;
- explain purpose: analyze storage, search/manage files, find cleanup candidates;
- no background upload;
- Limited Mode when not granted;
- grant state can be revoked by user;
- access is not reused for advertising profiles.

Do not claim Tooliva can see protected/private app areas that Android still blocks.

The local index should contain only fields required for product functionality.

---

# 3. Storage index

Local index may contain:
- file ref/path/name;
- type/extension/MIME;
- size;
- modified date;
- volume/category;
- fingerprint metadata required for duplicates.

Rules:
- local Room database only;
- no names/paths in analytics;
- no full hashes in analytics;
- clear/rebuild controls should exist if useful;
- invalidate stale entries when storage permission/volume changes;
- exclude app-private secrets/tool temp files from generic analytics.

At-rest database encryption is optional for ordinary shared-storage metadata, but required to be reconsidered for Notification History/Vault data.

---

# 4. Explainable cleanup safety

Tooliva must never hide risk behind the word `junk`.

Every candidate should have:
- category/reason;
- size;
- user review path;
- conservative default-selection rule.

Sensitive/ambiguous categories such as documents are never auto-selected merely because they are old or large.

Exact duplicates require actual exact-match verification. Similar files/photos must be labeled similar, not duplicate.

---

# 5. Destructive operations

Core safety invariant:

> No destructive state change without user intent, and no success claim without verification where technically possible.

Pipeline:
1. review/select;
2. revalidate file exists/state;
3. confirm;
4. execute via approved path;
5. verify/re-index;
6. show Cleanup Receipt.

Receipt distinguishes:
- requested;
- missing before;
- moved to Trash;
- physically freed;
- unchanged/failed;
- canceled;
- permission revoked.

Never count Trash bytes as physically freed.

For copy→delete move fallbacks, verify the destination before removing the source.

---

# 6. File Manager operations

Copy/move/rename/create/delete must handle:
- duplicate names/collisions;
- low storage;
- partial completion;
- cancellation;
- inaccessible/read-only targets;
- process interruption.

Do not overwrite a destination silently unless the user explicitly chose a replace policy.

Temporary operation files must be cleaned safely after success/failure.

---

# 7. App Manager privacy

Installed-app inventory is sensitive.

Rules:
- keep locally;
- never use for ad targeting;
- never send package list to analytics;
- `QUERY_ALL_PACKAGES` only if separately approved/declared;
- Usage Access is optional/user-enabled;
- usage summaries remain local.

Uninstall always goes through user/system-mediated Android behavior.

---

# 8. Cache cleanup

Cache Cleaner v2 uses package metadata for discovered browsers/YouTube and Android-provided `StorageStats.cacheBytes` only after explicit Usage Access. Values stay local, package failures are isolated and unavailable values are not replaced with fake zeroes.

The selected package list and before/after measurements are local ephemeral session data. They are cleared after completion/cancel/timeout and never sent to backend, analytics or ads.

The current build does not declare AccessibilityService. Cache cleanup opens Android App Info and leaves the final action to the user; no Settings UI is inspected or automated.

The official system-mediated Phone Optimizer action still does not expose a per-app deletion list or a cache-specific byte guarantee.

Do not display fake per-app reclaimable cache values if Android does not expose defensible figures.

---

# 9. Operational analytics

Allowed privacy-safe aggregate examples:
- feature opened;
- scan completed;
- scan duration bucket;
- item-count bucket;
- total-byte bucket if coarse and not linked to filenames;
- access mode Full/Limited;
- generic error code;
- app version;
- Android/API/device compatibility data.

Never include:
- filenames;
- paths;
- file contents;
- hashes/fingerprints;
- exact notification text;
- installed-package list;
- QR content;
- Vault metadata/content.

---

# 10. Vault threat model

Designed to reduce casual/local unauthorized access on a normally secured Android device.

Not promised:
- protection against rooted/fully compromised device;
- forensic-grade adversaries;
- recovery after uninstall/device loss without explicit backup/export.

Recommended encryption:
- AES-GCM authenticated encryption;
- master key protected by Android Keystore;
- unique nonce per file;
- versioned container;
- integrity validation.

Import:
1. read source;
2. encrypt to temporary destination;
3. flush/close;
4. verify decrypt/integrity;
5. commit encrypted metadata;
6. only then offer source deletion.

---

# 11. App Lock

PIN:
- never plaintext;
- salted slow KDF/verifier.

Biometric:
- system/AndroidX BiometricPrompt.

Do not claim protection against root/system attackers.

The selected-app Accessibility experiment was removed after unreliable Xiaomi behavior. Accessibility is not part of the current manifest or product path. Any future reintroduction requires a new explicit decision and policy review.

---

# 12. Notification History

- opt-in Notification Access only;
- prominent disclosure;
- local persistence;
- retention controls;
- exclude-app controls;
- clear-all;
- no notification content in analytics/logging/ads.

Consider database encryption based on performance/threat-model review before release.

---

# 13. Logging

Production logs must never include:
- file/folder names or paths;
- file hashes/fingerprints;
- installed package inventory;
- QR contents;
- notification text;
- PIN/biometric secrets;
- Vault item information that reveals content.

Use redacted IDs and generic error codes.

---

# 14. Android backup

Review Auto Backup explicitly.

- local storage index can generally be reconstructed and should not need cloud backup;
- notification content should not accidentally enter cloud backup without deliberate policy;
- Vault must never be backed up plaintext;
- users must understand uninstall/data-clear loss behavior for local-only Vault data.

---

# 15. Screen privacy

Evaluate secure-window/recents protections for:
- Vault;
- PIN setup/unlock;
- sensitive future exports.

Normal Cleaner/File Manager screens should remain screenshottable/shareable unless a specific sensitive preview demands otherwise.

---

# 16. Ads

Ad SDKs must never receive:
- file inventory;
- file/category selections tied to names;
- notification content;
- app inventory;
- Vault data;
- scan contents.

No ads on:
- All Files Access disclosure;
- destructive confirmation;
- Cleanup Receipt;
- Vault/auth screens.

---

# 17. Security/privacy release checklist

Storage/File Manager:
- [ ] Full Storage disclosure reviewed
- [ ] grant/deny/revoke tested
- [ ] no scan network traffic containing user inventory
- [ ] production logging audited
- [ ] copy/move verification tested
- [ ] destructive receipt verified
- [ ] protected path handling tested
- [ ] low-storage/process-death cases tested

Restricted app/package access:
- [ ] permissions match documented core purpose
- [ ] package inventory never leaves device
- [ ] Usage Access remains optional

Vault if shipped:
- [ ] encryption round-trip
- [ ] corrupted container
- [ ] process death during import
- [ ] insufficient space
- [ ] source deletion only after verified encrypted copy
- [ ] PIN throttling/brute-force review
- [ ] exported temp cleanup
- [ ] backup behavior review

Whole app:
- [ ] dependency/security review
- [ ] Data Safety matches actual SDK behavior
- [ ] privacy policy matches actual build
