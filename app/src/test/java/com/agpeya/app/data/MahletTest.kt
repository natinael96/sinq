package com.agpeya.app.data

import com.agpeya.app.model.MahletIndex
import com.agpeya.app.model.MahletKind
import com.agpeya.app.model.MahletOrder
import com.agpeya.app.model.MahletSeason
import com.agpeya.app.model.MahletSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the merged ሥርዓተ ማኅሌት.
 *
 * The numbers are the measured shape of the merge and are pinned on purpose:
 * a rebuild that changes them is either a content change worth noticing or a
 * regression in the builder, and either way the diff should have to say
 * which. What matters beyond the counts is that the merge held — the year is
 * covered, the ጽጌ orders can still be reached by date, no order or edition
 * lost its text on the way in, and every edition can be traced to its post.
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
    fun `the merged corpus is two hundred and fifteen orders, none of them empty`() {
        assertEquals(215, orders.size)
        assertEquals(3386, orders.sumOf { it.parts.size })
        assertTrue("an order has no parts", orders.all { it.parts.isNotEmpty() })
        assertTrue("a part has no verse", orders.all { o -> o.parts.all { it.verse.isNotBlank() } })
    }

    /**
     * An edition is a whole alternative text of an order, read in place of the
     * book's. One with no parts is a tab onto a blank page, and one with no
     * post behind it cannot be checked against its source.
     */
    @Test
    fun `every edition has text and a post it came from`() {
        val editions = orders.flatMap { it.versions }
        assertEquals(275, editions.size)
        assertTrue("an edition has no parts", editions.all { it.parts.isNotEmpty() })
        assertTrue("an edition part has no verse", editions.all { v -> v.parts.all { it.verse.isNotBlank() } })
        assertTrue("an edition has no id", editions.all { it.id.isNotBlank() })
        assertTrue("an edition has no source post", editions.all { !it.url.isNullOrBlank() })
    }

    /**
     * Where the book has no order and the channel does, the first edition
     * stands for it — and the order has to say so, because it is not the
     * book's text and a reader comparing it to the printed page should know.
     */
    @Test
    fun `an order standing on an edition says where it came from`() {
        val fromChannel = orders.filter { it.source == MahletSource.TELEGRAM }
        assertEquals(72, fromChannel.size)
        assertTrue(fromChannel.all { it.parts.isNotEmpty() })
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

    /**
     * Down from six: the Telegram editions carry four of the orders the scan
     * alone did not. The two left are the ones nothing else has.
     */
    @Test
    fun `the two orders nothing else carries keep their source`() {
        val fromGitsawe = orders.filter { it.source == MahletSource.GITSAWE }
        assertEquals(2, fromGitsawe.size)
        assertTrue(fromGitsawe.all { it.parts.isNotEmpty() })
    }

    /**
     * An order with no day, no ጽጌ date and no computus key is one no date can
     * reach. Exactly one is allowed, by name: the feast whose printed date the
     * merge could not settle. A second is a feast that fell through the build.
     */
    @Test
    fun `every undated order is appointed by the computus, save the one the book could not date`() {
        val unreachable = orders.filter {
            it.day == null && it.season == null && it.movable == null && it.source == null
        }
        assertEquals(listOf("ተክለ ሃይማኖት ወክርስቶሰ ሰምራ"), unreachable.map { it.feast })
        val movable = orders.filter { it.movable != null }
        // Six ጽጌ weeks, ስብከት/ብርሃን/ኖላዊ with their vigils, ሆሣዕና with its
        // procession, and the seven that stand alone from ሰሙነ ሕማማት to ጰራቅሊጦስ.
        assertEquals(21, movable.size)
        assertTrue("a movable order also has a day", movable.none { it.day != null })
    }

    @Test
    fun `ids are unique, so a route reaches exactly one order`() {
        assertEquals(orders.size, orders.mapTo(mutableSetOf()) { it.id }.size)
        val known = setOf(
            MahletKind.VIGIL, MahletKind.MAHLET, MahletKind.ANGERGARI,
            MahletKind.PROCESSION, MahletKind.PRAYER, MahletKind.UNSPECIFIED,
        )
        assertTrue("an order has a kind the app cannot label", orders.all { it.kind in known })
    }
}
