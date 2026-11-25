package com.alzikr.app

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Notes written like Markdown, with a toolbar so no one needs to know the marks:
 *   # / ## / ###  headings · **bold** · *italic* · __underline__ · ~~struck~~ · ==highlight==
 *   {c:red}coloured{/c} · {s:lg}bigger{/s} ({s:sm} smaller, {s:xl} biggest)
 *   - bullet · 1. numbered · - [ ] / - [x] a box to tick · > a quote · --- a line
 * The text stays plain (copy, share, back up), and is shown formatted.
 */
object Md {
    val colors = linkedMapOf("red" to Color(0xFFD64545), "green" to Color(0xFF2E9E6A), "blue" to Color(0xFF3B6FD8), "gold" to Color(0xFFC9962E), "purple" to Color(0xFF8A4FD1), "gray" to Color(0xFF8A8F99))
    val colorNames = mapOf("red" to "أحمر", "green" to "أخضر", "blue" to "أزرق", "gold" to "ذهبي", "purple" to "بنفسجي", "gray" to "رمادي")
    private val sizes = mapOf("sm" to .82f, "lg" to 1.25f, "xl" to 1.55f)
    private val token = Regex("""\*\*(.+?)\*\*|__(.+?)__|~~(.+?)~~|==(.+?)==|\*(.+?)\*|\{c:(\w+)\}(.+?)\{/c\}|\{s:(\w+)\}(.+?)\{/s\}""")

    /** The inline marks of one line as styled text (nested marks work: «**{c:red}word{/c}**»). */
    fun inline(text: String, base: SpanStyle = SpanStyle(), highlight: Color = Color(0x55F2C94C)): AnnotatedString = buildAnnotatedString {
        fun walk(t: String, style: SpanStyle) {
            var i = 0
            for (m in token.findAll(t)) {
                if (m.range.first > i) withStyle(style) { append(t.substring(i, m.range.first)) }
                val g = m.groupValues
                when {
                    g[1].isNotEmpty() -> walk(g[1], style.merge(SpanStyle(fontWeight = FontWeight.Bold)))
                    g[2].isNotEmpty() -> walk(g[2], style.merge(SpanStyle(textDecoration = TextDecoration.Underline)))
                    g[3].isNotEmpty() -> walk(g[3], style.merge(SpanStyle(textDecoration = TextDecoration.LineThrough)))
                    g[4].isNotEmpty() -> walk(g[4], style.merge(SpanStyle(background = highlight)))
                    g[5].isNotEmpty() -> walk(g[5], style.merge(SpanStyle(fontStyle = FontStyle.Italic)))
                    g[7].isNotEmpty() -> walk(g[7], style.merge(SpanStyle(color = colors[g[6]] ?: Color.Unspecified)))
                    g[9].isNotEmpty() -> walk(g[9], style.merge(SpanStyle(fontSize = (16 * (sizes[g[8]] ?: 1f)).sp)))
                }
                i = m.range.last + 1
            }
            if (i < t.length) withStyle(style) { append(t.substring(i)) }
        }
        walk(text, base)
    }

    /** Without any marks (notifications, the first line as a title). */
    fun plain(source: String): String = tidy(source).lines().joinToString("\n") { l ->
        l.replace(Regex("^#{1,3} "), "").replace(Regex("^- \\[[ xX]\\] "), "☐ ").replace(Regex("^> "), "").replace(Regex("^- "), "• ")
    }.replace(Regex("""\{c:\w+\}|\{/c\}|\{s:\w+\}|\{/s\}"""), "").replace(Regex("""\*\*|__|~~|=="""), "").replace(Regex("""(?<!\S)\*|\*(?!\S)"""), "")

    fun title(md: String): String = plain(md).lineSequence().firstOrNull { it.isNotBlank() }?.trim()?.take(60) ?: "ملاحظة"

