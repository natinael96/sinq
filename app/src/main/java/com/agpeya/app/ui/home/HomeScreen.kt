package com.agpeya.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.ContentRepository
import com.agpeya.app.data.UserDataRepository
import com.agpeya.app.model.Hour
import com.agpeya.app.ui.common.AgpeyaBottomBar
import com.agpeya.app.ui.common.Tab
import java.time.LocalTime

@Composable
fun HomeScreen(
    onOpenHour: (String) -> Unit,
    onOpenModes: () -> Unit,
    onSelectTab: (Tab) -> Unit,
) {
    val context = LocalContext.current
    val hours by produceState<List<Hour>>(initialValue = emptyList()) {
        value = ContentRepository.hours(context)
    }
    val recentIds by UserDataRepository.recents(context).collectAsState(initial = emptyList())
    val suggestedId = remember { ContentRepository.suggestedHourId(LocalTime.now().hour) }
    val suggested = hours.find { it.id == suggestedId }
    val recents = recentIds.mapNotNull { id -> hours.find { it.id == id } }
    val s = com.agpeya.app.ui.strings.LocalStrings.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { AgpeyaBottomBar(current = Tab.HOME, onSelect = onSelectTab) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 24.dp),
        ) {
            item {
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "ጸሎት",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    IconButton(onClick = onOpenModes) {
                        Icon(
                            Icons.Outlined.Notifications,
                            contentDescription = "የማንቂያ ሁነታዎች",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            if (suggested != null) {
                item {
                    SuggestedCard(hour = suggested, onClick = { onOpenHour(suggested.id) })
                    Spacer(Modifier.height(20.dp))
                }
            }
            if (recents.isNotEmpty()) {
                item {
                    Text(
                        text = s.continueReading,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(recents, key = { it.id }) { hour ->
                            RecentChip(hour = hour, onClick = { onOpenHour(hour.id) })
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
            items(hours, key = { it.id }) { hour ->
                HourRow(hour = hour, onClick = { onOpenHour(hour.id) })
                if (hour.id != hours.lastOrNull()?.id) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SuggestedCard(hour: Hour, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primary,
    ) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 22.dp)) {
            Text(
                text = com.agpeya.app.ui.strings.LocalStrings.current.nowPrayer,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = hour.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun RecentChip(hour: Hour, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = hour.name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun HourRow(hour: Hour, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = hour.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = hour.timeHint,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
