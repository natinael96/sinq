package com.agpeya.app.search

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
    fun `ha families fold together`() {
        // ሀ / ሐ / ኀ / ኸ all fold to the ሀ family
        val base = AmharicSearch.fold("ሀ")
        assertEquals(base, AmharicSearch.fold("ሐ"))
        assertEquals(base, AmharicSearch.fold("ኀ"))
        assertEquals(base, AmharicSearch.fold("ኸ"))
    }

    @Test
    fun `glottal families fold together`() {
        // አ U+12A0 and ዐ U+12D0
        assertEquals(AmharicSearch.fold("አmedia".take(1)), AmharicSearch.fold("ዐ"))
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
