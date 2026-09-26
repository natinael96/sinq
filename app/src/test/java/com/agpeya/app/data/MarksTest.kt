package com.agpeya.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * How a painted run reads back.
 *
 * A highlight is stored one verse at a time, because that is how it is applied,
 * but somebody who painted ሉቃስ ፲፥፴፰–፵፪ made one mark. A list that shows it as
 * five rows is a list of the storage rather than of what they did.
 */
class MarksTest {

    @Test
    fun `consecutive verses collapse into one run`() {
        assertEquals(listOf(38..42), MarksRepository.runs(listOf(38, 39, 40, 41, 42)))
    }

    @Test
    fun `a gap starts a new run`() {
        assertEquals(listOf(1..2, 7..7, 9..10), MarksRepository.runs(listOf(9, 1, 7, 2, 10)))
    }

    @Test
    fun `one verse is a run of one`() {
        assertEquals(listOf(3..3), MarksRepository.runs(listOf(3)))
    }

    @Test
    fun `a repeated verse is still one verse`() {
        assertEquals(listOf(4..5), MarksRepository.runs(listOf(4, 5, 4)))
    }

    @Test
    fun `nothing painted is no runs at all`() {
        assertEquals(emptyList<IntRange>(), MarksRepository.runs(emptyList()))
    }
}
