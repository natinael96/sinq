package com.agpeya.app.ui.common

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.agpeya.app.R
import com.agpeya.app.ui.strings.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import java.io.File
import java.util.ArrayList

/**
 * What a reader hands to the share actions: the passage itself plus the context
 * lines that make it legible outside the app — where it's from ([kicker]), what
 * it is ([title]), and the day it belongs to ([dateLabel]).
 */
data class SharePayload(
    val body: String,
    val kicker: String? = null,
    val title: String? = null,
    val dateLabel: String? = null,
) {
    /** The payload as plain text, for the clipboard and the text share sheet. */
    fun asText(): String = buildString {
        kicker?.takeIf { it.isNotBlank() && it != title }?.let { append(it); append("\n") }
        val heading = listOfNotNull(title, dateLabel).joinToString(" — ")
        if (heading.isNotBlank()) {
            append(heading)
            append("\n\n")
        }
        append(body.trimEnd())
    }
}

/**
 * Renders a passage as a 1080px-wide PNG card in the green & gold identity —
 * the same template for every reader, so a shared ምስባክ, a psalm verse and a
 * ስንክሳር entry all leave the app looking like pages of one book. The height
 * follows the text (a verse makes a compact card, a reading a tall one), capped
 * so a whole chapter ellipsizes rather than producing a scroll-length image.
 *
 * Rendered with the bundled Abyssinica face so the Ethiopic is identical on
 * every device, and shared from the app cache via FileProvider — no storage
 * permission, nothing persisted outside the app.
 */
object PassageShare {

    private const val TAG = "PassageShare"

    private const val W = 1080
    private const val MAX_H = 1920

    /** Card inset from the bitmap edge, and text inset from the card edge. */
    private const val EDGE = 56f
    private const val PAD = 72f

    private val GROUND = Color.parseColor("#0B3129")
    private val CARD = Color.parseColor("#10382F")
    private val CARD_LINE = Color.parseColor("#1B4A3E")
    private val GOLD = Color.parseColor("#E4BC5A")
    private val IVORY = Color.parseColor("#F2EDDE")
    private val MUTED = Color.parseColor("#9DBBAD")

