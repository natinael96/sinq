# Software Requirements Specification (SRS)
### Ge'ez Hours of Prayer — "ጸሎት" (Tselot) Android Application
Version 1.0 · Document status: Living · Aligned to the current build

---

## 1. Introduction

### 1.1 Purpose
This document specifies the functional and non-functional requirements for the
**ጸሎት** Android application: an offline Ethiopian/Coptic Orthodox hours-of-prayer
(Agpeya) reader with reminders, personalization, and a spiritual-habit tracker.
It is intended for the developer, reviewers (clergy/content), and testers.

### 1.2 Scope
The product delivers, fully offline on Android phones:
- The canonical prayer **hours**, each composed of psalms and a gospel reading in Amharic.
- **Reading** with two layouts, adjustable typography, bookmarks, and verse highlighting.
- **Search** across all prayer text, tolerant of Amharic homophones.
- **Personalization**: show/hide and reorder sections, add any psalm to an hour, and add/rename/reorder/hide/delete whole hours.
- **Reminders**: named prayer schedules ("modes") with per-entry times/weekdays, a real ringing alarm with snooze, and reliability handling across reboot/updates.
- **Habit tracking & streaks** (new module): daily check-off of spiritual practices with per-habit streaks and a GitHub-style contribution heatmap.
- **Localization** (Amharic/English UI), light/dark themes, first-run intro, and OEM battery-optimization guidance.

Out of scope for this version: audio playback, Ethiopian-calendar/fasting computation, the Synaxarium, iOS, cloud sync/accounts, and any network feature.

### 1.3 Definitions, Acronyms, Abbreviations
- **Hour** — a canonical prayer time (e.g., Morning/ጸሎተ ነግህ); a sequence of *sections*.
- **Section** — an ordered unit within an hour (a psalm, a gospel reading, etc.).
- **Psalter** — the full book of 150 Psalms bundled for the "add psalm" feature.
- **Mode** — a named reminder schedule containing reminder *entries*.
- **Entry** — one reminder: a target hour + time + weekday set.
- **Habit** — a trackable daily practice (e.g., Prayer, Church).
- **Record** — the set of habits completed on a given calendar day.
- **Streak** — count of consecutive days a habit (or any habit) was completed.
- **Heatmap** — GitHub-style calendar grid colored by daily completion count.
- **DataStore** — Jetpack Preferences DataStore (local persistence).

### 1.4 References
- Project plan: `PLAN.md`; content structure: `docs/CONTENT_STRUCTURE.md`; rights: `docs/CONTENT_RIGHTS.md`; wireframes: `docs/WIREFRAMES.md`.
- Content source: 80-weahadu open-source Amharic Bible (Psalms + Gospels).
- Fonts: Abyssinica SIL, Noto Sans Ethiopic (SIL OFL 1.1).

### 1.5 Overview
Section 2 gives the overall description and constraints. Section 3 lists external
interfaces, functional requirements (FR) grouped by module, and non-functional
requirements (NFR). Section 4 lists data requirements.

---

## 2. Overall Description

### 2.1 Product Perspective
A standalone, self-contained Android app. No servers, no accounts, no network.
All content is bundled at build time; all user data is stored locally on-device.

### 2.2 Product Functions (summary)
Read the hours · search · bookmark · highlight verses · customize sections and
hours · schedule and receive prayer alarms · track daily habits and streaks ·
switch language and theme.

### 2.3 User Classes
- **Daily worshipper** — opens the app several times daily to pray; wants speed, reminders, large readable text.
- **Learner / new convert** — needs clear structure, search, and section navigation.
- **Elder / low-vision** — needs large fonts, high contrast, simplicity, low-end-device support.
- **Content reviewer (clergy)** — verifies text accuracy (external role; not an app user class per se).

### 2.4 Operating Environment
- Android phones, **minimum Android 8.0 (API 26)**, target the latest API required by Google Play at release.
- Portrait-first; adaptive to light/dark system themes.
- Fully functional with the device in airplane mode.

### 2.5 Design & Implementation Constraints
- **Offline-only**: the app MUST NOT declare the `INTERNET` permission.
- Prayer/scripture **text is data, never translated**; UI chrome is localized.
- Content edits happen via the offline extraction pipeline, not at runtime.
- Single APK/AAB; no dynamic feature modules.

