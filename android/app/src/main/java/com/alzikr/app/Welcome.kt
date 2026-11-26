package com.alzikr.app

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/** The first launch: a few pages that show what the app holds, then the friend (optional). */
object Welcome {
    /** Seen, or an existing user (a city, a friend or the friend's intro already set) who never needs it. */
    val seen: Boolean get() = Store.prefs.getBoolean("welcomed", false) || Store.hasLocation || Pet.species != null || Pet.introSeen
    var forced by mutableStateOf(false)
    fun done() { Store.prefs.edit().putBoolean("welcomed", true).apply(); forced = false; Store.bump() }
    /** From «المساعدة»: show it again. */
    fun showAgain() { forced = true }

    data class Page(val icon: String, val title: String, val text: String, val points: List<String> = emptyList())
    val pages = listOf(
        Page("🌙", "أهلًا بك في ALzikr", "رفيقك في الصلاة والقرآن والذكر وتنظيم يومك. بلا إعلانات، وأغلبه يعمل دون إنترنت.",
            listOf("اسحب يمينًا أو يسارًا لتتعرّف على أهم ما فيه")),
        Page("🕌", "الصلاة", "أوقات الصلاة لمدينتك بدقة، وكل ما يعينك عليها.",
            listOf("الأذان بصوت تختاره، وتنبيه قبل الوقت", "منبه للصلوات يرنّ حتى توقفه", "القبلة، ومواقيت الشهر", "سجل صلواتك وقضاء ما فاتك")),
        Page("📖", "القرآن", "المصحف بصفحاته كما في المطبوع، أو آيات متتابعة.",
            listOf("تفاسير متعددة تحت كل آية", "التلاوة بأصوات كبار القرّاء مع التكرار", "ختمتك وورد يومي، وحفظ ومراجعة", "قصص القرآن")),
        Page("📚", "المكتبة", "كتب الحديث بشروحها، وآلاف الكتب الإسلامية.",
            listOf("صحيح البخاري ومسلم والسنن وغيرها", "شرح الحديث وأحاديث مختارة", "الأحكام الفقهية ومواسم الطاعات", "التسلسل التاريخي: من آدم عليه السلام إلى ١٩٠٠")),
        Page("📿", "الأذكار", "أذكار الصباح والمساء وكل الأحوال، مع العدّ.",
            listOf("السبحة", "أورادك الخاصة", "تذكير بالأذكار في وقتها")),
        Page("☑", "يومي", "نظّم يومك حول أوقات الصلاة.",
            listOf("مهام بتذكير، تكتبها كما تقولها: «اتصل بأمي غدًا بعد العصر»", "عادات وختمة ونقاط يومك", "جلسات تركيز تُسكت الهاتف", "ملاحظات تعلّقها في التطبيق أو على الشاشة")),
        Page("✨", "مفاتيح صغيرة", "أشياء تسهّل استعمالك:",
            listOf("🔍 البحث الشامل من أعلى الشاشة: في كل التطبيق مرة واحدة", "اسحب يمينًا أو يسارًا للتنقل بين الأقسام", "🎨 الثيمات من «المزيد»، و«راحة العين» للقراءة الليلية", "اضغط التاريخ مطولًا لتغيير شكله")),
    )
}

