package com.agpeya.app.ui.reading

private val ONES = listOf("", "፩", "፪", "፫", "፬", "፭", "፮", "፯", "፰", "፱")
private val TENS = listOf("", "፲", "፳", "፴", "፵", "፶", "፷", "፸", "፹", "፺")

/** Ge'ez numerals: verse numbers, years, and amete alem (፻ hundreds, ፼ ten-thousands). */
fun geezNumeral(n: Int): String = when {
    n >= 10_000 -> group(n / 10_000, n % 10_000, "፼")
    n >= 100 -> group(n / 100, n % 100, "፻")
    else -> TENS[n / 10] + ONES[n % 10]
}

private fun group(count: Int, rest: Int, mark: String): String =
    (if (count == 1) "" else geezNumeral(count)) + mark + (if (rest > 0) geezNumeral(rest) else "")
