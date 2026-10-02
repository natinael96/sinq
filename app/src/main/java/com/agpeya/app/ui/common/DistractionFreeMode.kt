package com.agpeya.app.ui.common

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay

/**
 * Manages Distraction-Free Prayer Mode for sacred liturgical readers.
 *
 * Immerses the worshipper into a quiet sanctuary by:
 * 1. Hiding the Android system bars (status bar with clock/battery/notifications, and navigation bar)
 *    using [WindowInsetsControllerCompat] with transient swipe-to-reveal.
 * 2. Auto-entering immersion after an initial delay (default 3 seconds) or upon reading scroll.
 * 3. Restoring system bars smoothly on tap or when navigating away from the reader.
 */
class DistractionFreeState(
    initialImmersive: Boolean = false,
) {
    var isImmersive by mutableStateOf(initialImmersive)
    var interactionCount by mutableIntStateOf(0)
        private set

    fun toggle() {
        isImmersive = !isImmersive
        interactionCount++
    }

    fun show() {
        if (isImmersive) {
            isImmersive = false
            interactionCount++
        }
    }

    fun hide() {
        if (!isImmersive) {
            isImmersive = true
            interactionCount++
        }
    }
}

@Composable
fun rememberDistractionFreeState(
    initialDelayMs: Long = 3000L,
    pauseAutoImmersion: Boolean = false,
): DistractionFreeState {
    val state = remember { DistractionFreeState() }
    val window = LocalActivity.current?.window

    // WindowInsets immersion: hide system bars when immersive, restore when normal
    DisposableEffect(window, state.isImmersive) {
        if (window != null) {
            val insets = WindowCompat.getInsetsController(window, window.decorView)
            insets.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (state.isImmersive) {
                insets.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                insets.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            if (window != null) {
                val insets = WindowCompat.getInsetsController(window, window.decorView)
                insets.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Auto-immersion timer: after initial delay or when controls become visible,
    // automatically immerse after [initialDelayMs] unless paused by an active sheet or menu.
    LaunchedEffect(state.isImmersive, pauseAutoImmersion, state.interactionCount) {
        if (!state.isImmersive && !pauseAutoImmersion) {
            delay(initialDelayMs)
            state.hide()
        }
    }

    return state
}
