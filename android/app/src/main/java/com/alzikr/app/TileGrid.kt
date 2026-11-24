package com.alzikr.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * An even grid of equal tiles: [cols] per row, every row the same, [rows] rows shown and then one full-width button for
 * the rest (and to fold them again). The calm alternative to wrapped tiles of different sizes.
 */
@Composable
fun <T> TileGrid(items: List<T>, cols: Int = 3, rows: Int = 2, startOpen: Boolean = false, noun: String = "", tile: @Composable (T, Modifier) -> Unit) {
    val all = rememberSaveable { mutableStateOf(startOpen) }
    val cap = cols * rows
    val shown = if (all.value || items.size <= cap) items else items.take(cap)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        shown.chunked(cols).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { tile(it, Modifier.weight(1f).fillMaxHeight()) }
                repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (items.size > cap) {
            val shape = L.shape(12.dp)
            Box(Modifier.fillMaxWidth().clip(shape).border(1.dp, P.line, shape).clickable { all.value = !all.value }.padding(vertical = 11.dp), contentAlignment = Alignment.Center) {
                GText(if (all.value) "إظهار أقل ▴" else "عرض ${if (noun.isEmpty()) "الكل" else "كل $noun"} (${Quran.digits(items.size)}) ▾", color = P.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * One picture tile: a square picture, its name on one line, a ✓ when chosen, and when locked a small badge with the level
 * in the corner (never over the drawing).
 */
@Composable
fun PictureTile(name: String, on: Boolean, open: Boolean, level: Int, modifier: Modifier, onClick: () -> Unit, picture: @Composable (Modifier) -> Unit) {
    val shape = L.shape(14.dp)
    Column(modifier.clip(shape).background(if (on) P.accent.copy(alpha = .12f) else P.card).border(if (on) 2.dp else 1.dp, if (on) P.accent else P.line, shape)
        .clickable(onClick = onClick).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            picture(Modifier.fillMaxSize().alpha(if (open) 1f else .32f))
            if (!open) Box(Modifier.align(Alignment.TopStart).clip(L.shape(8.dp)).background(P.bg.copy(alpha = .85f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                GText("🔒 ${Quran.digits(level)}", color = P.ink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            if (on) Box(Modifier.align(Alignment.TopEnd).clip(L.shape(8.dp)).background(P.accent).padding(horizontal = 6.dp, vertical = 1.dp)) {
                GText("✓", color = P.onAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        GText(name, color = if (open) P.ink else P.muted, fontSize = 13.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
    }
}
