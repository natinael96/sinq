package com.agpeya.app.data

import com.agpeya.app.model.TourContent
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Guards the bundled tour: bilingual, uniquely keyed, and its routes are real. */
class TourDataTest {

    private val json = Json { ignoreUnknownKeys = true }
    private fun find(rel: String): File =
        listOf(File(rel), File("app/$rel")).first { it.isFile }

    private val content: TourContent by lazy {
        json.decodeFromString(find("src/main/assets/content/tour/tour.json").readText())
    }

    /** Every route string MainActivity registers, read from the source. */
    private val registered: Set<String> by lazy {
        val src = find("src/main/java/com/agpeya/app/MainActivity.kt").readText()
        Regex("""composable\(\s*"([^"{}]+)"""").findAll(src).map { it.groupValues[1] }.toSet() +
            Regex("""route\s*=\s*"([^"{?]+)""").findAll(src).map { it.groupValues[1] }.toSet()
    }

    @Test
    fun `decodes with at least one tour`() {
        assertTrue(content.tours.isNotEmpty())
    }

    @Test
    fun `version codes are unique`() {
        val codes = content.tours.map { it.versionCode }
        assertEquals(codes.size, codes.toSet().size)
        assertTrue(codes.all { it > 0 })
    }

    @Test
    fun `every page carries both languages`() {
        for (t in content.tours) {
            assertTrue("${t.version} has no pages", t.pages.isNotEmpty())
            for ((i, p) in t.pages.withIndex()) {
                assertTrue("${t.version} p$i title am", p.title.am.isNotBlank())
                assertTrue("${t.version} p$i title en", p.title.en.isNotBlank())
                assertTrue("${t.version} p$i body am", p.body.am.isNotBlank())
                assertTrue("${t.version} p$i body en", p.body.en.isNotBlank())
            }
        }
    }

    @Test
    fun `every route a page offers is one the app registers`() {
        // A tour page whose button goes nowhere is worse than no button.
        for (t in content.tours) {
            for (p in t.pages) {
                if (p.route.isBlank()) continue
                assertTrue(
                    "${t.version}: route '${p.route}' is not registered (have: ${registered.sorted().take(8)})",
                    p.route in registered,
                )
            }
        }
    }

    @Test
    fun `a page offering a route also labels the button`() {
        for (t in content.tours) {
            for (p in t.pages) {
                if (p.route.isBlank()) continue
                assertTrue("${t.version}: ${p.route} has no action label", !p.action.isBlank)
            }
        }
    }
}
