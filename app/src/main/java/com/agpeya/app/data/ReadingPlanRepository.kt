package com.agpeya.app.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agpeya.app.model.ActivePlan
import com.agpeya.app.model.PlanDay
import com.agpeya.app.model.ReadingPlan
import com.agpeya.app.model.Redistribution
import com.agpeya.app.model.ReadingPlanContent
import com.agpeya.app.model.ReadingPlanState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val Context.readingPlanDataStore by preferencesDataStore(name = "reading_plan")

/**
 * ንባብ — the reading plan: its bundled definition, and what has been read of it.
 *
 * The plan is deliberately not a streak. Progress is the count of distinct days
 * read, exactly as [PrayerJourney] counts prayer — a missed day changes nothing
 * but that day, and nothing here ever resets to zero.
 */
object ReadingPlanRepository {

    /** ጉዞ's daily-reading habit, kept by reading a day of the plan. */
    private const val BIBLE_HABIT = "bible"

    private const val TAG = "ReadingPlanRepository"
    private const val PATH = "content/reading/plans.json"

    private val KEY_STATE = stringPreferencesKey("reading_plan_json")

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Volatile
    private var cache: ReadingPlanContent? = null

    // ── the bundled plans ────────────────────────────────────────────────────

    suspend fun content(context: Context): ReadingPlanContent =
        cache ?: withContext(Dispatchers.IO) {
            runCatching {
                val raw = context.applicationContext.assets
                    .open(PATH).readBytes().decodeToString()
                json.decodeFromString<ReadingPlanContent>(raw)
            }.onFailure { Log.e(TAG, "Failed to load reading plans", it) }
                .getOrNull()?.also { cache = it } ?: ReadingPlanContent()
        }

    suspend fun plan(context: Context, id: String): ReadingPlan? =
        content(context).plans.firstOrNull { it.id == id }

    // ── what has been read ───────────────────────────────────────────────────

    fun state(context: Context): Flow<ReadingPlanState> =
        context.readingPlanDataStore.data.map { decode(it[KEY_STATE]) }

    suspend fun current(context: Context): ReadingPlanState = state(context).first()

    private fun decode(raw: String?): ReadingPlanState =
        raw?.let { runCatching { json.decodeFromString<ReadingPlanState>(it) }.getOrNull() }
            ?: ReadingPlanState()

    private suspend fun update(context: Context, transform: (ReadingPlanState) -> ReadingPlanState) {
        context.readingPlanDataStore.edit { prefs ->
            prefs[KEY_STATE] = json.encodeToString(transform(decode(prefs[KEY_STATE])))
        }
    }

    /**
     * Begin a plan today, beside any already being kept.
     *
     * Starting the same plan again re-dates it rather than adding it twice;
     * what has been read of it is kept either way.
     */
    suspend fun start(context: Context, planId: String, today: LocalDate = LocalDate.now()) {
        update(context) { st ->
            val kept = st.plansKept.filterNot { it.planId == planId }
            st.copy(
                active = kept + ActivePlan(planId, today.toString()),
                // Folded now, so the legacy pair never disagrees with the list.
                activePlanId = "",
                startedOn = "",
                read = st.read + (planId to st.readFor(planId)),
            )
        }
    }

    /** Begin another cycle without erasing lifetime chapter history. */
    internal fun restarted(st: ReadingPlanState, planId: String, today: LocalDate): ReadingPlanState = st.copy(
        active = st.plansKept.filterNot { it.planId == planId } + ActivePlan(planId, today.toString()),
        activePlanId = "", startedOn = "",
        read = st.read + (planId to emptySet()),
        completedDays = st.completedDays - planId,
        redistributed = st.redistributed - planId,
    )

    suspend fun restart(context: Context, planId: String, today: LocalDate = LocalDate.now()) {
        update(context) { restarted(it, planId, today) }
    }

    /** Stop one plan. The others carry on, and what was read stays read. */
    suspend fun stop(context: Context, planId: String) {
        update(context) { st ->
            st.copy(
                active = st.plansKept.filterNot { it.planId == planId },
                activePlanId = "",
                startedOn = "",
            )
        }
    }

