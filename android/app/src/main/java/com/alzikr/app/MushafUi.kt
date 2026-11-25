package com.alzikr.app

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The reader's mushaf settings: edition (with tajweed colours), diacritics, and size, with a live sample. */
@Composable
fun MushafSettings() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    @Suppress("UNUSED_VARIABLE") val mv = Mushafs.version
    Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Quran.at(1, 2)?.let { sample ->
            Text(Mushafs.verse(ctx, sample), color = P.ink, fontSize = Quran.fontSize.sp, fontFamily = Fonts.quran, lineHeight = (Quran.fontSize * 1.8f).sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        Sub("الحجم", size = 12.sp)
        androidx.compose.material3.Slider(Quran.fontSize.toFloat(), { Quran.fontSize = it.toInt() }, valueRange = 18f..44f)
        SettingRow("التشكيل", "إظهار الحركات أو إخفاؤها") { Switch(Mushafs.tashkeel, { Mushafs.tashkeel = it }) }
        Sub("المصحف", size = 12.sp)
        Mushafs.editions.forEach { e ->
            if (e.id == "warsh") Sub("القراءات (الروايات)", Modifier.padding(top = 6.dp), size = 12.sp)
            val on = Mushafs.current == e.id
            val have = Mushafs.has(ctx, e.id)
            Row(Modifier.fillMaxWidth().clip(L.shape(12.dp)).background(if (on) P.accent.copy(alpha = .14f) else P.card).clickable {
                if (have) Mushafs.current = e.id
                else if (Mushafs.busy == null) scope.launch {
                    Mushafs.busy = e.id
                    val ok = withContext(Dispatchers.IO) { Mushafs.fetch(ctx, e.id) }
                    Mushafs.busy = null
                    if (ok) Mushafs.current = e.id else Toast.makeText(ctx, "تعذّر التنزيل، تحقق من الإنترنت", Toast.LENGTH_LONG).show()
                }
            }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    GText(e.name, color = P.ink, fontSize = 15.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
                    Sub(e.about + if (!have) " · يُنزَّل مرة واحدة" else "", size = 11.sp)
                }
                when { Mushafs.busy == e.id -> CircularProgressIndicator(Modifier.size(18.dp), color = P.accent, strokeWidth = 2.dp); on -> GText("✓", color = P.accent) }
            }
        }
        if (Mushafs.current == "tajweed") {
            Sub("ألوان التجويد", Modifier.padding(top = 6.dp), size = 12.sp)
            Wrap(gap = 6.dp, lines = 10) {
                Mushafs.tajweedColors.values.distinctBy { it.second }.forEach { (c, n) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).clip(L.shape(3.dp)).background(c)); GText(" $n", color = P.ink, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
