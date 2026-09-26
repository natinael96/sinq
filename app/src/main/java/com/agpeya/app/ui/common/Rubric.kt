package com.agpeya.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.agpeya.app.ui.books.Rubrication
import com.agpeya.app.ui.theme.sinqColors

/**
 * The rubric red, applied to a line of prayer.
 *
 * The rule itself is [Rubrication] and is deliberately Compose-free so it can
 * be tested; this is the one place that turns its ranges into spans, so every
 * reader in the app reddens the same words the same way. Before this existed
 * the rule reached the books reader only, and the ስንክሳር's closing prayer
 * carried a second, shorter name list of its own.
 *
 * Pick the [Rubrication.Scope] from what is being read, not from the screen:
 * GENERAL everywhere the text is prose or psalmody, and MELKIE / SINKSAR /
 * MAHLET only where the whole piece is about a saint.
 */
@Composable
fun rubricated(text: String, scope: Rubrication.Scope): AnnotatedString {
    val red = sinqColors.arke
    return remember(text, scope, red) {
        buildAnnotatedString { appendRubricated(text, red, scope) }
    }
}

/**
 * Append [text] with its red spans, for a line that carries other styling too
 * — the ስንክሳር's inline Ge'ez numeral, a highlight, a citation.
 *
 * Ranges are offset by whatever has already been appended, so this composes
 * with anything built before it.
 */
fun AnnotatedString.Builder.appendRubricated(
    text: String,
    red: Color,
    scope: Rubrication.Scope,
) {
    val start = length
    append(text)
    Rubrication.redRanges(text, scope).forEach {
        addStyle(SpanStyle(color = red), start + it.first, start + it.last + 1)
    }
}
