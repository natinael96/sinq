# Sinq software requirements specification

Current baseline: **2.3.2 / 72**, reviewed **2026-09-16**. This replaces the original Tselot/V1 specification. [Project status](PROJECT_STATUS.md) distinguishes implementation evidence from release and human-review gates.

## Scope and users

Sinq serves Ethiopian Orthodox readers who need bundled prayer hours, Scripture, daily church readings, a liturgical library and private personal records. It supports Amharic and English interface text on Android **6.0/API 23 and newer**, targeting/compiling API 36.

The current scope includes calendars, fasting, both Synaxarium editions, Mahlet, reading plans, widgets, sharing, journal and backup. These are no longer “future” modules. No iOS client, account system or cloud synchronization is implemented in this repository.

## Functional requirements

| ID | Current requirement |
|---|---|
| FR-01 | Present eight built-in prayer hours and the 150-Psalm reader with stable section IDs and verse navigation. |
| FR-02 | Support scroll/paged prayer reading, remembered positions, keep-screen-on and content navigation. |
| FR-03 | Persist six reader size steps (16/18/20/22/25/28sp), four reader fonts, line spacing and alignment preferences. |
| FR-04 | Present the bundled Amharic 1980 Bible and Ge'ez Psalms, preserving source headings, verse identifiers and citation metadata. |
| FR-05 | Resolve daily Gitsawe, movable weekday offices and Sunday-cycle entries through the calendar logic; expose historical Bahre Hasab reference data. |
| FR-06 | Present Amharic/Ge'ez Synaxarium, daily/weekday Wudase prayers, church books and Mahlet orders. Preserve alternatives and source editions explicitly. |
| FR-07 | Support homophone-folded search in the implemented corpora and feast-name search in Mahlet; retain working reader targets for results. |
| FR-08 | Persist bookmarks and verse highlights; combine marks and passage-linked reflections without exposing confession-draft text. |
| FR-09 | Apply custom-hour and section-layout overlays without mutating bundled content; keep legacy saved routes usable. |
| FR-10 | Support reminder modes, enabled entries, times, weekdays, snooze and notification actions; handle OS permission and scheduling constraints. |
| FR-11 | Initialize the six daytime prayer entries enabled; initialize Midnight and Veil disabled. Respect existing saved schedules. |
| FR-12 | Provide Journey/habit tracking, prayer intentions, reading-plan progress and daily reading navigation with a completion flow. |
| FR-13 | Provide tithe/vow records, penance and confession/communion preparation, with sensitive records kept locally. |
| FR-14 | Provide day/month journal browsing and an optional passphrase gate. Do not represent this gate as database encryption. |
| FR-15 | Export selected records through the system file picker, preview imports, preserve supported older backup formats and exclude confession drafts. Journal export is opt-in plaintext. |
| FR-16 | Share text or image cards and expose the Gitsawe/memento widgets. |
| FR-17 | Support localized interface chrome, light/dark theme, onboarding, help, source notices and release notes. |
| FR-18 | In builds with `UPDATE_NOTICE`, check GitHub release metadata at launch and link to the release page without installing APKs. Disable that path in default/Play builds. |
| FR-19 | Open external Catena commentary in its own WebView with the Early Fathers preference; distinguish network content from bundled readings. |

## Nonfunctional requirements

- **Offline reading:** all bundled corpora and core personal-record functions must remain usable without connectivity. Update checking and external commentary are network features; the manifest declares `INTERNET`.
- **Privacy:** no application account, cloud sync, ad SDK or analytics SDK. User-controlled exports and third-party web requests must not be described as “nothing ever leaves the device.” Android automatic backup is disabled.
- **Integrity:** do not rewrite sacred text or rename shipped IDs casually. Keep source provenance, transformation rules, review records and notices alongside generated data.
- **Compatibility:** minimum SDK 23, with core-library desugaring; permanent application identity `com.sinq.app`.
- **Accessibility:** shared components should provide 48dp touch targets, readable contrast, state semantics, scalable text and reduced-motion support. Device verification is required before claiming complete coverage.
- **Reliability:** reminder scheduling must handle reboot, time/timezone changes and permission restrictions; UI must explain missing content and failed operations without implying data loss when none occurred.
- **Performance:** content work belongs off the main thread where appropriate; use repository caching and avoid whole-journal serialization for normal edits. Low-end-device startup and scrolling targets require measurements, not assumptions.
- **Maintainability:** versioned content, Kotlin models, centralized strings/tokens, exported Room schema and reproducible release checks.

## Data contracts

Read-only data is bundled under `app/src/main/assets/content/`. Small user state is held in Preferences DataStore; journal entries use Room schema version 1. Backup format version 2 has defaulted backward-compatible fields and a 2 MiB import limit. [Content structure](CONTENT_STRUCTURE.md) documents stable identifiers and corpus boundaries.

## Acceptance and remaining approval

For a release, validate content, run JVM tests and lint, build artifacts, then record device checks for API 23 and newer Android permission behavior, restoration, reminders, widgets, font scaling and navigation. Review text and calendar rules against named church sources. A passing test suite is not liturgical approval, store publication or an accessibility certification.

See [SDS](SDS.md) for the design and [project status](PROJECT_STATUS.md) for current results and open work.
