package com.agpeya.app.ui.common

import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.agpeya.app.data.SettingsRepository

private val awakeOwners = java.util.WeakHashMap<android.view.Window, Int>()

/** Honor the reading preference consistently, including overlapping reader transitions. */
@Composable
fun ReaderAwake() {
    val context = LocalContext.current
    val window = LocalActivity.current?.window ?: return
    val enabled by SettingsRepository.keepScreenOn(context).collectAsState(initial = false)
    DisposableEffect(window, enabled) {
        if (enabled) {
            awakeOwners[window] = (awakeOwners[window] ?: 0) + 1
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            if (enabled) {
                val remaining = (awakeOwners[window] ?: 1) - 1
                if (remaining == 0) {
                    awakeOwners.remove(window)
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else awakeOwners[window] = remaining
            }
        }
    }
}
