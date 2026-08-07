package com.agpeya.app.model

import kotlinx.serialization.Serializable

/**
 * ግጻዌ (Gitsawe) — the Ethiopian Orthodox lectionary: which scripture readings,
 * synaxarium commemorations, and chants belong to each day, fasting/feast
 * season, and month of the church year.
 *
 * Deserialized from the bundled JSON under assets/content/gitsawe/ — extracted
 * and normalized from the open `gitsawe` npm dataset, never hand-edited. Text is
 * Ge'ez-first with partial Amharic and English. Daily coverage is incomplete
 * (~301 of the ~365 days), so lookups may legitimately return null.
 */

/** Anything carrying the two lectionary services — daily, seasonal, monthly. */
interface GitsaweServices {
    val negh: GitsaweService?
    val kidassie: GitsaweService?
}

/** Every reading in both services, flattened across the fixed slots. */
fun GitsaweServices.readings(): List<GitsaweReading> =
    listOfNotNull(negh, kidassie).flatMap {
        it.msbak + it.wengel + it.firstDeacon + it.secondDeacon + it.secondKahn
    }

/** A daily entry, keyed by the Ethiopian "DD-MM" date, with its two services. */
@Serializable
data class GitsaweEntry(
    /** Ethiopian "DD-MM" date. */
    val date: String,
    val title: String? = null,
    /** ስንክሳር — the day's synaxarium (saints and commemorations). */
    val snksar: List<GitsaweNote> = emptyList(),
    /** ነግህ — the morning-office readings. */
    override val negh: GitsaweService? = null,
    /** ቅዳሴ — the Divine Liturgy readings. */
    override val kidassie: GitsaweService? = null,
) : GitsaweServices

/**
 * A movable, season-relative entry (Great Lent, the Resurrection season, ክረምት
 * Sundays…). Not tied to a fixed calendar date — [season] + [week] locate it in
 * the liturgical year; matching a Gregorian date to it needs the Bahre Hasab
 * (Ethiopian computus). [raw] preserves the original source key.
 */
@Serializable
data class SeasonalEntry(
    val season: String? = null,
    val week: Int? = null,
    val part: Int? = null,
    val raw: String,
    val movable: Boolean = true,
    val title: String? = null,
    override val negh: GitsaweService? = null,
    override val kidassie: GitsaweService? = null,
) : GitsaweServices

/**
 * A monthly entry: readings for a span of days within an Ethiopian month, or the
 * [nthSunday] of it. [fromDay]..[toDay] is inclusive; [crossMonth] means the span
 * runs into the following month (e.g. 26 → 5). [raw] preserves the source key.
 */
@Serializable
data class MonthlyEntry(
    val month: String? = null,
    val monthNum: Int? = null,
    val fromDay: Int? = null,
    val toDay: Int? = null,
    val nthSunday: Int? = null,
    val crossMonth: Boolean = false,
    val appliesTo: String? = null,
    /** Chant incipit. */
    val mezmur: String? = null,
    val raw: String,
    val title: String? = null,
    override val negh: GitsaweService? = null,
    override val kidassie: GitsaweService? = null,
) : GitsaweServices

/** A synaxarium note. In the current data only [amharic] is populated. */
@Serializable
data class GitsaweNote(
    val amharic: String? = null,
    val geez: String? = null,
    val english: String? = null,
)

/**
 * One service (ነግህ or ቅዳሴ): a fixed set of lectionary slots, each a list of
 * readings, plus the names of the chants sung during it.
 */
@Serializable
data class GitsaweService(
    /** ምስባክ — the psalm verse/prokeimenon. */
    val msbak: List<GitsaweReading> = emptyList(),
    /** ወንጌል — the Gospel reading. */
    val wengel: List<GitsaweReading> = emptyList(),
    val firstDeacon: List<GitsaweReading> = emptyList(),
    val secondDeacon: List<GitsaweReading> = emptyList(),
    val secondKahn: List<GitsaweReading> = emptyList(),
    /** ቅዳሴ chant names sung at the liturgy (e.g. "ዘወልደ ነጐድጓድ"). */
    val kidassie: List<String> = emptyList(),
)

/** A single reading: an incipit ([text]) and its scripture reference ([verse]). */
@Serializable
data class GitsaweReading(
    val text: GitsaweText? = null,
    val verse: VerseRef? = null,
)

@Serializable
data class GitsaweText(
    val geez: String? = null,
    val amharic: String? = null,
    val english: String? = null,
)

/**
 * A scripture reference. [start]/[end] are verse numbers. [endNote] preserves
 * the rare non-numeric range annotation from the source (e.g. "(4)-6") for the
 * few cases where [end] can't be a plain integer. [endText] is the Ge'ez incipit
 * marking where the reading stops.
 */
@Serializable
data class VerseRef(
    val bookTitle: String? = null,
    val chapter: Int? = null,
    val start: Int? = null,
    val end: Int? = null,
    val endNote: String? = null,
    val endText: String? = null,
)

/**
 * A major feast. Fixed feasts carry [dateKey] (Ethiopian "DD-MM"), [monthNum]
 * (1–13) and [day]; [movable] feasts (Easter, ጽጌ) have none of those and are
 * located liturgically instead.
 */
@Serializable
data class Feast(
    val key: String,
    val name: String,
    val amharicName: String,
    val month: String? = null,
    val monthNum: Int? = null,
    val day: Int? = null,
    val dateKey: String? = null,
    val movable: Boolean = false,
)

/** A sub-division of a feast/season (e.g. a numbered week), linked by [feast]. */
@Serializable
data class SubFeast(
    val name: String,
    val amharicName: String,
    val key: String,
    val feast: String,
)

/** An Ethiopian month. The bundled list is partial (8 of 13). */
@Serializable
data class GitsaweMonth(
    val name: String,
    val amharicName: String,
    val key: String,
)

/** A ማኅሌት service order — a titled sequence of chant verses. */
@Serializable
data class Mahlet(
    val title: String,
    val detail: List<MahletVerse> = emptyList(),
    val subFeast: String? = null,
    val source: String? = null,
)

/** One chant in a ማኅሌት: its liturgical role ([key], e.g. "ዚቅ") and the [verse] text. */
@Serializable
data class MahletVerse(
    val key: String,
    val verse: String,
)

/** A reading-plan bundle (only "daily" ships today). */
@Serializable
data class GitsawePackage(
    val name: PackageName,
    val key: String,
)

@Serializable
data class PackageName(val am: String, val en: String)
