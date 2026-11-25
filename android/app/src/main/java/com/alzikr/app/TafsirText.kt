package com.alzikr.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Tafsir text, easier to read: Quran quoted inside ﴿…﴾ in the theme colour, references such as [القمر: 34] in gold,
 * and footnotes ([[…]] in the source) folded into a small coloured number; tapping it shows the note.
 */
object TafsirFormat {
    // Every bracket escaped: Android's regex engine (ICU) refuses a bare "}" or "]" that the desktop JVM accepts.
    private val token = Regex("""\[\[(.+?)\]\]|﴿[^﴾]*﴾|\{[^\{\}\[\]\n]{1,800}\}|\([^\(\)\[\]\n]{6,500}\)|\[[^\[\]\n]{2,40}?:\s*[٠-٩0-9\-–، ]+\]""", RegexOption.DOT_MATCHES_ALL)
    /** Quran quoted in ( … ) by some books: told apart from ordinary brackets by its vowel marks. */
    private fun voweled(t: String): Boolean {
        val letters = t.count { it in 'ء'..'ي' }; val marks = t.count { it in 'ً'..'ْ' }
        return letters >= 4 && marks * 10 >= letters * 6
    }
    private fun num(n: Int) = Quran.digits(n)

    /** [onNote] receives the footnote text of the tapped number. */
    fun build(raw: String, quran: Color, ref: Color, note: Color, onNote: (String) -> Unit): AnnotatedString = buildAnnotatedString {
        var at = 0; var n = 0
        for (m in token.findAll(raw)) {
            append(raw.substring(at, m.range.first))
            val t = m.value
            when {
                t.startsWith("[[") -> {
                    n++
                    val body = m.groupValues[1].trim()
                    withLink(LinkAnnotation.Clickable("note$n", TextLinkStyles(SpanStyle(color = note, fontWeight = FontWeight.SemiBold, fontSize = 13.sp))) { onNote(body) }) {
                        append("(${num(n)})")
                    }
                }
                t.startsWith("﴿") -> withStyle(SpanStyle(color = quran, fontWeight = FontWeight.SemiBold)) { append(t) }
                // { … }: the Quran in السعدي and others (shown with ﴿ ﴾); only numbers inside = a verse range.
                t.startsWith("{") -> if (t.none { it.isLetter() }) withStyle(SpanStyle(color = ref)) { append(t) }
                    else withStyle(SpanStyle(color = quran, fontWeight = FontWeight.SemiBold)) { append("﴿" + t.substring(1, t.length - 1).trim() + "﴾") }
                t.startsWith("(") -> if (voweled(t)) withStyle(SpanStyle(color = quran, fontWeight = FontWeight.SemiBold)) { append(t) } else append(t)
                else -> withStyle(SpanStyle(color = ref)) { append(t) }
            }
            at = m.range.last + 1
        }
        append(raw.substring(at))
    }
}

@Composable
fun TafsirText(raw: String, fontSize: TextUnit, lineHeight: TextUnit, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE) {
    var note by remember { mutableStateOf<String?>(null) }
    val q = P.accent; val g = P.gold
    val east = Quran.easternDigits
    val text = remember(raw, q, g, east) { TafsirFormat.build(Quran.numerals(raw), q, g, Color(0xFF4A90D9)) { note = it } }
    Text(text, color = P.ink, fontSize = fontSize, lineHeight = lineHeight, fontFamily = Fonts.naskh, modifier = modifier, maxLines = maxLines)
    note?.let { n ->
        AlertDialog(onDismissRequest = { note = null }, containerColor = P.surface,
            title = { GText("حاشية", color = P.gold, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) },
            text = { Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Text(TafsirFormat.build(n, P.accent, P.gold, P.accent) {}, color = P.ink, fontSize = 15.sp, lineHeight = 26.sp, fontFamily = Fonts.naskh)
            } },
            confirmButton = { TextButton({ note = null }) { GText("إغلاق", color = P.accent) } })
    }
}