    suspend fun share(context: Context, payload: SharePayload, strings: Strings? = null): Boolean = try {
        val files = withContext(Dispatchers.Default) { renderToCache(context, payload) }
        val uris = ArrayList(files.map {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it)
        })
        withContext(Dispatchers.Main) {
            val send = Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/png"
                if (uris.size == 1) putExtra(Intent.EXTRA_STREAM, uris.first())
                else putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                putExtra(Intent.EXTRA_TEXT, Sharing.sign(payload.asText()))
                putExtra(Intent.EXTRA_TITLE, payload.title ?: payload.kicker)
                clipData = ClipData.newUri(context.contentResolver, payload.title ?: "Sinq", uris.first()).also { clips ->
                    uris.drop(1).forEach { clips.addItem(ClipData.Item(it)) }
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(send, null).apply {
                if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
        true
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Log.e(TAG, "Unable to render or share passage image", failure)
        strings?.let { s ->
            withContext(Dispatchers.Main) {
                Toast.makeText(context, s.shareFailed, Toast.LENGTH_SHORT).show()
            }
        }
        false
    }

    /** Save every rendered page into the user's Pictures/Sinq collection. */
    suspend fun save(context: Context, payload: SharePayload, strings: Strings? = null): Boolean = try {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { "Gallery save requires Android 10+" }
        val files = withContext(Dispatchers.Default) { renderToCache(context, payload) }
        withContext(Dispatchers.IO) {
            files.forEachIndexed { index, file -> saveToPictures(context, file, index, files.size) }
        }
        strings?.let { s -> withContext(Dispatchers.Main) { Toast.makeText(context, s.imageSaved, Toast.LENGTH_SHORT).show() } }
        true
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Log.e(TAG, "Unable to render or save passage image", failure)
        strings?.let { s -> withContext(Dispatchers.Main) { Toast.makeText(context, s.imageSaveFailed, Toast.LENGTH_SHORT).show() } }
        false
    }

    private fun renderToCache(context: Context, payload: SharePayload): List<File> {
        val dir = File(context.cacheDir, "images")
        check(dir.exists() || dir.mkdirs()) { "Could not create image cache" }
        val staleBefore = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        dir.listFiles()?.filter { it.lastModified() < staleBefore }?.forEach { it.delete() }
        val bodies = paginateBodies(context, payload)
        return bodies.mapIndexed { index, body ->
            val bitmap = render(context, payload, body, index + 1, bodies.size)
            val file = File.createTempFile("passage-${index + 1}-", ".png", dir)
            try {
                val written = file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                check(written && file.length() > 0L) { "PNG encoding failed" }
                file
            } catch (failure: Exception) {
                file.delete()
                throw failure
            } finally {
                bitmap.recycle()
            }
        }
    }

    private fun saveToPictures(context: Context, file: File, index: Int, count: Int): Uri {
        val resolver = context.contentResolver
        val suffix = if (count == 1) "" else "-${index + 1}-of-$count"
        val name = "sinq-${System.currentTimeMillis()}$suffix.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Sinq")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = checkNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)) {
            "Gallery insert failed"
        }
        try {
            checkNotNull(resolver.openOutputStream(uri)).use { output -> file.inputStream().use { it.copyTo(output) } }
            resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            return uri
        } catch (failure: Exception) {
            resolver.delete(uri, null, null)
            throw failure
        }
    }

    private fun paginateBodies(context: Context, payload: SharePayload): List<String> {
        val ethiopic = ResourcesCompat.getFont(context, R.font.abyssinica_sil) ?: Typeface.SERIF
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 44f; color = IVORY; typeface = ethiopic }
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 58f; color = IVORY; typeface = Typeface.create(ethiopic, Typeface.BOLD)
        }
        val textWidth = (W - 2 * EDGE - 2 * PAD).toInt()
        val titleHeight = payload.title?.takeIf { it.isNotBlank() }?.let {
            StaticLayout.Builder.obtain(it, 0, it.length, titlePaint, textWidth).setLineSpacing(0f, 1.2f).setMaxLines(4).build().height
        } ?: 0
        val fixed = EDGE + 184f + (if (payload.kicker != null) 90f else 56f) +
            (if (titleHeight > 0) titleHeight + 28f else 0f) + 44f + 150f + EDGE
        val maxLines = ((MAX_H - fixed) / (bodyPaint.fontSpacing * 1.5f)).toInt().coerceAtLeast(4)
        val pages = mutableListOf<String>()
        var remaining = payload.body.trim()
        while (remaining.isNotEmpty()) {
            val full = StaticLayout.Builder.obtain(remaining, 0, remaining.length, bodyPaint, textWidth)
                .setLineSpacing(0f, 1.5f).build()
            val takeLines = minOf(maxLines, full.lineCount)
            val end = full.getLineEnd(takeLines - 1).coerceAtLeast(1)
            pages += remaining.substring(0, end).trimEnd()
            remaining = remaining.substring(end).trimStart()
        }
        return pages.ifEmpty { listOf("") }
    }

    private fun render(context: Context, payload: SharePayload, body: String, page: Int, pageCount: Int): Bitmap {
        val ethiopic = ResourcesCompat.getFont(context, R.font.abyssinica_sil) ?: Typeface.SERIF
        fun paint(size: Float, color: Int, bold: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = if (bold) Typeface.create(ethiopic, Typeface.BOLD) else ethiopic
        }

        val textWidth = (W - 2 * EDGE - 2 * PAD).toInt()
        fun layout(text: String, p: TextPaint, spacing: Float, maxLines: Int = Int.MAX_VALUE): StaticLayout =
            StaticLayout.Builder.obtain(text, 0, text.length, p, textWidth)
                .setLineSpacing(0f, spacing)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setMaxLines(maxLines)
                .setEllipsize(if (maxLines == Int.MAX_VALUE) null else TextUtils.TruncateAt.END)
                .build()

        val kickerPaint = paint(34f, GOLD)
        val titlePaint = paint(58f, IVORY, bold = true)
        val bodyPaint = paint(44f, IVORY)

        val titleLayout = payload.title
            ?.takeIf { it.isNotBlank() }
            ?.let { layout(it, titlePaint, 1.2f, maxLines = 4) }

        // Everything above and below the body is fixed; whatever height budget
        // remains under MAX_H decides how many body lines fit before ellipsis.
        val brandBlock = 140f + 44f            // brand baseline + rule below it
        val kickerBlock = if (payload.kicker != null) 90f else 56f
        val titleBlock = (titleLayout?.height?.plus(28f)) ?: 0f
        val ruleBlock = 44f                    // the short gold rule above the body
        val footerBlock = 150f                 // signature + bottom padding
        val fixed = EDGE + brandBlock + kickerBlock + titleBlock + ruleBlock + footerBlock + EDGE

        val bodyLineHeight = bodyPaint.fontSpacing * 1.5f
        val maxBodyLines = ((MAX_H - fixed) / bodyLineHeight).toInt().coerceAtLeast(4)
        val bodyLayout = layout(body, bodyPaint, 1.5f, maxLines = maxBodyLines)

        val h = (fixed + bodyLayout.height).toInt().coerceIn(640, MAX_H)
        val bmp = Bitmap.createBitmap(W, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        // Ground + soft gold glow in the top corner, then the card.
        c.drawColor(GROUND)
        c.drawRect(
            0f, 0f, W.toFloat(), h.toFloat(),
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    W * 0.86f, h * 0.08f, W * 0.55f,
                    Color.argb(70, 228, 188, 90), Color.TRANSPARENT, Shader.TileMode.CLAMP,
                )
            },
        )
        val card = RectF(EDGE, EDGE, W - EDGE, h - EDGE)
        c.drawRoundRect(card, 48f, 48f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CARD })
        c.drawRoundRect(
            card, 48f, 48f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = CARD_LINE; style = Paint.Style.STROKE; strokeWidth = 3f
            },
        )

        val left = card.left + PAD
        val right = card.right - PAD
        var y = card.top + 140f

        // Brand row: ስንቅ left, the date right, then a hairline rule.
        c.drawText("ስንቅ", left, y, paint(58f, GOLD, bold = true))
        payload.dateLabel?.let {
            val p = paint(32f, MUTED)
            val available = (right - left - paint(58f, GOLD, bold = true).measureText("ስንቅ") - 40f).coerceAtLeast(120f)
            val line = TextUtils.ellipsize(it, p, available, TextUtils.TruncateAt.END).toString()
            c.drawText(line, right - p.measureText(line), y, p)
        }
        y += 44f
        c.drawRect(left, y, right, y + 2f, Paint().apply { color = CARD_LINE })

        payload.kicker?.let {
            y += 90f
            val line = TextUtils.ellipsize(it, kickerPaint, right - left, TextUtils.TruncateAt.END).toString()
            c.drawText(line, left, y, kickerPaint)
        } ?: run { y += 56f }

        titleLayout?.let {
            y += 28f
            c.withTranslation(left, y) { it.draw(this) }
            y += it.height.toFloat()
        }

        // A short gold rule between the heading block and the passage.
        y += 28f
        c.drawRect(left, y, left + 56f, y + 3f, Paint().apply { color = GOLD })
        y += 16f

        c.withTranslation(left, y) { bodyLayout.draw(this) }

        // Footnote: the app's name in Ge'ez script, centred at the foot of the
        // card like a colophon, so every shared passage says where it came from.
        val sigPaint = paint(34f, GOLD)
        val sig = if (pageCount > 1) "— ስንቅ —  $page/$pageCount" else "— ስንቅ —"
        c.drawText(sig, (W - sigPaint.measureText(sig)) / 2f, card.bottom - 56f, sigPaint)

        return bmp
    }

    private inline fun Canvas.withTranslation(x: Float, y: Float, block: Canvas.() -> Unit) {
        save()
        translate(x, y)
        block()
        restore()
    }
}
