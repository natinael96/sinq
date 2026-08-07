package com.agpeya.app.data

import com.agpeya.app.model.Feast
import com.agpeya.app.model.MonthlyEntry
import com.agpeya.app.model.SeasonalEntry
import com.agpeya.app.model.SubFeast
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the restructured feast / seasonal / monthly metadata: the parsed,
 * date-matchable fields must stay correct, and the cross-references between
 * mahlets → sub-feasts → feasts must resolve.
 */
class GitsaweStructureTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val dir: File =
        listOf("src/main/assets/content/gitsawe", "app/src/main/assets/content/gitsawe")
            .map(::File).first { it.isDirectory }

    private fun <T> load(file: String, ser: KSerializer<T>): List<T> =
        json.decodeFromString(ListSerializer(ser), File(dir, file).readText())

    @Test
    fun `feasts recover a date from the Amharic name`() {
        val feasts = load("feasts.json", Feast.serializer())
        assertEquals(21, feasts.size)
        assertEquals("19 fixed-date feasts", 19, feasts.count { !it.movable && it.dateKey != null })
        assertEquals("2 movable feasts", 2, feasts.count { it.movable })

        val aregawi = feasts.first { it.key == "st_aregawi_tikimt" }   // (ጥቅምት ፲፬)
        assertEquals("14-02", aregawi.dateKey)
        assertEquals(2, aregawi.monthNum)
        assertEquals(14, aregawi.day)

        val george = feasts.first { it.key == "miyazya_stGeorge" }     // (ሚያዝያ 23), Arabic digits
        assertEquals("23-08", george.dateKey)

        val easter = feasts.first { it.key == "easter" }               // movable
        assertTrue(easter.movable)
        assertNull(easter.dateKey)
    }

    @Test
    fun `seasonal entries are all classified with a season`() {
        val seasonal = load("seasonal-gitsawe.json", SeasonalEntry.serializer())
        assertEquals(43, seasonal.size)
        assertTrue("every seasonal entry has a season", seasonal.all { it.season != null })
        val nineveh = seasonal.first { it.raw == "01-neneweTsom" }
        assertEquals("neneweTsom", nineveh.season)
        assertEquals(1, nineveh.week)
    }

    @Test
    fun `monthly entries parse a day-span or an nth-Sunday`() {
        val monthly = load("monthly-gitsawe.json", MonthlyEntry.serializer())
        assertEquals("duplicate dropped", 9, monthly.size)

        val yohannes = monthly.first { it.raw == "ዘመስከረም ዮሐንስ: ሰንበት" }
        assertEquals("meskerem", yohannes.month)
        assertEquals(1, yohannes.fromDay)
        assertEquals(8, yohannes.toDay)

        val hidar4 = monthly.first { it.raw == "ዘኅድር-፬" }
        assertEquals("hidar", hidar4.month)
        assertEquals(4, hidar4.nthSunday)

        // A range that spills into the next month is flagged.
        assertTrue(monthly.any { it.crossMonth && it.fromDay == 26 && it.toDay == 5 })
    }

    @Test
    fun `mahlets and sub-feasts reference existing parents`() {
        val feastKeys = load("feasts.json", Feast.serializer()).map { it.key }.toSet()
        val subs = load("sub-feasts.json", SubFeast.serializer())
        val subKeys = subs.map { it.key }.toSet()

        assertTrue("every sub-feast points at a real feast", subs.all { it.feast in feastKeys })

        // Mahlets carry a subFeast link (read loosely to avoid coupling to the model).
        val mahletSubs = json.parseToJsonElement(File(dir, "mahlets.json").readText())
            .let { it as kotlinx.serialization.json.JsonArray }
            .mapNotNull { it.jsonObject["subFeast"]?.jsonPrimitive?.content }
        assertTrue("every mahlet subFeast exists", mahletSubs.all { it in subKeys })
    }
}
