package com.agpeya.app.ui.nisiha

import androidx.compose.runtime.Composable
import com.agpeya.app.model.JournalKind
import com.agpeya.app.ui.journal.JournalEntryScreen

/** A private note, with the journal's lock, autosave and export exclusion. */
@Composable
fun ConfessionPrepScreen(onBack: () -> Unit) {
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
