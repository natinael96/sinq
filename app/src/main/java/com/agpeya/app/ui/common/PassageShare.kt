package com.agpeya.app.ui.common

import androidx.core.graphics.withSave
import androidx.core.graphics.toColorInt
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
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
/**
 * The shape of the card. One renderer, three sizes: the tall card the app has
 * always made, the square Telegram and Instagram previews crop to, and the
 * story people actually post.
 */
enum class ImageShape { CARD, SQUARE, STORY }

/** The grounds: the app's own green, its ivory, and a near-black for the night. */
enum class ImageGround { GREEN, IVORY, NIGHT }

/** How the passage sits in its column. */
enum class CardAlign { START, CENTER, JUSTIFY }

/** Where the whole block sits in a frame taller than it needs. */
enum class BlockPosition { TOP, CENTER, BOTTOM }

/** The inset from the card edge to the text, as three steps rather than a number. */
enum class CardMargin(val pad: Float) { TIGHT(52f), NORMAL(72f), GENEROUS(96f) }

/**
 * The named starting points.
 *
 * A preset is not a style on top of the card — it *is* a whole [CardSpec]. The
 * person reaching for this is sharing a verse, not designing, so the useful
 * unit is "the one that looks right for a verse", not eleven controls they have
 * to get right themselves.
 */
enum class CardPreset { VERSE, PASSAGE, STORY, FEAST, PLAIN }

/**
 * Everything about how a card is drawn, in one value.
 *
 * The editor mutates this, the preview renders it small and the export renders
 * the identical function large. There is exactly one function that knows how to
 * draw a card, so what the reader saw and what they sent cannot drift apart.
 */
data class CardSpec(
    val preset: CardPreset = CardPreset.PASSAGE,
    val shape: ImageShape = ImageShape.CARD,
    val ground: ImageGround = ImageGround.GREEN,
    /** null means the size is chosen from the passage; a number overrides it. */
    val sizeOverride: Float? = null,
    val spacing: Float = BODY_SPACING,
    val align: CardAlign = CardAlign.START,
    val position: BlockPosition = BlockPosition.CENTER,
    val margin: CardMargin = CardMargin.NORMAL,
    val showDate: Boolean = true,
    val showColophon: Boolean = true,
) {
    companion object {
        /** What the quick share has always produced, for a payload's own choices. */
        fun from(payload: SharePayload): CardSpec =
            CardSpec(shape = payload.shape, ground = payload.ground)

        /** The spec a preset stands for. */
        fun of(preset: CardPreset, base: CardSpec = CardSpec()): CardSpec = when (preset) {
            // A verse is the image: square, centred, generous, nothing above it
            // competing for the eye.
            CardPreset.VERSE -> base.copy(
                preset = preset, shape = ImageShape.SQUARE, align = CardAlign.CENTER,
                position = BlockPosition.CENTER, margin = CardMargin.GENEROUS,
                spacing = 1.72f, showDate = false,
            )
            // A reading, set to be read: the card that grows to its text.
            CardPreset.PASSAGE -> base.copy(
                preset = preset, shape = ImageShape.CARD, align = CardAlign.START,
                position = BlockPosition.TOP, margin = CardMargin.NORMAL,
                spacing = BODY_SPACING, showDate = true,
            )
            // For a status: the tall frame, centred so it clears the platform's
            // own chrome at the top and bottom.
            CardPreset.STORY -> base.copy(
                preset = preset, shape = ImageShape.STORY, align = CardAlign.CENTER,
                position = BlockPosition.CENTER, margin = CardMargin.GENEROUS,
                spacing = 1.8f, showDate = false,
            )
            // A feast keeps its date: that is the whole point of sharing it.
            CardPreset.FEAST -> base.copy(
                preset = preset, shape = ImageShape.SQUARE, align = CardAlign.CENTER,
                position = BlockPosition.CENTER, margin = CardMargin.NORMAL,
                spacing = 1.72f, showDate = true,
            )
            // Everything off that can be off.
            CardPreset.PLAIN -> base.copy(
                preset = preset, align = CardAlign.START, position = BlockPosition.CENTER,
                margin = CardMargin.NORMAL, spacing = BODY_SPACING,
                showDate = false, showColophon = false,
            )
        }
    }
}

