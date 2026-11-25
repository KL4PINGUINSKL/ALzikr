package com.alzikr.app

import android.content.Context
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * The kids section is read, not spoken: the phone's synthetic Arabic voice made many mistakes, so it was removed.
 * Only «قصار السور» has sound, recited by a real reciter. [say] stays as a no-op so the pages keep their taps.
 */
object Speak {
    @Suppress("UNUSED_PARAMETER") fun say(c: Context, text: String) = Unit
}

/** What the kids section teaches. Sunni sources, simple words. */
object Kids {
    data class Letter(val c: String, val name: String, val word: String, val emoji: String)
    val letters = listOf(
        Letter("ا", "ألف", "أرنب", "🐇"), Letter("ب", "باء", "بطة", "🦆"), Letter("ت", "تاء", "تفاحة", "🍎"), Letter("ث", "ثاء", "ثعلب", "🦊"),
        Letter("ج", "جيم", "جمل", "🐫"), Letter("ح", "حاء", "حصان", "🐎"), Letter("خ", "خاء", "خروف", "🐑"), Letter("د", "دال", "دبّ", "🐻"),
        Letter("ذ", "ذال", "ذرة", "🌽"), Letter("ر", "راء", "رسالة", "✉️"), Letter("ز", "زاي", "زرافة", "🦒"), Letter("س", "سين", "سمكة", "🐟"),
        Letter("ش", "شين", "شمس", "☀️"), Letter("ص", "صاد", "صاروخ", "🚀"), Letter("ض", "ضاد", "ضفدع", "🐸"), Letter("ط", "طاء", "طائرة", "✈️"),
        Letter("ظ", "ظاء", "ظبي", "🦌"), Letter("ع", "عين", "عنب", "🍇"), Letter("غ", "غين", "غيمة", "☁️"), Letter("ف", "فاء", "فيل", "🐘"),
        Letter("ق", "قاف", "قمر", "🌙"), Letter("ك", "كاف", "كتاب", "📖"), Letter("ل", "لام", "ليمون", "🍋"), Letter("م", "ميم", "موز", "🍌"),
        Letter("ن", "نون", "نحلة", "🐝"), Letter("ه", "هاء", "هدية", "🎁"), Letter("و", "واو", "وردة", "🌹"), Letter("ي", "ياء", "يد", "✋"),
    )
    /** Letters that do not join the next one. */
    private const val NON_JOIN = "ادذرزو"
    fun forms(c: String): List<Pair<String, String>> {
        val j = c !in NON_JOIN
        return listOf("وحده" to c, "في الأول" to (if (j) "${c}ـ" else c), "في الوسط" to (if (j) "ـ${c}ـ" else "ـ${c}"), "في الآخر" to "ـ${c}")
    }
    data class Haraka(val name: String, val mark: String, val about: String)
    val harakat = listOf(
        Haraka("الفتحة", "َ", "خط صغير فوق الحرف، نفتح فمنا: بَ"), Haraka("الكسرة", "ِ", "خط صغير تحت الحرف، ننزل فكّنا: بِ"),
        Haraka("الضمة", "ُ", "واو صغيرة فوق الحرف، نضمّ شفتينا: بُ"), Haraka("السكون", "ْ", "دائرة صغيرة، نقف على الحرف: أَبْ"),
        Haraka("تنوين الفتح", "ً", "فتحتان، نقول «ـَن»: بً"), Haraka("تنوين الكسر", "ٍ", "كسرتان، نقول «ـِن»: بٍ"),
        Haraka("تنوين الضم", "ٌ", "ضمتان، نقول «ـُن»: بٌ"), Haraka("الشدّة", "ّ", "حرفان في حرف واحد، نضغط عليه: بّ"),
    )
    val longVowels = listOf("با" to "مدّ بالألف", "بو" to "مدّ بالواو", "بي" to "مدّ بالياء")

