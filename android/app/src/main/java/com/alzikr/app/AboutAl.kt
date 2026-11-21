package com.alzikr.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * "About" in Settings: this app's version and what changed, a "Check for updates" button and the
 * other AL apps (open them, or get them through ALhub). Shared file: the same code lives in every
 * AL Android app; only [AboutAl.CHANGES] differs.
 */
object AboutAl {
    const val APP = "ALzikr"

    /** Newest first: version to what changed (shown under "What's new"). */
    val CHANGES: List<Pair<String, List<String>>> = listOf(
        "1.1.2" to listOf("المصحف أوضح: شريط سفلي فيه «استماع» و«التفسير تحت الآيات» و«حجم الخط»، ومشغّل واضح أثناء التلاوة (السابقة، الإيقاف، التالية، التكرار، القارئ)", "الضغط على آية يفتح لوحة بأزرار كبيرة ثم تفسيرها في قسم منفصل", "تلميحات في أول زيارة لكل صفحة تشرح أين تضغط، ويمكن إظهارها من جديد من الإعدادات"),
        "1.1.1" to listOf("سبعة ألوان لكل ثيم، ولون حر من دائرة الألوان", "أربعة ثيمات جديدة: الكعبة، المدينة، الفجر، البحر (12 ثيمًا)", "أحد عشر تفسيرًا: الميسر، المختصر، الجلالين، السعدي، ابن كثير، الطبري، القرطبي، البغوي، الوسيط، التحرير والتنوير، إعراب القرآن", "وضع الدراسة: التفسير تحت كل آية", "تلاوة القرآن بـ13 قارئًا مع تمييز الآية والتكرار للحفظ وإكمال السورة التالية", "ويدجت السبحة على الشاشة الرئيسية", "ثيم المصحف بلا زخرفة النجمة"),
        "1.1.0" to listOf("ثمانية ثيمات تغيّر الألوان والأشكال والخطوط وترتيب التنقل والأيقونات وعرض الصلوات وشكل العدّاد", "المكتبة: القرآن الكريم كاملًا بخط أميري قرآن، مع البحث والأجزاء والعلامات ومتابعة القراءة وتفسير الجلالين", "كتب الحديث: الأربعون النووية والقدسية، وتنزيل الصحيحين والسنن والموطأ ورياض الصالحين وغيرها", "آية اليوم وحديث اليوم، والمفضلة، والنسخ والمشاركة، وربط الختمة بالمصحف", "التركيز صار داخل «يومي»"),
        "1.0.1" to listOf("أذكار كاملة: الصباح والمساء وبعد الصلاة والنوم والاستيقاظ وأدعية القرآن والسنة والمناسبات، مع قارئ يعدّ لك", "سبحة حرة بإحصائيات، وتسجيل الصلوات مع زر «صليت» في الإشعار، وقضاء الفوائت", "بوصلة القبلة، ومواقيت الشهر، والتقويم الهجري والمناسبات، وأسماء الله الحسنى", "العادات اليومية والختمة، وويدجت الصلاة القادمة، والنسخ الاحتياطي", "تذكير النوم والجمعة والصيام والسحور، وصمت الهاتف بعد الأذان، والمظهر وحجم الخط"),
        "1.0.0" to listOf("تنبيه بأوقات الصلاة يُحسب على الهاتف بلا إنترنت، مع تذكير قبل الصلاة", "عدّاد للأذكار وأوراد يومية تختارها أنت، وتذكير بأذكار الصباح والمساء", "مهام تربطها بصلاة، وملاحظات", "جلسات تركيز مع مؤقّت في الإشعارات"),
    )

    data class Sibling(val name: String, val pkg: String, val color: Long, val about: String)

    val APPS = listOf(
        Sibling("ALmouse", "com.wirelessremote.app", 0xFFA78BFA, "الهاتف ماوس ولوحة مفاتيح وشاشة حية للكمبيوتر"),
        Sibling("ALphonectrl", "com.alphonectrl.app", 0xFFE5825A, "يومك مع الكمبيوتر: الملفات والمهام والتركيز والشاشة"),
        Sibling("ALspeednet", "com.alspeednet.app", 0xFF2DD4BF, "سرعة الإنترنت بجانب الساعة، واختبار السرعة وأدوات الشبكة"),
        Sibling("ALtvcontrol", "com.altvcontrol.app", 0xFFE0315B, "ريموت التلفزيون: LG وسامسونج وأندرويد TV عبر الواي فاي"),
        Sibling("ALzikr", "com.alzikr.app", 0xFF1F7A5A, "أوقات الصلاة والأذكار والمهام وجلسات التركيز"),
        Sibling("ALhub", "com.alhub.app", 0xFF8B5CF6, "مكان واحد لكل تطبيقات AL وتحديثاتها"),
    )

    fun version(c: Context): String = runCatching {
        val pm = c.packageManager
        if (Build.VERSION.SDK_INT >= 33) pm.getPackageInfo(c.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0)).versionName
        else @Suppress("DEPRECATION") pm.getPackageInfo(c.packageName, 0).versionName
    }.getOrNull() ?: "?"

    fun installed(c: Context, pkg: String) = c.packageManager.getLaunchIntentForPackage(pkg) != null

    /** Opens the app if installed; otherwise ALhub (which installs it), or the ALapps download page. */
    fun open(c: Context, app: Sibling) {
        val pm = c.packageManager
        val intent = pm.getLaunchIntentForPackage(app.pkg)
            ?: pm.getLaunchIntentForPackage("com.alhub.app")
            ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/KL4PINGUINSKL/ALapps/releases"))
        runCatching { c.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}

/**
 * The About section. Colors come from the app ([ink] text, [muted] secondary, [accent] buttons,
 * [card] background of rows), so it fits each app's own design.
 */
@Composable
fun AboutSection(ink: Color, muted: Color, accent: Color, card: Color, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val version = remember { AboutAl.version(context) }
    var all by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("عن التطبيق", color = ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${AboutAl.APP} $version", color = ink, fontWeight = FontWeight.Medium)
                Text("من تطبيقات AL · التحديثات من KL4PINGUINSKL/ALapps", color = muted, fontSize = 12.sp)
            }
            Text("البحث عن تحديث", color = accent, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { (context as? Activity)?.let { AppUpdate.checkNow(it) } }.padding(horizontal = 10.dp, vertical = 8.dp))
        }

        Text("الجديد", color = ink, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
        val shown = if (all) AboutAl.CHANGES else AboutAl.CHANGES.take(3)
        shown.forEach { (v, items) ->
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(card).padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(if (v == version) "$v · إصدارك الحالي" else v, color = if (v == version) accent else ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                items.forEach { Text("• $it", color = muted, fontSize = 12.5.sp) }
            }
        }
        if (AboutAl.CHANGES.size > 3) Text(if (all) "أقل" else "إصدارات أقدم", color = accent, fontSize = 13.sp,
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { all = !all }.padding(horizontal = 4.dp, vertical = 6.dp))

        Text("تطبيقات AL الأخرى", color = ink, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
        AboutAl.APPS.filter { it.name != AboutAl.APP }.forEach { app ->
            val has = remember(app.pkg) { AboutAl.installed(context, app.pkg) }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(card).clickable { AboutAl.open(context, app) }.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(Color(app.color)))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(app.name, color = ink, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text(app.about, color = muted, fontSize = 12.sp)
                }
                Spacer(Modifier.width(8.dp))
                Text(if (has) "فتح" else "تثبيت", color = accent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
