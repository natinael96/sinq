package com.agpeya.app.model

import kotlinx.serialization.Serializable

/**
 * ግጻዌ (Gitsawe) — the Ethiopian Orthodox lectionary: which scripture readings,
 * synaxarium commemorations, and chants belong to each day, fasting/feast
 * season, and month of the church year.
 *
 * Deserialized from the bundled JSON under assets/content/gitsawe/, structured
 * from the licensed scan transcription supplied for Sinq. Text is Ge'ez-first.
 * The fixed calendar covers all
 * 366 possible Ethiopian month-days, including leap-year Pagumen 6.
 */

/** Anything carrying the three lectionary offices — daily, seasonal, monthly. */
interface GitsaweServices {
    val negh: GitsaweService?
    val kidassie: GitsaweService?
    val serk: GitsaweService?
}

/** Every reading in all available offices, flattened across the fixed slots. */
fun GitsaweServices.readings(): List<GitsaweReading> =
    listOfNotNull(negh, kidassie, serk).flatMap {
        it.msbak + it.wengel + it.firstDeacon + it.secondDeacon + it.secondKahn
    }

/** A daily entry, keyed by the Ethiopian "DD-MM" date, with its three offices. */
@Serializable
data class GitsaweEntry(
    /** Ethiopian "DD-MM" date. */
    val date: String,
    val title: String? = null,
    /** ስንክሳር — the day's synaxarium (saints and commemorations). */
    val snksar: List<GitsaweNote> = emptyList(),
    /** Scan pages from which this fixed-cycle day was transcribed. */
    val sourcePages: List<Int> = emptyList(),
    /** ነግህ — the morning-office readings. */
    override val negh: GitsaweService? = null,
    /** ቅዳሴ — the Divine Liturgy readings. */
    override val kidassie: GitsaweService? = null,
    /** ሠርክ — the evening-office readings. */
    override val serk: GitsaweService? = null,
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
    /** Scan pages from which this seasonal entry was transcribed. */
    val sourcePages: List<Int> = emptyList(),
    override val negh: GitsaweService? = null,
    override val kidassie: GitsaweService? = null,
    override val serk: GitsaweService? = null,
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
    override val serk: GitsaweService? = null,
) : GitsaweServices

/**
 * One ordered row from the book's Sunday/mezmur cycle (master Part 3).
 * [period] is the printed selection rule; it remains source text until a rule
 * has been classified as fixed-date or computus-relative. Partial rows and
 * rubrics are intentionally retained rather than expanded into guessed services.
 */
@Serializable
data class SundayCycleEntry(
    val index: Int,
    val title: String,
    val period: String? = null,
    val heading: String? = null,
    val mezmur: String? = null,
    val gize: String? = null,
    val rubric: String? = null,
    val reviewNotes: String? = null,
    val sourcePages: List<Int> = emptyList(),
    /** Unambiguous fixed Ethiopian-date selector, when printed by the source. */
    val monthNum: Int? = null,
    val fromDay: Int? = null,
    val toDay: Int? = null,
    /** Unambiguous computus-relative selector, when printed by the source. */
    val season: String? = null,
    val week: Int? = null,
    override val negh: GitsaweService? = null,
    override val kidassie: GitsaweService? = null,
    override val serk: GitsaweService? = null,
) : GitsaweServices

/** A separately selected funeral or memorial reading from master Part 4. */
@Serializable
data class AthanasiusEntry(
    val index: Int,
    val title: String,
    /** person, riteChapter, burialPrayer, or memorial. */
    val category: String,
    val memorialDay: Int? = null,
    val observance: String? = null,
    /** መስተበቍዕ, kept distinct from the scripture-reading slots. */
    val supplication: String? = null,
    val sourcePages: List<Int> = emptyList(),
    override val negh: GitsaweService? = null,
    override val kidassie: GitsaweService? = null,
    override val serk: GitsaweService? = null,
) : GitsaweServices

/** The historical 2001–2015 EC reference table printed in master Part 5. */
@Serializable
data class BahreHasabReference(
    val title: String,
    val note: String,
    val sourcePages: List<Int> = emptyList(),
    val columns: List<String>,
    val rows: List<BahreHasabReferenceRow>,
)

@Serializable
data class BahreHasabReferenceRow(val values: List<String>)

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
    /** Citation as printed when it could not safely become a passage link. */
    val citation: String? = null,
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
    /** Citation exactly as printed in the transcribed source. */
    val citation: String? = null,
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
