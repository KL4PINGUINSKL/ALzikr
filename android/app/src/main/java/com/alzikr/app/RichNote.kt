package com.alzikr.app

import android.graphics.Typeface
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.Gravity
import android.widget.EditText
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Writing a note the way it will look — no marks on screen. The editor is Android's own rich text box: bold, colours,
 * sizes… are real styles on the words (and they carry on as you type after them). On saving, the styled text becomes
 * the note's plain Markdown-like text again (see [Md]), so notes stay small, can be shared and backed up.
 */
object Rich {
    // Our own span types, so the text can be read back into marks exactly.
    class Heading(val level: Int) : RelativeSizeSpan(when (level) { 1 -> 1.5f; 2 -> 1.28f; else -> 1.12f })
    class Size(val key: String) : RelativeSizeSpan(when (key) { "sm" -> .82f; "lg" -> 1.25f; else -> 1.55f })
    class Ink(val key: String, color: Int) : ForegroundColorSpan(color)
    class Mark(color: Int) : BackgroundColorSpan(color)
    class Quote(color: Int) : ForegroundColorSpan(color)

    const val BULLET = "•  "; const val BOX = "☐ "; const val TICK = "☑ "; const val QUOTE = "❝ "
    private const val FLAG = Spanned.SPAN_EXCLUSIVE_INCLUSIVE

    /** The note's text → styled text for the editor (the line marks become their signs: •, ☐, ☑, ❝). */
    fun load(md: String): SpannableStringBuilder {
        val sb = SpannableStringBuilder()
        Md.tidy(md).lines().forEachIndexed { n, raw ->
            if (n > 0) sb.append('\n')
            var line = raw; var heading = 0; var quote = false
            when {
                line.startsWith("### ") -> { heading = 3; line = line.drop(4) }
                line.startsWith("## ") -> { heading = 2; line = line.drop(3) }
                line.startsWith("# ") -> { heading = 1; line = line.drop(2) }
                line.startsWith("- [ ] ") -> { sb.append(BOX); line = line.drop(6) }
                Regex("^- \\[[xX]\\] ").containsMatchIn(line) -> { sb.append(TICK); line = line.drop(6) }
                line.startsWith("- ") -> { sb.append(BULLET); line = line.drop(2) }
                line.startsWith("> ") -> { sb.append(QUOTE); line = line.drop(2); quote = true }
            }
            val start = sb.length
            inline(sb, line)
            if (heading > 0) { sb.setSpan(Heading(heading), start, sb.length, FLAG); sb.setSpan(StyleSpan(Typeface.BOLD), start, sb.length, FLAG) }
            if (quote) sb.setSpan(Quote(0xFFB8862E.toInt()), start, sb.length, FLAG)
        }
        return sb
    }

    private val token = Regex("""\*\*(.+?)\*\*|__(.+?)__|~~(.+?)~~|==(.+?)==|\*(.+?)\*|\{c:(\w+)\}(.+?)\{/c\}|\{s:(\w+)\}(.+?)\{/s\}""")
    /** Appends one line's text with its inline marks turned into styles (nested marks work). */
    private fun inline(sb: SpannableStringBuilder, t: String) {
        var i = 0
        for (m in token.findAll(t)) {
            if (m.range.first > i) sb.append(t.substring(i, m.range.first))
            val g = m.groupValues; val s = sb.length
            when {
                g[1].isNotEmpty() -> { inline(sb, g[1]); sb.setSpan(StyleSpan(Typeface.BOLD), s, sb.length, FLAG) }
                g[2].isNotEmpty() -> { inline(sb, g[2]); sb.setSpan(UnderlineSpan(), s, sb.length, FLAG) }
                g[3].isNotEmpty() -> { inline(sb, g[3]); sb.setSpan(StrikethroughSpan(), s, sb.length, FLAG) }
                g[4].isNotEmpty() -> { inline(sb, g[4]); sb.setSpan(Mark(0x66F2C94C), s, sb.length, FLAG) }
                g[5].isNotEmpty() -> { inline(sb, g[5]); sb.setSpan(StyleSpan(Typeface.ITALIC), s, sb.length, FLAG) }
                g[7].isNotEmpty() -> { inline(sb, g[7]); Md.colors[g[6]]?.let { sb.setSpan(Ink(g[6], it.toArgb()), s, sb.length, FLAG) } }
                g[9].isNotEmpty() -> { inline(sb, g[9]); sb.setSpan(Size(g[8]), s, sb.length, FLAG) }
            }
            i = m.range.last + 1
        }
        if (i < t.length) sb.append(t.substring(i))
    }