    /** For the widget and the bubble: Android spans (bold, colours, sizes, struck, ticks). */
    fun spanned(source: String, ink: Int): CharSequence {
        val md = tidy(source)
        val sb = android.text.SpannableStringBuilder()
        md.lines().forEachIndexed { n, raw ->
            if (n > 0) sb.append('\n')
            var line = raw; var rel = 1f; var bold = false; var prefix = ""
            when {
                line.startsWith("### ") -> { line = line.drop(4); rel = 1.1f; bold = true }
                line.startsWith("## ") -> { line = line.drop(3); rel = 1.25f; bold = true }
                line.startsWith("# ") -> { line = line.drop(2); rel = 1.45f; bold = true }
                Regex("^- \\[[xX]\\] ").containsMatchIn(line) -> { line = line.drop(6); prefix = "☑ " }
                line.startsWith("- [ ] ") -> { line = line.drop(6); prefix = "☐ " }
                line.startsWith("- ") -> { line = line.drop(2); prefix = "• " }
                line.startsWith("> ") -> { line = line.drop(2); prefix = "❝ " }
                line.trim() == "---" -> { line = "────────"; }
            }
            val start = sb.length
            sb.append(prefix)
            val a = inline(line)
            val at = sb.length
            sb.append(a.text)
            a.spanStyles.forEach { r ->
                val s = r.item; val from = at + r.start; val to = at + r.end
                if (s.fontWeight == FontWeight.Bold) sb.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), from, to, 0)
                if (s.fontStyle == FontStyle.Italic) sb.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.ITALIC), from, to, 0)
                if (s.textDecoration == TextDecoration.LineThrough) sb.setSpan(android.text.style.StrikethroughSpan(), from, to, 0)
                if (s.textDecoration == TextDecoration.Underline) sb.setSpan(android.text.style.UnderlineSpan(), from, to, 0)
                if (s.color != Color.Unspecified) sb.setSpan(android.text.style.ForegroundColorSpan(s.color.toArgbCompat()), from, to, 0)
                if (s.fontSize.isSp) sb.setSpan(android.text.style.RelativeSizeSpan(s.fontSize.value / 16f), from, to, 0)
            }
            if (rel != 1f) sb.setSpan(android.text.style.RelativeSizeSpan(rel), start, sb.length, 0)
            if (bold) sb.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, sb.length, 0)
            if (prefix == "☑ ") sb.setSpan(android.text.style.ForegroundColorSpan(ink and 0x99FFFFFF.toInt()), start, sb.length, 0)
        }
        return sb
    }
    private fun Color.toArgbCompat(): Int = android.graphics.Color.argb((alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())

    /** Ticks the box on line [line] of [md]. */
    fun toggleBox(md: String, line: Int): String = md.lines().mapIndexed { i, l ->
        if (i != line) l else if (l.startsWith("- [ ] ")) "- [x] " + l.drop(6) else if (Regex("^- \\[[xX]\\] ").containsMatchIn(l)) "- [ ] " + l.drop(6) else l
    }.joinToString("\n")

    /**
     * The text as written, styled in place for the editor: the same characters (so the cursor stays right), with the
     * marks nearly invisible and what they hold bold, coloured, bigger…, so writing looks like the result.
     */
    fun editing(text: String, ink: Color): AnnotatedString = buildAnnotatedString {
        append(text)
        val hide = SpanStyle(color = Color.Transparent, fontSize = 1.sp)
        val dim = SpanStyle(color = ink.copy(alpha = .35f))
        fun mark(r: IntRange) { if (!r.isEmpty()) addStyle(hide, r.first, r.last + 1) }
        // Headings: the «# » fades, the line grows.
        var at = 0
        for (line in text.split('\n')) {
            val h = when { line.startsWith("### ") -> 4; line.startsWith("## ") -> 3; line.startsWith("# ") -> 2; else -> 0 }
            if (h > 0) { addStyle(dim, at, at + h); addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = when (h) { 2 -> 24.sp; 3 -> 20.sp; else -> 17.sp }), at + h, at + line.length) }
            if (line.startsWith("> ")) { addStyle(dim, at, at + 2); addStyle(SpanStyle(fontStyle = FontStyle.Italic, color = Color(0xFFC9962E)), at + 2, at + line.length) }
            at += line.length + 1
        }
        fun each(r: Regex, open: Int, close: Int, style: (MatchResult) -> SpanStyle) = r.findAll(text).forEach { m ->
            val s = m.range.first; val e = m.range.last + 1
            mark(s until s + open); mark(e - close until e)
            if (e - close > s + open) addStyle(style(m), s + open, e - close)
        }
        each(Regex("""\*\*(.+?)\*\*"""), 2, 2) { SpanStyle(fontWeight = FontWeight.Bold) }
        each(Regex("""__(.+?)__"""), 2, 2) { SpanStyle(textDecoration = TextDecoration.Underline) }
        each(Regex("""~~(.+?)~~"""), 2, 2) { SpanStyle(textDecoration = TextDecoration.LineThrough) }
        each(Regex("""==(.+?)=="""), 2, 2) { SpanStyle(background = Color(0x55F2C94C)) }
        each(Regex("""(?<![*])\*(?![*\s])(.+?)(?<![*\s])\*(?![*])"""), 1, 1) { SpanStyle(fontStyle = FontStyle.Italic) }
        Regex("""\{c:(\w+)\}(.*?)\{/c\}""").findAll(text).forEach { m ->
            val s = m.range.first; val e = m.range.last + 1; val open = m.groupValues[1].length + 4
            mark(s until s + open); mark(e - 4 until e)
            if (e - 4 > s + open) addStyle(SpanStyle(color = colors[m.groupValues[1]] ?: ink), s + open, e - 4)
        }
        Regex("""\{s:(\w+)\}(.*?)\{/s\}""").findAll(text).forEach { m ->
            val s = m.range.first; val e = m.range.last + 1; val open = m.groupValues[1].length + 4
            mark(s until s + open); mark(e - 4 until e)
            if (e - 4 > s + open) addStyle(SpanStyle(fontSize = (16 * (sizes[m.groupValues[1]] ?: 1f)).sp), s + open, e - 4)
        }
    }

    /** Takes out marks that hold nothing («{c:blue}{/c}», «****»), left when a mark was pressed with nothing selected. */
    fun tidy(md: String): String {
        var t = md
        // A size (or colour) mark right inside another of the same kind — «{s:sm}{s:lg}text{/s}{/s}» from pressing
        // twice — keeps the inner one only; marks around nothing go.
        while (true) {
            val before = t
            t = t.replace(Regex("""\{s:\w+\}(\{s:\w+\})"""), "$1").replace("{/s}{/s}", "{/s}")
                .replace(Regex("""\{c:\w+\}(\{c:\w+\})"""), "$1").replace("{/c}{/c}", "{/c}")
                .replace(Regex("""\{c:\w+\}\{/c\}|\{s:\w+\}\{/s\}|\*\*\*\*|____|~~~~|===="""), "")
            if (t == before) break
        }
        // Still unpaired: that kind of mark is dropped, so no mark is ever shown as text.
        fun balanced(open: Regex, close: String) = open.findAll(t).count() == Regex(Regex.escape(close)).findAll(t).count()
        if (!balanced(Regex("""\{s:\w+\}"""), "{/s}")) t = t.replace(Regex("""\{s:\w+\}|\{/s\}"""), "")
        if (!balanced(Regex("""\{c:\w+\}"""), "{/c}")) t = t.replace(Regex("""\{c:\w+\}|\{/c\}"""), "")
        return t
    }

    /** The papers a note can be written on. */
    val papers = listOf(
        Color(0xFFFFF4B8), Color(0xFFFFE0E6), Color(0xFFDDF3E4), Color(0xFFDCEBFF), Color(0xFFEDE3FF), Color(0xFFFFFFFF),
        Color(0xFFFFE2C2), Color(0xFFFFD6D6), Color(0xFFD5F5F0), Color(0xFFE3F7C8), Color(0xFFF6E7D2), Color(0xFFE8E8EC),
        Color(0xFFFFF0F7), Color(0xFFD9F1FF), Color(0xFFFDF6E3), Color(0xFFEAD9C6), Color(0xFFDDE6D3), Color(0xFFF3D9FA))
    /** The note's paper (index kept within the list). */
    fun paper(i: Int) = papers[i.coerceIn(0, papers.size - 1)]
}

