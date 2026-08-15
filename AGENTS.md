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
- visual consistency with the approved Tooliva design reference

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
16. Do not redesign Tooliva from scratch. Use the approved design references in `docs/design/` as the visual source of truth.
17. If a visual reference conflicts with Android behavior, accessibility, security, truthfulness or Play policy, preserve the visual intent but follow platform/policy rules.
18. Do not mark placeholders, mock values or fake scan results as completed functionality.

## Visual source of truth

Before implementing or modifying UI, inspect:

- `docs/design/tooliva-ui-showcase.jpg`
- `docs/design/tooliva-ui-system.jpg`
- `docs/design/README.md`

The agent must reproduce the approved visual language rather than inventing a new one:

- dark graphite background/surfaces
- teal/cyan for primary actions
- restrained blue/green/orange secondary accents
- large rounded Material 3 cards
- strong hierarchy and readable numeric data
- 4dp spacing grid
- minimum 48dp touch targets
- dark theme first, light theme supported
- no fake danger colors or manipulative warnings

The generated images are a design target, not permission to fake functionality. Example values shown in mockups must be replaced by real local Android data.

## Before coding a task

Read:
- `README.md`
- `TECH_SPEC.md`
- relevant section in `TODO.md`
- `docs/PLAY_POLICY.md`
- `docs/PRIVACY_SECURITY.md`
- `docs/design/README.md`
- both images under `docs/design/` for any UI task

Then:
1. inspect existing implementation;
2. identify the exact TODO item(s) being worked on;
3. state the smallest safe change;
4. implement one coherent vertical slice;
5. add/update tests;
6. run build/tests;
7. inspect the final UI against the design reference;
8. update TODO only for actually completed and verified work;
9. report what changed, what was tested, and what remains.

Do not mark a feature complete because UI exists if the functional path is not verified.

## Task execution discipline

- Work from P0 to P1 to P2 unless a human explicitly reprioritizes.
- Finish the current vertical slice before starting another major module.
- Avoid broad refactors while implementing an unrelated feature.
- Do not create speculative abstractions with no current use.
- Keep commits focused and descriptive.
- Never silently downgrade an explicit requirement; document blockers instead.
- If Android/OEM behavior is uncertain, build the smallest prototype and test it rather than guessing.
- When a feature requires special access/permission, implement a truthful explanation screen and denial fallback.

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
- loading, empty, error and permission-denied states are mandatory
- UI must remain usable on common phone widths and large font settings
- do not hardcode mock values from the reference images

## Storage scanning

- never scan on main thread
- support cancellation
- emit progress
- cache expensive duplicate fingerprints
- handle media disappearing during scan
- handle permission changes
- test large libraries
- show only files the app can genuinely access
- never label normal user files as junk without clear category/review context

## Destructive operations

- centralize deletion/trash logic
- show selected count and selected bytes before destructive action
- prefer Android user-mediated trash/delete flows where required
- verify results after the system action returns
- handle partial success/failure
- never claim space was freed until verified/recomputed

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

## Testing minimum

For every feature where applicable, verify:
- happy path
- permission denied
- permission revoked after grant
- empty data
- large data set
- process recreation/basic state restoration
- Android version differences
- no main-thread blocking
- no unexpected network use

For destructive/security features, add focused tests and manual validation notes.

## Definition of done

A task is done only when:
- implementation works end-to-end
- loading/error/empty state exists
- permission-denied path works
- tests are added where appropriate
- build passes
- no new policy/privacy problem was introduced
- UI is checked against `docs/design/`
- no mock/fake data remains in the production path
- docs updated if behavior changed
- TODO is updated honestly
