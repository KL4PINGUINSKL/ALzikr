package com.alzikr.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Real icons instead of emoji: every emoji the app used maps to a Material Symbols Rounded icon
 * (res/drawable/ms_*.xml), drawn in the theme's color. Unknown characters stay as text.
 */
object Glyphs {
    val map: Map<String, Int> = mapOf(
        "📖" to R.drawable.ms_menu_book, "🌙" to R.drawable.ms_dark_mode, "📜" to R.drawable.ms_history_edu, "💡" to R.drawable.ms_lightbulb,
        "⏰" to R.drawable.ms_alarm, "🕋" to R.drawable.ms_deployed_code, "🕌" to R.drawable.ms_mosque, "📚" to R.drawable.ms_library_books,
        "🎙" to R.drawable.ms_mic, "📍" to R.drawable.ms_location_on, "☀" to R.drawable.ms_light_mode, "📿" to R.drawable.ms_adjust,
        "🔍" to R.drawable.ms_search, "🔎" to R.drawable.ms_search, "📅" to R.drawable.ms_calendar_month, "📆" to R.drawable.ms_date_range,
        "🔔" to R.drawable.ms_notifications, "🔕" to R.drawable.ms_notifications_off, "🤲" to R.drawable.ms_volunteer_activism,
        "🌅" to R.drawable.ms_wb_twilight, "🌇" to R.drawable.ms_nights_stay, "✨" to R.drawable.ms_auto_awesome, "🧭" to R.drawable.ms_explore,
        "⏸" to R.drawable.ms_pause, "🔁" to R.drawable.ms_repeat, "🟢" to R.drawable.ms_adjust, "🔥" to R.drawable.ms_local_fire_department,
        "📣" to R.drawable.ms_campaign, "🌍" to R.drawable.ms_public, "⏹" to R.drawable.ms_stop, "🔀" to R.drawable.ms_shuffle,
        "🗓" to R.drawable.ms_event_note, "✎" to R.drawable.ms_edit, "📑" to R.drawable.ms_toc, "⏳" to R.drawable.ms_hourglass_empty,
        "📗" to R.drawable.ms_auto_stories, "📘" to R.drawable.ms_book, "🎉" to R.drawable.ms_celebration, "🐑" to R.drawable.ms_celebration,
        "🌕" to R.drawable.ms_circle, "☑" to R.drawable.ms_task_alt, "✅" to R.drawable.ms_check_circle, "🎓" to R.drawable.ms_school,
        "🧠" to R.drawable.ms_psychology, "☰" to R.drawable.ms_menu, "🎯" to R.drawable.ms_quiz, "💰" to R.drawable.ms_payments,
        "🎨" to R.drawable.ms_palette, "➕" to R.drawable.ms_add, "⚠" to R.drawable.ms_warning, "🔒" to R.drawable.ms_lock,
        "📳" to R.drawable.ms_vibration, "🍽" to R.drawable.ms_restaurant, "⭐" to R.drawable.ms_star, "⏱" to R.drawable.ms_timer,
        "🌿" to R.drawable.ms_eco, "👆" to R.drawable.ms_touch_app, "⚙" to R.drawable.ms_settings, "💾" to R.drawable.ms_save,
        "📌" to R.drawable.ms_push_pin, "🔖" to R.drawable.ms_bookmark, "⚖" to R.drawable.ms_balance, "🤝" to R.drawable.ms_handshake,
        "💚" to R.drawable.ms_favorite, "🏛" to R.drawable.ms_account_balance, "⛰" to R.drawable.ms_landscape, "ℹ" to R.drawable.ms_info,
        "📝" to R.drawable.ms_edit_note, "💤" to R.drawable.ms_bedtime, "⬆" to R.drawable.ms_system_update, "↗" to R.drawable.ms_share,
        "🔁" to R.drawable.ms_repeat, "🗂" to R.drawable.ms_library_books, "🍃" to R.drawable.ms_eco, "🌤" to R.drawable.ms_sunny,
        "📋" to R.drawable.ms_checklist, "📊" to R.drawable.ms_bar_chart, "🐾" to R.drawable.ms_pets, "💧" to R.drawable.ms_water_drop, "🧳" to R.drawable.ms_luggage, "🛏" to R.drawable.ms_bed, "🏁" to R.drawable.ms_flag, "❤" to R.drawable.ms_favorite_fill, "♡" to R.drawable.ms_favorite, "🖼" to R.drawable.ms_image, "👁" to R.drawable.ms_visibility, "⛔" to R.drawable.ms_block, "⏮" to R.drawable.ms_skip_previous, "⏭" to R.drawable.ms_skip_next, "⏩" to R.drawable.ms_fast_forward, "⏬" to R.drawable.ms_download, "▶" to R.drawable.ms_play_arrow, "💬" to R.drawable.ms_format_quote, "🔄" to R.drawable.ms_sync_alt, "🔂" to R.drawable.ms_repeat,
    )
    private val keys = map.keys.sortedByDescending { it.length }

