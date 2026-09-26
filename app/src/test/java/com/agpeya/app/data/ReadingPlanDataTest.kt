package com.agpeya.app.data

import com.agpeya.app.model.ReadingPlanContent
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the generated plans against the bundled Bible: every reading must
 * point at a real book and a chapter that exists, and each plan must cover the
 * corpus exactly once — a gap means a book silently never read, a duplicate
 * means a day wasted re-reading.
 */
class ReadingPlanDataTest {

    private val json = Json { ignoreUnknownKeys = true }
    private fun find(vararg rel: String): File =
        rel.map(::File).firstOrNull { it.isFile }
            ?: rel.map { File("app/$it") }.first { it.isFile }

    private val content: ReadingPlanContent by lazy {
        json.decodeFromString(find("src/main/assets/content/reading/plans.json").readText())
    }

    /** slug → chapter count, straight from the bundled canon. */
    private val chapters: Map<String, Int> by lazy {
        val canon = json.parseToJsonElement(
            find("src/main/assets/content/bible/canon.json").readText(),
        ).let { kotlinx.serialization.json.Json.parseToJsonElement(it.toString()) }
        val meta = kotlinx.serialization.json.Json.parseToJsonElement(
            find("src/main/assets/content/bible/am-1980/meta.json").readText(),
        ).let { it as kotlinx.serialization.json.JsonObject }
        val byId = (meta["books"] as kotlinx.serialization.json.JsonArray).associate { b ->
            val o = b as kotlinx.serialization.json.JsonObject
            o["id"].toString().trim('"') to o["chapters"].toString().trim('"').toInt()
        }
        (canon as kotlinx.serialization.json.JsonArray).mapNotNull { c ->
            val o = c as kotlinx.serialization.json.JsonObject
            val id = o["id"].toString().trim('"')
            val slug = o["slug"].toString().trim('"')
            byId[id]?.let { slug to it }
        }.toMap()
    }

    @Test
    fun `all three tracks are present`() {
        val ids = content.plans.map { it.id }.toSet()
        assertTrue("annual missing", "annual" in ids)
        assertTrue("half missing", "half" in ids)
        assertTrue("psalter missing", "psalter" in ids)
    }

    @Test
    fun `the annual track is a year and the half track is six months`() {
        assertEquals(360, content.plans.first { it.id == "annual" }.days)
        assertEquals(180, content.plans.first { it.id == "half" }.days)
    }

    @Test
    fun `every reading points at a real book and an existing chapter`() {
        for (plan in content.plans) {
            for (day in plan.readings) {
                for (r in day.r) {
                    val max = chapters[r.b]
                    assertTrue("${plan.id} day ${day.d}: unknown book '${r.b}'", max != null)
                    assertTrue("${plan.id} day ${day.d}: ${r.b} ${r.c}..${r.to} out of range (max $max)",
                        r.c in 1..max!! && r.to in r.c..max)
                }
            }
        }
    }

    @Test
    fun `each track covers its own corpus exactly once`() {
        for (plan in content.plans) {
            val seen = mutableListOf<Pair<String, Int>>()
            for (day in plan.readings) for (r in day.r) for (c in r.chapters) seen += r.b to c
            val dupes = seen.groupingBy { it }.eachCount().filterValues { it > 1 }
            assertTrue("${plan.id} repeats ${dupes.keys.take(3)}", dupes.isEmpty())
            // The ዳዊት track reads the Psalter; the others read the whole canon.
            val expected = if (plan.id == "psalter") 150 else 1610
            assertEquals("${plan.id} chapter total", expected, seen.size)
        }
    }

    @Test
    fun `the reading tracks read the whole canon, the Gospels and the Psalter included`() {
        // The plan used to skip the New Testament, because the ግጻዌ carries most
        // of it, and the Psalter, because it is prayed in the hours. That made
        // it a supplement: someone who finished it had still never read Matthew
        // straight through. It reads everything now, and overlapping the day's
        // ግጻዌ is the accepted cost.
        for (plan in content.plans.filter { it.id != "psalter" }) {
            val books = plan.readings.flatMap { day -> day.r.map { it.b } }.toSet()
            assertTrue("${plan.id}: the Psalter is missing", "psalms" in books)
            assertTrue("${plan.id}: Matthew is missing", "matthew" in books)
            assertTrue("${plan.id}: Revelation is missing", "revelation" in books)
        }
    }

    @Test
    fun `the psalter track is one psalm a day, all 150`() {
        val psalter = content.plans.first { it.id == "psalter" }
        assertEquals(150, psalter.days)
        assertEquals(150, psalter.readings.size)
        psalter.readings.forEachIndexed { i, day ->
            assertEquals("day ${day.d} is not a single reading", 1, day.r.size)
            val r = day.r.single()
            assertEquals("day ${day.d} is not from the Psalter", "psalms", r.b)
            // A psalm is a unit of prayer, never split and never doubled up.
            assertEquals("day ${day.d} spans more than one psalm", r.c, r.to)
            assertEquals("psalms are out of order", i + 1, r.c)
        }
    }

    @Test
    fun `day numbers run 1 to N with no gaps`() {
        for (plan in content.plans) {
            assertEquals(plan.id, (1..plan.days).toList(), plan.readings.map { it.d })
        }
    }

    @Test
    fun `no day is punishingly long`() {
        // A day that runs far past its budget is where people quit.
        //
        // Measured against the plan's own average rather than a fixed chapter
        // count: the packer budgets verses, so a long day is a run of short
        // chapters and a fixed cap would fail the moment the corpus changed —
        // which is exactly what it did when the plan grew to the whole canon.
        for (plan in content.plans) {
            val total = plan.readings.sumOf { day -> day.r.sumOf { it.chapterCount } }
            val average = total.toDouble() / plan.days
            for (day in plan.readings) {
                val n = day.r.sumOf { it.chapterCount }
                assertTrue(
                    "${plan.id} day ${day.d} has $n chapters, over 4x the ${"%.1f".format(average)} average",
                    n <= average * 4,
                )
            }
        }
    }

    @Test
    fun `the plan declares that it carries the day's gitsawe`() {
        assertTrue(content.plans.all { it.withGitsawe })
    }
}
