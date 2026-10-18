package com.sinq.app.search

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureNanoTime

class AmharicSearchBenchmarkTest {

    private val labels = AmharicSearch.Labels(
        psalter = "መዝሙረ ዳዊት",
        psalterGeez = "መዝሙረ ዳዊት (ግዕዝ)",
        scripture = "ቅዱስ መጽሐፍ",
        synaxarium = "ስንክሳር",
        wudase = "ውዳሴ ማርያም",
    )

    @Test
    fun `benchmark search latency on sample corpus`() {
        // Prepare 500 synthetic liturgical documents representing a loaded corpus partition
        val sampleWords = listOf("ጸሎት", "ፀሎት", "ሰላም", "ሠላም", "እግዚአብሔር", "ማርያም", "ቅዱስ", "በረከት", "ሃይማኖት", "ሐይማኖት")
        val docs = (1..500).map { i ->
            val w1 = sampleWords[i % sampleWords.size]
            val w2 = sampleWords[(i * 3) % sampleWords.size]
            val text = "ምዕራፍ $i ይህ የከበረ የቅዱሳን ቃል ነው $w1 እንዲሁም $w2 በረከተ ሥጋ ወነፍስ ያሳድርብን አሜን።"
            AmharicSearch.Doc(
                source = AmharicSearch.Source.BOOK,
                targetId = "bench_$i",
                targetIndex = i,
                title = "ክፍል $i $w1",
                route = "book/$i",
                haystack = text,
                folded = AmharicSearch.fold(text),
            )
        }

        // Measure first search latency (cold query)
        val query = "ጸሎት"
        val coldNanos = measureNanoTime {
            val results = AmharicSearch.searchDocs(
                docs = docs,
                needle = AmharicSearch.fold(query),
                matchLen = query.length,
                labels = labels,
                rawQuery = query,
            )
            assertTrue("Expected hits in synthetic corpus", results.isNotEmpty())
        }

        // Measure warm search latency (subsequent queries)
        var warmTotalNanos = 0L
        val iterations = 50
        for (k in 1..iterations) {
            val q = sampleWords[k % sampleWords.size]
            warmTotalNanos += measureNanoTime {
                AmharicSearch.searchDocs(
                    docs = docs,
                    needle = AmharicSearch.fold(q),
                    matchLen = q.length,
                    labels = labels,
                    rawQuery = q,
                )
            }
        }
        val avgWarmMs = (warmTotalNanos / iterations) / 1_000_000.0
        val coldMs = coldNanos / 1_000_000.0

        println("=== AmharicSearch Latency Benchmark ===")
        println("Corpus size: ${docs.size} documents")
        println("Cold search latency: ${"%.3f".format(coldMs)} ms")
        println("Warm average search latency (50 iterations): ${"%.3f".format(avgWarmMs)} ms")

        // In-memory or pre-folded search over 500 docs must easily complete under 50ms on any modern JVM / device
        assertTrue("Cold search took too long: $coldMs ms", coldMs < 100.0)
        assertTrue("Warm search took too long: $avgWarmMs ms", avgWarmMs < 20.0)
    }
}
