package com.agpeya.app.reminders

/**
 * Every notification id the app posts under, in one place.
 *
 * A notification id is a single namespace for the whole package: posting a
 * second notification under an id that is already showing **replaces** the
 * first, silently. Two receivers that each picked a number in isolation is
 * exactly how the morning ግጻዌ nudge disappeared — it posted at 06:00 under
 * 7300 and the reading plan's nudge posted under the same 7300 at 06:30,
 * overwriting it before most readers had looked at the phone.
 *
 * The families below are worse than a single clash. [DONE_BASE] and
 * [SNOOZE_BASE] each add `hourId.hashCode() mod 1000`, so 7100 and 7200 were
 * not two ids but two **thousand-wide ranges**, and both of them swallowed
 * every fixed id from 7100 up: the streak nudge at 7200, the ግጻዌ and reading
 * nudges at 7300, and the breath prayer at 7600 all sat inside a range a
 * pending snooze or a done-marker could land on.
 *
 * So: singles are packed into 7000, and each hashed family gets its own
 * thousand well clear of them and of each other. Add a new notification by
 * adding it here, never by picking a number at the call site.
 */
object NotificationIds {

    // ── Singles: one notification each, one id each ──────────────────────
    /** The ringing prayer alarm. */
    const val ALARM = 7001
    /** The nightly streak nudge. */
    const val STREAK = 7002
    /** The morning ግጻዌ reading nudge, 06:00. */
    const val GITSAWE = 7003
    /** The reading plan's daily nudge; its time is the reader's to set. */
    const val READING = 7004
    /** The breath prayer. */
    const val BREATH = 7005

    // ── Families: one notification per hour or per habit ─────────────────
    // Each takes BASE + bounded(key), so each occupies exactly its own
    // thousand. Keep the bases a thousand apart.
    private const val SPAN = 1000
    const val DONE_BASE = 10_000
    const val SNOOZE_BASE = 11_000
    const val HABIT_BASE = 12_000

    /**
     * A stable id within one family's thousand.
     *
     * floorMod rather than `%`, because a negative hashCode — which is half of
     * them — would otherwise push the id below its base and out of the family
     * it was meant to stay inside.
     */
    fun inFamily(base: Int, key: String): Int = base + Math.floorMod(key.hashCode(), SPAN)
}