    /**
     * Why [planId] cannot be kept beside what already is, or null when it can.
     *
     * The only pairing that makes no sense is the same text at two speeds: the
     * year and the six months read the same 1,610 chapters, so keeping both
     * would print every day's reading twice. A plan contained in another —
     * የዳዊት ንባብ inside a Bible plan — is a different matter: the psalms are
     * read twice on purpose, and each plan counts its own.
     */
    fun conflict(content: ReadingPlanContent, state: ReadingPlanState, planId: String): String? {
        val candidate = chaptersIn(content, planId) ?: return null
        if (candidate.isEmpty()) return null
        for (kept in state.plansKept) {
            if (kept.planId == planId) continue
            val other = chaptersIn(content, kept.planId) ?: continue
            if (other.isEmpty()) continue
            val shared = candidate.count { it in other }
            // The same corpus, whatever the pace: nine tenths of each.
            if (shared * 10 >= candidate.size * 9 && shared * 10 >= other.size * 9) {
                return kept.planId
            }
        }
        return null
    }

    /** Every chapter a plan asks for, across all its days. */
    fun chaptersIn(content: ReadingPlanContent, planId: String): Set<String>? =
        content.plans.firstOrNull { it.id == planId }
            ?.readings?.flatMapTo(mutableSetOf()) { chaptersOf(it) }

    /**
     * Mark a day's chapters read. Idempotent — reading the same day twice is
     * not worth recording twice, and a chapter this day shares with another is
     * simply read.
     *
     * A day of the plan read is also the day's Bible reading kept, so it
     * writes ጉዞ's `bible` habit for the day it was marked on — the ledger and
     * the plan were two records of one act, and the reader had to keep both.
     */
    suspend fun markDay(
        context: Context,
        planId: String,
        day: PlanDay,
        today: LocalDate = LocalDate.now(),
    ) {
        update(context) { st ->
            st.copy(
                read = st.read + (planId to (st.readFor(planId) + chaptersOf(day))),
                readChapters = st.readChapters + chaptersOf(day),
                lastReadOn = today.toString(),
            )
        }
        HabitsRepository.markDone(context, today.toString(), BIBLE_HABIT)
    }

    /**
     * Unmark a day. Chapters this day shares with another already-read day are
     * kept: they have still been read, and taking them back would leave that
     * other day looking unread.
     */
    suspend fun unmarkDay(context: Context, plan: ReadingPlan, day: PlanDay) {
        update(context) { st ->
            val elsewhere = effectiveDays(plan, st)
                .filter { it.d != day.d && isRead(st, plan.id, it) }
                .flatMapTo(mutableSetOf()) { chaptersOf(it) }
            val taken = chaptersOf(day).toSet() - elsewhere
            // Only this plan's count goes back. [ReadingPlanState.readChapters]
            // is the record of what has been read, and unmarking a day does not
            // make it unread — it says the day is not finished.
            st.copy(read = st.read + (plan.id to (st.readFor(plan.id) - taken)))
        }
    }

    /**
     * Keep or release one passage of a day.
     *
     * The day was always stored chapter by chapter; only the marking was
     * all-or-nothing. A day of three passages can now be kept as it is read,
     * and the day counts as read when its last passage does.
     */
    suspend fun toggleReading(
        context: Context,
        planId: String,
        chapters: List<String>,
        today: LocalDate = LocalDate.now(),
    ) {
        if (chapters.isEmpty()) return
        var kept = false
        update(context) { st ->
            val mine = st.readFor(planId)
            kept = !chapters.all { it in mine }
            toggledChapters(st, planId, chapters, today)
        }
        if (kept) {
            HabitsRepository.markDone(context, today.toString(), BIBLE_HABIT)
        }
    }

    /** Halfway reading records one chapter and never toggles existing completion off. */
    suspend fun markChapter(
        context: Context, book: String, chapter: Int, planIds: List<String>,
        today: LocalDate = LocalDate.now(),
    ) {
        update(context) { state ->
            val keptIds = state.plansKept.map { it.planId }.toSet()
            markedChapter(state, chapterKey(book, chapter), planIds.filter { it in keptIds }, today)
        }
        HabitsRepository.markDone(context, today.toString(), BIBLE_HABIT)
    }

    internal fun markedChapter(
        state: ReadingPlanState, chapter: String, planIds: List<String>, today: LocalDate,
    ): ReadingPlanState = state.copy(
        read = state.read + planIds.distinct().associateWith { state.readFor(it) + chapter },
        readChapters = state.readChapters + chapter,
        lastReadOn = today.toString(),
    )

