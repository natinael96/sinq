package com.sinq.app.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AmharicSearchRankingTest {

    private val labels = AmharicSearch.Labels(
        psalter = "መዝሙረ ዳዊት",
        psalterGeez = "መዝሙረ ዳዊት (ግዕዝ)",
        scripture = "ቅዱስ መጽሐፍ",
        synaxarium = "ስንክሳር",
        wudase = "ውዳሴ ማርያም",
    )

    @Test
    fun `exact match ranks higher than folded match and sets matchQuality`() {
        val docExact = AmharicSearch.Doc(
            source = AmharicSearch.Source.BOOK,
            targetId = "exact_doc",
            targetIndex = 1,
            title = "ስለ ጸሎት",
            route = "book/1",
            haystack = "ይህ ስለ ጸሎት የተጻፈ ትምህርት ነው።",
            folded = AmharicSearch.fold("ይህ ስለ ጸሎት የተጻፈ ትምህርት ነው።"),
        )
        val docFolded = AmharicSearch.Doc(
            source = AmharicSearch.Source.BOOK,
            targetId = "folded_doc",
            targetIndex = 2,
            title = "ስለ ፀሎት",
            route = "book/2",
            haystack = "ይህ ስለ ፀሎት የተጻፈ ትምህርት ነው።",
            folded = AmharicSearch.fold("ይህ ስለ ፀሎት የተጻፈ ትምህርት ነው።"),
        )

        // Querying with exact "ጸሎት"
        val queryExact = "ጸሎት"
        val resultsExact = AmharicSearch.searchDocs(
            docs = listOf(docFolded, docExact),
            needle = AmharicSearch.fold(queryExact),
            matchLen = queryExact.length,
            labels = labels,
            rawQuery = queryExact,
        )

        assertEquals(2, resultsExact.size)
        // docExact must rank first
        assertEquals("exact_doc", resultsExact[0].targetId)
        assertEquals(AmharicSearch.MatchQuality.EXACT, resultsExact[0].matchQuality)
        assertEquals("folded_doc", resultsExact[1].targetId)
        assertEquals(AmharicSearch.MatchQuality.FOLDED, resultsExact[1].matchQuality)
        assertTrue(resultsExact[0].score > resultsExact[1].score)

        // Querying with variant "ፀሎት"
        val queryVariant = "ፀሎት"
        val resultsVariant = AmharicSearch.searchDocs(
            docs = listOf(docExact, docFolded),
            needle = AmharicSearch.fold(queryVariant),
            matchLen = queryVariant.length,
            labels = labels,
            rawQuery = queryVariant,
        )

        assertEquals(2, resultsVariant.size)
        // Now docFolded matches exactly and must rank first
        assertEquals("folded_doc", resultsVariant[0].targetId)
        assertEquals(AmharicSearch.MatchQuality.EXACT, resultsVariant[0].matchQuality)
        assertEquals("exact_doc", resultsVariant[1].targetId)
        assertEquals(AmharicSearch.MatchQuality.FOLDED, resultsVariant[1].matchQuality)
        assertTrue(resultsVariant[0].score > resultsVariant[1].score)
    }

    @Test
    fun `variant spellings across Ha families rank exact above folded`() {
        val doc1 = AmharicSearch.Doc(
            source = AmharicSearch.Source.BOOK,
            targetId = "ha1",
            targetIndex = 1,
            title = "ሀይማኖት",
            route = "book/ha1",
            haystack = "ኦርቶዶክሳዊት ሀይማኖት",
            folded = AmharicSearch.fold("ኦርቶዶክሳዊት ሀይማኖት"),
        )
        val doc2 = AmharicSearch.Doc(
            source = AmharicSearch.Source.BOOK,
            targetId = "ha2",
            targetIndex = 2,
            title = "ሐይማኖት",
            route = "book/ha2",
            haystack = "ኦርቶዶክሳዊት ሐይማኖት",
            folded = AmharicSearch.fold("ኦርቶዶክሳዊት ሐይማኖት"),
        )
        val doc3 = AmharicSearch.Doc(
            source = AmharicSearch.Source.BOOK,
            targetId = "ha3",
            targetIndex = 3,
            title = "ኀይማኖት",
            route = "book/ha3",
            haystack = "ኦርቶዶክሳዊት ኀይማኖት",
            folded = AmharicSearch.fold("ኦርቶዶክሳዊት ኀይማኖት"),
        )

        val results = AmharicSearch.searchDocs(
            docs = listOf(doc1, doc2, doc3),
            needle = AmharicSearch.fold("ሐይማኖት"),
            matchLen = 7,
            labels = labels,
            rawQuery = "ሐይማኖት",
        )

        assertEquals(3, results.size)
        assertEquals("ha2", results[0].targetId)
        assertEquals(AmharicSearch.MatchQuality.EXACT, results[0].matchQuality)
        assertEquals(AmharicSearch.MatchQuality.FOLDED, results[1].matchQuality)
        assertEquals(AmharicSearch.MatchQuality.FOLDED, results[2].matchQuality)
    }

    @Test
    fun `glottal and sibilant variants match correctly`() {
        val docAlem = AmharicSearch.Doc(
            source = AmharicSearch.Source.BOOK,
            targetId = "alem1",
            targetIndex = 1,
            title = "ዓለም",
            route = "book/alem1",
            haystack = "ፍቅረ ዓለም",
            folded = AmharicSearch.fold("ፍቅረ ዓለም"),
        )
        val docAlemVariant = AmharicSearch.Doc(
            source = AmharicSearch.Source.BOOK,
            targetId = "alem2",
            targetIndex = 2,
            title = "አለም",
            route = "book/alem2",
            haystack = "ፍቅረ አለም",
            folded = AmharicSearch.fold("ፍቅረ አለም"),
        )

        val results = AmharicSearch.searchDocs(
            docs = listOf(docAlemVariant, docAlem),
            needle = AmharicSearch.fold("ዓለም"),
            matchLen = 3,
            labels = labels,
            rawQuery = "ዓለም",
        )

        assertEquals(2, results.size)
        assertEquals("alem1", results[0].targetId)
        assertEquals(AmharicSearch.MatchQuality.EXACT, results[0].matchQuality)
        assertEquals("alem2", results[1].targetId)
        assertEquals(AmharicSearch.MatchQuality.FOLDED, results[1].matchQuality)
    }
}