    data class Step(val emoji: String, val title: String, val text: String)
    val wudu = listOf(
        Step("❤️", "النية", "أنوي في قلبي أني أتوضأ للصلاة."),
        Step("🗣️", "بسم الله", "أقول: بسم الله."),
        Step("👐", "الكفّان", "أغسل كفّيّ ثلاث مرات."),
        Step("💦", "المضمضة", "أضع الماء في فمي وأحرّكه ثم أخرجه، ثلاث مرات."),
        Step("👃", "الاستنشاق", "أُدخل الماء في أنفي برفق ثم أُخرجه، ثلاث مرات."),
        Step("😊", "الوجه", "أغسل وجهي كله ثلاث مرات، من الجبهة إلى الذقن ومن الأذن إلى الأذن."),
        Step("💪", "اليدان", "أغسل يدي اليمنى إلى المرفق ثلاثًا، ثم اليسرى ثلاثًا."),
        Step("🙆", "الرأس والأذنان", "أمسح رأسي بيديّ المبلولتين من الأمام إلى الخلف ثم أرجع، ثم أمسح أذنيّ. مرة واحدة."),
        Step("🦶", "الرجلان", "أغسل رجلي اليمنى إلى الكعب ثلاثًا، ثم اليسرى ثلاثًا، ولا أنسى ما بين الأصابع."),
        Step("🤲", "الدعاء", "أقول: «أشهد أن لا إله إلا الله وحده لا شريك له، وأشهد أن محمدًا عبده ورسوله»، فتُفتح لي أبواب الجنة الثمانية (رواه مسلم)."),
    )
    val salah = listOf(
        Step("💧", "أتوضأ", "أتوضأ وألبس ثوبًا نظيفًا، وأقف على مكان طاهر."),
        Step("🧭", "القبلة", "أتّجه إلى القبلة (الكعبة)، وأنوي في قلبي الصلاة التي سأصليها."),
        Step("🙌", "التكبير", "أرفع يديّ وأقول: «الله أكبر»، ثم أضع يدي اليمنى على اليسرى على صدري."),
        Step("📖", "القراءة", "أقرأ سورة الفاتحة، ثم سورة قصيرة أحفظها (في الركعتين الأوليين)."),
        Step("🙇", "الركوع", "أقول «الله أكبر» وأنحني، ويداي على ركبتيّ وظهري مستقيم، وأقول: «سبحان ربي العظيم» ثلاثًا."),
        Step("🧍", "الرفع", "أرفع وأقول: «سمع الله لمن حمده»، فإذا وقفت قلت: «ربنا ولك الحمد»."),
        Step("🧎", "السجود", "أقول «الله أكبر» وأسجد على جبهتي وأنفي وكفّيّ وركبتيّ وأصابع رجليّ، وأقول: «سبحان ربي الأعلى» ثلاثًا. وأدعو الله بما أحب."),
        Step("🧘", "الجلوس", "أقول «الله أكبر» وأجلس، وأقول: «رب اغفر لي، رب اغفر لي»."),
        Step("🧎", "السجدة الثانية", "أسجد مرة ثانية مثل الأولى. وهكذا انتهت ركعة كاملة!"),
        Step("☝️", "التشهد", "بعد كل ركعتين أجلس وأقرأ التشهد: «التحيات لله والصلوات والطيبات…»، وفي آخر الصلاة أزيد الصلاة على النبي ﷺ."),
        Step("👋", "السلام", "ألتفت إلى اليمين وأقول: «السلام عليكم ورحمة الله»، ثم إلى اليسار مثلها. انتهت الصلاة!"),
    )
    val rakat = listOf("الفجر" to 2, "الظهر" to 4, "العصر" to 4, "المغرب" to 3, "العشاء" to 4)

