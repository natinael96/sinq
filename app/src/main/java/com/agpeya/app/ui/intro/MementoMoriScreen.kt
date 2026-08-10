package com.agpeya.app.ui.intro

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agpeya.app.ui.strings.LocalStrings
import kotlinx.coroutines.delay

/** How long the reminder rests on screen before the app opens. */
private const val HOLD_MS = 2200L
private const val FADE_MS = 700

/**
 * The opening breath: *memento mori* — remember that you will die — the ancient
 * monastic reminder that frames why one prays at all. Held for a moment, then
 * the app opens; a tap moves on early for anyone who doesn't want to wait.
 *
 * [onDone] is called exactly once, whether by timeout or by tap.
 */
@Composable
fun MementoMoriScreen(onDone: () -> Unit) {
    val s = LocalStrings.current
    var dismissed by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf(false) }

    fun finish() {
        if (!dismissed) {
            dismissed = true
            onDone()
        }
    }

    LaunchedEffect(Unit) {
        shown = true
        delay(HOLD_MS)
        finish()
    }

    val fade by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = FADE_MS, easing = LinearEasing),
        label = "memento-fade",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { finish() },
        contentAlignment = Alignment.Center,
    ) {
        // A faint gold bloom behind the words, the same glow the Home hero uses.
        Box(
            Modifier
                .align(Alignment.Center)
                .height(260.dp)
                .width(260.dp)
                .alpha(fade * 0.5f)
                .background(
                    Brush.radialGradient(
                        listOf(MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f), Color.Transparent),
                    ),
                    CircleShape,
                ),
        )
        Column(
            modifier = Modifier.alpha(fade).padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "memento mori",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 4.sp,
                ),
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
            )
            androidx.compose.foundation.layout.Spacer(Modifier.height(14.dp))
            Text(
                text = s.mementoMoriGloss,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
