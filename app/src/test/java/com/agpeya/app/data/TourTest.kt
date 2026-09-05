package com.agpeya.app.data

import com.agpeya.app.model.Tour
import com.agpeya.app.model.TourPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * When the tour appears. Being wrong here is either a tour that never shows —
 * the bug this whole feature exists to avoid — or one that shows every launch.
 */
class TourTest {

    private fun tour(code: Int) =
        Tour(versionCode = code, version = "x", pages = listOf(TourPage()))

    private val tours = listOf(tour(58), tour(60), tour(61))

    @Test
    fun `a first install gets the newest tour`() {
        // lastSeen null and nothing to compare against: the person has never
        // seen it, so they get today's app explained rather than nothing.
        assertEquals(61, TourRepository.pending(tours, lastSeen = null, installed = 61)?.versionCode)
    }

    @Test
    fun `an upgrade gets the newest tour, not every tour in between`() {
        assertEquals(61, TourRepository.pending(tours, lastSeen = 58, installed = 61)?.versionCode)
    }

    @Test
    fun `a second launch on the same version shows nothing`() {
        assertNull(TourRepository.pending(tours, lastSeen = 61, installed = 61))
    }

    @Test
    fun `a version with no tour of its own falls back to the newest that applies`() {
        // 62 ships without a tour entry; someone landing on it from 58 still
        // gets 61's, which is the newest thing there is to say.
        assertEquals(61, TourRepository.pending(tours, lastSeen = 58, installed = 62)?.versionCode)
    }

    @Test
    fun `a tour newer than the installed build is never shown`() {
        assertNull(TourRepository.pending(listOf(tour(99)), lastSeen = null, installed = 61))
    }

    @Test
    fun `an empty tour is not shown`() {
        val empty = listOf(Tour(versionCode = 61, version = "1.7.3"))
        assertNull(TourRepository.pending(empty, lastSeen = null, installed = 61))
    }

    @Test
    fun `no tours at all is not a crash`() {
        assertNull(TourRepository.pending(emptyList(), lastSeen = null, installed = 61))
    }

    @Test
    fun `an unreadable version code shows nothing rather than guessing`() {
        assertNull(TourRepository.pending(tours, lastSeen = null, installed = 0))
    }
}