    data class Card(val emoji: String, val title: String, val text: String, val src: String = "")
    val islam = listOf(
        Card("☝️", "الشهادتان", "أن أشهد أن لا إله إلا الله، وأن محمدًا رسول الله."),
        Card("🕌", "الصلاة", "خمس صلوات في اليوم والليلة."),
        Card("💰", "الزكاة", "يعطي الغنيّ جزءًا من ماله للفقراء."),
        Card("🌙", "صوم رمضان", "نصوم شهر رمضان من الفجر إلى المغرب."),
        Card("🕋", "الحج", "زيارة بيت الله الحرام في مكة لمن استطاع."),
    )
    val iman = listOf(
        Card("💚", "الإيمان بالله", "الله واحد لا شريك له، خلقنا ويرزقنا ويحبّنا."),
        Card("✨", "الملائكة", "خلقهم الله من نور، يطيعونه ولا يعصونه، مثل جبريل عليه السلام."),
        Card("📚", "الكتب", "القرآن والتوراة والإنجيل والزبور وصحف إبراهيم، وآخرها القرآن."),
        Card("🌟", "الرسل", "أرسلهم الله ليعلّمونا، أولهم نوح وآخرهم محمد ﷺ."),
        Card("⏳", "اليوم الآخر", "يوم القيامة، يجزي الله فيه كل إنسان بعمله."),
        Card("🎁", "القدر", "كل شيء يحدث بعلم الله وتقديره، خيره وشره."),
    )
    val adhkar = listOf(
        Card("🌅", "عندما أستيقظ", "«الحمد لله الذي أحيانا بعد ما أماتنا وإليه النشور»", "البخاري"),
        Card("🛏️", "عندما أنام", "«باسمك اللهم أموت وأحيا»", "البخاري"),
        Card("🍽️", "قبل الطعام", "«بسم الله»، وإن نسيت: «بسم الله أوله وآخره»", "أبو داود والترمذي، وصححه الألباني"),
        Card("😋", "بعد الطعام", "«الحمد لله الذي أطعمني هذا ورزقنيه من غير حول مني ولا قوة»", "أبو داود والترمذي، وحسّنه الألباني"),
        Card("🚪", "دخول الخلاء", "أدخل برجلي اليسرى وأقول: «اللهم إني أعوذ بك من الخبث والخبائث»", "البخاري ومسلم"),
        Card("✨", "الخروج من الخلاء", "أخرج برجلي اليمنى وأقول: «غفرانك»", "أبو داود والترمذي، وصححه الألباني"),
        Card("🏠", "الخروج من البيت", "«بسم الله، توكلت على الله، ولا حول ولا قوة إلا بالله»", "أبو داود والترمذي، وصححه الألباني"),
        Card("🕌", "دخول المسجد", "أدخل برجلي اليمنى وأقول: «اللهم افتح لي أبواب رحمتك»", "مسلم"),
        Card("🚶", "الخروج من المسجد", "أخرج برجلي اليسرى وأقول: «اللهم إني أسألك من فضلك»", "مسلم"),
        Card("🤧", "العطاس", "أقول: «الحمد لله»، فيقول من سمعني: «يرحمك الله»، فأرد: «يهديكم الله ويصلح بالكم»", "البخاري"),
        Card("👕", "لبس الثوب", "«الحمد لله الذي كساني هذا الثوب ورزقنيه من غير حول مني ولا قوة»", "أبو داود والترمذي، وحسّنه الألباني"),
        Card("🚗", "ركوب السيارة", "«سبحان الذي سخّر لنا هذا وما كنا له مقرنين، وإنا إلى ربنا لمنقلبون»", "الزخرف 13–14، ورواه مسلم"),
    )
    val manners = listOf(
        Card("👋", "أُلقي السلام", "أقول «السلام عليكم» لمن عرفت ومن لم أعرف.", "«أفشوا السلام بينكم» — مسلم"),
        Card("🍽️", "آداب الطعام", "أسمّي الله، وآكل بيميني، ومما يليني.", "«يا غلام، سمِّ الله، وكُل بيمينك، وكُل مما يليك» — البخاري ومسلم"),
        Card("👨‍👩‍👧", "أبرّ والديّ", "أطيعهما وأكلّمهما بلطف وأساعدهما.", "﴿وبالوالدين إحسانًا﴾ — الإسراء 23"),
        Card("✅", "أصدق دائمًا", "الصدق يهدي إلى الخير والجنة.", "«عليكم بالصدق» — البخاري ومسلم"),
        Card("😊", "أبتسم", "الابتسامة في وجه أخي صدقة.", "«تبسّمك في وجه أخيك لك صدقة» — الترمذي، وصححه الألباني"),
        Card("🐱", "أرحم الحيوان", "غفر الله لرجل سقى كلبًا عطشان.", "البخاري ومسلم"),
        Card("🪥", "أحب النظافة", "أستعمل السواك وأنظّف أسناني.", "«السواك مطهرة للفم مرضاة للرب» — النسائي، وصححه الألباني"),
        Card("🚪", "أستأذن", "أطرق الباب وأستأذن قبل أن أدخل غرفة غيري.", "النور 27"),
        Card("🤐", "أحفظ لساني", "أقول الكلام الطيب أو أسكت.", "«من كان يؤمن بالله واليوم الآخر فليقل خيرًا أو ليصمت» — البخاري ومسلم"),
        Card("🤝", "أساعد غيري", "الله في عون العبد ما كان العبد في عون أخيه.", "مسلم"),
    )
    /** Short surahs a child learns first. */
    val surahs = listOf(1, 114, 113, 112, 111, 110, 109, 108, 107, 106, 105, 104, 103, 102, 101, 100, 99, 97, 95, 94, 93)
    fun memorized(s: Int) = "$s" in (Store.prefs.getStringSet("kids_hifz", emptySet()) ?: emptySet())
    fun setMemorized(s: Int, on: Boolean) {
        val set = (Store.prefs.getStringSet("kids_hifz", emptySet()) ?: emptySet()).toMutableSet()
        if (on) set += "$s" else set -= "$s"
        Store.prefs.edit().putStringSet("kids_hifz", set).apply(); Store.bump()
    }
    /** Sun or moon letters: the letter after «ال». */
    val alWords = listOf("الشمس", "القمر", "الرحمن", "الحمد", "النور", "الكتاب", "الصلاة", "العلم", "الدين", "البيت", "الناس", "الجنة", "السماء", "الأرض", "التين", "الفجر", "الليل", "اليوم", "المسجد", "الذكر", "الطير", "الولد", "الزيتون", "الخير")
    fun isSun(w: String) = w.getOrNull(2)?.let { it in "تثدذرزسشصضطظلن" } ?: false