    /** The styled text → the note's text with its marks. */
    fun save(text: Spanned): String {
        val out = StringBuilder()
        var at = 0
        text.toString().split('\n').forEachIndexed { n, full ->
            if (n > 0) out.append('\n')
            val lineStart = at; at += full.length + 1
            var line = full; var off = 0
            var prefix = ""
            when {
                line.startsWith(BOX) -> { prefix = "- [ ] "; off = BOX.length }
                line.startsWith(TICK) -> { prefix = "- [x] "; off = TICK.length }
                line.startsWith(BULLET) -> { prefix = "- "; off = BULLET.length }
                line.startsWith("• ") -> { prefix = "- "; off = 2 }
                line.startsWith(QUOTE) -> { prefix = "> "; off = QUOTE.length }
            }
            val from = lineStart + off; val to = lineStart + full.length
            if (from <= to) {
                val h = text.getSpans(from, to, Heading::class.java).firstOrNull { text.getSpanStart(it) <= from && text.getSpanEnd(it) >= to && to > from }
                if (h != null && prefix.isEmpty()) prefix = "#".repeat(h.level) + " "
                out.append(prefix)
                out.append(runs(text, from, to, heading = h != null))
            } else out.append(prefix)
            line = ""
        }
        return out.toString().trimEnd()
    }

    /** The characters of [from]..[to] with marks around each stretch of the same style. */
    private fun runs(t: Spanned, from: Int, to: Int, heading: Boolean): String {
        data class St(val b: Boolean, val i: Boolean, val u: Boolean, val s: Boolean, val m: Boolean, val c: String?, val z: String?)
        fun at(k: Int): St {
            fun <T> has(c: Class<T>, ok: (T) -> Boolean = { true }) = t.getSpans(k, k + 1, c).any { t.getSpanStart(it) <= k && t.getSpanEnd(it) > k && ok(it) }
            return St(!heading && has(StyleSpan::class.java) { it.style == Typeface.BOLD || it.style == Typeface.BOLD_ITALIC },
                has(StyleSpan::class.java) { it.style == Typeface.ITALIC || it.style == Typeface.BOLD_ITALIC },
                has(UnderlineSpan::class.java) { it !is android.text.style.SuggestionSpan }, has(StrikethroughSpan::class.java), has(Mark::class.java),
                t.getSpans(k, k + 1, Ink::class.java).lastOrNull { t.getSpanStart(it) <= k && t.getSpanEnd(it) > k }?.key,
                t.getSpans(k, k + 1, Size::class.java).lastOrNull { t.getSpanStart(it) <= k && t.getSpanEnd(it) > k }?.key)
        }
        val sb = StringBuilder()
        var k = from
        while (k < to) {
            val st = at(k); var e = k + 1
            while (e < to && at(e) == st) e++
            var piece = t.subSequence(k, e).toString()
            // Keep the spaces at the edges outside the marks («** word**» would not read back).
            val lead = piece.takeWhile { it == ' ' }; val trail = piece.drop(lead.length).takeLastWhile { it == ' ' }
            piece = piece.substring(lead.length, piece.length - trail.length)
            if (piece.isNotEmpty()) {
                if (st.z != null) piece = "{s:${st.z}}$piece{/s}"
                if (st.c != null) piece = "{c:${st.c}}$piece{/c}"
                if (st.m) piece = "==$piece=="
                if (st.s) piece = "~~$piece~~"
                if (st.u) piece = "__${piece}__"
                if (st.i) piece = "*$piece*"
                if (st.b) piece = "**$piece**"
            }
            sb.append(lead).append(piece).append(trail)
            k = e
        }
        return sb.toString()
    }

    // ---------------------------------------------------------------- the toolbar's work on the editor
    /** The selection, or the line the cursor is on (after its sign) when nothing is selected. */
    fun range(e: EditText): Pair<Int, Int> {
        val t = e.text; var a = e.selectionStart.coerceAtLeast(0); var b = e.selectionEnd.coerceAtLeast(0)
        if (a > b) { val x = a; a = b; b = x }
        if (a == b) {
            a = t.lastIndexOf('\n', (a - 1).coerceAtLeast(0)).let { if (it < 0 || e.selectionStart == 0) 0 else it + 1 }
            b = t.indexOf('\n', b).let { if (it < 0) t.length else it }
            listOf(BULLET, BOX, TICK, QUOTE).firstOrNull { t.substring(a, b).startsWith(it) }?.let { a += it.length }
        }
        return a to b
    }

