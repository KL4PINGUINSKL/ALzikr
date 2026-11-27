package com.alzikr.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * «أحاديث يُشكل فهمها»: every hadith with an extended sharh (SharhPlus). One with a HadeethEnc page
 * opens that page (which shows the note and the extended sharh); the others open [MushkilPage].
 */
@Composable
fun MushkilList() {
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    open?.let { k -> Subpage(SharhPlus.byKey(k)?.title ?: "", { open = null }) { MushkilPage(k) }; return }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Hint("mushkil", "⚠", "أحاديث صحيحة قد يُشكل فهمها أو تُثار حولها الشبهات. لكل حديث تنبيه مختصر، ثم شرح موسّع بأقوال أهل العلم ومصادره.")
        SharhPlus.list.forEach { e ->
            Row(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.card).clickable {
                val id = e.ids.firstOrNull()
                if (id != null) Nav.overlaySharh = id else open = e.key
            }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(e.title, color = P.ink, fontSize = 16.sp, fontWeight = FontWeight.Medium, fontFamily = Fonts.naskh)
                    Sub(e.sources.first(), size = 11.sp)
                }
                Text("‹", color = P.muted, fontSize = 18.sp)
            }
        }
    }
}

/** An extended sharh with no HadeethEnc page: the hadith, the note, the sections, the sources and «للاستزادة». */
@Composable
fun MushkilPage(key: String) {
    val ctx = LocalContext.current
    val e = SharhPlus.byKey(key) ?: return
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Panel(color = P.card) {
            Text(e.hadith, color = P.ink, fontSize = (18 * Data.textScale).sp, lineHeight = (32 * Data.textScale).sp, fontFamily = Fonts.naskh)
        }
        Panel(color = P.card) {
            GText("⚠ يحتاج إلى بيان", color = P.gold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(e.note, color = P.ink, fontSize = (15 * Data.textScale).sp, lineHeight = (26 * Data.textScale).sp, fontFamily = Fonts.naskh, modifier = Modifier.padding(top = 6.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val full = e.hadith + "\n\n" + SharhPlus.text(e)
            Tile("⧉", "نسخ", Modifier.weight(1f)) {
                val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText(e.title, full))
                android.widget.Toast.makeText(ctx, "نُسخ", android.widget.Toast.LENGTH_SHORT).show()
            }
            Tile("↗", "مشاركة", Modifier.weight(1f)) {
                runCatching { ctx.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, full), "مشاركة")) }
            }
        }
        Title("الشرح الموسع")
        e.sections.forEach { (t, b) ->
            Text(t, color = P.accent, fontSize = (16 * Data.textScale).sp, fontWeight = FontWeight.Bold, fontFamily = Fonts.naskh)
            Text(b, color = P.ink, fontSize = (15 * Data.textScale).sp, lineHeight = (27 * Data.textScale).sp, fontFamily = Fonts.naskh)
        }
        Title("المصادر")
        e.sources.forEach { Sub("• $it", size = 12.sp) }
        Title("للاستزادة")
        Sub("ابحث عن شرح هذا الحديث وفتاوى أهل العلم فيه:", size = 12.sp)
        val q = SharhPlus.phrase(e.title, "")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SharhPlus.sites.forEach { site -> Tile("↗", site.label, Modifier.weight(1f)) { SharhPlus.open(ctx, site.url(q)) } }
        }
        HadithInQuranpedia(e.hadith)
    }
}