/** A note shown formatted; [onToggle] ticks a box (line index), [scale] for small previews, [maxLines] to cut long ones. */
@Composable
fun NoteText(source: String, ink: Color = P.ink, scale: Float = 1f, maxLines: Int = Int.MAX_VALUE, onToggle: ((Int) -> Unit)? = null) {
    val md = Md.tidy(source)
    val lines = md.lines()
    Column(verticalArrangement = Arrangement.spacedBy((3 * scale).dp)) {
        var number = 0
        lines.take(maxLines).forEachIndexed { i, raw ->
            val base = SpanStyle(color = ink)
            when {
                raw.startsWith("### ") -> Text(Md.inline(raw.drop(4), base), fontSize = (17 * scale).sp, fontWeight = FontWeight.Bold, color = ink)
                raw.startsWith("## ") -> Text(Md.inline(raw.drop(3), base), fontSize = (20 * scale).sp, fontWeight = FontWeight.Bold, color = ink)
                raw.startsWith("# ") -> Text(Md.inline(raw.drop(2), base), fontSize = (24 * scale).sp, fontWeight = FontWeight.Bold, color = ink, lineHeight = (32 * scale).sp)
                raw.trim() == "---" -> Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(ink.copy(alpha = .25f)))
                raw.startsWith("> ") -> Row(Modifier.fillMaxWidth()) {
                    Box(Modifier.width(3.dp).height((22 * scale).dp).background(P.gold))
                    Text(Md.inline(raw.drop(2), base.merge(SpanStyle(fontStyle = FontStyle.Italic))), fontSize = (15 * scale).sp, color = ink, fontFamily = Fonts.naskh, modifier = Modifier.padding(start = 8.dp))
                }
                raw.startsWith("- [ ] ") || Regex("^- \\[[xX]\\] ").containsMatchIn(raw) -> {
                    val on = !raw.startsWith("- [ ] ")
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = if (onToggle != null) Modifier.clip(RoundedCornerShape(6.dp)).clickable { onToggle(i) } else Modifier) {
                        Box(Modifier.size((18 * scale).dp).clip(RoundedCornerShape(4.dp)).background(if (on) P.accent else Color.Transparent).border(1.5.dp, if (on) P.accent else ink.copy(alpha = .6f), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center) { if (on) Text("✓", color = Color.White, fontSize = (11 * scale).sp, fontWeight = FontWeight.Bold) }
                        Text(Md.inline(raw.drop(6), if (on) base.merge(SpanStyle(textDecoration = TextDecoration.LineThrough, color = ink.copy(alpha = .55f))) else base),
                            fontSize = (15 * scale).sp, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                raw.startsWith("- ") -> Row { Text("•  ", color = P.gold, fontSize = (15 * scale).sp); Text(Md.inline(raw.drop(2), base), fontSize = (15 * scale).sp, lineHeight = (24 * scale).sp) }
                Regex("^\\d+\\. ").containsMatchIn(raw) -> { number++; val body = raw.substringAfter(". ")
                    Row { Text("${Quran.digits(number)}.  ", color = P.gold, fontSize = (15 * scale).sp); Text(Md.inline(body, base), fontSize = (15 * scale).sp, lineHeight = (24 * scale).sp) } }
                raw.isBlank() -> Spacer(Modifier.height((6 * scale).dp))
                else -> Text(Md.inline(raw, base), fontSize = (15 * scale).sp, lineHeight = (24 * scale).sp)
            }
            if (!Regex("^\\d+\\. ").containsMatchIn(raw)) number = 0
        }
        if (lines.size > maxLines) Text("…", color = ink.copy(alpha = .6f), fontSize = (14 * scale).sp)
    }
}

/** Writing a note: the text, a toolbar for the marks, and a preview. */
@Composable
fun NoteEditor(note: Store.Note?, close: () -> Unit) {
    BackHandler(onBack = close)
    val ctx = LocalContext.current
    var v by remember { mutableStateOf(TextFieldValue(note?.text ?: "")) }
    var preview by remember { mutableStateOf(false) }
    var colorsOpen by remember { mutableStateOf(false) }
    /** Puts [a]…[b] around the selection (or at the cursor). */
    fun wrap(a: String, b: String) {
        var s = v.selection; val t = v.text
        if (s.collapsed) {
            // Nothing selected: the mark goes around the line the cursor is on (after its «# », «- » …), not into empty space.
            val from = t.lastIndexOf('\n', (s.min - 1).coerceAtLeast(0)).let { if (it < 0 || s.min == 0) 0 else it + 1 }
            val to = t.indexOf('\n', s.min).let { if (it < 0) t.length else it }
            val lead = Regex("^(#{1,3} |- \\[[ xX]\\] |- |> |\\d+\\. )").find(t.substring(from, to))?.value?.length ?: 0
            if (to > from + lead) s = TextRange(from + lead, to)
        }
        val mid = t.substring(s.min, s.max)
        v = TextFieldValue(t.substring(0, s.min) + a + mid + b + t.substring(s.max), TextRange(s.min + a.length, s.min + a.length + mid.length))
    }
    /** Puts [p] at the start of the selected lines (or takes it away when they already have it). */
    fun line(p: String) {
        val t = v.text; val s = v.selection
        val from = t.lastIndexOf('\n', (s.min - 1).coerceAtLeast(0)).let { if (it < 0 || s.min == 0) 0 else it + 1 }
        val to = t.indexOf('\n', s.max).let { if (it < 0) t.length else it }
        val block = t.substring(from, to).lines()
        val heads = listOf("# ", "## ", "### ", "- [ ] ", "- [x] ", "- ", "> ", "1. ")
        val all = block.all { it.startsWith(p) }
        val out = block.joinToString("\n") { l -> val bare = heads.firstOrNull { l.startsWith(it) }?.let { l.drop(it.length) } ?: l; if (all) bare else p + bare }
        v = TextFieldValue(t.substring(0, from) + out + t.substring(to), TextRange(from + out.length))
    }
    Column(Modifier.fillMaxSize().background(P.bg).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("→", color = P.accent, fontSize = 22.sp, modifier = Modifier.clip(L.shape(10.dp)).clickable(onClick = close).padding(horizontal = 10.dp, vertical = 6.dp))
            Text(if (note == null) "ملاحظة جديدة" else "تعديل الملاحظة", color = P.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(if (preview) "✎ كتابة" else "👁 معاينة", color = P.accent, fontSize = 14.sp, modifier = Modifier.clip(L.shape(10.dp)).clickable { preview = !preview }.padding(8.dp))
            Text("حفظ", color = P.onAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(L.shape(10.dp)).background(P.accent).clickable {
                val text = Md.tidy(v.text).trimEnd()
                if (text.isNotBlank()) Store.saveNote(note?.id, text) else Toast.makeText(ctx, "الملاحظة فارغة", Toast.LENGTH_SHORT).show()
                close()
            }.padding(horizontal = 14.dp, vertical = 8.dp))
        }
        // The toolbar: one row that scrolls sideways.
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            @Composable fun B(label: String, bold: Boolean = false, act: () -> Unit) = Text(label, color = P.ink, fontSize = 15.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.clip(L.shape(10.dp)).background(P.surface).border(1.dp, P.line, L.shape(10.dp)).clickable { preview = false; act() }.padding(horizontal = 12.dp, vertical = 8.dp))
            B("عنوان ١", true) { line("# ") }; B("عنوان ٢", true) { line("## ") }; B("عنوان ٣") { line("### ") }
            B("B", true) { wrap("**", "**") }; B("I") { wrap("*", "*") }; B("U") { wrap("__", "__") }; B("S̶") { wrap("~~", "~~") }; B("🖍") { wrap("==", "==") }
            B("🎨 لون") { colorsOpen = !colorsOpen }; B("A+") { wrap("{s:lg}", "{/s}") }; B("A++") { wrap("{s:xl}", "{/s}") }; B("a−") { wrap("{s:sm}", "{/s}") }
            B("•") { line("- ") }; B("1.") { line("1. ") }; B("☐") { line("- [ ] ") }; B("❝") { line("> ") }
            B("—") { val t = v.text; val at = v.selection.max; v = TextFieldValue(t.substring(0, at) + "\n---\n" + t.substring(at), TextRange(at + 5)) }
        }
        if (colorsOpen) Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Md.colors.forEach { (k, c) -> Box(Modifier.size(30.dp).clip(CircleShape).background(c).clickable { wrap("{c:$k}", "{/c}"); colorsOpen = false }) }
            Sub("اختر كلمات ثم لونًا، أو يلوَّن السطر كله", size = 11.sp)
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp)) {
            if (preview) Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) { NoteText(v.text) { i -> v = v.copy(text = Md.toggleBox(v.text, i)) } }
            else {
                val ink = P.ink
                OutlinedTextField(v, { v = it }, modifier = Modifier.fillMaxSize(), placeholder = { Text("اكتب هنا… واستعمل الشريط للعناوين والخط العريض والألوان وقوائم المهام") },
                    textStyle = androidx.compose.ui.text.TextStyle(color = ink, fontSize = 16.sp, lineHeight = 26.sp),
                    // What you write shows formatted as you write it (same characters, so the cursor stays right).
                    visualTransformation = { t -> androidx.compose.ui.text.input.TransformedText(Md.editing(t.text, ink), androidx.compose.ui.text.input.OffsetMapping.Identity) })
            }
        }
    }
}