data class SharePayload(
    val body: String,
    val kicker: String? = null,
    val title: String? = null,
    val dateLabel: String? = null,
    val shape: ImageShape = ImageShape.CARD,
    val ground: ImageGround = ImageGround.GREEN,
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
 * Line spacing for the passage. Ethiopic wants more air than Latin does; this
 * is the floor of what reads comfortably in Abyssinica.
 */
internal const val BODY_SPACING = 1.5f

/**
 * The band the passage is typeset in.
 *
 * [BODY_MIN] is the important half. Below it the Ethiopic stops being
 * comfortable at the size a phone actually displays a shared image, and a card
 * nobody can read is worse than a card that says "1 / 3" — so a passage that
 * will not fit at the floor is paginated rather than shrunk any further.
 */
internal const val BODY_MAX = 78f
internal const val BODY_MIN = 40f
internal const val BODY_STEP = 2f

/**
 * The body size a passage of [chars] characters wants, before it is measured.
 *
 * One size shrunk until the text fits would give a wall of small type for a
 * reading and a lonely line adrift in a story frame for a single verse. The
 * length is what decides the register: a verse is the image and is set as
 * display type; a reading is set to be read.
 *
 * This is only what the length *wants*. [BODY_MIN] is not applied here on
 * purpose: the frame decides whether the wanted size survives, and a story with
 * room for 44 px should not be handed 40 px merely because the passage is long.
 * The floor is enforced where the measuring happens.
 *
 * Top-level rather than a member of [PassageShare], so it can be tested on the
 * JVM: reaching into the object at all runs its palette initialisers, and those
 * need a real android.graphics.Color.
 */
internal fun sizeForLength(chars: Int): Float = when {
    chars <= 90 -> BODY_MAX     // one verse: the verse is the image
    chars <= 180 -> 62f
    chars <= 260 -> 54f
    chars <= 450 -> 48f
    else -> 44f                 // a reading, set to be read
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

    /** A card stops here so a story is visibly the taller frame of the two. */
    private const val CARD_MAX_H = 1440

    /** Card inset from the bitmap edge, and text inset from the card edge. */
    private const val EDGE = 56f
    private const val PAD = 72f

    // ── The chrome ───────────────────────────────────────────────────────────
    // Everything that is not the passage. Named rather than written inline,
    // because the fit has to add them up before the renderer draws them and the
    // two agreeing by coincidence is what went wrong here before.
    private const val BRAND_BASELINE = 140f
    private const val BRAND_RULE_GAP = 44f
    private const val KICKER_GAP = 90f
    private const val NO_KICKER_GAP = 56f
    private const val TITLE_GAP = 28f
    private const val BODY_RULE_GAP = 28f
    private const val BODY_RULE_DROP = 16f
    private const val FOOTER_BLOCK = 150f
    private const val SIG_BASELINE = 56f

    private const val BRAND_SIZE = 58f
    private const val TITLE_SIZE = 58f
    private const val KICKER_SIZE = 34f
    private const val DATE_SIZE = 32f
    private const val SIG_SIZE = 34f

    /** No frame gives the passage less than this, whatever the chrome wants. */
    private const val MIN_BODY_BAND = 180f

    /**
     * The chrome costs the same pixels on a 1080-tall square as on a 1920-tall
     * story, which on the square left barely a third of the frame for the text
     * the card exists to present. On the tight frame it is drawn smaller so the
     * passage keeps the room.
     */
    private fun chromeScale(shape: ImageShape): Float =
        if (shape == ImageShape.SQUARE) 0.78f else 1f

    /**
     * How the body will be typeset, decided once and used by everything after.
     *
     * The bug this replaces: the paginator split pages against a 1920 px budget
     * whatever frame had been chosen, and the renderer then laid that page into
     * the frame the reader actually picked and ellipsized the remainder. On a
     * square, nine lines in fifteen were thrown away without a word. There is
     * one budget now and both halves read it from here.
     */
    private data class Fit(
        val bodySize: Float,
        val pages: List<String>,
        val fixed: Float,
        val frame: Int,
        val chrome: Float,
    )

    /** One palette per ground; the typography and the colophon do not change. */
    private data class Palette(
        val ground: Int,
        val card: Int,
        val line: Int,
        val gold: Int,
        val ink: Int,
        val muted: Int,
        val glow: Int,
    )

    private val GREEN = Palette(
        ground = "#0B3129".toColorInt(),
        card = "#10382F".toColorInt(),
        line = "#1B4A3E".toColorInt(),
        gold = "#E4BC5A".toColorInt(),
        ink = "#F2EDDE".toColorInt(),
        muted = "#9DBBAD".toColorInt(),
        glow = Color.argb(70, 228, 188, 90),
    )

    // The app's own light palette, so a card can sit on a white page the way
    // the reader does.
    private val IVORY_PALETTE = Palette(
        ground = "#E7E4D6".toColorInt(),
        card = "#EFEDE2".toColorInt(),
        line = "#D5D1BF".toColorInt(),
        gold = "#7E5F1E".toColorInt(),
        ink = "#1D2B24".toColorInt(),
        muted = "#5C6A5F".toColorInt(),
        glow = Color.argb(46, 126, 95, 30),
    )

    // Near-black with the gold kept bright: a card posted to a status at night
    // sitting on a screen that is already dark.
    private val NIGHT = Palette(
        ground = "#07100D".toColorInt(),
        card = "#0C1714".toColorInt(),
        line = "#1C2E27".toColorInt(),
        gold = "#E4BC5A".toColorInt(),
        ink = "#ECE7DA".toColorInt(),
        muted = "#8A9A92".toColorInt(),
        glow = Color.argb(54, 228, 188, 90),
    )

    private fun palette(ground: ImageGround): Palette = when (ground) {
        ImageGround.IVORY -> IVORY_PALETTE
        ImageGround.NIGHT -> NIGHT
        ImageGround.GREEN -> GREEN
    }

    suspend fun share(
        context: Context,
        payload: SharePayload,
        strings: Strings? = null,
        spec: CardSpec = CardSpec.from(payload),
    ): Boolean = try {
        val files = withContext(Dispatchers.Default) { renderToCache(context, payload, spec) }
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
    suspend fun save(
        context: Context,
        payload: SharePayload,
        strings: Strings? = null,
        spec: CardSpec = CardSpec.from(payload),
    ): Boolean = try {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { "Gallery save requires Android 10+" }
        val files = withContext(Dispatchers.Default) { renderToCache(context, payload, spec) }
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

    private fun renderToCache(context: Context, payload: SharePayload, spec: CardSpec): List<File> {
        val dir = File(context.cacheDir, "images")
        check(dir.exists() || dir.mkdirs()) { "Could not create image cache" }
        val staleBefore = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        dir.listFiles()?.filter { it.lastModified() < staleBefore }?.forEach { it.delete() }
        val fit = fit(context, payload, spec)
        return fit.pages.mapIndexed { index, body ->
            val bitmap = render(context, payload, spec, fit, body, index + 1, fit.pages.size)
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

    /**
     * The first page as a bitmap for the editor to show, and how many pages
     * there are in all.
     *
     * The same [fit] and the same [render] the export uses, scaled down
     * afterwards rather than drawn differently — a preview that composed itself
     * would be a second renderer, and a second renderer is a promise the export
     * can quietly break.
     */
    suspend fun preview(
        context: Context,
        payload: SharePayload,
        spec: CardSpec,
        maxWidthPx: Int,
    ): Preview? = withContext(Dispatchers.Default) {
        runCatching {
            val fit = fit(context, payload, spec)
            val full = render(context, payload, spec, fit, fit.pages.first(), 1, fit.pages.size)
            val scale = (maxWidthPx.toFloat() / full.width).coerceIn(0.1f, 1f)
            val small = full.scale((full.width * scale).toInt(), (full.height * scale).toInt())
            if (small !== full) full.recycle()
            Preview(small, fit.pages.size, fit.bodySize)
        }.onFailure { Log.e(TAG, "preview failed", it) }.getOrNull()
    }

    /** What the editor needs to draw itself: the picture, and what it cost. */
    data class Preview(val bitmap: Bitmap, val pages: Int, val bodySize: Float)

    private fun ethiopic(context: Context): Typeface =
        ResourcesCompat.getFont(context, R.font.abyssinica_sil) ?: Typeface.SERIF

    private fun textWidth(pad: Float): Int = (W - 2 * EDGE - 2 * pad).toInt()

    private fun alignmentOf(align: CardAlign): Layout.Alignment =
        if (align == CardAlign.CENTER) Layout.Alignment.ALIGN_CENTER
        else Layout.Alignment.ALIGN_NORMAL

    private fun bodyLayout(
        text: String,
        paint: TextPaint,
        spec: CardSpec,
        maxLines: Int = Int.MAX_VALUE,
    ): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, textWidth(spec.margin.pad))
            .setLineSpacing(0f, spec.spacing)
            .setAlignment(alignmentOf(spec.align))
            .setMaxLines(maxLines)
            .apply {
                // Inter-word justification arrived in Oreo and only makes sense
                // on a column wide enough not to open rivers between words.
                if (spec.align == CardAlign.JUSTIFY &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ) {
                    setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD)
                }
            }
            .build()

    /**
     * Decide the body size and the pages together, against the one height
     * budget the renderer will actually draw into.
     *
     * Three steps, in this order: start from what the length wants, shrink by
     * [BODY_STEP] while the whole passage is still too tall, and stop at
     * [BODY_MIN]. Whatever is over at the floor becomes another page — never a
     * smaller font and never an ellipsis.
     */
    private fun fit(context: Context, payload: SharePayload, spec: CardSpec): Fit {
        val face = ethiopic(context)
        val k = chromeScale(spec.shape)
        val body = payload.body.trim()

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = TITLE_SIZE * k
            typeface = Typeface.create(face, Typeface.BOLD)
        }
        val titleHeight = payload.title?.takeIf { it.isNotBlank() }?.let {
            StaticLayout.Builder.obtain(it, 0, it.length, titlePaint, textWidth(spec.margin.pad))
                .setLineSpacing(0f, 1.2f).setMaxLines(4).build().height
        } ?: 0

        val brandBlock = (BRAND_BASELINE + BRAND_RULE_GAP) * k
        val kickerBlock = (if (payload.kicker != null) KICKER_GAP else NO_KICKER_GAP) * k
        val titleBlock = if (titleHeight > 0) titleHeight + TITLE_GAP * k else 0f
        val ruleBlock = (BODY_RULE_GAP + BODY_RULE_DROP) * k
        val footerBlock = if (spec.showColophon) FOOTER_BLOCK * k else NO_KICKER_GAP * k
        val fixed = EDGE + brandBlock + kickerBlock + titleBlock + ruleBlock + footerBlock + EDGE

        val frame = when (spec.shape) {
            ImageShape.SQUARE -> W
            ImageShape.STORY -> MAX_H
            ImageShape.CARD -> CARD_MAX_H
        }
        val available = (frame - fixed).coerceAtLeast(MIN_BODY_BAND)

        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = face }
        // An override is the reader's decision and is obeyed exactly; without
        // one the length proposes a size and the frame shrinks it until it fits.
        var size = spec.sizeOverride ?: sizeForLength(body.length)
        if (spec.sizeOverride == null) {
            while (size > BODY_MIN) {
                paint.textSize = size
                if (bodyLayout(body, paint, spec).height <= available) break
                size -= BODY_STEP
            }
        }
        size = size.coerceIn(BODY_MIN, BODY_MAX)
        paint.textSize = size

        return Fit(
            bodySize = size,
            pages = paginate(body, paint, spec, available),
            fixed = fixed,
            frame = frame,
            chrome = k,
        )
    }

    /**
     * Cut [body] into pages that each fit [available] at the paint's own size.
     *
     * Every page is measured against the same budget the renderer draws into,
     * so a page can never arrive at the canvas too tall to be drawn. The guard
     * on `end` is for the pathological case of a single line taller than the
     * whole band: without it the loop would take zero characters and spin.
     */
    private fun paginate(
        body: String,
        paint: TextPaint,
        spec: CardSpec,
        available: Float,
    ): List<String> {
        if (body.isEmpty()) return listOf("")
        val lineHeight = paint.fontSpacing * spec.spacing
        val perPage = (available / lineHeight).toInt().coerceAtLeast(1)
        val pages = mutableListOf<String>()
        var remaining = body
        while (remaining.isNotEmpty()) {
            val full = bodyLayout(remaining, paint, spec)
            if (full.lineCount <= perPage) {
                pages += remaining
                break
            }
            val end = full.getLineEnd(perPage - 1).coerceAtLeast(1)
            pages += remaining.substring(0, end).trimEnd()
            remaining = remaining.substring(end).trimStart()
        }
        return pages.ifEmpty { listOf("") }
    }

    private fun render(
        context: Context,
        payload: SharePayload,
        spec: CardSpec,
        fit: Fit,
        body: String,
        page: Int,
        pageCount: Int,
    ): Bitmap {
        val face = ethiopic(context)
        val k = fit.chrome
        fun paint(size: Float, color: Int, bold: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = if (bold) Typeface.create(face, Typeface.BOLD) else face
        }

        val pad = spec.margin.pad
        val width = textWidth(pad)
        fun layout(text: String, p: TextPaint, spacing: Float, maxLines: Int = Int.MAX_VALUE): StaticLayout =
            StaticLayout.Builder.obtain(text, 0, text.length, p, width)
                .setLineSpacing(0f, spacing)
                .setAlignment(alignmentOf(spec.align))
                .setMaxLines(maxLines)
                .setEllipsize(if (maxLines == Int.MAX_VALUE) null else TextUtils.TruncateAt.END)
                .build()

        val pal = palette(spec.ground)
        val brandPaint = paint(BRAND_SIZE * k, pal.gold, bold = true)
        val kickerPaint = paint(KICKER_SIZE * k, pal.gold)
        val titlePaint = paint(TITLE_SIZE * k, pal.ink, bold = true)
        val bodyPaint = paint(fit.bodySize, pal.ink)

        val titleLayout = payload.title
            ?.takeIf { it.isNotBlank() }
            ?.let { layout(it, titlePaint, 1.2f, maxLines = 4) }

        // No maxLines and no ellipsize: fit() already cut this page to something
        // that fits the band, so anything the renderer had to trim here would be
        // text quietly thrown away — which is the whole bug this replaced.
        val bodyLayout = bodyLayout(body, bodyPaint, spec)

        val h = when (spec.shape) {
            ImageShape.CARD -> (fit.fixed + bodyLayout.height).toInt().coerceIn(640, CARD_MAX_H)
            else -> fit.frame
        }
        // Where the block sits in whatever space the frame has left over.
        val spare = (h - fit.fixed - bodyLayout.height).coerceAtLeast(0f)
        val slack = when (spec.position) {
            BlockPosition.TOP -> 0f
            BlockPosition.CENTER -> spare / 2f
            BlockPosition.BOTTOM -> spare
        }
        val bmp = createBitmap(W, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        // Ground + soft gold glow in the top corner, then the card.
        c.drawColor(pal.ground)
        c.drawRect(
            0f, 0f, W.toFloat(), h.toFloat(),
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    W * 0.86f, h * 0.08f, W * 0.55f,
                    pal.glow, Color.TRANSPARENT, Shader.TileMode.CLAMP,
                )
            },
        )
        val card = RectF(EDGE, EDGE, W - EDGE, h - EDGE)
        c.drawRoundRect(card, 48f, 48f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pal.card })
        c.drawRoundRect(
            card, 48f, 48f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = pal.line; style = Paint.Style.STROKE; strokeWidth = 3f
            },
        )

        val left = card.left + pad
        val right = card.right - pad
        var y = card.top + BRAND_BASELINE * k + slack

        // Brand row: ስንቅ left, the date right, then a hairline rule.
        c.drawText("ስንቅ", left, y, brandPaint)
        payload.dateLabel?.takeIf { spec.showDate }?.let {
            val p = paint(DATE_SIZE * k, pal.muted)
            val available = (right - left - brandPaint.measureText("ስንቅ") - 40f).coerceAtLeast(120f)
            val line = TextUtils.ellipsize(it, p, available, TextUtils.TruncateAt.END).toString()
            c.drawText(line, right - p.measureText(line), y, p)
        }
        y += BRAND_RULE_GAP * k
        c.drawRect(left, y, right, y + 2f, Paint().apply { color = pal.line })

        payload.kicker?.let {
            y += KICKER_GAP * k
            val line = TextUtils.ellipsize(it, kickerPaint, right - left, TextUtils.TruncateAt.END).toString()
            c.drawText(line, left, y, kickerPaint)
        } ?: run { y += NO_KICKER_GAP * k }

        titleLayout?.let {
            y += TITLE_GAP * k
            c.withTranslation(left, y) { it.draw(this) }
            y += it.height.toFloat()
        }

        // A short gold rule between the heading block and the passage.
        y += BODY_RULE_GAP * k
        c.drawRect(left, y, left + 56f, y + 3f, Paint().apply { color = pal.gold })
        y += BODY_RULE_DROP * k

        c.withTranslation(left, y) { bodyLayout.draw(this) }

        // Footnote: the app's name in Ge'ez script, centred at the foot of the
        // card like a colophon, so every shared passage says where it came from.
        // A page count is drawn even with the colophon off — "2 / 3" is not
        // decoration, it is the only thing saying the passage continues.
        if (spec.showColophon || pageCount > 1) {
            val sigPaint = paint(SIG_SIZE * k, pal.gold)
            val sig = when {
                spec.showColophon && pageCount > 1 -> "— ስንቅ —  $page/$pageCount"
                spec.showColophon -> "— ስንቅ —"
                else -> "$page/$pageCount"
            }
            c.drawText(sig, (W - sigPaint.measureText(sig)) / 2f, card.bottom - SIG_BASELINE * k, sigPaint)
        }

        return bmp
    }

    private inline fun Canvas.withTranslation(x: Float, y: Float, block: Canvas.() -> Unit) {
        withSave {
            translate(x, y)
            block()
        }
    }
}
