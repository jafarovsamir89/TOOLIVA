# Tooliva — TODO

Legend: `[P0]` release blocker, `[P1]` important, `[P2]` later.

## Phase 0 — Product freeze

- [x] [P0] Working brand: Tooliva
- [ ] [P0] Reserve package name `az.simplesoft.tooliva`
- [x] [P0] Create GitHub repository
- [x] [P0] Add README / agent rules
- [ ] [P0] Add full architecture / policy / privacy docs
- [ ] [P0] Confirm minSdk 26
- [ ] [P0] Set compileSdk/targetSdk 36
- [ ] [P0] Choose ads SDK
- [ ] [P0] Confirm no `MANAGE_EXTERNAL_STORAGE` in MVP
- [ ] [P0] Confirm App Lock implementation strategy before release

## Phase 1 — Foundation

- [ ] [P0] Create Kotlin + Jetpack Compose project
- [ ] [P0] Configure Gradle and version catalog
- [ ] [P0] Set package namespace
- [ ] [P0] Add Material 3
- [ ] [P0] Add Navigation Compose
- [ ] [P0] Add Hilt
- [ ] [P0] Add Room
- [ ] [P0] Add DataStore
- [ ] [P0] Add WorkManager
- [ ] [P0] Create light/dark design system
- [ ] [P0] Add common cards/buttons/dialogs
- [ ] [P0] Add unit/UI test foundations
- [ ] [P0] Add CI build

## Phase 2 — Home

- [ ] [P0] Home screen
- [ ] [P0] Storage summary card
- [ ] [P0] Battery/device summary
- [ ] [P0] Module cards
- [ ] [P0] `CHECK MY PHONE` CTA
- [ ] [P1] Favorites/recent tools
- [ ] [P1] Insight card

## Phase 3 — Cleaner vertical slice

### Storage
- [ ] [P0] MediaStore repository
- [ ] [P0] Storage summary
- [ ] [P0] Progressive scan
- [ ] [P0] Cancellation
- [ ] [P0] Large files query
- [ ] [P0] Preview / multi-select
- [ ] [P0] Central delete coordinator
- [ ] [P0] Android trash/delete request flow
- [ ] [P0] Cleanup result screen

### Screenshots
- [ ] [P0] Detect screenshot buckets
- [ ] [P0] Age filters
- [ ] [P0] Preview grid
- [ ] [P0] Safe delete flow

### Duplicate photos
- [ ] [P1] Metadata pre-grouping
- [ ] [P1] Hash worker
- [ ] [P1] Fingerprint cache
- [ ] [P1] Duplicate groups UI
- [ ] [P1] Keep-one helper

### Cleanup Swipe
- [ ] [P1] Swipe UI
- [ ] [P1] Undo
- [ ] [P1] Delete queue
- [ ] [P1] Finish summary

## Phase 4 — Protect

### Private Vault
- [ ] [P0] Threat model
- [ ] [P0] PIN + biometric unlock
- [ ] [P0] Android Keystore master key
- [ ] [P0] AES-GCM encrypted import
- [ ] [P0] Encrypted metadata
- [ ] [P0] Secure export
- [ ] [P0] Auto-lock
- [ ] [P0] Uninstall/data-loss warning

### Notification History
- [ ] [P0] Disclosure screen
- [ ] [P0] Notification access flow
- [ ] [P0] NotificationListenerService
- [ ] [P0] Local persistence
- [ ] [P0] App filters / retention / clear history
- [ ] [P1] Noisy-app insights

### App Lock
- [ ] [P0] Prototype on Pixel/Samsung/Xiaomi
- [ ] [P0] Verify Play-policy path
- [ ] [P0] Foreground app detection prototype
- [ ] [P0] Lock screen + PIN/biometric
- [ ] [P0] Timeout / relock / reboot tests
- [ ] [P0] Decide GO / NO-GO for Play build

## Phase 5 — Diagnose

- [ ] [P0] Device info
- [ ] [P0] Battery state/temperature when available
- [ ] [P0] Memory/storage facts
- [ ] [P0] Thermal state
- [ ] [P0] Sensor inventory
- [ ] [P1] Display test
- [ ] [P1] Touch test
- [ ] [P1] Vibration / flashlight / speaker / microphone tests
- [ ] [P1] App Usage via Usage Access

## Phase 6 — File tools

- [ ] [P1] Image compress
- [ ] [P1] Image resize
- [ ] [P1] JPEG/PNG/WebP conversion
- [ ] [P1] EXIF viewer
- [ ] [P1] Remove GPS/private metadata
- [ ] [P1] Images → PDF
- [ ] [P2] ZIP/unzip
- [ ] [P2] SHA-256/SHA-512/MD5

## Phase 7 — Everyday tools

- [ ] [P1] QR/barcode scanner
- [ ] [P1] QR generator
- [ ] [P1] Wi-Fi QR
- [ ] [P1] Network info
- [ ] [P1] Ping / DNS lookup
- [ ] [P1] Compass
- [ ] [P1] Bubble level
- [ ] [P1] Flashlight
- [ ] [P2] Magnifier
- [ ] [P2] Unit converter / password generator / color picker

## Phase 8 — Phone Checkup

- [ ] [P0] Checkup orchestration
- [ ] [P0] Storage facts
- [ ] [P0] Cleaner recommendations
- [ ] [P0] Battery/thermal facts
- [ ] [P0] Sensor availability
- [ ] [P1] Notification insights if enabled
- [ ] [P1] Usage insights if enabled
- [ ] [P0] Result cards and deep-links
- [ ] [P0] Verify no fake score/deceptive warnings

## Phase 9 — Monetization

- [ ] [P0] Ads abstraction
- [ ] [P0] Consent flow where required
- [ ] [P0] Restrained banner/native placement
- [ ] [P0] Frequency-capped interstitials
- [ ] [P0] Google Play Billing
- [ ] [P0] Pro Lifetime purchase / restore

## Phase 10 — Privacy / Play

- [ ] [P0] Privacy policy
- [ ] [P0] Data Safety inventory
- [ ] [P0] SDK data audit
- [ ] [P0] Permission disclosures
- [ ] [P0] Verify merged manifest
- [ ] [P0] Ads declarations
- [ ] [P0] Content rating / target audience

## Phase 11 — QA

- [ ] [P0] Android 8–16 matrix
- [ ] [P0] Pixel / Samsung / Xiaomi
- [ ] [P1] 50k-photo stress test
- [ ] [P1] Permission-denied / process-death / reboot tests
- [ ] [P0] Destructive-operation suite
- [ ] [P0] Vault encryption suite

# Tooliva 1.0 release gate

- [ ] Home
- [ ] Phone Checkup basic
- [ ] Storage overview
- [ ] Large files
- [ ] Screenshot cleaner
- [ ] Exact duplicates
- [ ] Cleanup Swipe
- [ ] Notification History
- [ ] Phone Doctor
- [ ] Image Compress
- [ ] EXIF Privacy Clean
- [ ] Images to PDF
- [ ] QR Scanner/Generator
- [ ] Network Info
- [ ] Compass / Level / Flashlight
- [ ] Ads
- [ ] Pro Lifetime
- [ ] Privacy/Data Safety
- [ ] Crash-free closed test

**App Lock and Vault enter 1.0 only if their security and Play-review paths are solid. Otherwise ship them in 1.1.**
