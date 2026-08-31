package com.agpeya.app.data

import android.content.Context
import android.net.Uri
import com.agpeya.app.model.Bookmark
import com.agpeya.app.model.HabitsState
import com.agpeya.app.model.PrayerPerson
import com.agpeya.app.model.HoursConfig
import com.agpeya.app.model.HourLayout
import com.agpeya.app.model.JournalEntry
import com.agpeya.app.model.ModesState
import com.agpeya.app.model.TitheEntry
import com.agpeya.app.model.Vow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Export and restore the things the user builds up and can't get back: the
 * streak/habit history, bookmarks, highlights, and the prayer list.
 *
 * The app is offline by design, so a backup is a file the user writes wherever
 * they like through the system file picker — nothing is uploaded anywhere.
 */
object BackupRepository {

    /** Bumped only if the shape changes incompatibly; readers tolerate older files. */
    private const val VERSION = 2
    private const val MAX_BACKUP_BYTES = 2 * 1024 * 1024

    @Serializable
    data class Backup(
        val version: Int = VERSION,
        /** Whatever the exporting app called itself, for a human reading the file. */
        val app: String = "Sinq",
        /** ISO date the backup was taken. */
        val created: String = "",
        val habits: HabitsState = HabitsState(),
        val bookmarks: List<Bookmark> = emptyList(),
        val highlights: Map<String, String> = emptyMap(),
        /** Added after v1 shipped; defaulted so older files still decode. */
        val prayerList: List<PrayerPerson> = emptyList(),
        /** Added in v2. */
        val modes: ModesState? = null,
        val hours: HoursConfig? = null,
        val layouts: Map<String, HourLayout> = emptyMap(),
        val settings: SettingsRepository.BackupSettings? = null,
        /**
         * The አስራት ledger and ስዕለት list. Records of money given and promises
         * made are exactly the kind of thing a person cannot reconstruct after
         * losing a phone, so they belong in the backup even though nothing else
         * about giving is stored. Defaulted, so a v2 file without them decodes.
         */
        val titheEntries: List<TitheEntry> = emptyList(),
        val tithePercent: Int = OfferingRepository.DEFAULT_TITHE_PERCENT,
        val currency: String = "",
        val vows: List<Vow> = emptyList(),
        /**
         * Journal entries, in PLAINTEXT. Exporting them is opt-in and gated on
         * the journal passphrase, but the file itself is readable by anyone who
         * opens it — see [JournalLock]. ንስሐ drafts are never written here.
         */
        val journal: List<JournalEntry> = emptyList(),
    )

    /**
     * What the person chose to put in the file.
     *
     * A backup used to be all-or-nothing, which was fine while everything in it
     * was innocuous. It no longer is: the አስራት ledger is money and the journal
     * is the interior life, and neither should ride along unasked in a file
     * destined for someone's email. Everything defaults to on except the
     * journal, which must be asked for.
     */
    data class Selection(
        val habits: Boolean = true,
        val bookmarks: Boolean = true,
        val highlights: Boolean = true,
        val prayerList: Boolean = true,
        val setup: Boolean = true,
        val offerings: Boolean = true,
        val journal: Boolean = false,
    )

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    /** The chosen parts, as a JSON document. */
    suspend fun export(
        context: Context,
        today: String,
        selection: Selection = Selection(),
    ): String = withContext(Dispatchers.IO) {
        val backup = Backup(
            created = today,
            habits = if (selection.habits) HabitsRepository.current(context) else HabitsState(),
            bookmarks = if (selection.bookmarks) UserDataRepository.bookmarks(context).first() else emptyList(),
            highlights = if (selection.highlights) HighlightRepository.highlights(context).first() else emptyMap(),
            prayerList = if (selection.prayerList) PrayerListRepository.current(context) else emptyList(),
            modes = if (selection.setup) ModesRepository.current(context) else null,
            hours = if (selection.setup) HoursRepository.current(context) else null,
            layouts = if (selection.setup) LayoutRepository.current(context) else emptyMap(),
            settings = if (selection.setup) SettingsRepository.backupSettings(context) else null,
            titheEntries = if (selection.offerings) OfferingRepository.titheEntries(context).first() else emptyList(),
            tithePercent = OfferingRepository.tithePercent(context).first(),
            currency = OfferingRepository.currency(context).first(),
            vows = if (selection.offerings) OfferingRepository.vows(context).first() else emptyList(),
            // Already filtered of ንስሐ drafts by the repository, so a bug in
            // the picker can never be the thing that lets one out.
            journal = if (selection.journal) JournalRepository.exportable(context) else emptyList(),
        )
        json.encodeToString(Backup.serializer(), backup)
    }

