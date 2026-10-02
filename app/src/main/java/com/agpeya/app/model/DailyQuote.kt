package com.agpeya.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One patristic saying for daily contemplation (Apophthegmata Patrum / የአበው ምክር).
 */
@Serializable
data class DailyQuote(
    val day: Int,
    @SerialName("day_geez") val dayGeez: String,
    @SerialName("author_en") val authorEn: String,
    @SerialName("author_am") val authorAm: String,
    val quote: String,
    @SerialName("quote_id") val quoteId: String,
)

/**
 * Creates a stable Bookmark snapshot from a Desert Fathers daily quote.
 * Adheres to identity stability contract: hourId = "daily_quote", sectionId = quoteId.
 */
fun DailyQuote.toBookmark(channelName: String): Bookmark = Bookmark(
    hourId = "daily_quote",
    hourName = channelName,
    sectionId = quoteId,
    title = authorAm.ifBlank { authorEn },
    subtitle = quote,
    route = "daily_quote?id=$quoteId",
)
