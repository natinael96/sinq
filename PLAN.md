# Sinq (ስንቅ) Android App — Historical Master Plan

> **Status:** archived planning record. This document describes the original V1 scope and
> architecture and is no longer the current product contract. For shipped behavior use
> `README.md`, `docs/IMPLEMENTATION.md`, and the source/tests. Items marked “future” below may
> already be implemented.

> Ethiopian Orthodox Hours of Prayer app, fully in Amharic.
> Solo developer learning Kotlin + Android while building.
> Target: V1 on Google Play in ~12 weeks.
> This document is retained for decision history, not as the current source of truth.

> **Renamed 2026-07-16:** the app is now **Sinq (ስንቅ — "provisions for the journey")**; repo:
> [github.com/natinael96/---sinq](https://github.com/natinael96/---sinq). Where "Agpeya" appears
> below it refers to the prayer book itself (and the built-in reminder mode named after it), not
> the app. The `com.agpeya.app` applicationId is a permanent contract and does not change.

---

# PART A — PRODUCT DEFINITION

## 1. Vision, Users, Scope

### 1.1 Vision statement
A fully offline, reverent, distraction-free Amharic Agpeya that an Ethiopian Orthodox believer opens 1–7 times a day to pray the canonical hours — with the text always available, easy to read, easy to find, and with gentle reminders at the times they choose.

### 1.2 Target users (design for these three people)
| Persona | Description | What they need most |
|---|---|---|
| **The daily prayer** | Prays 2–4 hours daily, knows the structure by heart | Fast access (≤2 taps to text), remembers position, large readable text |
| **The learner** | Young person or new convert learning the Agpeya | Clear structure, section navigation, search ("where is Psalm 50?") |
| **The elder** | Older user, possibly low vision, low-end phone | Very large font option, high contrast, simple navigation, works on cheap/old devices |

### 1.3 Scope contract — V1
**IN:** 7–8 prayer hours complete in Amharic · offline always · section-level navigation · search (homophone-aware) · bookmarks · recents · font size · light/dark · keep-screen-on · **Prayer Modes reminder system (§2.7): built-in Agpeya mode + user-created custom modes** · about/credits page.

**OUT (frozen — write the version they move to):** audio (V1.1) · Ethiopian calendar & fasting periods (V1.2) · feast days & Synaxarium (V2) · Ge'ez or English texts (V2) · widgets (V2) · tablet layout (V1.1 polish) · sharing verses as images (V1.1) · any account/cloud feature (never, ideally).

**Quality bar:** zero network permission in the manifest. The Data Safety form reads "No data collected." This is a feature — advertise it.

### 1.4 Success criteria for V1 (measurable)
- Installs and runs correctly on a 2018-era low-end phone (≤2 GB RAM, Android 8).
- Cold start to Home in ≤ 2 seconds on that device.
- Any prayer text reachable in ≤ 2 taps from launch.
- A reminder set for 6:00 fires daily within a few minutes, including after reboot.
- A fluent reviewer signs off every hour's text as accurate.
- Published on Google Play with ≥ 4.5 rating goal after first 50 reviews.

---

## 2. Content Specification (the real product)

> **SCOPE CHANGE (2026-06-12):** V1 content = **psalms + gospel reading per hour only**, extracted
> programmatically from the open-source [80-weahadu](https://github.com/EOTCOpenSource/80-weahadu)
> Amharic Bible (JSON, LXX psalm numbering — verified compatible). Litanies, absolutions, creed
> blocks etc. are dropped from V1 (revisit in V1.1+ if a licensed source appears). This eliminates
> §2.3's 30–50h manual-entry estimate and most of §2.4's rights workflow — the one open rights item
> is that 80-weahadu has **no license file** (owner contact pending, see docs/PHASE1_CHECKLIST.md).
> The hour→psalms/gospel mapping lives in `content/hour_mapping.json`; §§2.2–2.5 below are kept for
> reference but superseded where they describe manual entry of non-biblical sections.

### 2.1 The hours — final list to confirm
| # | Amharic | Transliteration | Western name | Traditional time | Default reminder |
|---|---|---|---|---|---|
| 1 | ጸሎተ ነግህ | Tselote Negh | Prime / Morning | Sunrise | 6:00 |
| 2 | ጸሎተ ሠለስት | Tselote Selest | Terce / Third Hour | 9 AM | 9:00 |
| 3 | ጸሎተ ቀትር | Tselote Ketr | Sext / Sixth Hour | Noon | 12:00 |
| 4 | ጸሎተ ተሰዓት | Tselote Tese'at | None / Ninth Hour | 3 PM | 15:00 |
| 5 | ጸሎተ ሰርክ | Tselote Serk | Vespers / Eleventh | Sunset | 18:00 |
| 6 | ጸሎተ ንዋም | Tselote Nwam | Compline / Twelfth | Before sleep | 21:00 |
| 7 | ጸሎተ መንፈቀ ሌሊት | Menfeqe Lelit | Midnight | Midnight | off by default |
| 8 | ጸሎተ ሥውር (decide) | Tselote Sewur | Prayer of the Veil | After Compline | off by default |

**Open decisions (answer in week 0, record the answer here):**
- D1: Include the Veil prayer? → Recommend **yes**, with a one-line note that it is traditionally monastic/clerical.
- D2: Midnight Prayer's three watches (ነዋ/መንፈቀ structure): one hour entry containing three top-level parts, or three entries on Home? → Recommend **one entry, three collapsible parts** — keeps Home to 8 cards.
- D3: Exact Amharic naming convention (some editions use different titles) — copy from your approved source edition verbatim.

### 2.2 Canonical section skeleton (applies to every hour)
Every hour decomposes into ordered sections. Define **section types** now — they drive icons, search filters, and future audio chaptering:

1. `opening` — Thanksgiving, Lord's Prayer, Psalm 50 intro block
2. `psalm` — one section per psalm (don't merge psalms; users navigate to specific ones)
3. `gospel` — the hour's Gospel reading
4. `litany` — the short petitions/Kyrie blocks
5. `creed` — Trisagion, Hail Mary, Creed block
6. `absolution` — the hour's absolution prayer
7. `closing` — Lord have mercy (41×), final prayer

**Per-section data captured during content entry:**
- Hour it belongs to, position/order within the hour
- Type (above), title in Amharic (e.g., መዝሙር ፷፪), optional subtitle (e.g., "Psalm 62 / የዳዊት መዝሙር")
- Body text (Amharic, with paragraph breaks preserved)
- Optional rubric/instruction line (e.g., "said three times", "kneeling") — styled differently from prayer text
- Reference field for psalms/gospels (book + chapter:verse) — invisible plumbing now, enables features later

### 2.3 Content size estimate (plan the typing time)
- Morning Prayer alone ≈ 19 psalms + gospel + litanies → roughly 40–60 sections, 15–25k Amharic characters.
- Whole Agpeya ≈ **200–300 sections, 80–120k characters**.
- Realistic entry speed incl. checking: ~2–4k characters/hour of work → **30–50 hours of content work**. This is the single largest task in the project. Schedule it as ~1 hour/day across weeks 0–6, or recruit a helper.

### 2.4 Sourcing & rights workflow (week 0, blocking)
1. Choose source edition (a specific printed Agpeya, a church-published digital text, or a verified public-domain text).
2. Determine rights: public domain? Church permission needed? Publisher permission? → **Get it in writing** (an email is fine). Record in `CONTENT_RIGHTS.md`.
3. If no clean source exists: transcribe from a printed edition the church blesses, and have clergy review — note this in the app's About page ("Text reviewed by …" adds user trust).
4. Decide numerals convention: Ge'ez numerals (፩፪፫) vs Arabic (1 2 3) for psalm numbers — follow the source edition; be consistent everywhere.

### 2.5 Content production pipeline (repeatable, trackable)
- **Master tracking sheet** (Google Sheets/Excel): one row per section → columns: hour, order, type, title, status (`empty → drafted → self-checked → reviewer-approved`), notes.
- **Entry format:** one structured text/JSON file per hour in the project repo. Content lives in version control from day one — every typo fix is a tracked change.
- **Proofreading protocol:** (a) self-check against source line by line; (b) second pass by fluent reviewer reading *on a phone screen* (catches rendering + typos); (c) mark approved in sheet. No hour ships below `reviewer-approved`.
- **Encoding rules:** UTF-8 everywhere; watch for visually identical but different codepoints; no Latin punctuation inside Amharic text unless the source uses it (use ።፣፤፥ correctly); normalize whitespace.
- **Content version number** baked into the content files; the app re-imports when it increases (see §7.2).

### 2.6 Typography & rendering requirements
- Bundle **Noto Sans Ethiopic** (and test Noto Serif Ethiopic as a reading option — serif often feels more "prayer book"). Never rely on device system fonts for Ethiopic.
- Font sizes: 5 steps, from ~16sp to ~28sp body text; elder persona must comfortably read at max.
- Line height ≈ 1.6–1.8× for Ethiopic body text; paragraph spacing distinct from line breaks.
- Test glyph coverage on a real device early — emulators can render Ethiopic that real cheap devices won't (hence bundling the font).

### 2.7 Prayer Modes — the reminder model (core concept)

Reminders are not a flat per-hour list; they are organized into **modes**. A mode is a named prayer schedule: a set of reminder entries, where each entry = *which prayer* + *what time* + *which days*.

**Built-in mode — "አግፔያ / Agpeya (Classic)":**
- Ships with the app: all 7–8 hours at their traditional times (§2.1 table).
- User can toggle individual entries on/off and adjust times, but cannot delete the mode or its entries. A "Reset to traditional times" action restores defaults.
- This is the active mode out of the box (with all entries off until the user enables them — never notify without consent).

**Custom modes — user-created:**
- User creates a mode, names it (e.g., "የሥራ ቀን" / "Workdays", "ጾም" / "Fasting season", "Mornings only").
- Adds any number of reminder entries. Each entry:
  - **Prayer target:** one of the hours (V1). (Targeting a specific *section* or a bookmark = V1.1 — keep the data model ready for it.)
  - **Time:** any time of day, user's choice.
  - **Days:** daily, or specific weekdays (e.g., Wed/Fri only — natural fit for fasting practice). Decide D11 below; recommend **weekday selection in V1** since it's the most requested customization for prayer apps.
  - **Enabled** toggle per entry.
- The same hour may appear in multiple entries (e.g., Compline at 21:00 on weekdays and 22:00 on weekends).
- Modes can be **duplicated** ("Duplicate Agpeya mode → customize") — the expected way most users will start a custom mode. Custom modes can be renamed and deleted (delete asks for confirmation).

**Activation model (decide D12):**
- Recommended: **exactly one active mode at a time.** Switching modes atomically replaces the scheduled alarms. Simple to understand ("which schedule am I on?"), simple to implement, matches the word "mode."
- Rejected alternative: multiple simultaneous modes (union of all entries) — more flexible but confusing (duplicate notifications, unclear ownership) and harder to debug. Revisit only if users ask.

**Practical limits (guardrails, not features):** max ~20 modes, max ~24 entries per mode — prevents pathological alarm counts; show a friendly message at the limit.

**Sound:** per-mode sound choice (default + 1–2 bundled); entries inherit the mode's sound. Per-entry sound = V1.1 if requested.

---

## 3. UX Specification — screen by screen

### 3.1 Navigation map
```
Home ──→ Prayer Reading (hourId)
  │            └──→ (in-screen) section jump sheet
  ├──→ Search ──→ Prayer Reading (hourId, scroll to sectionId)
  ├──→ Bookmarks ──→ Prayer Reading (hourId, scroll to sectionId)
  ├──→ Prayer Modes (list) ──→ Mode Editor (modeId) ──→ Entry Editor (sheet/dialog)
  └──→ Settings ──→ About / Credits
Notification tap ──→ Prayer Reading (hourId)   [deep link]
```
Bottom navigation bar with 4 destinations: **Home · Search · Bookmarks · Settings**. Prayer Modes is reached from a card on Home showing the active mode ("Active: አግፔያ Classic") and from a row in Settings (D4 resolved by the modes pivot).

### 3.2 Home screen
- **Header:** app name in Amharic; optionally today's date (Gregorian only in V1; Ethiopian date is V1.2).
- **"Now" card (hero):** the hour matching the current time of day ("It is morning — ጸሎተ ነግህ"), one tap to open. Logic: simple local-time bands matching §2.1 table. This is the most-used button in the app.
- **Hours list:** all 7–8 hours as cards in canonical order — Amharic name, small type/time hint, subtle icon. Visually calm; no thumbnails/photos.
- **Recents row:** "Continue" chips for last 3 opened hours (hide row if empty).
- **Empty/first-launch state:** none needed — content is bundled; first launch shows a one-time 2–3 screen gentle intro (what the Agpeya is, enable reminders?, choose theme) — skippable.

### 3.3 Prayer Reading screen (the core — spend 40% of design effort here)
- **Layout:** single scrollable column of sections. Section header (title + subtitle) visually distinct; rubrics in smaller italic-style accent color; body text in the reading font.
- **Top bar:** hour name; overflow contains font-size control if not inline. Auto-hide on scroll down, reappear on scroll up (maximizes text area).
- **Section jump:** a "contents" button opens a bottom sheet listing all sections (grouped: Opening / Psalms / Gospel / Litanies / Closing); tap scrolls to section. The learner persona depends on this.
- **Bookmark:** each section header has a subtle bookmark toggle. (Bookmark = section, not scroll offset.)
- **Font size:** A−/A+ control (persisted globally). Apply live without losing scroll position.
- **Keep screen on** while this screen is visible (if enabled in settings).
- **Scroll position memory:** on reopen of the same hour within the same day, offer/restore last position ("Continue from መዝሙር ፷፪?" — small snackbar, not a modal). If too fiddly, V1 fallback: always restore silently per-hour.
- **Reading progress:** thin progress indicator (% of hour scrolled) — optional, cut first if time is short.
- **What this screen deliberately does NOT have:** share buttons everywhere, animations, images, related-content suggestions. Sacred text, minimal chrome.

### 3.4 Search screen
- Search field autofocuses; results update as you type (debounced).
- **Normalization (critical for Amharic):** index and query are both folded: ሀ/ሐ/ኀ/ኸ→ሀ-class, ሰ/ሠ→ሰ-class, አ/ዐ→አ-class, ጸ/ፀ→ጸ-class (apply across each character's full vowel series). Document the exact fold table in `SEARCH_NORMALIZATION.md` during Phase 5 and unit-test it.
- **Result row:** hour name → section title → 1–2 line snippet with match highlighted. Tap → reading screen scrolled to that section.
- Empty states: before typing → recent searches (local only) or a hint; no results → "Not found — try different spelling" in Amharic.
- Scope: searches titles + body of all sections. No filters in V1 (type filter chips = V1.1).

### 3.5 Bookmarks screen
- Grouped by hour, in canonical hour order; within hour, by section order.
- Row: section title + hour name + first line of text. Tap → jump to section. Swipe or icon → remove (with undo snackbar).
- Empty state: friendly illustration-free message explaining the bookmark icon on the reading screen.

### 3.6 Prayer Modes screens (three levels)

**3.6.1 Modes list screen**
- The built-in Agpeya mode pinned at top with a "built-in" badge; custom modes below, most recently used first.
- Each mode card: name · entry count summary ("5 reminders · daily") · **radio-style "Active" selector** (exactly one active, per D12). Selecting a mode immediately swaps the alarm schedule, with a snackbar confirming ("Switched to የሥራ ቀን").
- "＋ New mode" actions: **"Start from Agpeya"** (duplicate built-in, then edit) and "Start empty". Overflow per custom mode: rename, duplicate, delete (confirm dialog: "Deletes 5 reminders").
- Footer help row: "Reminders not firing?" → §8.6 battery guidance.

**3.6.2 Mode editor screen**
- Header: mode name (editable for custom; fixed + "Reset to traditional times" action for built-in).
- Mode sound picker (applies to all entries in the mode).
- Entry list, sorted by time: each row = prayer name · time · day chips (ዕለታዊ/daily, or Mo Tu We…) · enabled switch.
- Built-in mode: rows can be toggled and re-timed but not deleted/added; custom modes: full add/edit/delete with swipe-to-delete + undo.
- "＋ Add reminder" → entry editor. Enabling the first entry anywhere triggers the notification-permission flow (§8.4).

**3.6.3 Entry editor (bottom sheet)**
- Prayer picker: the 7–8 hours as a simple list (section/bookmark targets reserved for V1.1).
- Time picker (system Material picker), defaulting to the hour's traditional time when that hour is picked.
- Day-of-week selector: "Every day" toggle, or individual weekday chips (D11).
- Save validates: at least one day selected; duplicate (same prayer + same time + overlapping days) warned but allowed.

### 3.7 Settings screen
- Theme: System / Light / Dark.
- Reading font: Sans / Serif (if both bundled).
- Font size (same control as reading screen).
- Keep screen on while reading: toggle.
- Reminders → §3.6 page. Sound picker if not on reminders page.
- About: app version, content source & reviewer credits, content version, privacy policy link, feedback mailto link, license notices (fonts!).

### 3.8 Visual design language
- Material 3, but muted: deep liturgical palette (e.g., deep purple/indigo or warm parchment accents — decide D5 with 2–3 mockups), high contrast text.
- Dark theme = true comfortable night reading (not just inverted) — Compline & Midnight are prayed in the dark; test dark theme at max font in a dark room.
- One accent color; no gradients, no stock imagery. Iconography: simple line icons; consider a small Ethiopian cross motif for the app icon and section dividers.
- Dynamic color (Material You): optional; recommend **off** to keep brand consistency — decide D6.

---

# PART B — ENGINEERING PLAN (no code, all decisions)

## 4. Architecture & Technical Decisions

### 4.1 Locked decisions table
| ID | Decision | Choice | Why |
|---|---|---|---|
| T1 | Language / UI | Kotlin + Jetpack Compose, Material 3 | Modern default; best learning investment |
| T2 | Min SDK | 26 (Android 8.0) | Covers elder-persona devices; sane notification APIs |
| T3 | Target SDK | Latest required by Play at release time | Play requirement |
| T4 | Modules | Single `app` module | Solo dev; multi-module is ceremony with no payoff here |
| T5 | Pattern | MVVM-lite: Screen → ViewModel(StateFlow) → Repository → Room/DataStore | One ViewModel per screen, unidirectional data flow |
| T6 | DI | None or manual (a simple AppContainer) — **not** Hilt in V1 | One less thing to learn; refactor to Hilt later if wanted |
| T7 | Persistence | Room (content, bookmarks, recents) + DataStore Preferences (settings, reminder configs) | Standard, well-documented |
| T8 | Content shipping | JSON assets bundled in APK → imported into Room on first launch / content-version bump | Texts version-controlled & human-editable; Room gives FTS + fast queries |
| T9 | Search | Room FTS4 over normalized text column | Built-in, offline, fast |
| T10 | Scheduling | AlarmManager, **inexact** repeating daily; reschedule on boot/update | Avoids exact-alarm permission & Play policy review |
| T11 | Navigation | Navigation Compose, single Activity | Standard |
| T12 | Image/network libs | **None.** No INTERNET permission | Privacy as feature; smaller app |
| T13 | Crash reporting | None in V1 (rely on Play Console vitals) | Keeps "no data collected" literally true. Revisit in V1.1 if bug reports are vague |
| T14 | Build distribution | AAB via Play App Signing; keep upload keystore + passwords in two safe places | Standard; key loss is unrecoverable pain |

### 4.2 Project structure (folders, conceptual)
- `model/` — Hour, Section, SectionType, Bookmark, ReminderConfig, plus UI-state classes
- `data/content/` — JSON asset reader, content importer, content versioning
- `data/db/` — Room entities, DAOs, FTS table, database
- `data/prefs/` — DataStore: settings + reminder configurations
- `data/repo/` — PrayerRepository (hours/sections/search), UserDataRepository (bookmarks/recents/settings), ReminderRepository
- `reminders/` — alarm scheduling logic, boot/update receiver, notification builder
- `ui/home/`, `ui/reading/`, `ui/search/`, `ui/bookmarks/`, `ui/reminders/`, `ui/settings/` — screen + ViewModel each
- `ui/theme/` — colors, type scale (Ethiopic fonts), shapes
- `assets/content/` — one JSON per hour + manifest (content version, hour list)

### 4.3 Data model — field level
**Hour**: id (stable string, e.g., `"morning"` — used in deep links & reminders, never renumber) · orderIndex · nameAmharic · nameTransliteration · timeHintAmharic · defaultReminderTime.

**Section**: id (stable string `"morning_ps62"`) · hourId · orderIndex · type (enum §2.2) · titleAmharic · subtitle (nullable) · rubric (nullable) · bodyText · reference (nullable) · normalizedSearchText (derived at import).

**SearchIndex (FTS):** sectionId · normalized title · normalized body.

**Bookmark**: sectionId · createdAt. **Recent**: hourId · lastOpenedAt (keep max 5).

**PrayerMode** (Room — dynamic user data, not DataStore): id · name · isBuiltIn (true only for the shipped Agpeya mode) · isActive (exactly one row true — enforce in repository logic) · soundChoice · createdAt · lastUsedAt.

**ReminderEntry** (Room, belongs to a mode): id · modeId · targetType (`hour` in V1; enum reserved for `section`/`bookmark` in V1.1) · targetId (hourId) · hour · minute · daysOfWeek (set of 1–7) · enabled.

**Built-in mode seeding:** created on first launch from the §2.1 defaults with all entries disabled; "Reset to traditional times" re-seeds its entries. App updates must never overwrite the user's edits to its times/toggles (seed only if absent).

**Settings** (DataStore): theme · fontScaleStep · fontFamily · keepScreenOn · onboardingDone · importedContentVersion.

**Rules:** IDs are permanent contracts (bookmarks/reminder targets reference them across app updates). Content re-import must preserve bookmarks and reminder entries → import = upsert by sectionId/hourId, never wipe user tables. The alarm schedule is **derived state**: Room (active mode + its enabled entries) is the source of truth; a single "reschedule from database" routine rebuilds all alarms (§8.4).

### 4.4 Content JSON shape (described, not coded)
Per-hour file: hour metadata block + ordered array of section objects mirroring §4.3 fields (minus derived ones). A `manifest` file lists hour files + integer `contentVersion`. Validation rules (enforced by a check during development): every section has non-empty title+body, orderIndexes contiguous, ids unique across app, types valid, no stray control characters.

---

## 5. Learning Plan (Weeks 1–4, runs parallel to content entry)

### 5.1 Week 1 — Kotlin core (≈2h/day)
| Day | Topic | Domain exercise (console) |
|---|---|---|
| 1 | val/var, types, strings, string templates | Print a formatted prayer-hour table |
| 2 | Functions, default & named args, expression bodies | `timeOfDayToHour(now)` → which hour to suggest |
| 3 | Null safety: `?.`, `?:`, `let`, `requireNotNull` | Lookup section by id, handle missing gracefully |
| 4 | Data classes, enums | Model Hour, Section, SectionType; print Morning's skeleton |
| 5 | Collections: filter/map/find/sortedBy/groupBy | Group sections by type; find all psalms across hours |
| 6 | Lambdas, higher-order functions, scope functions | A tiny "search" over a hardcoded section list |
| 7 | Review + read about coroutines conceptually (suspend, Flow as "stream of values") | Re-do day 6 with the search wrapped in a suspend function |

### 5.2 Week 2 — Kotlin consolidation
- Sealed classes/interfaces (you'll use one for UI state: Loading/Content/Error).
- `when` exhaustiveness, smart casts.
- Collections part 2: `associateBy`, `flatMap`, `take`, chunking.
- Basic file/JSON reading concept (kotlinx.serialization at a "what it does" level).
- Mini-project: console Agpeya — load 2 hours from a JSON file, list, "open", search. **This becomes your test data for the real app.**

### 5.3 Week 3 — Android & Compose entry
- Day 1: Android Studio install, SDKs, emulator + **run on your real phone the same day**.
- Day 2–3: Compose mental model — composables, recomposition, `remember`/`mutableStateOf`, preview.
- Day 4: Layouts — Column/Row/Box/Spacer/Modifier; build a static Hour card.
- Day 5: `LazyColumn` — render a fake hour's sections (your reading screen prototype is born here).
- Day 6: Material 3 theming — colors, type scale; load the Ethiopic font into the type scale; dark theme toggle.
- Day 7: State hoisting properly; font-size state lifted and applied to the list.

### 5.4 Week 4 — App skeleton concepts
- Navigation Compose: two routes with an argument (hour list → hour detail).
- ViewModel + StateFlow + `collectAsStateWithLifecycle`: move fake data behind a ViewModel.
- Room "hello world": one entity, one DAO, display from DB.
- DataStore "hello world": persist the font-size step.
- Lifecycle basics: what survives rotation (ViewModel) vs process death (persisted state); enable "Don't keep activities" once and observe.
- **Exit test:** throwaway app = list of 8 hour names from Room → detail screen with sections from Room → font slider persisted in DataStore → dark/light toggle. If this works, the real MVP is assembly, not research.

---

## 6. Phase 4 — MVP Build (Weeks 5–6, step-by-step)

Each numbered step ends with a runnable app. Never start the next step with the current one broken.

**Step 1 — Project bootstrap (0.5 day):** new project, package name (permanent! e.g., `com.<name>.agpeya`), min/target SDK, Compose + Material 3, version control init, `.gitignore`, the folder structure of §4.2 as empty packages. Bundle fonts; set up theme + type scale with Ethiopic font.

**Step 2 — Reading screen with hardcoded data (2 days):** one hardcoded hour (3–4 real sections from your console project). Build the section rendering: header style, rubric style, body style, paragraph spacing, font-size control, auto-hiding top bar. Get the typography *right now* — every later screen inherits it. Test at min and max font size, light and dark.

**Step 3 — Home + navigation (1 day):** Home with hour cards (static list), Now-card with the time-band logic, navigate to reading screen with hourId. Bottom nav shell with placeholder Search/Bookmarks/Settings.

**Step 4 — Content pipeline (2 days):** finalize JSON shape; write importer (assets → Room on first launch, guarded by contentVersion); repository serves hours/sections from Room; Home and Reading screens now read from Room via ViewModels. Verify import time is acceptable on the old device (if > ~2s, show a tiny one-time "Preparing prayers…" state).

**Step 5 — Section jump + scroll memory (1 day):** contents bottom sheet, scroll-to-section, per-hour scroll position persistence, keep-screen-on flag.

**Step 6 — Content entry sprint (parallel, ongoing):** keep filling hour JSONs (§2.5). MVP exit requires **at least 3 hours fully approved**, all 8 present even if drafted.

**MVP exit criteria:** open app → Now card → full hour reads beautifully offline at any font size in both themes on both test devices; process death during reading doesn't crash or lose the screen.

---

## 7. Phase 5 — Search & Content Hardening (Week 7, days 1–3)

1. Define and document the homophone fold table (§3.4); derive `normalizedSearchText` at import.
2. FTS table + search query (prefix matching for type-as-you-search); debounce input.
3. Search UI per §3.4 incl. snippet + highlight; navigation to section.
4. **Unit tests (the project's most valuable tests):** fold function — every homophone pair maps to same form across full vowel series; query "ጸሎት" finds text written "ፀሎት" and vice versa; empty/whitespace/Latin input doesn't crash.
5. Content validation pass: run the §4.4 validation rules over all hour files; fix violations.
6. Re-import on contentVersion bump verified to preserve bookmarks (write this test scenario down and do it manually).

## 8. Phase 6+7 — User Features & Reminders (Week 7 day 4 → Week 8)

### 8.1 Bookmarks & recents (1 day)
Toggle on section headers; bookmarks screen per §3.5; recents recorded on hour open, surfaced on Home. Persistence across app kill verified.

### 8.2 Settings (1 day)
All §3.7 items wired to DataStore; theme applies instantly; about page with credits & licenses.

### 8.3 Reminder behavior spec (decide before building)
- **Only the active mode schedules anything.** Each enabled entry in the active mode = one alarm at its next occurrence (respecting its weekday set). Switching the active mode = cancel all + reschedule from the new mode, atomically. Inexact alarms: may fire within a window of minutes — acceptable; documented in the help row.
- **Next-occurrence logic** (unit-test this hard): given entry time + weekday set + "now", compute the next fire time — covers "today but time passed → next valid day", weekday wrap-around (Sat→Mon), and DST/timezone shifts.
- Notification: channel "Prayer reminders" (sound set on channel — note: changing sound after channel creation requires care on Android; plan the sound picker to recreate/use distinct channels per sound choice, or set sound per-notification pre-O — since minSdk 26, it's channel-based: **decide D7: one channel per bundled sound** (2–3 channels) so the picker works reliably).
- Content: hour name + one fixed short verse per hour (pick 8 one-liners during content entry). Tap → deep link to that hour's reading screen. Auto-dismiss on tap.
- If the user disabled notifications at the system level: reminders page shows an inline warning with a button to system settings.

### 8.4 Permission & scheduling flows
- **POST_NOTIFICATIONS (Android 13+):** request only at the moment the user first flips a reminder on; pre-prompt with one sentence of context. Denied → show how to enable in settings; don't nag.
- **Schedule points:** on any entry/mode change (toggle, time, days, mode switch, mode delete); on BOOT_COMPLETED; on MY_PACKAGE_REPLACED (app update); on time/timezone change. All five call the single **"reschedule everything from the active mode in Room"** routine — alarms are always derived, never independently mutated.
- Alarm fires → post notification → compute and schedule that entry's *next occurrence* per its weekday set (chain pattern) — **decide D8: chain pattern** (more reliable across doze; weekday sets make repeating alarms unworkable anyway).
- Deleting the active mode → built-in Agpeya mode becomes active (with whatever entries the user had enabled on it); state communicated via snackbar.

### 8.5 Reminder test matrix (all must pass on 2 physical devices)
| # | Scenario | Pass condition |
|---|---|---|
| R1 | Enable 6:00, wait (or set clock) | Fires within acceptable window |
| R2 | Reboot device after R1 | Next day still fires |
| R3 | Update app (reinstall over) | Still fires |
| R4 | Disable toggle | Never fires again |
| R5 | Change device time/timezone | Fires at correct local time |
| R6 | Tap notification | Opens correct hour, back goes Home |
| R7 | Deny notification permission | App fully usable; clear guidance shown |
| R8 | Battery saver on (Xiaomi/Samsung) | Document behavior; help text accurate |
| R9 | Switch active mode | Old mode's alarms stop entirely; new mode's fire |
| R10 | Weekday-limited entry (e.g., Wed/Fri) | Fires only on those days; correct wrap to next week |
| R11 | Delete the active custom mode | Falls back to Agpeya mode; no orphaned alarms fire |
| R12 | Edit an entry's time while its alarm is pending | Old time cancelled, new time fires |

### 8.6 OEM battery-manager mitigation
Help row content: explain that some phones stop reminders; per-brand one-liners (Xiaomi: Autostart + No battery restrictions; Samsung: remove from Sleeping apps). Test on at least one such device; dontkillmyapp.com is the reference for writing this help text.

---

## 9. Phase 10 — Quality, Testing, Release (Weeks 9–12)

### 9.1 Device matrix
| Device | Why |
|---|---|
| Your daily phone | Primary dev |
| Low-end Android 8–10, ≤2 GB RAM | Elder persona; performance gate |
| Samsung or Xiaomi | Battery-manager behavior, OEM skin |
| (Optional) Firebase Test Lab free tier | Extra screen sizes/API levels, smoke only |

### 9.2 Full manual test checklist (run completely before each release candidate)
**Content:** every hour opens; spot-read 3 random sections per hour vs source; no tofu/� glyphs; Ge'ez numerals render; rubrics styled correctly.
**Reading:** min & max font; dark & light; rotation mid-scroll; process death (background → "Don't keep activities" → return) lands sanely; keep-screen-on honored; section jump for first/middle/last section; scroll memory.
**Search:** homophone cases; partial word; whole phrase; result tap lands on correct section; rapid typing doesn't stutter.
**Bookmarks/recents:** add/remove/undo; survive force-stop; survive app update; removed section edge case (content update deleting a bookmarked id — importer must handle: keep orphan hidden or prune silently — decide D9: prune + no crash).
**Reminders:** full §8.5 matrix.
**Settings:** every toggle persists; theme switch everywhere (check bottom sheets & snackbars too).
**A11y:** TalkBack walk of all 6 screens (sections announced meaningfully); touch targets ≥ 48dp; contrast both themes; font scale 200% system setting doesn't break layouts.
**Performance:** cold start ≤2s on low-end device; longest hour scrolls at 60fps-ish (no visible jank); app size sane (~ a few MB + fonts); no INTERNET permission in final manifest (verify in merged manifest!).

### 9.3 Automated tests (deliberately minimal)
- Unit: normalization/fold table (§7.4) · time-band "Now card" logic (boundaries: 23:59, 00:00) · **next-occurrence computation for reminder entries** (weekday sets, time-already-passed-today, week wrap-around, DST/timezone change) · active-mode invariant (exactly one active; delete-active falls back to built-in) · content validator rules.
- One Room migration test placeholder discipline: **never ship a schema change without a migration from v1.** Write this rule on the wall.

### 9.4 Play release pipeline
**Week 9:** developer account ($25; start **now** — identity verification can take days). App icon finalized (test legibility at 48px). Screenshots from real device, Amharic UI, both themes; feature graphic. Privacy policy page (static hosting, e.g., GitHub Pages): "collects nothing, stores everything on device." Store listing text Amharic + English. Data Safety: no collection/sharing. Content rating questionnaire (Reference/Spirituality → Everyone). App category: Lifestyle or Books & Reference (D10 — recommend Books & Reference; better discovery for prayer books).
**Closed testing (start week 9!):** ⚠️ personal accounts created after Nov 2023 must run a closed test with **12+ testers opted-in for 14 consecutive days** before production access. Recruit 15–20 from church community; give them the §9.2 highlights as a guided test script; create a feedback channel (Telegram/WhatsApp group — common in the community).
**Weeks 10–11:** fix rounds from testers; at least one content-correction cycle (testers *will* find typos — that's the point); release candidates as needed.
**Week 12:** apply for production; staged rollout 20% → 50% → 100% over ~4–5 days watching vitals; tag the release in version control; archive the exact content files shipped.

### 9.5 Versioning & branching (lightweight)
- Semantic-ish: 1.0.0; content-only fixes bump patch. `versionCode` increments every upload.
- Solo flow: main branch always buildable; short-lived branches optional. Tag every Play upload.
- Backups: upload keystore + passwords in password manager **and** one offline copy; content sheet exported monthly.

---

## 10. Post-Launch Roadmap

### V1.0.x (first month) — listen and patch
Typos/content fixes (cheap via contentVersion bump) · crash/ANR triage from Play vitals · reminder reliability reports per OEM → improve §8.6 help text.

### V1.1 — Audio (own mini-project, plan before building)
1. **Content first:** source recordings (record a blessed reader chapter by chapter, or license existing). Per-hour audio, section-level timestamps captured during editing if feasible.
2. Media3/ExoPlayer + MediaSessionService: background play, media notification, headset controls.
3. Audio **not** bundled: per-hour download (now the app needs INTERNET — gate it: permission added only in 1.1, privacy policy updated, downloads only on user tap), stored in app-specific storage, delete/manage UI, sizes shown.
4. Stretch: highlight current section during playback (needs the timestamps).
5. Also in 1.1: sepia theme · share-section-as-text · type filter chips in search · tablet polish · **Prayer Modes upgrades:** entries targeting a specific section or bookmark (targetType already reserved), per-entry sounds, pre-reminder "X minutes before" (market standard per §15.2), per-hour section hide/collapse ("my prayer plan"), and shareable/preset modes (e.g., a "Lent"/ጾም preset).

### V1.2 — Ethiopian calendar & fasting
- Ethiopic calendar conversion (13 months, Amete Mihret offset, leap rule) — validate against published tables for ≥5 different years incl. a leap year.
- **Bahire Hasab** for movable feasts/fasts (Nineveh, Abiy Tsom/Lent, Fasika…) — find an authoritative reference implementation/table; unit-test against known historical dates; have clergy sanity-check outputs.
- Features: today's Ethiopian date on Home · fasting-period indicator · major feast list · optional feast notifications (reuse reminder infra).

### V2 — candidates (pick by user feedback)
Synaxarium/daily saint (large content + licensing project) · Ge'ez parallel text · English translation · home-screen widget ("today's hour") · Wear OS glance · verse-image sharing.

---

## 11. Risk Register (expanded)

| # | Risk | L×I | Mitigation | Trigger to act |
|---|---|---|---|---|
| 1 | Content rights unclear/refused | M×Fatal | Resolve week 0 before code; fallback = transcribe public-domain/blessed edition | No written OK by end of week 0 → switch source |
| 2 | Content entry underestimated | H×H | Start week 0, 1h/day cadence, recruit helper, track sheet weekly | <50% sections drafted by end of week 4 → get helper / cut Veil prayer from V1 |
| 3 | Ethiopic rendering broken on some devices | M×H | Bundle Noto fonts; low-end real-device test in week 5 | Any tofu in step-2 testing → font/shaping investigation immediately |
| 4 | OEM kills alarms | H×M | Inexact+chain pattern, help text, OEM device testing | Tester reports in closed test → expand help, consider exact-alarm opt-in |
| 5 | Play 14-day closed test delays launch | H×M | Account + testers ready by week 8; start test week 9 | <12 testers by week 9 → recruit harder before anything else |
| 6 | Identity verification / account issues at Play | L×H | Create account week 1, not week 9 | — |
| 7 | Scope creep | H×M | §1.3 contract; every new idea gets a version label, not code | Catching yourself building audio in week 6 |
| 8 | Learning stalls (Compose/coroutines confusion) | M×M | Course-driven weeks 3–4; throwaway app gate before MVP | Week-4 exit test failing → add 3–4 buffer days, cut §3.3 nice-to-haves |
| 9 | Burnout (solo, 12 weeks) | M×H | Every step ships a runnable app; content as "low-energy day" work; one rest day/week | Two zero-progress weeks → re-scope V1 down (search to 1.1 if needed — bookmarks+reminders matter more) |
| 10 | Key/keystore loss | L×Fatal | Two backups, password manager + offline | — |
| 11 | Prayer Modes scope balloons week 8 | M×H | D12 (one active mode) and hour-only targets keep it bounded; entry/mode count limits | Week 8 half over without scheduling engine working → ship V1 with built-in mode only, custom modes in 1.0.1 |

---

## 12. Consolidated Timeline

| Week | Engineering / Learning | Content (parallel track) | Milestone gate |
|---|---|---|---|
| 0 | Decisions D1–D10 drafted; wireframes; Play account created | Source chosen, **rights cleared**, tracking sheet built | Rights in writing |
| 1 | Kotlin week 1 (§5.1) | Entry begins (Morning) | — |
| 2 | Kotlin week 2 + console mini-project | Morning drafted | Console Agpeya works |
| 3 | Compose basics (§5.3) | Third + Sixth drafted | — |
| 4 | App-skeleton concepts (§5.4) | Ninth + Vespers drafted | Throwaway-app exit test |
| 5 | MVP steps 1–3 | Compline + Midnight drafted | Reading screen approved at all sizes |
| 6 | MVP steps 4–5 | Veil drafted; proofreading wave 1 | **MVP exit criteria** |
| 7 | Search + bookmarks + settings (§7, §8.1–8.2) | Proofreading wave 2 | Feature-complete minus reminders |
| 8 | Prayer Modes: data model + modes/editor UI + scheduling engine (§2.7, §3.6, §8.3–8.6) — the heaviest engineering week; cut §3.3 nice-to-haves before cutting here | All hours reviewer-approved | Reminder matrix R1–R12 passes |
| 9 | Polish, a11y, icon, screenshots, listing; **closed test starts** | Final content freeze (contentVersion 1) | 12+ testers opted in |
| 10 | Bug-fix round 1; full §9.2 checklist run | Typo fixes from testers | RC1 |
| 11 | Bug-fix round 2; performance pass | — | RC final; 14-day clock done |
| 12 | Production application; staged rollout | — | 🎉 **V1 live** |

---

## 13. Decision Log — resolved via market research 2026-06-12 (see §15 for evidence)
| ID | Question | Decision | Basis |
|---|---|---|---|
| D1 | Include Veil prayer? | **Yes**, with one-line note on its monastic/clerical tradition | agpeya.org and standard Coptic editions include it positioned after Midnight "to facilitate daily usage by laymen" |
| D2 | Midnight watches presentation | **One entry, three watch parts** | agpeya.org and printed editions treat Midnight as one prayer of three watches (Gethsemane structure) |
| D3 | Hour naming convention | Follow source edition verbatim | Content decision; confirm with reviewer in week 0 |
| D4 | Modes entry point | Home card showing active mode + Settings row | Resolved by modes pivot |
| D5 | Color palette | Deep muted palette; **invest heavily in dark theme** — pick from 2–3 mockups | Competitor reviews single out dark mode for praise; night hours (Compline/Midnight) are prayed in the dark |
| D6 | Dynamic color (Material You) | **Off** — fixed brand palette | No competitor uses it; consistent reverent identity matters more than wallpaper-matching |
| D7 | Notification channels for sounds | One channel per bundled sound (2–3 channels) | Technical: channel sound is immutable after creation on Android 8+ |
| D8 | Alarm pattern | Chain (schedule next occurrence on fire) | Technical: weekday sets make repeating alarms unworkable; more doze-reliable |
| D9 | Orphaned bookmarks on content update | Prune silently | Technical; no user-visible benefit to keeping orphans |
| D10 | Play category | **Books & Reference** | Religious text apps split between Books & Reference and Lifestyle; B&R fits a prayer *book* and its browse intent better |
| D11 | Weekday selection per entry in V1? | **Yes** | Market standard: Muslim Pro/Athan support daily-or-specific-days per prayer; fasting-day (Wed/Fri) use case |
| D12 | One active mode vs multiple simultaneous | **Exactly one active** | No mainstream prayer app runs multiple simultaneous schedules; per-prayer customization within one schedule is the norm — modes-with-switching already exceeds market |
| D13 | Entry targets beyond hours | V1.1 (targetType reserved) | Competitor's loved "hide sections → my prayer plan" feature shows demand for sub-hour customization; defer to keep week 8 bounded |

---

## 15. Competitive Landscape (researched 2026-06-12)

### 15.1 What exists
**Coptic Agpeya apps (Arabic/Coptic/English — none in Amharic):**
- [Coptic Agpeya](https://play.google.com/store/apps/details?id=com.xpproductions.copticagpeya) (Mina D. Makar) — the feature benchmark: up to **7 prayer reminders**, **collapsible/hideable sections so users "create their unique prayer plan"**, prayer logging/progress tracking, font sizes, portrait/landscape, light/dark. Reviews praise the customization and dark-mode polish.
- [Agpeya الأجبية](https://play.google.com/store/apps/details?id=com.aletheia.agpeya) — **auto-opens the proper hour for the time of day** (validates our Home "Now" card), reminders, night mode, vertical/horizontal scrolling. Reviews praise readability and the auto-open.
- [agpeya.org](https://agpeya.org/) (web) — reference for canonical structure: Midnight as three watches, Veil prayer included for lay use, font size + theme settings.

**Ethiopian Orthodox apps (Amharic — our direct space):**
- [Bete Tselot](https://play.google.com/store/apps/details?id=com.kiduel.bete_tselot) (4.8★) — broad content (81-book Bible, Kidase in Ge'ez), Amharic+Ge'ez, offline, free and ad-free.
- [Mezgebe Tselot](https://play.google.com/store/apps/details?id=com.yosef.ethiopian.orthodox.mezgebe.teselot) — 300+ prayers, multi-language.
- Pattern: Ethiopian apps are **broad libraries**; none found is a focused, beautifully-typeset Agpeya/Saatat with a modern reminder system. **That is our gap.**

**Reminder-system benchmark (Muslim prayer apps):**
- [Muslim Pro](https://support.muslimpro.com/hc/en-us/articles/360029518492-How-to-set-prayer-notifications-or-adhan) / [Athan](https://play.google.com/store/apps/details?id=com.athan) — per-prayer notification toggle, per-prayer sound, repeat **daily or on selected days**, and **pre-reminder "X minutes before"**. This is the customization bar users coming from those apps expect.
- No mainstream prayer app offers **multiple named, switchable schedules** — our Prayer Modes concept (Agpeya Classic + custom modes) goes beyond the market while D12 (one active) keeps it as simple as what users already understand.

### 15.2 Implications adopted into this plan
1. **Modes are a differentiator, not table stakes** — market it in the Play listing ("create your own prayer schedule; switch for fasting seasons").
2. **Pre-reminder ("10 minutes before")** is standard in the benchmark apps → added to V1.1 list (cheap once the chain scheduler exists: schedule a second, earlier notification per entry).
3. **Section hide/collapse ("my prayer plan")** is the competitor's most-loved customization → strengthens D13's V1.1 priority: section-level mode targets + per-hour section visibility.
4. **Prayer streak/log** exists in the Coptic benchmark → V2 candidate; do it gently if at all (prayer ≠ gamification; consider a private "prayed today" mark, no streak pressure).
5. **Free, ad-free, offline is the expected norm** in the Ethiopian space (Bete Tselot) → our no-INTERNET, no-ads stance matches expectations; lead with it in the listing.
6. **Auto-open / suggest current hour** is loved in reviews → keeps the Home "Now" card as a P1 feature, not a nice-to-have.
7. **Dark mode quality gets called out by name in reviews** → §3.8 dark theme is review-bait; test it like a feature, not a toggle.

## 14. Definition of Done — V1 (final gate)
- [ ] All hours `reviewer-approved`; content freeze tagged
- [ ] No INTERNET permission in merged manifest
- [ ] Homophone search passes unit tests + manual cases
- [ ] Bookmarks/recents/settings survive update & force-stop
- [ ] Reminder matrix R1–R12 passes on 2 physical devices (incl. mode switching, weekday entries, active-mode deletion)
- [ ] Built-in Agpeya mode seeds correctly on fresh install; user edits to it survive app updates
- [ ] Full §9.2 manual checklist green on RC
- [ ] TalkBack pass on all screens; 48dp targets; contrast OK
- [ ] Cold start ≤2s on low-end device
- [ ] Privacy policy live; Data Safety "no data collected"; licenses page includes fonts
- [ ] Closed test: 12+ testers, 14 days, feedback triaged
- [ ] Keystore backed up twice; release tagged; shipped content archived
- [ ] **Live on Google Play**
