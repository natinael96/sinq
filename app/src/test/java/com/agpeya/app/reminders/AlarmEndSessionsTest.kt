package com.agpeya.app.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class AlarmEndSessionsTest {

    @Test
    fun `only the first terminal action wins`() {
        val session = UUID.randomUUID().toString()
        AlarmEndSessions.begin(session)

        assertEquals(AlarmEndSessions.End.CURRENT, AlarmEndSessions.claim(session))
        assertEquals(AlarmEndSessions.End.ALREADY, AlarmEndSessions.claim(session))
    }

    @Test
    fun `an older ring ends as stale, and the current ring still ends as itself`() {
        // Stale rather than refused. Refusing threw away the older hour's
        // "done?" follow-up along with its claim on the notification, and only
        // the notification belongs to the newer ring.
        val old = UUID.randomUUID().toString()
        val current = UUID.randomUUID().toString()
        AlarmEndSessions.begin(old)
        AlarmEndSessions.begin(current)

        assertEquals(AlarmEndSessions.End.STALE, AlarmEndSessions.claim(old))
        assertEquals(AlarmEndSessions.End.CURRENT, AlarmEndSessions.claim(current))
    }

    @Test
    fun `a stale ring still ends only once`() {
        val old = UUID.randomUUID().toString()
        AlarmEndSessions.begin(old)
        AlarmEndSessions.begin(UUID.randomUUID().toString())

        assertEquals(AlarmEndSessions.End.STALE, AlarmEndSessions.claim(old))
        assertEquals(AlarmEndSessions.End.ALREADY, AlarmEndSessions.claim(old))
    }

    @Test
    fun `an ending with no ring in memory is the current one`() {
        // The process died between the ring and the answer, and the session id
        // in the intent is all that is left of it. It must still be able to
        // take the notification down.
        assertEquals(
            AlarmEndSessions.End.CURRENT,
            AlarmEndSessions.claim(UUID.randomUUID().toString()),
        )
    }

    @Test
    fun `concurrent terminal actions have exactly one winner`() {
        val session = UUID.randomUUID().toString()
        AlarmEndSessions.begin(session)
        val workers = 16
        val ready = CountDownLatch(workers)
        val start = CountDownLatch(1)
        val done = CountDownLatch(workers)
        val winners = AtomicInteger()
        val pool = Executors.newFixedThreadPool(workers)

        repeat(workers) {
            pool.execute {
                ready.countDown()
                start.await()
                if (AlarmEndSessions.claim(session) != AlarmEndSessions.End.ALREADY) {
                    winners.incrementAndGet()
                }
                done.countDown()
            }
        }
        assertTrue(ready.await(2, TimeUnit.SECONDS))
        start.countDown()
        assertTrue(done.await(2, TimeUnit.SECONDS))
        pool.shutdownNow()

        org.junit.Assert.assertEquals(1, winners.get())
    }
}
