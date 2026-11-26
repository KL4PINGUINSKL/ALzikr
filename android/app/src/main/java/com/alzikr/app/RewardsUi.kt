package com.alzikr.app

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/** «مكافآتي»: the level, today's experience, what each level opens, the widget frames, the badges, and the certificate. */
@Composable
fun RewardsPage() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) { RewardsContent() }
}

/** The rewards in four short sections (no long page to scroll through). */
@Composable
fun RewardsContent() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val ctx = LocalContext.current
    val sp = Pet.species
    var part by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf("me") }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Hint("rewards", "🎁", "النقاط والمستويات مكافآت داخل التطبيق تفرح بها مع صديقك، وليست مقياسًا للأجر ولا لصلاح أحد؛ فالأجر عند الله وحده، والعبرة بالإخلاص. وتستطيع إخفاءها من آخر هذه الصفحة.")
        SettingRow("إظهار المستويات والنقاط", if (Progress.showLevels) null else "مخفية: تبقى المكافآت تُفتح بهدوء") { Switch(Progress.showLevels, { Progress.showLevels = it }) }
        if (!Progress.showLevels) return@Column
        val lv = Progress.level(); val x = Progress.xp
        TabGrid(listOf(Triple("me", "✨", "تقدّمي"), Triple("badges", "🏅", "أوسمتي"), Triple("gifts", "❤", "جوائز الصداقة"), Triple("levels", "🔓", "ما يُفتح")), part, cols = 4) { part = it }
        if (part == "me") Panel(color = P.card, pad = 14.dp) {
            if (sp != null) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { PetLottie(sp, Modifier.size(110.dp)) }
            Text("${Progress.title(lv)} · المستوى ${Quran.digits(lv)}", color = P.ink, fontSize = 19.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Sub("${Quran.digits(x)} ✨ · المستوى التالي عند ${Quran.digits(Progress.need(lv + 1))}", Modifier.fillMaxWidth(), size = 12.sp)
        }
        if (part == "me") Panel(pad = 14.dp) {
            Title("خبرة اليوم ✨")
            Progress.Kind.entries.forEach { k ->
                val got = Progress.todayXp(k)
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(k.ar, color = P.ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text("${Quran.digits(got)} / ${Quran.digits(k.cap)}", color = if (got >= k.cap) P.accent else P.muted, fontSize = 13.sp)
                }
            }
            Sub("لكل عمل نقاطه مرة في اليوم، ولكل نوع حدّ يومي، حتى يكبر صديقك بهدوء. الصفحة تُحسب بعد عشر ثوانٍ عليها في وضع القراءة أو عشرين في المصحف، أو بأزرار الختمة لمن يقرأ في مصحف ورقي. والحديث بعد 15 ثانية عليه، والشرح بعد دقيقة.", size = 11.sp)
        }
        if (part == "levels") Panel(pad = 14.dp) {
            Title("ما يُفتح لك")
            Progress.rewards().forEach { r ->
                val got = lv >= r.level
                Row(Modifier.fillMaxWidth().padding(top = 8.dp).alpha(if (got) 1f else .55f), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (got) r.icon else "🔒", fontSize = 20.sp, modifier = Modifier.width(34.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.name, color = P.ink, fontSize = 14.sp)
                        Sub(if (got) "مفتوح ✓" else "المستوى ${Quran.digits(r.level)}", size = 11.sp, color = if (got) P.accent else P.muted)
                    }
                }
            }
            Sub("وكل مستوى من 2 إلى 125 يفتح صديقًا أو شكلًا جديدًا. وصديقك الذي اخترته من قبل وشكله باقيان لك.", Modifier.padding(top = 8.dp), size = 11.sp)
        }
        if (part == "gifts") Panel(pad = 14.dp) {
            Title("جوائز الصداقة ${Friendship.heart}")
            Sub("بنقاط الصداقة وحدها (${Quran.digits(Progress.bondPoints)} ❤ الآن)، منفصلة عن المستوى: إطارات، وثيمات، وأوسمة، ومزايا.", size = 12.sp)
            Friendship.gifts.forEach { g ->
                val got = Progress.bondPoints >= g.bond
                Row(Modifier.fillMaxWidth().padding(top = 8.dp).alpha(if (got) 1f else .55f), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (got) g.icon else "🔒", fontSize = 20.sp, modifier = Modifier.width(34.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${g.name} · ${g.kind.ar}", color = P.ink, fontSize = 14.sp)
                        Sub(if (got) "مفتوح ✓ · ${g.about}" else "${Quran.digits(g.bond)} ❤ · ${g.about}", size = 11.sp, color = if (got) P.accent else P.muted)
                    }
                }
            }
        }
        if (part == "gifts") Sub("🖼 إطارات الأداة وخلفياتها تختارها من «صديقي» › «الزينة».", size = 12.sp)
        if (part == "badges") Panel(pad = 14.dp) {
            Title("أوسمتي 🏅")
            val earned = Progress.earned()
            Progress.badges.sortedBy { if (earned[it.id] != null) 0 else 1 }.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { b ->
                        val at = earned[b.id]; val now = runCatching { b.now() }.getOrDefault(0)
                        Column(Modifier.weight(1f).clip(L.shape(12.dp)).background(if (at != null) P.gold.copy(alpha = .14f) else P.card).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(b.icon, fontSize = 26.sp, modifier = Modifier.alpha(if (at != null) 1f else .3f))
                            Text(b.name, color = P.ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 2)
                            Sub(if (at != null) at else "${Quran.digits(now.coerceAtMost(b.target))}/${Quran.digits(b.target)}", size = 10.sp)
                        }
                    }
                    repeat(3 - row.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
        if (part == "badges" && sp != null) Panel(pad = 14.dp) {
            Title("شهادة الصداقة 📜")
            val cert = Friendship.has("certificate")
            Sub(if (cert) "صورة فيها اسم صديقك ومستواه وأيامكما معًا، تشاركها إن شئت. لا تظهر فيها تفاصيل عبادتك." else "تُفتح بـ ${Quran.digits(Friendship.gift("certificate")!!.bond)} ❤ صداقة.", size = 12.sp)
            if (cert) Action("مشاركة الشهادة", Modifier.fillMaxWidth().padding(top = 8.dp)) { Certificate.share(ctx) }
        }
    }
}

