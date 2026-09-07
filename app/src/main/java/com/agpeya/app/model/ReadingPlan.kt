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

/**
 * Which plan is being kept, and what has been read of it.
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
 */
@Serializable
data class ReadingPlanState(
    val activePlanId: String = "",
    /** ISO-8601 date the plan's day 1 fell on. */
    val startedOn: String = "",
    /**
     * The old ledger: planId → day numbers read. Read once at launch, folded
     * into [readChapters], and then left empty. Restored backups written by an
     * older build arrive here too, and are folded the same way.
     */
    val completedDays: Map<String, Set<Int>> = emptyMap(),
    /** Every chapter read, as "slug:chapter". Never cleared by a miss. */
    val readChapters: Set<String> = emptySet(),
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
