package com.agpeya.app.data

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import com.agpeya.app.model.Feast
import com.agpeya.app.model.Mahlet
import com.agpeya.app.model.SubFeast
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * ሥርዓተ ማኅሌት, and the joins that date it.
 *
 * The book files an order of service under a sub-feast and a sub-feast under a
 * feast, so putting one on a day is a join rather than a new calendar: nineteen
 * feasts carry a fixed dateKey, the ጽጌ weeks are the Sunday ordinal
 * SundayCycleCalendar already computes, and ትንሣኤ is ባሕረ ሓሳብ.
 */
class MahletTest {

    private val dir: File =
        listOf("src/main/assets/content/gitsawe", "app/src/main/assets/content/gitsawe")
            .map(::File).first { it.isDirectory }
    private val json = Json { ignoreUnknownKeys = true }

    private inline fun <reified T> load(file: String, serializer: kotlinx.serialization.KSerializer<T>): List<T> =
        json.decodeFromString(ListSerializer(serializer), File(dir, file).readText())

    private val feasts by lazy { load("feasts.json", Feast.serializer()) }
    private val subs by lazy { load("sub-feasts.json", SubFeast.serializer()) }
    private val mahlets by lazy { load("mahlets.json", Mahlet.serializer()) }

    @Test
    fun `every order of service reaches a feast through its sub-feast`() {
        val subKeys = subs.associateBy { it.key }
        val feastKeys = feasts.mapTo(mutableSetOf()) { it.key }
        val orphans = mahlets.filter { m ->
            val sub = subKeys[m.subFeast]
            sub == null || sub.feast !in feastKeys
        }
        assertTrue("orphaned: ${orphans.map { it.title }}", orphans.isEmpty())
    }

    @Test
    fun `the book is thirty-seven orders and seven hundred and thirty-two parts`() {
        assertEquals(37, mahlets.size)
        assertEquals(732, mahlets.sumOf { it.detail.size })
        assertTrue(mahlets.all { it.detail.isNotEmpty() })
    }

    @Test
    fun `three sub-feasts have no order yet, and they are named`() {
        val have = mahlets.mapNotNull { it.subFeast }.toSet()
        assertEquals(
            setOf("1_1st_week", "st_estifanos_tir_negs", "st_gebriel_tahsas_eve"),
            subs.mapTo(mutableSetOf()) { it.key } - have,
        )
    }

    @Test
    fun `all but the movable feasts can be dated by their own key`() {
        val subKeys = subs.associateBy { it.key }
        val byKey = feasts.associateBy { it.key }
        val movable = mahlets.count { m ->
            byKey[subKeys[m.subFeast]?.feast]?.movable == true
        }
        assertEquals(31, mahlets.size - movable)
        assertEquals(6, movable)
    }

    @Test
    fun `a season's week is read out of the sub-feast key`() {
        assertEquals(3, GitsaweRepository.seasonWeekOf("1_3rd_week"))
        assertEquals(6, GitsaweRepository.seasonWeekOf("1_6th_week"))
        assertEquals(null, GitsaweRepository.seasonWeekOf("st_aregawi_tikmt_negs"))
    }

    @Test
    fun `ኅዳር ፮ carries two feasts and both are kept`() {
        val onSix = feasts.filter { it.dateKey == "06-03" }
        assertEquals(2, onSix.size)
        assertEquals(
            listOf("ቁስቋም ማርያም (ህዳር ፮)", "ቅዱስ ጊዮርጊስ (ህዳር ፮)"),
            onSix.map { it.amharicName }.sorted(),
        )
    }
}
