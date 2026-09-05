package com.agpeya.app.model

import kotlinx.serialization.Serializable

/** A line of tour copy in both languages; the screen picks by [Strings.isAmharic]. */
@Serializable
data class TourText(val am: String = "", val en: String = "") {
    fun pick(amharic: Boolean): String = if (amharic) am else en
    val isBlank: Boolean get() = am.isBlank() && en.isBlank()
}

/**
 * One page of the tour.
 *
 * [route] is what makes this more than a changelog: when a page names one, it
 * grows a button that opens the thing being described. Reading about a feature
 * and then having to go and find it is where a tour stops being useful.
 */
@Serializable
data class TourPage(
    val kicker: TourText = TourText(),
    val title: TourText = TourText(),
    val body: TourText = TourText(),
    val route: String = "",
    val action: TourText = TourText(),
)

/** The tour for one release, keyed to the versionCode that introduced it. */
@Serializable
data class Tour(
    val versionCode: Int = 0,
    val version: String = "",
    val title: TourText = TourText(),
    val pages: List<TourPage> = emptyList(),
)

/** assets/content/tour/tour.json */
@Serializable
data class TourContent(
    val contentVersion: Int = 1,
    val tours: List<Tour> = emptyList(),
)
