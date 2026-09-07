package com.agpeya.app.ui.reading

private val ONES = listOf("", "፩", "፪", "፫", "፬", "፭", "፮", "፯", "፰", "፱")
private val TENS = listOf("", "፲", "፳", "፴", "፵", "፶", "፷", "፸", "፹", "፺")

/** Ge'ez numerals: verse numbers, years, and amete alem (፻ hundreds, ፼ ten-thousands). */
fun geezNumeral(n: Int): String = when {
    // The system has no negative numbers (0 renders empty, as ever); a negative
    // from bad data falls back to digits instead of indexing out of bounds.
    n < 0 -> n.toString()
    n >= 10_000 -> group(n / 10_000, n % 10_000, "፼")
    n >= 100 -> group(n / 100, n % 100, "፻")
    else -> TENS[n / 10] + ONES[n % 10]
}

private fun group(count: Int, rest: Int, mark: String): String =
    (if (count == 1) "" else geezNumeral(count)) + mark + (if (rest > 0) geezNumeral(rest) else "")

/**
 * A Ge'ez numeral read back into an Int, or null when the text is not one.
 *
 * The inverse of [geezNumeral], for the places a person types what the app
 * printed: searching "መዝሙር ፶" should find psalm 50, and it could not, because
 * the reference parser only ever knew Arabic digits.
 *
 * The system is additive within a group and multiplicative across ፻ and ፼:
 * ፻ is a hundred, ፪፻ two hundred, ፻፳፫ a hundred and twenty-three. A bare mark
 * with nothing before it counts as one of itself, which is how the script
 * writes ፻ rather than ፩፻.
 */
fun parseGeezNumeral(text: String): Int? {
    val t = text.trim()
    if (t.isEmpty() || t.any { it !in GEEZ_DIGITS }) return null
    // Split on ፼ first: everything left of it is counted in ten-thousands.
    t.indexOf('፼').takeIf { it >= 0 }?.let { at ->
        val high = t.substring(0, at).let { if (it.isEmpty()) 1 else parseGeezNumeral(it) ?: return null }
        val low = t.substring(at + 1).let { if (it.isEmpty()) 0 else parseGeezNumeral(it) ?: return null }
        return high * 10_000 + low
    }
    t.indexOf('፻').takeIf { it >= 0 }?.let { at ->
        val high = t.substring(0, at).let { if (it.isEmpty()) 1 else parseGeezNumeral(it) ?: return null }
        val low = t.substring(at + 1).let { if (it.isEmpty()) 0 else parseGeezNumeral(it) ?: return null }
        return high * 100 + low
    }
    var total = 0
    for (ch in t) {
        val tens = TENS.indexOf(ch.toString())
        val ones = ONES.indexOf(ch.toString())
        total += when {
            tens > 0 -> tens * 10
            ones > 0 -> ones
            else -> return null
        }
    }
    return total.takeIf { it > 0 }
}

private val GEEZ_DIGITS: Set<Char> =
    (ONES.drop(1) + TENS.drop(1) + listOf("፻", "፼")).map { it.first() }.toSet()
