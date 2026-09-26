package com.agpeya.app.data

import com.agpeya.app.model.Tour
import com.agpeya.app.model.TourPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TourRepositoryTest {

    private fun tour(code: Int) = Tour(versionCode = code, pages = listOf(TourPage()))
    private val tours = listOf(tour(61), tour(62))

    @Test
    fun `a first install gets the newest tour`() {
        assertEquals(62, TourRepository.pending(tours, null, 66)?.versionCode)
    }

    @Test
    fun `the tour for this release is shown once`() {
        assertEquals(62, TourRepository.pending(tours, 61, 62)?.versionCode)
    }

    @Test
    fun `having seen it, the same release does not show it again`() {
        assertNull(TourRepository.pending(tours, 62, 62))
    }

    /**
     * The bug this guards: releases 63-66 added no tour of their own, and every
     * one of them re-presented the 1.7.4 tour as though it were new.
     */
    @Test
    fun `a release with no tour of its own shows nothing`() {
        assertNull(TourRepository.pending(tours, 62, 66))
        assertNull(TourRepository.pending(tours, 65, 66))
    }

    @Test
    fun `a newer tour is shown even after an older one was seen`() {
        val withNew = tours + tour(66)
        assertEquals(66, TourRepository.pending(withNew, 62, 66)?.versionCode)
    }

    @Test
    fun `a tour from the future is not shown early`() {
        val withNew = tours + tour(70)
        assertEquals(62, TourRepository.pending(withNew, null, 66)?.versionCode)
    }
}
