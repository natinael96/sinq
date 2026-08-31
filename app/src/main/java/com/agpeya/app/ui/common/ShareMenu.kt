package com.agpeya.app.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import android.widget.Toast
import android.os.Build
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
    var imageBusy by remember { mutableStateOf(false) }

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
                payload()?.let { Sharing.share(context, it.asText(), it.title ?: it.kicker, s) }
            }
            MenuItem(s.shareAsImage, Icons.Outlined.Image, enabled = !imageBusy) {
                open = false
                payload()?.let { value -> scope.launch {
                    imageBusy = true
                    Toast.makeText(context, s.imagePreparing, Toast.LENGTH_SHORT).show()
                    try { PassageShare.share(context, value, s) } finally { imageBusy = false }
                } }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MenuItem(s.saveImage, Icons.Outlined.SaveAlt, enabled = !imageBusy) {
                    open = false
                    payload()?.let { value -> scope.launch {
                        imageBusy = true
                        Toast.makeText(context, s.imagePreparing, Toast.LENGTH_SHORT).show()
                        try { PassageShare.save(context, value, s) } finally { imageBusy = false }
                    } }
                }
            }
        }
    }
}

/**
 * Compact reader toolbar for narrow screens. Font controls, optional display
 * toggles, and sharing live behind one overflow icon so the page title and its
 * primary contextual action keep predictable space in the app bar.
 */
@Composable
fun ReaderToolsMenu(
    fontStep: Int,
    maxFontStep: Int,
    onFontChange: (Int) -> Unit,
    shareEnabled: Boolean = true,
    sharePayload: (() -> SharePayload?)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    /** Opens a journal entry anchored to what is being read. */
    onWriteNote: (() -> Unit)? = null,
    onToggleReadingMode: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    var open by remember { mutableStateOf(false) }
    var imageBusy by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                Icons.Outlined.MoreVert,
                contentDescription = s.more,
                modifier = Modifier.size(IconSize.medium),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("A−  ${s.readingFontTitle}") },
                enabled = fontStep > 0,
                onClick = {
                    open = false
                    onFontChange(fontStep - 1)
                },
            )
            DropdownMenuItem(
                text = { Text("A+  ${s.readingFontTitle}") },
                enabled = fontStep < maxFontStep,
                onClick = {
                    open = false
                    onFontChange(fontStep + 1)
                },
            )
            if (secondaryActionLabel != null && onSecondaryAction != null) {
                DropdownMenuItem(
                    text = { Text(secondaryActionLabel) },
                    onClick = {
                        open = false
                        onSecondaryAction()
                    },
                )
            }
            if (onWriteNote != null) {
                DropdownMenuItem(
                    text = { Text(s.writeAboutThis) },
                    onClick = {
                        open = false
                        onWriteNote()
                    },
                )
            }
            if (onToggleReadingMode != null) {
                DropdownMenuItem(
                    text = { Text(s.readingModeToggle) },
                    onClick = {
                        open = false
                        onToggleReadingMode()
                    },
                )
            }
            if (sharePayload != null) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(s.copyAction) },
                    leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                    enabled = shareEnabled,
                    onClick = {
                        open = false
                        sharePayload()?.let { Sharing.copy(context, it.asText(), s) }
                    },
                )
                DropdownMenuItem(
                    text = { Text(s.shareAction) },
                    leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                    enabled = shareEnabled,
                    onClick = {
                        open = false
                        sharePayload()?.let { Sharing.share(context, it.asText(), it.title ?: it.kicker, s) }
                    },
                )
                DropdownMenuItem(
                    text = { Text(s.shareAsImage) },
                    leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) },
                    enabled = shareEnabled && !imageBusy,
                    onClick = {
                        open = false
                        sharePayload()?.let { payload -> scope.launch {
                            imageBusy = true
                            Toast.makeText(context, s.imagePreparing, Toast.LENGTH_SHORT).show()
                            try { PassageShare.share(context, payload, s) } finally { imageBusy = false }
                        } }
                    },
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    DropdownMenuItem(
                        text = { Text(s.saveImage) },
                        leadingIcon = { Icon(Icons.Outlined.SaveAlt, contentDescription = null) },
                        enabled = shareEnabled && !imageBusy,
                        onClick = {
                            open = false
                            sharePayload()?.let { payload -> scope.launch {
                                imageBusy = true
                                Toast.makeText(context, s.imagePreparing, Toast.LENGTH_SHORT).show()
                                try { PassageShare.save(context, payload, s) } finally { imageBusy = false }
                            } }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuItem(label: String, icon: ImageVector, enabled: Boolean = true, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(IconSize.medium)) },
        enabled = enabled,
        onClick = onClick,
    )
}
