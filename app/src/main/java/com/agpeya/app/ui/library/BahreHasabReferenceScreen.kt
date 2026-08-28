package com.agpeya.app.ui.library

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.agpeya.app.data.GitsaweRepository
import com.agpeya.app.model.BahreHasabReference
import com.agpeya.app.ui.common.LoadingPanel
import com.agpeya.app.ui.common.SinqCard
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.inReadingFont

/** Master Part 5 — finite printed table shown as historical reference. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BahreHasabReferenceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val table by produceState<BahreHasabReference?>(null) { value = GitsaweRepository.bahreHasabReference(context) }
    var selectedRow by rememberSaveable { mutableStateOf<Int?>(null) }
    BackHandler(enabled = selectedRow != null) { selectedRow = null }
    val data = table
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = s.bahreHasabTitle,
                subtitle = selectedRow?.let { data?.rows?.getOrNull(it)?.values?.firstOrNull() }
                    ?: data?.title ?: s.annualTable,
                onBack = { if (selectedRow != null) selectedRow = null else onBack() },
            )
        },
    ) { padding ->
        if (data == null) LoadingPanel(Modifier.padding(padding))
        else if (selectedRow == null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                itemsIndexed(data.rows, key = { _, row -> row.values.first() }) { index, row ->
                    SinqCard(
                        onClick = { selectedRow = index },
                        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
                    ) {
                        Text(row.values[0], style = MaterialTheme.typography.titleSmall.inReadingFont())
                        Text("${row.values[1]} · ትንሣኤ ${row.values[11]}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item { Spacer(Modifier.height(Spacing.xxl)) }
            }
        } else {
            val row = data.rows[selectedRow!!]
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                items((data.columns.size + 1) / 2) { pairIndex ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                        val first = pairIndex * 2
                        ReferenceField(data.columns[first], row.values[first], Modifier.weight(1f))
                        val second = first + 1
                        if (second < data.columns.size) {
                            ReferenceField(data.columns[second], row.values[second], Modifier.weight(1f))
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
                item { Spacer(Modifier.height(Spacing.xxl)) }
            }
        }
    }
}

@Composable
private fun ReferenceField(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        Text(value, style = MaterialTheme.typography.bodyMedium.inReadingFont())
    }
}
