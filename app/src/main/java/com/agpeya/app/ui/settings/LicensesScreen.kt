package com.agpeya.app.ui.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.strings.LocalStrings

/**
 * In-app attribution and licence notices for everything the app bundles.
 *
 * Like AboutScreen, the body stays English regardless of the app language:
 * these are legal notices, and they should read exactly as worded — a
 * translation could drift from the licence terms it is meant to state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(title = LocalStrings.current.licensesTitle, onBack = onBack)
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        ) {
            item {
                LicSection("Scripture text")
                LicPara(
                    "All bundled scripture — the Bible books readable in the Library, the Psalter " +
                        "in Amharic and Ge'ez, and the psalms and gospels arranged into the hours " +
                        "of prayer — is drawn from 80-weahadu, the open-source Ethiopian Orthodox " +
                        "Tewahedo Bible published by the EOTCOpenSource community " +
                        "(github.com/EOTCOpenSource/80-weahadu), used under the Creative Commons " +
                        "Attribution-NonCommercial-NoDerivatives 4.0 International licence " +
                        "(creativecommons.org/licenses/by-nc-nd/4.0).",
                )
                LicPara(
                    "The verse text is reproduced unchanged, with two disclosed exceptions: the " +
                        "acrostic letters of Psalm 118, which the source encodes at the end of " +
                        "stanza-final verses, are shown as stanza headings; and psalm chapter " +
                        "labels follow the Ge'ez (LXX) numbering used by the Ethiopian tradition. " +
                        "Provided as-is, without warranties. A copy of the licence is bundled with " +
                        "the app at content/bible/LICENSE.",
                )
            }
            item {
                LicSection("Gitsawe (ግጻዌ)")
                LicPara(
                    "The ግጻዌ lectionary — the fixed 366-day cycle together with the movable, " +
                        "Sunday, Athanasius, and Bahre Hasab collections — was scanned and " +
                        "transcribed from the printed ግጻዌ by the Sinq maintainer, and is released " +
                        "as open content under the Creative Commons " +
                        "Attribution-NonCommercial-NoDerivatives 4.0 International licence, the " +
                        "same terms as the bundled scripture. The underlying lectionary is " +
                        "traditional Ethiopian Orthodox liturgical material. Scripture citations " +
                        "inside the readings open the bundled scripture credited above.",
                )
            }
            item {
                LicSection("Synaxarium (ስንክሳር)")
                LicPara(
                    "The Amharic ስንክሳር is drawn from two sources: the gitsaweandsinksarbot " +
                        "project by hailemariam-eyayu " +
                        "(github.com/hailemariam-eyayu/gitsaweandsinksarbot), and the " +
                        "Nexuss0781/synaxarium dataset on the Hugging Face Hub " +
                        "(huggingface.co/datasets/Nexuss0781/synaxarium), which is published " +
                        "under the MIT License. The underlying commemorations are traditional " +
                        "Ethiopian Orthodox liturgical content.",
                )
                LicPara(
                    "The MIT License requires that its permission notice accompany copies:",
                )
                LicenseBlock(MIT_LICENSE_TEXT)
            }
            item {
                LicSection("Wudase Maryam (ውዳሴ ማርያም)")
                LicPara(
                    "ውዳሴ ማርያም and ጸሎት ዘዘወትር are bundled from the digitisation at " +
                        "github.com/tecleet/wudase-mariam. The underlying prayer is centuries-old, " +
                        "traditional Ethiopian Orthodox liturgical text; this particular " +
                        "digitisation is credited to that repository.",
                )
            }
            item {
                LicSection("Fonts")
                LicPara(
                    "All bundled fonts are used under the SIL Open Font License, Version 1.1 " +
                        "(openfontlicense.org):",
                )
                LicPara(
                    "• Abyssinica SIL — Copyright (c) SIL Global, with Reserved Font Names " +
                        "“Abyssinica” and “SIL”; Modern Gurage glyphs " +
                        "Copyright (c) The Ge'ez Frontier Foundation\n" +
                        "• Noto Sans Ethiopic — Copyright (c) Google\n" +
                        "• Ethiopic Abay Light — abass alamnehe, via the Font.et open font library\n" +
                        "• Bela Bereka — Abel Daniel, via the Font.et open font library\n" +
                        "• Zemenay — Abel Yeshewalem, via the Font.et open font library (font.et)",
                )
            }
            item {
                LicSection("App code")
                LicPara(
                    "The Sinq application source code is licensed under the Apache License, " +
                        "Version 2.0 (see the LICENSE file in the source repository). The content " +
                        "licences above do not inherit from it: forking the source code does not " +
                        "grant Apache-2.0 rights over the bundled scripture, lectionary, " +
                        "synaxarium, or prayers.",
                )
            }
            item {
                LicSection("SIL Open Font License 1.1 — full text")
                LicenseBlock(OFL_1_1_TEXT)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun LicSection(title: String) {
    Spacer(Modifier.height(14.dp))
    Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun LicPara(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
    Spacer(Modifier.height(6.dp))
}

/** Verbatim licence text: small, monospace, never translated or reflowed by style. */
@Composable
private fun LicenseBlock(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            lineHeight = 15.sp,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(6.dp))
}

