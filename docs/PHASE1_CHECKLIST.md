# Phase 1 Checklist — Week 0
Phase 1 is done when every box is checked. Items marked 👤 only you can do.

> **SCOPE CHANGE (2026-06-12):** content = **psalms + gospel reading per hour only**,
> extracted from the open-source [80-weahadu](https://github.com/EOTCOpenSource/80-weahadu)
> Amharic Bible (cloned at `..\80-weahadu`). No litanies/absolutions/creed blocks in V1.
> Manual content entry is eliminated; CONTENT_TRACKER.csv is superseded by
> [content/hour_mapping.json](../content/hour_mapping.json).

## A. Content foundation (blocking — do first)
- [x] Source chosen: 80-weahadu Amharic Bible (psalms + gospels)
- [x] Hour → psalms/gospel mapping captured → [content/hour_mapping.json](../content/hour_mapping.json) (from agpeya.org, 2026-06-12)
- [ ] 👤 **License**: 80-weahadu has NO LICENSE file — open a GitHub issue / contact EOTCOpenSource asking them to add one or grant permission; record outcome in [CONTENT_RIGHTS.md](CONTENT_RIGHTS.md)
- [ ] 👤 Recruit the fluent reviewer → confirm (a) the translation matches church usage, (b) the hour→psalm mapping, (c) spot-check extracted text
- [ ] 👤 Confirm which Amharic translation 80-weahadu contains (old/1954-style orthography visible in text, e.g. ኹ/ኸ forms — reviewer should approve readability)
- [ ] Verify Psalm 118 stanza splitting (22 stanzas × 8 verses) renders correctly for Midnight watch 1 / Veil

## B. Product definition (done — review them once)
- [x] Hour list + section skeleton → CONTENT_STRUCTURE.md
- [x] Wireframes for all screens incl. Prayer Modes → [WIREFRAMES.md](WIREFRAMES.md)
- [x] Design decisions D1–D13 resolved with market evidence → PLAN.md §13, §15
- [ ] 👤 Read PLAN.md §13 decisions and veto/confirm each (they're yours, not mine)

## C. Logistics (start the slow clocks)
- [ ] 👤 Create Google Play Console account ($25 — identity verification can take days)
- [ ] 👤 Check app name availability on Play ("Agpeya — አግፔያ" or your pick)
- [ ] 👤 Choose package name (permanent): com.________.agpeya
- [ ] 👤 Start a list of 15–20 closed-test volunteers (need 12+ for 14 days at week 9)
- [ ] Install Android Studio (start of Week 3 prep; can wait)

## D. Phase 1 exit gate
- [ ] Rights recorded in writing
- [ ] Tracker expanded to full per-psalm rows for at least Morning
- [ ] Morning Prayer entry started
- [ ] Play account created
→ then begin Week 1: Kotlin (PLAN.md §5.1) in parallel with daily content entry
