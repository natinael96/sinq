package com.agpeya.app.reminders

import org.junit.Assert.assertFalse
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

        assertTrue(AlarmEndSessions.claim(session))
        assertFalse(AlarmEndSessions.claim(session))
    }

    @Test
    fun `late action from an older ring cannot end the current ring`() {
        val old = UUID.randomUUID().toString()
        val current = UUID.randomUUID().toString()
        AlarmEndSessions.begin(old)
        AlarmEndSessions.begin(current)

        assertFalse(AlarmEndSessions.claim(old))
        assertTrue(AlarmEndSessions.claim(current))
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
                if (AlarmEndSessions.claim(session)) winners.incrementAndGet()
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
