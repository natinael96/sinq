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

/** Exactly one terminal action per ringing-alarm session. */
internal object AlarmEndSessions {

    /**
     * What an ending turned out to be.
     *
     * The distinction matters because two things used to be decided by one
     * answer: whether this ending is allowed at all, and whether it may take
     * the ringing notification down. A late ending from a superseded ring must
     * not touch the notification — that belongs to the ring now on screen — but
     * the hour it came from still deserves its "done?" follow-up, and refusing
     * the whole ending threw that away.
     */
    enum class End {
        /** The ring on screen, or one from a process that has since restarted. */
        CURRENT,

        /** An older ring ending after a newer one began. Follow up, touch nothing. */
        STALE,

        /** Already ended once. Do nothing at all. */
        ALREADY,
    }

    private val monitor = Any()
    private var current: String? = null
    private val ended = LinkedHashSet<String>()

    fun begin(session: String) = synchronized(monitor) {
        current = session
        // A tiny bound prevents a long-lived process accumulating tokens.
        while (ended.size > 16) ended.remove(ended.first())
    }

    /**
     * Claim this ring's one ending.
     *
     * A null [current] is treated as this session's own: the process has
     * restarted between the ring and the ending, and the session id carried by
     * the intent is the only memory of it left.
     */
    fun claim(session: String): End = synchronized(monitor) {
        if (!ended.add(session)) return@synchronized End.ALREADY
        val mine = current == null || current == session
        if (mine) {
            current = null
            End.CURRENT
        } else {
            End.STALE
        }
    }
}