    internal fun toggledChapters(
        st: ReadingPlanState, planId: String, chapters: List<String>, today: LocalDate,
    ): ReadingPlanState {
        if (chapters.isEmpty()) return st
        val mine = st.readFor(planId)
        val kept = !chapters.all { it in mine }
        return st.copy(
            read = st.read + (planId to (if (kept) mine + chapters else mine - chapters.toSet())),
            readChapters = if (kept) st.readChapters + chapters else st.readChapters,
            lastReadOn = if (kept) today.toString() else st.lastReadOn,
        )
    }

    /** Days of the last week (today back six) the reading habit was kept. */
    fun daysReadInWeek(records: Map<String, Set<String>>, today: LocalDate): Int =
        (0L..6L).count { back ->
            BIBLE_HABIT in (records[today.minusDays(back).toString()] ?: emptySet())
        }

    /**
     * Fold the old day-number ledger into chapters, and drop a repacking that
     * a new bundle has renumbered out from under. Runs at launch, before any
     * screen reads the state; a no-op on every launch after the first.
     */
    suspend fun reconcile(context: Context, content: ReadingPlanContent) = update(context) { st ->
        // A state that has never been stamped is not stale — it was written
        // against the bundle it is being read on. Only a version that changed
        // under a reader renumbers the days.
        val stale = st.contentVersion != 0 && st.contentVersion != content.contentVersion
        val plural = st.activePlanId.isBlank() || st.active.isNotEmpty()
        if (st.completedDays.isEmpty() && plural && !stale &&
            st.contentVersion == content.contentVersion
        ) {
            return@update st
        }
        val chapters = st.readChapters.toMutableSet()
        val perPlan = st.read.toMutableMap()
        st.completedDays.forEach { (planId, days) ->
            val plan = content.plans.firstOrNull { it.id == planId } ?: return@forEach
            val folded = mutableSetOf<String>()
            effectiveDays(plan, st).forEach { d -> if (d.d in days) folded += chaptersOf(d) }
            chapters += folded
            perPlan[planId] = (perPlan[planId] ?: emptySet()) + folded
        }
        // A state written before plans were plural: the one plan it held keeps
        // the chapters it recorded, and the pair of legacy fields is retired.
        val kept = st.plansKept
        if (st.activePlanId.isNotBlank() && st.active.isEmpty()) {
            perPlan[st.activePlanId] = (perPlan[st.activePlanId] ?: emptySet()) + st.readChapters
        }
        st.copy(
            active = kept,
            activePlanId = "",
            startedOn = "",
            read = perPlan,
            readChapters = chapters,
            completedDays = emptyMap(),
            redistributed = if (stale) emptyMap() else st.redistributed,
            contentVersion = content.contentVersion,
        )
    }

    /**
     * Shift day 1 so that [day] becomes today — "finish later, same daily
     * reading". Nothing is skipped; the plan's last day simply moves out.
     */
    suspend fun rebaseTo(
        context: Context,
        planId: String,
        day: Int,
        today: LocalDate = LocalDate.now(),
    ) {
        val began = today.minusDays((day - 1).toLong()).toString()
        update(context) { st ->
            st.copy(
                active = st.plansKept.map { if (it.planId == planId) it.copy(startedOn = began) else it },
                activePlanId = "",
                startedOn = "",
            )
        }
    }

    /**
     * The other answer to falling behind: keep the finish date and let the days
     * grow. Everything from [fromDay] is repacked across the days from [onDay]
     * to the plan's last — see [effectiveDays], which recomputes the packing.
     */
    suspend fun redistributeFrom(context: Context, planId: String, fromDay: Int, onDay: Int) {
        update(context) {
            it.copy(redistributed = it.redistributed + (planId to Redistribution(fromDay, onDay)))
        }
    }

    /**
     * The plan's days as they stand for this reader: the bundled days until the
     * repacking took effect, then the repacked tail. Days before it keep the
     * readings they were given — that is what those days were, and they are
     * still marked unread — while everything owed moves into the days ahead.
     */
    fun effectiveDays(plan: ReadingPlan, state: ReadingPlanState): List<PlanDay> {
        val r = state.redistributed[plan.id] ?: return plan.readings
        if (!r.inForce) return plan.readings
        return plan.readings.filter { it.d < r.on } +
            redistribute(plan.readings, r.from, plan.days - r.on + 1, startDay = r.on)
    }

