# Tooliva — QA Plan

Revision: 2026-08-16

## QA principle

Tooliva touches user files. A crash is bad; a wrong-file deletion is unacceptable.

Cleaner/File Manager QA has higher priority than adding more features.

---

# 1. Device / Android matrix

Required before Cleaner/File Manager Beta:
- Android 11
- Android 12
- Android 13
- Android 14
- Android 15
- Android 16

Compatibility checks:
- Android 8–10 Limited/legacy paths where minSdk support remains

Physical OEM minimum:
- Xiaomi/Redmi/HyperOS or MIUI
- Samsung/One UI
- Pixel/AOSP-like Android

Optional later:
- Oppo/Realme
- Vivo
- Motorola

---

# 2. Storage access matrix

For every supported modern Android version verify:

## Full Storage Mode
- permission not granted
- disclosure shown
- open Special App Access
- grant
- return to Tooliva
- access state refreshes
- deep scan works
- revoke while app backgrounded
- return to app
- state downgrades to Limited without crash

## Limited Mode
- deny All Files Access
- media access grant/deny
- SAF grant/cancel where used
- UI explicitly says coverage is limited
- no false `full scan completed` claim

---

# 3. Synthetic storage dataset

Maintain a reproducible test dataset that contains disposable files only.

Minimum:
- images
- videos
- audio
- screenshots with old/new dates
- APK installers
- ZIP
- RAR/7Z if supported
- PDF
- DOC/DOCX
- XLS/XLSX
- PPT/PPTX
- TXT
- unknown extensions
- zero-byte files
- empty folders
- nested folders
- Unicode names
- names with spaces/symbols
- same filename in different folders
- duplicate exact files
- same-size but different-content files
- files >100 MB
- files >500 MB
- files >1 GB when test storage allows

Never use irreplaceable personal media for destructive QA.

---

# 4. Scan tests

- progressive first results
- cancel immediately
- cancel mid-scan
- restart after cancel
- file deleted externally during scan
- file renamed externally during scan
- volume removed during scan
- unreadable path
- very deep folder structure
- large folder count
- scan after permission revoke
- scan after permission re-grant
- repeated scan uses index/reuse strategy correctly
- Tooliva internal/temp files excluded

Performance datasets:
- 1k files
- 10k files
- 50k files P0
- 100k+ P1

Measure:
- scan time
- time to first useful result
- peak memory
- ANR
- cancellation latency
- index DB size

---

# 5. Cleaner category tests

## Large Files
Verify categories:
- Video
- Image
- Audio
- APK
- Archive
- Document
- Download
- Other

Verify thresholds:
- 100 MB
- 500 MB
- 1 GB

Verify sorting:
- size
- date
- name

## Downloads
- APK
- archives
- documents
- media
- old files
- large files

## Old files
- 30/90/180/365 filters
- boundary dates
- unknown modified date

## Junk rules
Every rule gets unit fixtures for:
- positive match
- negative match
- ambiguous case
- default-selected state
- reason text/domain reason

Documents must not become generic junk by accident.

---

# 6. Screenshot tests

- screenshot bucket/path variants from Xiaomi/Samsung/Pixel
- 30/90/365 filters
- real thumbnails
- missing/corrupt thumbnail
- multi-select
- Select all
- deselect
- permission deny/revoke
- trash cancel
- trash success
- rescan removes active entry
- Cleanup Receipt accurate

---

# 7. Duplicate tests

Fixtures:
- exact duplicate bytes / different names
- exact duplicate / different folders
- same size / different content
- image re-encoded visually same but not exact
- file modified after fingerprint cached
- hash cancellation
- large duplicate files

Verify:
- exact groups only contain verified exact matches
- keep-one helper never selects every copy
- recoverable byte total correct
- cache invalidates stale fingerprints

Similar-photo feature later must use separate labels/tests.

---

# 8. File Manager tests

Navigation:
- root/volume
- nested folders
- breadcrumbs
- category shortcuts

Operations:
- open
- share
- rename
- create folder
- copy
- move
- delete/trash

Collision cases:
- destination exists
- same name different contents
- same source/destination
- nested target invalid

Failure cases:
- destination read-only
- insufficient space
- source disappears
- permission revoked
- volume removed
- process death/cancel during copy/move

For move fallback using copy+delete:
- destination verification must happen before source deletion.

---

# 9. Destructive operation suite

For each applicable file type:
- image
- video
- APK
- ZIP
- PDF
- document
- audio
- unknown

Test:
- select one
- select many
- cancel before platform action
- cancel platform action
- success
- partial result
- file already missing
- permission revoked
- repeat scan

Cleanup Receipt must correctly report:
- requested
- already missing
- moved to Trash
- physically freed
- unchanged/failed
- canceled

Do not accept a release if Trash bytes are shown as physically freed.

---

# 10. Cache cleanup

On API 30+:
- Full Storage permission missing
- launch official action
- user cancels
- success result
- OEM does not implement as expected
- no fake cache amount shown

Tooliva must never imply direct private-cache deletion if the system action is unavailable.

---

# 11. App Manager tests

Before `QUERY_ALL_PACKAGES`:
- inspect real visibility and document missing user-facing behavior

If broad visibility is approved later:
- user apps
- system apps
- disabled apps where visible
- launch
- App Info
- uninstall request
- no package list in logs/analytics

Usage Access:
- deny
- grant
- revoke
- no `unused` recommendation without evidence

---

# 12. Storage Map

- total displayed bytes reconcile with indexed hierarchy
- large folder drill-down
- tiny sectors remain accessible through list fallback
- empty volume
- huge number of children
- path opens correct File Manager location

---

# 13. Phone Doctor

Verify values against system settings/ADB where possible:
- battery level
- temperature
- voltage
- RAM
- storage
- thermal state
- sensors

Unsupported values display `Unavailable`, never fabricated values.

---

# 14. Notification History

- access deny/grant/revoke
- notification received/removed
- excluded app
- long text
- 10k records
- retention cleanup
- search/filter
- no notification text in logs/network analytics

---

# 15. Ads / monetization regression

- no ad on All Files disclosure
- no ad before scan result
- no ad during selection→delete→receipt
- no ad on Cleanup Receipt
- offline ad failure cannot block utility
- Pro removes ads
- restore purchase
- no fake system-looking ad surface

---

# 16. Network/privacy audit

During storage scan and file operations capture network traffic.

Required result:
- no filename/path/hash/file-content upload
- no installed-app inventory upload
- no Notification History upload

Audit production logs for same data.

---

# 17. Release gates

## Cleaner/File Manager Beta
Must have:
- Full + Limited access tests
- Xiaomi/Samsung/Pixel smoke test
- 50k-file stress
- all-type Large Files
- File Manager critical operations
- destructive regression suite
- duplicates/screenshots
- Storage Map basic
- cache action tested
- CI green

## Tooliva 1.0
Additionally:
- App Manager
- Phone Doctor/Checkup
- Notification History
- core content/tools
- monetization regression
- Data Safety/privacy/restricted-permission review
- closed test with no P0 file-loss bugs
