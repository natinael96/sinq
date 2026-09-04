package com.agpeya.app.ui.nisiha

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.agpeya.app.data.JournalRepository
import com.agpeya.app.data.KurbanRepository
import com.agpeya.app.data.PenanceRepository
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.model.KurbanContent
import com.agpeya.app.model.KurbanPrayer
import com.agpeya.app.ui.common.NavRow
import com.agpeya.app.ui.common.SectionHeader
import com.agpeya.app.ui.common.SinqCard
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * የቁርባን ዝግጅት — the order of approach and its prayers.
 *
 * The checklist is a preparation for one day: its marks silently expire at
 * midnight, are never backed up, and reward nothing. The two status lines
 * speak only in generalities — "a draft is waiting", "a penance remains" —
 * because this screen is not behind the journal lock and must carry no
 * confessional detail.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CommunionPrepScreen(
    onBack: () -> Unit,
    onOpenConfessionPrep: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current

    val content by produceState(KurbanContent()) {
        value = KurbanRepository.load(context)
    }
    val today = remember { LocalDate.now().toString() }
    val checked by SettingsRepository.kurbanChecked(context, today)
        .collectAsState(initial = emptySet())
    val draftCount by JournalRepository.draftCount(context).collectAsState(initial = 0)
    val penances by PenanceRepository.penances(context).collectAsState(initial = emptyList())

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SinqTopBar(title = s.kurbanPrepTitle, onBack = onBack) },
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.md),
        ) {
            SectionHeader(s.kurbanChecklistHeader)
            for (item in content.checklist) {
                val isChecked = item.id in checked
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch {
                                SettingsRepository.setKurbanChecked(
                                    context,
                                    today,
                                    if (isChecked) checked - item.id else checked + item.id,
                                )
                            }
                        }
                        .padding(vertical = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = isChecked, onCheckedChange = null)
                    Spacer(Modifier.width(Spacing.sm))
                    Column {
                        Text(item.title, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            item.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Only shown when something actually stands in the way; a clear
            // path is answered with silence, not congratulation.
            //
            // Unfinished is `settled` alone, never `enabled`: switching a ቀኖና's
            // reminder off silences the nudge, it does not discharge the rule,
            // and this screen must not report a clear path on that account.
            val penanceUnsettled = penances.any { !it.settled }
            if (draftCount > 0 || penanceUnsettled) {
                Spacer(Modifier.height(Spacing.lg))
                SectionHeader(s.kurbanStatusHeader)
                if (draftCount > 0) {
                    NavRow(title = s.kurbanConfessionPending, onClick = onOpenConfessionPrep)
                }
                if (penanceUnsettled) {
                    Text(
                        s.kurbanPenanceUnsettled,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = Spacing.xs),
                    )
                }
            }

            Spacer(Modifier.height(Spacing.lg))
            SectionHeader(s.kurbanPrePrayersHeader)
            for (prayer in content.prePrayers) PrayerCard(prayer)

            Spacer(Modifier.height(Spacing.lg))
            SectionHeader(s.kurbanPostPrayersHeader)
            for (prayer in content.postPrayers) PrayerCard(prayer)

            Spacer(Modifier.height(Spacing.huge))
        }
    }
}

@Composable
private fun PrayerCard(prayer: KurbanPrayer) {
    var expanded by rememberSaveable(prayer.id) { mutableStateOf(false) }
    SinqCard(onClick = { expanded = !expanded }) {
        Text(prayer.title, style = MaterialTheme.typography.titleSmall)
        if (expanded) {
            Spacer(Modifier.height(Spacing.sm))
            Text(prayer.body, style = MaterialTheme.typography.bodyLarge)
        }
    }
    Spacer(Modifier.height(Spacing.sm))
}
