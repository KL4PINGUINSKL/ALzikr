package com.alzikr.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.widget.RemoteViews
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable

/**
 * «صديقي» on the phone's home screen: the friend's picture (a frame of its Lottie look), its name, its
 * friendship title, the day's hearts and a word. «كلّمني» gives a new word and a new pose, «سبّح» counts
 * one tasbih with the app's tasbih, and a tap on the friend opens its page.
 */
class PetWidget : AppWidgetProvider() {
    override fun onUpdate(c: Context, manager: AppWidgetManager, ids: IntArray) {
        Store.init(c)
        val done = goAsync()
        Thread { try { update(c) } finally { done.finish() } }.start()
    }
    override fun onReceive(c: Context, i: Intent) {
        super.onReceive(c, i)
        when (i.action) {
            TALK -> { Store.init(c); Store.prefs.edit().putInt("pet_wn", Store.prefs.getInt("pet_wn", 0) + 1).apply(); Progress.bond("wtalk" + System.currentTimeMillis() / 1_800_000L); update(c, full = false) }
            NAP -> { Store.init(c); Pet.setNap(!Pet.asleep()); update(c) }
            TASBIH -> {
                Store.init(c)
                val n = Data.tasbihTap()
                Haptics.tap(c, n % 33 == 0)
                if (n % 33 == 0) Pet.gain("tasbih_${n / 33}", 5)
                Store.prefs.edit().putString("pet_wsay", "${Pet.tasbihWord(n)} · ${Quran.digits(n)}").apply()
                runCatching { TasbihWidget.update(c); CounterNotice.update(c) }
                update(c, full = false)
            }
        }
    }