    data class Topic(val id: String, val emoji: String, val title: String, val color: Long)
    val topics = listOf(
        Topic("letters", "🔤", "الحروف العربية", 0xFFFFD6A5), Topic("harakat", "✏️", "الحركات", 0xFFFDFFB6),
        Topic("lettergame", "🎮", "لعبة الحروف", 0xFFCAFFBF), Topic("tajweed", "🎙️", "التجويد للصغار", 0xFF9BF6FF),
        Topic("surahs", "📖", "قصار السور", 0xFFA0C4FF), Topic("wudu", "💧", "كيف أتوضأ", 0xFFBDB2FF),
        Topic("salah", "🕌", "كيف أصلي", 0xFFFFC6FF), Topic("pillars", "🕋", "أركان الإسلام والإيمان", 0xFFFFADAD),
        Topic("adhkar", "🤲", "أذكاري اليومية", 0xFFB9FBC0), Topic("manners", "😊", "آداب المسلم الصغير", 0xFFFBF8CC),
        Topic("quiz", "⭐", "تعلّم مع صديقي", 0xFFF1C0E8), Topic("stories", "📚", "قصص الأنبياء", 0xFFCFBAF0),
    )
}

/** «قسم الأطفال»: the first tab in the children's theme, and a page in More for everyone else. */
@Composable
fun KidsHome() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    var page by rememberSaveable { mutableStateOf<String?>(null) }
    page?.let { p ->
        androidx.activity.compose.BackHandler { page = null }
        Column(Modifier.fillMaxSize()) {
            val t = Kids.topics.firstOrNull { it.id == p }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("‹ القائمة", color = P.accent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clip(L.shape(10.dp)).clickable { page = null }.padding(8.dp))
                Spacer(Modifier.weight(1f))
                Text((t?.emoji ?: "") + " " + (t?.title ?: ""), color = P.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.weight(1f)) {
                when (p) {
                    "letters" -> LettersPage()
                    "harakat" -> HarakatPage()
                    "lettergame" -> LetterGame()
                    "tajweed" -> KidsTajweed()
                    "surahs" -> KidsSurahs()
                    "wudu" -> StepsPage(Kids.wudu, "أحسنت! أنت الآن على وضوء 💧", "wudu")
                    "salah" -> StepsPage(Kids.salah, "ما شاء الله! تعلّمت الصلاة 🕌", "salah")
                    "pillars" -> CardsPage(listOf("أركان الإسلام الخمسة" to Kids.islam, "أركان الإيمان الستة" to Kids.iman),
                        "«بُني الإسلام على خمس» — البخاري ومسلم. وأركان الإيمان من حديث جبريل — مسلم")
                    "adhkar" -> CardsPage(listOf("أذكاري كل يوم" to Kids.adhkar), "")
                    "manners" -> CardsPage(listOf("أخلاق المسلم الصغير" to Kids.manners), "")
                    "quiz" -> KidsPage()
                    else -> {}
                }
            }
        }
        return
    }
    val sp = Pet.species
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().clip(L.shape(24.dp)).background(P.accent.copy(alpha = .12f)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (sp != null) PetLottie(sp, Modifier.size(84.dp), place = com.alzikr.app.pets.Place.KIDS) else Text("🌟", fontSize = 54.sp)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text("أهلًا يا بطل! 👋", color = P.ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("ماذا نتعلّم اليوم؟", color = P.muted, fontSize = 15.sp)
                Text("⭐ ${Pet.stars} نجمة", color = P.gold, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Kids.topics.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { t ->
                    Column(Modifier.weight(1f).heightIn(min = 128.dp).clip(L.shape(26.dp)).background(Color(t.color)).clickable {
                        if (t.id == "stories") { Nav.library = 1; Nav.go("library") } else page = t.id
                    }.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(t.emoji, fontSize = 42.sp)
                        Text(t.title, color = Color(0xFF1D2B5A), fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2, modifier = Modifier.padding(top = 6.dp))
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (!Looks.byId(Data.look).kids) Text("🎨 اجعل التطبيق كله للأطفال: اختر ثيم «الأطفال» من الثيمات", color = P.accent, fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.card).clickable { Data.look = "kids"; Nav.tab = "kids"; Nav.history.clear() }.padding(12.dp))
    }
}

@Composable
private fun BigButton(text: String, modifier: Modifier = Modifier, filled: Boolean = true, onClick: () -> Unit) {
    Text(text, color = if (filled) P.onAccent else P.accent, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
        modifier = modifier.clip(L.shape(20.dp)).background(if (filled) P.accent else P.accent.copy(alpha = .12f)).clickable(onClick = onClick).padding(vertical = 14.dp, horizontal = 18.dp))
}

@Composable
private fun LettersPage() {
    val ctx = LocalContext.current
    var sel by rememberSaveable { mutableStateOf<Int?>(null) }
    sel?.let { i ->
        val l = Kids.letters[i]
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(l.c, color = P.accent, fontSize = 120.sp, fontFamily = Fonts.naskh, modifier = Modifier.padding(horizontal = 30.dp))
            Text("حرف ${l.name}", color = P.ink, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(6.dp))
            Row(Modifier.fillMaxWidth().clip(L.shape(24.dp)).background(P.card).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(l.emoji, fontSize = 56.sp)
                Text(l.word, color = P.ink, fontSize = 34.sp, fontFamily = Fonts.naskh, modifier = Modifier.weight(1f).padding(start = 12.dp))
            }
            Text("أشكال الحرف في الكلمة", color = P.gold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Kids.forms(l.c).forEach { (n, f) ->
                    Column(Modifier.weight(1f).clip(L.shape(18.dp)).background(P.surface).border(1.dp, P.line, L.shape(18.dp)).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(f, color = P.ink, fontSize = 36.sp, fontFamily = Fonts.naskh)
                        Text(n, color = P.muted, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            }
            Text("مع الحركات", color = P.gold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("َ", "ِ", "ُ", "ْ").forEach { m ->
                    val s = if (l.c == "ا") "أ$m" else l.c + m
                    Text(s, color = P.ink, fontSize = 38.sp, fontFamily = Fonts.naskh, textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).clip(L.shape(18.dp)).background(P.card).padding(vertical = 6.dp))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (i > 0) BigButton("› ${Kids.letters[i - 1].c}", Modifier.weight(1f), filled = false) { sel = i - 1 }
                BigButton("الحروف", Modifier.weight(1f), filled = false) { sel = null }
                if (i < Kids.letters.lastIndex) BigButton("${Kids.letters[i + 1].c} ‹", Modifier.weight(1f)) { sel = i + 1 }
            }
        }
        return
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("اضغط على حرف لترى اسمه وكلمة تبدأ به.", color = P.muted, fontSize = 15.sp)
        Kids.letters.chunked(4).forEachIndexed { r, row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEachIndexed { k, l ->
                    val colors = Kids.topics.map { it.color }
                    Column(Modifier.weight(1f).clip(L.shape(22.dp)).background(Color(colors[(r * 4 + k) % colors.size])).clickable { sel = r * 4 + k }.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(l.c, color = Color(0xFF1D2B5A), fontSize = 40.sp, fontFamily = Fonts.naskh)
                        Text(l.emoji, fontSize = 18.sp)
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun HarakatPage() {
    val ctx = LocalContext.current
    var letter by rememberSaveable { mutableIntStateOf(1) }
    val c = Kids.letters[letter].c.let { if (it == "ا") "أ" else it }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("الحركات تخبرنا كيف ننطق الحرف. اختر حرفًا وانظر كيف يتغيّر مع كل حركة:", color = P.muted, fontSize = 15.sp)
        Wrap(gap = 6.dp) {
            Kids.letters.forEachIndexed { i, l -> Choice(l.c, letter == i) { letter = i } }
        }
        Kids.harakat.forEach { h ->
            val s = c + h.mark
            Row(Modifier.fillMaxWidth().clip(L.shape(20.dp)).background(P.card).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(s, color = P.accent, fontSize = 44.sp, fontFamily = Fonts.naskh, modifier = Modifier.padding(horizontal = 12.dp))
                Column(Modifier.weight(1f)) {
                    Text(h.name, color = P.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(h.about, color = P.muted, fontSize = 14.sp)
                }
            }
        }
        Text("حروف المدّ: نمدّ الصوت قليلًا", color = P.gold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("َا" to "مدّ بالألف", "ُو" to "مدّ بالواو", "ِي" to "مدّ بالياء").forEach { (m, n) ->
                val s = c + m
                Column(Modifier.weight(1f).clip(L.shape(18.dp)).background(P.surface).border(1.dp, P.line, L.shape(18.dp)).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(s, color = P.ink, fontSize = 36.sp, fontFamily = Fonts.naskh)
                    Text(n, color = P.muted, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun LetterGame() {
    val ctx = LocalContext.current
    var round by remember { mutableIntStateOf(0) }
    val q = remember(round) { val l = Kids.letters.random(); l to (Kids.letters.filter { it != l }.shuffled().take(3) + l).shuffled() }
    var picked by remember(round) { mutableStateOf<String?>(null) }
    var streak by remember { mutableIntStateOf(0) }
    val (l, opts) = q
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("بأي حرف تبدأ هذه الكلمة؟", color = P.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Column(Modifier.clip(L.shape(30.dp)).background(P.card).padding(horizontal = 30.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(l.emoji, fontSize = 80.sp)
            Text(if (picked == null) "ـ" + l.word.drop(1) else l.word, color = P.ink, fontSize = 30.sp, fontFamily = Fonts.naskh)
        }
        opts.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { o ->
                    val right = o.c == l.c
                    val bg = when { picked == null -> P.surface; right -> P.accent; picked == o.c -> P.danger.copy(alpha = .25f); else -> P.surface }
                    Text(o.c, color = if (picked != null && right) P.onAccent else P.ink, fontSize = 48.sp, fontFamily = Fonts.naskh, textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).clip(L.shape(24.dp)).background(bg).border(2.dp, P.line, L.shape(24.dp)).clickable(enabled = picked == null) {
                            picked = o.c
                            if (right) { Pet.stars = Pet.stars + 1; streak++; Haptics.tap(ctx, true); if (streak % 5 == 0) Pet.cheer("رائع! $streak إجابات صحيحة") } else { streak = 0 }
                        }.padding(vertical = 10.dp))
                }
            }
        }
        if (picked != null) {
            Text(if (picked == l.c) "⭐ أحسنت!" else "الحرف الصحيح: ${l.c} (${l.name})", color = if (picked == l.c) P.gold else P.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            BigButton("التالي ›", Modifier.fillMaxWidth()) { round++ }
        }
        Text("⭐ ${Pet.stars}", color = P.gold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun KidsTajweed() {
    var game by rememberSaveable { mutableStateOf(false) }
    if (game) { SunMoonGame { game = false }; return }
    data class K(val emoji: String, val title: String, val text: String, val ex: List<Tajweed.Ex>)
    val cards = listOf(
        K("📖", "ما هو التجويد؟", "أن نقرأ القرآن كما قرأه النبي ﷺ: نُخرج كل حرف من مكانه، ونعطيه حقّه. قال الله: ﴿ورتّل القرآن ترتيلًا﴾.", listOf(Tajweed.Ex("ورتل القرآن ترتيلا", 73, 4))),
        K("👃", "الغنّة", "صوت جميل يخرج من الأنف مع النون والميم المشدّدتين. أمسك أنفك وقل «إنّ» فستشعر بها! نمدّها بمقدار حركتين.", listOf(Tajweed.Ex("إنا أعطيناك", 108, 1), Tajweed.Ex("ثم", 2, 28))),
        K("🏀", "القلقلة", "خمسة حروف تجمعها كلمة «قُطْبُ جَدٍّ». إذا سكنت ارتدّ صوتها مثل الكرة: «الفلقْ»، «أحدْ».", listOf(Tajweed.Ex("الفلق", 113, 1), Tajweed.Ex("أحد", 112, 1), Tajweed.Ex("يقطعون", 2, 27))),
        K("🐍", "المدّ", "نمدّ الصوت بالألف والواو والياء. حركتان في «قال»، وأطول إذا جاءت بعدها همزة مثل «جاء»، وأطول وأطول (6 حركات) في «الضّالّين».", listOf(Tajweed.Ex("قال", 2, 30), Tajweed.Ex("جاء", 110, 1), Tajweed.Ex("الضالين", 1, 7))),
        K("☀️", "اللام الشمسية والقمرية", "في «ال»: إذا اختفت اللام وشُدّد الحرف بعدها فهي شمسية مثل «الشّمس». وإذا ظهرت اللام فهي قمرية مثل «القَمَر». العب لعبة الشمس والقمر تحت!", listOf(Tajweed.Ex("والشمس", 91, 1), Tajweed.Ex("القمر", 54, 1))),
        K("🟢", "النون الساكنة: تظهر", "إذا جاء بعدها حرف من حروف الحلق (ء هـ ع ح غ خ) نقولها واضحة: «مَنْ آمن». هذا هو الإظهار.", listOf(Tajweed.Ex("من آمن", 2, 62))),
        K("🫥", "النون الساكنة: تختبئ", "إذا جاء بعدها حرف من «يرملون» دخلت فيه واختفت: «مَن يقول». هذا هو الإدغام.", listOf(Tajweed.Ex("من يقول", 2, 8))),
        K("🔄", "النون الساكنة: تتحوّل", "إذا جاء بعدها باء صارت ميمًا: «مِن بَعد» نقرؤها «مِم بعد». هذا هو الإقلاب.", listOf(Tajweed.Ex("من بعد", 2, 27))),
        K("🌫️", "النون الساكنة: بين بين", "مع باقي الحروف لا تظهر ولا تختفي تمامًا، مع غنّة: «مِن قبل». هذا هو الإخفاء.", listOf(Tajweed.Ex("من قبل", 2, 25))),
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BigButton("☀️ لعبة الشمس والقمر 🌙", Modifier.fillMaxWidth()) { game = true }
        cards.forEach { k ->
            Column(Modifier.fillMaxWidth().clip(L.shape(24.dp)).background(P.surface).border(1.dp, P.line, L.shape(24.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(k.emoji, fontSize = 34.sp)
                    Text(k.title, color = P.ink, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp))
                }
                Text(k.text, color = P.ink, fontSize = 16.sp, lineHeight = 26.sp)
                k.ex.forEach { ExampleRow(it) }
            }
        }
        Text("للكبار ولمن يريد المزيد: «تعلّم التجويد» في المزيد ›", color = P.accent, fontSize = 14.sp,
            modifier = Modifier.clip(L.shape(10.dp)).clickable { Nav.morePage = "tajweed"; Nav.go("more") }.padding(8.dp))
    }
}

@Composable
private fun SunMoonGame(back: () -> Unit) {
    androidx.activity.compose.BackHandler(onBack = back)
    val ctx = LocalContext.current
    var round by remember { mutableIntStateOf(0) }
    val w = remember(round) { Kids.alWords.random() }
    var picked by remember(round) { mutableStateOf<Boolean?>(null) }
    val sun = Kids.isSun(w)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("‹ رجوع", color = P.accent, fontSize = 15.sp, modifier = Modifier.align(Alignment.Start).clip(L.shape(10.dp)).clickable(onClick = back).padding(6.dp))
        Text("هل اللام في هذه الكلمة شمسية أم قمرية؟", color = P.ink, fontSize = 19.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(w, color = P.ink, fontSize = 56.sp, fontFamily = Fonts.naskh, modifier = Modifier.clip(L.shape(26.dp)).background(P.card).padding(horizontal = 26.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(true to "☀️\nشمسية", false to "🌙\nقمرية").forEach { (isSun, label) ->
                val right = isSun == sun
                val bg = when { picked == null -> P.surface; right -> P.accent; picked == isSun -> P.danger.copy(alpha = .25f); else -> P.surface }
                Text(label, color = if (picked != null && right) P.onAccent else P.ink, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).clip(L.shape(26.dp)).background(bg).border(2.dp, P.line, L.shape(26.dp)).clickable(enabled = picked == null) {
                        picked = isSun; if (right) { Pet.stars = Pet.stars + 1; Haptics.tap(ctx, true) }
                    }.padding(vertical = 18.dp))
            }
        }
        if (picked != null) {
            Text((if (picked == sun) "⭐ أحسنت! " else "") + if (sun) "شمسية: اللام لا تُنطق، والحرف بعدها مشدّد (${w[2]})." else "قمرية: اللام تُنطق واضحة قبل (${w[2]}).",
                color = P.ink, fontSize = 17.sp, textAlign = TextAlign.Center)
            BigButton("التالي ›", Modifier.fillMaxWidth()) { round++ }
        }
    }
}

@Composable
private fun KidsSurahs() {
    val ctx = LocalContext.current
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    androidx.compose.runtime.LaunchedEffect(Unit) { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { Quran.load(ctx) } }
    if (!Quran.ready) return
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val n = Kids.surahs.count { Kids.memorized(it) }
        Text("حفظت ${Quran.digits(n)} من ${Quran.digits(Kids.surahs.size)} سورة ⭐", color = P.gold, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text("اضغط 🔊 لتسمع السورة وردّد معها، و📖 لتقرأها، وإذا حفظتها اضغط ⭐.", color = P.muted, fontSize = 14.sp)
        Kids.surahs.forEach { s ->
            val sur = Quran.surahs[s - 1]
            val done = Kids.memorized(s)
            val playing = Recitation.now?.first == s
            Row(Modifier.fillMaxWidth().clip(L.shape(22.dp)).background(if (done) P.accent.copy(alpha = .14f) else P.surface).border(1.dp, P.line, L.shape(22.dp)).padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("سورة ${sur.name}", color = P.ink, fontSize = 20.sp, fontFamily = Fonts.naskh, fontWeight = FontWeight.Bold)
                    Text("${Quran.digits(sur.count)} آيات", color = P.muted, fontSize = 13.sp)
                }
                Text(if (playing) "⏹" else "🔊", fontSize = 24.sp, modifier = Modifier.clip(L.shape(12.dp)).clickable {
                    if (playing) Recitation.stop() else Recitation.play(ctx, s, 1, only = Recitation.Range.SURAH)
                }.padding(10.dp))
                Text("📖", fontSize = 24.sp, modifier = Modifier.clip(L.shape(12.dp)).clickable { Nav.overlayQuran = s to 1 }.padding(10.dp))
                Text(if (done) "⭐" else "☆", color = P.gold, fontSize = 26.sp, modifier = Modifier.clip(L.shape(12.dp)).clickable {
                    Kids.setMemorized(s, !done); if (!done) { Pet.stars = Pet.stars + 3; Pet.cheer("ما شاء الله! حفظت سورة ${sur.name}") }
                }.padding(10.dp))
            }
        }
    }
}

@Composable
private fun StepsPage(steps: List<Kids.Step>, finish: String, id: String) {
    val ctx = LocalContext.current
    var i by rememberSaveable(id) { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            steps.indices.forEach { k -> Box(Modifier.weight(1f).height(8.dp).clip(L.shape(4.dp)).background(if (k <= i) P.accent else P.line)) }
        }
        if (i >= steps.size) {
            Text("🎉", fontSize = 96.sp)
            Text(finish, color = P.ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            if (id == "salah") {
                Text("كم ركعة في كل صلاة؟", color = P.gold, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Kids.rakat.forEach { (n, r) ->
                        Column(Modifier.weight(1f).clip(L.shape(18.dp)).background(P.card).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(Quran.digits(r), color = P.accent, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                            Text(n, color = P.ink, fontSize = 13.sp)
                        }
                    }
                }
            } else Text("ينتقض الوضوء إذا دخلت الحمام، أو نمت نومًا عميقًا، فأتوضأ من جديد.", color = P.muted, fontSize = 15.sp, textAlign = TextAlign.Center)
            BigButton("من البداية", Modifier.fillMaxWidth(), filled = false) { i = 0 }
            return@Column
        }
        val s = steps[i]
        Text("${Quran.digits(i + 1)} / ${Quran.digits(steps.size)}", color = P.muted, fontSize = 15.sp)
        Text(s.emoji, fontSize = 96.sp)
        Text(s.title, color = P.accent, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(s.text, color = P.ink, fontSize = 20.sp, lineHeight = 32.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (i > 0) BigButton("› السابق", Modifier.weight(1f), filled = false) { i-- }
            BigButton(if (i == steps.lastIndex) "انتهيت 🎉" else "التالي ‹", Modifier.weight(1f)) {
                if (i == steps.lastIndex) { Pet.stars = Pet.stars + 2; Pet.cheer(finish) }
                i++
            }
        }
    }
}

@Composable
private fun CardsPage(groups: List<Pair<String, List<Kids.Card>>>, footer: String) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        groups.forEach { (title, cards) ->
            Text(title, color = P.gold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            cards.forEachIndexed { k, c ->
                val colors = Kids.topics.map { it.color }
                Row(Modifier.fillMaxWidth().clip(L.shape(24.dp)).background(Color(colors[k % colors.size])).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(c.emoji, fontSize = 40.sp)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(c.title, color = Color(0xFF1D2B5A), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(c.text, color = Color(0xFF1D2B5A), fontSize = 17.sp, lineHeight = 27.sp, fontFamily = if (c.text.startsWith("«")) Fonts.naskh else null)
                        if (c.src.isNotBlank()) Text(c.src, color = Color(0xFF4A5680), fontSize = 12.sp)
                    }
                }
            }
        }
        if (footer.isNotBlank()) Text(footer, color = P.muted, fontSize = 12.sp)
    }
}
