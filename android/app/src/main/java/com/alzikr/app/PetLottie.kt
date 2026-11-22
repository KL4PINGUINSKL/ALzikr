package com.alzikr.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    data class Look(val id: String, val name: String, val by: String)
    val looks: Map<Species, List<Look>> = mapOf(
        Species.CAT to listOf(Look("cat-1", "تلعب", "Abdul Latif"), Look("cat-2", "تضحك", "Abdul Latif"), Look("cat-5", "بوكي", "LottieFiles"), Look("cat-12", "القطة السوداء", "PoPoF")),
        Species.PENGUIN to listOf(Look("penguin-2", "بالمنظار", "LottieFiles"), Look("penguin-3", "يحتفل", "LottieFiles"), Look("penguin-5", "يقفز", "Ankur K"), Look("penguin-7", "نائم", "LottieFiles")),
        Species.ALBATROSS to listOf(Look("seabird-1", "فوق البحر", "LottieFiles")),
        Species.SWALLOW to listOf(Look("bird-3", "سعيد", "LottieFiles"), Look("bird-6", "يلوّح", "LottieFiles"), Look("bird-9", "مرح", "LottieFiles")),
        Species.RABBIT to listOf(Look("rabbit-6", "منفوش", "LottieFiles"), Look("rabbit-2", "يقفز", "LottieFiles"), Look("rabbit-5", "الساحر", "LottieFiles"), Look("rabbit-10", "أرنبان", "LottieFiles"), Look("rabbit-12", "يلوّح", "LottieFiles"), Look("rabbit-9", "أرنب", "LottieFiles")),
        Species.SHEEP to listOf(Look("sheep-2", "تعدّ الخراف", "LottieFiles"), Look("sheep-4", "غنمة", "LottieFiles"), Look("sheep-5", "تقفز", "LottieFiles"), Look("sheep-8", "صغيرة", "LottieFiles")),
        Species.CHICKEN to listOf(Look("chicken-1", "دجاجة", "LottieFiles"), Look("chicken-5", "عصفورة زرقاء", "LottieFiles"), Look("chicken-6", "ترقص", "LottieFiles"), Look("chicken-12", "تركض", "LottieFiles"), Look("chicken-7", "تنقر", "LottieFiles"), Look("chicken-9", "كتكوت", "LottieFiles")),
    )
    fun of(s: Species) = looks[s].orEmpty()
    /** The look chosen for this friend (each friend keeps its own). */
    fun chosen(s: Species): Look? {
        val id = Store.prefs.getString("pet_look_${s.name}", null)
        return of(s).firstOrNull { it.id == id } ?: of(s).firstOrNull()
    }
    fun choose(s: Species, id: String) { Store.prefs.edit().putString("pet_look_${s.name}", id).apply(); Store.bump() }
}

/** One friend, animated by its Lottie look. [playing] false keeps a still frame (saves battery over other apps). */
@Composable
fun PetLottie(species: Species, modifier: Modifier, look: PetLooks.Look? = PetLooks.chosen(species), playing: Boolean = true, speed: Float = 1f) {
    val l = look ?: return
    val comp = rememberLottieComposition(LottieCompositionSpec.Asset("pets/${l.id}.json"))
    val progress = animateLottieCompositionAsState(comp.value, iterations = LottieConstants.IterateForever, isPlaying = playing, speed = speed)
    LottieAnimation(comp.value, { progress.value }, modifier, contentScale = ContentScale.Fit)
}
