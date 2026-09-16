# Sinq screen structure and flows

Reviewed **2026-09-16** for **2.3.2 / 72** from `MainActivity.kt` and the Compose screens. These are structural wireframes, not pixel-accurate screenshots or a device visual-QA report. The original V1 Home/Search/Bookmarks/Streak/Settings tab layout is superseded.

## Application shell

```text
First launch → intro / setup → application shell

┌──────────────────────────────────────────────────────┐
│ Current screen: date, title, actions and content      │
│                                                      │
│ Reader / sheet / detail screens open from this shell │
├──────────────────────────────────────────────────────┤
│ Home          Journey          Library      Settings │
└──────────────────────────────────────────────────────┘
```

Search and marks are pushed destinations. `bookmarks` remains the route for the combined marks surface, preserving older navigation entry points.

## Home and Journey

Home presents today's context and prayer entry points, with access to the current hour and daily readings. Journey presents prayer/habit history, progress and management entry points. Supporting flows include reading plans, the prayer list, journal and preparation/record screens.

```text
Home → hour reader → contents / verse actions / reading preferences
     → Gitsawe → office or citation → Scripture passage
     → search → matching content

Journey → habits / history
        → reading plan → current day / book map / completion
        → personal record and preparation destinations
```

Exact card placement is defined in the Compose source; this map describes supported destinations rather than prescribing a second layout.

## Library

```text
Library
  ├─ Scripture hub → testament → book → chapter
  ├─ Psalter → Psalm → optional Ge'ez edition
  ├─ Wudase → daily / weekday pages → Amharic or Ge'ez
  ├─ Synaxarium → Ethiopian date → Amharic or Ge'ez
  ├─ Gitsawe / Sunday cycle / Bahre Hasab reference
  ├─ Church books → shelf/book → chapters and blocks
  └─ Mahlet → month pager / feast search / Tsige season
             → selected service → contents / source edition
```

Mahlet service chips open their own service, rather than always taking the first order. The Tsige route also includes relevant undated month-0 orders. Alternatives and editions remain visibly separate.

## Shared reading surface

```text
Back        Reading title / reference         Actions
──────────────────────────────────────────────────────
Section heading / rubric
Verse numeral   Reading text
Verse numeral   Reading text

Contents • type size/font • bookmark/verse actions
Copy/share → text or image cards
Citation → linked Scripture
Commentary → external Catena WebView (online)
```

Not every action applies to every corpus. Reader settings share six size steps, four selectable fonts, line-spacing/alignment preferences and keep-screen-on. Scroll/paged mode applies where implemented; do not assume every book uses the prayer-hour pager.

## Marks, journal and records

The marks surface combines bookmarks, highlights and passage-linked journal reflections. The journal can be browsed by day/month and optionally gated by a passphrase. Confession drafts have their own preparation flow and are excluded from backup.

Backup uses a selection dialog, optional journal passphrase check, system document picker and restore preview. The exported JSON is plaintext; the gate protects access to the action, not the resulting file.

## Settings and reminders

Settings is an overview with reading, fonts/copy, prayer, reminders and records destinations, plus help/about/licenses/changelog. The schedule editor and reminder modes share the configured prayer timing.

A fresh built-in mode enables the six daytime hours; Midnight and Veil start disabled. Alarm notifications offer actions including snooze. Availability and delivery depend on Android permissions/settings; device QA must exercise denied permissions and vendor battery restrictions.

## Accessibility and verification

Use shared [design-system](DESIGN_SYSTEM.md) components, selectable semantics for month/service controls and labeled icon actions. Check long Amharic titles, both themes, large system text and reduced motion on a device. Source inspection alone does not establish visual quality or screen-reader usability.