### 2.6 Assumptions & Dependencies
- The bundled Amharic text and hour→psalm mapping are correct (subject to reviewer sign-off) and licensed for distribution.
- The device grants notification and exact-alarm capabilities typical of the target OS versions.

---

## 3. Specific Requirements

### 3.1 External Interface Requirements
- **UI**: Jetpack Compose, Material 3; bottom navigation with Home, Search, Bookmarks, **Streak**, Settings; full-screen pushed screens for reading, reminders, customization, habit management, about, and help.
- **System interfaces**: Android `AlarmManager` (exact alarm clock), high-priority notifications, foreground service (system-exempted type for the ringing alarm), vibrator, `RingtoneManager`, and the system "App details / battery" settings screen (deep-linked from Help).
- **Hardware interfaces**: speaker (alarm sound) and vibration motor.
- **Files/assets**: bundled JSON content (hours + 150-psalm Psalter + manifest) and TTF fonts.

### 3.2 Functional Requirements

#### 3.2.1 Content & Reading
- **FR-1** The system SHALL present the canonical hours, each as an ordered list of sections (psalms, gospel).
- **FR-2** The system SHALL render each verse with its verse number (Ge'ez numerals) and preserve paragraphing.
- **FR-3** The system SHALL provide two reading layouts — **vertical scroll** and **horizontal page-per-section** — with a persistent user toggle.
- **FR-4** The system SHALL provide 5 adjustable font sizes, persisted across sessions and screens.
- **FR-5** The system SHALL provide a section "contents" sheet to jump to any section.
- **FR-6** The system SHALL remember and restore the last reading position per hour.
- **FR-7** The Home screen SHALL suggest the hour matching the current time of day and list recently opened hours.

#### 3.2.2 Search
- **FR-8** The system SHALL search titles and body text across all hours.
- **FR-9** Search SHALL be **homophone-tolerant** for Amharic (folding ሀ/ሐ/ኀ/ኸ, ሰ/ሠ, አ/ዐ, ጸ/ፀ families while preserving vowel order) so a query matches regardless of which homophone is typed.
- **FR-10** Results SHALL show hour, section title, and a snippet, and open the reading screen at that section.

#### 3.2.3 Bookmarks & Highlights
- **FR-11** The user SHALL bookmark/un-bookmark a section; bookmarks are grouped by hour on a dedicated screen and persist across updates.
- **FR-12** The user SHALL highlight/clear individual verses in one of several colors; highlights persist and display in both reading layouts.

#### 3.2.4 Personalization of Content
- **FR-13** Per hour, the user SHALL show/hide and reorder sections ("prayer plan").
- **FR-14** The user SHALL add any of the 150 psalms to any hour, using a picker searchable by **Arabic numeral** (1–150); added psalms can be reordered/hidden/removed.
- **FR-15** The user SHALL manage the hour list: **add** custom hours, **rename** any hour (built-in or custom), **reorder**, **hide/show**, and **delete** custom hours. Built-in hours cannot be deleted, only hidden.
- **FR-16** A custom hour's content SHALL be the psalms the user adds to it.
- **FR-17** All personalization SHALL be non-destructive and resettable per hour.

#### 3.2.5 Reminders & Alarm
- **FR-18** The system SHALL support named **modes**, exactly one active at a time; only the active mode schedules alarms.
- **FR-19** A built-in mode SHALL ship with the traditional hour times (all initially off); the user MAY duplicate it or start an empty mode.
- **FR-20** Each **entry** SHALL specify a target hour, time, weekday set (daily or specific days), and enabled flag.
- **FR-21** On firing, the system SHALL raise a **real alarm**: play the selected sound on the alarm stream and/or vibrate per user preference, and post an ongoing high-priority notification without launching a full-screen activity.
- **FR-22** The alarm SHALL offer **Open**, **Snooze** (fixed interval), and **Dismiss**; Snooze SHALL reliably re-fire after the interval.
- **FR-23** The alarm alert text SHALL be discreet ("It's time") and reveal no prayer/hour name.
- **FR-24** The user SHALL choose alert behavior (sound+vibrate / sound only / vibrate only / silent) and the sound type (alarm / ringtone / notification).
- **FR-25** The system SHALL reschedule all alarms after device reboot, app update, and time/timezone change.
- **FR-26** Tapping a reminder SHALL open the targeted hour.
- **FR-27** The system SHALL request notification permission only when the user first enables a reminder, and SHALL provide OEM battery-optimization guidance from a Help screen.

#### 3.2.6 Habit Tracking & Streaks (new module)
- **FR-28** The system SHALL provide a **Streak** tab with default habits (Prayer, Church, Prostration, Daily Bible) and allow the user to **add, rename, reorder, hide, and delete** custom habits (built-in habits renameable/hideable, not deletable).
- **FR-29** The user SHALL mark each visible habit done/undone for **today** via manual check-off; state persists.
- **FR-30** The system SHALL compute and display, per habit, the **current streak** and **longest streak**.
- **FR-31** The current streak SHALL be day-tolerant: it counts consecutive completed days ending today, or yesterday if today is not yet marked, and only breaks after a full missed day.
- **FR-32** The system SHALL display an **overall current streak** (consecutive days with at least one habit completed).
- **FR-33** The system SHALL render a **contribution heatmap**: a 7-row × ~53-week grid where each day's color intensity reflects the number of habits completed that day (0 = empty, increasing to all-done), horizontally scrollable, defaulting scrolled to the current week, with a Less→More legend.
- **FR-34** Habit data SHALL persist locally and survive app restart and process death.

#### 3.2.7 Localization, Theme, Onboarding, About
- **FR-35** The UI SHALL support **Amharic and English**, selectable as System/Amharic/English; prayer content remains Amharic.
- **FR-36** The user SHALL select theme: System / Light / Dark; applied instantly app-wide.
- **FR-37** The user SHALL toggle "keep screen on while reading."
- **FR-38** A first-launch intro SHALL appear once; an About screen SHALL credit the text source and fonts and state that no data is collected.

### 3.3 Non-Functional Requirements

- **NFR-1 (Privacy)** The app SHALL collect, transmit, or store off-device **no** user data; it SHALL declare no network permission. Data Safety declaration: "No data collected."
- **NFR-2 (Offline)** All features except none SHALL function with no connectivity.
- **NFR-3 (Performance)** Cold start to Home SHALL be ≤ ~2 s on a low-end (≈2 GB RAM, API 26) device; the longest hour SHALL scroll smoothly.
- **NFR-4 (Reliability)** Enabled reminders SHALL fire on time subject to OS constraints; scheduling SHALL never crash (fallback to inexact if exact is denied); alarms SHALL survive reboot/update.
- **NFR-5 (Usability/Accessibility)** Touch targets ≥ 48 dp; sufficient contrast in both themes; readable at maximum font and 200% system font scale; content descriptions on actionable icons.
- **NFR-6 (Rendering)** Amharic SHALL render correctly on all target devices via bundled Ethiopic fonts (no reliance on system fonts).
- **NFR-7 (Portability)** Runs on Android 8.0+; single AAB.
- **NFR-8 (Maintainability)** Prayer content SHALL be regenerable via the offline pipeline and versioned; UI strings SHALL be centralized and compiler-checked across languages.
- **NFR-9 (Integrity)** Personalization and user data SHALL be non-destructive to bundled content and preserved across content updates (keyed by stable IDs).
- **NFR-10 (Security)** No sensitive data is handled; internal components (alarm receiver/service/activity) SHALL not be exported.

### 3.4 Data Requirements
- **DR-1** Bundled content: per-hour JSON (sections with verses), a 150-psalm Psalter JSON, and a manifest with content version.
- **DR-2** User data (local, JSON in DataStore): settings (theme, language, font size, reading mode, keep-screen-on, alarm alert/sound, onboarded); bookmarks; recents; per-hour scroll positions; highlights (verse→color); per-hour layout (order/hidden/added psalms); hours config (custom hours/order/hidden/name overrides); reminder modes and entries; **habits state (custom habits/order/hidden/name overrides/daily records)**.
- **DR-3** All user-data records reference **stable IDs** so bundled-content updates never orphan or corrupt personalization.

---

## 4. Acceptance Criteria (representative)
- Searching a word typed with a "wrong" homophone still finds it.
- An enabled reminder rings with a high-priority notification, snoozes correctly, and survives a reboot.
- Adding a custom hour and psalms makes them appear on Home and in the reader.
- Checking habits fills today's heatmap cell and increments the shown streaks; data persists after force-stop.
- The merged manifest contains no `INTERNET` permission.
