package com.alzikr.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** How the mushaf opens from the library, and the reader's defaults: all in one place (Library → Quran). */
object MushafView {
    /** "verses" (verses with tafsir, hifz, notes) | "scroll" (continuous pages) | "pages" (turning pages like a mushaf). */
    var openAs: String
        get() = Store.prefs.getString("mushaf_open_as", "verses") ?: "verses"
        set(v) { Store.prefs.edit().putString("mushaf_open_as", v).apply(); if (v != "verses") Reading.pages = v == "pages"; Store.bump() }
    var studyDefault: Boolean
        get() = Store.prefs.getBoolean("mushaf_study_default", false)
        set(v) { Store.prefs.edit().putBoolean("mushaf_study_default", v).apply(); Store.bump() }
    var hifzDefault: Boolean
        get() = Store.prefs.getBoolean("mushaf_hifz_default", false)
        set(v) { Store.prefs.edit().putBoolean("mushaf_hifz_default", v).apply(); Store.bump() }
    /** Opens [s]:[a] in the chosen view. */
    fun open(s: Int, a: Int) { if (openAs == "verses") Nav.openQuran(s, a) else { Reading.pages = openAs == "pages"; Nav.reading = s to a } }
}

/** «طريقة عرض المصحف»: a card that opens to every choice of the mushaf. */
@Composable
fun MushafViewCard() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    if (Reading.scope != "quran") Reading.scope = "quran"   // the paper below is the mushaf's own
    var open by rememberSaveable { mutableStateOf(false) }
    Panel(pad = 0.dp) {
        Row(Modifier.fillMaxWidth().clickable { open = !open }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                GText("⚙ طريقة عرض المصحف", color = P.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Sub(when (MushafView.openAs) { "scroll" -> "تمرير متصل"; "pages" -> "صفحات كالمصحف"; else -> "آيات وتفسير" } + " · " +
                    (Mushafs.editions.firstOrNull { it.id == Mushafs.current }?.name ?: "") + (if (!Mushafs.tashkeel) " · بلا تشكيل" else ""), size = 12.sp)
            }
            GText(if (open) "▴" else "▾", color = P.muted, fontSize = 16.sp)
        }
        if (open) Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
            Sub("تفتح السور والأجزاء بـ:", size = 12.sp)
            Box(Modifier.padding(top = 6.dp)) {
                TabGrid(listOf(Triple("verses", "📖", "آيات وتفسير"), Triple("scroll", "📜", "تمرير متصل"), Triple("pages", "📄", "تقليب صفحات")), MushafView.openAs) { MushafView.openAs = it }
            }
            Sub(when (MushafView.openAs) {
                "scroll" -> "صفحات المصحف متتابعة تمرّرها للأسفل، بلون ورق مريح."
                "pages" -> "صفحة صفحة كالمصحف الورقي، تقلّبها يمينًا ويسارًا."
                else -> "آية آية: اضغط على أي آية لتفسيرها ومعاني كلماتها والاستماع لها وتدوين ملاحظتك."
            }, Modifier.padding(top = 6.dp), size = 12.sp)
            if (MushafView.openAs == "verses") {
                SettingRow("التفسير تحت الآيات", "يظهر تفسيرك المختار تحت كل آية") { Switch(MushafView.studyDefault, { MushafView.studyDefault = it }) }
                SettingRow("وضع الحفظ", "تظهر أول كلمة من كل آية، اضغط لتتأكد") { Switch(MushafView.hifzDefault, { MushafView.hifzDefault = it }) }
            } else {
                if (MushafView.openAs == "pages") SettingRow("الصفحة كاملة على الشاشة", "يُضبط الخط ليظهر كل الصفحة؛ ومن أراد خطًا أكبر كبّره من «Aa»") {
                    Switch(Reading.fitPage, { Reading.fitPage = it })
                }
                Sub("لون الورق", Modifier.padding(top = 8.dp, bottom = 6.dp), size = 12.sp)
                Wrap(gap = 8.dp) {
                    Reading.Paper.entries.forEach { pp ->
                        val on = Reading.paper == pp
                        Row(Modifier.clip(L.shape(10.dp)).background(Color(pp.bg)).border(if (on) 2.dp else 1.dp, if (on) P.accent else P.line, L.shape(10.dp))
                            .clickable { Reading.paper = pp }.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(pp.label, color = Color(pp.ink), fontSize = 13.sp)
                        }
                    }
                }
            }
            Box(Modifier.padding(top = 10.dp)) { MushafSettings() }
        }
    }
}
