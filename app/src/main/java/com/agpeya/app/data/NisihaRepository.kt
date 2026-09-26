package com.agpeya.app.data

import android.content.Context
import android.util.Log
import com.agpeya.app.model.ExaminationContent
import com.agpeya.app.model.ExaminationSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Loads the bundled ንስሐ examination of conscience from assets/content/nisiha/.
 * One small file, loaded once and cached.
 */
object NisihaRepository {

    private const val TAG = "NisihaRepository"
    private const val PATH = "content/nisiha/examination.json"

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cache: ExaminationContent? = null

    suspend fun load(context: Context): ExaminationContent =
        cache ?: withContext(Dispatchers.IO) {
            runCatching {
                val raw = context.applicationContext.assets
                    .open(PATH).readBytes().decodeToString()
                json.decodeFromString<ExaminationContent>(raw)
            }.onFailure { Log.e(TAG, "Failed to load examination", it) }
                // Cache only success — a transient failure shouldn't stick as an
                // empty screen for the rest of the process lifetime.
                .getOrNull()?.also { cache = it } ?: ExaminationContent()
        }

    /**
     * Assembles the confession draft body from the sections the person chose to
     * write under. [stamp] leads the body so it is never blank even when no
     * notes were taken — [JournalRepository.save] deletes blank-bodied entries,
     * and a completed examination with nothing written is still worth keeping
     * until it is confessed.
     */
    fun buildConfessionBody(
        sections: List<ExaminationSection>,
        notes: Map<String, String>,
        stamp: String,
    ): String = buildString {
        append(stamp.trim())
        for (section in sections) {
            val note = notes[section.id]?.trim().orEmpty()
            if (note.isEmpty()) continue
            append("\n\n")
            append(section.title.trim())
            append('\n')
            append(note)
        }
    }.trim()
}