/** One note, open: formatted, its boxes tickable, and where it is pinned. */
@Composable
fun NoteSheet(id: Long, edit: () -> Unit, close: () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val ver = Store.version
    val ctx = LocalContext.current
    val n = Store.notes().firstOrNull { it.id == id } ?: run { close(); return }
    var del by remember { mutableStateOf(false) }
    AppSheet(onDismissRequest = close) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(6.dp)).clip(RoundedCornerShape(6.dp)).background(Md.paper(n.paper)).padding(16.dp)) {
                NoteText(n.text, ink = Color(0xFF2A2A2A)) { i -> Store.updateNote(n.id) { it.copy(text = Md.toggleBox(it.text, i)) } }
            }
            Wrap(gap = 6.dp) {
                Choice("✎ تعديل", false) { close(); edit() }
                Choice(if (n.pinned) "📌 مثبّتة أعلى الملاحظات" else "📌 أعلى الملاحظات", n.pinned) { Store.togglePin(n.id) }
                Choice("↗ مشاركة", false) { ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, Md.plain(n.text)), "مشاركة")) }
                Choice("🗑 حذف", false) { del = true }
            }
            Title("علّقها كقصاصة في…")
            Wrap(gap = 6.dp) {
                listOf("" to "لا مكان", "all" to "كل الصفحات", "prayer" to "الصلاة", "adhkar" to "الأذكار", "library" to "المكتبة", "daily" to "يومي", "calendar" to "التقويم", "more" to "المزيد").forEach { (k, l) ->
                    Choice(l, n.place == k) { Store.updateNote(n.id) { it.copy(place = k) } }
                }
            }
            Title("لون الورقة")
            Wrap(gap = 8.dp) {
                Md.papers.forEachIndexed { i, c -> Box(Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).background(c).border(if (n.paper == i) 3.dp else 1.dp, if (n.paper == i) P.accent else P.line, RoundedCornerShape(6.dp)).clickable { Store.updateNote(n.id) { it.copy(paper = i) } }) }
            }
            Title("خارج التطبيق")
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("📋 على الشاشة الرئيسية", color = P.ink, fontSize = 15.sp)
                    Sub("في أداة «ملاحظتي» (أضفها من أدوات الشاشة الرئيسية في هاتفك)", size = 12.sp)
                }
                Switch(n.widget, { Store.noteOnWidget(n.id, it); NoteWidget.update(ctx) })
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("🫧 فقاعة فوق التطبيقات", color = P.ink, fontSize = 15.sp)
                    Sub("دبوس صغير على حافة الشاشة فوق كل التطبيقات؛ اضغطه لترى الملاحظة، واسحبه حيث شئت", size = 12.sp)
                }
                Switch(n.bubble, { on ->
                    Store.noteInBubble(n.id, on)
                    if (on && !Settings.canDrawOverlays(ctx)) { Toast.makeText(ctx, "اسمح لـ ALzikr بالظهور فوق التطبيقات، ثم ارجع", Toast.LENGTH_LONG).show()
                        runCatching { ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${ctx.packageName}"))) } }
                    NoteBubble.refresh(ctx)
                })
            }
        }
    }
    if (del) androidx.compose.material3.AlertDialog(onDismissRequest = { del = false }, containerColor = P.surface,
        title = { Text("حذف الملاحظة؟", color = P.ink) },
        confirmButton = { androidx.compose.material3.TextButton({ Store.deleteNote(n.id); NoteWidget.update(ctx); NoteBubble.refresh(ctx); del = false; close() }) { Text("حذف", color = P.danger) } },
        dismissButton = { androidx.compose.material3.TextButton({ del = false }) { Text("إلغاء") } })
}

