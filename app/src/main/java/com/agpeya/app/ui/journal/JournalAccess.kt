package com.agpeya.app.ui.journal

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.agpeya.app.data.JournalLock
import com.agpeya.app.ui.common.LoadingPanel
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.strings.LocalStrings
import kotlinx.coroutines.flow.MutableStateFlow

/** Foreground-only authorization, shared by every route to private records. */
internal object JournalSession {
    val authenticated = MutableStateFlow(false)
    fun lock() { authenticated.value = false }
    fun mayRead(locked: Boolean?, verified: Boolean): Boolean =
        locked == false || (locked == true && verified)
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun JournalAccess(onBack: () -> Unit, content: @Composable () -> Unit) {
    SecureScreen()
    val context = LocalContext.current
    val owner = LocalActivity.current as? LifecycleOwner
    val locked by JournalLock.isLocked(context).collectAsState(initial = null)
    val authenticated by JournalSession.authenticated.collectAsState()
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) JournalSession.lock()
        }
        owner?.lifecycle?.addObserver(observer)
        onDispose { owner?.lifecycle?.removeObserver(observer) }
    }
    if (JournalSession.mayRead(locked, authenticated)) {
        content()
    } else {
        val s = LocalStrings.current
        Scaffold(topBar = { SinqTopBar(s.journalLockTitle, onBack) }) { inner ->
            Column(Modifier.fillMaxSize().padding(inner)) {
                if (locked == null) LoadingPanel()
                else JournalLockGate(s) { JournalSession.authenticated.value = true }
            }
        }
    }
}
