package com.sinq.app.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AmharicSearchTest {

    @Test
    fun `sa families fold together`() {
        // ሰላም (ሰ U+1230) and ሠላም (ሠ U+1220) should fold to the same string
        assertEquals(AmharicSearch.fold("ሰላም"), AmharicSearch.fold("ሠላም"))
    }

    @Test
    fun `tsadi families fold together`() {
        // ጸሎት (ጸ U+1338) and ፀሎት (ፀ U+1340)
        assertEquals(AmharicSearch.fold("ጸሎት"), AmharicSearch.fold("ፀሎት"))
    }

    @Test
    fun `ha families fold together including 1st and 4th order`() {
        val base = AmharicSearch.fold("ሀ")
        assertEquals(base, AmharicSearch.fold("ሐ"))
        assertEquals(base, AmharicSearch.fold("ኀ"))
        assertEquals(base, AmharicSearch.fold("ኸ"))
        assertEquals(base, AmharicSearch.fold("ሃ"))
        assertEquals(base, AmharicSearch.fold("ሓ"))
        assertEquals(base, AmharicSearch.fold("ኃ"))
        assertEquals(base, AmharicSearch.fold("ኻ"))
    }

    @Test
    fun `glottal families fold together including 1st and 4th order`() {
        val base = AmharicSearch.fold("አ")
        assertEquals(base, AmharicSearch.fold("ዐ"))
        assertEquals(base, AmharicSearch.fold("ዓ"))
        assertEquals(base, AmharicSearch.fold("ኣ"))
    }

    @Test
    fun `labialized velars fold to canonical forms`() {
        assertEquals("ኮ", AmharicSearch.fold("ኰ"))
        assertEquals("ጎ", AmharicSearch.fold("ጐ"))
        assertEquals("ቆ", AmharicSearch.fold("ቈ"))
        assertEquals("ሆ", AmharicSearch.fold("ኈ"))
        assertEquals("ዃ", AmharicSearch.fold("ኋ"))
    }

    @Test
    fun `ethiopic punctuation is stripped and wordspace becomes space`() {
        assertEquals("ሰላም ለኪ", AmharicSearch.fold("ሰላም፡ለኪ"))
        assertEquals("ጸሎት እግዚአብሄር", AmharicSearch.fold("ፀሎት።፣፤፥፦፧፨ እግዚአብሔር"))
    }

    @Test
    fun `geez numerals normalize to arabic numerals`() {
        assertEquals(AmharicSearch.fold("መዝሙር 23"), AmharicSearch.fold("መዝሙር ፳፫"))
        assertEquals(AmharicSearch.fold("ምዕራፍ 50"), AmharicSearch.fold("ምዕራፍ ፶"))
        assertEquals(AmharicSearch.fold("መዝሙር 150"), AmharicSearch.fold("መዝሙር ፻፶"))
    }

    @Test
    fun `vowel order is preserved within a family`() {
        // ሱ (U+1231) folds to itself; ሡ (U+1221) folds to ሱ — same order (u), different family
        assertEquals(AmharicSearch.fold("ሱ"), AmharicSearch.fold("ሡ"))
        // but ሰ (order 0) and ሱ (order 1) must NOT fold together
        assert(AmharicSearch.fold("ሰ") != AmharicSearch.fold("ሱ"))
    }

    @Test
    fun `non-homophone letters are unchanged`() {
        assertEquals("በዘ", AmharicSearch.fold("በዘ"))
    }

    // ---- typed references ------------------------------------------------

    @Test
    fun `a reference splits into its book and number`() {
        assertEquals("ሉቃስ" to 10, AmharicSearch.parseReference("ሉቃስ 10"))
        assertEquals("ሉቃስ" to 10, AmharicSearch.parseReference("ሉቃስ፥10"))
        assertEquals("" to 23, AmharicSearch.parseReference("23"))
        assertEquals("Luke" to 4, AmharicSearch.parseReference("  Luke 4  "))
    }

    @Test
    fun `plain words are not references`() {
        assertEquals(null, AmharicSearch.parseReference("ሰላም"))
        assertEquals(null, AmharicSearch.parseReference(""))
    }

    @Test
    fun `bare and psalm-prefixed numbers mean a psalm`() {
        assertTrue(AmharicSearch.looksLikePsalm(""))
        assertTrue(AmharicSearch.looksLikePsalm("መዝሙር"))
        assertTrue(AmharicSearch.looksLikePsalm("ps"))
        assertTrue(AmharicSearch.looksLikePsalm("Psalm"))
        assertFalse(AmharicSearch.looksLikePsalm("ሉቃስ"))
    }
}
