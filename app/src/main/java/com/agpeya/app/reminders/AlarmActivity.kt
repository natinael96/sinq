package com.agpeya.app.reminders

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.agpeya.app.MainActivity
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.stringsFor
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.AgpeyaTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Full-screen alarm shown when a reminder fires — over the lock screen. */
class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val hourId = intent.getStringExtra(ReminderScheduler.EXTRA_HOUR_ID) ?: "morning"
        val hourName = intent.getStringExtra(ReminderScheduler.EXTRA_HOUR_NAME) ?: ""
        val strings = stringsFor(runBlocking { SettingsRepository.language(this@AlarmActivity).first() })

        setContent {
            AgpeyaTheme {
                CompositionLocalProvider(LocalStrings provides strings) {
                    AlarmScreen(
                        hourName = hourName,
                        onSnooze = {
                            ReminderScheduler.snooze(this, hourId, hourName)
                            AlarmService.dismiss(this)
                            finish()
                        },
                        onDismiss = {
                            AlarmService.dismiss(this)
                            finish()
                        },
                        onOpen = {
                            AlarmService.dismiss(this)
                            startActivity(
                                Intent(this, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    putExtra(ReminderScheduler.EXTRA_HOUR_ID, hourId)
                                }
                            )
                            finish()
                        },
                    )
                }
            }
        }
    }
}

// Green & gold alarm screen — deep liturgical green ground, gold action.
private val AlarmGround = Color(0xFF0B3129)
private val AlarmGold = Color(0xFFE4BC5A)
private val AlarmIvory = Color(0xFFF2EDDE)
private val AlarmOnGold = Color(0xFF123829)

@Composable
private fun AlarmScreen(
    hourName: String,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
) {
    val s = LocalStrings.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AlarmGround),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = s.itsTime,
                style = MaterialTheme.typography.headlineMedium,
                color = AlarmIvory,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(64.dp))
            Button(
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlarmGold,
                    contentColor = AlarmOnGold,
                ),
            ) { Text(s.openShort) }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("${s.snooze} · ${ReminderScheduler.SNOOZE_MINUTES}'", color = AlarmIvory)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(s.dismiss, color = AlarmIvory)
            }
        }
    }
}
