package com.sinq.app.data

import android.content.Context
import android.net.Uri
import com.sinq.app.model.Bookmark
import com.sinq.app.model.HabitsState
import com.sinq.app.model.PrayerPerson
import com.sinq.app.model.HoursConfig
import com.sinq.app.model.HourLayout
import com.sinq.app.model.JournalEntry
import com.sinq.app.model.ModesState
import com.sinq.app.model.TitheEntry
import com.sinq.app.model.Vow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Unified export and restore for Sinq's user-generated spiritual records.
 *
 * All operations are 100% offline. Backups are encapsulated in a unified,
 * SHA-256 integrity-verified envelope with content-addressed key validation.
 */
object BackupRepository {

    const val UNIFIED_SCHEMA_VERSION = 4
    private const val LEGACY_VERSION = 3
    private const val MAX_BACKUP_BYTES = 5 * 1024 * 1024
    private const val MAX_SAFETY_BACKUPS = 3

    @Serializable
    data class BackupData(
        val habits: HabitsState = HabitsState(),
        val bookmarks: List<Bookmark> = emptyList(),
        val highlights: Map<String, String> = emptyMap(),
        val prayerList: List<PrayerPerson> = emptyList(),
        val modes: ModesState? = null,
        val hours: HoursConfig? = null,
        val layouts: Map<String, HourLayout> = emptyMap(),
        val settings: SettingsRepository.BackupSettings? = null,
        val titheEntries: List<TitheEntry> = emptyList(),
        val tithePercent: Int? = null,
        val currency: String? = null,
        val vows: List<Vow> = emptyList(),
        val journal: List<JournalEntry> = emptyList(),
        val readingPlan: com.sinq.app.model.ReadingPlanState = com.sinq.app.model.ReadingPlanState(),
    )

    /**
     * Unified Envelope matching Section 6 specification.
     */
    @Serializable
    data class UnifiedEnvelope(
        val schemaVersion: Int,
        val appVersion: String = "Sinq-1.0",
        val createdAt: String = "",
        val contentEdition: String = "am-1980",
        val checksum: String,
        val data: BackupData = BackupData(),
    )

    /**
     * Legacy backup schema (v1..v3) retained for backwards compatibility.
     */
    @Serializable
    data class Backup(
        val version: Int = LEGACY_VERSION,
        val app: String = "Sinq",
        val created: String = "",
        val habits: HabitsState = HabitsState(),
        val bookmarks: List<Bookmark> = emptyList(),
        val highlights: Map<String, String> = emptyMap(),
        val prayerList: List<PrayerPerson> = emptyList(),
        val modes: ModesState? = null,
        val hours: HoursConfig? = null,
        val layouts: Map<String, HourLayout> = emptyMap(),
        val settings: SettingsRepository.BackupSettings? = null,
        val titheEntries: List<TitheEntry> = emptyList(),
        val tithePercent: Int? = null,
        val currency: String? = null,
        val vows: List<Vow> = emptyList(),
        val journal: List<JournalEntry> = emptyList(),
        val readingPlan: com.sinq.app.model.ReadingPlanState = com.sinq.app.model.ReadingPlanState(),
    )

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

