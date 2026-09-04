package com.agpeya.app.data

import com.agpeya.app.model.Penance
import com.agpeya.app.model.PenanceKind
import com.agpeya.app.model.PenanceProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the ቀኖና lifecycle: what counts as done, what is still owed, and when
 * the reminders must fall silent — being wrong here means nagging someone about
 * a penance already finished, or letting one lapse unfinished.
 */
class PenanceTest {

    private fun progress(amount: Int, date: String = "2026-09-01") =
        PenanceProgress(id = "p-$date-$amount", date = date, amount = amount)

    private fun penance(quota: Int, vararg done: Int) = Penance(
        id = "k1",
        kind = PenanceKind.PROSTRATIONS,
        quota = quota,
        progress = done.map { progress(it) },
    )

    @Test
    fun `done and remaining follow the records`() {
        val p = penance(40, 15, 10)
        assertEquals(25, p.done)
        assertEquals(15, p.remaining)
        assertFalse(p.settled)
    }

    @Test
    fun `a counted penance settles exactly when the quota is met`() {
        assertFalse(penance(40, 39).settled)
        assertTrue(penance(40, 40).settled)
        assertTrue(penance(40, 25, 20).settled)
    }

    @Test
    fun `overshooting the quota never reports a negative remainder`() {
        assertEquals(0, penance(40, 50).remaining)
    }

    @Test
    fun `an unmeasured penance settles on the first record`() {
        assertFalse(penance(0).settled)
        assertTrue(penance(0, 1).settled)
    }

    @Test
    fun `reminders stop once settled and never restart`() {
        assertTrue(penance(40, 10).remindsStill)
        assertFalse(penance(40, 40).remindsStill)
    }

    @Test
    fun `a disabled penance does not remind even while unfinished`() {
        val p = penance(40, 10).copy(enabled = false)
        assertFalse(p.remindsStill)
        assertFalse(p.settled)
    }
}
