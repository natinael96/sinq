package com.agpeya.app.ui.common

import androidx.compose.runtime.*
import androidx.compose.material3.*
import com.agpeya.app.ui.strings.LocalStrings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Serializes user writes and leaves a recoverable failure visible on the current screen. */
@Stable
class UserAction(private val scope: CoroutineScope) {
    var busy by mutableStateOf(false)
        private set
    var failed by mutableStateOf(false)
        private set
    fun dismissError() { failed = false }
    fun run(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        failed = false
        scope.launch {
            try { action() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { failed = true }
            finally { busy = false }
        }
    }
}

@Composable
fun rememberUserAction(): UserAction {
    val scope = rememberCoroutineScope()
    val action = remember(scope) { UserAction(scope) }
    val s = LocalStrings.current
    if (action.failed) {
        AlertDialog(
            onDismissRequest = action::dismissError,
            text = { Text(if (s.isAmharic) "ይህን ተግባር ማጠናቀቅ አልተቻለም። እንደገና ይሞክሩ።" else "Could not finish this action. Please try again.") },
            confirmButton = { TextButton(onClick = action::dismissError) { Text(s.dismiss) } },
        )
    }
    return action
}
