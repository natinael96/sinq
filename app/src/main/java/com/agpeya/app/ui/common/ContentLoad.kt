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

@Composable
fun <T> rememberContentLoad(vararg keys: Any?, load: suspend () -> T): ContentLoad<T> {
    var attempt by remember { mutableIntStateOf(0) }
    val state = key(*keys, attempt) {
        produceState<Result<T>?>(null) {
            value = try { Result.success(load()) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { Result.failure(error) }
        }
    }
    return ContentLoad(state.value) { attempt++ }
}

/** Returns true when this recovery screen replaces the regular content. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun <T> contentLoadScreen(load: ContentLoad<T>, title: String, onBack: () -> Unit, missing: Boolean = false): Boolean {
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
    val state = key(*keys, attempt) {
        produceState<Result<T>?>(null) {
            try { flow().collect { value = Result.success(it) } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { value = Result.failure(error) }
        }
    }
    return ContentLoad(state.value) { attempt++ }
}
