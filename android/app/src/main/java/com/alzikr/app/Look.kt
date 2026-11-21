package com.alzikr.app

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Where the sections live: a bottom bar, a floating dock (icons only), tabs at the top, or words only. */
enum class NavStyle { BAR, DOCK, TOP, WORDS }
enum class IconStyle { FILLED, OUTLINED, ROUNDED, SHARP, GLYPH, NONE }
/** How the five prayers are shown: a list, a grid of cards, or a timeline. */
enum class PrayerLayout { LIST, CARDS, TIMELINE }
/** The dhikr counter: a ring, prayer beads, or a large numeral. */
enum class CounterStyle { RING, BEADS, PLAIN }
enum class Corners { ROUND, SOFT, SQUARE, CUT }

/**
 * A theme is more than colors: corners, fonts, where the navigation sits, the icon set, how the
 * prayers and the counter are drawn, and whether panels are filled cards or just lines.
 */
data class Look(
    val id: String, val name: String, val about: String,
    val day: Pal?, val night: Pal?,
    val corners: Corners, val radius: Dp,
    val body: FontFamily?, val head: FontFamily?,
    val nav: NavStyle, val icons: IconStyle, val prayers: PrayerLayout, val counter: CounterStyle,
    val flat: Boolean = false, val ornament: String = "",
) {
    fun basePal(systemDark: Boolean, mode: Int): Pal = when {
        day == null -> night!!
        night == null -> day
        mode == 1 -> day
        mode == 2 -> night
        systemDark -> night
        else -> day
    }
    /** The palette with this theme's chosen color (one of seven, or a free hue). */
    fun pal(systemDark: Boolean, mode: Int, color: Int = Data.lookColor(id), hue: Float = Data.lookHue(id)): Pal =
        Tints.apply(basePal(systemDark, mode), color, hue)

    fun shape(r: Dp = radius): Shape = when (corners) {
        Corners.CUT -> CutCornerShape(r.coerceAtMost(14.dp))
        Corners.SQUARE -> RoundedCornerShape(r.coerceAtMost(4.dp))
        else -> RoundedCornerShape(r)
    }
}

