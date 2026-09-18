package com.agpeya.app.reminders

/**
 * Serializes the last scheduling/delivery step inside this process.
 *
 * DataStore writes happen before Settings calls a scheduler's sync method. A
 * receiver may already be preparing a notification on another thread at that
 * moment. Both paths enter this gate for their final state check and side
 * effect: either delivery wins first and the disable path immediately removes
 * it, or disabling wins and the receiver observes the new state and stops.
 */
internal object ReminderDispatchGate {
    private val monitor = Any()

    fun <T> locked(block: () -> T): T = synchronized(monitor) { block() }
}

/** First terminal action wins for each ringing-alarm session. */
internal object AlarmEndSessions {
    private val monitor = Any()
    private var active: String? = null
    private val ended = LinkedHashSet<String>()

    fun begin(session: String) = synchronized(monitor) {
        active = session
        // A tiny bound prevents a long-lived process accumulating tokens.
        while (ended.size > 16) ended.remove(ended.first())
    }

    fun claim(session: String): Boolean = synchronized(monitor) {
        val current = active
        if (current != null && current != session) return@synchronized false
        if (!ended.add(session)) return@synchronized false
        if (current == session) active = null
        true
    }
}
