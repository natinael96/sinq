package com.agpeya.app.ui.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.agpeya.app.ui.strings.Strings

/**
 * Copying and sharing prayer text. Everything that leaves the app carries the
 * same signature line, so a verse forwarded into a chat still says where it
 * came from.
 */
object Sharing {

    /** The line appended to every shared or copied passage. */
    private const val SIGNATURE = "— ስንቅ"

    /** [body] with the app signature appended, separated by a blank line. */
    fun sign(body: String): String = body.trimEnd() + "\n\n" + SIGNATURE

    /**
     * Put [body] on the clipboard, signed. Android 13+ shows its own copy
     * confirmation, so the toast is only for older versions that show nothing.
     */
    fun copy(context: Context, body: String, s: Strings) {
        val clip = context.getSystemService(ClipboardManager::class.java) ?: return
        clip.setPrimaryClip(ClipData.newPlainText(SIGNATURE, sign(body)))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, s.copiedToast, Toast.LENGTH_SHORT).show()
        }
    }

    /** Open the system share sheet with [body], signed. */
    fun share(context: Context, body: String, subject: String? = null) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, sign(body))
            subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
        }
        context.startActivity(Intent.createChooser(send, null))
    }
}