object Fonts {
    val quran = FontFamily(Font(R.font.amiri_quran))
    val naskh = FontFamily(Font(R.font.amiri), Font(R.font.amiri, FontWeight.Bold))
    val modern = FontFamily(Font(R.font.tajawal), Font(R.font.tajawal_bold, FontWeight.Bold), Font(R.font.tajawal_bold, FontWeight.SemiBold), Font(R.font.tajawal, FontWeight.Medium))
    @OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
    private fun variable(res: Int) = FontFamily(
        Font(res, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
        Font(res, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
        Font(res, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
        Font(res, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    )
    val kufi by lazy { variable(R.font.reem_kufi) }
    val messiri by lazy { variable(R.font.el_messiri) }
    val changa by lazy { variable(R.font.changa) }
}

object Looks {
    private fun pal(dark: Boolean, bg: Long, surface: Long, card: Long, line: Long, ink: Long, muted: Long, accent: Long, onAccent: Long, gold: Long, danger: Long = 0xFFD9534F) =
        Pal(dark, Color(bg), Color(surface), Color(card), Color(line), Color(ink), Color(muted), Color(accent), Color(onAccent), Color(gold), Color(danger))

    val all: List<Look> by lazy { listOf(
        Look("emerald", "زمرّد", "الأخضر الهادئ مع الذهبي، بطاقات مستديرة وشريط سفلي",
            Day, Night, Corners.ROUND, 20.dp, null, null, NavStyle.BAR, IconStyle.FILLED, PrayerLayout.LIST, CounterStyle.RING),
        Look("mushaf", "مصحف", "ورق عتيق وحبر بني، خط النسخ، زوايا حادة وتبويبات علوية وزخرفة",
            pal(false, 0xFFF3EAD3, 0xFFFAF3E2, 0xFFECDDBC, 0xFFD5C196, 0xFF3A2816, 0xFF7A6248, 0xFF8A3B1C, 0xFFFFF8EC, 0xFFA47C1E),
            pal(true, 0xFF1C150D, 0xFF251C12, 0xFF2F2417, 0xFF4A3923, 0xFFF0E4C8, 0xFFB9A586, 0xFFD58A55, 0xFF1C1008, 0xFFD9B45A),
            Corners.SQUARE, 3.dp, Fonts.naskh, Fonts.naskh, NavStyle.TOP, IconStyle.GLYPH, PrayerLayout.LIST, CounterStyle.PLAIN),
        Look("night", "سماء الليل", "كحلي عميق ونجوم ذهبية، خط كوفي، شريط عائم وبطاقات للصلوات وسبحة خرز",
            null, pal(true, 0xFF0A0F25, 0xFF121A3A, 0xFF19224D, 0xFF27325F, 0xFFE9ECFF, 0xFF98A2CF, 0xFFF2C14E, 0xFF1A1300, 0xFFF2C14E),
            Corners.ROUND, 28.dp, null, Fonts.kufi, NavStyle.DOCK, IconStyle.ROUNDED, PrayerLayout.CARDS, CounterStyle.BEADS, ornament = "☾"),
        Look("andalus", "أندلس", "فيروزي وطوبي، زوايا مقصوصة كالزليج، خط المسيري وخط زمني للصلوات",
            pal(false, 0xFFF2EFE7, 0xFFFFFFFF, 0xFFE2EEEB, 0xFFC5DBD6, 0xFF0F3636, 0xFF4C6B68, 0xFF0E6B6B, 0xFFFFFFFF, 0xFFC0622F),
            pal(true, 0xFF0B1A1A, 0xFF112424, 0xFF173030, 0xFF244444, 0xFFE3F1EE, 0xFF8FB0AB, 0xFF3FB8B0, 0xFF031414, 0xFFE58A55),
            Corners.CUT, 12.dp, null, Fonts.messiri, NavStyle.BAR, IconStyle.OUTLINED, PrayerLayout.TIMELINE, CounterStyle.RING, ornament = "◇"),
        Look("desert", "صحراء", "رمل وطين، خط المسيري كاملًا، كلمات بدل الأيقونات وبطاقات للصلوات",
            pal(false, 0xFFF5E9D8, 0xFFFFF7EB, 0xFFECD4B4, 0xFFDDBF98, 0xFF472C1A, 0xFF86664D, 0xFFB94A2A, 0xFFFFF7EE, 0xFFB2802B),
            pal(true, 0xFF1E140C, 0xFF2A1C11, 0xFF352416, 0xFF4E3621, 0xFFF4E4CF, 0xFFBFA184, 0xFFE07A52, 0xFF1E0E05, 0xFFE0AE55),
            Corners.SOFT, 12.dp, Fonts.messiri, Fonts.messiri, NavStyle.WORDS, IconStyle.NONE, PrayerLayout.CARDS, CounterStyle.PLAIN),
        Look("minimal", "بسيط", "أبيض وأسود بلا بطاقات، خطوط فاصلة فقط، خط تجوّل وكلمات للتنقل",
            pal(false, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFF2F2F2, 0xFFE2E2E2, 0xFF111111, 0xFF6B6B6B, 0xFF111111, 0xFFFFFFFF, 0xFF8A6A1F),
            pal(true, 0xFF0B0B0B, 0xFF0B0B0B, 0xFF1A1A1A, 0xFF2A2A2A, 0xFFF2F2F2, 0xFF9A9A9A, 0xFFF2F2F2, 0xFF0B0B0B, 0xFFD9B45A),
            Corners.SQUARE, 0.dp, Fonts.modern, Fonts.modern, NavStyle.WORDS, IconStyle.NONE, PrayerLayout.LIST, CounterStyle.PLAIN, flat = true),
        Look("amoled", "أسود نقي", "أسود حقيقي يوفّر البطارية، أخضر نيون، خط شنغا وشريط عائم",
            null, pal(true, 0xFF000000, 0xFF070707, 0xFF101010, 0xFF1E1E1E, 0xFFFFFFFF, 0xFF8C8C8C, 0xFF00E08A, 0xFF00140B, 0xFFFFC857),
            Corners.ROUND, 16.dp, Fonts.changa, Fonts.changa, NavStyle.DOCK, IconStyle.OUTLINED, PrayerLayout.TIMELINE, CounterStyle.RING),
        Look("rose", "ورد", "وردي وعنابي، زوايا مستديرة جدًا، خط المسيري للعناوين وسبحة خرز",
            pal(false, 0xFFFBF0F2, 0xFFFFFFFF, 0xFFF5DCE3, 0xFFEBC6D1, 0xFF481B2B, 0xFF8C5868, 0xFFB0436A, 0xFFFFFFFF, 0xFFC4913A),
            pal(true, 0xFF1A0E14, 0xFF24131C, 0xFF2E1824, 0xFF452636, 0xFFF7E3EA, 0xFFC096A6, 0xFFE57CA2, 0xFF22060F, 0xFFE3B260),
            Corners.ROUND, 26.dp, null, Fonts.messiri, NavStyle.BAR, IconStyle.ROUNDED, PrayerLayout.CARDS, CounterStyle.BEADS, ornament = "❀"),
        Look("kaaba", "الكعبة", "سواد الكسوة وذهب الحزام، خط النسخ وعناوين كوفية، تبويبات علوية وسبحة خرز",
            null, pal(true, 0xFF0A0A09, 0xFF141310, 0xFF1D1A14, 0xFF3A3122, 0xFFF3EAD3, 0xFFA89A7C, 0xFFD4AF37, 0xFF1A1406, 0xFFE6C35C),
            Corners.SQUARE, 2.dp, Fonts.naskh, Fonts.kufi, NavStyle.TOP, IconStyle.GLYPH, PrayerLayout.LIST, CounterStyle.BEADS),
        Look("madinah", "المدينة", "أخضر القبة وبياض الحرم، خط المسيري، شريط عائم وبطاقات للصلوات",
            pal(false, 0xFFF3F7F2, 0xFFFFFFFF, 0xFFE1EEE4, 0xFFCDE0D2, 0xFF123524, 0xFF557563, 0xFF1B6B45, 0xFFFFFFFF, 0xFFB08A2E),
            pal(true, 0xFF0B1611, 0xFF112019, 0xFF172A21, 0xFF24402F, 0xFFE4F1E8, 0xFF8FB09D, 0xFF4CC48A, 0xFF03140B, 0xFFD9B45A),
            Corners.ROUND, 22.dp, Fonts.messiri, Fonts.messiri, NavStyle.DOCK, IconStyle.FILLED, PrayerLayout.CARDS, CounterStyle.RING),
        Look("fajr", "الفجر", "ألوان الشروق الهادئة، خط تجوّل، كلمات للتنقل وخط زمني وعدّاد رقمي",
            pal(false, 0xFFFFF3EB, 0xFFFFFFFF, 0xFFFBE2D5, 0xFFF1CCBA, 0xFF3C2A3E, 0xFF8A6E80, 0xFFCC5F76, 0xFFFFFFFF, 0xFFE0963A),
            pal(true, 0xFF181424, 0xFF201B30, 0xFF29223C, 0xFF3B3254, 0xFFF1E8F4, 0xFFB4A6C2, 0xFFF08FA4, 0xFF2A0812, 0xFFF2B35E),
            Corners.SOFT, 16.dp, Fonts.modern, Fonts.modern, NavStyle.WORDS, IconStyle.NONE, PrayerLayout.TIMELINE, CounterStyle.PLAIN),
        Look("sea", "البحر", "أزرق عميق وزبد أبيض، خط شنغا، أيقونات حادة وبطاقات للصلوات",
            pal(false, 0xFFEEF5F8, 0xFFFFFFFF, 0xFFDCEBF1, 0xFFC4DCE6, 0xFF0B2A3A, 0xFF4F6E7E, 0xFF0B5E86, 0xFFFFFFFF, 0xFFDB9B2F),
            pal(true, 0xFF06131B, 0xFF0B1D28, 0xFF102735, 0xFF1C3B4E, 0xFFE2F1F8, 0xFF8EAFC0, 0xFF3AB0E6, 0xFF021018, 0xFFF0B755),
            Corners.ROUND, 24.dp, Fonts.changa, Fonts.changa, NavStyle.BAR, IconStyle.SHARP, PrayerLayout.CARDS, CounterStyle.RING),
    ) }

    fun byId(id: String) = all.firstOrNull { it.id == id } ?: all.first()
}

val LocalLook = staticCompositionLocalOf { Looks.all.first() }
val L: Look @Composable get() = LocalLook.current

/**
 * Seven colors for every theme: its own (index 0) and six more, plus a free hue (index 7).
 * A color changes the accent and the gold, and tints the backgrounds a little toward it, so the
 * whole theme follows (not only the buttons).
 */
object Tints {
    data class Tint(val name: String, val day: Long, val night: Long, val gold: Long)
    val list = listOf(
        Tint("الأصلي", 0, 0, 0),
        Tint("فيروزي", 0xFF0E7C7B, 0xFF3CC6C0, 0xFFC98A2B),
        Tint("أزرق", 0xFF1E4FA8, 0xFF6E9BFF, 0xFFD4A23C),
        Tint("بنفسجي", 0xFF6B3FA0, 0xFFB48CF0, 0xFFD9A441),
        Tint("عنابي", 0xFF8E2340, 0xFFE0708E, 0xFFC9973F),
        Tint("كهرماني", 0xFFB0661A, 0xFFF0A64A, 0xFF2F8F7A),
        Tint("فحمي", 0xFF3E4A52, 0xFFB8C4CC, 0xFFB08D3C),
    )
    const val CUSTOM = 7

    private fun mix(a: Color, b: Color, t: Float) = Color(a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t, a.blue + (b.blue - a.blue) * t, 1f)
    private fun on(c: Color) = if (c.luminance() > 0.5f) Color(0xFF111111) else Color(0xFFFFFFFF)

    fun accentFor(index: Int, hue: Float, dark: Boolean): Color? = when (index) {
        in 1..6 -> Color(if (dark) list[index].night else list[index].day)
        CUSTOM -> Color.hsv(hue, if (dark) 0.55f else 0.70f, if (dark) 0.88f else 0.55f)
        else -> null
    }

    fun apply(base: Pal, index: Int, hue: Float): Pal {
        val accent = accentFor(index, hue, base.dark) ?: return base
        val gold = if (index in 1..6) Color(list[index].gold) else Color.hsv((hue + 40f) % 360f, 0.6f, if (base.dark) 0.85f else 0.65f)
        val k = if (base.dark) 1.4f else 1f
        return base.copy(
            bg = mix(base.bg, accent, 0.05f * k), surface = mix(base.surface, accent, 0.03f * k), card = mix(base.card, accent, 0.10f * k),
            line = mix(base.line, accent, 0.14f * k), accent = accent, onAccent = on(accent), gold = gold,
        )
    }
}
