package com.agpeya.app.ui.journal

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.activity.compose.LocalActivity

/**
 * Keeps the journal out of screenshots and the recents thumbnail while it is on
 * screen.
 *
 * FLAG_SECURE is a window flag, and Sinq is a single-Activity app, so it has to
 * be cleared again on the way out — otherwise reading one journal entry would
 * silently make the whole app unscreenshottable for the rest of the session.
 *
 * This is not a serious defence (anyone can photograph a screen), but the
 * recents thumbnail is the realistic leak: the journal should not be sitting
 * there in the app switcher for whoever glances at the phone next.
 */
@Composable
fun SecureScreen() {
    val activity = LocalActivity.current ?: return
    DisposableEffect(Unit) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}
