package com.alzikr.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * «الإبلاغ عن مشكلة أو اقتراح ميزة» (user, 2026-11-28): the words go to the app's site (alzikr.alpharmasy.com/feedback,
 * kept in its database), which also works where GitHub does not. Nothing is sent but what is written here and, if
 * left on, the app's version and the phone's model and Android version.
 */
object Feedback {
    /** The kind the page opens on («اقتراح ميزة» opens on the idea). */
    var startKind = "bug"
    val kinds = listOf("bug" to "🐞 مشكلة", "idea" to "💡 اقتراح ميزة", "other" to "💬 أخرى")

    /** Blocking; true when the site took it. */
    fun send(c: android.content.Context, kind: String, text: String, contact: String, withDevice: Boolean): Boolean = runCatching {
        val body = JSONObject().put("kind", kind).put("text", text.take(4000)).put("contact", contact.take(200))
            .put("version", runCatching { c.packageManager.getPackageInfo(c.packageName, 0).versionName }.getOrNull() ?: "")
        if (withDevice) body.put("device", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}").put("android", android.os.Build.VERSION.RELEASE)
        val con = (java.net.URL("$APP_SITE/feedback").openConnection() as java.net.HttpURLConnection).apply {
            requestMethod = "POST"; doOutput = true; connectTimeout = 15_000; readTimeout = 15_000
            setRequestProperty("content-type", "application/json; charset=utf-8"); setRequestProperty("user-agent", "ALzikr")
        }
        con.outputStream.use { it.write(body.toString().toByteArray()) }
        val ok = con.responseCode in 200..299
        con.disconnect(); ok
    }.getOrDefault(false)
}

@Composable
fun FeedbackPage() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var kind by rememberSaveable { mutableStateOf(Feedback.startKind) }
    var text by rememberSaveable { mutableStateOf("") }
    var contact by rememberSaveable { mutableStateOf("") }
    var device by rememberSaveable { mutableStateOf(true) }
    var state by rememberSaveable { mutableStateOf(0) }   // 0 writing, 1 sending, 2 sent, 3 failed
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Hint("feedback", "💬", "رأيك يصل إلى صاحب التطبيق مباشرة: مشكلة رأيتها، أو ميزة تتمنّاها. اكتب ما حدث وأين، وكيف يتكرر إن عرفت.")
        if (state == 2) {
            Panel(pad = 16.dp) {
                Text("✓ وصلت رسالتك، جزاك الله خيرًا", color = P.accent, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Sub("ستُقرأ إن شاء الله، وإن تركت وسيلة تواصل فقد يُرَدّ عليك.", Modifier.padding(top = 4.dp))
                Action("رسالة أخرى", Modifier.fillMaxWidth().padding(top = 10.dp), filled = false) { text = ""; state = 0 }
            }
            return@Column
        }
        Wrap(gap = 6.dp) { Feedback.kinds.forEach { (id, n) -> Choice(n, kind == id) { kind = id } } }
        OutlinedTextField(text, { text = it.take(4000) }, minLines = 6, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(when (kind) { "bug" -> "ماذا حدث؟ وفي أي قسم؟ وماذا كنت تتوقع؟"; "idea" -> "ما الميزة التي تريدها؟ وكيف تفيدك؟"; else -> "اكتب رسالتك…" }) })
        OutlinedTextField(contact, { contact = it.take(200) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("وسيلة للرد عليك (اختياري): بريد أو تيليجرام") })
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("أرفق نسخة التطبيق ونوع الهاتف", color = P.ink, fontSize = 14.sp)
                Sub("يساعد على إصلاح المشكلة؛ لا يُرسل غيرهما.", size = 11.sp)
            }
            androidx.compose.material3.Switch(device, { device = it })
        }
        if (state == 3) Sub("تعذّر الإرسال، تأكد من الاتصال ثم أعد المحاولة. أو أرسلها بتطبيق آخر:", color = P.danger, size = 13.sp)
        Action(if (state == 1) "جارٍ الإرسال…" else "إرسال", Modifier.fillMaxWidth()) {
            if (text.isBlank() || state == 1) return@Action
            state = 1
            scope.launch {
                val ok = withContext(Dispatchers.IO) { Feedback.send(ctx, kind, text.trim(), contact.trim(), device) }
                state = if (ok) 2 else 3
            }
        }
        if (state == 3) Action("مشاركة الرسالة", Modifier.fillMaxWidth(), filled = false) {
            val k = Feedback.kinds.firstOrNull { it.first == kind }?.second ?: ""
            ctx.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain")
                .putExtra(android.content.Intent.EXTRA_TEXT, "ALzikr · $k\n\n${text.trim()}"), "إرسال"))
        }
    }
}
