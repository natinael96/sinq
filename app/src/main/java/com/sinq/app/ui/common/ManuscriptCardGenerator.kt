package com.sinq.app.ui.common

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.sinq.app.ui.strings.Strings
import java.io.File
import java.io.FileOutputStream

/**
 * Generates and shares traditional Ethiopian manuscript cards (የብራና ሰሌዳ).
 *
 * Guarantees per Section 8:
 * - Every card includes the source reference (book, chapter, verse) and edition name.
 * - App attribution watermark ("— ስንቅ") is governed by the Settings toggle (on by default),
 *   while the source reference remains always present.
 */
enum class CardAspectRatio(val label: String, val width: Int, val height: Int) {
    SQUARE("Square (1:1)", 1080, 1080),
    STORY("Story (9:16)", 1080, 1920);
}

object ManuscriptCardGenerator {

    private const val TAG = "ManuscriptCardGenerator"
    private const val CARD_WIDTH = 1080
    private const val MIN_CARD_HEIGHT = 1080
    private const val PADDING = 72

    // Manuscript Palette
    private const val COLOR_PARCHMENT = 0xFFFAF7EF.toInt()
    private const val COLOR_INK = 0xFF1C1B1F.toInt()
    private const val COLOR_RUBRIC = 0xFF931A1D.toInt()
    private const val COLOR_GOLD_BORDER = 0xFFD6C7A1.toInt()
    private const val COLOR_MUTED = 0xFF5C5749.toInt()

    fun renderBitmap(
        passage: Passage,
        format: CopyFormat,
        ratio: CardAspectRatio = CardAspectRatio.SQUARE,
    ): Bitmap {
        val cardWidth = ratio.width
        val contentWidth = cardWidth - (PADDING * 2)

        // 1. Text paints
        val rubricPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_RUBRIC
            textSize = if (ratio == CardAspectRatio.STORY) 42f else 38f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK
            textSize = if (ratio == CardAspectRatio.STORY) 46f else 42f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.LEFT
        }

        val referencePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_MUTED
            textSize = 32f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.LEFT
        }

        val watermarkPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_RUBRIC
            textSize = 32f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.RIGHT
        }

        val headingText = PassageFormat.heading(passage) ?: ""
        val bodyText = PassageFormat.body(passage, format).ifBlank { "..." }
        val referenceText = PassageFormat.cardReference(passage)
        val attributionText = PassageFormat.cardAttribution(format)

        val bodyLayout = StaticLayout.Builder.obtain(
            bodyText, 0, bodyText.length, bodyPaint, contentWidth
        ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(16f, 1.3f)
            .setIncludePad(false)
            .build()

        val headerHeight = if (headingText.isNotBlank()) 130 else 40
        val footerHeight = 140
        val totalBlockHeight = headerHeight + bodyLayout.height + footerHeight

        val cardHeight = when (ratio) {
            CardAspectRatio.SQUARE -> maxOf(MIN_CARD_HEIGHT, totalBlockHeight + (PADDING * 2))
            CardAspectRatio.STORY -> ratio.height
        }

        val startY = when (ratio) {
            CardAspectRatio.STORY -> ((cardHeight - totalBlockHeight) / 2f).coerceAtLeast(PADDING.toFloat())
            CardAspectRatio.SQUARE -> {
                if (cardHeight > totalBlockHeight + (PADDING * 2)) {
                    ((cardHeight - totalBlockHeight) / 2f).coerceAtLeast(PADDING.toFloat())
                } else {
                    PADDING.toFloat()
                }
            }
        }

        val bitmap = Bitmap.createBitmap(cardWidth, cardHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw parchment background
        canvas.drawColor(COLOR_PARCHMENT)

        // Draw double gold fillet framing
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GOLD_BORDER
            style = Paint.Style.STROKE
        }

        // Outer border
        borderPaint.strokeWidth = 6f
        canvas.drawRect(28f, 28f, cardWidth - 28f, cardHeight - 28f, borderPaint)

        // Inner border
        borderPaint.strokeWidth = 2f
        canvas.drawRect(42f, 42f, cardWidth - 42f, cardHeight - 42f, borderPaint)

        // Draw ornamental corner rosettes (፨)
        val rosettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_RUBRIC
            textSize = 34f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("፨", 35f, 48f, rosettePaint)
        canvas.drawText("፨", cardWidth - 35f, 48f, rosettePaint)
        canvas.drawText("፨", 35f, cardHeight - 34f, rosettePaint)
        canvas.drawText("፨", cardWidth - 35f, cardHeight - 34f, rosettePaint)

        // Draw Header / Heading
        var currentY = startY + 30f
        if (headingText.isNotBlank()) {
            canvas.drawText("✦  $headingText  ✦", cardWidth / 2f, currentY + 30f, rubricPaint)
            currentY += headerHeight
        } else {
            currentY += 40f
        }

        // Draw Body text
        canvas.save()
        canvas.translate(PADDING.toFloat(), currentY)
        bodyLayout.draw(canvas)
        canvas.restore()

        // Draw Footer: source reference is ALWAYS present
        val footerY = if (ratio == CardAspectRatio.STORY) {
            maxOf(cardHeight - PADDING.toFloat(), currentY + bodyLayout.height + 80f)
                .coerceAtMost(cardHeight - 40f)
        } else {
            cardHeight - PADDING.toFloat()
        }
        if (referenceText.isNotBlank()) {
            canvas.drawText(referenceText, PADDING.toFloat(), footerY, referencePaint)
        }

        // Draw Watermark attribution if enabled by settings toggle
        if (attributionText != null) {
            canvas.drawText(attributionText, cardWidth - PADDING.toFloat(), footerY, watermarkPaint)
        }

        return bitmap
    }

    /**
     * Renders the card, writes to local cache, and triggers the Android share sheet.
     */
    fun shareCard(
        context: Context,
        passage: Passage,
        format: CopyFormat,
        ratio: CardAspectRatio = CardAspectRatio.SQUARE,
        strings: Strings? = null,
    ): Boolean {
        return runCatching {
            val bitmap = renderBitmap(passage, format, ratio)
            val shareDir = File(context.cacheDir, "shares").apply { mkdirs() }
            val imageFile = File(shareDir, "manuscript_card.png")

            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile,
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val subject = PassageFormat.heading(passage) ?: "ስንቅ"
                putExtra(Intent.EXTRA_SUBJECT, subject)
            }

            context.startActivity(Intent.createChooser(shareIntent, strings?.shareCardAction ?: "Share Card"))
            true
        }.onFailure {
            Log.e(TAG, "Failed to share manuscript card", it)
            strings?.let { s -> Toast.makeText(context, s.shareFailed, Toast.LENGTH_SHORT).show() }
        }.getOrDefault(false)
    }
}
