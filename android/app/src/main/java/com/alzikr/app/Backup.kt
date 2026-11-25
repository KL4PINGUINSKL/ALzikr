package com.alzikr.app

import android.app.backup.BackupAgentHelper
import android.app.backup.BackupManager
import android.app.backup.SharedPreferencesBackupHelper
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Android's own backup of the user's progress (levels, points, friendship, prayers, settings: the "alzikr" file).
 * Only that small file goes, so the backup never passes Android's size limit (the downloaded tafsirs and recitations
 * used to make it fail). When the app is installed again, Android puts it back before the first launch.
 */
class AlzikrBackupAgent : BackupAgentHelper() {
    override fun onCreate() { addHelper("prefs", SharedPreferencesBackupHelper(this, "alzikr")) }
}

/**
 * A second copy that does not depend on Google: the user picks a folder once (Documents, say) and the app keeps
 * «ALzikr-backup.json» there up to date by itself. Files in such a folder stay when the app is removed, and on the
 * first launch after installing again the app offers to bring them back.
 */
object AutoBackup {
    const val NAME = "ALzikr-backup.json"
    private const val GAP = 20 * 60_000L
    private fun sp(c: Context) = c.applicationContext.getSharedPreferences("alzikr_backup", Context.MODE_PRIVATE)

    /** Shown once on a fresh install (nothing restored by Android): "Had ALzikr before? Bring your progress back." */
    var offer by mutableStateOf(false)
    var last by mutableStateOf(0L); private set
    var folder by mutableStateOf(""); private set

    /** Call right after [Store.init], before anything writes. */
    fun onLaunch(c: Context) {
        val p = Store.prefs
        val fresh = !p.contains("first_seen") && p.all.size <= 5
        if (!p.contains("first_seen")) p.edit().putLong("first_seen", System.currentTimeMillis()).apply()
        offer = fresh
        last = sp(c).getLong("last", 0L)
        folder = sp(c).getString("name", "") ?: ""
    }

    fun tree(c: Context): Uri? = sp(c).getString("tree", null)?.let(Uri::parse)
        ?.takeIf { t -> c.contentResolver.persistedUriPermissions.any { it.uri == t && it.isWritePermission } }

    fun setTree(c: Context, t: Uri?) {
        if (t == null) { sp(c).edit().remove("tree").remove("name").apply(); folder = ""; return }
        runCatching { c.contentResolver.takePersistableUriPermission(t, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
        val name = runCatching { DocumentsContract.getTreeDocumentId(t).substringAfter(':').ifEmpty { "الذاكرة الداخلية" } }.getOrDefault("")
        sp(c).edit().putString("tree", t.toString()).putString("name", name).apply(); folder = name
    }

    /** Asks Android to back up soon, and refreshes the file in the chosen folder. Blocking: call off the main thread. */
    fun save(c: Context, force: Boolean = false): Boolean {
        runCatching { BackupManager(c).dataChanged() }
        val tree = tree(c) ?: return false
        if (!force && System.currentTimeMillis() - sp(c).getLong("last", 0L) < GAP) return false
        return runCatching {
            val cr = c.contentResolver
            val root = DocumentsContract.getTreeDocumentId(tree)
            var doc: Uri? = null
            cr.query(DocumentsContract.buildChildDocumentsUriUsingTree(tree, root),
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { cur ->
                while (cur.moveToNext()) if (cur.getString(1) == NAME) { doc = DocumentsContract.buildDocumentUriUsingTree(tree, cur.getString(0)); break }
            }
            val target = doc ?: DocumentsContract.createDocument(cr, DocumentsContract.buildDocumentUriUsingTree(tree, root), "application/json", NAME)!!
            cr.openOutputStream(target, "wt")!!.use { it.write(Data.export().toByteArray()) }
            val now = System.currentTimeMillis()
            sp(c).edit().putLong("last", now).apply(); last = now
            true
        }.getOrDefault(false)
    }

    fun restore(c: Context, uri: Uri): Boolean {
        val json = runCatching { c.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() } }.getOrNull()
        val ok = json != null && Data.import(json)
        if (ok) { Store.prefs.edit().putLong("first_seen", System.currentTimeMillis()).apply(); Alerts.reschedule(c); offer = false }
        return ok
    }

    fun lastText(): String = if (last == 0L) "لم تُحفظ بعد" else
        DateTimeFormatter.ofPattern("EEEE d MMMM · h:mm a", Locale("ar")).format(Instant.ofEpochMilli(last).atZone(ZoneId.systemDefault()))
}

/** In «نسخ احتياطي»: the automatic copy. */
@Composable
fun AutoBackupSection() {
    val ctx = LocalContext.current
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { t ->
        if (t == null) return@rememberLauncherForActivityResult
        AutoBackup.setTree(ctx, t)
        Thread { val ok = AutoBackup.save(ctx, force = true); android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(ctx, if (ok) "حُفظت نسختك في «${AutoBackup.folder}»، وستتجدد وحدها" else "تعذّر الحفظ في هذا المجلد، اختر مجلدًا آخر", Toast.LENGTH_LONG).show() } }.start()
    }
    val on = AutoBackup.folder.isNotEmpty() && AutoBackup.tree(ctx) != null
    Title("الحفظ التلقائي")
    Sub("يحفظ Android تقدّمك (المستوى والنقاط والصداقة والصلوات والإعدادات) في نسخة Google الاحتياطية إن كانت مفعّلة في هاتفك، فيعود وحده إذا حذفت التطبيق ثم ثبّته.", size = 12.sp)
    Sub(if (on) "📁 ونسخة في مجلد «${AutoBackup.folder}» تتجدد وحدها كلما استعملت التطبيق: ${AutoBackup.lastText()}"
        else "ولزيادة الأمان اختر مجلدًا (مثل Documents) ليحفظ فيه التطبيق ملف «${AutoBackup.NAME}» ويجدّده وحده. يبقى الملف إذا حذفت التطبيق، وعند تثبيته من جديد يعرض عليك استعادته.",
        size = 12.sp, color = if (on) P.ink else P.muted)
    Action(if (on) "تغيير المجلد" else "اختر مجلد الحفظ التلقائي", Modifier.fillMaxWidth(), filled = !on) { pick.launch(null) }
    if (on) Action("إيقاف الحفظ في المجلد", Modifier.fillMaxWidth(), filled = false) { AutoBackup.setTree(ctx, null) }
}

/** On the first launch of a fresh install: bring back a saved copy. */
@Composable
fun RestoreOffer() {
    if (!AutoBackup.offer) return
    val ctx = LocalContext.current
    val load = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ok = AutoBackup.restore(ctx, uri)
        Toast.makeText(ctx, if (ok) "عاد تقدّمك كما كان" else "هذا الملف ليس نسخة من ALzikr", Toast.LENGTH_LONG).show()
    }
    AlertDialog(
        onDismissRequest = { AutoBackup.offer = false },
        title = { Text("أهلًا بك في ALzikr") },
        text = { Text("هل استعملت ALzikr من قبل؟ إن كان عندك ملف نسخة احتياطية (${AutoBackup.NAME} أو ALzikr-التاريخ.json) فاستعده ليعود مستواك وتقدّمك كما كانا.") },
        confirmButton = { TextButton({ load.launch(arrayOf("application/json", "application/octet-stream", "*/*")) }) { Text("استعادة من ملف") } },
        dismissButton = { TextButton({ AutoBackup.offer = false }) { Text("بداية جديدة") } },
    )
}
