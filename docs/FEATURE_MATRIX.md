# Tooliva — Feature Matrix

| Feature | Priority | Server | Sensitive access | V1 |
|---|---:|---:|---|---:|
| Storage overview | P0 | No | Media/storage APIs | Yes |
| Large files | P0 | No | Media/SAF | Yes |
| Screenshot cleaner | P0 | No | Media | Yes |
| Old videos | P1 | No | Media | Yes |
| Exact duplicates | P0/P1 | No | Media | Yes |
| Similar photos | P2 | No | Media | Later |
| Cleanup Swipe | P0 | No | Media delete | Yes |
| Downloads analyzer | P1 | No | SAF | Maybe |
| App Lock | P0 experimental | No | Usage/overlay special access | Conditional |
| Private Vault | P0/P1 | No | Files/biometric | Conditional |
| Notification History | P0 | No | Notification access | Yes |
| Phone Doctor | P0 | No | Device APIs | Yes |
| App Usage | P1 | No | Usage access | 1.0/1.1 |
| Hardware tests | P1 | No | Sensors/camera/mic | Partial |
| Image Compress | P1 | No | Media/SAF | Yes |
| Resize/Convert | P1 | No | Media/SAF | Yes |
| EXIF Privacy Clean | P0/P1 | No | Media | Yes |
| Images to PDF | P1 | No | Media/SAF | Yes |
| ZIP/unzip | P2 | No | SAF | Later |
| QR scanner | P1 | No | Camera | Yes |
| QR generator | P1 | No | None | Yes |
| Network info | P1 | No | Network state | Yes |
| Ping/DNS | P1 | No | Internet | Yes |
| Compass | P1 | No | Sensors | Yes |
| Level | P1 | No | Sensors | Yes |
| Flashlight | P1 | No | Camera/torch | Yes |
| Magnifier | P2 | No | Camera | Later |
| Phone Checkup | P0 | No | Aggregates enabled modules | Yes |

## High-risk feature flags

- `APP_LOCK_ENABLED`
- `VAULT_ENABLED`
- `ADS_INTERSTITIAL_ENABLED`

The codebase must allow risky features to be disabled for a Play build without destabilizing the rest of the application.