    /** Adds the style, or takes it away when the whole range already has it. */
    fun <T : Any> toggle(e: EditText, cls: Class<T>, same: (T) -> Boolean, make: () -> Any) {
        val (a, b) = range(e); if (a >= b) return
        val t = e.text
        val existing = t.getSpans(a, b, cls).filter(same)
        val covered = (a until b).all { k -> existing.any { t.getSpanStart(it) <= k && t.getSpanEnd(it) > k } }
        if (covered) existing.forEach { s -> split(t, s, a, b) } else t.setSpan(make(), a, b, FLAG)
    }

    /** Takes [s] off a..b, keeping it on what lies outside. */
    private fun split(t: Editable, s: Any, a: Int, b: Int) {
        val ss = t.getSpanStart(s); val se = t.getSpanEnd(s); t.removeSpan(s)
        fun copy(): Any? = when (s) {
            is Ink -> Ink(s.key, s.foregroundColor); is Size -> Size(s.key); is Mark -> Mark(s.backgroundColor); is StyleSpan -> StyleSpan(s.style)
            is UnderlineSpan -> UnderlineSpan(); is StrikethroughSpan -> StrikethroughSpan(); else -> null
        }
        if (ss < a) copy()?.let { t.setSpan(it, ss, a, FLAG) }
        if (se > b) copy()?.let { t.setSpan(it, b, se, FLAG) }
    }

    /** A colour (or a size) replaces any other colour (size) on the range. */
    fun <T : Any> replace(e: EditText, cls: Class<T>, make: () -> Any?) {
        val (a, b) = range(e); if (a >= b) return
        val t = e.text
        t.getSpans(a, b, cls).forEach { split(t, it, a, b) }
        make()?.let { t.setSpan(it, a, b, FLAG) }
    }

    /** The lines the selection touches. */
    private fun lines(e: EditText): IntRange {
        val t = e.text.toString()
        val a = e.selectionStart.coerceAtLeast(0); val b = e.selectionEnd.coerceAtLeast(0)
        val first = t.substring(0, minOf(a, b)).count { it == '\n' }; val last = t.substring(0, maxOf(a, b)).count { it == '\n' }
        return first..last
    }
    private fun lineStart(t: CharSequence, n: Int): Int { var k = 0; var line = 0; while (line < n) { k = t.indexOf('\n', k).let { if (it < 0) t.length else it + 1 }; line++ }; return k }
    private fun CharSequence.indexOf(c: Char, from: Int): Int { for (i in from until length) if (this[i] == c) return i; return -1 }

    /** A sign at the start of the selected lines (•, ☐, ❝); pressed again it goes. */
    fun sign(e: EditText, sign: String) {
        val t = e.text
        val ls = lines(e).map { lineStart(t, it) }
        val all = ls.all { t.subSequence(it, minOf(t.length, it + sign.length)).toString() == sign }
        ls.reversed().forEach { s ->
            val cur = listOf(BULLET, BOX, TICK, QUOTE).firstOrNull { t.subSequence(s, minOf(t.length, s + it.length)).toString() == it }
            if (cur != null) t.delete(s, s + cur.length)
            if (!all) t.insert(s, sign)
        }
    }

    /** Heading level 1..3 on the selected lines (pressed again: back to plain text). */
    fun heading(e: EditText, level: Int) {
        val t = e.text
        lines(e).forEach { n ->
            val s = lineStart(t, n); val end = t.indexOf('\n', s).let { if (it < 0) t.length else it }
            if (end <= s) return@forEach
            val had = t.getSpans(s, end, Heading::class.java)
            val same = had.any { it.level == level }
            had.forEach { t.removeSpan(it) }
            t.getSpans(s, end, StyleSpan::class.java).filter { it.style == Typeface.BOLD && t.getSpanStart(it) <= s && t.getSpanEnd(it) >= end }.forEach { t.removeSpan(it) }
            if (!same) { t.setSpan(Heading(level), s, end, FLAG); t.setSpan(StyleSpan(Typeface.BOLD), s, end, FLAG) }
        }
    }

    /** A tap on ☐ / ☑ at the start of a line ticks it. */
    fun tickAt(e: EditText, offset: Int): Boolean {
        val t = e.text
        val s = t.lastIndexOf('\n', (offset - 1).coerceAtLeast(0)).let { if (it < 0 || offset == 0) 0 else it + 1 }
        if (offset - s > 2) return false
        val now = t.subSequence(s, minOf(t.length, s + 2)).toString()
        when (now) { BOX -> t.replace(s, s + 2, TICK); TICK -> t.replace(s, s + 2, BOX); else -> return false }
        return true
    }
}

