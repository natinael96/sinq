# Foundation checklist — reconciled project status

Reviewed **2026-09-17**, current build **2.4.0 / 74**. The original “Week 0” checklist is superseded: the app is implemented and has a release history. See [detailed project status](PROJECT_STATUS.md) for the current baseline and [historical plan](../PLAN.md) for the learning/release plan as originally written.

## Completed in the repository

- [x] Product identity: **Sinq (ስንቅ)**, permanent application ID `com.sinq.app`, source namespace `com.agpeya.app`.
- [x] Hour mapping captured in [sources/hours/hour_mapping.json](../sources/hours/hour_mapping.json).
- [x] Eight built-in hours and 150 Psalter sections generated and bundled; manual entry is no longer the workflow.
- [x] CC BY-NC-ND 4.0 source terms recorded in [NOTICE](../NOTICE) and [CONTENT_RIGHTS.md](CONTENT_RIGHTS.md); underlying provenance questions remain separate below.
- [x] Psalm 118 stanza extraction implemented and core section IDs guarded by the validator.
- [x] Reader, search, marks, personalization, reminders, Journey, library, calendars and expanded content implemented.
- [x] Gradle build, content validator, unit tests and release automation present.
- [x] Requirements/design/content documentation reconciled to the current code.

## Human review still needed or unverified

- [ ] Record the fluent liturgical reviewer's name, role, reviewed edition and scope of approval.
- [ ] Confirm the prayer-hour mapping and extracted text against approved church usage.
- [ ] Close the underlying translation provenance and transcription/font questions in the rights record.
- [ ] Record device review of Psalm 118, each reading font, large text, TalkBack and both themes.
- [ ] Record reminder/reboot/permission/widget checks across the supported Android range.
- [ ] Record Play account, testing-track and publication status if Play distribution is intended; these cannot be established from the local workflow.

## Release gate now

Run content validation, unit tests, appropriate lint and builds; inspect the generated release artifacts and conduct device/content review. Preserve version, signing and stable-ID compatibility. The tag workflow produces a hand-installable APK and a separate Play-upload AAB.

The old manual tracker, app-name placeholder, missing-license statement and “start Kotlin learning” exit gate no longer describe project work. See [project status](PROJECT_STATUS.md#verification-on-2026-09-16) for this audit's verification results.