    /**
     * Merge for backup restore: union, so an old file never erases newer
     * reading. A backup written by an older build carries day numbers instead
     * of chapters; they land in [ReadingPlanState.completedDays] and the next
     * [reconcile] folds them in.
     */
    suspend fun merge(context: Context, restored: ReadingPlanState) {
        update(context) { cur ->
            val days = (cur.completedDays.keys + restored.completedDays.keys).associateWith {
                cur.readDays(it) + restored.readDays(it)
            }
            val kept = cur.plansKept.ifEmpty { restored.plansKept }
            val merged = (cur.read.keys + restored.read.keys).associateWith {
                cur.readFor(it) + restored.readFor(it)
            }
            cur.copy(
                active = kept,
                activePlanId = "",
                startedOn = "",
                read = merged,
                completedDays = days,
                readChapters = cur.readChapters + restored.readChapters,
                lastReadOn = maxOf(cur.lastReadOn, restored.lastReadOn),
                // The device's own repacking wins: it matches the dates it is living under.
                redistributed = restored.redistributed + cur.redistributed,
            )
        }
    }

    // ── pure day math (unit-tested; no Context) ──────────────────────────────

    /**
     * Which day of the plan today is — 1 on the day it was started.
     *
     * Clamped to the plan's length so a plan left running for two years shows
     * its last day rather than day 800.
     */
    fun dayOn(startedOn: String, today: LocalDate, planDays: Int): Int {
        val start = runCatching { LocalDate.parse(startedOn) }.getOrNull() ?: return 1
        val n = ChronoUnit.DAYS.between(start, today).toInt() + 1
        return n.coerceIn(1, maxOf(1, planDays))
    }

    /** How a chapter is recorded: the book's slug and the chapter number. */
    fun chapterKey(slug: String, chapter: Int): String = "$slug:$chapter"

    /** Every chapter a day of the plan asks for. */
    fun chaptersOf(day: PlanDay): List<String> =
        day.r.flatMap { r -> r.chapters.map { chapterKey(r.b, it) } }

    /** A day is read when every chapter it asks for has been, for this plan. */
    fun isRead(state: ReadingPlanState, planId: String, day: PlanDay): Boolean {
        if (day.r.isEmpty()) return false
        val read = state.readFor(planId)
        return chaptersOf(day).all { it in read }
    }

    /** Only today's unfinished passages, across every kept plan. Past reading is not completion. */
    fun unreadToday(
        content: ReadingPlanContent,
        state: ReadingPlanState,
        today: LocalDate,
    ): List<PlanDay> = state.plansKept.mapNotNull { kept ->
        val plan = content.plans.firstOrNull { it.id == kept.planId } ?: return@mapNotNull null
        val days = effectiveDays(plan, state)
        // The day owed, which is the day the reader will be shown — so the
        // reminder and the reading agree about what is outstanding.
        val number = dueDay(state, plan.id, days, kept.startedOn, today, plan.days)
        val day = days.firstOrNull { it.d == number } ?: return@mapNotNull null
        val read = state.readFor(plan.id)
        val remaining = day.r.filter { passage ->
            passage.chapters.any { chapterKey(passage.b, it) !in read }
        }
        day.copy(r = remaining).takeIf { remaining.isNotEmpty() }
    }

    /**
     * True when the reader is being handed a day older than the calendar's —
     * when the plan is waiting on something missed rather than keeping pace.
     *
     * The reminder asks about it in different words: a notice headed "today's
     * reading" that offers a day from last month is not telling the truth, and
     * the reader who sees it should be told they are being taken back to where
     * they stopped.
     */
    fun isBehind(
        content: ReadingPlanContent,
        state: ReadingPlanState,
        today: LocalDate,
    ): Boolean = state.plansKept.any { kept ->
        val plan = content.plans.firstOrNull { it.id == kept.planId } ?: return@any false
        val days = effectiveDays(plan, state)
        dueDay(state, plan.id, days, kept.startedOn, today, plan.days) <
            dayOn(kept.startedOn, today, plan.days)
    }

    /** Which of [days] are read, by day number. */
    fun readDayNumbers(state: ReadingPlanState, planId: String, days: List<PlanDay>): Set<Int> =
        days.filterTo(mutableListOf()) { isRead(state, planId, it) }.mapTo(mutableSetOf()) { it.d }

    /** Days actually read — the only progress number the app shows. */
    fun daysRead(state: ReadingPlanState, planId: String, days: List<PlanDay>): Int =
        days.count { isRead(state, planId, it) }

    /** Chapters of the plan read, and chapters it asks for in all. */
    fun chapterProgress(
        state: ReadingPlanState,
        planId: String,
        days: List<PlanDay>,
    ): Pair<Int, Int> {
        val asked = days.flatMapTo(mutableSetOf()) { chaptersOf(it) }
        val read = state.readFor(planId)
        return asked.count { it in read } to asked.size
    }

