package com.agpeya.app.ui.gitsawe

/**
 * The fixed ጸሎት (closing salutation) said after reading ANY day's ስንክሳር — it is
 * not part of the daily data, so it lives here and is appended once at the end
 * of every day. Rendered as its own separated section.
 *
 * Stanzas 1–3 are web-verified against the Atrons Media synaxarium text;
 * stanzas 4–5 and the coda were supplied and corrected by the user. The holy
 * names in [CLOSING_HOLY_NAMES] are drawn in red wherever they appear in the
 * stanzas below.
 */

/**
 * One stanza of the closing salutation: the [geez] salam verse and, where one
 * exists, its [amharic] rendering. Stanzas 4–5 were supplied directly and have
 * no Amharic counterpart, so they stay in Ge'ez in either language.
 */
data class SynaxariumClosingStanza(val geez: String, val amharic: String? = null)

/** Each stanza is one salam verse (lines joined by ።, closed by ።). */
val SYNAXARIUM_CLOSING_STANZAS: List<SynaxariumClosingStanza> = listOf(
    // Stanzas 1–3: web-verified against the Atrons Media synaxarium text; the
    // Amharic is the rendering carried alongside them in the source.
    SynaxariumClosingStanza(
        "ዘአቅረብኩ ማኅሌተ አዘኪርየ አእላፈ። እምእለ ተጸምዱከ ዘልፈ። ለለአነብብ ተወከፍ ዘንዴትየ መጽሐፈ። " +
            "እንተ ረሰይከ እግዚኦ ጸሪቀ መበለት ውኩፈ። እምእለ አብኡ ብዑላን ዘተርፈ።",
        "ጌታ ሆይ ከብልጽግናቸው ብዛት የተነሳ ብዙ መባ ካገቡት ይልቅ የደሀዪቱን አነስተኛ መባ እንደተቀበልህ " +
            "ለሁል ጊዜ የሚያገለግሉህ አእላፍ መላእክትን እያሳሰብሁ በችግሬ ጊዜ የማቀርብልህን ምስጋና ተቀበል።",
    ),
    SynaxariumClosingStanza(
        "ነቢያት ቅዱሳን ወሐዋርያት ሰባኪያን። ሰማዕት ወጻድቃን ወመላእክት ትጉሃን። ደናግል ዓዲ ወመነኰሳት ኄራን። " +
            "ባርኩ ባርኮ ጉባኤ ዛቲ መካን። እስከ አረጋዊ ልሒቅ ወንኡስ ሕፃን።",
        "እናንተም ቅዱሳን ነቢያት የወንጌል ሰባኪያን ሐዋርያት ሰማዕታትና ጻድቃን ትጉሃን መላእክት ፍጹማን ደናግል " +
            "እንዲሁም ደጋጎች መነኰሳት ሆይ ልጅ አዋቂ ሳይለይ የምንሰበሰብበትን የጉባኤ ቦታ ባርኩ።",
    ),
    SynaxariumClosingStanza(
        "ለዘጸሐፎ በክርታስ ወለዘአጽሐፎ እንዘ ይደርስ። ለዘአንበቦ ወለዘተርጐሞ በልሳን ሐዲስ። ወለዘሰምዐ ቃሎ በዕዝነ መንፈስ። " +
            "በጸሎተ እሙ ማርያም ዐራቂተ ኵሉ እምባእስ። ኅቡረ ይምሐረነ ኢየሱስ ክርስቶስ።",
        "ይህን መጽሐፍ ወረቀት አዘጋጅቶ ብራና ደምጦ የጻፈውና ያጻፈውም ከግዕዝ ወደ አማርኛ የተረጐመውን ቃለ ንባቡን " +
            "በእርጋታ መንፈስ እና በተመስጦ ሕሊና ያዳመጠ በደለኛን ሁሉ የምታስምር የጌታ እናቱ በማርያም ጸሎት " +
            "ኢየሱስ ክርስቶስ በአንድነት ይማረን።",
    ),
    // Stanzas 4–5 + coda: corrected text supplied by the user.
    SynaxariumClosingStanza(
        "ሰላም ለክሙ ጻድቃን ወሰማዕት፤ እለ አዕረፍክሙ በዛቲ ዕለት፤ መዋዕልየ ዓለም እንትሙ በብዙኅ ትዕግሥት፤ " +
            "ስአሉ ቅድመ ፈጣሪ በኵሉ ሰዓት፤ እንበለ ንስሐ ኪያነ ኢይነሥአነ ሞት።",
    ),
    SynaxariumClosingStanza(
        "ሰላም ለክሙ ጻድቃነ ዛቲ ዕለት ኵልክሙ፤ እድ ወአንስት በበአስማቲክሙ፤ ቅዱሳነ ሰማይ ወምድር ማኅበረ ሥላሴ አንትሙ፤ " +
            "ትዝክሩነ በጸሎትክሙ በእንተ ማርያም እሙ፤ ተማኅፀነ ለክርስቶስ በሥጋሁ ወደሙ።",
    ),
)

/** The closing rubric, on its own line. */
const val SYNAXARIUM_CLOSING_CODA = "አቡነ ዘበሰማያት በል።"

/** Names drawn in red wherever they occur (order matters: longest first). */
val CLOSING_HOLY_NAMES: List<String> = listOf("ኢየሱስ ክርስቶስ", "ክርስቶስ", "ማርያም")