    /** The icon for [g] (variation selectors ignored), or null. */
    fun of(g: String): Int? = map[g.replace("️", "").trim()]

    /** Splits [text] into plain text and icons. */
    fun parts(text: String): List<Pair<String, Int?>> {
        val t = text.replace("️", "")
        val out = ArrayList<Pair<String, Int?>>()
        val sb = StringBuilder(); var i = 0
        loop@ while (i < t.length) {
            for (k in keys) if (t.startsWith(k, i)) {
                if (sb.isNotEmpty()) { out += sb.toString() to null; sb.clear() }
                out += k to map[k]; i += k.length
                continue@loop
            }
            sb.append(t[i]); i++
        }
        if (sb.isNotEmpty()) out += sb.toString() to null
        return out
    }
    fun has(text: String) = keys.any { text.contains(it) }
}

/** One icon (or the character itself when it has no icon), in [tint]. */
@Composable
fun Glyph(g: String, tint: Color = LocalContentColor.current, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    val res = Glyphs.of(g)
    if (res != null) Icon(painterResource(res), null, tint = tint, modifier = modifier.size(size))
    else Text(g, color = tint, fontSize = (size.value * .9f).sp, modifier = modifier)
}

/** Text whose emoji are drawn as real icons inline, sized and colored like the text. */
@Composable
fun GText(
    text: String, modifier: Modifier = Modifier, color: Color = LocalContentColor.current, fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null, fontFamily: FontFamily? = null, lineHeight: TextUnit = TextUnit.Unspecified, textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE, overflow: TextOverflow = TextOverflow.Clip, softWrap: Boolean = true,
) {
    @Suppress("NAME_SHADOWING") val text = Quran.numerals(text)   // the app's digit style, everywhere text goes through here
    if (!Glyphs.has(text)) { Text(text, modifier, color = color, fontSize = fontSize, fontWeight = fontWeight, fontFamily = fontFamily, lineHeight = lineHeight, textAlign = textAlign, maxLines = maxLines, overflow = overflow, softWrap = softWrap); return }
    val parts = remember(text) { Glyphs.parts(text) }
    val inline = HashMap<String, InlineTextContent>()
    val s: AnnotatedString = buildAnnotatedString {
        parts.forEachIndexed { i, (p, res) ->
            if (res == null) append(p)
            else {
                val id = "g$i"
                appendInlineContent(id, p)
                inline[id] = InlineTextContent(Placeholder(1.25.em, 1.15.em, PlaceholderVerticalAlign.TextCenter)) {
                    Box { Icon(painterResource(res), null, tint = color, modifier = Modifier.size(if (fontSize.isSp) (fontSize.value * 1.15f).dp else 18.dp)) }
                }
            }
        }
    }
    Text(s, modifier, color = color, fontSize = fontSize, fontWeight = fontWeight, fontFamily = fontFamily, lineHeight = lineHeight, textAlign = textAlign,
        maxLines = maxLines, overflow = overflow, softWrap = softWrap, inlineContent = inline)
}
