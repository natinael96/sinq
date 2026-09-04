package com.agpeya.app.model

import kotlinx.serialization.Serializable

/**
 * One section of the ንስሐ self-examination — a heading and the questions read
 * under it. The questions are prompts to sit with, not fields to fill: the
 * flow renders them as text to be read, never as checkboxes, because a
 * conscience is examined, not audited.
 */
@Serializable
data class ExaminationSection(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val questions: List<String> = emptyList(),
)

/** The bundled examination of conscience (assets/content/nisiha/). */
@Serializable
data class ExaminationContent(
    val contentVersion: Int = 1,
    val intro: String = "",
    val sections: List<ExaminationSection> = emptyList(),
)
