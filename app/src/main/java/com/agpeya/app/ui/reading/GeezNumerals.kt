package com.agpeya.app.ui.reading

private val ONES = listOf("", "፩", "፪", "፫", "፬", "፭", "፮", "፯", "፰", "፱")
private val TENS = listOf("", "፲", "፳", "፴", "፵", "፶", "፷", "፸", "፹", "፺")

/** Ge'ez numerals for 1..199 — enough for any verse number. */
fun geezNumeral(n: Int): String = when {
    n >= 100 -> "፻" + if (n > 100) geezNumeral(n - 100) else ""
    else -> TENS[n / 10] + ONES[n % 10]
}
