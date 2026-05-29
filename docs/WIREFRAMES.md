# Wireframes — V1 (low-fi, text form)
Phase 1 deliverable. Layout intent only; spacing/colors come from §3.8 of PLAN.md.
Amharic labels shown where they are the real UI text; English in (parens) is annotation.

---

## 1. Home
```
┌──────────────────────────────────┐
│  አግፔያ                        ⚙  │  ← app name; settings shortcut
│                                  │
│  ┌────────────────────────────┐  │
│  │ 🕖 አሁን ጊዜው የነግህ ጸሎት ነው │  │  ← "Now" hero card (time-band logic)
│  │     ጸሎተ ነግህ  →            │  │     one tap → reading screen
│  └────────────────────────────┘  │
│                                  │
│  ቀጥል (Continue):                │  ← recents row, hide if empty
│  [ሰርክ] [ንዋም]                   │
│                                  │
│  ┌──────────┐  ┌──────────┐      │
│  │ ጸሎተ ነግህ │  │ ጸሎተ ሠለስት│      │  ← all 8 hours, canonical order
│  └──────────┘  └──────────┘      │     card: name + small time hint
│  ┌──────────┐  ┌──────────┐      │
│  │ ጸሎተ ቀትር │  │ ጸሎተ ተሰዓት│      │
│  └──────────┘  └──────────┘      │
│  …(ሰርክ ንዋም መንፈቀ ሌሊት ሥውር)   │
│                                  │
│  ┌────────────────────────────┐  │
│  │ ⏰ ማንቂያ: አግፔያ (Classic) → │  │  ← active-mode card → Modes list
│  └────────────────────────────┘  │
├──────────────────────────────────┤
│  🏠 ቤት   🔍 ፈልግ   🔖 ምልክት  ⚙ │  ← bottom nav (4)
└──────────────────────────────────┘
```

## 2. Prayer Reading (the core screen)
```
┌──────────────────────────────────┐
│ ←  ጸሎተ ነግህ            ☰  A−A+ │  ← top bar auto-hides on scroll down
│                                  │     ☰ = section contents sheet
│  ── መዝሙር ፷፪ ──────────────    │  ← section header (title+subtitle)
│     የዳዊት መዝሙር          🔖    │     bookmark toggle per section
│                                  │
│  አምላኪየ አምላኪየ ለከ እገኝ…        │  ← body: reading font, 1.6–1.8 line
│  (full prayer text, generous     │     height, paragraph spacing
│   line height, no clutter)       │
│                                  │
│  ⟪ rubric: ሦስት ጊዜ ይባላል ⟫     │  ← rubric style: smaller, accent
│                                  │
│  ── ወንጌል ──────────────────    │
│  …                               │
└──────────────────────────────────┘
   ☰ opens bottom sheet:
   ┌──────────────────────────┐
   │ ይዘት (Contents)          │
   │  መግቢያ ▸                 │
   │  መዝሙራት ▸ ፷፪ ፷፮ ፷፱ …  │   ← grouped; tap scrolls to section
   │  ወንጌል ▸                 │
   │  ሊጣንያ ▸  መዝጊያ ▸       │
   └──────────────────────────┘
```

## 3. Search
```
┌──────────────────────────────────┐
│  🔍 [ ፈልግ…              ]  ✕   │  ← autofocus; debounced as-you-type
│                                  │
│  ጸሎተ ነግህ › መዝሙር ፷፪          │  ← result: hour › section
│  …አምላኪየ **አምላኪየ** ለከ…       │     snippet with match highlighted
│  ─────────────────────────────   │
│  ጸሎተ ሰርክ › ሊጣንያ              │
│  …**አምላክ**ን እንለምን…           │
│                                  │
│  (homophone-folded: ሰ=ሠ ሀ=ሐ=ኀ  │
│   አ=ዐ ጸ=ፀ — finds either form)  │
├──────────────────────────────────┤
│  🏠   🔍   🔖   ⚙               │
└──────────────────────────────────┘
```

