package com.agpeya.app.data

import android.content.Context
import com.agpeya.app.model.DayContext
import com.agpeya.app.model.JournalEntry
import com.agpeya.app.model.JournalKind
import com.agpeya.app.ui.common.EthiopianDate
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.UUID

/**
 * The journal, day by day.
 *
 * The one thing worth knowing about this layer is [contextFor]: every entry
 * captures the Church's day it was written on, so that reading it back years
 * later gives more than a Gregorian date. That snapshot is taken once, at
 * writing time, and never recomputed — the bundled ግጻዌ and ስንክሳር have been
 * re-extracted and corrected before now, and an old entry should keep saying
 * what the app told the person on the day, not what the app would say today.
 */
object JournalRepository {

    private fun dao(context: Context) = JournalDatabase.get(context).journalDao()

    // ── Reading ──────────────────────────────────────────────────────────────

    fun onDate(context: Context, date: LocalDate): Flow<List<JournalEntry>> =
        dao(context).onDate(date.toString())

    fun inEthiopianMonth(context: Context, year: Int, month: Int): Flow<List<JournalEntry>> =
        dao(context).inEthiopianMonth(year, month)

    fun writtenDaysIn(context: Context, year: Int, month: Int): Flow<List<Int>> =
        dao(context).writtenDaysIn(year, month)

    fun count(context: Context): Flow<Int> = dao(context).count()

    suspend fun byId(context: Context, id: String): JournalEntry? = dao(context).byId(id)

    // ── Writing ──────────────────────────────────────────────────────────────

    /**
     * A blank entry for [date], already carrying that day's context.
     *
     * Not yet saved: an entry the person opened and closed without writing
     * anything should leave nothing behind, so persistence waits for [save].
     */
    suspend fun draft(
        context: Context,
        date: LocalDate = LocalDate.now(),
        kind: JournalKind = JournalKind.REFLECTION,
        anchorRoute: String? = null,
        anchorLabel: String? = null,
        now: Long = System.currentTimeMillis(),
    ): JournalEntry = JournalEntry(
        id = UUID.randomUUID().toString(),
        date = date.toString(),
        kind = kind,
        context = contextFor(context, date),
        anchorRoute = anchorRoute,
        anchorLabel = anchorLabel,
        createdAt = now,
        updatedAt = now,
    )

    /**
     * Persist an entry, or remove it if the person emptied it.
     *
     * A journal entry with no text is not a record of anything, and leaving
     * blank rows behind would make the month view lie about which days were
     * written on. Returns true when something is now stored.
     */
    suspend fun save(
        context: Context,
        entry: JournalEntry,
        now: Long = System.currentTimeMillis(),
    ): Boolean {
        if (entry.body.isBlank()) {
            dao(context).deleteById(entry.id)
            return false
        }
        dao(context).upsert(entry.copy(updatedAt = now))
        return true
    }

    suspend fun delete(context: Context, id: String) = dao(context).deleteById(id)

    /** The "confessed" action: every ንስሐ draft, gone. */
    suspend fun dischargeDrafts(context: Context) = dao(context).deleteAllDrafts()

    // ── Export ───────────────────────────────────────────────────────────────

    /**
     * Everything that may be carried off the device — reflections and passage
     * notes, never ንስሐ drafts.
     */
    suspend fun exportable(context: Context): List<JournalEntry> =
        dao(context).all().filter { it.exportable }

    /**
     * Merge restored entries, keeping whichever copy was edited last.
     *
     * A journal is the one thing here where a blind merge could destroy work:
     * the same entry may have been extended on two devices, and the app has no
     * way to reconcile two versions of a paragraph. Last-write-wins at least
     * loses only the older of the two, and never silently drops an entry that
     * exists on one side alone. Any restored ንስሐ draft is discarded — one
     * should not exist in a backup, and if it does, it is not welcome here.
     */
    suspend fun merge(context: Context, entries: List<JournalEntry>) {
        val incoming = entries.filter { it.exportable }
        if (incoming.isEmpty()) return
        val existing = dao(context).all().associateBy { it.id }
        val toWrite = incoming.filter { candidate ->
            val current = existing[candidate.id] ?: return@filter true
            candidate.updatedAt > current.updatedAt
        }
        if (toWrite.isNotEmpty()) dao(context).upsertAll(toWrite)
    }

    // ── The Church's day ─────────────────────────────────────────────────────

    /**
     * What the app knows about [date], for stamping onto a new entry.
     *
     * Every lookup fails soft: the bundled ግጻዌ covers about 301 of 365 days,
     * the fast calendar can throw on a year outside its range, and none of that
     * should stop someone writing. A context of just the ግእዝ date is a
     * perfectly good context.
     */
    suspend fun contextFor(context: Context, date: LocalDate): DayContext {
        val eth = EthiopianDate.from(date)
        return DayContext(
            ethYear = eth.year,
            ethMonth = eth.month,
            ethDay = eth.day,
            monthlyFeast = runCatching {
                HolidayCalendar.monthlyOn(context, eth.day)?.primary
            }.getOrNull(),
            fast = runCatching { FastingCalendar.fastOn(date)?.nameAm }.getOrNull(),
            gitsawe = runCatching {
                GitsaweRepository.dailyFor(context, date)?.title
            }.getOrNull()?.takeIf { it.isNotBlank() },
        )
    }
}
