package com.sinq.app.ui.nisiha

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.sinq.app.data.JournalLock
import com.sinq.app.model.JournalKind
import com.sinq.app.ui.journal.JournalEntryScreen
import com.sinq.app.ui.journal.JournalLockGate
import com.sinq.app.ui.strings.LocalStrings

/**
 * A private confession preparation note, guarded by biometric/master passphrase gate,
 * hardware-backed AES-256-GCM encryption at rest, and an explicit "ተናዝዣለሁ / Absolved" purge action.
 */
@Composable
fun ConfessionPrepScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val locked by JournalLock.isLocked(context).collectAsState(initial = false)
    var unlockedSession by remember { mutableStateOf(false) }

    if (locked && !unlockedSession) {
        JournalLockGate(
            s = LocalStrings.current,
            onUnlocked = { unlockedSession = true },
        )
    } else {
        JournalEntryScreen(
            entryId = null,
            initialKind = JournalKind.CONFESSION_DRAFT,
            anchorRoute = null,
            anchorLabel = null,
            onBack = onBack,
            onOpenAnchor = {},
            confessionOnly = true,
        )
    }
}
