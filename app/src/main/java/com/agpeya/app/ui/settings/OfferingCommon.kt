package com.agpeya.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import com.agpeya.app.model.Cents
import com.agpeya.app.ui.common.ListRow
import com.agpeya.app.ui.common.formatEthiopian
import com.agpeya.app.ui.strings.Strings
import com.agpeya.app.ui.theme.Spacing
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Money formatting and entry, shared by the አስራት ledger and the ስዕለት record.
 *
 * Amounts are held as whole minor units ([Cents]) everywhere and only ever
 * become a decimal at the edges — here, on the way to a screen or back from a
 * text field. A tithe is arithmetic somebody checks against their own notes,
 * and floating point would eventually make the app disagree with them.
 */

/** "1,500.00 ብር", or without the currency when none is set. */
fun formatCents(cents: Cents, currency: String): String =
    "%,.2f %s".format(cents / 100.0, currency).trim()

/**
 * Read a typed amount into minor units, or null if it isn't a number.
 *
 * Tolerant of how people actually type: thousands separators, a stray space,
 * and either a dot or the comma used as a decimal mark in some locales. More
 * than two decimal places are truncated rather than rounded up, so the app
 * never records a larger gift than was entered.
 */
fun parseAmount(text: String): Cents? {
    val cleaned = text.trim().replace(" ", "")
    if (cleaned.isEmpty()) return null
    // A comma is a decimal mark only when it is the sole separator and is
    // followed by one or two digits; otherwise it is grouping and comes out.
    val normalized = if (Regex("""^\d+,\d{1,2}$""").matches(cleaned)) {
        cleaned.replace(',', '.')
    } else {
        cleaned.replace(",", "")
    }
    if (!Regex("""^\d+(\.\d*)?$""").matches(normalized)) return null
    val (whole, fraction) = normalized.split('.').let {
        it[0] to (it.getOrNull(1) ?: "")
    }
    val minor = fraction.padEnd(2, '0').take(2)
    return runCatching { whole.toLong() * 100 + minor.toLong() }.getOrNull()
}

/**
 * The dialog behind every "record" button: an amount, a date, and a note.
 *
 * Save stays disabled until the amount parses, so a typo cannot be filed as
 * zero — a zero line in a ledger reads as "I gave nothing that day", which is a
 * different and false claim.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AmountEntryDialog(
    title: String,
    currency: String,
    s: Strings,
    onDismiss: () -> Unit,
    onSave: (amount: Cents, date: LocalDate, note: String) -> Unit,
    amountOptional: Boolean = false,
    initialAmount: Cents = 0,
    initialNote: String = "",
) {
    var amountText by remember {
        mutableStateOf(if (initialAmount > 0) "%.2f".format(initialAmount / 100.0) else "")
    }
    var note by remember { mutableStateOf(initialNote) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var pickingDate by remember { mutableStateOf(false) }

    val parsed = parseAmount(amountText)
    val valid = if (amountOptional) amountText.isBlank() || parsed != null else parsed != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    singleLine = true,
                    label = { Text(s.amountLabel) },
                    suffix = { Text(currency) },
                    isError = amountText.isNotBlank() && parsed == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Spacing.sm))
                ListRow(
                    title = s.dateLabel,
                    subtitle = formatEthiopian(date, s),
                    onClick = { pickingDate = true },
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    singleLine = true,
                    label = { Text(s.noteLabel) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onSave(parsed ?: 0L, date, note.trim()) },
            ) { Text(s.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )

    if (pickingDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        AlertDialog(
            onDismissRequest = { pickingDate = false },
            title = { Text(s.dateLabel) },
            text = { DatePicker(state = state, showModeToggle = false) },
            confirmButton = {
                TextButton(onClick = {
                    // The picker works in UTC midnights; reading the date back
                    // in UTC keeps a gift recorded on the day it was tapped,
                    // whatever side of midnight the local zone is on.
                    state.selectedDateMillis?.let {
                        date = java.time.Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    pickingDate = false
                }) { Text(s.save) }
            },
            dismissButton = {
                TextButton(onClick = { pickingDate = false }) { Text(s.cancel) }
            },
        )
    }
}

/** One figure of a reckoning: a quiet label over the number it names. */
@Composable
fun FigureCell(label: String, value: String, modifier: Modifier = Modifier, emphasis: Boolean = false) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (emphasis) FontWeight.SemiBold else FontWeight.Normal,
            color = if (emphasis) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.onBackground
            },
        )
    }
}

/** Two figures side by side, each taking half the width. */
@Composable
fun FigureRow(
    leftLabel: String,
    leftValue: String,
    rightLabel: String,
    rightValue: String,
    rightEmphasis: Boolean = false,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        FigureCell(leftLabel, leftValue, Modifier.weight(1f))
        FigureCell(rightLabel, rightValue, Modifier.weight(1f), emphasis = rightEmphasis)
    }
}
