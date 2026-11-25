package com.alzikr.app

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime

/**
 * The painted scenes of the themes Codex proposed («اقتراح ثيمات», rendered by design/themes/render_codex_scenes.cjs):
 * each in the seven lights of the day by the real prayer times (and the seasons scene in its season). A theme with a
 * [Look.hero] shows its scene at the top of the prayer page, with the clock and the next prayer over it.
 */
object Scenes {
    private val cache = object : LinkedHashMap<String, ImageBitmap>(4, .75f, true) {
        override fun removeEldestEntry(e: MutableMap.MutableEntry<String, ImageBitmap>?) = size > 3
    }
    fun light(): String = when (Riwaq.moment()) {
        Riwaq.Moment.FAJR -> "fajr"; Riwaq.Moment.SUNRISE -> "sunrise"; Riwaq.Moment.DHUHR -> "dhuhr"; Riwaq.Moment.ASR -> "asr"
        Riwaq.Moment.MAGHRIB -> "maghrib"; Riwaq.Moment.ISHA -> "isha"; Riwaq.Moment.MIDNIGHT -> "midnight"
    }
    fun night() = Riwaq.moment().let { it == Riwaq.Moment.ISHA || it == Riwaq.Moment.MIDNIGHT }
    private fun season(): String = when (YearSeason.now()) {
        YearSeason.S.SPRING -> "spring"; YearSeason.S.SUMMER -> "summer"; YearSeason.S.AUTUMN -> "autumn"; YearSeason.S.WINTER -> "winter"
        else -> when (java.time.LocalDate.now().monthValue) { 3, 4, 5 -> "spring"; 6, 7, 8 -> "summer"; 9, 10, 11 -> "autumn"; else -> "winter" }
    }
    fun path(scene: String) = "themes/$scene-${light()}${if (scene == "seasons") "-" + season() else ""}.webp"
    fun bitmap(c: Context, path: String): ImageBitmap? = cache[path] ?: runCatching {
        c.assets.open(path).use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
    }.getOrNull()?.also { cache[path] = it }
}

/** The next prayer over the theme's scene: the clock in a medallion, the prayer and the time left. */
@Composable
fun HeroNext(scene: String, next: Prayer, nextAt: String, left: String, onClick: () -> Unit) {
    val ctx = LocalContext.current
    @Suppress("UNUSED_VARIABLE") val tick = Sky.tick
    val path = Scenes.path(scene)
    val bmp = remember(path) { Scenes.bitmap(ctx, path) }
    val shape = RoundedCornerShape(24.dp)
    Box(Modifier.fillMaxWidth().height(270.dp).clip(shape).background(P.card).border(1.dp, P.gold.copy(alpha = .5f), shape).clickable(onClick = onClick)) {
        if (bmp != null) Image(bmp, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
        // Night darkens the garden too (the paintings change only their sky), and a soft shade under the words.
        if (Scenes.night()) Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xAA050A1A)))))
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color(0x22000000), Color.Transparent, Color(0x99000000)))))
        val now = LocalTime.now()
        Column(Modifier.align(Alignment.Center).padding(bottom = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(112.dp).clip(CircleShape).background(Color(0xCC10241F)).border(2.dp, Color(0xFFC9A66B), CircleShape), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(Alerts.timeText(now.hour * 60 + now.minute).substringBefore(' '), color = Color(0xFFF5EFE3), fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text(window().current?.ar ?: "", color = Color(0xFFC9A66B), fontSize = 12.sp)
                }
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("الصلاة القادمة · ${next.ar}  $nextAt", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.padding(top = 6.dp).clip(RoundedCornerShape(50)).background(Color(0x66000000)).border(1.dp, Color(0x88C9A66B), RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 4.dp)) {
                Text("بعد $left", color = Color(0xFFF5EFE3), fontSize = 14.sp)
            }
        }
    }
}
