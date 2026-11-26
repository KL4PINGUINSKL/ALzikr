package com.alzikr.app

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * «الذكر البسيط»: two sections only, the prayer and the Quran, with a clean header (the date and the settings).
 * The friend, «يومي», the library, the points and celebrations step aside; nothing is deleted, and choosing the full
 * app again brings everything back as it was.
 */
object Simple {
    /** The prayer page in the simple mode: the next prayer, today's times, the adhkar of the moment, a few tiles. */
    val parts = listOf("next", "forbid", "times", "moment", "simple", "place")

    fun set(c: Context, on: Boolean) {
        Data.simple = on
        if (on) PetOverlay.stop(c) else PetOverlay.start(c)
        Nav.morePage = null
        Nav.go("prayer")
        Store.bump()
    }
}

/** The few things the simple prayer page needs beyond the times. */
@Composable
fun SimpleTiles(openSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Tile("🧭", "القبلة", Modifier.weight(1f)) { Nav.morePage = "qibla"; Nav.go("more") }
        Tile("📅", "مواقيت الشهر", Modifier.weight(1f)) { Nav.morePage = "month"; Nav.go("more") }
        Tile("⚙", "الإعدادات", Modifier.weight(1f)) { openSettings() }
    }
}

/** At the top of the settings: the full app or the simple one. */
@Composable
fun SimpleModePanel() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val ctx = LocalContext.current
    Panel(pad = 14.dp) {
        Title("شكل التطبيق")
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeCard("✨", "الذكر الكامل", "كل المميزات", !Data.simple, Modifier.weight(1f)) { if (Data.simple) Simple.set(ctx, false) }
            ModeCard("🕊", "الذكر البسيط", "الصلاة والقرآن فقط", Data.simple, Modifier.weight(1f)) { if (!Data.simple) Simple.set(ctx, true) }
        }
        Sub(if (Data.simple) "قسمان فقط: الصلاة (المواقيت والأذان والقبلة وأذكار الوقت) والقرآن (المصحف والتفسير والتلاوة والختمة). لا يُحذف شيء: اختر «الكامل» فيعود كل شيء كما كان."
            else "«البسيط» يُبقي الصلاة والقرآن فقط، بلا صديق ولا مهام ولا نقاط، لمن يحب تطبيقًا خفيفًا هادئًا. لا يُحذف شيء من بياناتك.",
            Modifier.padding(top = 8.dp), size = 12.sp)
    }
}

@Composable
fun ModeCard(icon: String, title: String, sub: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = L.shape(16.dp)
    Column(modifier.clip(shape).background(if (on) P.accent.copy(alpha = .14f) else P.card).border(if (on) 2.dp else 1.dp, if (on) P.accent else P.line, shape)
        .clickable(onClick = onClick).padding(14.dp)) {
        Text(icon, fontSize = 26.sp)
        Text(title, color = P.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
        Text(sub, color = P.muted, fontSize = 12.sp)
        if (on) Text("✓ المختار", color = P.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
    }
}
