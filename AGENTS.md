# Tooliva — Instructions for AI Coding Agents

This file is authoritative for Codex/Claude/Gemini or other coding agents working in this repository.

## Mission

Build a production-quality, policy-safe Android utility.

Optimize for:
- reliability
- privacy
- truthful behavior
- fast UX
- maintainable code
- Google Play compliance

## Hard rules

1. Kotlin only unless an external library requires otherwise.
2. Jetpack Compose for app UI.
3. Target Android API 36.
4. Never add `MANAGE_EXTERNAL_STORAGE` without an explicit human decision recorded in the repository.
5. Never add AccessibilityService to implement App Lock without explicit human approval.
6. Never add `QUERY_ALL_PACKAGES` casually.
7. Never add a permission “just in case.”
8. Never delete a user file without explicit user action/confirmation.
9. Never implement fake RAM boost, CPU cooling, virus warning or invented health score.
10. Never upload user files, notification text or vault content to a server.
11. Never log filenames, notification content or secrets in production.
12. Never add an ad SDK directly inside a feature package.
13. Never show ads in Vault, biometric/PIN, permission or destructive confirmation screens.
14. Do not create a backend unless the task explicitly requires one.
15. Core utilities must keep working offline.

## Before coding a task

Read:
- `README.md`
- `TECH_SPEC.md`
- relevant section in `TODO.md`
- `docs/PLAY_POLICY.md`
- `docs/PRIVACY_SECURITY.md`

Then:
1. inspect existing implementation;
2. state the smallest safe change;
3. implement;
4. add/update tests;
5. run build/tests;
6. update TODO only for actually completed work.

Do not mark a feature complete because UI exists if the functional path is not verified.

## Architecture

Prefer:
- package-by-feature
- unidirectional data flow
- immutable UI state
- repository abstractions for Android/platform APIs
- coroutines/Flow
- centralized permission handling
- centralized deletion handling
- centralized ads/billing

Avoid:
- god classes
- global mutable state
- business logic in Composables
- direct database access from UI
- direct ad SDK calls from screen Composables

## UI

- Material 3
- dark/light
- accessible touch targets
- no fake alarm UI
- destructive actions visually distinct but not manipulative
- show `unsupported` rather than invent data

## Storage scanning

- never scan on main thread
- support cancellation
- emit progress
- cache expensive duplicate fingerprints
- handle media disappearing during scan
- handle permission changes
- test large libraries

## Security

Vault:
- versioned encryption format
- authenticated encryption
- Keystore-backed master secret
- verify encrypted write before offering source deletion
- re-auth before export

PIN:
- never plaintext
- use salted KDF/verifier

Any cryptographic design change requires updating `docs/PRIVACY_SECURITY.md`.

## App Lock

App Lock is a risk-isolated feature.

Do not “solve” reliability by secretly introducing:
- Accessibility abuse
- device admin misuse
- aggressive background behavior

If platform restrictions make requested behavior unreliable, document the limitation instead of hiding it.

## Dependencies

Do not add libraries without checking:
- license
- maintenance
- data collection
- manifest permissions
- transitive SDKs
- binary size

Prefer AndroidX and standard platform APIs.

## Definition of done

A task is done only when:
- implementation works
- loading/error/empty state exists
- permission-denied path works
- tests are added where appropriate
- build passes
- no new policy/privacy problem was introduced
- docs updated if behavior changed
