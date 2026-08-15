# Tooliva — Technical Specification

Version: 0.1  
Platform: Android  
Product: Offline-first all-in-one utility

## 1. Goal

Build a trustworthy Android toolbox with five coherent pillars:

1. Clean
2. Protect
3. Diagnose
4. Files
5. Tools

Tooliva is not a fake optimizer. It only reports measurable facts and only deletes user data after explicit review/confirmation.

## 2. Non-goals

V1 must not implement:
- fake RAM booster
- fake CPU cooler
- fake antivirus warnings
- invented battery-health percentages
- root features
- device-admin abuse
- hidden monitoring
- VPN
- cloud photo upload
- mandatory account creation
- remote control
- aggressive notification spam

## 3. Navigation

Bottom navigation, maximum five destinations:
- Home
- Clean
- Protect
- Tools
- More

Diagnostics and file tools may be launched from Home/Tools cards so the app does not become a wall of tabs.

## 4. Home

Header:
- Tooliva logo
- Settings
- Pro status

Summary:
- storage used/total
- battery percentage
- thermal state when available
- network state

Primary CTA:
`CHECK MY PHONE`

Main cards:
- Clean storage
- Protect apps/data
- Notification history
- Phone Doctor
- File tools
- QR/network tools

Insights must be derived from local facts, e.g. `Screenshots use 3.2 GB`.

## 5. Clean

### Storage overview

Show total/used/free storage plus accessible categories.

Requirements:
- cancellable scan
- progressive results
- no UI freeze
- cached metadata where useful

### Large files

Filters:
- >100 MB
- >500 MB
- >1 GB
- videos
- downloads
- archives

Actions:
- preview
- open
- share
- select
- delete

### Screenshot cleaner

Detect screenshots through MediaStore metadata/path buckets. Filters: 30/90/365 days and manual selection.

Never label a screenshot “safe to delete.”

### Old videos

Filter by age and size. Do not claim a file was “never opened” unless Android provides defensible evidence.

### Exact duplicate photos

V1 algorithm:
1. pre-group by metadata/size/dimensions;
2. compute content hash for candidates;
3. cache fingerprints;
4. group exact matches.

Similar-photo detection is a later feature.

### Cleanup Swipe

Review queue:
- left = mark delete
- right = keep
- undo
- details

At finish show selected count/bytes and request final deletion confirmation.

### Downloads analyzer

Prefer Storage Access Framework/user-selected directory access.

Categories:
- APK
- archive
- PDF/document
- media
- other

## 6. Protect

### App Lock

Goal: PIN/biometric gate for selected apps.

Preferred exploration:
- permitted package visibility
- UsageStats/usage events for foreground observation where viable
- overlay/lock activity only after explicit setup
- AndroidX Biometric
- relock timeout

Do not base architecture on AccessibilityService without explicit approval and a current Play-policy review.

### Private Vault

Supported:
- photos
- videos
- documents

Flow:
1. user selects content;
2. encrypt/copy into app-private storage;
3. verify encrypted write;
4. only then offer removal of source.

Requirements:
- PIN + biometric
- Android Keystore
- AES-GCM or equivalent authenticated encryption
- encrypted metadata
- auto-lock on background
- secure export with re-authentication
- clear uninstall/data-loss warning

### Notification History

After explicit opt-in to Notification Access:
- local history
- app filter
- search
- retention controls
- clear history
- noisy-app insights

Never send notification content to analytics or server.

## 7. Diagnose

### Phone Doctor

Device:
- manufacturer/model
- Android/API
- ABI
- display density/resolution

Battery:
- level
- charging state/source
- voltage if available
- temperature if available

Memory:
- total/available RAM
- low-memory state

Thermal:
- platform thermal state when available

Sensors:
- accelerometer
- gyroscope
- proximity
- magnetometer
- light
- pressure
- rotation vector
- step sensors

### Hardware tests

Guided tests:
- display colors/dead pixels
- touch grid
- speaker
- microphone
- vibration
- flashlight
- proximity
- accelerometer
- compass

Every result indicates whether it was automatically measured or manually confirmed by the user.

### App Usage

After explicit Usage Access:
- today
- 7 days
- 30 days
- top apps
- usage duration/trends

No moralizing notifications by default.

## 8. File tools

### Images
- compress JPEG/WebP
- resize
- convert JPEG/PNG/WebP
- batch mode
- preserve original by default

### EXIF Privacy Clean
Show detected metadata such as GPS/device/date and let the user remove selected private metadata while preserving visual orientation.

### Images to PDF
- multi-select
- reorder
- page size/margins
- compression
- save/share

### Archive/hash
Later:
- ZIP/unzip with path traversal protection
- SHA-256
- SHA-512
- MD5 compatibility mode

## 9. Everyday tools

### QR/barcode
Scan common formats. Generate QR for:
- text
- URL
- phone/email
- contact
- Wi-Fi

Never auto-open a scanned URL without user confirmation.

### Network Doctor
- connection type
- local IP
- DNS/link properties where available
- metered state
- ping
- DNS lookup
- gateway reachability

No hosted speed-test backend in V1.

### Small tools
- compass
- bubble level
- flashlight
- magnifier later

## 10. Phone Checkup

Pipeline:
1. storage/device facts
2. accessible media scan
3. screenshot analysis
4. large files
5. cached exact duplicates
6. battery/thermal facts
7. sensor availability
8. notification summary if enabled
9. usage summary if enabled

Each result contains:
- category
- measured value
- explanation
- action

Avoid fake health scores and red “danger” styling for normal states.

## 11. Permissions strategy

Request just in time, not all at onboarding.

Potential access:
- Android-version-appropriate media permissions
- camera/microphone only for the tool using them
- notification listener special access
- usage access
- overlay only if App Lock requires it
- SAF grants

Default MVP position: **do not request `MANAGE_EXTERNAL_STORAGE`.**

## 12. Persistence

Room candidates:
- `ScanSnapshot`
- `MediaFingerprint`
- `CleanupDecision`
- `NotificationRecord`
- `NotificationAppRule`
- `LockedApp`
- `UsageSnapshot`
- `HardwareTestResult`
- `ToolFavorite`

DataStore:
- app preferences

Vault data is isolated and encrypted.

## 13. Performance

- no blocking scan on UI thread
- incremental results
- cancellable work
- WorkManager only for appropriate deferred work
- no permanent foreground service unless a feature truly requires it
- avoid repeated full-device scans
- test large media libraries

## 14. Ads

Allowed:
- restrained banner/native ads on non-sensitive utility screens
- frequency-capped interstitial after a completed workflow

Forbidden:
- startup interstitial
- ad after every tap
- ad disguised as a Fix/System button
- ads in Vault/PIN/biometric screens
- ads on permission explanations or delete confirmation

## 15. Release criteria

Do not publish until:
- core flows work Android 8–16
- destructive operations are verified
- privacy policy/Data Safety match the build
- merged manifest is audited
- targetSdk 36
- App Lock has a documented go/no-go decision
- no restricted permission is pulled in accidentally by an SDK
