package com.agpeya.app.model

import kotlinx.serialization.Serializable

/**
 * One passage of a plan day: chapters [c]..[to] of the book with slug [b].
 * A single chapter has [c] == [to]. Chapters are never split across days.
 */
@Serializable
data class PlanReading(
    val b: String = "",
    val c: Int = 0,
    val to: Int = 0,
) {
    val chapters: IntRange get() = c..maxOf(c, to)
    val chapterCount: Int get() = chapters.count()
}

/** A numbered day of a plan. Day numbers are 1-based and never dates. */
@Serializable
data class PlanDay(
    val d: Int = 0,
    val r: List<PlanReading> = emptyList(),
)

/**
 * A bundled reading plan.
 *
 * [withGitsawe] means the screen shows the day's appointed readings above the
 * plan's own — the Church first, then the rest of Scripture. That block is
 * never part of [readings] and is never counted as progress: the ግጻዌ is given,
 * not achieved.
 */
@Serializable
data class ReadingPlan(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val days: Int = 0,
    val withGitsawe: Boolean = true,
    /**
     * The middle day's verse count, measured by tools/build_reading_plans.py.
     *
     * The plans are packed to a verse budget, so the chapter count runs from
     * one to sixteen while the reading stays the same length. This is the
     * number worth showing, and the minutes it implies.
     */
    val versesADay: Int = 0,
    val readings: List<PlanDay> = emptyList(),
) {
    fun day(n: Int): PlanDay? = readings.firstOrNull { it.d == n }
    val totalChapters: Int get() = readings.sumOf { d -> d.r.sumOf { it.chapterCount } }
}

/** The bundled plans (assets/content/reading/plans.json). */
@Serializable
data class ReadingPlanContent(
    val contentVersion: Int = 1,
    val plans: List<ReadingPlan> = emptyList(),
)

/** One plan being kept, and the day it began. */
@Serializable
data class ActivePlan(
    val planId: String = "",
    /** ISO-8601 date this plan's day 1 fell on. */
    val startedOn: String = "",
)

/**
 * Which plans are being kept, and what has been read of each.
 *
 * Days, not dates. That is what makes a plan undated: falling behind shifts the
 * remaining days rather than accumulating a visible deficit, and redistributing
 * is arithmetic on a number instead of a rewrite of a calendar.
 *
 * But what is *recorded* is chapters, not day numbers. A day number means
 * nothing on its own: repack the plan and day 40 holds different chapters than
 * it did, rebuild the bundled plans and it holds different chapters again, and
 * a reader who had read ኦሪት ዘሌዋውያን would be told to read it once more. Chapters
 * are the thing actually read, so they are the thing stored, and a day counts
 * as read when every chapter it asks for has been.
 *
 * Recorded **per plan**, though, and that is the one thing the chapters alone
 * could not carry. የዳዊት ንባብ reads the hundred and fifty psalms that ዓመታዊ ንባብ
 * also reads; against a single ledger a reader two hundred days into the year
 * would be handed a Psalter plan already finished. [readChapters] is still
 * every chapter ever read by anyone — it is what the map of the books draws —
 * while [read] says which plan asked for it.
 */
@Serializable
data class ReadingPlanState(
    /** Superseded by [active]; folded into it on first read, then left blank. */
    val activePlanId: String = "",
    /** Superseded by [ActivePlan.startedOn]; folded in the same way. */
    val startedOn: String = "",
    /** The plans being kept. Two at most in practice — see the bundle. */
    val active: List<ActivePlan> = emptyList(),
    /**
     * The old ledger: planId → day numbers read. Read once at launch, folded
     * into [readChapters], and then left empty. Restored backups written by an
     * older build arrive here too, and are folded the same way.
     */
    val completedDays: Map<String, Set<Int>> = emptyMap(),
    /**
     * Every chapter read, by any plan, as "slug:chapter". Never cleared by a
     * miss, and never reduced by unmarking a day: it is the record of what has
     * been read, which is what the map of the books shows.
     */
    val readChapters: Set<String> = emptySet(),
    /** planId → the chapters read *for that plan*. What its progress counts. */
    val read: Map<String, Set<String>> = emptyMap(),
    val lastReadOn: String = "",
    /** planId → the repacking in force, when the reader chose to finish on time. */
    val redistributed: Map<String, Redistribution> = emptyMap(),
    /**
     * The `contentVersion` of the bundled plans this state was last reconciled
     * with. A new bundle renumbers the days, so the repacking held against the
     * old numbering is dropped; the chapters read survive it untouched.
     */
    val contentVersion: Int = 0,
) {
    /** The old day-number ledger for one plan; empty once it has been folded in. */
    fun readDays(planId: String): Set<Int> = completedDays[planId] ?: emptySet()

    /**
     * The plans being kept, with a state written by an older build read as the
     * one plan it could hold. Every screen goes through this rather than
     * [active], so the fold costs nothing and can never be half-applied.
     */
    val plansKept: List<ActivePlan>
        get() = when {
            active.isNotEmpty() -> active
            activePlanId.isNotBlank() -> listOf(ActivePlan(activePlanId, startedOn))
            else -> emptyList()
        }

    fun kept(planId: String): ActivePlan? = plansKept.firstOrNull { it.planId == planId }

    /** The chapters read for one plan, folding a pre-[read] state into its plan. */
    fun readFor(planId: String): Set<String> = read[planId]
        ?: readChapters.takeIf { planId == activePlanId && active.isEmpty() }
        ?: emptySet()
}

/**
 * A plan repacked so it still ends on its original day.
 *
 * Only the two numbers are stored: everything from day [from] onward was spread
 * across the days from [on] to the plan's last, and the packing is recomputed
 * from the bundled plan each time rather than copied into the user's data.
 */
@Serializable
data class Redistribution(
    /** First day whose readings were repacked — the oldest unread at the time. */
    val from: Int = 0,
    /** The day it was applied on, where the repacked reading begins. */
    val on: Int = 0,
) {
    val inForce: Boolean get() = from in 1..<on
}
