package com.alzikr.app

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * «اقتباس» (user, 2026-11-28): fill a new dua (or a new wird) from what is already in the app — the reported adhkar
 * and duas, or a verse of the Quran — instead of typing it. [pick] gets the text and its usual count (1 for a verse).
 * [duas] true: the duas first (Quran duas, Sunnah duas); false: the adhkar first (morning, evening, after prayer…).
 */
@Composable
fun QuotePicker(duas: Boolean, close: () -> Unit, pick: (String, Int) -> Unit) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { Quran.load(ctx) } }
    var tab by rememberSaveable { mutableStateOf(0) }       // 0 the reported, 1 the Quran
    var find by rememberSaveable { mutableStateOf("") }
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    val first = if (duas) listOf("quran_duas", "sunnah_duas", "distress") else listOf("morning", "evening", "prayer", "sleep")
    val cols = remember(duas) { Content.collections.sortedBy { c -> first.indexOf(c.id).let { if (it < 0) 99 else it } } }
    AppSheet(onDismissRequest = close) {
        // Above the keyboard (user, 2026-11-28: the search field hid behind it); the list gives up the room.
        // One fixed height whatever the tab or the results (user, 2026-11-28: it jumped between the tabs, and with the
        // keyboard it rose to the top of the screen): the keyboard takes its room from the list, not from the screen.
        val h = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * .68f).dp
        Column(Modifier.fillMaxWidth().height(h).imePadding().padding(horizontal = 16.dp).padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Title(if (duas) "📋 اقتبس دعاءً" else "📋 اقتبس وردًا")
            Sub("اختر نصًّا فيُنسخ إلى ${if (duas) "دعائك" else "وردك"}، وتعدّله قبل الحفظ إن شئت.", size = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Choice(if (duas) "🤲 الأدعية المأثورة" else "📿 الأذكار المأثورة", tab == 0) { tab = 0; open = null }
                Choice("📖 آية من القرآن", tab == 1) { tab = 1 }
            }
            OutlinedTextField(find, { find = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(if (tab == 0) "ابحث في المأثور…" else "ابحث بكلمة من الآية… (مثل: ربنا آتنا)") })
            val m = remember(find) { HadithSearch.matcher(find) }
            LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (tab == 0) {
                    val col = cols.firstOrNull { it.id == open }
                    when {
                        find.isNotBlank() -> items(cols.flatMap { c -> c.items.filter { m(it.text) }.map { c to it } }.take(80)) { (c, it) -> QuoteRow(it.text, c.title, it.count) { pick(it.text, it.count) } }
                        col == null -> items(cols) { c ->
                            Row(Modifier.fillMaxWidth().clip(L.shape(12.dp)).border(1.dp, P.line, L.shape(12.dp)).clickable { open = c.id }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(c.title, color = P.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                    Sub("${c.subtitle} · ${Quran.digits(c.items.size)}", size = 12.sp)
                                }
                                Text("‹", color = P.muted, fontSize = 18.sp)
                            }
                        }
                        else -> {
                            item { Text("→ ${col.title}", color = P.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clip(L.shape(8.dp)).clickable { open = null }.padding(6.dp)) }
                            items(col.items) { QuoteRow(it.text, it.note, it.count) { pick(it.text, it.count) } }
                        }
                    }
                } else when {
                    !Quran.ready -> item { Sub("جارٍ تحميل المصحف…") }
                    find.isBlank() -> item { Sub("اكتب كلمة أو أكثر من الآية لتظهر هنا، ثم اضغطها.", size = 13.sp) }
                    else -> {
                        val hits = Quran.ayahs.filter { m(it.plain) }.take(60)
                        if (hits.isEmpty()) item { Sub("لا آية فيها هذه الكلمات.", size = 13.sp) }
                        items(hits) { a ->
                            val ref = "${Quran.surahs[a.s - 1].name}: ${Quran.digits(a.a)}"
                            QuoteRow(a.text, ref, 1) { pick("﴿${a.text}﴾ [$ref]", 1) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuoteRow(text: String, note: String, count: Int, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(L.shape(12.dp)).border(1.dp, P.line, L.shape(12.dp)).clickable(onClick = onClick).padding(12.dp)) {
        Text(text, color = P.ink, fontSize = 15.sp, lineHeight = 24.sp, maxLines = 6)
        val tail = listOfNotNull(note.takeIf { it.isNotBlank() }, if (count > 1) "× ${Quran.digits(count)}" else null).joinToString(" · ")
        if (tail.isNotEmpty()) Sub(tail, color = P.gold, size = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

/** Under the text field of a new dua / wird: «اقتبس» (the picker) and «الصق» (what was copied, e.g. from a verse). */
@Composable
fun QuoteButtons(duas: Boolean, paste: (String) -> Unit, quote: () -> Unit) {
    val ctx = LocalContext.current
    Wrap(Modifier.padding(top = 6.dp), gap = 6.dp) {
        Choice(if (duas) "📋 اقتبس دعاءً مأثورًا أو آية" else "📋 اقتبس من المأثور أو آية", false) { quote() }
        Choice("📎 الصق", false) {
            val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val t = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(ctx)?.toString()?.trim().orEmpty()
            if (t.isNotEmpty()) paste(t) else android.widget.Toast.makeText(ctx, "لا شيء منسوخ", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
