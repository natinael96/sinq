package com.agpeya.app.ui.common

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import com.agpeya.app.ui.strings.LocalStrings
import kotlinx.coroutines.CancellationException

data class ContentLoad<T>(val result: Result<T>?, val retry: () -> Unit) {
    val value: T? get() = result?.getOrNull()
}

/**
 * Load something once per set of [keys], with a loading state and a retry.
 *
 * **The keys go to [produceState], never to `key`.** `key` is a compiler
 * intrinsic: it has to read its arguments at the call site to build a group
 * identity, and a `vararg` spread is not something it can enumerate. Given
 * `key(*keys, attempt)` the compiler silently dropped the spread and keyed the
 * group on `attempt` alone — verified in the bytecode, which emitted
 * `startMovableGroup(…, Integer.valueOf(attempt))` and nothing else.
 *
 * The effect was that a key change never restarted the producer: the ግጻዌ screen
 * swiped to the next day, relabelled itself from its own state, and went on
 * showing the previous day's readings, because those were still sitting in a
 * `produceState` that was never asked again. Every screen whose key changes
 * without leaving the screen had the same fault — the Psalter's Ge'ez switch,
 * the reading layout, the Sunday cycle's day.
 *
 * [produceState]'s own `keys` are compared at runtime by `remember`, which
 * handles a spread correctly, so this cannot recur quietly.
 */
@Composable
fun <T> rememberContentLoad(vararg keys: Any?, load: suspend () -> T): ContentLoad<T> {
    var attempt by remember { mutableIntStateOf(0) }
    val state = produceState<Result<T>?>(null, *keys, attempt) {
        // produceState keeps its value across a key change, so the first thing
        // a restart must do is drop an answer that belongs to the old keys —
        // otherwise the screen shows the previous load while this one runs,
        // which is the bug this whole comment is about, only briefer.
        value = null
        value = try { Result.success(load()) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { Result.failure(error) }
    }
    return ContentLoad(state.value) { attempt++ }
}

/** Returns true when this recovery screen replaces the regular content. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun <T> contentLoadScreen(
    load: ContentLoad<T>,
    title: String,
    onBack: () -> Unit,
    missing: Boolean = false,
    comingSoon: Boolean = false,
): Boolean {
    if (comingSoon) {
        val s = LocalStrings.current
        Scaffold(topBar = { SinqTopBar(title, onBack) }) { padding ->
            StatePanel(
                title = s.comingSoon,
                body = s.comingSoonBody,
                modifier = Modifier.padding(padding),
            )
        }
        return true
    }
    if (load.result?.isSuccess == true && !missing) return false
    val s = LocalStrings.current
    Scaffold(topBar = { SinqTopBar(title, onBack) }) { padding ->
        if (load.result == null) LoadingPanel(Modifier.padding(padding))
        else StatePanel(
            title = s.contentUnavailable, body = s.contentMissingBody,
            actionLabel = s.retryAction, onAction = load.retry, modifier = Modifier.padding(padding),
        )
    }
    return true
}

/** Keeps loading distinct from a genuinely empty saved collection, and permits retry. */
@Composable
fun <T> rememberFlowLoad(vararg keys: Any?, flow: () -> kotlinx.coroutines.flow.Flow<T>): ContentLoad<T> {
    var attempt by remember { mutableIntStateOf(0) }
    val state = produceState<Result<T>?>(null, *keys, attempt) {
        // Keys via produceState, not key(); see [rememberContentLoad].
        value = null
        try { flow().collect { value = Result.success(it) } }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { value = Result.failure(error) }
    }
    return ContentLoad(state.value) { attempt++ }
}
