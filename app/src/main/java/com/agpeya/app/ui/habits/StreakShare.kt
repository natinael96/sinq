package com.agpeya.app.ui.habits

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
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.agpeya.app.R
import com.agpeya.app.data.HabitsRepository
import com.agpeya.app.ui.common.formatEthiopian
import com.agpeya.app.ui.strings.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/**
 * Renders the streak as a fancy 1080×1350 card — name, overall streak, and the
 * contribution heatmap in the green & gold identity — then opens the share sheet.
 * The image is written to the app cache and shared via FileProvider (no storage
 * permission, nothing persisted outside the app).
 */
object StreakShare {

    private const val W = 1080
    private const val H = 1350

    private val GROUND = Color.parseColor("#0B3129")
    private val CARD = Color.parseColor("#10382F")
    private val CARD_LINE = Color.parseColor("#1B4A3E")
    private val GOLD = Color.parseColor("#E4BC5A")
    private val IVORY = Color.parseColor("#F2EDDE")
    private val MUTED = Color.parseColor("#9DBBAD")
    private val CELL_EMPTY = Color.parseColor("#1B4A3E")

    suspend fun share(
        context: Context,
        records: Map<String, Set<String>>,
        today: LocalDate,
        name: String,
        maxPossible: Int,
        s: Strings,
    ) = withContext(Dispatchers.Default) {
        val bitmap = render(context, records, today, name, maxPossible, s)
        val dir = File(context.cacheDir, "images").apply { mkdirs() }
        val file = File(dir, "streak.png")
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

    private fun render(
        context: Context,
        records: Map<String, Set<String>>,
        today: LocalDate,
        name: String,
        maxPossible: Int,
        s: Strings,
    ): Bitmap {
        val ethiopic = ResourcesCompat.getFont(context, R.font.abyssinica_sil) ?: Typeface.SERIF
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        // Ground + soft gold glow in the top corner.
        c.drawColor(GROUND)
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                W * 0.86f, H * 0.10f, W * 0.55f,
                Color.argb(70, 228, 188, 90), Color.TRANSPARENT, Shader.TileMode.CLAMP,
            )
        }
        c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), glow)

        // Card
        val card = RectF(56f, 56f, W - 56f, H - 56f)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CARD }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = CARD_LINE; style = Paint.Style.STROKE; strokeWidth = 3f
        }
        c.drawRoundRect(card, 48f, 48f, cardPaint)
        c.drawRoundRect(card, 48f, 48f, linePaint)

        fun text(size: Float, color: Int, bold: Boolean = false, tf: Typeface = ethiopic) =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = size
                this.color = color
                typeface = if (bold) Typeface.create(tf, Typeface.BOLD) else tf
            }

        val left = card.left + 72f
        val right = card.right - 72f
        var y = card.top + 140f

        // Brand row: ስንቅ left, Ethiopian date right, then a hairline rule.
        c.drawText("ስንቅ", left, y, text(58f, GOLD, bold = true))
        val datePaint = text(32f, MUTED)
        val dateStr = formatEthiopian(today, s)
        c.drawText(dateStr, right - datePaint.measureText(dateStr), y, datePaint)
        y += 44f
        c.drawRect(left, y, right, y + 2f, Paint().apply { color = CARD_LINE })

        // Name
        if (name.isNotBlank()) {
            y += 120f
            c.drawText(name, left, y, text(82f, IVORY, bold = true))
        }

        // Overall streak
        y += 120f
        val overall = HabitsRepository.overallCurrentStreak(records, today)
        c.drawText(s.currentStreakLabel, left, y, text(38f, MUTED))
        y += 140f
        c.drawText(s.daysUnit(overall), left, y, text(150f, GOLD, bold = true))

        // Heatmap — exactly as many recent weeks as fit the card width.
        val cell = 30f
        val gap = 7f
        val col = cell + gap
        val cols = ((right - left + gap) / col).toInt()
        val gridTop = y + 100f
        val thisMonday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val firstWeek = thisMonday.minusWeeks((cols - 1).toLong())
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (w in 0 until cols) {
            val week = firstWeek.plusWeeks(w.toLong())
            val x = left + w * col
            for (d in 0..6) {
                val date = week.plusDays(d.toLong())
                cellPaint.color =
                    if (date.isAfter(today)) withAlpha(CELL_EMPTY, 0.4f)
                    else when (HabitsRepository.level(HabitsRepository.dayCount(records, date), maxPossible)) {
                        0 -> CELL_EMPTY
                        1 -> withAlpha(GOLD, 0.35f)
                        2 -> withAlpha(GOLD, 0.55f)
                        3 -> withAlpha(GOLD, 0.78f)
                        else -> GOLD
                    }
                val top = gridTop + d * col
                c.drawRoundRect(RectF(x, top, x + cell, top + cell), 6f, 6f, cellPaint)
            }
        }

        // Footer, bottom-left inside the card.
        c.drawText(s.streaksTitle, left, card.bottom - 76f, text(32f, MUTED))

        return bmp
    }

    private fun withAlpha(color: Int, alpha: Float): Int =
        Color.argb(
            (alpha * 255).toInt(),
            Color.red(color), Color.green(color), Color.blue(color),
        )
}