@Composable
private fun framePreview(id: String): androidx.compose.ui.graphics.Brush =
    Progress.frames.firstOrNull { it.id == id && it.res != null }?.let { f ->
        androidx.compose.ui.graphics.Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color(f.top), androidx.compose.ui.graphics.Color(f.bottom)))
    } ?: androidx.compose.ui.graphics.Brush.verticalGradient(listOf(P.surface, P.card))

/** A new level or badge: a short celebration with the friend, once. Lives at the app's root. */
@Composable
fun ProgressCelebration() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    if (Data.simple) return
    val sp = Pet.species ?: return
    val l = Progress.pendingLevelUp()
    val badge = if (l == null) Progress.newBadge() else null
    val gift = if (l == null && badge == null) Friendship.newGift() else null
    if (gift != null) {
        androidx.compose.material3.AlertDialog(onDismissRequest = { Friendship.giftSeen() }, containerColor = P.surface,
            confirmButton = { androidx.compose.material3.TextButton({ Friendship.giftSeen() }) { Text("جميل!", color = P.accent) } },
            title = { Text("${Friendship.heart} هدية الصداقة", color = P.ink, fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    PetLottie(sp, Modifier.size(130.dp), speed = 1.3f)
                    Text("${gift.icon} ${gift.name}", color = P.gold, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Sub(gift.about, size = 13.sp)
                    Sub("صداقتكما الآن: ${Progress.bondName()}", size = 12.sp)
                }
            })
        return
    }
    if (l == null && badge == null) return
    val opened = if (l != null) Progress.rewards().filter { it.level == l } else emptyList()
    val looks = if (l != null) PetLooks.of(sp).indices.count { Progress.lookLevel(sp, it) == l } else 0
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { if (l != null) Progress.celebrated() else Progress.badgeSeen() },
        containerColor = P.surface,
        confirmButton = { androidx.compose.material3.TextButton({ if (l != null) Progress.celebrated() else Progress.badgeSeen() }) { Text("الحمد لله", color = P.accent) } },
        title = { Text(if (l != null) "🎉 ${Pet.name} في المستوى ${Quran.digits(l)}" else "🏅 وسام جديد", color = P.ink, fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                PetLottie(sp, Modifier.size(130.dp), speed = 1.3f)
                if (l != null) {
                    Text("«${Progress.title(l)}» · كبرت صداقتنا!", color = P.gold, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    if (opened.isNotEmpty() || looks > 0) Text("فُتح لك:", color = P.ink, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                    opened.forEach { Text("${it.icon} ${it.name}", color = P.ink, fontSize = 14.sp, textAlign = TextAlign.Center) }
                    if (looks > 0) Text("🐾 ${Quran.digits(looks)} ${if (looks == 1) "شكل جديد" else "أشكال جديدة"} لـ${sp.ar}", color = P.ink, fontSize = 14.sp)
                } else if (badge != null) {
                    Text("${badge.icon} ${badge.name}", color = P.gold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Sub(badge.how, size = 13.sp)
                }
            }
        },
    )
}

/** The friendship certificate as a shareable picture: the friend, its name, level and days together; nothing about worship. */
object Certificate {
    fun share(c: Context) {
        val sp = Pet.species ?: return
        val pal = ThemeColors.of(c)
        val w = 1080; val h = 1350
        val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        val cv = android.graphics.Canvas(bmp)
        cv.drawColor(pal.surface.toArgb())
        val frame = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = pal.gold.toArgb(); style = android.graphics.Paint.Style.STROKE; strokeWidth = 6f }
        cv.drawRoundRect(60f, 60f, w - 60f, h - 60f, 40f, 40f, frame)
        frame.strokeWidth = 2f; cv.drawRoundRect(84f, 84f, w - 84f, h - 84f, 30f, 30f, frame)
        PetWidget.frameBitmap(c, sp, 0.3f, 460)?.let { cv.drawBitmap(it, (w - 460) / 2f, 200f, null) }
        val t = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { textAlign = android.graphics.Paint.Align.CENTER }
        t.color = pal.gold.toArgb(); t.textSize = 64f; t.isFakeBoldText = true
        cv.drawText("شهادة صداقة", w / 2f, 160f, t)
        t.color = pal.ink.toArgb(); t.textSize = 76f
        cv.drawText(Pet.name, w / 2f, 760f, t)
        t.isFakeBoldText = false; t.textSize = 46f; t.color = pal.accent.toArgb()
        cv.drawText("«${Friendship.title()}» · المستوى ${Quran.digits(Progress.level())}", w / 2f, 850f, t)
        t.color = pal.ink.toArgb(); t.textSize = 42f
        val who = Pet.userName.takeIf { it.isNotBlank() }
        cv.drawText(if (who != null) "صديق $who" else "صديقي في ALzikr", w / 2f, 950f, t)
        if (Pet.daysTogether() > 0) cv.drawText("معًا منذ ${Quran.digits(Pet.daysTogether())} يومًا", w / 2f, 1020f, t)
        t.color = pal.muted.toArgb(); t.textSize = 32f
        cv.drawText(java.time.LocalDate.now().toString(), w / 2f, 1120f, t)
        cv.drawText("ALzikr", w / 2f, h - 120f, t)
        val dir = java.io.File(c.cacheDir, "share").apply { mkdirs() }
        val f = java.io.File(dir, "alzikr-friend.png")
        f.outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        val uri = androidx.core.content.FileProvider.getUriForFile(c, c.packageName + ".files", f)
        c.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "مشاركة الشهادة").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}


/** The widget's frames, an even grid (used in «الزينة»). */
@Composable
fun WidgetFramesGrid() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val ctx = LocalContext.current
    TileGrid(Progress.frames, cols = 3, rows = 2, startOpen = Progress.frames.indexOfFirst { it.id == Progress.frame } >= 6, noun = "الإطارات") { f, m ->
        val open = f.open; val on = Progress.frame == f.id
        PictureTile(f.name, on, open, f.bond, m, onClick = {
            if (open) { Progress.frame = f.id; PetWidget.update(ctx) }
            else android.widget.Toast.makeText(ctx, "يُفتح عند ${Quran.digits(f.bond)} ❤ (معك ${Quran.digits(Progress.bondPoints)})", android.widget.Toast.LENGTH_SHORT).show()
        }) { pm -> Box(pm.padding(6.dp).clip(L.shape(10.dp)).background(framePreview(f.id))) }
    }
}
