package com.agpeya.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

/** A local date that advances at midnight and refreshes when the app resumes. */
@Composable
fun rememberCurrentDate(): State<LocalDate> {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentDate = remember { mutableStateOf(LocalDate.now()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) currentDate.value = LocalDate.now()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val now = ZonedDateTime.now()
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            delay(Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1L) + 250L)
            currentDate.value = LocalDate.now()
        }
    }
    return currentDate
}
