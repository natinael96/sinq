package com.agpeya.app.data

import com.agpeya.app.model.MahletIndex
import com.agpeya.app.model.MahletKind
import com.agpeya.app.model.MahletOrder
import com.agpeya.app.model.MahletSeason
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the merged ሥርዓተ ማኅሌት.
 *
 * The old test asserted the ግጻዌ's own thirty-seven orders; that book is now a
 * source rather than an asset, folded into this corpus, so what is worth
 * asserting is that the merge held: the year is covered, the ጽጌ orders can
 * still be reached by date, and no order lost its parts on the way in.
 */
class MahletTest {

    @Serializable
    private data class MonthFile(val month: Int = 0, val orders: List<MahletOrder> = emptyList())

    private val dir: File =
        listOf("src/main/assets/content/mahlet", "app/src/main/assets/content/mahlet")
            .map(::File).first { it.isDirectory }
    private val json = Json { ignoreUnknownKeys = true }

    private val index: MahletIndex by lazy {
        json.decodeFromString(File(dir, "index.json").readText())
    }
    private val orders: List<MahletOrder> by lazy {
        index.months.flatMap {
            json.decodeFromString<MonthFile>(File(dir, "m${it.month}.json").readText()).orders
        }
    }

    @Test
    fun `the merged book is a hundred and forty-two orders, none of them empty`() {
        assertEquals(142, orders.size)
        assertEquals(2247, orders.sumOf { it.parts.size })
        assertTrue("an order has no parts", orders.all { it.parts.isNotEmpty() })
        assertTrue("a part has no verse", orders.all { o -> o.parts.all { it.verse.isNotBlank() } })
    }

    /**
     * The ግጻዌ's ማኅሌት stopped after ሚያዝያ. Reaching ጳጉሜን is the whole point of the
     * merge, so it is asserted rather than assumed.
     */
    @Test
    fun `the year is covered past ሚያዝያ`() {
        val months = orders.mapNotNull { it.month }.toSet()
        assertTrue("ግንቦት has no orders", 9 in months)
        assertTrue("ሰኔ has no orders", 10 in months)
        assertTrue("ሐምሌ has no orders", 11 in months)
        assertTrue("ነሐሴ has no orders", 12 in months)
        assertTrue("ጳጉሜን has no orders", 13 in months)
    }

    /**
     * A ጽጌ order is appointed by its date falling on a Sunday, so one without a
     * date can never be reached — the season would silently lose a week.
     */
    @Test
    fun `every dated ጽጌ order carries the date that appoints it`() {
        val tsige = orders.filter { it.season == MahletSeason.TSIGE }
        assertEquals(41, tsige.size)
        assertTrue(
            "a ጽጌ order is appointed by a date it does not carry",
            tsige.none { it.whenSunday && (it.month == null || it.day == null) },
        )
    }

    @Test
    fun `the six orders the scan does not carry keep their source`() {
        val fromGitsawe = orders.filter { it.source == "ግጻዌ" }
        assertEquals(6, fromGitsawe.size)
        assertTrue(fromGitsawe.all { it.parts.isNotEmpty() })
    }

    @Test
    fun `ids are unique, so a route reaches exactly one order`() {
        assertEquals(orders.size, orders.mapTo(mutableSetOf()) { it.id }.size)
        assertTrue(orders.all { it.kind == MahletKind.VIGIL || it.kind == MahletKind.MAHLET })
    }
}
