package com.sinq.app.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CorpusIndexerTest {

    private val contentDir: File =
        listOf("src/main/assets", "app/src/main/assets")
            .map(::File).first { it.isDirectory }

    private val fileLoader = CorpusIndexer.AssetLoader { path ->
        File(contentDir, path).inputStream()
    }

    @Test
    fun `indexAll extracts all corpus documents from bundled assets`() {
        val docs = CorpusIndexer.indexAll(fileLoader)
        val bySource = docs.groupBy { it.source }

        // Verify each source is represented and matches corpus db
        assertEquals(setOf("SCRIPTURE", "SYNAXARIUM", "BOOK", "WUDASE", "HOUR", "PSALTER"), bySource.keys)

        assertEquals(1188, bySource["SCRIPTURE"]?.size)
        assertEquals(108, bySource["BOOK"]?.size)
        assertEquals(181, bySource["HOUR"]?.size)
        assertEquals(300, bySource["PSALTER"]?.size)
        assertEquals(10, bySource["WUDASE"]?.size)
        assertEquals(1242, bySource["SYNAXARIUM"]?.size)

        // Verify total document count matches the target corpus size (3,029 docs)
        assertEquals(3029, docs.size)

        // Verify documents have non-blank fields
        val blankDocs = docs.filter { it.folded.isBlank() }
        if (blankDocs.isNotEmpty()) {
            val sample = blankDocs.take(5).map { "${it.source}: ${it.target_id} - ${it.title} (haystack len: ${it.haystack.length})" }
            org.junit.Assert.fail("Found ${blankDocs.size} blank folded docs: $sample")
        }
        docs.forEach { doc ->
            assertFalse("source cannot be blank", doc.source.isBlank())
            assertFalse("title cannot be blank", doc.title.isBlank())
            assertFalse("route cannot be blank", doc.route.isBlank())
            assertFalse("haystack cannot be blank", doc.haystack.isBlank())
            assertFalse("folded cannot be blank", doc.folded.isBlank())
        }
    }
}