## 4. Bookmarks
```
┌──────────────────────────────────┐
│  ምልክቶች (Bookmarks)             │
│                                  │
│  ጸሎተ ነግህ                       │  ← grouped by hour, canonical order
│   ┌────────────────────────────┐ │
│   │ መዝሙር ፷፪ — አምላኪየ…    🗑 │ │  ← tap = jump; swipe/icon = remove
│   └────────────────────────────┘ │     (undo snackbar)
│  ጸሎተ ንዋም                       │
│   ┌────────────────────────────┐ │
│   │ ፍትሐት — …               🗑 │ │
│   └────────────────────────────┘ │
│                                  │
│  (empty state: "በንባብ ገጽ ላይ 🔖   │
│   በመንካት ያስቀምጡ")              │
└──────────────────────────────────┘
```

## 5. Prayer Modes — list
```
┌──────────────────────────────────┐
│  ← የጸሎት ማንቂያ ሁነታዎች (Modes)  │
│                                  │
│  ┌────────────────────────────┐  │
│  │ ◉ አግፔያ (Classic)  [በነባር] │  │  ← built-in badge; ◉ = active
│  │   7 ማንቂያዎች · በየቀኑ      → │  │     radio = exactly one active
│  └────────────────────────────┘  │
│  ┌────────────────────────────┐  │
│  │ ○ የጾም ቀናት            ⋮ → │  │  ← custom; ⋮ = rename/duplicate/
│  │   3 ማንቂያዎች · ረቡዕ፣ ዓርብ  │  │       delete (confirm)
│  └────────────────────────────┘  │
│                                  │
│  [＋ ከአግፔያ ጀምር]  [＋ ባዶ ጀምር] │  ← duplicate-Agpeya is primary CTA
│                                  │
│  ❓ ማንቂያ አይሰራም? →             │  ← battery-manager help (§8.6)
└──────────────────────────────────┘
```

## 6. Prayer Modes — editor
```
┌──────────────────────────────────┐
│  ←  የጾም ቀናት            [✎ ስም] │  ← name editable (custom only;
│  ድምፅ: [ደወል ▾]                  │     built-in: "Reset times" action)
│                                  │
│  06:00  ጸሎተ ነግህ          [ON]  │  ← sorted by time
│         ረቡዕ ዓርብ                │     day chips under each entry
│  12:00  ጸሎተ ቀትር          [ON]  │
│         በየቀኑ                    │
│  21:00  ጸሎተ ንዋም          [OFF] │
│         ረቡዕ ዓርብ                │
│                                  │
│  [＋ ማንቂያ ጨምር]                │
└──────────────────────────────────┘
   Entry editor (bottom sheet):
   ┌──────────────────────────┐
   │ ጸሎት:  [ጸሎተ ነግህ ▾]     │  ← hour picker (sections = V1.1)
   │ ሰዓት:  [ 06 : 00 ]       │  ← defaults to traditional time
   │ ቀናት:  (በየቀኑ) ሰ ማ ረ ሐ  │  ← every-day toggle or weekday chips
   │         ዓ ቅ እ            │
   │        [ሰርዝ]  [አስቀምጥ]  │
   └──────────────────────────┘
```

## 7. Settings
```
┌──────────────────────────────────┐
│  ቅንብሮች (Settings)              │
│  ገጽታ: ስርዓት / ብርሃን / ጨለማ      │
│  የንባብ ፊደል: Sans / Serif        │
│  የፊደል መጠን: A− ──●── A+        │
│  ማያ እንዳይጠፋ: [ON]              │
│  የጸሎት ማንቂያ ሁነታዎች →          │
│  ──────────────────────────      │
│  ስለ መተግበሪያው → (version,       │
│   source & reviewer credits,     │
│   content version, privacy,      │
│   feedback mailto, licenses)     │
└──────────────────────────────────┘
```

## 8. First-launch intro (2 screens, skippable)
```
[1] አግፔያ — brief: what the app is, fully offline, no data collected
[2] ማንቂያ — "Want prayer reminders?" → [አዘጋጅ] opens Modes / [በኋላ] skip
    (notification permission asked only when first reminder enabled)
```

## Flow notes
- Notification tap deep-links to its hour's reading screen; back → Home.
- Search/bookmark taps open reading screen pre-scrolled to the section.
- Switching active mode shows snackbar: "ወደ ___ ተቀይሯል".
- Deleting active custom mode → Agpeya mode becomes active (snackbar).
