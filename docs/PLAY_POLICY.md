# Tooliva — Google Play / Android Policy Notes

Last reviewed: 2026-08-15

This is an engineering checklist, not legal advice.

## Target API

Project baseline:
- compileSdk 36
- targetSdk 36

From 2026-08-31 Google Play requires new apps and updates to target Android 16 / API 36.

Official references:
- https://support.google.com/googleplay/android-developer/answer/11926878
- https://developer.android.com/google/play/requirements/target-sdk

## All Files Access

`MANAGE_EXTERNAL_STORAGE` is restricted.

Tooliva MVP rule: **do not request it.**

Prefer:
- MediaStore
- Storage Access Framework
- user-selected directory access

Official:
- https://support.google.com/googleplay/android-developer/answer/10467955

If future functionality cannot work without broad access, stop and perform a fresh Play eligibility review before adding the permission.

## Accessibility API

Do not assume AccessibilityService is a free shortcut for App Lock.

Google Play requires disclosure/declaration for non-accessibility-tool use and restricts autonomous behavior.

Official:
- https://support.google.com/googleplay/android-developer/answer/10964491

Tooliva rule:
- prototype App Lock without AccessibilityService first;
- adding Accessibility requires explicit human approval and current policy review.

## Ads

Rules:
- no full-screen ad at app startup
- no ad before expected content
- no interstitial after every tap
- no ad triggered by Back/exit
- no ad that resembles a system warning
- no ads in Vault/PIN/biometric screens
- no ads on permission explanations
- no ads on destructive confirmation
- frequency-cap interstitials

Natural completion point example:
`cleanup completed → result visible → optional ad later`

The useful result must never be held hostage by an ad.

Official references:
- https://support.google.com/googleplay/android-developer/answer/9857753
- https://support.google.com/googleplay/android-developer/answer/12271244

## Package visibility

Avoid broad package visibility unless necessary and eligible.

Do not add `QUERY_ALL_PACKAGES` casually. App Lock must query only what is legitimately needed.

## User data

Notification history, file metadata and Vault content are sensitive from a user-trust perspective.

Rules:
- local processing by default
- prominent disclosure for special access
- no selling/sharing user content
- Data Safety form must match actual SDK behavior
- audit all SDKs before release

## Deletion

Use platform-approved, user-mediated delete/trash flows where required. Do not bypass scoped-storage protections.

## Claims

Do not market unsupported claims such as:
- “boost phone 300%”
- “cool CPU”
- “remove viruses” without a real antivirus engine
- invented battery-health percentages
- “10 GB junk” when referring to normal user files

Prefer factual claims:
- “Find large files”
- “Review old screenshots”
- “Find exact duplicate photos”
- “View battery temperature when your phone reports it”

## Pre-submission audit

Before every production release:
- inspect merged manifest
- review permissions/special access
- audit SDK data collection
- update Data Safety
- verify privacy policy
- test ad placement
- verify store claims against the build
- document App Lock go/no-go decision