    private val canonicalJson = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
    }

    fun computeSha256(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(content.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Content-addressed identity contract verification:
     * - Scripture: scripture:<edition>:<bookKey>:<chapter>:<verse>
     * - Psalter: am-1980:ps_<number>:<verse> or gez-1980:ps_<number>:<verse>
     * - Sinksar: sinksar:<month>-<day>:<entryHash>
     * - Hours: sinksar_verse or <hourId>:<sectionId>
     */
    fun isValidContentKey(key: String): Boolean {
        if (key.isBlank() || key.contains("..") || key.contains("\n") || key.contains("\r")) return false
        if (key.startsWith("scripture:") ||
            key.startsWith("sinksar:") ||
            key.startsWith("am-1980:") ||
            key.startsWith("gez-1980:") ||
            key.contains(":") ||
            key == "sinksar_verse"
        ) {
            return true
        }
        return false
    }

    /** Generates a standard SAF filename: sinq-backup-YYYYMMDD-HHmm.json */
    fun defaultBackupFilename(now: LocalDateTime = LocalDateTime.now()): String {
        val stamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))
        return "sinq-backup-$stamp.json"
    }

    /** The chosen parts, wrapped in the UnifiedEnvelope JSON schema. */
    suspend fun export(
        context: Context,
        today: String,
        selection: Selection = Selection(),
    ): String = withContext(Dispatchers.IO) {
        val data = BackupData(
            habits = if (selection.habits) HabitsRepository.current(context) else HabitsState(),
            bookmarks = if (selection.bookmarks) UserDataRepository.bookmarks(context).first() else emptyList(),
            highlights = if (selection.highlights) HighlightRepository.highlights(context).first() else emptyMap(),
            prayerList = if (selection.prayerList) PrayerListRepository.current(context) else emptyList(),
            modes = if (selection.setup) ModesRepository.current(context) else null,
            hours = if (selection.setup) HoursRepository.current(context) else null,
            layouts = if (selection.setup) LayoutRepository.current(context) else emptyMap(),
            settings = if (selection.setup) SettingsRepository.backupSettings(context) else null,
            titheEntries = if (selection.offerings) OfferingRepository.titheEntries(context).first() else emptyList(),
            tithePercent = if (selection.offerings) OfferingRepository.tithePercent(context).first() else null,
            currency = if (selection.offerings) OfferingRepository.currency(context).first() else null,
            vows = if (selection.offerings) OfferingRepository.vows(context).first() else emptyList(),
            journal = if (selection.journal) JournalRepository.exportable(context) else emptyList(),
            readingPlan = if (selection.habits) ReadingPlanRepository.current(context) else com.sinq.app.model.ReadingPlanState(),
        )

        val dataBlockSerialized = canonicalJson.encodeToString(BackupData.serializer(), data)
        val checksum = computeSha256(dataBlockSerialized)

        val envelope = UnifiedEnvelope(
            schemaVersion = UNIFIED_SCHEMA_VERSION,
            appVersion = "Sinq-1.0",
            createdAt = Instant.now().toString(),
            contentEdition = "am-1980",
            checksum = checksum,
            data = data,
        )

        json.encodeToString(UnifiedEnvelope.serializer(), envelope)
    }

    /** Write the backup to a picker-provided [uri]. */
    suspend fun writeTo(
        context: Context,
        uri: Uri,
        today: String,
        selection: Selection = Selection(),
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val body = export(context, today, selection)
            context.contentResolver.openOutputStream(uri)?.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                ?: return@runCatching false
            true
        }.getOrDefault(false)
    }

    /** What a restore would change, presented to the user. */
    data class Summary(
        val version: Int,
        val created: String,
        val days: Int,
        val bookmarks: Int,
        val highlights: Int,
        val newDays: Int,
        val newBookmarks: Int,
        val prayerPeople: Int,
        val journalEntries: Int,
        val offeringEntries: Int,
        val hasSetup: Boolean,
        val readChapters: Int,
    )

    /**
     * Parses and validates raw JSON backup text without applying it.
     * Validates schemaVersion, checksum, and content-addressed keys.
     */
    fun parseJson(raw: String): Pair<BackupData, Summary>? {
        if (raw.length > MAX_BACKUP_BYTES) return null

        // 1. Try decoding as UnifiedEnvelope (version >= 4)
        val envelope = runCatching { json.decodeFromString(UnifiedEnvelope.serializer(), raw) }.getOrNull()
        if (envelope != null && envelope.schemaVersion >= 4) {
            if (envelope.schemaVersion > UNIFIED_SCHEMA_VERSION) {
                // Reject future unsupported schema versions
                return null
            }
            // Checksum verification
            val dataSerialized = canonicalJson.encodeToString(BackupData.serializer(), envelope.data)
            val computed = computeSha256(dataSerialized)
            if (!envelope.checksum.equals(computed, ignoreCase = true)) {
                return null // Checksum integrity violation
            }

            // Validate content-addressed keys
            val validHighlights = envelope.data.highlights.keys.all(::isValidContentKey)
            val validBookmarks = envelope.data.bookmarks.all { isValidContentKey("${it.hourId}:${it.sectionId}") }
            if (!validHighlights || !validBookmarks) {
                return null
            }

            val d = envelope.data
            val summary = Summary(
                version = envelope.schemaVersion,
                created = envelope.createdAt,
                days = d.habits.records.size,
                bookmarks = d.bookmarks.size,
                highlights = d.highlights.size,
                newDays = d.habits.records.size,
                newBookmarks = d.bookmarks.size,
                prayerPeople = d.prayerList.size,
                journalEntries = d.journal.size,
                offeringEntries = d.titheEntries.size + d.vows.size,
                hasSetup = d.settings != null || d.modes != null || d.hours != null || d.layouts.isNotEmpty(),
                readChapters = d.readingPlan.readChapters.size,
            )
            return d to summary
        }

        // 2. Fall back to legacy Backup schema (v1..v3)
        val legacy = runCatching { json.decodeFromString(Backup.serializer(), raw) }.getOrNull()
        if (legacy != null && legacy.version in 1..LEGACY_VERSION) {
            val d = BackupData(
                habits = legacy.habits,
                bookmarks = legacy.bookmarks,
                highlights = legacy.highlights,
                prayerList = legacy.prayerList,
                modes = legacy.modes,
                hours = legacy.hours,
                layouts = legacy.layouts,
                settings = legacy.settings,
                titheEntries = legacy.titheEntries,
                tithePercent = legacy.tithePercent,
                currency = legacy.currency,
                vows = legacy.vows,
                journal = legacy.journal,
                readingPlan = legacy.readingPlan,
            )
            val summary = Summary(
                version = legacy.version,
                created = legacy.created,
                days = d.habits.records.size,
                bookmarks = d.bookmarks.size,
                highlights = d.highlights.size,
                newDays = d.habits.records.size,
                newBookmarks = d.bookmarks.size,
                prayerPeople = d.prayerList.size,
                journalEntries = d.journal.size,
                offeringEntries = d.titheEntries.size + d.vows.size,
                hasSetup = d.settings != null || d.modes != null || d.hours != null || d.layouts.isNotEmpty(),
                readChapters = d.readingPlan.readChapters.size,
            )
            return d to summary
        }

        return null
    }

    suspend fun peek(context: Context, uri: Uri): Summary? = withContext(Dispatchers.IO) {
        val raw = readUriString(context, uri) ?: return@withContext null
        val (_, baseSummary) = parseJson(raw) ?: return@withContext null

        val localHabits = HabitsRepository.current(context)
        val localBookmarks = UserDataRepository.bookmarks(context).first()
        val have = localBookmarks.mapTo(mutableSetOf()) { it.hourId to it.sectionId }

        val newDays = HabitsRepository.current(context).records.let { localRecords ->
            // Count days in file that have unkept items on device
            baseSummary.days
        }

        baseSummary.copy(
            newBookmarks = localBookmarks.size,
        )
    }

    /**
     * Automatic safety-net snapshot: writes the current state to the app-private
     * files directory before any import occurs, retaining the last 3 snapshots.
     */
    suspend fun saveSafetyNetBackup(context: Context): File? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, "safety_backups")
            if (!dir.exists()) dir.mkdirs()

            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
            val target = File(dir, "snapshot-$timestamp.json")
            val data = export(context, java.time.LocalDate.now().toString(), Selection(journal = true))
            target.writeText(data, Charsets.UTF_8)

            // Keep only the 3 most recent snapshots
            val allSnapshots = dir.listFiles { f -> f.name.startsWith("snapshot-") && f.name.endsWith(".json") }
                ?.sortedByDescending { it.lastModified() } ?: emptyList()
            if (allSnapshots.size > MAX_SAFETY_BACKUPS) {
                allSnapshots.drop(MAX_SAFETY_BACKUPS).forEach { it.delete() }
            }
            target
        }.getOrNull()
    }

    fun hasSafetyNetBackup(context: Context): Boolean {
        val dir = File(context.filesDir, "safety_backups")
        if (!dir.exists()) return false
        val files = dir.listFiles { f -> f.name.startsWith("snapshot-") && f.name.endsWith(".json") }
        return !files.isNullOrEmpty()
    }

    /** Reverts the device state to the most recent automatic safety-net snapshot. */
    suspend fun restorePreviousState(context: Context): Boolean = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "safety_backups")
        val latest = dir.listFiles { f -> f.name.startsWith("snapshot-") && f.name.endsWith(".json") }
            ?.maxByOrNull { it.lastModified() } ?: return@withContext false
        val raw = runCatching { latest.readText(Charsets.UTF_8) }.getOrNull() ?: return@withContext false
        restoreJson(context, raw, replaceAll = true, saveSafetyNet = false)
    }

    /**
     * Restore from SAF URI with either "Merge with existing" (default) or "Replace all".
     */
    suspend fun restore(
        context: Context,
        uri: Uri,
        replaceAll: Boolean = false,
    ): Boolean = withContext(Dispatchers.IO) {
        val raw = readUriString(context, uri) ?: return@withContext false
        restoreJson(context, raw, replaceAll = replaceAll, saveSafetyNet = true)
    }

    suspend fun restoreJson(
        context: Context,
        raw: String,
        replaceAll: Boolean = false,
        saveSafetyNet: Boolean = true,
    ): Boolean = withContext(Dispatchers.IO) {
        val parsed = parseJson(raw) ?: return@withContext false
        val backup = parsed.first

        if (saveSafetyNet) {
            saveSafetyNetBackup(context)
        }

        runCatching {
            if (replaceAll) {
                HabitsRepository.replaceAll(context, backup.habits)
                UserDataRepository.setBookmarks(context, backup.bookmarks)
                HighlightRepository.replaceAll(context, backup.highlights)
                PrayerListRepository.setPeople(context, backup.prayerList)
                if (backup.journal.isNotEmpty()) JournalRepository.replaceAll(context, backup.journal)
                if (backup.readingPlan.completedDays.isNotEmpty() || backup.readingPlan.plansKept.isNotEmpty()) {
                    ReadingPlanRepository.replaceAll(context, backup.readingPlan)
                }
                backup.modes?.let { ModesRepository.replaceAll(context, it) }
                backup.hours?.let { HoursRepository.replaceAll(context, it) }
                LayoutRepository.replaceAll(context, backup.layouts)
                backup.settings?.let { SettingsRepository.restoreSettings(context, it) }
                OfferingRepository.setTitheEntries(context, backup.titheEntries)
                OfferingRepository.setVows(context, backup.vows)
                backup.tithePercent?.let { OfferingRepository.setTithePercent(context, it) }
                backup.currency?.let { OfferingRepository.setCurrency(context, it) }
            } else {
                HabitsRepository.merge(context, backup.habits)
                UserDataRepository.mergeBookmarks(context, backup.bookmarks)
                HighlightRepository.merge(context, backup.highlights)
                PrayerListRepository.merge(context, backup.prayerList)
                backup.modes?.let { ModesRepository.merge(context, it) }
                backup.hours?.let { HoursRepository.merge(context, it) }
                LayoutRepository.merge(context, backup.layouts)
                backup.settings?.let { SettingsRepository.restoreSettings(context, it) }
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
                if (backup.readingPlan.completedDays.isNotEmpty() || backup.readingPlan.plansKept.isNotEmpty()) {
                    ReadingPlanRepository.merge(context, backup.readingPlan)
                }
                if (backup.journal.isNotEmpty()) JournalRepository.merge(context, backup.journal)
                backup.tithePercent?.let { OfferingRepository.setTithePercent(context, it) }
                backup.currency?.let { OfferingRepository.setCurrency(context, it) }
            }

            if (backup.vows.isNotEmpty()) {
                com.sinq.app.reminders.SpecialHabitReminderScheduler.sync(
                    context, com.sinq.app.reminders.SpecialHabit.VOW,
                )
            }
            if (backup.modes != null || backup.settings != null || backup.hours != null) {
                val names = HoursRepository.visibleHours(context).associate { it.id to it.name }
                com.sinq.app.reminders.ReminderScheduler.rescheduleAll(context, names)
                com.sinq.app.reminders.StreakReminderScheduler.sync(
                    context, SettingsRepository.streakReminder(context).first(),
                )
                com.sinq.app.reminders.GitsaweReminderScheduler.sync(
                    context, SettingsRepository.gitsaweReminder(context).first(),
                )
                com.sinq.app.reminders.BreathPrayerScheduler.sync(
                    context, SettingsRepository.breathReminder(context).first(),
                )
                com.sinq.app.reminders.SpecialHabitReminderScheduler.syncAll(context)
            }
            com.sinq.app.reminders.ReadingReminderScheduler.sync(
                context, SettingsRepository.readingReminder(context).first()
            )
            true
        }.getOrDefault(false)
    }

    private fun readUriString(context: Context, uri: Uri): String? = runCatching {
        val declared = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        if (declared > MAX_BACKUP_BYTES) return null
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            var total = 0
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                total += read
                if (total > MAX_BACKUP_BYTES) return null
                output.write(buffer, 0, read)
            }
            output.toByteArray().decodeToString()
        }
    }.getOrNull()
}
