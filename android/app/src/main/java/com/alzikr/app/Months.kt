package com.alzikr.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

/** Why the months are called what they are: the Hijri months, and the Gregorian ones with their Levantine names. */
object Months {
    data class Month(val n: Int, val name: String, val also: String, val why: String)

    val hijri = listOf(
        Month(1, "المحرّم", "من الأشهر الحرم", "سُمّي بذلك لتحريم القتال فيه، فهو أحد الأشهر الحرم الأربعة. وهو أول السنة الهجرية منذ جعله عمر بن الخطاب رضي الله عنه أولها حين وضع التاريخ الهجري."),
        Month(2, "صفر", "", "قيل: لإصفار البيوت وخلوّها من أهلها، لخروجهم فيه للسفر والقتال بعد قعودهم في المحرّم؛ يقال: صَفِر المكان إذا خلا."),
        Month(3, "ربيع الأول", "", "سُمّي بذلك لأن تسميته وافقت زمن الربيع، فكانوا يرتبعون فيه، أي يقيمون في المراعي مع الخصب والكلأ."),
        Month(4, "ربيع الآخر", "ويقال: ربيع الثاني", "تبعًا لربيع الأول، إذ وافق هو أيضًا زمن الربيع عند التسمية."),
        Month(5, "جمادى الأولى", "", "سُمّي بذلك لأن تسميته وافقت زمن الشتاء، فكان الماء يجمد فيه من شدة البرد."),
        Month(6, "جمادى الآخرة", "ويقال: جمادى الثانية", "تبعًا لجمادى الأولى، إذ وافق هو أيضًا زمن البرد وجمود الماء."),
        Month(7, "رجب", "من الأشهر الحرم، ويقال: رجب مُضَر", "من الترجيب وهو التعظيم؛ كانت العرب تعظّمه وتترك فيه القتال، وهو أحد الأشهر الحرم. ونُسب إلى قبيلة مضر لأنها كانت أشد تعظيمًا له."),
        Month(8, "شعبان", "", "قيل: لتشعّب القبائل وتفرّقها فيه لطلب الماء والمراعي أو للغارات، بعد قعودهم عن القتال في رجب."),
        Month(9, "رمضان", "شهر الصيام", "من الرَّمَض والرمضاء وهي شدة الحر، لأن تسميته وافقت زمن الحر الشديد. وهو الشهر الذي أُنزل فيه القرآن وفُرض صيامه."),
        Month(10, "شوّال", "", "قيل: من شَوَلان الإبل بأذنابها، أي رفعها، في ذلك الوقت، وقيل: لنقص ألبانها وجفافها فيه؛ يقال: شوّلت الناقة إذا قلّ لبنها."),
        Month(11, "ذو القعدة", "من الأشهر الحرم", "لقعود العرب فيه عن القتال والترحال، استعدادًا للحج، وهو أحد الأشهر الحرم."),
        Month(12, "ذو الحجة", "من الأشهر الحرم", "لأداء فريضة الحج فيه، وفيه يوم عرفة وعيد الأضحى، وهو أحد الأشهر الحرم."),
    )

    val gregorian = listOf(
        Month(1, "يناير", "كانون الثاني", "نسبة إلى «يانوس» إله البدايات والأبواب عند الرومان، كانوا يصوّرونه بوجهين: وجه ينظر إلى السنة الماضية وآخر إلى المقبلة."),
        Month(2, "فبراير", "شباط", "من «فِبْرُوا» (Februa) وهو عيد التطهير عند الرومان، كانوا يقيمونه في هذا الشهر."),
        Month(3, "مارس", "آذار", "نسبة إلى «مارس» إله الحرب عند الرومان، وكان أول شهور السنة في التقويم الروماني القديم، وفيه تبدأ الحملات العسكرية بعد الشتاء."),
        Month(4, "أبريل", "نيسان", "قيل: من الكلمة اللاتينية «أَبِرِيرِه» (aperire) أي يفتح، لتفتّح الأزهار والبراعم فيه، وقيل غير ذلك."),
        Month(5, "مايو", "أيّار", "نسبة إلى «مايا» (Maia) إلهة الخصب والنماء عند الرومان."),
        Month(6, "يونيو", "حزيران", "نسبة إلى «جونو» (Juno) زوجة كبير الآلهة عند الرومان، وكانت عندهم ربة الزواج."),
        Month(7, "يوليو", "تمّوز", "نسبة إلى يوليوس قيصر الذي أصلح التقويم، وكان اسمه قبل ذلك «كوينتيليس» أي الشهر الخامس."),
        Month(8, "أغسطس", "آب", "نسبة إلى الإمبراطور الروماني أغسطس، وكان اسمه قبل ذلك «سكستيليس» أي الشهر السادس."),
        Month(9, "سبتمبر", "أيلول", "من «سِبْتِم» (septem) وتعني سبعة، لأنه كان الشهر السابع في التقويم الروماني القديم الذي يبدأ بمارس."),
        Month(10, "أكتوبر", "تشرين الأول", "من «أُكْتو» (octo) وتعني ثمانية، فقد كان الشهر الثامن في التقويم الروماني القديم."),
        Month(11, "نوفمبر", "تشرين الثاني", "من «نُوفِم» (novem) وتعني تسعة، فقد كان الشهر التاسع في التقويم الروماني القديم."),
        Month(12, "ديسمبر", "كانون الأول", "من «دِيسِم» (decem) وتعني عشرة، فقد كان الشهر العاشر في التقويم الروماني القديم."),
    )
}

