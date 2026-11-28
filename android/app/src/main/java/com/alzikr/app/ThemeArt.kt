package com.alzikr.app

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb

/**
 * «على ذوق الثيم» (user, 2026-11-28): a frame and a background for the friend's card, its bubble and its widget that
 * follow the app's chosen theme — its colours, and the ornaments of its living background.
 */
object ThemeArt {
    const val FRAME = "theme"
    const val SCENE = "theme"
    const val WSCENE = "w_theme"

    /** The theme's colours now (the app sets them; outside it, worked out from the saved choice). */
    fun pal(c: android.content.Context? = null): Pal = Reading.themePal ?: run {
        val night = c?.let { (it.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES } ?: true
        runCatching { Looks.byId(Data.look).pal(night, Data.theme) }.getOrDefault(Night)
    }
    fun backdrop(): Backdrop = runCatching { Looks.byId(Data.look).backdrop }.getOrDefault(Backdrop.NONE)

    /** The existing ornaments that suit each living background (drawn frames hang; the others sit on the band). */
    fun motif(b: Backdrop = backdrop()): String = when (b) {
        Backdrop.BLOSSOM -> "hearts"
        Backdrop.PETALS -> "sakura"
        Backdrop.MEADOW, Backdrop.VINES -> "butterflies"
        Backdrop.SPACE, Backdrop.SATURN, Backdrop.ORBS -> "galaxy"
        Backdrop.STARS, Backdrop.OBSERVATORY, Backdrop.SKYLINE -> "crescents"
        Backdrop.SKY -> "night"
        Backdrop.AQUARIUM, Backdrop.CORAL, Backdrop.BUBBLES, Backdrop.RAIN -> "soap"
        Backdrop.GEMS -> "gold"
        Backdrop.PENGUINS, Backdrop.SNOW -> "snowflakes"
        Backdrop.LANTERN, Backdrop.RIWAQ -> "lanterns"
        Backdrop.FIREFLIES -> "candles"
        Backdrop.GIRIH, Backdrop.ILLUMINATED, Backdrop.DAMASK, Backdrop.MANUSCRIPT, Backdrop.MARBLE, Backdrop.ROSETTE, Backdrop.INK, Backdrop.PAPER -> "pattern"
        Backdrop.STAINED -> "zellige"
        Backdrop.DUNES, Backdrop.MESA, Backdrop.CACTI, Backdrop.OASIS -> "desert"
        Backdrop.AURORA -> "aurora"
        Backdrop.RAYS -> "dawn"
        Backdrop.BUNTING -> "bunting"
        Backdrop.SEASONS -> "autumn"
        Backdrop.NONE -> "pattern"
    }

    /** The frame's entry in the frames list (its colours are the theme's, so it is made fresh each time). */
    fun frame(): Progress.Frame {
        val p = pal()
        return Progress.Frame(FRAME, "على ذوق الثيم", 0, null, Tints.mix(p.surface, p.accent, .12f).toArgbLong(), Tints.mix(p.card, p.accent, .22f).toArgbLong(), !p.dark)
    }
    fun hangs(): Boolean = Decor.isDrawn(motif())
}

private fun Color.toArgbLong(): Long = (toArgb().toLong() and 0xFFFFFFFFL)

/** The theme's frame: a band from its accent to its gold, a fine inner line, and its background's ornaments. */
fun DrawScope.drawThemeFrame(t: Float, radius: Float) {
    val p = ThemeArt.pal()
    val m = ThemeArt.motif()
    if (Decor.isDrawn(m)) drawDecorFrame(m, t, p.accent, p.gold, radius)
    else {
        val band = 3f * density
        drawRoundRect(Brush.verticalGradient(listOf(p.accent, p.gold)), Offset(band / 2, band / 2), Size(size.width - band, size.height - band), CornerRadius(radius), style = Stroke(band))
        drawFrameExtras(m, t, radius)
    }
    val i = 7f * density
    drawRoundRect(p.gold.copy(alpha = .45f), Offset(i, i), Size(size.width - 2 * i, size.height - 2 * i), CornerRadius((radius - i).coerceAtLeast(2f)), style = Stroke(.8f * density))
}

/** The theme's background: its colours, and its living background drawn small (or an eight-point pattern when it has none). */
fun DrawScope.drawThemeScene(t: Float, c: android.content.Context? = null) {
    val p = ThemeArt.pal(c)
    drawRect(Brush.verticalGradient(listOf(p.bg, Tints.mix(p.bg, p.accent, if (p.dark) .22f else .14f))))
    val b = ThemeArt.backdrop()
    if (b != Backdrop.NONE) drawBackdrop(b, p, t)
    else {
        val s = 34f * density
        var y = s / 2; var row = 0
        while (y < size.height + s) {
            var x = if (row % 2 == 0) s / 2 else s
            while (x < size.width + s) { star8(x, y, s * .28f, p.accent.copy(alpha = .16f)); x += s }
            y += s * .87f; row++
        }
        drawCircle(Brush.radialGradient(listOf(p.gold.copy(alpha = .22f), Color.Transparent), Offset(size.width * .5f, size.height * .3f), size.minDimension * .7f),
            size.minDimension * .7f, Offset(size.width * .5f, size.height * .3f))
    }
}
