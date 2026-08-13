package com.agpeya.app.ui.common

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
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.agpeya.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

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
 * every device, and shared from the app cache via FileProvider like StreakShare
 * — no storage permission, nothing persisted outside the app.
 */
object PassageShare {

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

    suspend fun share(context: Context, payload: SharePayload) = withContext(Dispatchers.Default) {
        val bitmap = render(context, payload)
        val dir = File(context.cacheDir, "images").apply { mkdirs() }
        val file = File(dir, "passage.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        withContext(Dispatchers.Main) { context.startActivity(chooser) }
    }

    private fun render(context: Context, payload: SharePayload): Bitmap {
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
        val bodyLayout = layout(payload.body.trim(), bodyPaint, 1.5f, maxLines = maxBodyLines)

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
            c.drawText(it, right - p.measureText(it), y, p)
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
        val sig = "— ስንቅ —"
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