@Composable
fun WelcomePages() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    if (Welcome.seen && !Welcome.forced) return
    // A new install starts on «زمرّد» over pure black (the full and the simple app alike); a later choice is kept.
    if (!Welcome.forced) LaunchedEffect(Unit) {
        if (!Store.prefs.contains("look_back_emerald")) Store.prefs.edit().putInt("look_back_emerald", 1).apply()
        Store.bump()
    }
    val count = Welcome.pages.size + 2   // then: the full or the simple app, then the friend
    val modeAt = Welcome.pages.size
    val pager = rememberPagerState { count }
    val scope = rememberCoroutineScope()
    val again = Welcome.forced
    androidx.activity.compose.BackHandler { if (pager.currentPage > 0) scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } else if (again) Welcome.done() }
    Box(Modifier.fillMaxSize().background(P.bg).statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("ALzikr", color = P.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = L.head, modifier = Modifier.weight(1f))
                if (pager.currentPage < modeAt) Text("تخطٍّ", color = P.muted, fontSize = 15.sp,
                    modifier = Modifier.clip(L.shape(10.dp)).clickable { scope.launch { pager.animateScrollToPage(modeAt) } }.padding(10.dp))
            }
            HorizontalPager(pager, Modifier.weight(1f)) { i ->
                when {
                    i < modeAt -> WelcomePage(Welcome.pages[i])
                    i == modeAt -> WelcomeMode { simple ->
                        if (simple) { Pet.introSeen = true; Welcome.done() }   // no friend in the simple app
                        else scope.launch { pager.animateScrollToPage(modeAt + 1) }
                    }
                    else -> WelcomeFriend(again)
                }
            }
            // The dots, then «التالي».
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center) {
                repeat(count) { k ->
                    val w by animateDpAsState(if (k == pager.currentPage) 22.dp else 8.dp, label = "dot")
                    Box(Modifier.padding(3.dp).height(8.dp).width(w).clip(L.shape(4.dp)).background(if (k == pager.currentPage) P.accent else P.line))
                }
            }
            if (pager.currentPage < modeAt)
                Action("التالي", Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp)) { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }
            else Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun WelcomePage(p: Welcome.Page) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(110.dp).clip(L.shape(55.dp)).background(P.accent.copy(alpha = .12f)), contentAlignment = Alignment.Center) { Text(p.icon, fontSize = 52.sp) }
        Spacer(Modifier.height(22.dp))
        Text(p.title, color = P.ink, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = L.head, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(p.text, color = P.muted, fontSize = 16.sp, textAlign = TextAlign.Center, lineHeight = 24.sp)
        if (p.points.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            Column(Modifier.fillMaxWidth().clip(L.shape(16.dp)).background(P.surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                p.points.forEach { s ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(Modifier.padding(top = 8.dp).size(7.dp).clip(L.shape(4.dp)).background(P.accent))
                        Text(s, color = P.ink, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(start = 10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeFriend(again: Boolean) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(110.dp).clip(L.shape(55.dp)).background(P.accent.copy(alpha = .12f)), contentAlignment = Alignment.Center) { Text("🐾", fontSize = 52.sp) }
        Spacer(Modifier.height(22.dp))
        Text("صديق صغير، إن شئت", color = P.ink, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = L.head, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text("يعيش في أعلى التطبيق، يذكّرك بالصلاة والأذكار بكلمة لطيفة، ويفرح بإنجازك. وللصغار يتعلّم معهم.\nهو اختياري تمامًا: تخفيه متى شئت من صفحته، أو تبدأ بدونه.",
            color = P.muted, fontSize = 16.sp, textAlign = TextAlign.Center, lineHeight = 24.sp)
        Spacer(Modifier.height(26.dp))
        if (again) Action("تم", Modifier.fillMaxWidth()) { Welcome.done() }
        else {
            Action("🐾 اختر صديقًا", Modifier.fillMaxWidth()) { Welcome.done() }   // the friend's own sheet opens next
            Spacer(Modifier.height(10.dp))
            Action("ابدأ بدون صديق", Modifier.fillMaxWidth(), filled = false) { Pet.introSeen = true; Welcome.done() }
            Text("تجده لاحقًا في «المزيد» إن غيّرت رأيك", color = P.muted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

/** «كيف تحب التطبيق؟»: everything, or the prayer and the Quran only. */
@Composable
private fun WelcomeMode(pick: (Boolean) -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("كيف تحب التطبيق؟", color = P.ink, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = L.head, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("تغيّره متى شئت من الإعدادات، ولا يُحذف شيء.", color = P.muted, fontSize = 15.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(22.dp))
        ModeCard("✨", "الذكر الكامل", "الصلاة والقرآن والمكتبة والأذكار ويومي وصديقك، وكل ما ذُكر", !Data.simple, Modifier.fillMaxWidth()) { Simple.set(ctx, false); pick(false) }
        Spacer(Modifier.height(12.dp))
        ModeCard("🕊", "الذكر البسيط", "قسمان فقط: الصلاة والقرآن. خفيف وهادئ، لمن يريد الأساس دون غيره", Data.simple, Modifier.fillMaxWidth()) { Simple.set(ctx, true); pick(true) }
    }
}