    /** Write the backup to a picker-provided [uri]. */
    suspend fun writeTo(
        context: Context,
        uri: Uri,
        today: String,
        selection: Selection = Selection(),
    ): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = export(context, today, selection)
                context.contentResolver.openOutputStream(uri)?.use { it.write(body.toByteArray()) }
                    ?: return@runCatching false
                true
            }.getOrDefault(false)
        }

    /** What a restore would change, so the user can be told before it happens. */
    data class Summary(
        val version: Int,
        val created: String,
        val days: Int,
        val bookmarks: Int,
        val highlights: Int,
        /** Days present in the file that the device does not already have. */
        val newDays: Int,
        /** Bookmarks in the file that the device does not already have. */
        val newBookmarks: Int,
    )

    /**
     * Read and validate a backup without applying it, reporting both what the
     * file holds and how much of it is actually new here — a restore that would
     * change nothing is worth saying so before the user commits to it.
     *
     * Returns null when the file is unreadable, is not a Sinq backup, or was
     * written by a newer format than this build understands.
     */
    suspend fun peek(context: Context, uri: Uri): Summary? = withContext(Dispatchers.IO) {
        val b = parse(context, uri) ?: return@withContext null
        if (b.version < 1 || b.version > VERSION) return@withContext null

        val localHabits = HabitsRepository.current(context)
        val localBookmarks = UserDataRepository.bookmarks(context).first()
        val have = localBookmarks.mapTo(mutableSetOf()) { it.hourId to it.sectionId }

        Summary(
            version = b.version,
            created = b.created,
            days = b.habits.records.size,
            bookmarks = b.bookmarks.size,
            highlights = b.highlights.size,
            newDays = b.habits.records.keys.count { day ->
                (b.habits.records[day] ?: emptySet()) - (localHabits.records[day] ?: emptySet()) != emptySet<String>()
            },
            newBookmarks = b.bookmarks.count { (it.hourId to it.sectionId) !in have },
        )
    }

    /**
     * Restore a backup, merging rather than replacing: a day marked done in
     * either the file or the device stays done, and existing bookmarks and
     * highlights survive. Restoring an old backup can therefore never erase
     * progress made since it was taken.
     */
    suspend fun restore(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val backup = parse(context, uri) ?: return@withContext false
        // Refuse a file from a newer format rather than half-applying it.
        if (backup.version < 1 || backup.version > VERSION) return@withContext false
        runCatching {
            HabitsRepository.merge(context, backup.habits)
            UserDataRepository.mergeBookmarks(context, backup.bookmarks)
            HighlightRepository.merge(context, backup.highlights)
            PrayerListRepository.merge(context, backup.prayerList)
            backup.modes?.let { ModesRepository.merge(context, it) }
            backup.hours?.let { HoursRepository.merge(context, it) }
            LayoutRepository.merge(context, backup.layouts)
            backup.settings?.let { SettingsRepository.restoreSettings(context, it) }
            // Ledger lines and vows merge by id rather than replacing: restoring
            // an old backup must not delete what has been given since.
            if (backup.titheEntries.isNotEmpty()) {
                val existing = OfferingRepository.titheEntries(context).first()
                val merged = (existing + backup.titheEntries.filter { entry ->
                    existing.none { it.id == entry.id }
                }).sortedByDescending { it.date }
                OfferingRepository.setTitheEntries(context, merged)
            }
            if (backup.vows.isNotEmpty()) {
                val existing = OfferingRepository.vows(context).first()
                OfferingRepository.setVows(
                    context,
                    existing + backup.vows.filter { vow -> existing.none { it.id == vow.id } },
                )
            }
            // Journal entries merge last-write-wins; any ንስሐ draft that
            // somehow appears in a file is discarded rather than restored.
            if (backup.journal.isNotEmpty()) JournalRepository.merge(context, backup.journal)
            OfferingRepository.setTithePercent(context, backup.tithePercent)
            if (backup.currency.isNotBlank()) OfferingRepository.setCurrency(context, backup.currency)
            if (backup.vows.isNotEmpty()) {
                com.agpeya.app.reminders.SpecialHabitReminderScheduler.sync(
                    context, com.agpeya.app.reminders.SpecialHabit.VOW,
                )
            }
            if (backup.modes != null || backup.settings != null || backup.hours != null) {
                val names = HoursRepository.visibleHours(context).associate { it.id to it.name }
                com.agpeya.app.reminders.ReminderScheduler.rescheduleAll(context, names)
                com.agpeya.app.reminders.StreakReminderScheduler.sync(
                    context, SettingsRepository.streakReminder(context).first(),
                )
                com.agpeya.app.reminders.GitsaweReminderScheduler.sync(
                    context, SettingsRepository.gitsaweReminder(context).first(),
                )
                com.agpeya.app.reminders.BreathPrayerScheduler.sync(
                    context, SettingsRepository.breathReminder(context).first(),
                )
                com.agpeya.app.reminders.SpecialHabitReminderScheduler.syncAll(context)
            }
            true
        }.getOrDefault(false)
    }

    private fun parse(context: Context, uri: Uri): Backup? = runCatching {
        val declared = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        if (declared > MAX_BACKUP_BYTES) return null
        val raw = context.contentResolver.openInputStream(uri)?.use {
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            var total = 0
            while (true) {
                val read = it.read(buffer)
                if (read < 0) break
                total += read
                if (total > MAX_BACKUP_BYTES) return null
                output.write(buffer, 0, read)
            }
            output.toByteArray().decodeToString()
        } ?: return null
        json.decodeFromString(Backup.serializer(), raw)
    }.getOrNull()
}
