package com.agpeya.app.data

import com.agpeya.app.model.TitheEntry
import com.agpeya.app.model.TitheEntryKind
import com.agpeya.app.ui.settings.formatCents
import com.agpeya.app.ui.settings.parseAmount
import org.junit.Assert.assertEquals
import org.junit.Test

class OfferingAmountBoundsTest {
    @Test fun `format and parse preserve every minor unit at the limit`() {
        assertEquals(Long.MAX_VALUE, parseAmount(formatCents(Long.MAX_VALUE, "")))
    }
    @Test fun `percentage multiplication cannot overflow a valid amount`() {
        val entry = TitheEntry(id = "large", kind = TitheEntryKind.INCOME,
            amount = Long.MAX_VALUE, date = "2026-09-16")
        assertEquals(Long.MAX_VALUE / 10, OfferingRepository.reckon(listOf(entry), 10).due)
    }
}
