package com.alzikr.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

/**
 * The friends are professional Lottie animations (LottieFiles free animations, Lottie Simple License),
 * chosen by the user: several looks per animal, kept in assets/pets/<id>.json.
 */
object PetLooks {
    /** [zoom] enlarges a drawing that leaves empty space around the animal; [lift] moves it up (fraction of the box) so it stands on the ground. */
    data class Look(val id: String, val name: String, val by: String, val zoom: Float = 1f, val lift: Float = 0f, val darkLines: Boolean = false)
    val looks: Map<Species, List<Look>> = mapOf(
        Species.CAT to listOf(Look("cat-1", "تلعب", "Abdul Latif", zoom = 1.15f), Look("cat-2", "تضحك", "Abdul Latif"), Look("cat-5", "بوكي", "LottieFiles"), Look("cat-12", "القطة السوداء", "PoPoF", darkLines = true),
            Look("cat-n1", "على القمر", "Mohammad Saiduzzaman Sayed"), Look("cat-n2", "تطلّ", "Tom Fabre", zoom = 1.6f), Look("cat-n3", "تتدحرج نائمة", "Cảnh Ngô", zoom = 1.2f), Look("cat-n4", "في الصندوق", "Julio"), Look("cat-n5", "في القرعة", "Tam Doan"), Look("cat-n7", "نائمة… هدوء", "Bayu Febrianto"), Look("cat-n12", "بالنظارة", "Haligul Jaman"), Look("cat-n13", "تموء", "konstaner"), Look("cat-n14", "في دائرة", "Pramod"), Look("cat-n16", "تبتسم", "Nastinator"), Look("cat-n17", "تنعس", "Nastinator"), Look("cat-n18", "غاضبة", "Nastinator"), Look("cat-n28", "تتربّص", "Denys Zavalii"), Look("cat-n26", "مع الكرة", "Anita Kumari Pandey"), Look("cat-n29", "سوداء جالسة", "Masud Rana"), Look("cat-n30", "تلوّح", "aan hamdani"), Look("cat-n35", "تمشي", "Egor Petushkov"), Look("cat-n32", "سوداء تهزّ ذيلها", "Zoheir Hassoun"), Look("cat-n40", "تتمطّى", "Rasul Gadzhiev"), Look("cat-n41", "في الفنجان", "Charly Hill"), Look("cat-n49", "متحيّرة", "Nastinator"), Look("cat-n45", "تبكي", "Nastinator"), Look("cat-n44", "بالقلم", "Roman Karpiuk"), Look("cat-n43", "في القرعة الصغيرة", "Ridwan Ridun"), Look("cat-n52", "تحلم", "Sourav Haldar")),
        Species.PENGUIN to listOf(Look("penguin-2", "بالمنظار", "LottieFiles"), Look("penguin-3", "يحتفل", "LottieFiles", zoom = 1.6f), Look("penguin-5", "يقفز", "Ankur K", zoom = 1.7f), Look("penguin-7", "نائم", "LottieFiles"),
            Look("penguin-n1", "أزرق", "Apps for kids"), Look("penguin-n2", "يركل الكرة", "Nick J Cave"), Look("penguin-n3", "مع أصدقائه", "Bianca Beatrix Quicho"), Look("penguin-n6", "على الجليد", "Roshni N")),
        Species.ALBATROSS to listOf(Look("seabird-1", "فوق البحر", "LottieFiles")),
        Species.SWALLOW to listOf(Look("bird-3", "سعيد", "LottieFiles", zoom = 1.15f), Look("bird-6", "يلوّح", "LottieFiles"), Look("bird-9", "مرح", "LottieFiles", zoom = 1.15f)),
        Species.RABBIT to listOf(Look("rabbit-6", "منفوش", "LottieFiles"), Look("rabbit-2", "يقفز", "LottieFiles"), Look("rabbit-5", "الساحر", "LottieFiles"), Look("rabbit-10", "أرنبان", "LottieFiles"), Look("rabbit-12", "يلوّح", "LottieFiles"), Look("rabbit-9", "أرنب", "LottieFiles", zoom = 1.4f),
            Look("rabbit-n7", "خجول", "Brian Karungani"), Look("rabbit-n16", "مع الجزرة", "Roshni N")),
        Species.SHEEP to listOf(Look("sheep-2", "تعدّ الخراف", "LottieFiles"), Look("sheep-4", "غنمة", "LottieFiles", zoom = 2.0f), Look("sheep-5", "تقفز", "LottieFiles"), Look("sheep-8", "صغيرة", "LottieFiles", zoom = 1.1f),
            Look("sheep-n2", "صوفية", "拓真 豊原")),
        Species.CHICKEN to listOf(Look("chicken-1", "دجاجة", "LottieFiles"), Look("chicken-5", "عصفورة زرقاء", "LottieFiles"), Look("chicken-6", "ترقص", "LottieFiles"), Look("chicken-12", "تركض", "LottieFiles", zoom = 1.3f), Look("chicken-7", "تنقر", "LottieFiles", zoom = 1.3f), Look("chicken-9", "كتكوت", "LottieFiles"),
            Look("chicken-n1", "دجاجة بيضاء", "Guilherme Galvao"), Look("chicken-n7", "الكتكوت الأنيق", "Ciara"), Look("chicken-n5", "يفقس", "Elmer Vergara"), Look("chicken-n13", "ديك", "Bashir Ahmad"), Look("chicken-n9", "كتكوت بقبعة", "DezigningEye")),
        Species.BIRD to listOf(Look("bird-n1", "بومة", "Brian Karungani"), Look("bird-n2", "يطير", "Oyinloluwa Adedoyin"), Look("bird-n5", "يحلّق", "Diogo de Freitas", zoom = 1.8f), Look("bird-n6", "يحمل رسالة", "Zef Cherry-Kynaston"), Look("bird-n7", "أحمر الرأس", "modor mockups"), Look("bird-n8", "وردي", "Pixora Studio"), Look("bird-n10", "على الغصن", "Sekayi Liburd"), Look("bird-n11", "أخضر", "gomotion"), Look("bird-n12", "أصفر", "Akash Gupta")),
        Species.BEAR to listOf(Look("bear-n5", "الدب البني", "Bashir Ahmad"), Look("bear-n11", "دبدوب", "Itay Alon"), Look("bear-n22", "صغير لطيف", "Fphox Nguyen"), Look("bear-n3", "يشرب الشاي", "Amanda"), Look("bear-n10", "يصفّق", "Abdul Latif"), Look("bear-n25", "يرسل التحية", "Tyler Williams"), Look("bear-n15", "معجب", "Telegram Tracker"), Look("bear-n7", "نائم على الغصن", "Motionstk.studio"), Look("bear-n20", "يطير على القلم", "Abdul Latif"), Look("bear-n12", "لاعب الكرة", "Abdul Latif")),
        Species.PANDA to listOf(Look("panda-n4", "يلوّح", "Bhaskar Wary"), Look("panda-n11", "يقرأ", "Abdul Latif"), Look("panda-n8", "يلعب", "Abdul Latif"), Look("panda-n7", "مع الخيزران", "ucilucil"), Look("panda-n1", "نائم", "Zakaria Harry Utomo"), Look("panda-n2", "يحلم", "Irfanlesnar"), Look("panda-n13", "يغفو", "Steven Junior"), Look("panda-n5", "صغير نائم", "Yash Yash"), Look("panda-n9", "في الفضاء", "Sant Rojas")),
        Species.COW to listOf(Look("cow-2", "ترقص", "Betsa"), Look("cow-6", "تأكل العشب", "Bashir Ahmad"), Look("cow-4", "تمضغ", "Stephane Urth"), Look("cow-5", "السوداء", "Phase Media"),
            Look("cow-7", "وجه البقرة", "balaji madiri"), Look("cow-1", "صغيرة", "Sheikh Sohel", zoom = 2.4f), Look("cow-13", "غاضبة", "Anna Mazurkevich")),
        Species.ELEPHANT to listOf(Look("elephant-2", "يمشي", "Bashir Ahmad"), Look("elephant-3", "على التل", "Porag Gogoi"), Look("elephant-5", "يفكّر", "Noah Wise")),
        Species.GIRAFFE to listOf(Look("giraffe-3", "سعيدة", "Tarryn Myburgh"), Look("giraffe-1", "تمشي", "Bashir Ahmad"), Look("giraffe-4", "صغيرة جالسة", "Tom Fabre"),
            Look("giraffe-13", "تطلّ", "Sam Fry"), Look("giraffe-9", "تبتسم", "Sam Fry"), Look("giraffe-2", "على الدراجة", "Doug Fuchs", zoom = 2.4f), Look("giraffe-7", "مع الحوت", "Qrious Studio")),
    )
    fun of(s: Species) = looks[s].orEmpty()
    /** The look chosen for this friend (each friend keeps its own). */
    fun chosen(s: Species): Look? {
        val id = Store.prefs.getString("pet_look_${s.name}", null)
        // A look not opened yet (a new friend's rare look) falls back to the first one.
        val all = of(s); val i = all.indexOfFirst { it.id == id }
        return if (i >= 0 && Progress.lookOpen(s, i)) all[i] else all.firstOrNull()
    }
    fun choose(s: Species, id: String) { Store.prefs.edit().putString("pet_look_${s.name}", id).apply(); Store.bump() }
}

