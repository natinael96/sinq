package com.agpeya.app.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
import kotlinx.coroutines.launch

/**
 * The one share affordance every reader's app bar uses: a share icon opening
 * the same three actions everywhere — copy, share as text, share as a PNG card
 * ([PassageShare]). One menu instead of three icons, so a reading bar that
 * already carries A−/A+ and a bookmark doesn't turn into a toolbar.
 *
 * [payload] is a producer, not a value: the passage is assembled only when the
 * user actually picks an action, so scrolling a long reader never pays for it.
 */
@Composable
fun ShareMenuAction(
    enabled: Boolean = true,
    payload: () -> SharePayload?,
) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    var open by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { open = true }, enabled = enabled) {
            Icon(
                Icons.Outlined.Share,
                contentDescription = s.shareAction,
                modifier = Modifier.size(IconSize.medium),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            MenuItem(s.copyAction, Icons.Outlined.ContentCopy) {
                open = false
                payload()?.let { Sharing.copy(context, it.asText(), s) }
            }
            MenuItem(s.shareAction, Icons.Outlined.Share) {
                open = false
                payload()?.let { Sharing.share(context, it.asText(), it.title ?: it.kicker) }
            }
            MenuItem(s.shareAsImage, Icons.Outlined.Image) {
                open = false
                payload()?.let { scope.launch { PassageShare.share(context, it) } }
            }
        }
    }
}

@Composable
private fun MenuItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(IconSize.medium)) },
        onClick = onClick,
    )
}
