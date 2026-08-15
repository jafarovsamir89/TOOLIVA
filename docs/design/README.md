# Tooliva Design Reference

These images are the visual source of truth for the first production UI.

## Files

- `tooliva-ui-showcase.jpg` — product-level visual direction and key screens.
- `tooliva-ui-system.jpg` — component/style system for implementation.

## Implementation priority

1. Match the overall hierarchy, spacing, density and dark visual language.
2. Use Material 3 components where they can reproduce the design cleanly.
3. Preserve accessibility, responsive layout and Android platform conventions over pixel-perfect copying.
4. Do not invent a different visual system unless a human explicitly approves it.
5. If a generated reference contains impossible or misleading data, keep the visual treatment but implement truthful Android behavior.

## Core style

- dark graphite background and surfaces
- teal/cyan primary actions
- blue secondary accents
- large rounded cards (roughly 16–24dp)
- 4dp spacing grid
- minimum 48dp touch targets
- high information clarity, not decorative clutter
- restrained use of red; red is for real destructive/error states only
- no fake alarm or fake health UI

## Key screens to match first

### Home
- Tooliva title and concise subtitle
- storage ring/stat card
- battery and thermal status cards
- prominent `Check My Phone` CTA
- module grid
- bottom navigation

### Clean
- storage overview
- truthful explanatory text
- list of cleanup tools with measured totals

### Large Files
- filter chips
- file list with selection state
- thumbnail/type/size
- selected-size total
- bottom delete/trash CTA

### Notification History
- grouped notifications
- clear app source and timestamps
- optional locally-derived noise insights

## Important

The design reference is aspirational. The engineering rules in `AGENTS.md`, `TECH_SPEC.md`, `docs/PLAY_POLICY.md` and `docs/PRIVACY_SECURITY.md` always override any visual detail that would require unsafe behavior, unsupported data, deceptive claims or policy violations.