/** The standard MIT permission notice, as required to accompany copies. */
private val MIT_LICENSE_TEXT = """
MIT License

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
""".trimIndent()

/**
 * The generic portion of the SIL Open Font License 1.1, verbatim from
 * docs/AbyssinicaSIL-OFL.txt (the per-font copyright lines appear in the
 * font list above).
 */
private val OFL_1_1_TEXT = """
SIL OPEN FONT LICENSE Version 1.1 - 26 February 2007

PREAMBLE
The goals of the Open Font License (OFL) are to stimulate worldwide
development of collaborative font projects, to support the font creation
efforts of academic and linguistic communities, and to provide a free and
open framework in which fonts may be shared and improved in partnership
with others.

The OFL allows the licensed fonts to be used, studied, modified and
redistributed freely as long as they are not sold by themselves. The
fonts, including any derivative works, can be bundled, embedded,
redistributed and/or sold with any software provided that any reserved
names are not used by derivative works. The fonts and derivatives,
however, cannot be released under any other type of license. The
requirement for fonts to remain under this license does not apply
to any document created using the fonts or their derivatives.

DEFINITIONS
"Font Software" refers to the set of files released by the Copyright
Holder(s) under this license and clearly marked as such. This may
include source files, build scripts and documentation.

"Reserved Font Name" refers to any names specified as such after the
copyright statement(s).

"Original Version" refers to the collection of Font Software components as
distributed by the Copyright Holder(s).

"Modified Version" refers to any derivative made by adding to, deleting,
or substituting -- in part or in whole -- any of the components of the
Original Version, by changing formats or by porting the Font Software to a
new environment.

"Author" refers to any designer, engineer, programmer, technical
writer or other person who contributed to the Font Software.

PERMISSION & CONDITIONS
Permission is hereby granted, free of charge, to any person obtaining
a copy of the Font Software, to use, study, copy, merge, embed, modify,
redistribute, and sell modified and unmodified copies of the Font
Software, subject to the following conditions:

1) Neither the Font Software nor any of its individual components,
in Original or Modified Versions, may be sold by itself.

2) Original or Modified Versions of the Font Software may be bundled,
redistributed and/or sold with any software, provided that each copy
contains the above copyright notice and this license. These can be
included either as stand-alone text files, human-readable headers or
in the appropriate machine-readable metadata fields within text or
binary files as long as those fields can be easily viewed by the user.

3) No Modified Version of the Font Software may use the Reserved Font
Name(s) unless explicit written permission is granted by the corresponding
Copyright Holder. This restriction only applies to the primary font name as
presented to the users.

4) The name(s) of the Copyright Holder(s) or the Author(s) of the Font
Software shall not be used to promote, endorse or advertise any
Modified Version, except to acknowledge the contribution(s) of the
Copyright Holder(s) and the Author(s) or with their explicit written
permission.

5) The Font Software, modified or unmodified, in part or in whole,
must be distributed entirely under this license, and must not be
distributed under any other license. The requirement for fonts to
remain under this license does not apply to any document created
using the Font Software.

TERMINATION
This license becomes null and void if any of the above conditions are
not met.

DISCLAIMER
THE FONT SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO ANY WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT
OF COPYRIGHT, PATENT, TRADEMARK, OR OTHER RIGHT. IN NO EVENT SHALL THE
COPYRIGHT HOLDER BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
INCLUDING ANY GENERAL, SPECIAL, INDIRECT, INCIDENTAL, OR CONSEQUENTIAL
DAMAGES, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF THE USE OR INABILITY TO USE THE FONT SOFTWARE OR FROM
OTHER DEALINGS IN THE FONT SOFTWARE.
""".trimIndent()