/** The note editor: the text as it will look, a toolbar of styles, and «حفظ». */
@Composable
fun RichNoteEditor(note: Store.Note?, close: () -> Unit) {
    BackHandler(onBack = close)
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var edit by remember { mutableStateOf<EditText?>(null) }
    var colors by remember { mutableStateOf(false) }
    val ink = P.ink.toArgb(); val hintColor = P.muted.toArgb()
    Column(Modifier.fillMaxSize().background(P.bg).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("→", color = P.accent, fontSize = 22.sp, modifier = Modifier.clip(L.shape(10.dp)).clickable(onClick = close).padding(horizontal = 10.dp, vertical = 6.dp))
            Text(if (note == null) "ملاحظة جديدة" else "تعديل الملاحظة", color = P.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("حفظ", color = P.onAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(L.shape(10.dp)).background(P.accent).clickable {
                val e = edit
                val text = e?.let { Rich.save(it.text) }.orEmpty()
                if (text.isNotBlank()) Store.saveNote(note?.id, text) else android.widget.Toast.makeText(ctx, "الملاحظة فارغة", android.widget.Toast.LENGTH_SHORT).show()
                close()
            }.padding(horizontal = 14.dp, vertical = 8.dp))
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            @Composable fun B(label: String, bold: Boolean = false, act: (EditText) -> Unit) = Text(label, color = P.ink, fontSize = 15.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.clip(L.shape(10.dp)).background(P.surface).border(1.dp, P.line, L.shape(10.dp)).clickable { edit?.let(act) }.padding(horizontal = 12.dp, vertical = 8.dp))
            B("عنوان ١", true) { Rich.heading(it, 1) }; B("عنوان ٢", true) { Rich.heading(it, 2) }; B("عنوان ٣") { Rich.heading(it, 3) }
            B("B", true) { Rich.toggle(it, StyleSpan::class.java, { s -> s.style == Typeface.BOLD }) { StyleSpan(Typeface.BOLD) } }
            B("I") { Rich.toggle(it, StyleSpan::class.java, { s -> s.style == Typeface.ITALIC }) { StyleSpan(Typeface.ITALIC) } }
            B("U") { Rich.toggle(it, UnderlineSpan::class.java, { true }) { UnderlineSpan() } }
            B("S̶") { Rich.toggle(it, StrikethroughSpan::class.java, { true }) { StrikethroughSpan() } }
            B("🖍") { Rich.toggle(it, Rich.Mark::class.java, { true }) { Rich.Mark(0x66F2C94C) } }
            B("🎨 لون") { colors = !colors }
            B("A+") { Rich.replace(it, Rich.Size::class.java) { Rich.Size("lg") } }; B("A++") { Rich.replace(it, Rich.Size::class.java) { Rich.Size("xl") } }
            B("a−") { Rich.replace(it, Rich.Size::class.java) { Rich.Size("sm") } }; B("A") { Rich.replace(it, Rich.Size::class.java) { null } }
            B("•") { Rich.sign(it, Rich.BULLET) }; B("☐") { Rich.sign(it, Rich.BOX) }; B("❝") { Rich.sign(it, Rich.QUOTE) }
        }
        if (colors) Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Md.colors.forEach { (k, c) -> Box(Modifier.size(30.dp).clip(CircleShape).background(c).clickable { edit?.let { e -> Rich.replace(e, Rich.Ink::class.java) { Rich.Ink(k, c.toArgb()) } }; colors = false }) }
            Box(Modifier.size(30.dp).clip(CircleShape).border(2.dp, P.line, CircleShape).clickable { edit?.let { e -> Rich.replace(e, Rich.Ink::class.java) { null } }; colors = false }, contentAlignment = Alignment.Center) { Text("✕", color = P.muted, fontSize = 13.sp) }
            Sub("اختر كلمات ثم لونًا، أو يلوَّن السطر", size = 11.sp)
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp).clip(L.shape(12.dp)).border(1.dp, P.line, L.shape(12.dp))) {
            AndroidView(factory = { c ->
                EditText(c).apply {
                    background = null; setPadding(36, 28, 36, 28); gravity = Gravity.TOP or Gravity.START
                    textDirection = android.view.View.TEXT_DIRECTION_FIRST_STRONG_RTL; textSize = 16f; setLineSpacing(0f, 1.25f)
                    setTextColor(ink); setHintTextColor(hintColor); hint = "اكتب هنا… واختر من الشريط العناوين والخط العريض والألوان والقوائم"
                    setText(Rich.load(note?.text ?: ""), android.widget.TextView.BufferType.SPANNABLE)
                    // A tap on a ☐ at a line's start ticks it.
                    setOnClickListener { if (selectionStart == selectionEnd) Rich.tickAt(this, selectionStart) }
                    edit = this
                }
            }, modifier = Modifier.fillMaxSize())
        }
    }
}