@Composable
fun MonthsPage() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val current = if (tab == 0) Data.hijri().month else LocalDate.now().monthValue
    val list = if (tab == 0) Months.hijri else Months.gregorian
    val state = rememberLazyListState()
    LaunchedEffect(tab) { state.scrollToItem(0) }
    LazyColumn(Modifier.fillMaxSize(), state = state, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Choice("الشهور الهجرية", tab == 0) { tab = 0 }
                Choice("الشهور الميلادية", tab == 1) { tab = 1 }
            }
        }
        item {
            Panel(color = P.card, pad = 14.dp) {
                if (tab == 0) {
                    Text("سمّت العرب شهورها قبل الإسلام بحسب أحوالها وأزمنة السنة حين وُضعت الأسماء، فبقيت الأسماء وإن تنقّلت الشهور بين الفصول، لأن السنة القمرية أقصر من الشمسية بنحو أحد عشر يومًا.",
                        color = P.ink, fontSize = 14.sp, lineHeight = 23.sp)
                    Text("﴿إِنَّ عِدَّةَ الشُّهُورِ عِندَ اللَّهِ اثْنَا عَشَرَ شَهْرًا فِي كِتَابِ اللَّهِ يَوْمَ خَلَقَ السَّمَاوَاتِ وَالْأَرْضَ مِنْهَا أَرْبَعَةٌ حُرُمٌ﴾ [التوبة: ٣٦]، والحُرُم: ذو القعدة وذو الحجة والمحرّم ورجب.",
                        color = P.gold, fontSize = 14.sp, lineHeight = 24.sp, fontFamily = Fonts.naskh, modifier = Modifier.padding(top = 6.dp))
                } else {
                    Text("الأسماء الميلادية المستعملة (يناير، فبراير…) لاتينية رومانية الأصل، أغلبها نسبة إلى معبودات الرومان أو أباطرتهم أو أرقام ترتيبها القديم. وفي بلاد الشام والعراق تُستعمل أسماء سريانية قديمة (كانون، شباط، آذار…) تجدها بجانب كل شهر.",
                        color = P.ink, fontSize = 14.sp, lineHeight = 23.sp)
                }
                Sub("هذه أشهر الأقوال عند أهل اللغة والتاريخ، وفي بعضها خلاف.", Modifier.padding(top = 6.dp), size = 11.sp)
            }
        }
        items(list, key = { "$tab-${it.n}" }) { m ->
            val now = m.n == current
            Column(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(if (now) P.accent.copy(alpha = .10f) else P.surface)
                .border(if (now) 1.5.dp else 0.dp, if (now) P.accent else P.surface, L.shape(14.dp)).padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(Quran.digits(m.n), color = P.gold, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 10.dp))
                    Text(m.name, color = if (now) P.accent else P.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (now) Text("الشهر الحالي", color = P.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                if (m.also.isNotEmpty()) Sub(m.also, size = 12.sp, color = P.gold)
                Text(m.why, color = P.ink, fontSize = (15 * Data.textScale).sp, lineHeight = (25 * Data.textScale).sp, fontFamily = Fonts.naskh, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
