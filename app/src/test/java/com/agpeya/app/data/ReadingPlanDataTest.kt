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
    fun `both tracks are present`() {
        val ids = content.plans.map { it.id }.toSet()
        assertTrue("annual missing", "annual" in ids)
        assertTrue("half missing", "half" in ids)
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
    fun `each track covers the corpus exactly once`() {
        for (plan in content.plans) {
            val seen = mutableListOf<Pair<String, Int>>()
            for (day in plan.readings) for (r in day.r) for (c in r.chapters) seen += r.b to c
            val dupes = seen.groupingBy { it }.eachCount().filterValues { it > 1 }
            assertTrue("${plan.id} repeats ${dupes.keys.take(3)}", dupes.isEmpty())
            assertEquals("${plan.id} chapter total", 1067, seen.size)
        }
    }

    @Test
    fun `psalms and the new testament are not in the plan`() {
        // The ግጻዌ already carries 88.5% of the NT and every psalm it needs;
        // duplicating them here would double a devout reader's day for nothing.
        for (plan in content.plans) {
            for (day in plan.readings) for (r in day.r) {
                assertTrue("${plan.id}: psalms should not be in the plan", r.b != "psalms")
                assertTrue("${plan.id}: matthew should not be in the plan", r.b != "matthew")
            }
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
        for (plan in content.plans) {
            val cap = if (plan.id == "annual") 8 else 14
            for (day in plan.readings) {
                val n = day.r.sumOf { it.chapterCount }
                assertTrue("${plan.id} day ${day.d} has $n chapters", n <= cap)
            }
        }
    }

    @Test
    fun `the plan declares that it carries the day's gitsawe`() {
        assertTrue(content.plans.all { it.withGitsawe })
    }
}
