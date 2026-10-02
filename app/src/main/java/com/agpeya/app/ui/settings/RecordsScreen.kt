package com.agpeya.app.ui.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agpeya.app.ui.common.NavRow
import com.agpeya.app.ui.common.SectionHeader
import com.agpeya.app.ui.common.SinqCard
import com.agpeya.app.ui.common.SinqDivider
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * መዝገብ — what the app keeps a record of.
 *
 * These were scattered: አስራት, ስዕለት and ቀኖና sat under the reminders page, which
 * made a money ledger a child of an alarm; ምልክቶቼ, የጸሎት ዝርዝር and አጽዋማት lived
 * only behind ቤት's ⋮ menu and were reachable nowhere else. A reminder is
 * something that rings. A record is something that is kept. They are different
 * things and now they are on different pages.
 */
@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun RecordsScreen(
    onBack: () -> Unit,
    onOpenTithe: () -> Unit,
    onOpenVows: () -> Unit,
    onOpenPenance: () -> Unit,
    onOpenMarks: () -> Unit,
    onOpenPrayerList: () -> Unit,
    onOpenFasting: () -> Unit,
) {
    val s = LocalStrings.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val name by com.agpeya.app.data.SettingsRepository.profileName(context)
        .collectAsState(initial = "")
    val christianName by com.agpeya.app.data.SettingsRepository.christianName(context)
        .collectAsState(initial = "")
    val lastBackupAt by com.agpeya.app.data.SettingsRepository.lastBackupAt(context)
        .collectAsState(initial = 0L)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SinqTopBar(s.settingsGroupRecords, onBack) },
    ) { inner ->
        LazyColumn(
            Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            item {
                SectionHeader(s.remindersGroupGiving)
                SinqCard(contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.xs)) {
                    NavRow(
                        title = s.settingsTitheTitle,
                        onClick = onOpenTithe,
                        subtitle = s.settingsTitheDesc,
                        leadingIcon = Icons.Outlined.CardGiftcard,
                    )
                    SinqDivider()
                    NavRow(
                        title = s.settingsVowTitle,
                        onClick = onOpenVows,
                        subtitle = s.settingsVowDesc,
                        leadingIcon = Icons.Outlined.VolunteerActivism,
                    )
                    SinqDivider()
                    NavRow(
                        title = s.settingsPenanceTitle,
                        onClick = onOpenPenance,
                        subtitle = s.settingsPenanceDesc,
                        leadingIcon = Icons.Outlined.FavoriteBorder,
                    )
                }

                Spacer(Modifier.height(Spacing.lg))
                SectionHeader(s.settingsGroupReading)
                SinqCard(contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.xs)) {
                    NavRow(
                        title = s.marksTitle,
                        onClick = onOpenMarks,
                        subtitle = "${s.marksTabBookmarks} · ${s.marksTabHighlights} · ${s.marksTabNotes}",
                        leadingIcon = Icons.Outlined.Bookmarks,
                    )
                    SinqDivider()
                    NavRow(
                        title = s.prayerListTitle,
                        onClick = onOpenPrayerList,
                        subtitle = if (s.isAmharic) "የግልና የቤተሰብ የጸሎት መዝገብ" else "Intercessory prayer list",
                        leadingIcon = Icons.AutoMirrored.Outlined.FormatListBulleted,
                    )
                    SinqDivider()
                    NavRow(
                        title = s.fastingTitle,
                        onClick = onOpenFasting,
                        subtitle = if (s.isAmharic) "የአጽዋማት የቀን መቁጠሪያና ሥርዓት" else "Fasting calendar and rules",
                        leadingIcon = Icons.Outlined.CalendarMonth,
                    )
                }

                Spacer(Modifier.height(Spacing.lg))
                SectionHeader(s.settingsGroupData)
                Text(
                    if (s.isAmharic) "መረጃዎ በዚህ ስልክ ላይ ይቀመጣል። ለሌላ መሣሪያ ለማዛወር ምትኬ ይፍጠሩ። የማስታወሻ ምትኬ ፋይል በይለፍ ቃል አይጠበቅም፤ ቀኖና በምትኬ አይካተትም።"
                    else "Your records stay on this phone. Create a backup to move them to another device. Exported journal files are not password-protected; penance records are excluded from backups.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spacing.sm),
                )
                SinqCard(contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.xs)) {
                    EditableRow(s.yourNameLabel, name, s.addName) {
                        scope.launch { com.agpeya.app.data.SettingsRepository.setProfileName(context, it) }
                    }
                    SinqDivider()
                    EditableRow(s.christianNameLabel, christianName, s.addChristianName) {
                        scope.launch { com.agpeya.app.data.SettingsRepository.setChristianName(context, it) }
                    }
                    SinqDivider()
                    BackupRows(s)
                }
                if (lastBackupAt > 0L) {
                    val saved = java.time.Instant.ofEpochMilli(lastBackupAt)
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        "${s.lastBackupLabel}: ${com.agpeya.app.ui.common.formatEthiopian(saved, s)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.xs),
                    )
                }
                Spacer(Modifier.height(Spacing.xxl))
            }
        }
    }
}