    companion object {
        const val TALK = "com.alzikr.app.PET_W_TALK"
        const val TASBIH = "com.alzikr.app.PET_W_TASBIH"
        const val NAP = "com.alzikr.app.PET_W_NAP"

        fun has(c: Context) = AppWidgetManager.getInstance(c).getAppWidgetIds(ComponentName(c, PetWidget::class.java)).isNotEmpty()

        /** Asks the launcher to place the widget (Android 8+; the launcher shows its own confirmation). */
        fun pin(c: Context): Boolean {
            val m = AppWidgetManager.getInstance(c)
            if (!m.isRequestPinAppWidgetSupported) return false
            return runCatching { m.requestPinAppWidget(ComponentName(c, PetWidget::class.java), null, null) }.getOrDefault(false)
        }

        /**
         * [full]: colors, the animation frames and the buttons too; otherwise only the words change (a tap on a button),
         * so the frames are not sent again.
         */
        fun update(c: Context, full: Boolean = true) {
            val manager = AppWidgetManager.getInstance(c)
            val ids = manager.getAppWidgetIds(ComponentName(c, PetWidget::class.java))
            if (ids.isEmpty()) return
            Store.init(c)
            if (!full) {
                val t = RemoteViews(c.packageName, R.layout.widget_pet)
                words(t)
                runCatching { manager.partiallyUpdateAppWidget(ids, t) }.onFailure { update(c, true) }
                return
            }
            val pal = ThemeColors.of(c)
            val v = RemoteViews(c.packageName, R.layout.widget_pet)
            // The theme's colors, or a frame opened by the friend's level (light text on the dark frames, dark on gold).
            val frame = Progress.frames.firstOrNull { it.id == Progress.frame }
            val fr = frame?.res
            var ink = ThemeColors.argb(pal.ink); var gold = ThemeColors.argb(pal.gold); var acc = ThemeColors.argb(pal.accent)
            if (fr == null) ThemeColors.background(v, R.id.p_root, pal.surface)
            else {
                v.setInt(R.id.p_root, "setBackgroundResource", fr)
                if (android.os.Build.VERSION.SDK_INT >= 31) v.setColorStateList(R.id.p_root, "setBackgroundTintList", null)
                if (frame?.light == true) { ink = 0xFF2A1E10.toInt(); gold = 0xFF7A4E00.toInt(); acc = 0xFF4A3200.toInt() }
                else { ink = 0xFFFFFFFF.toInt(); gold = 0xFFF3D27A.toInt(); acc = 0xFFFFFFFF.toInt() }
            }
            v.setTextColor(R.id.p_name, ink); v.setTextColor(R.id.p_title, gold)
            v.setTextColor(R.id.p_hearts, acc); v.setTextColor(R.id.p_say, ink); v.setTextColor(R.id.p_extra, (ink and 0x00FFFFFF) or 0xB3000000.toInt())
            v.setTextColor(R.id.p_talk, acc); v.setTextColor(R.id.p_tasbih, acc); v.setTextColor(R.id.p_nap, acc)
            // The buttons' pill takes the accent, faintly, so it shows on light and dark backgrounds alike.
            if (android.os.Build.VERSION.SDK_INT >= 31) listOf(R.id.p_talk, R.id.p_tasbih, R.id.p_nap).forEach {
                v.setColorStateList(it, "setBackgroundTintList", android.content.res.ColorStateList.valueOf((acc and 0x00FFFFFF) or 0x55000000))
            }
            words(v)
            v.removeAllViews(R.id.p_img)
            Pet.species?.let { sp ->
                val frames = runCatching { frames(c, sp, Pet.asleep()) }.getOrDefault(emptyList())
                frames.forEach { bmp -> v.addView(R.id.p_img, RemoteViews(c.packageName, R.layout.widget_pet_frame).apply { setImageViewBitmap(R.id.p_frame, bmp) }) }
            }
            val open = PendingIntent.getActivity(c, 98, Intent(c, MainActivity::class.java).putExtra("page", "pet").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            v.setOnClickPendingIntent(R.id.p_img, open); v.setOnClickPendingIntent(R.id.p_name, open)
            v.setOnClickPendingIntent(R.id.p_talk, PendingIntent.getBroadcast(c, 99, Intent(c, PetWidget::class.java).setAction(TALK), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            v.setTextViewText(R.id.p_nap, if (Pet.asleep()) "☀ أيقظه" else "💤 نم")
            v.setOnClickPendingIntent(R.id.p_nap, PendingIntent.getBroadcast(c, 101, Intent(c, PetWidget::class.java).setAction(NAP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            v.setOnClickPendingIntent(R.id.p_tasbih, PendingIntent.getBroadcast(c, 100, Intent(c, PetWidget::class.java).setAction(TASBIH), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            // If the launcher refuses so many pictures, keep the friend still rather than leave the widget empty.
            runCatching { ids.forEach { manager.updateAppWidget(it, v) } }.onFailure {
                v.removeAllViews(R.id.p_img)
                Pet.species?.let { sp -> frameBitmap(c, sp, 0f)?.let { b -> v.addView(R.id.p_img, RemoteViews(c.packageName, R.layout.widget_pet_frame).apply { setImageViewBitmap(R.id.p_frame, b) }) } }
                runCatching { ids.forEach { manager.updateAppWidget(it, v) } }
            }
        }

        /** The words: name, title, hearts, what it says, and the challenge or the streak. */
        private fun words(v: RemoteViews) {
            runCatching { Pet.noteGoodDay() }
            val sp = Pet.species
            val n = Store.prefs.getInt("pet_wn", 0)
            if (sp == null) {
                v.setTextViewText(R.id.p_name, "صديقي"); v.setTextViewText(R.id.p_title, "")
                v.setTextViewText(R.id.p_hearts, ""); v.setTextViewText(R.id.p_say, "اضغط لتختار صديقك 🐾")
                v.setTextViewText(R.id.p_extra, "")
                return
            }
            v.setTextViewText(R.id.p_name, Pet.name)
            v.setTextViewText(R.id.p_title, if (Progress.showLevels) "${Friendship.title()} · المستوى ${Quran.digits(Pet.level())}" else "")
            val mood = Pet.mood()
            v.setTextViewText(R.id.p_hearts, (1..5).joinToString("") { k -> if (mood >= k * 20 - 10) Friendship.heart else "♡" })
            // A tasbih word from the widget shows once; otherwise a fitting word for now.
            val said = Store.prefs.getString("pet_wsay", null)
            if (said != null) Store.prefs.edit().remove("pet_wsay").apply()
            val text = said ?: if (Pet.asleep()) "💤 نائم… لا تنسَ أذكار النوم" else runCatching { Pet.say(n).text }.getOrDefault("لا تنسَ ذكر الله")
            v.setTextViewText(R.id.p_say, text)
            // The rest of the space: today's challenge until it is done, then the good days in a row.
            val st = Pet.streak()
            v.setTextViewText(R.id.p_extra, when {
                !Pet.challengeDone -> "🎯 تحدي اليوم: ${Pet.challenge()}"
                st > 0 -> "🔥 ${Quran.digits(st)} ${if (st in 3..10) "أيام" else "يوم"} طيبة متتالية · ✓ أتممت تحدي اليوم"
                else -> "✓ أتممت تحدي اليوم، تقبّل الله منك"
            })
        }

        /**
         * The animation as pictures: about one frame per 90 ms of the look (8 to 18 frames), kept in memory per look.
         * Asleep, one still frame.
         */
        @Volatile private var cache: Pair<String, List<Bitmap>>? = null
        private fun frames(c: Context, sp: Species, asleep: Boolean): List<Bitmap> {
            val look = PetLooks.chosen(sp) ?: return emptyList()
            val skin = com.alzikr.app.pets.PetPacks.registry().skin(look.id) ?: return emptyList()
            val key = "${look.id}|$asleep|${com.alzikr.app.pets.PetPacks.revision}"
            cache?.takeIf { it.first == key }?.let { return it.second }
            // Reduced motion or no animation in the source: one still frame.
            val still = asleep || com.alzikr.app.pets.PetPerf.reduceMotion || !com.alzikr.app.pets.PetFrames.animated(skin)
            val n = if (still) 1 else (com.alzikr.app.pets.PetFrames.durationMs(c, skin) / 90f).toInt().coerceIn(8, 18)
            val px = (120 * c.resources.displayMetrics.density).toInt().coerceIn(200, 300)
            val list = (0 until n).mapNotNull { i -> com.alzikr.app.pets.PetFrames.frame(c, skin, if (n == 1) 0f else i.toFloat() / n, px, if (asleep) com.alzikr.app.pets.PetState.SLEEP else com.alzikr.app.pets.PetState.IDLE) }
            cache = key to list
            return list
        }

        /** One frame of the chosen look (the certificate and the still fallback). */
        internal fun frameBitmap(c: Context, sp: Species, at: Float, size: Int = 0): Bitmap? {
            val look = PetLooks.chosen(sp) ?: return null
            val skin = com.alzikr.app.pets.PetPacks.registry().skin(look.id) ?: return null
            val px = if (size > 0) size else (150 * c.resources.displayMetrics.density).toInt().coerceAtMost(460)
            return com.alzikr.app.pets.PetFrames.frame(c, skin, at, px)
        }
    }
}
