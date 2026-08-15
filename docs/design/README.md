# Tooliva Design Reference

These images are the visual source of truth for the first production UI.

## Files

- `tooliva-ui-showcase.webp` — product-level visual direction and key screens.
- `tooliva-ui-system.webp` — component/style system for implementation.

The WebP files are intentionally optimized previews so they remain lightweight inside the repository. They are sufficient for layout, hierarchy, color, component and spacing reference. The written rules in this file are authoritative where small text in the image is difficult to read.

## Implementation priority

1. Match the overall hierarchy, spacing, density and dark visual language.
2. Use Material 3 components where they can reproduce the design cleanly.
3. Preserve accessibility, responsive layout and Android platform conventions over pixel-perfect copying.
4. Do not invent a different visual system unless a human explicitly approves it.
5. If a generated reference contains impossible or misleading data, keep the visual treatment but implement truthful Android behavior.

## Core style

- background: near-black graphite (`#0B0F14` direction)
- surfaces: dark slate (`#141A22` direction)
- primary action: teal/cyan (`#00D4E8` direction)
- secondary accent: blue (`#3B82F6` direction)
- success: green, warning: amber; use red only for real destructive/error states
- large rounded cards, normally 16–24dp radius
- 4dp spacing grid
- minimum 48dp touch targets
- readable high-contrast typography
- high information clarity, not decorative clutter
- no fake alarm or fake health UI

## Key screens to match first

### Home
- Tooliva title and concise subtitle
- storage ring/stat card using real device values
- battery and thermal status cards using real values or `Unavailable`
- prominent `Check My Phone` CTA
- module grid
- bottom navigation

### Clean
- storage overview
- truthful explanatory text
- list of cleanup tools with measured totals when available
- never claim normal user files are automatically safe to delete

### Large Files
- category/filter chips
- file list with selection state
- thumbnail/type/size
- selected item count and selected-size total
- bottom delete/trash CTA
- user-mediated Android deletion/trash confirmation

### Notification History
- grouped notifications
- clear app source and timestamps
- optional locally-derived noise insights
- never upload notification content

## Component rules

- Primary CTA: teal/cyan filled, strong contrast, 48dp+ height.
- Secondary CTA: surface/outlined treatment.
- Tool cards: one clear icon, title, short description, optional measured value, chevron when navigable.
- Stat cards: large number/value first; label second.
- Lists: avoid oversized rows; maintain scannability.
- Destructive CTA: clearly identified as delete/trash; do not disguise it as a normal primary action.
- Permission card: explain the exact benefit before opening the Android permission/special-access UI.

## Important

The design reference is aspirational. The engineering rules in `AGENTS.md`, `TECH_SPEC.md`, `docs/PLAY_POLICY.md` and `docs/PRIVACY_SECURITY.md` always override any visual detail that would require unsafe behavior, unsupported data, deceptive claims or policy violations.