/**
 * Notes pinned to this page hang at its top corner like paper scraps on a board: a red pin, a slight tilt, a gentle
 * swing. A tap opens the note; «⌃» folds the scraps to small pins until the app is opened again.
 */
@Composable
fun PinnedScraps(place: String, modifier: Modifier = Modifier) {
    @Suppress("UNUSED_VARIABLE") val ver = Store.version
    val notes = Store.notes().filter { it.place == "all" || (it.place.isNotEmpty() && it.place == place) }
    if (notes.isEmpty()) return
    var open by remember { mutableStateOf<Long?>(null) }
    var editing by remember { mutableStateOf<Store.Note?>(null) }
    val swing = rememberInfiniteTransition(label = "swing")
    val a by swing.animateFloat(-1.6f, 1.6f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Reverse), label = "a")
    Row(modifier.padding(top = 2.dp, end = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        notes.take(3).forEachIndexed { k, n ->
            val folded = n.id in Pins.folded
            Box(contentAlignment = Alignment.TopCenter, modifier = Modifier.graphicsLayer {
                transformOrigin = TransformOrigin(.5f, 0f)
                rotationZ = (if (k % 2 == 0) 3f else -2.5f) + if (com.alzikr.app.pets.PetPerf.reduceMotion) 0f else a
            }) {
                if (!folded) Column(Modifier.padding(top = 9.dp).widthIn(max = 150.dp).heightIn(max = 130.dp).shadow(4.dp, RoundedCornerShape(3.dp))
                    .background(Brush.verticalGradient(listOf(Md.paper(n.paper), Md.paper(n.paper).copy(alpha = .92f))))
                    .clickable { open = n.id }.padding(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 8.dp)) {
                    NoteText(n.text, ink = Color(0xFF2A2A2A), scale = .72f, maxLines = 6)
                }
                // The pin.
                Box(Modifier.size(16.dp).shadow(2.dp, CircleShape).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFFFF8A80), Color(0xFFC62828))))
                    .clickable { if (folded) Pins.folded.remove(n.id) else Pins.folded.add(n.id) })
            }
        }
    }
    open?.let { id -> NoteSheet(id, edit = { editing = Store.notes().firstOrNull { it.id == id } }) { open = null } }
    editing?.let { e -> androidx.compose.ui.window.Dialog({ editing = null }, androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) { RichNoteEditor(e) { editing = null } } }
}

/** Scraps folded to their pins (tap the pin), until the app starts again. */
object Pins { val folded = androidx.compose.runtime.mutableStateListOf<Long>() }

@Suppress("unused") private fun Context.keep() = Unit
