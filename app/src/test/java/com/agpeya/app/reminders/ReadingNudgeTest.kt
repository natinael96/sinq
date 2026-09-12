package com.agpeya.app.reminders

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The reading nudge's rules, pinned.
 *
 * The design borrows Duolingo's mechanics and refuses its framing. What is
 * taken: stop asking once the asking plainly is not working, and never ask
 * about something already done. What is not taken: a streak, a count, or
 * anything at stake — the app settled that for the nightly nudge already, whose
 * wording has to be equally true on a first morning and on the morning someone
 * returns after a month away.
 */
class ReadingNudgeTest {

    private val backoff = 7

    private fun decide(
        // A plan being kept, rather than the one plan's id: plans are plural now.
        plan: Boolean = true,
        lastReadOn: String = "2026-09-08",
        today: String = "2026-09-09",
        quiet: Boolean = false,
        unanswered: Int = 0,
    ) = decideReadingNudge(plan, lastReadOn, today, quiet, unanswered, backoff)

    @Test
    fun `no plan means no nudge, however long the setting has been on`() {
        assertEquals(ReadingNudge.SILENT, decide(plan = false, unanswered = 0))
        assertEquals(ReadingNudge.SILENT, decide(plan = false, unanswered = 99))
    }

    @Test
    fun `a day already read is not asked about`() {
        assertEquals(ReadingNudge.SILENT, decide(lastReadOn = "2026-09-09", today = "2026-09-09"))
    }

    @Test
    fun `quiet hours stay quiet`() {
        assertEquals(ReadingNudge.SILENT, decide(quiet = true))
    }

    /**
     * The ordering that matters: a reader whose quiet hours cover the reminder
     * time would otherwise be "stopped for not responding" to notifications
     * that were never sent to them.
     */
    @Test
    fun `a quiet night is not counted as a nudge that was ignored`() {
        assertEquals(ReadingNudge.SILENT, decide(quiet = true, unanswered = backoff + 3))
    }

    @Test
    fun `it sends while it is still being answered`() {
        assertEquals(ReadingNudge.SEND, decide(unanswered = 0))
        assertEquals(ReadingNudge.SEND, decide(unanswered = backoff - 1))
    }

    @Test
    fun `a week of being ignored stops it, once`() {
        assertEquals(ReadingNudge.STOP, decide(unanswered = backoff))
        assertEquals(ReadingNudge.STOP, decide(unanswered = backoff + 1))
    }
}
