# Sinq (ስንቅ) — TalkBack & Accessibility Manual Test Checklist

This checklist defines the manual validation procedure for screen reader (TalkBack) navigation, dynamic font scaling, touch targets, and color contrast across the Sinq application.

---

## General Accessibility Invariants

- [ ] **Touch Targets**: All interactive elements (buttons, chips, list items, toggles) have a minimum touch target size of 48×48 dp (`Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)` or `heightIn(min = 48.dp)`).
- [ ] **Content Descriptions**: Every `Icon`, `IconButton`, and image element provides a localized `contentDescription` or is explicitly marked `contentDescription = null` when redundant with adjacent text.
- [ ] **Role Semantics**: Checkboxes, toggles, radio buttons, and tabs supply appropriate `Role` (`Role.Checkbox`, `Role.Switch`, `Role.Tab`).
- [ ] **Font Scale Adaptability**: UI text dynamically expands with system font scale (`LocalDensity`). Reader surfaces allow opting into system scale via "Follow system font scale".
- [ ] **Non-Visual Alternatives**: Visual data representations (e.g. 53×7 devotional heatmap) offer non-visual equivalents (e.g. `JourneyListView` week-by-week screen reader list).

---

## 1. Home Screen (ቤት)

- [ ] **Liturgical Date & Fast Banner**: TalkBack announces the current Ethiopian date (e.g. "ጥቅምት ፰ ቀን ፳፻፲፰ ዓ.ም.") and current fasting season with role information.
- [ ] **Candle / Devotional State**: The candle icon announces whether prayer has been kept today without guilt-inducing or punitive phrasing.
- [ ] **Canonical Hour Hero**:
  - Focuses cleanly as a card.
  - Announces the appointed canonical hour name, Ge'ez time interval, and completion status.
  - Action labels declare "Pray hour" or "View hour".
- [ ] **Lectionary (ግጻዌ) Card**:
  - Announces Paul, Catholic Epistles, Acts, and Gospel readings clearly in order.
  - Each scripture link provides a distinct touch target.
- [ ] **Bottom Navigation Bar**:
  - All 5 destinations (ቤት, ጸሎት, መጻሕፍት, ጉዞ, ማስታወሻ) declare selected/unselected state and position in set (e.g., "Tab 1 of 5").

---

## 2. Devotional Journey Screen (ጉዞ)

- [ ] **Hero Metric**: Announces the non-punitive monthly prayer summary (e.g. "23 days of prayer this month").
- [ ] **Today's Prayer Hours (Horizontal Scroll)**:
  - Each canonical hour chip (ነግህ, ሠለስት, ስድስት, ተስዐት, ሠርክ, ነዋም, መንፈቀ ሌሊት) is selectable.
  - TalkBack announces: `"[Hour Name]: Prayed"` or `"[Hour Name]: Not prayed"`.
  - Double-tap toggles completion; long-press opens reading.
- [ ] **Devotional Habits Checklist**:
  - Each `HabitRow` is announced with its completion state and summary cadence (e.g., `"Wudase Maryam: Completed. ፲፰/፴"`).
  - Row target height is at least 48 dp.
- [ ] **Devotional Heatmap / List Alternative**:
  - Settings allows switching between "Grid" and "List".
  - TalkBack users default to "List" (`JourneyListView`), presenting chronological weekly summaries of prayers and fasts.
  - In "Grid" mode, individual day cells announce: Ethiopian date, hours kept, fasting status.
- [ ] **Day Inspection Card**:
  - Close, Previous Day, and Next Day buttons announce their actions with >= 48 dp targets.
  - Fasting status announces "Fasting day" label alongside fast name.
  - Displays and announces the exact hour count (e.g. "3 of 7 hours prayed").

---

## 3. Reader & Scripture Screens (ንባብ)

- [ ] **Top Bar Actions**:
  - Back, Font Options, Share, and Bookmark buttons have descriptive, localized labels.
- [ ] **Verse / Paragraph Traversal**:
  - TalkBack navigates sequentially from verse to verse in LXX order.
  - Ge'ez numerals (e.g. ፩, ፪, ፫) are read cleanly without pronunciation breakage.
  - Rubricated holy names (ቀይ ጽሑፍ) maintain semantic text flow without broken reading pauses.
- [ ] **Text Selection & Actions**:
  - Highlight action bar buttons (Gold, Coral, Olive, Azure) announce color names and actions.
  - Share action declares full passage reference and text copy.
- [ ] **Horizontal / Leaf Pagination**:
  - In Leaf mode, swipe gestures and navigation affordances declare current page and total page count.

---

## 4. Search Screen (ፍለጋ)

- [ ] **Search Field**:
  - Declares search input field hint ("Search scriptures, prayers, Synaxarium...").
  - Clear text button appears when query is present and announces "Clear query".
- [ ] **Filter Chips**:
  - Filter chips (All, Scripture, Sinksar, Mahlet, Hours) announce selected state and toggle cleanly.
- [ ] **Search Results**:
  - Each result row announces: document title, section/chapter, match quality (exact vs folded orthography), and excerpt snippet.
  - Empty search state announces helpful guidance without abrupt focus traps.

---

## 5. Settings Screen (ቅንብሮች)

- [ ] **Category Navigation**:
  - Hub items (Appearance & Reading, Prayer Rule, Reminders, Data & Backup) announce subtitles and navigation arrows.
- [ ] **Typography Controls**:
  - Font choice radio items announce font name and active state.
  - Font size stepper buttons announce "Decrease font size" and "Increase font size" with current point size.
  - "Follow system font scale" switch announces current on/off state.
- [ ] **Journey View Mode Toggle**:
  - Announces "Journey view: Grid / List" with current mode.
- [ ] **Backup & Restore**:
  - Create backup and Restore backup buttons declare their actions and confirm completion through accessible alerts.