/** One friend, animated by its Lottie look. [playing] false keeps a still frame (saves battery over other apps). */
@Composable
fun PetLottie(species: Species, modifier: Modifier, look: PetLooks.Look? = PetLooks.chosen(species), playing: Boolean = true, speed: Float = 1f) {
    val l = look ?: return
    val comp = rememberLottieComposition(LottieCompositionSpec.Asset("pets/${l.id}.json"))
    // A drawing made of dark lines needs light behind it on a dark theme.
    val halo = if (l.darkLines && P.dark) Modifier.background(androidx.compose.ui.graphics.Brush.radialGradient(listOf(androidx.compose.ui.graphics.Color.White.copy(alpha = .55f), androidx.compose.ui.graphics.Color.Transparent))) else Modifier
    val progress = animateLottieCompositionAsState(comp.value, iterations = LottieConstants.IterateForever, isPlaying = playing, speed = speed)
    LottieAnimation(comp.value, { progress.value }, modifier.then(halo).graphicsLayer { scaleX = l.zoom; scaleY = l.zoom; translationY = -l.lift * size.height }, contentScale = ContentScale.Fit)
}

/** Developer check: every look at the size it has in the header (opened with the deep link page=pettest). */
@Composable
fun PetLooksTest() {
    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(androidx.compose.foundation.lazy.grid.GridCells.Fixed(4), Modifier.fillMaxSize()) {
        PetLooks.looks.forEach { (s, ls) ->
            ls.forEach { l ->
                item {
                    androidx.compose.foundation.layout.Column(Modifier.padding(4.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        androidx.compose.foundation.layout.Box(Modifier.size(80.dp).background(P.card)) { PetLottie(s, Modifier.fillMaxSize(), look = l) }
                        androidx.compose.material3.Text(l.id, color = P.ink, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
