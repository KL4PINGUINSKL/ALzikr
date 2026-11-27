package com.alzikr.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Arabic dictionaries (More › تعلّم): type a word, open it in a dictionary in the browser
 * (Al-Maany's modern and classical dictionaries, the classical lexicons by root, the Doha
 * historical dictionary, Wiktionary), and see offline where the word comes in the Quran.
 */
object Dict {
    class Source(val icon: String, val name: String, val about: String, val url: (String) -> String)

    val sources = listOf(
        Source("📖", "معجم المعاني", "المعجم الوسيط ومعجم اللغة العربية المعاصرة وغيرهما في صفحة واحدة") { w -> "https://www.almaany.com/ar/dict/ar-ar/" + Uri.encode(w) + "/" },
        Source("📚", "المعاجم القديمة", "لسان العرب وتاج العروس والقاموس المحيط ومختار الصحاح ومعجم لين؛ ابحث بالجذر (كتب لا كتاب)") { w -> "https://arabiclexicon.hawramani.com/?s=" + Uri.encode(w) },
        Source("⏳", "معجم الدوحة التاريخي", "تاريخ الكلمة واستعمالها منذ أقدم النصوص؛ ابحث بالجذر") { w -> "https://www.dohadictionary.org/dictionary/" + Uri.encode(w) },
        Source("🔎", "ويكاموس", "المعنى والتصريف والجمع") { w -> "https://ar.wiktionary.org/w/index.php?search=" + Uri.encode(w) },
    )

    /** The word without marks or tatweel, as typed in a dictionary. */
    fun clean(w: String) = w.replace(Regex("[\\u064B-\\u065F\\u0670\\u0640]"), "").trim()

    fun recent(): List<String> = Store.prefs.getString("dict_recent", "")!!.split('\n').filter { it.isNotBlank() }
    fun remember(w: String) {
        val list = (listOf(w) + recent().filter { it != w }).take(15)
        Store.prefs.edit().putString("dict_recent", list.joinToString("\n")).apply()
    }
    fun clearRecent() { Store.prefs.edit().remove("dict_recent").apply(); Store.bump() }
}

@Composable
fun DictionaryPage() {
    val ctx = LocalContext.current
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    var word by rememberSaveable { mutableStateOf("") }
    var looked by rememberSaveable { mutableStateOf("") }
    fun look(w: String) { val c = Dict.clean(w); if (c.isEmpty()) return; word = c; looked = c; Dict.remember(c); Store.bump() }
    fun open(url: String) { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    val inQuran = remember(looked) { if (looked.length < 2) emptyList() else Quran.search(looked, 60) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Hint("dict", "📖", "اكتب كلمة ثم اختر قاموسًا، فيفتح معناها في المتصفح. وتحتها مواضع الكلمة في القرآن بلا إنترنت. المعاجم القديمة ومعجم الدوحة تبحث بالجذر: «كتب» لا «الكتاب».")
        OutlinedTextField(word, { word = it }, placeholder = { Text("اكتب الكلمة أو جذرها") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { look(word) }))
        Action("ابحث", Modifier.fillMaxWidth()) { look(word) }
        if (looked.isNotEmpty()) {
            Title("معنى «$looked» في")
            Dict.sources.forEach { s ->
                Row(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.card).clickable { open(s.url(looked)) }.padding(14.dp)) {
                    GText(s.icon, fontSize = 20.sp)
                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        Text(s.name, color = P.ink, fontSize = 15.sp)
                        Sub(s.about, size = 11.sp)
                    }
                    GText("↗", color = P.accent, fontSize = 16.sp)
                }
            }
            Title(if (inQuran.isEmpty()) "لم ترد بهذا اللفظ في القرآن" else "في القرآن (${Quran.digits(inQuran.size)}${if (inQuran.size >= 60) "+" else ""})")
            inQuran.take(30).forEach { a ->
                Column(Modifier.fillMaxWidth().clip(L.shape(12.dp)).background(P.surface).clickable { Nav.overlayQuran = a.s to a.a }.padding(12.dp)) {
                    Text(a.text, color = P.ink, fontSize = (17 * Data.textScale).sp, lineHeight = (30 * Data.textScale).sp, fontFamily = Fonts.naskh)
                    Sub("${Quran.surahs.getOrNull(a.s - 1)?.name ?: ""} ${Quran.digits(a.a)}", size = 11.sp)
                }
            }
        }
        val recent = Dict.recent()
        if (recent.isNotEmpty()) {
            Row { Title("آخر ما بحثت عنه", Modifier.weight(1f)); Text("مسح", color = P.muted, fontSize = 12.sp, modifier = Modifier.clickable { Dict.clearRecent() }.padding(6.dp)) }
            Wrap { recent.forEach { w -> Choice(w, w == looked) { look(w) } } }
        }
    }
}
