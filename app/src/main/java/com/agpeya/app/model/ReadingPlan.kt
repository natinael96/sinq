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
 * Which plan is being kept, and which of its days have been read.
 *
 * Days, not dates. That is what makes a plan undated: falling behind shifts the
 * remaining days rather than accumulating a visible deficit, and redistributing
 * is arithmetic on a number instead of a rewrite of a calendar.
 */
@Serializable
data class ReadingPlanState(
    val activePlanId: String = "",
    /** ISO-8601 date the plan's day 1 fell on. */
    val startedOn: String = "",
    /** planId → the day numbers read. Never cleared by a miss. */
    val completedDays: Map<String, Set<Int>> = emptyMap(),
    val lastReadOn: String = "",
) {
    fun readDays(planId: String): Set<Int> = completedDays[planId] ?: emptySet()
}
