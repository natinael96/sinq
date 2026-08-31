package com.agpeya.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agpeya.app.model.Cents
import com.agpeya.app.model.TitheEntry
import com.agpeya.app.model.TitheEntryKind
import com.agpeya.app.model.Vow
import com.agpeya.app.ui.common.EthiopianDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.LocalDate

private val Context.offeringDataStore by preferencesDataStore(name = "offerings")

/**
 * አስራት (tithe) and ስዕለት (vow): the two obligations the app does keep a record
 * of.
 *
 * This is a deliberate departure from ምጽዋት and ንስሐ, which the app reminds and
 * then forgets. Alms are hidden by intent — "let not thy left hand know what
 * thy right hand doeth" — but a tithe is a reckoning and a vow is a debt
 * willingly taken on, and neither can be kept by someone who cannot see where
 * they stand. So this store holds amounts, and nothing else in the app does.
 *
 * It lives in its own DataStore rather than in [SettingsRepository] because it
 * is records, not preferences: it grows over years, and a person clearing
 * their settings should not thereby erase what they have given.
 */
object OfferingRepository {

    /** The tenth. Kept adjustable because people also pledge other fractions. */
    const val DEFAULT_TITHE_PERCENT = 10

    private val KEY_TITHE_ENTRIES = stringPreferencesKey("tithe_entries")
    private val KEY_TITHE_PERCENT = intPreferencesKey("tithe_percent")
    private val KEY_VOWS = stringPreferencesKey("vows")
    private val KEY_CURRENCY = stringPreferencesKey("currency_label")

    // encodeDefaults keeps every field in the stored JSON, so a downgrade to an
    // app version whose model lacks a default cannot fail to decode and wipe
    // someone's ledger — the same guard [UserDataRepository] uses.
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val titheSerializer = ListSerializer(TitheEntry.serializer())
    private val vowSerializer = ListSerializer(Vow.serializer())

    // ── አስራት ledger ──────────────────────────────────────────────────────────

    fun titheEntries(context: Context): Flow<List<TitheEntry>> =
        context.offeringDataStore.data.map { prefs ->
            prefs[KEY_TITHE_ENTRIES]?.let {
                runCatching { json.decodeFromString(titheSerializer, it) }.getOrNull()
            } ?: emptyList()
        }

    suspend fun setTitheEntries(context: Context, entries: List<TitheEntry>) {
        context.offeringDataStore.edit {
            it[KEY_TITHE_ENTRIES] = json.encodeToString(titheSerializer, entries)
        }
    }

    /** Add one line, newest first — the order the ledger is read in. */
    suspend fun addTitheEntry(context: Context, entry: TitheEntry) {
        val current = titheEntries(context).first()
        setTitheEntries(context, (listOf(entry) + current).sortedByDescending { it.date })
    }

    suspend fun deleteTitheEntry(context: Context, id: String) {
        setTitheEntries(context, titheEntries(context).first().filterNot { it.id == id })
    }

    /** The fraction owed, as a percentage. */
    fun tithePercent(context: Context): Flow<Int> =
        context.offeringDataStore.data.map { it[KEY_TITHE_PERCENT] ?: DEFAULT_TITHE_PERCENT }

    suspend fun setTithePercent(context: Context, percent: Int) {
        context.offeringDataStore.edit { it[KEY_TITHE_PERCENT] = percent.coerceIn(1, 100) }
    }

    /**
     * What the amounts are counted in. Free text and not validated: "ብር" for
     * most, but the diaspora keeps a tithe in dollars or euros and the app has
     * no business converting between them.
     */
    fun currency(context: Context): Flow<String> =
        context.offeringDataStore.data.map { it[KEY_CURRENCY] ?: "" }

    suspend fun setCurrency(context: Context, label: String) {
        context.offeringDataStore.edit { it[KEY_CURRENCY] = label.trim().take(8) }
    }

    // ── ስዕለት ────────────────────────────────────────────────────────────────

    fun vows(context: Context): Flow<List<Vow>> =
        context.offeringDataStore.data.map { prefs ->
            prefs[KEY_VOWS]?.let {
                runCatching { json.decodeFromString(vowSerializer, it) }.getOrNull()
            } ?: emptyList()
        }

    suspend fun setVows(context: Context, vows: List<Vow>) {
        context.offeringDataStore.edit { it[KEY_VOWS] = json.encodeToString(vowSerializer, vows) }
    }

    /** Read off the main thread for the alarm receiver, which has no scope. */
    fun vowsBlocking(context: Context): List<Vow> =
        runCatching { runBlocking(Dispatchers.IO) { vows(context).first() } }.getOrDefault(emptyList())

    // ── Reckoning ────────────────────────────────────────────────────────────

    /**
     * What a set of ledger lines comes to over a period: income received, the
     * tithe due on it, what has been given, and the difference.
     *
     * [owed] can go negative when someone gives more than the tenth, and is
     * reported that way rather than clamped — giving beyond the tithe is not an
     * error to be rounded away, and a person carrying a surplus forward wants
     * to see it.
     */
    data class Reckoning(
        val income: Cents,
        val due: Cents,
        val given: Cents,
        val percent: Int,
    ) {
        val owed: Cents get() = due - given
        val settled: Boolean get() = owed <= 0
    }

    fun reckon(entries: List<TitheEntry>, percent: Int): Reckoning {
        val income = entries.filter { it.kind == TitheEntryKind.INCOME }.sumOf { it.amount }
        val given = entries.filter { it.kind == TitheEntryKind.GIVEN }.sumOf { it.amount }
        // Integer arithmetic throughout: the tenth of an odd number of santim
        // rounds down, which under-states what is owed rather than inventing a
        // debt of a fraction of a coin.
        return Reckoning(income = income, due = income * percent / 100, given = given, percent = percent)
    }

    /** The lines falling inside Ethiopian month [month] of year [year]. */
    fun inEthiopianMonth(entries: List<TitheEntry>, year: Int, month: Int): List<TitheEntry> =
        entries.filter { entry ->
            entry.localDate?.let { EthiopianDate.from(it) }
                ?.let { it.year == year && it.month == month } == true
        }

    /** The lines falling inside Ethiopian year [year]. */
    fun inEthiopianYear(entries: List<TitheEntry>, year: Int): List<TitheEntry> =
        entries.filter { entry ->
            entry.localDate?.let { EthiopianDate.from(it) }?.year == year
        }

    /** Today, as the ledger stores dates. */
    fun today(): String = LocalDate.now().toString()
}
