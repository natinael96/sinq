package com.agpeya.app.ui.reading

import com.agpeya.app.data.ReadingPlanRepository
import com.agpeya.app.model.*
import java.time.LocalDate

/** Explicit assignment context wins; stale links must never fall back to today. */
internal fun readerPlanDays(
    content: ReadingPlanContent,
    state: ReadingPlanState,
    today: LocalDate,
    selectedPlanId: String = "",
    selectedPlanDay: Int = 0,
): List<Pair<ReadingPlan, PlanDay>> = state.plansKept.mapNotNull { kept ->
    if (selectedPlanId.isNotBlank() && kept.planId != selectedPlanId) return@mapNotNull null
    val plan = content.plans.firstOrNull { it.id == kept.planId } ?: return@mapNotNull null
    val number = if (selectedPlanId.isNotBlank()) selectedPlanDay
        else ReadingPlanRepository.dayOn(kept.startedOn, today, plan.days)
    val day = ReadingPlanRepository.effectiveDays(plan, state).firstOrNull { it.d == number }
        ?: return@mapNotNull null
    plan to day
}
