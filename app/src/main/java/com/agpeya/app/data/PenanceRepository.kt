package com.agpeya.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agpeya.app.model.Penance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.penanceDataStore by preferencesDataStore(name = "penance")

/**
 * ቀኖና records. Deliberately its own store, and deliberately absent from
 * [BackupRepository]: a penance is confessional material, the same class of
 * thing as a confession draft, and it never leaves the device — not in a
 * backup file, not anywhere. Losing an unfinished ቀኖና with a lost phone is
 * the accepted price; the Data settings say so.
 */
object PenanceRepository {

    private val KEY_PENANCES = stringPreferencesKey("penances")

    // encodeDefaults keeps every field in the stored JSON, so a downgrade to an
    // app version whose model lacks a default cannot fail to decode and wipe
    // the records — the same guard [OfferingRepository] uses.
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val serializer = ListSerializer(Penance.serializer())

    fun penances(context: Context): Flow<List<Penance>> =
        context.penanceDataStore.data.map { prefs ->
            prefs[KEY_PENANCES]?.let {
                runCatching { json.decodeFromString(serializer, it) }.getOrNull()
            } ?: emptyList()
        }

    suspend fun setPenances(context: Context, penances: List<Penance>) {
        context.penanceDataStore.edit {
            it[KEY_PENANCES] = json.encodeToString(serializer, penances)
        }
    }

    /** Read off the main thread for the alarm receiver, which has no scope. */
    fun penancesBlocking(context: Context): List<Penance> =
        runCatching { runBlocking(Dispatchers.IO) { penances(context).first() } }
            .getOrDefault(emptyList())
}