    /** Every day read: the plan is finished, whatever the calendar says. */
    fun isComplete(state: ReadingPlanState, planId: String, days: List<PlanDay>): Boolean =
        days.isNotEmpty() && days.all { isRead(state, planId, it) }

    /** The last day that asks for anything — a repacked plan can end early. */
    fun lastDay(days: List<PlanDay>): Int = days.maxOfOrNull { it.d } ?: 0

    /**
     * The earliest unread day at or before [currentDay] — where "catch up"
     * would start. Null when nothing is owed, which is the common case.
     *
     * A repacking resets the floor: once what was owed has been folded into the
     * days ahead, the days it came from are no longer behind.
     */
    fun oldestUnread(
        state: ReadingPlanState,
        planId: String,
        days: List<PlanDay>,
        currentDay: Int,
    ): Int? {
        val floor = state.redistributed[planId]?.takeIf { it.inForce }?.on ?: 1
        val byNumber = days.associateBy { it.d }
        return (floor..currentDay).firstOrNull { n ->
            val day = byNumber[n] ?: return@firstOrNull false
            !isRead(state, planId, day)
        }
    }

    /**
     * The day of the plan to read now.
     *
     * **Not the calendar day.** A plan is a queue, not a timetable: if day 2 was
     * never finished, day 3 is not today's reading — day 2 still is, and the
     * plan waits there rather than walking on and leaving a hole behind. Once
     * everything up to the calendar day is read this *is* the calendar day, so
     * a reader who is keeping up is never held back.
     *
     * [dayOn] remains what it says — which day the calendar is on — and is
     * still what the progress figures and the catch-up dialog count against.
     * This is only what the reader is handed.
     *
     * A day that asks for nothing cannot hold the queue. No bundled plan has
     * one and [redistribute] never makes one, but [isRead] answers false for an
     * empty day, so without this a rest day would stop a plan forever.
     *
     * It holds however far back it has to. A cap was tried and taken out: a
     * sliding window left the reader permanently six days behind the calendar
     * and stopped offering the day that was missed at all, which put the hole
     * back in the plan — the one thing waiting exists to prevent. Come back
     * after a long absence and the plan is still at the day it stopped; the
     * plan's own page is where the arrears get settled.
     */
    fun dueDay(
        state: ReadingPlanState,
        planId: String,
        days: List<PlanDay>,
        startedOn: String,
        today: LocalDate,
        planDays: Int,
    ): Int {
        val calendar = dayOn(startedOn, today, planDays)
        val byNumber = days.associateBy { it.d }
        // A repacking is the only thing that lifts the floor: after it, what was
        // owed has been folded into the days ahead and nothing is behind.
        val floor = state.redistributed[planId]?.takeIf { it.inForce }?.on ?: 1
        val owed = (floor..calendar).firstOrNull { n ->
            val day = byNumber[n] ?: return@firstOrNull false
            day.r.isNotEmpty() && !isRead(state, planId, day)
        }
        return owed ?: calendar
    }

    /**
     * Redistribute what is left across the days that remain — the default
     * answer to falling behind, and the only one that never shows a shortfall.
     *
     * Returns the days of the plan re-packed from [fromDay] onward into
     * [remainingDays], preserving order and dropping nothing.
     */
    fun redistribute(
        days: List<PlanDay>,
        fromDay: Int,
        remainingDays: Int,
        /** Day number the repacked run starts on; the same day it came from unless
         *  the reader has fallen behind and it is being folded into today. */
        startDay: Int = fromDay,
    ): List<PlanDay> {
        if (remainingDays <= 0) return emptyList()
        val tail = days.filter { it.d >= fromDay }.flatMap { it.r }
        if (tail.isEmpty()) return emptyList()
        // Spread across the days themselves rather than into chunks of a
        // rounded-up size. Chunking by ceil(n/d) yields ceil(n/ceil(n/d))
        // chunks, which is fewer than d whenever the division is not tight:
        // a ዳዊት reader who fell behind on day 5 and repacked on day 40 was
        // given readings to day 112 and nothing at all for the 38 days after.
        val spread = minOf(remainingDays, tail.size)
        val each = tail.size / spread
        val extra = tail.size % spread
        var from = 0
        return (0 until spread).map { i ->
            val take = each + if (i < extra) 1 else 0
            val slice = tail.subList(from, from + take).toList()
            from += take
            PlanDay(d = startDay + i, r = slice)
        }
    }
}
