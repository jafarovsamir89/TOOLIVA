# Tooliva — Privacy & Security Model

## Product promise

> User content stays on the user's device by default.

## Local-only sensitive data

- Vault files
- Vault metadata
- Notification History
- file/media inventory
- duplicate fingerprints
- App Lock settings
- usage summaries

Do not transmit these to analytics or ad systems.

## Operational analytics

Allowed only when privacy-safe:
- feature opened
- scan completed
- duration bucket
- item-count bucket
- error code
- app version
- coarse device/API compatibility data

Never include filenames, paths or contents.

## Vault threat model

Designed to reduce casual/local unauthorized access on a normally secured Android device.

Not promised:
- protection against a rooted/fully compromised device
- forensic-grade adversaries
- recovery after uninstall/device loss without explicit backup/export

## Vault encryption

Recommended:
- AES-GCM authenticated encryption
- master key protected by Android Keystore
- per-file unique nonce
- versioned encrypted container
- integrity validation before source deletion

Import sequence:
1. read source;
2. encrypt to temporary destination;
3. close/sync;
4. verify decrypt/integrity;
5. commit encrypted metadata;
6. only then offer source deletion.

Never reuse a nonce with the same key.

## App Lock

PIN:
- never stored plaintext
- salted slow KDF/verifier

Biometric:
- AndroidX/System BiometricPrompt

Do not claim App Lock protects third-party data from root/system-level attackers.

## Notification History

- opt-in only
- prominent disclosure
- local persistence
- configurable retention
- per-app exclusion
- clear-all action
- no content in analytics/logging

## Logging

Production logs must never include:
- file names/paths
- QR contents
- notification text
- PIN/biometric secrets
- Vault item information that reveals content

## Android backup

Review Auto Backup behavior explicitly.

Vault data must never accidentally be backed up in plaintext. Users must understand that local-only encrypted Vault content may be lost when app data is deleted/uninstalled unless an explicit export/backup feature is later provided.

## Screen privacy

Evaluate secure-window/recents protections for:
- Vault
- PIN setup/unlock

Normal utility screens should remain shareable/screenshottable.

## QR safety

Never automatically:
- open URLs
- dial phone numbers
- send SMS/email
- join networks

Parse, display, then require user action.

## Ads

Ad SDKs must never receive:
- filenames
- notification content
- Vault data
- scan contents

Sensitive screens contain no ads.

## Security release checklist

- encryption round-trip tests
- corrupted Vault file tests
- process death during import
- insufficient-space import
- source deletion only after verified copy
- PIN brute-force/throttling review
- exported temp-file cleanup
- backup behavior review
- production-log audit
- dependency/security review
