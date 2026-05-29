# Agpeya Content Structure — Working Document
Phase 1 deliverable. This defines every hour and its sections. The psalm/gospel
identities are left as slots to be copied **verbatim from your approved source
edition** — do not fill them from memory or the internet.

> ⚠️ **First decision (blocks everything):** Ethiopian practice has two related
> traditions — the **Agpeya** structure (as in the Coptic book of hours, widely
> available in Amharic translation) and the Ethiopian **መጽሐፈ ሰዓታት (Metsihafe
> Se'atat)**, which differs in places. Your source edition decides which
> structure this file follows. Everything below uses the common Agpeya
> structure; adjust after the source is chosen. Record the choice in
> [CONTENT_RIGHTS.md](CONTENT_RIGHTS.md).

## Section ID conventions
- Hour IDs: `morning`, `terce`, `sext`, `none`, `vespers`, `compline`, `midnight`, `veil`
- Section IDs: `<hourId>_<slug>` e.g. `morning_ps62`, `terce_gospel`, `midnight_w1_ps118`
- IDs are permanent once content ships — never rename, only add.

## Shared opening block (appears in every hour — confirm per hour against source)
| Order | Type | Section | Notes |
|---|---|---|---|
| 1 | opening | Introduction / መቅድም | "In the name of the Father…", Thanksgiving prayer |
| 2 | opening | The Lord's Prayer / አቡነ ዘበሰማያት | |
| 3 | opening | Prayer of Thanksgiving / ጸሎተ አኰቴት | |
| 4 | opening | Psalm 50 / መዝሙር ፶ | "Have mercy on me, O God…" |

## Shared closing block (confirm per hour — some hours vary)
| Order | Type | Section | Notes |
|---|---|---|---|
| n−3 | creed | Trisagion / ቅዱስ እግዚአብሔር | |
| n−2 | creed | Hail Mary + Creed block | per edition |
| n−1 | closing | Lord have mercy ×41 / ኪርያላይሶን ፵፩ | rubric: said 41 times |
| n | absolution + closing | Absolution of the hour + concluding prayer | each hour has its own |

---

## Hour 1 — Morning / ጸሎተ ነግህ (`morning`)
The longest hour. Structure: opening block → psalms (≈19 in common editions —
**confirm count and list from source**) → Gospel → doxologies/litanies →
creed block → closing block.

Psalm slots (fill title + body from source):
`morning_ps_01` … `morning_ps_19` — replace `_NN` with the actual psalm number
once known (e.g. `morning_ps62`), then record in the tracker.

Other named sections to expect (per common editions — confirm):
- `morning_gospel` — Gospel of the hour
- `morning_doxology` — morning doxology/praise (ግብረ ሐዋርያት? per edition)
- `morning_litany` — litanies of the hour
- `morning_absolution`, `morning_closing`

## Hour 2 — Third Hour / ጸሎተ ሠለስት (`terce`)
Commemorates the descent of the Holy Spirit. ≈12 psalms in common editions.
Slots: `terce_ps_01`…`terce_ps_12`, `terce_gospel`, `terce_litany`,
`terce_absolution`, `terce_closing`.

## Hour 3 — Sixth Hour / ጸሎተ ቀትር (`sext`)
Commemorates the Crucifixion. ≈12 psalms.
Slots: `sext_ps_01`…`sext_ps_12`, `sext_gospel`, `sext_litany`,
`sext_absolution`, `sext_closing`.

## Hour 4 — Ninth Hour / ጸሎተ ተሰዓት (`none`)
Commemorates the death of Christ. ≈12 psalms.
Slots: `none_ps_01`…`none_ps_12`, `none_gospel`, `none_litany`,
`none_absolution`, `none_closing`.

## Hour 5 — Vespers / ጸሎተ ሰርክ (`vespers`)
Evening thanksgiving. ≈12 psalms.
Slots: `vespers_ps_01`…`vespers_ps_12`, `vespers_gospel`, `vespers_litany`,
`vespers_absolution`, `vespers_closing`.

## Hour 6 — Compline / ጸሎተ ንዋም (`compline`)
Before sleep. ≈12 psalms.
Slots: `compline_ps_01`…`compline_ps_12`, `compline_gospel`,
`compline_litany`, `compline_absolution`, `compline_closing`.

## Hour 7 — Midnight / ጸሎተ መንፈቀ ሌሊት (`midnight`)
**One hour, three watches** (decision D2). Each watch has its own psalms,
gospel, and litanies. In the app: three top-level collapsible parts.
- Watch 1: `midnight_w1_ps_*`, `midnight_w1_gospel`, `midnight_w1_litany`
  (commonly includes the long Psalm 118/119 — confirm)
- Watch 2: `midnight_w2_ps_*`, `midnight_w2_gospel`, `midnight_w2_litany`
- Watch 3: `midnight_w3_ps_*`, `midnight_w3_gospel`, `midnight_w3_litany`
- Shared: `midnight_absolution`, `midnight_closing`

## Hour 8 — Prayer of the Veil / ጸሎተ ሥውር (`veil`)
Included (decision D1) with an intro note on its monastic/clerical tradition.
≈12 psalms. Slots: `veil_note` (rubric intro), `veil_ps_01`…`veil_ps_12`,
`veil_gospel`, `veil_litany`, `veil_absolution`, `veil_closing`.

---

## Per-section fields to capture during entry
| Field | Required | Example |
|---|---|---|
| id | yes | `morning_ps62` |
| hourId | yes | `morning` |
| orderIndex | yes | 7 |
| type | yes | `psalm` |
| titleAmharic | yes | መዝሙር ፷፪ |
| subtitle | no | የዳዊት መዝሙር |
| rubric | no | ሦስት ጊዜ ይባላል ("said three times") |
| bodyText | yes | full Amharic text, paragraphs preserved |
| reference | no | Ps 62 (63) |

## Numerals & punctuation conventions (decision D3 area — set once, follow everywhere)
- [ ] Psalm numbers: Ge'ez numerals (፷፪) or Arabic (62)? → follow source: ______
- [ ] Psalm numbering tradition: Septuagint (62) vs Hebrew (63) → follow source: ______
- [ ] Ethiopic punctuation ። ፣ ፤ used as in source; no Latin punctuation inside Amharic text
- [ ] UTF-8, no BOM issues; check homophone characters are typed as the source spells them
