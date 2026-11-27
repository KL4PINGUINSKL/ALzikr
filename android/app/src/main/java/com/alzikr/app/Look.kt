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
enum class NavStyle { BAR, DOCK, TOP, WORDS, LANTERNS, RAIL, ARC }
enum class IconStyle { FILLED, OUTLINED, ROUNDED, SHARP, GLYPH, NONE }
/** How the five prayers are shown: a list, a grid of cards, or a timeline. */
enum class PrayerLayout { LIST, CARDS, TIMELINE, LANTERNS, SUNDIAL, GARDEN }
/** The dhikr counter: a ring, prayer beads, or a large numeral. */
enum class CounterStyle { RING, BEADS, PLAIN, LANTERN, FLOWER, CRESCENT }
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
    /** Titles in the accent colour (gold on «الهلال»), the words under them in the theme's second colour. */
    val accentTitles: Boolean = false,
    /** The theme for children: the kids section is the first tab, text is larger, the friend speaks to a child. */
    val kids: Boolean = false,
    /** A living background drawn behind the app (Backdrops.kt). */
    val backdrop: Backdrop = Backdrop.NONE,
    /** The colors follow the hour of the day by the real prayer times (Sky). */
    val sky: Boolean = false,
    /** The panels' form (ThemeForms.kt), and what grows in the prayers' garden ("rose", "cactus", "crystal"). */
    val panel: PanelStyle = PanelStyle.CARD,
    val motif: String = "rose",
    /** A palette that follows the time or the season (رواق النور، حديقة الفصول), checked each minute. */
    val palFor: (() -> Pal)? = null,
    /** A painted scene shown at the top of the prayer page (HeroScenes.kt), its light following the day. */
    val hero: String? = null,
) {
    fun basePal(systemDark: Boolean, mode: Int): Pal = when {
        day == null -> night!!
        night == null -> day
        mode == 1 -> day
        mode == 2 -> night
        systemDark -> night
        else -> day
    }
    /**
     * The palette: the theme's colors, its chosen buttons color, and optionally its own background and
     * text color (mixed freely). A black background stays black whatever the color.
     */
    fun pal(systemDark: Boolean, mode: Int, color: Int = Data.lookColor(id), hue: Float = Data.lookHue(id),
            back: Int = Data.lookBack(id), ink: Int = Data.lookInk(id)): Pal {
        val b = Backs.list.getOrNull(back)?.takeIf { back > 0 }?.takeIf { !Data.simple || basePal(systemDark, mode).dark }
            // «أسود نقي» on a theme already black at night («الهلال»): keep its own coloured cards over the black.
            ?.takeIf { !(it.bg == 0xFF000000L && night?.bg == Color.Black && basePal(systemDark, mode).dark) }
        var p = if (b == null) (palFor?.invoke() ?: if (sky) Sky.pal() else basePal(systemDark, mode)) else (if (b.dark) night ?: day else day ?: night)!!
        if (b != null) p = p.copy(dark = b.dark, bg = Color(b.bg), surface = Color(b.surface), card = Color(b.card), line = Color(b.line),
            ink = if (b.dark) Color(0xFFF2F2F2) else Color(0xFF151515), muted = if (b.dark) Color(0xFF9A9A9A) else Color(0xFF6A6A6A))
        p = Tints.apply(p, color, hue, keepBg = b != null || p.bg.luminance() < 0.004f)
        Inks.list.getOrNull(ink)?.takeIf { ink > 0 }?.let { k ->
            val c = Color(k.c)
            if (Tints.contrast(c, p.bg) >= 3f) p = p.copy(ink = c, muted = Tints.mix(c, p.bg, .42f))
        }
        val accent = Tints.fit(p.accent, p.bg)
        return p.copy(accent = accent, onAccent = Tints.on(accent), gold = Tints.fit(p.gold, p.bg))
    }

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

    /** The same without onAccent: black or white text on the accent, whichever reads. */
    private fun pal(dark: Boolean, bg: Long, surface: Long, card: Long, line: Long, ink: Long, muted: Long, accent: Long, gold: Long) =
        pal(dark, bg, surface, card, line, ink, muted, accent, if (Tints.on(Color(accent)) == Color.White) 0xFFFFFFFF else 0xFF111111, gold)

    val all: List<Look> by lazy { listOf(
        Look("hilal", "الهلال", "أزرق ملكي وذهبي كأيقونة التطبيق، بطاقات مستديرة وشريط سفلي",
            pal(false, 0xFFF4F1E8, 0xFFFFFDF7, 0xFFECE7D8, 0xFFD9D2BE, 0xFF0E1E4E, 0xFF5A6488, 0xFF1A3274, 0xFFFFFFFF, 0xFFA67C1A),
            pal(true, 0xFF000000, 0xFF071338, 0xFF0C1B4D, 0xFF2A48A8, 0xFFF6EED8, 0xFF8DB0FF, 0xFFF3D27A, 0xFF0B1433, 0xFF6E9BFF),
            Corners.ROUND, 20.dp, null, null, NavStyle.BAR, IconStyle.FILLED, PrayerLayout.LIST, CounterStyle.RING, accentTitles = true),
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
        Look("kids", "الأطفال", "سماء وشمس وألوان مرحة، خط كبير، وقسم الأطفال أول الأقسام: الحروف والتجويد والوضوء والصلاة… التطبيق كله للصغار",
            pal(false, 0xFFEAF6FF, 0xFFFFFFFF, 0xFFFFF2C4, 0xFFBFDFF5, 0xFF1D2B5A, 0xFF56689A, 0xFFF0603F, 0xFFFFFFFF, 0xFF1E9E63),
            pal(true, 0xFF101634, 0xFF19204A, 0xFF242C5E, 0xFF344080, 0xFFF1F4FF, 0xFFA9B4E0, 0xFFFF8A65, 0xFF2A0A00, 0xFF5EE0A0),
            Corners.ROUND, 30.dp, Fonts.changa, Fonts.changa, NavStyle.BAR, IconStyle.ROUNDED, PrayerLayout.CARDS, CounterStyle.BEADS, ornament = "⭐", kids = true),
        // For girls, open from the start: soft pink and lavender, bubbles floating up, petal-shaped panels, the five prayers as
        // five roses that open as you pray, a flower for the counter, rounded Messiri letters and a floating bar.
        Look("girls", "بنوتة", "وردي ولافندر ناعمان، قلوب وورود صغيرة تطفو وتتمايل، لوحات كالبتلات، والصلوات خمس ورود تتفتّح بصلاتك، والسبحة زهرة، وخط المسيري المستدير · خاص بالبنات 🎀",
            pal(false, 0xFFFFF4F9, 0xFFFFFFFF, 0xFFFDE6F1, 0xFFF4C9DE, 0xFF5B2449, 0xFF9C6C8C, 0xFFE2589C, 0xFFFFFFFF, 0xFF9B72D8),
            pal(true, 0xFF1B1021, 0xFF25162B, 0xFF311B3A, 0xFF4D2B57, 0xFFFDE9F4, 0xFFCDA6C8, 0xFFFF8CC6, 0xFF2B0619, 0xFFC7A6F5),
            Corners.ROUND, 30.dp, Fonts.messiri, Fonts.messiri, NavStyle.DOCK, IconStyle.ROUNDED, PrayerLayout.GARDEN, CounterStyle.FLOWER, ornament = "♡",
            accentTitles = true, backdrop = Backdrop.BLOSSOM, panel = PanelStyle.PETAL, motif = "rose"),
        // For boys, open from the start: blue and green, stars, planets and a little rocket.
        Look("faris", "فارس", "أزرق السماء وأخضر الربيع، نجوم تتلألأ وكواكب تسبح وصاروخ صغير يعبر أحيانًا، وخط شنغا الواضح · للأولاد 🚀",
            pal(false, 0xFFF0F6FF, 0xFFFFFFFF, 0xFFDDEBFF, 0xFFB9D2F2, 0xFF14284B, 0xFF5A6F92, 0xFF2F6FE0, 0xFFFFFFFF, 0xFF1FA37A),
            pal(true, 0xFF0B1226, 0xFF121B36, 0xFF1A2548, 0xFF2C3C6C, 0xFFEAF1FF, 0xFF9FB0D6, 0xFF5FA2FF, 0xFF04102A, 0xFF4FE0A8),
            Corners.ROUND, 20.dp, Fonts.changa, Fonts.changa, NavStyle.DOCK, IconStyle.ROUNDED, PrayerLayout.CARDS, CounterStyle.RING, ornament = "☾",
            accentTitles = true, backdrop = Backdrop.SPACE),
        // A meadow: grass and flowers swaying, butterflies wandering.
        Look("meadow", "المرج", "عشب وأزهار تتمايل في الأسفل وفراشات ملوّنة تتجوّل، أخضر ربيعي وذهب الشمس، والصلوات حديقة تُزهر",
            pal(false, 0xFFF4FAEF, 0xFFFFFFFF, 0xFFE4F2DA, 0xFFC9E2B8, 0xFF1F3A1E, 0xFF5E7A58, 0xFF4E9A3A, 0xFFFFFFFF, 0xFFE0A030),
            pal(true, 0xFF0F1A10, 0xFF162418, 0xFF1E3020, 0xFF33502F, 0xFFEAF6E4, 0xFFA6C29C, 0xFF8BD06E, 0xFF0A1A06, 0xFFF2C04E),
            Corners.ROUND, 22.dp, null, Fonts.messiri, NavStyle.BAR, IconStyle.ROUNDED, PrayerLayout.GARDEN, CounterStyle.FLOWER, ornament = "❀",
            backdrop = Backdrop.MEADOW, panel = PanelStyle.PETAL, motif = "rose"),
        // A calm aquarium: fish, bubbles, seaweed.
        Look("aquarium", "حوض الأسماك", "أسماك صغيرة ملوّنة تسبح بهدوء وفقاعات تصعد وأعشاب تتمايل، فيروزي ومرجاني، ولوحات كالزجاج",
            pal(false, 0xFFEFF9FB, 0xFFFFFFFF, 0xFFD8F0F4, 0xFFB3DCE6, 0xFF0F3440, 0xFF4F7480, 0xFF1F8FA8, 0xFFFFFFFF, 0xFFF08A5D),
            pal(true, 0xFF061A22, 0xFF0B2530, 0xFF10303D, 0xFF1E4B5C, 0xFFE4F7FB, 0xFF8FB9C4, 0xFF4FC9E0, 0xFF02161C, 0xFFFF9E7A),
            Corners.ROUND, 20.dp, null, Fonts.changa, NavStyle.DOCK, IconStyle.ROUNDED, PrayerLayout.TIMELINE, CounterStyle.RING, ornament = "◇",
            backdrop = Backdrop.AQUARIUM, panel = PanelStyle.GLASS),
        // Opened by friendship (Friendship.gifts theme_<id>).
        Look("breeze", "نسيم", "نعناعي وسماوي هادئ، خط المسيري للعناوين، شريط عائم وبطاقات للصلوات · هدية الصداقة",
            pal(false, 0xFFEEF8F5, 0xFFFFFFFF, 0xFFDDF1EC, 0xFFC2E2DA, 0xFF123A36, 0xFF5A7E78, 0xFF2A9D8F, 0xFFFFFFFF, 0xFFE0A84A),
            pal(true, 0xFF08171A, 0xFF0E2226, 0xFF142D32, 0xFF21444B, 0xFFE3F5F2, 0xFF8DB3AD, 0xFF5CD6C2, 0xFF02201C, 0xFFF0C060),
            Corners.ROUND, 22.dp, null, Fonts.messiri, NavStyle.DOCK, IconStyle.ROUNDED, PrayerLayout.CARDS, CounterStyle.RING, ornament = "≈"),
        Look("dusk", "غسق", "بنفسجي المغيب وذهب خافت، عناوين كوفية، خط زمني للصلوات وسبحة خرز · هدية الصداقة",
            pal(false, 0xFFF6F0F8, 0xFFFFFFFF, 0xFFEADDF0, 0xFFD8C4E3, 0xFF2E1A3D, 0xFF7A6688, 0xFF7B4BA8, 0xFFFFFFFF, 0xFFD2953A),
            pal(true, 0xFF140E1E, 0xFF1C1429, 0xFF261B36, 0xFF3A2B52, 0xFFF0E6F8, 0xFFAE9CC0, 0xFFB58AE6, 0xFF1A0A2A, 0xFFE8B060),
            Corners.SOFT, 16.dp, Fonts.modern, Fonts.kufi, NavStyle.BAR, IconStyle.OUTLINED, PrayerLayout.TIMELINE, CounterStyle.BEADS, ornament = "✦"),
        Look("moon", "ضوء القمر", "ليل فضّي وذهب القمر، خط النسخ وعناوين كوفية، شريط عائم وسبحة خرز · هدية الصداقة",
            null, pal(true, 0xFF0C0F16, 0xFF141925, 0xFF1C2232, 0xFF2C3447, 0xFFEDEFF5, 0xFF9AA3B8, 0xFFC9D3E6, 0xFF0C0F16, 0xFFE8C877),
            Corners.ROUND, 26.dp, Fonts.naskh, Fonts.kufi, NavStyle.DOCK, IconStyle.GLYPH, PrayerLayout.CARDS, CounterStyle.BEADS, ornament = "☽"),
        // Friendship themes with living backgrounds (Backdrops.kt).
        Look("lantern", "مشكاة", "توهّج قنديل معلّق يتنفّس كاللهب وجمرات صاعدة، كهرماني دافئ، خط النسخ وعناوين كوفية وسبحة خرز · هدية الصداقة",
            pal(false, 0xFFFBF3E4, 0xFFFFFAF0, 0xFFF3E2C2, 0xFFE4CB9C, 0xFF3A2410, 0xFF85674A, 0xFFB8641A, 0xFFFFF8EE, 0xFFC9922A),
            pal(true, 0xFF120C06, 0xFF1B130A, 0xFF251A0E, 0xFF3D2B17, 0xFFF6E7CC, 0xFFBFA27C, 0xFFF0A040, 0xFF1E1002, 0xFFF5C35A),
            Corners.ROUND, 24.dp, Fonts.naskh, Fonts.kufi, NavStyle.DOCK, IconStyle.GLYPH, PrayerLayout.CARDS, CounterStyle.BEADS, ornament = "✺", backdrop = Backdrop.LANTERN),
        Look("garden", "روضة", "كروم وأوراق تتسلّق جانبي الشاشة وتتمايل وأزهار صغيرة، أخضر الربيع، خط المسيري · هدية الصداقة",
            pal(false, 0xFFF2F8EE, 0xFFFFFFFF, 0xFFE2F0DA, 0xFFC9E2BC, 0xFF1C3A18, 0xFF5E7A56, 0xFF3E8E2E, 0xFFFFFFFF, 0xFFD46A8A),
            pal(true, 0xFF0A1508, 0xFF10200D, 0xFF172B12, 0xFF24421D, 0xFFE6F4DE, 0xFF97B48C, 0xFF7CD65E, 0xFF0A1A05, 0xFFF08FB0),
            Corners.ROUND, 24.dp, Fonts.messiri, Fonts.messiri, NavStyle.BAR, IconStyle.ROUNDED, PrayerLayout.CARDS, CounterStyle.RING, ornament = "❦", backdrop = Backdrop.VINES),
        Look("zellige", "زليج", "شبكة نجوم ثمانية كبلاط الأندلس، أزرق كوبالتي وطيني، زوايا مقصوصة وتبويبات علوية وخط زمني · هدية الصداقة",
            pal(false, 0xFFF1F4FA, 0xFFFFFFFF, 0xFFDFE7F5, 0xFFC2D0EA, 0xFF102347, 0xFF51648A, 0xFF1F4FA8, 0xFFFFFFFF, 0xFFC0622F),
            pal(true, 0xFF070D1C, 0xFF0D1529, 0xFF131E38, 0xFF223255, 0xFFE3EBFA, 0xFF8FA0C6, 0xFF6E9BFF, 0xFF020818, 0xFFE58A55),
            Corners.CUT, 12.dp, null, Fonts.kufi, NavStyle.TOP, IconStyle.SHARP, PrayerLayout.TIMELINE, CounterStyle.RING, ornament = "✵", backdrop = Backdrop.GIRIH),
        Look("rain", "غيث", "مطر لطيف يهطل ودوائر تتّسع حيث يسقط، رمادي أزرق هادئ، خط شنغا وعدّاد رقمي · هدية الصداقة",
            pal(false, 0xFFEFF3F6, 0xFFFFFFFF, 0xFFDFE7EE, 0xFFC7D4DF, 0xFF1A2A38, 0xFF5C6E7E, 0xFF2F6E9E, 0xFFFFFFFF, 0xFF6E9E5A),
            pal(true, 0xFF0D1319, 0xFF141C24, 0xFF1B2530, 0xFF2A3846, 0xFFE4ECF3, 0xFF94A6B6, 0xFF6FB2E8, 0xFF04121E, 0xFF9ACB7E),
            Corners.SOFT, 18.dp, Fonts.changa, Fonts.changa, NavStyle.DOCK, IconStyle.OUTLINED, PrayerLayout.LIST, CounterStyle.PLAIN, ornament = "◌", backdrop = Backdrop.RAIN),
        Look("aurora", "شفق", "أشرطة الشفق القطبي تتموّج أعلى الشاشة بالأخضر والبنفسجي فوق ليل عميق، عناوين كوفية · هدية الصداقة",
            null, pal(true, 0xFF050B14, 0xFF0B1422, 0xFF111D30, 0xFF1D2E48, 0xFFE6F4F2, 0xFF8FA9B8, 0xFF3CE0A8, 0xFF02160F, 0xFFC77DFF),
            Corners.ROUND, 28.dp, Fonts.modern, Fonts.kufi, NavStyle.DOCK, IconStyle.ROUNDED, PrayerLayout.TIMELINE, CounterStyle.BEADS, ornament = "✧", backdrop = Backdrop.AURORA),
        Look("illuminated", "مخطوطة مذهّبة", "إطار مذهّب مزدوج بوريدات في الزوايا كصفحة مخطوطة مزخرفة، رقّ وحبر، خط النسخ كاملًا · هدية الصداقة",
            pal(false, 0xFFF6EEDB, 0xFFFCF6E8, 0xFFEFE2C3, 0xFFDCC69A, 0xFF2A1C0C, 0xFF77603F, 0xFF8A2A1C, 0xFFFFF6E8, 0xFFB08A2E),
            pal(true, 0xFF0D1220, 0xFF131A2C, 0xFF1A2238, 0xFF2C3654, 0xFFF1E8D2, 0xFFB0A486, 0xFFE0B050, 0xFF161002, 0xFFE8C468),
            Corners.SQUARE, 2.dp, Fonts.naskh, Fonts.naskh, NavStyle.TOP, IconStyle.GLYPH, PrayerLayout.LIST, CounterStyle.BEADS, ornament = "❁", backdrop = Backdrop.ILLUMINATED),
        Look("sky", "سماء الوقت", "ثيم حيّ تتبدّل ألوانه مع أوقات الصلاة الحقيقية في مدينتك: فجر وضحى وظهيرة وعصر وغروب وليل، بشمس تعبر السماء وقمر ونجوم · هدية الصداقة",
            Day, Night, Corners.ROUND, 22.dp, null, Fonts.messiri, NavStyle.DOCK, IconStyle.ROUNDED, PrayerLayout.TIMELINE, CounterStyle.RING, ornament = "☀", backdrop = Backdrop.SKY, sky = true),
        Look("stars", "سَحَر", "سماء الثلث الأخير من الليل: نجوم تتلألأ وشهاب يعبر بين حين وآخر، بنفسجي ليلي وفضّة · هدية الصداقة",
            null, pal(true, 0xFF07060F, 0xFF0F0D1C, 0xFF16132A, 0xFF262043, 0xFFEDEAFB, 0xFFA29CC4, 0xFFB9C6FF, 0xFF0A0A1A, 0xFFF2D27A),
            Corners.ROUND, 30.dp, Fonts.modern, Fonts.kufi, NavStyle.DOCK, IconStyle.OUTLINED, PrayerLayout.CARDS, CounterStyle.BEADS, ornament = "✦", backdrop = Backdrop.STARS),
        Look("light", "نور", "أشعّة نور ذهبية تنسكب من أعلى وتدور ببطء، أبيض وذهب، خط المسيري · هدية الصداقة",
            pal(false, 0xFFFFFCF4, 0xFFFFFFFF, 0xFFFBF1D8, 0xFFF0DFB2, 0xFF33280F, 0xFF7E6E48, 0xFFB8860B, 0xFFFFFFFF, 0xFFC9A02A),
            pal(true, 0xFF14110A, 0xFF1C180E, 0xFF262013, 0xFF3D341F, 0xFFFAF2DC, 0xFFC4B48C, 0xFFF0C24A, 0xFF1A1300, 0xFFF5D67A),
            Corners.ROUND, 26.dp, Fonts.messiri, Fonts.messiri, NavStyle.BAR, IconStyle.FILLED, PrayerLayout.CARDS, CounterStyle.RING, ornament = "✺", backdrop = Backdrop.RAYS),
        Look("snow", "ثلج", "ندف ثلج تتساقط وتتمايل على أزرق جليدي، خط تجوّل وعدّاد رقمي · هدية الصداقة",
            pal(false, 0xFFF2F7FC, 0xFFFFFFFF, 0xFFE3EEF8, 0xFFCADCEC, 0xFF14304A, 0xFF5A7690, 0xFF2D7BC0, 0xFFFFFFFF, 0xFF8AA6C8),
            pal(true, 0xFF0A121C, 0xFF101A28, 0xFF172334, 0xFF26364C, 0xFFEAF2FA, 0xFF9AB0C6, 0xFF8CC8FF, 0xFF041222, 0xFFD6E6F6),
            Corners.ROUND, 20.dp, Fonts.modern, Fonts.modern, NavStyle.DOCK, IconStyle.ROUNDED, PrayerLayout.LIST, CounterStyle.PLAIN, ornament = "❄", backdrop = Backdrop.SNOW),
        Look("petals", "بتلات", "بتلات وردية تنساب مع النسيم وتدور وهي تسقط، وردي ناعم وأخضر، خط المسيري · هدية الصداقة",
            pal(false, 0xFFFDF4F6, 0xFFFFFFFF, 0xFFF8E3E9, 0xFFEFCAD5, 0xFF46202E, 0xFF8E6271, 0xFFC0507A, 0xFFFFFFFF, 0xFF4E9A6A),
            pal(true, 0xFF1A0F14, 0xFF24151C, 0xFF2E1B25, 0xFF472A39, 0xFFF8E6EC, 0xFFC29CAB, 0xFFF08FB4, 0xFF2A0612, 0xFF8ED6A6),
            Corners.ROUND, 28.dp, null, Fonts.messiri, NavStyle.BAR, IconStyle.ROUNDED, PrayerLayout.CARDS, CounterStyle.BEADS, ornament = "✿", backdrop = Backdrop.PETALS),
        Look("fireflies", "يراعات", "يراعات مضيئة تحوم في ليل أخضر وكل واحدة تتوهّج وتخفت وحدها، خط شنغا · هدية الصداقة",
            null, pal(true, 0xFF060E08, 0xFF0B160E, 0xFF111F15, 0xFF1E3324, 0xFFE8F4E6, 0xFF93AE95, 0xFFD8EE6A, 0xFF101400, 0xFFE8F57A),
            Corners.SOFT, 18.dp, Fonts.changa, Fonts.changa, NavStyle.DOCK, IconStyle.OUTLINED, PrayerLayout.TIMELINE, CounterStyle.RING, ornament = "✦", backdrop = Backdrop.FIREFLIES),
        Look("bubbles", "أعماق", "فقاعات تصعد وضوء يتراقص كقاع البحر، فيروزي عميق، أيقونات حادة وبطاقات للصلوات · هدية الصداقة",
            null, pal(true, 0xFF03141C, 0xFF072029, 0xFF0B2B36, 0xFF15414F, 0xFFDDF4F8, 0xFF8AB3BE, 0xFF3FD0E0, 0xFF001A20, 0xFFF0C060),
            Corners.ROUND, 24.dp, Fonts.changa, Fonts.changa, NavStyle.BAR, IconStyle.SHARP, PrayerLayout.CARDS, CounterStyle.BEADS, ornament = "◦", backdrop = Backdrop.BUBBLES),
        Look("dunes", "كثبان", "كثبان متموّجة في أسفل الشاشة ورمال تسفوها الريح، رملي وطيني، خط المسيري كاملًا · هدية الصداقة",
            pal(false, 0xFFFAF1E2, 0xFFFFFAF0, 0xFFF1E0C2, 0xFFE2C79C, 0xFF3E2814, 0xFF85684A, 0xFFB0562A, 0xFFFFF7EE, 0xFFC48A2A),
            pal(true, 0xFF19110A, 0xFF22180E, 0xFF2D2014, 0xFF46331F, 0xFFF6E8D2, 0xFFC0A486, 0xFFE8885A, 0xFF1E0E05, 0xFFE8B860),
            Corners.SOFT, 14.dp, Fonts.messiri, Fonts.messiri, NavStyle.WORDS, IconStyle.NONE, PrayerLayout.TIMELINE, CounterStyle.PLAIN, ornament = "〰", backdrop = Backdrop.DUNES),
        Look("rosette", "وردة هندسية", "وردة إسلامية من اثني عشر ضلعًا في ثلاث حلقات تدور ببطء شديد خلف كل شيء، زمردي وذهبي، عناوين كوفية · هدية الصداقة",
            pal(false, 0xFFF3F6F1, 0xFFFFFFFF, 0xFFE2ECDF, 0xFFC9DAC4, 0xFF15301E, 0xFF587060, 0xFF1F6B45, 0xFFFFFFFF, 0xFFB8892A),
            pal(true, 0xFF07120C, 0xFF0D1C14, 0xFF13261B, 0xFF213C2C, 0xFFE4F2E8, 0xFF93AF9C, 0xFF5CCB8E, 0xFF02160A, 0xFFE6C35C),
            Corners.CUT, 10.dp, Fonts.naskh, Fonts.kufi, NavStyle.TOP, IconStyle.GLYPH, PrayerLayout.LIST, CounterStyle.RING, ornament = "✹", backdrop = Backdrop.ROSETTE),
        Look("skyline", "مآذن", "قباب ومآذن في الأفق وهلال فوقها، وفي الليل تضيء نوافذها واحدة بعد أخرى، خط النسخ · هدية الصداقة",
            pal(false, 0xFFF4F1EA, 0xFFFFFFFF, 0xFFE8E1D2, 0xFFD6CBB4, 0xFF2A2418, 0xFF746A55, 0xFF2E5E7A, 0xFFFFFFFF, 0xFFC0902E),
            pal(true, 0xFF0B0E16, 0xFF121722, 0xFF19202E, 0xFF2A3346, 0xFFECEFF5, 0xFF9AA3B6, 0xFF7FB0D8, 0xFF061420, 0xFFF0C860),
            Corners.ROUND, 18.dp, Fonts.naskh, Fonts.naskh, NavStyle.DOCK, IconStyle.GLYPH, PrayerLayout.TIMELINE, CounterStyle.BEADS, ornament = "☪", backdrop = Backdrop.SKYLINE),
        // Themes that change the app's whole form (ThemeForms.kt): panels, navigation, the prayers and the counter.
        Look("cactus", "صبّار", "لوحات كالحصى، والأقسام على قوس كغصن، والصلوات خمس صبّارات تُزهر كلما صلّيت، والسبحة زهرة تتفتّح بتلاتها · هدية الصداقة",
            pal(false, 0xFFF7F1E6, 0xFFFFFBF3, 0xFFEDE3CF, 0xFFDCCBA8, 0xFF2B3A22, 0xFF6E7A5C, 0xFF3E7D46, 0xFFFFFFFF, 0xFFD0603A),
            pal(true, 0xFF12140E, 0xFF1A1D14, 0xFF22261A, 0xFF353B28, 0xFFEDEFDF, 0xFFA6AD8E, 0xFF7CC47E, 0xFF0C1A0A, 0xFFF08A5A),
            Corners.ROUND, 26.dp, Fonts.changa, Fonts.messiri, NavStyle.ARC, IconStyle.ROUNDED, PrayerLayout.GARDEN, CounterStyle.FLOWER, ornament = "🌵",
            backdrop = Backdrop.CACTI, panel = PanelStyle.PEBBLE, motif = "cactus"),
        Look("ramadan", "فانوس رمضان", "الأقسام فوانيس معلّقة بخيط، واللوحات محاريب مقوّسة، والصلوات خمسة فوانيس تُضيء بصلاتك، والسبحة فانوس يمتلئ نورًا، وزينة رمضان تتمايل · هدية الصداقة",
            null, pal(true, 0xFF0E0A1E, 0xFF171130, 0xFF201840, 0xFF35295E, 0xFFF6EEDC, 0xFFB4A8C8, 0xFFF2C14E, 0xFF1A1300, 0xFFF2C14E),
            Corners.ROUND, 16.dp, Fonts.naskh, Fonts.kufi, NavStyle.LANTERNS, IconStyle.GLYPH, PrayerLayout.LANTERNS, CounterStyle.LANTERN, ornament = "🏮",
            backdrop = Backdrop.BUNTING, panel = PanelStyle.ARCH),
        Look("damask", "الوردة الدمشقية", "لوحات على شكل بتلة، والأقسام على قوس، والصلوات خمس ورود تتفتّح بصلاتك، والسبحة زهرة، وبتلات تتساقط، وردي وقرمزي وذهب · هدية الصداقة",
            pal(false, 0xFFFDF3F4, 0xFFFFFFFF, 0xFFF8E1E5, 0xFFEFC5CE, 0xFF4A1426, 0xFF8E5A68, 0xFFB0274E, 0xFFFFFFFF, 0xFFC4913A),
            pal(true, 0xFF190A10, 0xFF241018, 0xFF301622, 0xFF4A2234, 0xFFF9E4EA, 0xFFC897A6, 0xFFF0668E, 0xFF2A0612, 0xFFE8B860),
            Corners.ROUND, 28.dp, Fonts.messiri, Fonts.messiri, NavStyle.ARC, IconStyle.ROUNDED, PrayerLayout.GARDEN, CounterStyle.FLOWER, ornament = "🌹",
            backdrop = Backdrop.PETALS, panel = PanelStyle.PETAL, motif = "rose"),
        Look("arizona", "أريزونا", "صحراء الهضاب الحمراء والصبّار العملاق تحت شمس الغروب، والأقسام على شريط جانبي، والصلوات مزولة تعبرها الشمس، والسبحة هلال يكتمل بدرًا · هدية الصداقة",
            pal(false, 0xFFFBF0E6, 0xFFFFF8F0, 0xFFF3DCC8, 0xFFE3BFA0, 0xFF3E1E10, 0xFF8A5E48, 0xFFC0502A, 0xFFFFFFFF, 0xFF2E9A9A),
            pal(true, 0xFF1A0F0A, 0xFF24150E, 0xFF301C13, 0xFF4A2C1E, 0xFFF8E6D8, 0xFFC6A08A, 0xFFF08050, 0xFF1E0A02, 0xFF4CC8C0),
            Corners.SOFT, 12.dp, Fonts.changa, Fonts.changa, NavStyle.RAIL, IconStyle.SHARP, PrayerLayout.SUNDIAL, CounterStyle.CRESCENT, ornament = "☀",
            backdrop = Backdrop.MESA, panel = PanelStyle.FRAMED),
        Look("ink", "حبر وقلم", "التطبيق كصفحة يكتبها خطّاط: لوحات كلفائف الرقّ، وقطرات حبر تنتشر، وكلمات للتنقل، وخط النسخ كاملًا · هدية الصداقة",
            pal(false, 0xFFF8F3E8, 0xFFFFFCF4, 0xFFF0E8D6, 0xFFDCCFB2, 0xFF1A140C, 0xFF6A5E4A, 0xFF9A2A1E, 0xFFFFF8EE, 0xFF8A6A2A),
            pal(true, 0xFF11100D, 0xFF191712, 0xFF221F18, 0xFF38332A, 0xFFF2ECDE, 0xFFADA48E, 0xFFE0705A, 0xFF1E0602, 0xFFD8B060),
            Corners.SQUARE, 2.dp, Fonts.naskh, Fonts.naskh, NavStyle.WORDS, IconStyle.NONE, PrayerLayout.TIMELINE, CounterStyle.CRESCENT, ornament = "✒",
            backdrop = Backdrop.INK, panel = PanelStyle.SCROLL),
        Look("glass", "زجاج", "لوحات من زجاج مصنفر تسبح خلفها أضواء ملوّنة، والأقسام على قوس، والصلوات خمس بلّورات تُضيء بصلاتك، والسبحة هلال · هدية الصداقة",
            pal(false, 0xFFF0F2F8, 0xFFFFFFFF, 0xFFE6E9F3, 0xFFD3D8E8, 0xFF161A2E, 0xFF5C6280, 0xFF5B4BE0, 0xFFFFFFFF, 0xFF00A89E),
            pal(true, 0xFF0A0B14, 0xFF12131F, 0xFF1A1B2B, 0xFF2C2E45, 0xFFF0F1FA, 0xFF9EA2C0, 0xFFA89CFF, 0xFF100A30, 0xFF4CE0D2),
            Corners.ROUND, 24.dp, Fonts.modern, Fonts.modern, NavStyle.ARC, IconStyle.OUTLINED, PrayerLayout.GARDEN, CounterStyle.CRESCENT, ornament = "◈",
            backdrop = Backdrop.ORBS, panel = PanelStyle.GLASS, motif = "crystal"),
        Look("velvet", "المخمل الملكي", "قرمزي مخملي ونسيج دمشقي خافت، ولوحات بإطار مذهّب مزدوج، والأقسام على شريط جانبي، والصلوات مزولة ذهبية، وسبحة خرز · هدية الصداقة",
            null, pal(true, 0xFF1A060C, 0xFF240A12, 0xFF2F0E18, 0xFF4E1A28, 0xFFF8E8DA, 0xFFC8A090, 0xFFE6C35C, 0xFF1A1000, 0xFFE6C35C),
            Corners.SQUARE, 3.dp, Fonts.naskh, Fonts.kufi, NavStyle.RAIL, IconStyle.GLYPH, PrayerLayout.SUNDIAL, CounterStyle.BEADS, ornament = "♛",
            backdrop = Backdrop.DAMASK, panel = PanelStyle.FRAMED),
        // From the themes the user described (رواق النور and the others).
        Look("riwaq", "رواق النور", "عمارة أندلسية: أقواس أمام حديقة تتبدّل إضاءتها مع كل صلاة (فجر بنفسجي، شروق ذهبي بين الأقواس، ظهر أخضر، ظلال العصر، مغرب برتقالي، فوانيس العشاء ونجومه، ليل فضّي)، ونافورة وساعة في القوس وأوراق تتساقط، ولوحات من زجاج مصقول بحواف ذهبية · هدية الصداقة",
            Riwaq.palOf(Riwaq.Moment.DHUHR), Riwaq.palOf(Riwaq.Moment.ISHA), Corners.ROUND, 22.dp, Fonts.messiri, Fonts.kufi, NavStyle.DOCK, IconStyle.ROUNDED, PrayerLayout.SUNDIAL, CounterStyle.BEADS, ornament = "۞",
            backdrop = Backdrop.RIWAQ, panel = PanelStyle.GLASS, palFor = { Riwaq.pal() }),
        Look("sultani", "المخطوطة السلطانية", "مكتبة تاريخية فاخرة: ورق عاجي بأطراف مذهّبة وشمسة في رأس الصفحة، ولوحات كأغلفة الجلد المخيطة، وخط النسخ، وكحلي وذهب في الليل · هدية الصداقة",
            pal(false, 0xFFF7F0DE, 0xFFFFF9EA, 0xFFEFE3C4, 0xFFDCC79A, 0xFF2A1A0C, 0xFF7A6040, 0xFF7A2A18, 0xFFFFF6E8, 0xFFB08A2E),
            pal(true, 0xFF0C1326, 0xFF131C34, 0xFF1A2542, 0xFF2C3A60, 0xFFF1E8D2, 0xFFB0A486, 0xFFE0B050, 0xFF161002, 0xFFE8C468),
            Corners.SOFT, 8.dp, Fonts.naskh, Fonts.naskh, NavStyle.TOP, IconStyle.GLYPH, PrayerLayout.LIST, CounterStyle.BEADS, ornament = "❈",
            backdrop = Backdrop.MANUSCRIPT, panel = PanelStyle.LEATHER),
        Look("oasis", "الواحة الحية", "واحة بنخيل يتمايل وماء صافٍ وجبال بعيدة، طبقاتها تتحرك بسرعات مختلفة فتبدو عميقة، وسماؤها تتبع الوقت، ومطر خفيف في الشتاء وضباب الصباح · هدية الصداقة",
            pal(false, 0xFFF6F4EC, 0xFFFFFFFF, 0xFFE9EFE2, 0xFFD2DEC6, 0xFF1E3624, 0xFF5E7462, 0xFF2E8A5A, 0xFFD49A3A),
            pal(true, 0xFF0B1612, 0xFF12211A, 0xFF192C22, 0xFF284434, 0xFFE6F2EA, 0xFF96B0A0, 0xFF6CCB96, 0xFFE8B860),
            Corners.ROUND, 24.dp, Fonts.changa, Fonts.messiri, NavStyle.ARC, IconStyle.ROUNDED, PrayerLayout.GARDEN, CounterStyle.FLOWER, ornament = "🌴",
            backdrop = Backdrop.OASIS, panel = PanelStyle.PEBBLE, motif = "cactus", sky = true),
        Look("stained", "زجاج معشّق", "خلفية ليلية عميقة ونافذة هندسية من زجاج ملوّن يتسلّل منها الضوء فتنعكس ألوانه على الأرض، ولوحات من زجاج مصقول — بهيّ على شاشات AMOLED · هدية الصداقة",
            null, pal(true, 0xFF060A18, 0xFF0D1326, 0xFF141C34, 0xFF243052, 0xFFEEF0F8, 0xFF9CA4C0, 0xFFF2B53A, 0xFF1A1000, 0xFF20B8C0),
            Corners.ROUND, 22.dp, Fonts.modern, Fonts.kufi, NavStyle.RAIL, IconStyle.OUTLINED, PrayerLayout.LANTERNS, CounterStyle.CRESCENT, ornament = "❖",
            backdrop = Backdrop.STAINED, panel = PanelStyle.GLASS),
        Look("paperworld", "عالم من الورق", "عالم مصنوع يدويًا من طبقات الورق المقصوص: جبال وتلال ومسجد وأشجار بظلال حقيقية وسحب ورقية تتحرك، ولوحات كأوراق مقصوصة بظلّها · هدية الصداقة",
            pal(false, 0xFFF4EEE4, 0xFFFFFBF4, 0xFFEDE4D4, 0xFFD8CBB4, 0xFF3A2E22, 0xFF7E6E5A, 0xFFC06A3A, 0xFF4E8A6A),
            pal(true, 0xFF1A1E2E, 0xFF22283C, 0xFF2A3148, 0xFF3C4560, 0xFFEEEAF4, 0xFFA8A4BC, 0xFFF0A070, 0xFF8ED6A6),
            Corners.SOFT, 4.dp, Fonts.changa, Fonts.changa, NavStyle.BAR, IconStyle.ROUNDED, PrayerLayout.CARDS, CounterStyle.FLOWER, ornament = "✂",
            backdrop = Backdrop.PAPER, panel = PanelStyle.PAPER),
        Look("observatory", "المرصد", "مرصد مفتوح على السماء: أسطرلاب بحلقات مدرّجة تدور ببطء، والقمر كما يبدو الليلة فعلًا، ونجوم، والصلوات مزولة يعبرها الوقت؛ أزرق ملكي وفضّة وذهب — للجمال وقياس الزمن فقط · هدية الصداقة",
            null, pal(true, 0xFF0B1030, 0xFF121942, 0xFF1A2352, 0xFF2C3870, 0xFFECEEFA, 0xFFA2A8CC, 0xFFC8D0E0, 0xFF0B1030, 0xFFD8B060),
            Corners.ROUND, 26.dp, Fonts.modern, Fonts.kufi, NavStyle.ARC, IconStyle.OUTLINED, PrayerLayout.SUNDIAL, CounterStyle.CRESCENT, ornament = "✧",
            backdrop = Backdrop.OBSERVATORY, panel = PanelStyle.GLASS),
        Look("seasons", "حديقة الفصول", "حديقة تتبدّل مع السنة: أزهار وفراشات في الربيع، وماء يتلألأ في الصيف، وأوراق ذهبية تتساقط في الخريف، وثلج ونوافذ دافئة في الشتاء، وفوانيس رمضان، وزهور العيد وألوانه · هدية الصداقة",
            YearSeason.palOf(YearSeason.S.SPRING), YearSeason.palOf(YearSeason.S.WINTER), Corners.ROUND, 24.dp, Fonts.messiri, Fonts.messiri, NavStyle.BAR, IconStyle.ROUNDED, PrayerLayout.GARDEN, CounterStyle.FLOWER, ornament = "❀",
            backdrop = Backdrop.SEASONS, panel = PanelStyle.PETAL, motif = "rose", palFor = { YearSeason.pal() }),
        Look("marble", "الرخام والذهب", "فخامة القصور: رخام بعروق خفيفة أبيض في النهار وأسود في الليل، وتطعيم زمردي بحواف ذهبية، ولوحات رخامية بزوايا من الزمرد — جمال الخامات بلا حركة كثيرة · هدية الصداقة",
            pal(false, 0xFFF8F7F4, 0xFFFFFFFF, 0xFFF0EEEA, 0xFFDAD6CE, 0xFF1A1A1A, 0xFF6A665E, 0xFF174D43, 0xFFB0883E),
            pal(true, 0xFF0D0D0E, 0xFF161617, 0xFF1E1E20, 0xFF33322E, 0xFFF4F2EC, 0xFFA8A49A, 0xFFC9A66B, 0xFF174D43),
            Corners.SQUARE, 6.dp, Fonts.naskh, Fonts.kufi, NavStyle.TOP, IconStyle.SHARP, PrayerLayout.CARDS, CounterStyle.BEADS, ornament = "◆",
            backdrop = Backdrop.MARBLE, panel = PanelStyle.MARBLE),
        // Codex's proposals («اقتراح ثيمات»), renamed so they do not clash with the themes above.
        Look(id = "c_arches", name = "حديقة الأقواس", about = "«سكينةٌ تسكن الضوء» — أقواس عالية وحديقة ونافورة وفوانيس، ومشهدها المرسوم أعلى صفحة الصلاة مع الساعة والصلاة القادمة، وضوؤه يتبدّل مع أوقات اليوم · من تصميم Codex · هدية الصداقة",
            day = Pal(false, Color(0xFFF5EFE3), Color(0xFFFFFCF6), Color(0xFFE6EDE6), Color(0x55C9A66B), Color(0xFF173D35), Color(0xFF53665F), Color(0xFF174D43), Color(0xFFFFFFFF), Color(0xFFC9A66B), Color(0xFFB84545)),
            night = Pal(true, Color(0xFF101D35), Color(0xFF18283D), Color(0xFF21354A), Color(0x55C9A66B), Color(0xFFF5EFE3), Color(0xFFB9C8D3), Color(0xFF82C7BB), Color(0xFF101D24), Color(0xFFC9A66B), Color(0xFFFF9992)),
            corners = Corners.ROUND, radius = 22.dp,
            body = Fonts.modern, head = Fonts.messiri,
            nav = NavStyle.BAR, icons = IconStyle.OUTLINED,
            prayers = PrayerLayout.TIMELINE, counter = CounterStyle.RING,
            backdrop = Backdrop.NONE, hero = "riwaq", panel = PanelStyle.GLASS),
        Look(id = "c_diwan", name = "ديوان الخطّاط", about = "«لكلّ حرفٍ مقام» — رقّ عاجي وحبر عنّابي وتذهيب، ومشهدها المرسوم أعلى صفحة الصلاة مع الساعة والصلاة القادمة، وضوؤه يتبدّل مع أوقات اليوم · من تصميم Codex · هدية الصداقة",
            day = Pal(false, Color(0xFFF3EADB), Color(0xFFFCF6E9), Color(0xFFEDE0C8), Color(0x55AF8C48), Color(0xFF402F26), Color(0xFF74604C), Color(0xFF713F36), Color(0xFFFFFFFF), Color(0xFFAF8C48), Color(0xFFB84545)),
            night = Pal(true, Color(0xFF151D30), Color(0xFF1D2940), Color(0xFF29344A), Color(0x55C7A264), Color(0xFFF5EAD4), Color(0xFFC7BDA9), Color(0xFFE4BF83), Color(0xFF101D24), Color(0xFFC7A264), Color(0xFFFF9992)),
            corners = Corners.ROUND, radius = 4.dp,
            body = Fonts.modern, head = Fonts.messiri,
            nav = NavStyle.BAR, icons = IconStyle.OUTLINED,
            prayers = PrayerLayout.LIST, counter = CounterStyle.BEADS,
            backdrop = Backdrop.NONE, hero = "manuscript", panel = PanelStyle.FRAMED),
        Look(id = "c_palms", name = "نخيل الواحة", about = "«مساحةٌ لتتنفّس» — نخيل وجبال وماء فيروزي، ومشهدها المرسوم أعلى صفحة الصلاة مع الساعة والصلاة القادمة، وضوؤه يتبدّل مع أوقات اليوم · من تصميم Codex · هدية الصداقة",
            day = Pal(false, Color(0xFFEBF1E5), Color(0xFFF8FBF1), Color(0xFFDCE9DC), Color(0x55A58645), Color(0xFF244B3D), Color(0xFF536D5A), Color(0xFF2D6851), Color(0xFFFFFFFF), Color(0xFFA58645), Color(0xFFB84545)),
            night = Pal(true, Color(0xFF112B2C), Color(0xFF193839), Color(0xFF224648), Color(0x55D1B477), Color(0xFFE4F1E6), Color(0xFFB3CCC2), Color(0xFF97CEAE), Color(0xFF101D24), Color(0xFFD1B477), Color(0xFFFF9992)),
            corners = Corners.ROUND, radius = 28.dp,
            body = Fonts.modern, head = Fonts.messiri,
            nav = NavStyle.BAR, icons = IconStyle.OUTLINED,
            prayers = PrayerLayout.GARDEN, counter = CounterStyle.FLOWER,
            backdrop = Backdrop.NONE, hero = "oasis", panel = PanelStyle.PEBBLE),
        Look(id = "c_lamps", name = "قناديل المسجد", about = "«نورٌ على سكينة» — نوافذ ملوّنة وقناديل دافئة، ومشهدها المرسوم أعلى صفحة الصلاة مع الساعة والصلاة القادمة، وضوؤه يتبدّل مع أوقات اليوم · من تصميم Codex · هدية الصداقة",
            day = Pal(true, Color(0xFF101D35), Color(0xFF17273F), Color(0xFF22334E), Color(0x55D2B781), Color(0xFFF4EFE6), Color(0xFFB7C5D9), Color(0xFFA6DAD5), Color(0xFF101D24), Color(0xFFD2B781), Color(0xFFFF9992)),
            night = Pal(true, Color(0xFF070D18), Color(0xFF111D30), Color(0xFF1A2940), Color(0x55D2B781), Color(0xFFF4EFE6), Color(0xFFB7C5D9), Color(0xFFA6DAD5), Color(0xFF101D24), Color(0xFFD2B781), Color(0xFFFF9992)),
            corners = Corners.ROUND, radius = 20.dp,
            body = Fonts.modern, head = Fonts.messiri,
            nav = NavStyle.BAR, icons = IconStyle.OUTLINED,
            prayers = PrayerLayout.LANTERNS, counter = CounterStyle.LANTERN,
            backdrop = Backdrop.NONE, hero = "mishkat", panel = PanelStyle.GLASS),
        Look(id = "c_cutouts", name = "قصاصات ورقية", about = "«تفاصيلُ تُصنع برفق» — مسجد وتلال وسحب من الورق، ومشهدها المرسوم أعلى صفحة الصلاة مع الساعة والصلاة القادمة، وضوؤه يتبدّل مع أوقات اليوم · من تصميم Codex · هدية الصداقة",
            day = Pal(false, Color(0xFFF5EEE4), Color(0xFFFFFCF6), Color(0xFFE8E7D9), Color(0x55B08A54), Color(0xFF3B514B), Color(0xFF657268), Color(0xFF547563), Color(0xFFFFFFFF), Color(0xFFB08A54), Color(0xFFB84545)),
            night = Pal(true, Color(0xFF243344), Color(0xFF304258), Color(0xFF3B5067), Color(0x55DFC49A), Color(0xFFF7EEE0), Color(0xFFC5D1D5), Color(0xFFBCDAC6), Color(0xFF101D24), Color(0xFFDFC49A), Color(0xFFFF9992)),
            corners = Corners.ROUND, radius = 16.dp,
            body = Fonts.modern, head = Fonts.messiri,
            nav = NavStyle.BAR, icons = IconStyle.OUTLINED,
            prayers = PrayerLayout.CARDS, counter = CounterStyle.BEADS,
            backdrop = Backdrop.NONE, hero = "paper", panel = PanelStyle.CARD),
        Look(id = "c_spheres", name = "ساعة الأفلاك", about = "«للوقتِ مدار» — أسطرلاب نحاسي وفضة ونجوم، ومشهدها المرسوم أعلى صفحة الصلاة مع الساعة والصلاة القادمة، وضوؤه يتبدّل مع أوقات اليوم · من تصميم Codex · هدية الصداقة",
            day = Pal(true, Color(0xFF182747), Color(0xFF223655), Color(0xFF2C4265), Color(0x55CEAF72), Color(0xFFF1F0EB), Color(0xFFBDCADA), Color(0xFFC4D4E7), Color(0xFF101D24), Color(0xFFCEAF72), Color(0xFFFF9992)),
            night = Pal(true, Color(0xFF0E142A), Color(0xFF17213D), Color(0xFF212D4B), Color(0x55CEAF72), Color(0xFFF1F0EB), Color(0xFFBCC8DC), Color(0xFFC4D4E7), Color(0xFF101D24), Color(0xFFCEAF72), Color(0xFFFF9992)),
            corners = Corners.ROUND, radius = 22.dp,
            body = Fonts.modern, head = Fonts.messiri,
            nav = NavStyle.BAR, icons = IconStyle.OUTLINED,
            prayers = PrayerLayout.SUNDIAL, counter = CounterStyle.CRESCENT,
            backdrop = Backdrop.NONE, hero = "observatory", panel = PanelStyle.GLASS),
        Look(id = "c_home", name = "بيت الفصول", about = "«كلّ فصلٍ بداية» — بيت وأشجار تتبدّل مع الفصول، ومشهدها المرسوم أعلى صفحة الصلاة مع الساعة والصلاة القادمة، وضوؤه يتبدّل مع أوقات اليوم، وأشجاره مع الفصول · من تصميم Codex · هدية الصداقة",
            day = Pal(false, Color(0xFFF6EDE9), Color(0xFFFFF8F3), Color(0xFFEEDDD6), Color(0x55B28A56), Color(0xFF583E40), Color(0xFF7E6260), Color(0xFF83524E), Color(0xFFFFFFFF), Color(0xFFB28A56), Color(0xFFB84545)),
            night = Pal(true, Color(0xFF252536), Color(0xFF343246), Color(0xFF423D51), Color(0x55DCBD8D), Color(0xFFF6EBE5), Color(0xFFD1BCC0), Color(0xFFE4B6AA), Color(0xFF101D24), Color(0xFFDCBD8D), Color(0xFFFF9992)),
            corners = Corners.ROUND, radius = 24.dp,
            body = Fonts.modern, head = Fonts.messiri,
            nav = NavStyle.BAR, icons = IconStyle.OUTLINED,
            prayers = PrayerLayout.GARDEN, counter = CounterStyle.FLOWER,
            backdrop = Backdrop.NONE, hero = "seasons", panel = PanelStyle.PETAL),
        Look(id = "c_palace", name = "قصر الرخام", about = "«فخامةٌ لا ترفع صوتها» — رخام بعروق وزمرد وإطارات معدنية، ومشهدها المرسوم أعلى صفحة الصلاة مع الساعة والصلاة القادمة، وضوؤه يتبدّل مع أوقات اليوم · من تصميم Codex · هدية الصداقة",
            day = Pal(false, Color(0xFFF8F7F4), Color(0xFFFFFFFF), Color(0xFFEAECE6), Color(0x55B29357), Color(0xFF243F36), Color(0xFF66716A), Color(0xFF174D43), Color(0xFFFFFFFF), Color(0xFFB29357), Color(0xFFB84545)),
            night = Pal(true, Color(0xFF111916), Color(0xFF1B2722), Color(0xFF27362E), Color(0x55D1B475), Color(0xFFF7F2E8), Color(0xFFBFCAC2), Color(0xFFB5D4BF), Color(0xFF101D24), Color(0xFFD1B475), Color(0xFFFF9992)),
            corners = Corners.ROUND, radius = 6.dp,
            body = Fonts.modern, head = Fonts.messiri,
            nav = NavStyle.BAR, icons = IconStyle.OUTLINED,
            prayers = PrayerLayout.CARDS, counter = CounterStyle.RING,
            backdrop = Backdrop.NONE, hero = "marble", panel = PanelStyle.FRAMED),
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
    /** More colors after the free one (indices 8, 9, …), so saved choices keep their meaning. */
    val extra = listOf(
        Tint("بنفسجي غامق", 0xFF3F1D6B, 0xFF9C6ADE, 0xFFD9A441),
        Tint("ذهبي", 0xFF8A6512, 0xFFE3C060, 0xFF2F8F7A),
        Tint("وردي", 0xFFB0436A, 0xFFF08FB4, 0xFFD9A441),
    )
    /** The preset swatches in order: the original, the six, then the extra ones. */
    val presets: List<Int> = (0..6).toList() + extra.indices.map { it + 8 }
    fun tint(index: Int): Tint? = when (index) { in 0..6 -> list[index]; in 8 until 8 + extra.size -> extra[index - 8]; else -> null }

    fun mix(a: Color, b: Color, t: Float) = Color(a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t, a.blue + (b.blue - a.blue) * t, 1f)
    fun on(c: Color) = if (c.luminance() > 0.5f) Color(0xFF111111) else Color(0xFFFFFFFF)
    fun contrast(a: Color, b: Color): Float { val x = a.luminance() + .05f; val y = b.luminance() + .05f; return maxOf(x, y) / minOf(x, y) }
    /** The same color, lightened on a dark background (or darkened on a light one) until it reads clearly. */
    fun fit(c: Color, bg: Color): Color {
        val toward = if (bg.luminance() < .4f) Color.White else Color.Black
        var t = 0f; var r = c
        while (contrast(r, bg) < 3f && t < 1f) { t += .08f; r = mix(c, toward, t) }
        return r
    }

    fun accentFor(index: Int, hue: Float, dark: Boolean): Color? = when (index) {
        CUSTOM -> Color.hsv(hue, if (dark) 0.55f else 0.70f, if (dark) 0.88f else 0.55f)
        0 -> null
        else -> tint(index)?.let { Color(if (dark) it.night else it.day) }
    }

    /** [keepBg]: the background and panels keep their own color (black stays black); only cards and lines take a hint. */
    fun apply(base: Pal, index: Int, hue: Float, keepBg: Boolean = false): Pal {
        val accent = accentFor(index, hue, base.dark) ?: return base
        val gold = tint(index)?.takeIf { index != 0 }?.let { Color(it.gold) } ?: Color.hsv((hue + 40f) % 360f, 0.6f, if (base.dark) 0.85f else 0.65f)
        val k = if (base.dark) 1.4f else 1f
        return base.copy(
            bg = if (keepBg) base.bg else mix(base.bg, accent, 0.05f * k), surface = if (keepBg) base.surface else mix(base.surface, accent, 0.03f * k),
            card = mix(base.card, accent, (if (keepBg) 0.06f else 0.10f) * k),
            line = mix(base.line, accent, 0.14f * k), accent = accent, onAccent = on(accent), gold = gold,
        )
    }
}

/** Backgrounds any theme can take, dark and light (0 = the theme's own). */
object Backs {
    data class Back(val name: String, val dark: Boolean, val bg: Long, val surface: Long, val card: Long, val line: Long)
    val list = listOf(
        Back("الأصلية", true, 0, 0, 0, 0),
        Back("أسود نقي", true, 0xFF000000, 0xFF0A0A0A, 0xFF141414, 0xFF262626),
        Back("أسود فانتوم", true, 0xFF0A0A0F, 0xFF121219, 0xFF1A1A24, 0xFF2B2B38),
        Back("أسود شديد", true, 0xFF000000, 0xFF000000, 0xFF080808, 0xFF303030),
        Back("بنفسجي غامق", true, 0xFF120A1F, 0xFF1B1030, 0xFF24163E, 0xFF3A2659),
        Back("كحلي غامق", true, 0xFF0A1020, 0xFF111A30, 0xFF18233F, 0xFF263457),
        Back("أخضر غامق", true, 0xFF07150F, 0xFF0D2018, 0xFF132A20, 0xFF1F4031),
        Back("عنابي غامق", true, 0xFF19080D, 0xFF241017, 0xFF30161F, 0xFF4A2331),
        Back("فحمي", true, 0xFF1C1E21, 0xFF25282C, 0xFF2E3237, 0xFF40454C),
        Back("أبيض", false, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFF2F2F2, 0xFFE0E0E0),
        Back("كريمي", false, 0xFFF6EFDF, 0xFFFFF9EC, 0xFFEEE3CB, 0xFFDCCBA8),
        Back("رمادي فاتح", false, 0xFFEEF0F2, 0xFFFFFFFF, 0xFFE3E6E9, 0xFFD2D6DA),
    )
}

/** Text colors any theme can take (0 = automatic). A color too close to the background is ignored. */
object Inks {
    data class Ink(val name: String, val c: Long)
    val list = listOf(
        Ink("تلقائي", 0), Ink("أبيض", 0xFFFFFFFF), Ink("عاجي", 0xFFF3EAD3), Ink("رمادي فاتح", 0xFFC9C9C9), Ink("ذهبي فاتح", 0xFFF0DCA0),
        Ink("أخضر فاتح", 0xFFCBEBD8), Ink("سماوي فاتح", 0xFFCFE6F5), Ink("بنفسجي فاتح", 0xFFE2D4F7),
        Ink("أسود", 0xFF111111), Ink("بني داكن", 0xFF3A2816), Ink("كحلي", 0xFF0F2440),
    )
}
