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
    override fun onUpdate(c: Context, manager: AppWidgetManager, ids: IntArray) { Store.init(c); update(c) }
    override fun onReceive(c: Context, i: Intent) {
        super.onReceive(c, i)
        when (i.action) {
            TALK -> { Store.init(c); Store.prefs.edit().putInt("pet_wn", Store.prefs.getInt("pet_wn", 0) + 1).apply(); Progress.bond("wtalk" + System.currentTimeMillis() / 1_800_000L); update(c) }
            TASBIH -> {
                Store.init(c)
                val n = Data.tasbihTap()
                Haptics.tap(c, n % 33 == 0)
                if (n % 33 == 0) Pet.gain("tasbih_${n / 33}", 5)
                Store.prefs.edit().putString("pet_wsay", "${Pet.tasbihWord(n)} · ${Quran.digits(n)}").apply()
                runCatching { TasbihWidget.update(c); CounterNotice.update(c) }
                update(c)
            }
        }
    }

    companion object {
        const val TALK = "com.alzikr.app.PET_W_TALK"
        const val TASBIH = "com.alzikr.app.PET_W_TASBIH"

        fun has(c: Context) = AppWidgetManager.getInstance(c).getAppWidgetIds(ComponentName(c, PetWidget::class.java)).isNotEmpty()

        /** Asks the launcher to place the widget (Android 8+; the launcher shows its own confirmation). */
        fun pin(c: Context): Boolean {
            val m = AppWidgetManager.getInstance(c)
            if (!m.isRequestPinAppWidgetSupported) return false
            return runCatching { m.requestPinAppWidget(ComponentName(c, PetWidget::class.java), null, null) }.getOrDefault(false)
        }

        fun update(c: Context) {
            val manager = AppWidgetManager.getInstance(c)
            val ids = manager.getAppWidgetIds(ComponentName(c, PetWidget::class.java))
            if (ids.isEmpty()) return
            Store.init(c)
            val pal = ThemeColors.of(c)
            val v = RemoteViews(c.packageName, R.layout.widget_pet)
            // The theme's colors, or a frame opened by the friend's level (light text on the dark frames, dark on gold).
            val fr = Progress.frames.firstOrNull { it.id == Progress.frame }?.res
            var ink = ThemeColors.argb(pal.ink); var gold = ThemeColors.argb(pal.gold); var acc = ThemeColors.argb(pal.accent)
            if (fr == null) ThemeColors.background(v, R.id.p_root, pal.surface)
            else {
                v.setInt(R.id.p_root, "setBackgroundResource", fr)
                if (android.os.Build.VERSION.SDK_INT >= 31) v.setColorStateList(R.id.p_root, "setBackgroundTintList", null)
                if (Progress.frame == "gold") { ink = 0xFF3A2A06.toInt(); gold = 0xFF7A4E00.toInt(); acc = 0xFF5A3A00.toInt() }
                else { ink = 0xFFFFFFFF.toInt(); gold = 0xFFF3D27A.toInt(); acc = 0xFFFFFFFF.toInt() }
            }
            v.setTextColor(R.id.p_name, ink); v.setTextColor(R.id.p_title, gold)
            v.setTextColor(R.id.p_hearts, acc); v.setTextColor(R.id.p_say, ink); v.setTextColor(R.id.p_extra, (ink and 0x00FFFFFF) or 0xB3000000.toInt())
            v.setTextColor(R.id.p_talk, acc); v.setTextColor(R.id.p_tasbih, acc)
            // The buttons' pill takes the accent, faintly, so it shows on light and dark backgrounds alike.
            if (android.os.Build.VERSION.SDK_INT >= 31) listOf(R.id.p_talk, R.id.p_tasbih).forEach {
                v.setColorStateList(it, "setBackgroundTintList", android.content.res.ColorStateList.valueOf((acc and 0x00FFFFFF) or 0x55000000))
            }
            runCatching { Pet.noteGoodDay() }
            val sp = Pet.species
            val n = Store.prefs.getInt("pet_wn", 0)
            if (sp == null) {
                v.setTextViewText(R.id.p_name, "صديقي"); v.setTextViewText(R.id.p_title, "")
                v.setTextViewText(R.id.p_hearts, ""); v.setTextViewText(R.id.p_say, "اضغط لتختار صديقك 🐾")
                v.setImageViewBitmap(R.id.p_img, null); v.setTextViewText(R.id.p_extra, "")
            } else {
                v.setTextViewText(R.id.p_name, Pet.name)
                v.setTextViewText(R.id.p_title, if (Progress.showLevels) "${Pet.title()} · المستوى ${Quran.digits(Pet.level())}" else "")
                val mood = Pet.mood()
                v.setTextViewText(R.id.p_hearts, (1..5).joinToString("") { k -> if (mood >= k * 20 - 10) "❤" else "♡" })
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
                runCatching { frameBitmap(c, sp, if (Pet.asleep()) 0f else (n * 0.37f) % 1f) }.getOrNull()?.let { v.setImageViewBitmap(R.id.p_img, it) }
            }
            val open = PendingIntent.getActivity(c, 98, Intent(c, MainActivity::class.java).putExtra("page", "pet").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            v.setOnClickPendingIntent(R.id.p_img, open); v.setOnClickPendingIntent(R.id.p_name, open)
            v.setOnClickPendingIntent(R.id.p_talk, PendingIntent.getBroadcast(c, 99, Intent(c, PetWidget::class.java).setAction(TALK), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            v.setOnClickPendingIntent(R.id.p_tasbih, PendingIntent.getBroadcast(c, 100, Intent(c, PetWidget::class.java).setAction(TASBIH), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            ids.forEach { manager.updateAppWidget(it, v) }
        }

        /** One frame of the chosen look, drawn into a small bitmap (widgets cannot play animations). */
        internal fun frameBitmap(c: Context, sp: Species, at: Float, size: Int = 0): Bitmap? {
            val look = PetLooks.chosen(sp) ?: return null
            val comp = LottieCompositionFactory.fromAssetSync(c, "pets/${look.id}.json").value ?: return null
            val px = if (size > 0) size else (150 * c.resources.displayMetrics.density).toInt().coerceAtMost(460)
            val d = LottieDrawable().apply { composition = comp; progress = at }
            val b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
            val cv = Canvas(b)
            val w = comp.bounds.width().toFloat().coerceAtLeast(1f); val h = comp.bounds.height().toFloat().coerceAtLeast(1f)
            val s = minOf(px / w, px / h) * look.zoom
            cv.translate((px - w * s) / 2f, (px - h * s) / 2f - look.lift * px)
            cv.scale(s, s)
            d.setBounds(0, 0, w.toInt(), h.toInt())
            d.draw(cv)
            return b
        }
    }
}
