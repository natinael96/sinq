package com.agpeya.app.ui.gitsawe

/**
 * The fixed ጸሎት (closing salutation) said after reading ANY day's ስንክሳር — it is
 * not part of the daily data, so it lives here and is appended once at the end
 * of every day. Rendered as its own separated section.
 *
 * Stanzas 1–3 are web-verified against the Atrons Media synaxarium text;
 * stanzas 4–5 and the coda are still transcribed from the image and NOT yet
 * verified. The holy names in [CLOSING_HOLY_NAMES] are drawn in red wherever
 * they appear in the stanzas below.
 */

/** Each stanza is one salam verse (lines joined by ።, closed by ።). */
val SYNAXARIUM_CLOSING_STANZAS: List<String> = listOf(
    // Stanzas 1–3: web-verified against the Atrons Media synaxarium text.
    "ዘአቅረብኩ ማኅሌተ አዘኪርየ አእላፈ። እምእለ ተጸምዱከ ዘልፈ። ለለአነብብ ተወከፍ ዘንዴትየ መጽሐፈ። " +
        "እንተ ረሰይከ እግዚኦ ጸሪቀ መበለት ውኩፈ። እምእለ አብኡ ብዑላን ዘተርፈ።",
    "ነቢያት ቅዱሳን ወሐዋርያት ሰባኪያን። ሰማዕት ወጻድቃን ወመላእክት ትጉሃን። ደናግል ዓዲ ወመነኰሳት ኄራን። " +
        "ባርኩ ባርኮ ጉባኤ ዛቲ መካን። እስከ አረጋዊ ልሒቅ ወንኡስ ሕፃን።",
    "ለዘጸሐፎ በክርታስ ወለዘአጽሐፎ እንዘ ይደርስ። ለዘአንበቦ ወለዘተርጐሞ በልሳን ሐዲስ። ወለዘሰምዐ ቃሎ በዕዝነ መንፈስ። " +
        "በጸሎተ እሙ ማርያም ዐራቂተ ኵሉ እምባእስ። ኅቡረ ይምሐረነ ኢየሱስ ክርስቶስ።",
    // Stanzas 4–5 + coda: still from the image — NOT yet web-verified.
    "ሰላም ለክሙ ጸድቃን ወሰማዕት፤ እለ አዕረፍክሙ በዛቲ ዕለት፤ መዋዕየን ዓለም አንትሙ በበዙኀን ትዕግሥት፤ " +
        "ሰአሉ ቅድመ ፈጣሪ በኵሉ ሰዓት፤ እንበለ ንስሓ ኪያነ ኢይንሣእ ሞት።",
    "ሰላም ለክሙ ጸድቃን ወሰማዕት ዛቲ ዕለት ኵልክሙ፤ እድ ወአንስት በበአስማቲክሙ፤ ቅዱሳን ሰማይ ወምድር ማኅበረ ሥላሴ አንትሙ፤ " +
        "ትዘክሩነ በጸሎትክሙ በእንተ ማርያም እሙ፤ ተማኅፅን ለክርስቶስ በሥጋሁ ወደሙ።",
)

/** The closing rubric, on its own line. */
const val SYNAXARIUM_CLOSING_CODA = "አቡነ ዘበሰማያት በል።"

/** Names drawn in red wherever they occur (order matters: longest first). */
val CLOSING_HOLY_NAMES: List<String> = listOf("ኢየሱስ ክርስቶስ", "ክርስቶስ", "ማርያም")
