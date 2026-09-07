package com.agpeya.app.ui.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agpeya.app.ui.common.NavRow
import com.agpeya.app.ui.common.SectionHeader
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.Spacing

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
                NavRow(s.settingsTitheTitle, onOpenTithe, subtitle = s.settingsTitheDesc)
                NavRow(s.settingsVowTitle, onOpenVows, subtitle = s.settingsVowDesc)
                NavRow(s.settingsPenanceTitle, onOpenPenance, subtitle = s.settingsPenanceDesc)
                Spacer(Modifier.height(Spacing.lg))
                SectionHeader(s.settingsGroupReading)
                NavRow(
                    s.marksTitle,
                    onOpenMarks,
                    subtitle = "${s.marksTabBookmarks} · ${s.marksTabHighlights} · ${s.marksTabNotes}",
                )
                NavRow(s.prayerListTitle, onOpenPrayerList)
                NavRow(s.fastingTitle, onOpenFasting)
                Spacer(Modifier.height(Spacing.xxl))
            }
        }
    }
}
