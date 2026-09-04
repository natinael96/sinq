package com.agpeya.app.model

import kotlinx.serialization.Serializable

/** One rule of ቁርባን preparation, as the tradition states it. */
@Serializable
data class KurbanChecklistItem(
    val id: String = "",
    val title: String = "",
    val detail: String = "",
)

/** A prayer said before or after receiving. */
@Serializable
data class KurbanPrayer(
    val id: String = "",
    val title: String = "",
    val body: String = "",
)

/** The bundled ቁርባን preparation content (assets/content/kurban/). */
@Serializable
data class KurbanContent(
    val contentVersion: Int = 1,
    val checklist: List<KurbanChecklistItem> = emptyList(),
    val prePrayers: List<KurbanPrayer> = emptyList(),
    val postPrayers: List<KurbanPrayer> = emptyList(),
)
