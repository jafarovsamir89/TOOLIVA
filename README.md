# Tooliva

**Tooliva — Cleaner, App Lock & Tools**

> One Android app for cleaning storage, protecting private data, diagnosing the phone, working with files and handling everyday utilities.

## Product idea

Tooliva is an **offline-first, all-in-one Android utility**. It replaces a group of separate apps with one polished toolbox:

- Smart Cleaner
- Large Files
- Screenshot Cleaner
- Duplicate Photos
- App Lock
- Private Vault
- Notification History
- Phone Doctor
- App Usage
- Image Tools
- PDF Tools
- Network Tools
- QR Tools
- Device Tests
- Small everyday tools

The app must **never use fake “RAM boost”, “CPU cooling”, fake antivirus alerts or invented health scores**. Tooliva only shows information Android can actually provide and only deletes data after explicit user confirmation.

## Product principles

1. **Useful before clever.**
2. **Local-first.** Core features work without an account or backend.
3. **Privacy-first.** Files, photos, notification history and vault content stay on device.
4. **No fake optimization.**
5. **One-tap entry points.** Every major feature should be reachable in 1–2 taps.
6. **Fast on mid-range phones.**
7. **No ad spam.**
8. **Modular architecture.** New tools can be added without turning the app into a monolith.

## V1 feature set

### Clean
- Storage overview
- Large files
- Screenshot cleaner
- Old videos
- Exact duplicate photos
- Cleanup Swipe
- Downloads analyzer

### Protect
- App Lock
- Private Vault
- Notification History

### Diagnose
- Phone Doctor
- Battery / thermal status
- Sensor diagnostics
- App usage statistics
- Screen / touch / speaker / microphone / vibration tests

### Files
- Image compress
- Image resize
- Image convert
- EXIF / GPS metadata remover
- Images to PDF
- ZIP / unzip
- File hash

### Tools
- QR / barcode scanner
- QR generator
- Wi-Fi QR
- Network info
- Ping / DNS lookup
- Compass
- Bubble level
- Flashlight
- Magnifier

Not all small tools need to ship on day one. The MVP release gate is defined in `TODO.md`.

## Killer feature: Phone Checkup

A guided one-tap scan:

1. Storage status
2. Large files
3. Screenshot accumulation
4. Duplicate media
5. Battery / thermal state
6. Sensors
7. Notification volume
8. App usage

Example result:

> Storage: 93% full  
> 5.4 GB ready for review  
> 1,283 old screenshots  
> 24 exact duplicate photos  
> Battery temperature: normal  
> Instagram generated 41% of notifications this week

Actions:
- Review storage
- Review notifications
- Review app usage
- Run hardware tests

The checkup **must not invent a “phone health score” unless every component of the score is transparent and measurable**.

## Technology

- Kotlin
- Jetpack Compose
- Material 3
- Coroutines + Flow
- Room
- DataStore
- Hilt
- WorkManager
- AndroidX Biometric
- MediaStore
- Storage Access Framework
- UsageStatsManager
- NotificationListenerService
- SensorManager
- ConnectivityManager
- Android Keystore
- CameraX / barcode scanning where needed

### Android targets

- `compileSdk`: 36
- `targetSdk`: 36
- `minSdk`: 26 initially
- AAB for Google Play
- 64-bit support

## Backend

**No backend for V1.**

The app must remain useful if every server disappears.

## Monetization

### Free
- Core tools
- Small banner/native ads in non-sensitive screens
- Limited interstitials only at natural completion points

### Pro Lifetime
Initial target: **US$4.99** equivalent where appropriate.

Pro:
- no ads
- advanced duplicate/similar-photo features
- advanced filters
- extended notification history controls
- extra themes / customization
- future premium local tools

No subscription is required for the first release.

## Repository documents

- `TECH_SPEC.md` — technical/product specification
- `TODO.md` — implementation checklist
- `BUSINESS_PLAN.md` — monetization and growth
- `ARCHITECTURE.md` — code architecture
- `AGENTS.md` — rules for AI coding agents
- `docs/FEATURE_MATRIX.md` — feature priorities and permissions
- `docs/PLAY_POLICY.md` — Google Play risk notes
- `docs/PRIVACY_SECURITY.md` — privacy/security model
- `docs/BRAND_ASO.md` — branding and store strategy
- `docs/QA_PLAN.md` — testing plan

## Status

**Phase: specification / pre-development**

Next milestone: scaffold the Android project and ship the first vertical slice:

`Dashboard → Storage Scan → Large Files → Review → Delete → Result`
