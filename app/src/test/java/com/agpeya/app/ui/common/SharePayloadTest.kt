package com.agpeya.app.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class SharePayloadTest {
    @Test
    fun `plain text keeps source title date and body`() {
        val payload = SharePayload(
            body = "Passage text  ",
            kicker = "Gitsawe of the day",
            title = "Luke 4:17–23",
            dateLabel = "Hamle 19",
        )

        assertEquals(
            "Gitsawe of the day\nLuke 4:17–23 — Hamle 19\n\nPassage text",
            payload.asText(),
        )
    }

    @Test
    fun `plain text does not repeat an identical kicker and title`() {
        val payload = SharePayload(body = "Text", kicker = "Synaxarium", title = "Synaxarium")

        assertEquals("Synaxarium\n\nText", payload.asText())
    }
}
