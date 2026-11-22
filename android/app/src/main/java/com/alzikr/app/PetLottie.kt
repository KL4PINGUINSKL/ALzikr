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
        Species.CAT to listOf(Look("cat-1", "تلعب", "Abdul Latif", zoom = 1.15f), Look("cat-2", "تضحك", "Abdul Latif"), Look("cat-5", "بوكي", "LottieFiles"), Look("cat-12", "القطة السوداء", "PoPoF", darkLines = true)),
        Species.PENGUIN to listOf(Look("penguin-2", "بالمنظار", "LottieFiles"), Look("penguin-3", "يحتفل", "LottieFiles", zoom = 1.6f), Look("penguin-5", "يقفز", "Ankur K", zoom = 1.7f), Look("penguin-7", "نائم", "LottieFiles")),
        Species.ALBATROSS to listOf(Look("seabird-1", "فوق البحر", "LottieFiles")),
        Species.SWALLOW to listOf(Look("bird-3", "سعيد", "LottieFiles", zoom = 1.15f), Look("bird-6", "يلوّح", "LottieFiles"), Look("bird-9", "مرح", "LottieFiles", zoom = 1.15f)),
        Species.RABBIT to listOf(Look("rabbit-6", "منفوش", "LottieFiles"), Look("rabbit-2", "يقفز", "LottieFiles"), Look("rabbit-5", "الساحر", "LottieFiles"), Look("rabbit-10", "أرنبان", "LottieFiles"), Look("rabbit-12", "يلوّح", "LottieFiles"), Look("rabbit-9", "أرنب", "LottieFiles", zoom = 1.4f)),
        Species.SHEEP to listOf(Look("sheep-2", "تعدّ الخراف", "LottieFiles"), Look("sheep-4", "غنمة", "LottieFiles", zoom = 2.0f), Look("sheep-5", "تقفز", "LottieFiles"), Look("sheep-8", "صغيرة", "LottieFiles", zoom = 1.1f)),
        Species.CHICKEN to listOf(Look("chicken-1", "دجاجة", "LottieFiles"), Look("chicken-5", "عصفورة زرقاء", "LottieFiles"), Look("chicken-6", "ترقص", "LottieFiles"), Look("chicken-12", "تركض", "LottieFiles", zoom = 1.3f), Look("chicken-7", "تنقر", "LottieFiles", zoom = 1.3f), Look("chicken-9", "كتكوت", "LottieFiles")),
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
