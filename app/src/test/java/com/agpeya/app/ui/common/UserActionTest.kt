package com.agpeya.app.ui.common

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class UserActionTest {
    @Test fun `duplicate taps cannot start concurrent writes`() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val action = UserAction(this)
        var writes = 0
        action.run { writes++; gate.await() }
        action.run { writes++ }
        yield()
        assertEquals(1, writes)
        assertTrue(action.busy)
        gate.complete(Unit)
        yield()
        assertFalse(action.busy)
    }
    @Test fun `failed writes remain recoverable and can be retried`() = runBlocking {
        val action = UserAction(this)
        action.run { throw java.io.IOException("disk unavailable") }
        yield()
        assertTrue(action.failed)
        assertFalse(action.busy)
        var saved = false
        action.run { saved = true }
        yield()
        assertTrue(saved)
        assertFalse(action.failed)
    }
    @Test fun `leaving a screen does not report cancellation as a save failure`() = runBlocking {
        val action = UserAction(this)
        action.run { throw CancellationException() }
        yield()
        assertFalse(action.failed)
        assertFalse(action.busy)
    }
}
