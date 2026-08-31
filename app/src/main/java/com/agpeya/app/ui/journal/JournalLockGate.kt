package com.agpeya.app.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.agpeya.app.data.JournalLock
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.strings.Strings
import com.agpeya.app.ui.theme.Spacing
import kotlinx.coroutines.launch

/** The shortest passphrase worth calling one. */
internal const val MIN_PASSPHRASE = 4

/**
 * Stands in front of the journal until the passphrase is given.
 *
 * The unlock is per-visit and held only in composition — leaving the journal
 * re-locks it. Nothing about the unlocked state is persisted, because a
 * "remember me" flag would quietly turn the lock into decoration.
 */
@Composable
fun JournalLockGate(s: Strings, onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var passphrase by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.screen),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(s.journalLockTitle, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(Spacing.sm))
        Text(
            s.journalLockPrompt,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.lg))
        OutlinedTextField(
            value = passphrase,
            onValueChange = { passphrase = it; wrong = false },
            singleLine = true,
            label = { Text(s.passphraseLabel) },
            isError = wrong,
            supportingText = if (wrong) ({ Text(s.passphraseWrong) }) else null,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.md))
        Button(
            onClick = {
                scope.launch {
                    if (JournalLock.verify(context, passphrase)) onUnlocked() else wrong = true
                }
            },
            enabled = passphrase.isNotEmpty(),
        ) { Text(s.unlockAction) }
    }
}

/**
 * Set, change, or clear the passphrase.
 *
 * The no-recovery warning is shown at the moment of setting, not buried in a
 * help page: this is the only point where the person can still decide to pick
 * something they will remember.
 */
@Composable
fun PassphraseDialog(
    s: Strings = LocalStrings.current,
    onDismiss: () -> Unit,
    onSet: (String) -> Unit,
) {
    var passphrase by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val tooShort = passphrase.isNotEmpty() && passphrase.length < MIN_PASSPHRASE
    val mismatch = confirm.isNotEmpty() && confirm != passphrase
    val valid = passphrase.length >= MIN_PASSPHRASE && confirm == passphrase

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.journalSetPassphrase) },
        text = {
            Column {
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    singleLine = true,
                    label = { Text(s.passphraseLabel) },
                    isError = tooShort,
                    supportingText = if (tooShort) ({ Text(s.passphraseTooShort) }) else null,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Spacing.sm))
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    singleLine = true,
                    label = { Text(s.passphraseConfirmLabel) },
                    isError = mismatch,
                    supportingText = if (mismatch) ({ Text(s.passphraseMismatch) }) else null,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Spacing.md))
                Text(
                    s.passphraseNoRecovery,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onSet(passphrase) }) { Text(s.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}

/**
 * Asks for the existing passphrase before an action that hands the journal to
 * something outside it — today, only the export.
 */
@Composable
fun PassphrasePrompt(
    title: String,
    body: String? = null,
    s: Strings = LocalStrings.current,
    onDismiss: () -> Unit,
    onVerified: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var passphrase by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                body?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(Spacing.sm))
                }
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it; wrong = false },
                    singleLine = true,
                    label = { Text(s.passphraseLabel) },
                    isError = wrong,
                    supportingText = if (wrong) ({ Text(s.passphraseWrong) }) else null,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = passphrase.isNotEmpty(),
                onClick = {
                    scope.launch {
                        if (JournalLock.verify(context, passphrase)) onVerified() else wrong = true
                    }
                },
            ) { Text(s.continueAction) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}
