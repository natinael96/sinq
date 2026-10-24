package com.sinq.app.ui.psalter

import org.junit.Assert.assertEquals
import org.junit.Test

class PsalterModeTest {

    @Test
    fun `psalter language toggle cycles amharic then geez then english`() {
        var mode = PsalterMode.AMHARIC
        mode = mode.next()
        assertEquals(PsalterMode.GEEZ, mode)

        mode = mode.next()
        assertEquals(PsalterMode.ENGLISH, mode)

        mode = mode.next()
        assertEquals(PsalterMode.PARALLEL_AMH_GEEZ, mode)

        mode = mode.next()
        assertEquals(PsalterMode.AMHARIC, mode)
    }
}
