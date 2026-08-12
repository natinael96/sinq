# Sinq design system

Sinq's look was not redesigned — it was made consistent. This file records the
rules that hold it together, so the next screen doesn't invent a fifth card.

The principle underneath all of it: **hierarchy comes from spacing and type, not
from wrapping things in boxes.** Cards are for genuine grouping. A row of text
with air around it is usually the calmer answer, and calm is the point — this is
an app people open to pray.

## Where things live

| Concern | File |
|---|---|
| Spacing, radii, icon sizes, motion, brand colours | `ui/theme/Tokens.kt` |
| Material colour schemes, light and dark | `ui/theme/Theme.kt` |
| The Ethiopic type scale | `ui/theme/Type.kt` |
| Reader typography (optical size, leading, verse gap) | `ui/theme/ReadingTypography.kt` |
| Shared cards, rows, states, app bar | `ui/common/Components.kt` |
| Shared section/verse rendering for every reader | `ui/reading/SectionUi.kt` |

## Spacing

One scale, in `Spacing`: `xxs 2 · xs 4 · sm 8 · md 12 · lg 16 · xl 20 · xxl 28 ·
huge 40`. Page margins are `Spacing.screen` (24dp) on **every** screen, chrome
and reader alike. Don't write a bare `.dp` for a gap.

## Shape and size

`AgpeyaShapes`: extraSmall 6 · small 10 · medium 14 · large 20 · extraLarge 28.
Cards are `shapes.large`, pressable rows clip to `shapes.small`, tags to
`shapes.extraSmall`. Icons are `IconSize.small/medium/large` (18/22/26) — nothing
else. Any row you can tap is at least 48dp tall.

## Colour

The Material roles are **all** defined, in both schemes. That is not tidiness:
bottom sheets, dropdown menus, date pickers and chips paint themselves from
`surfaceContainer*`, `outline` and `error`, and anything left unset falls back to
Material's purple baseline.

Two rules:

1. **`primary` obeys Material, not the brand.** Switches, chips, sliders and
   progress bars tint themselves with `primary`, so in the dark scheme it has to
   be a *light* green or those controls disappear into the ground.
2. **The brand's dark-green hero surface is `sinqColors.hero`** (see
   `SinqColors`), identical in both themes — that constancy is what makes it read
   as the brand rather than as "the dark surface". Use it via `HeroCard`.

Gold (`secondary`) marks what matters: kickers, counts, verse numerals, streaks,
selection. On the ivory ground it is bronzed (`#7E5F1E`) rather than bright —
`#A67F2E` only reached 3.1:1, and gold carries small text all over the app.

`outlineVariant` is for hairlines: borders and dividers. `surfaceVariant` is a
*container fill* — a divider drawn in it reads as a bar.

Dark mode is designed on its own terms, not inverted. A near-black ground would
make ivory text glare at night; the deep green keeps page luminance low while
staying warm, and surfaces step up in small increments so hierarchy survives
without borders everywhere. Highlight tints are lighter and slightly more opaque
in the dark scheme, because the same tints go muddy over green.

## Typography

All fifteen Material roles are defined in Ethiopic. Three things are
non-negotiable for this script:

- the **bundled** face, never the device's;
- font padding **off** and the line box centred, or Noto's tall Ethiopic metrics
  add invisible leading that sets rows off-centre against their icons;
- **zero letter spacing** — Material's Latin scale tracks labels out by up to
  0.5sp, which visibly breaks Ethiopic syllables apart.

Reader text goes through `readingBodyStyle()` / `inReadingFont()`, which apply the
per-face optical correction (`opticalScale`) so 19sp looks like 19sp in Abay Light
and in Zemenay. Verse separation uses `readingVerseGap(fontSp)` — a fixed 4dp gap
vanishes at the 29sp step. Lines are capped at `ReadingMaxWidth` (640dp) so a
tablet reads like a book rather than a spreadsheet.

## Motion

`Motion.fast 130 · standard 220 · slow 300`, curves in `Easings`. Get the spec
from `LocalMotion.current.spec(...)` — **never** a bare `tween`. `LocalMotion`
reads the system animation scale, so at zero every duration collapses to nothing
and reduce-motion is honoured rather than approximated.

Motion is used to explain a change, never to decorate one: expand/collapse,
selection cross-fades, sheets, page transitions (a fade plus a twelfth-of-a-screen
drift). No parallax, no bounce, no scaling.

## The components

- `SinqCard` — the one card. Flat, hairline border, same padding. `accented =
  true` to draw the eye without changing its shape.
- `HeroCard` — the brand green. **One per screen**; a page with two has none.
- `SectionHeader` / `CollapsibleHeader` — the gold kicker that names a block.
- `ListRow` / `NavRow` / `ToggleRow` — every list row in the app.
- `SelectPill` — a selectable chip (ግጻዌ office, scripture chapter). Uses
  `selectable`, so selection reaches a screen reader instead of being carried by
  colour alone.
- `LoadingPanel` — waits 150ms before showing a spinner; content that resolves in
  40ms should look instant.
- `StatePanel` — empty, missing, failed. The body says what happened, whether the
  user's data is affected, and what to do next. Never a stack trace.
- `SinqTopBar` — every secondary screen's app bar.

## Accessibility

State is never carried by colour alone — pair it with a filled glyph, a label, or
a semantics role. Interactive rows use `toggleable`/`selectable` so the state is
announced, not just the tap. Everything is sized in `sp`/`dp` and wraps, so system
font scaling and long Amharic labels both survive.
