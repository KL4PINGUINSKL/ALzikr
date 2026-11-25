package com.alzikr.app

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import org.json.JSONArray
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * The app updates itself, without needing ALhub: on start it reads ALzikr's own site on Cloudflare
 * (https://alzikr.alpharmasy.com/latest.json, reachable where GitHub is not, e.g. Syria) and the public
 * KL4PINGUINSKL/ALapps releases (asset "<App>-<ver>-Android.apk"), and when a newer build exists it
 * asks the user. "Update" downloads the APK and hands it to Android's PackageInstaller (Android asks
 * once more to confirm). "Later" stays quiet about that version for a day.
 * Shared file: the same code lives in ALmouse, ALspeednet, ALtvcontrol and ALphonectrl.
 */
object AppUpdate {
    private const val APP = "ALzikr"
    private const val TAG = "AppUpdate"
    private const val CHECK_EVERY_MS = 20 * 60_000L        // GitHub allows 60 anonymous requests per hour
    private const val LATER_MS = 24 * 60 * 60_000L
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var busy = false

    private data class Release(val version: String, val url: String, val size: Long, val notes: String, val sha256: String = "", val mirror: String = "")
    private const val SITE = "https://alzikr.alpharmasy.com/latest.json"

    /** The offer on screen (drawn in the app's theme by UpdateDialog): your version, the new one, and what is new in each version since yours. */
    data class Offer(val current: String, val version: String, val news: List<Pair<String, List<String>>>, val required: Boolean = false)
    var offer by androidx.compose.runtime.mutableStateOf<Offer?>(null)
        private set
    /** While updating: null = not updating, -1 = waiting, 0..1 = downloaded part. */
    var progress by androidx.compose.runtime.mutableStateOf<Float?>(null)
        private set
    var stage by androidx.compose.runtime.mutableStateOf("")
        private set
    private var pending: Release? = null
    /** Every release of this app seen in the last check, newest first (for «ما الجديد»). */
    private var seen: List<Release> = emptyList()

    /** A release's notes as short lines (links and empty lines left out). */
    private fun lines(body: String): List<String> = body.lines().map { it.trim().trimStart('-', '•', '*', ' ') }
        .filter { it.isNotBlank() && !it.contains("http") && !it.startsWith("رابط") && !isRequiredMark(it) }
    /** A release whose notes hold «[إلزامي]» (or "[required]") cannot be put off: everyone moves past the versions before it. */
    private fun isRequiredMark(s: String) = s.contains("[إلزامي]") || s.contains("[required]", ignoreCase = true)
    private fun required(r: Release) = r.notes.lines().any { isRequiredMark(it) }

    fun accept(activity: Activity) { val r = pending ?: return; offer = null; start(activity, r) }
    fun later(context: Context) {
        val r = pending ?: return
        if (offer?.required == true) return
        context.getSharedPreferences("app_update", Context.MODE_PRIVATE).edit().putString("later_version", r.version).putLong("later_at", System.currentTimeMillis()).apply()
        offer = null
    }

    /** Call from the main activity's onResume. Quiet when offline, up to date, or checked recently. */
    fun check(activity: Activity) = doCheck(activity, force = false)

    /** Called on the main thread when a newer version becomes known (to show a banner). */
    var onFound: (() -> Unit)? = null

    /** The newer version known from the last check, or null (also after updating). */
    fun available(context: Context): String? {
        val v = context.getSharedPreferences("app_update", Context.MODE_PRIVATE).getString("latest_version", null) ?: return null
        val cur = installedVersion(context) ?: return null
        return v.takeIf { compare(it, cur) > 0 }
    }

    /** "Check for updates" in Settings › About: always asks the update servers (Cloudflare and GitHub), and says when there is nothing new. */
    fun checkNow(activity: Activity) {
        Toast.makeText(activity, "جارٍ البحث عن تحديث…", Toast.LENGTH_SHORT).show()
        doCheck(activity, force = true)
    }

    /** Automatic retries left after a failed check (weak internet), one a minute while the app stays open. */
    private var retries = 0
    private const val RETRY_MS = 60_000L

    private fun doCheck(activity: Activity, force: Boolean, retry: Boolean = false) {
        val prefs = activity.getSharedPreferences("app_update", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (busy || !force && !retry && now - prefs.getLong("checked_at", 0) < CHECK_EVERY_MS) return
        if (!retry) retries = 3
        busy = true
        fun say(text: String) { if (force) main.post { Toast.makeText(activity.applicationContext, text, Toast.LENGTH_LONG).show() } }
        // A check counts as done (and waits CHECK_EVERY_MS before the next) only once its answer was given: up to date,
        // put off with «later», or the offer actually on screen. A failed or unseen check is tried again soon.
        fun done() = prefs.edit().putLong("checked_at", now).apply()
        fun again() {
            if (force || retries <= 0) return
            retries--
            main.postDelayed({ if (!activity.isFinishing && !activity.isDestroyed) doCheck(activity, force = false, retry = true) }, RETRY_MS)
        }
        Thread {
            try {
                val latest = latest() ?: run { say("تعذّر الوصول إلى خادم التحديثات الآن"); again(); return@Thread }
                prefs.edit().putString("latest_version", latest.version).apply()
                main.post { onFound?.invoke() }
                val current = installedVersion(activity) ?: return@Thread
                if (compare(latest.version, current) <= 0) { done(); say("لديك أحدث إصدار ($current)"); return@Thread }
                val must = seen.any { compare(it.version, current) > 0 && required(it) }
                if (!must && !force && prefs.getString("later_version", null) == latest.version && now - prefs.getLong("later_at", 0) < LATER_MS) { done(); return@Thread }
                main.post { if (ask(activity, current, latest)) done() }
            } catch (e: Exception) {
                Log.i(TAG, "check failed: $e"); say("تعذّر البحث عن تحديث"); again()
            } finally { busy = false }
        }.start()
    }

    private fun installedVersion(context: Context): String? = try {
        val pm = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= 33) pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        else @Suppress("DEPRECATION") pm.getPackageInfo(context.packageName, 0)
        info.versionName
    } catch (_: Exception) { null }

    /** The newest release of this app: Cloudflare's latest.json and every page of ALapps releases (either may be unreachable). */
    private fun latest(): Release? {
        all.clear()
        val site = runCatching { fromSite() }.onFailure { Log.i(TAG, "site: $it") }.getOrNull()
        runCatching {
            for (page in 1..10) {
                val (_, count) = latestOn(page) ?: break
                if (count < 100) break
            }
        }.onFailure { Log.i(TAG, "github: $it") }
        val github = all.toList()
        all.clear()
        // One entry per version; the Cloudflare file is preferred for downloading, GitHub's copy is the fallback.
        val merged = LinkedHashMap<String, Release>()
        for (r in site.orEmpty()) merged[r.version] = r
        for (g in github) {
            val c = merged[g.version]
            merged[g.version] = if (c == null) g else c.copy(notes = c.notes.ifBlank { g.notes }, mirror = g.url)
        }
        if (merged.isEmpty()) return null
        seen = merged.values.sortedWith { a, b -> compare(b.version, a.version) }
        return seen.firstOrNull { it.url.isNotEmpty() }
    }

    /** latest.json on alzikr.alpharmasy.com: the newest APK, and the notes of recent versions. */
    private fun fromSite(): List<Release>? {
        val c = URL(SITE).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000; c.readTimeout = 15_000; c.useCaches = false
        c.setRequestProperty("User-Agent", APP); c.setRequestProperty("Cache-Control", "no-cache")
        try {
            if (c.responseCode != 200) { Log.i(TAG, "site http=${c.responseCode}"); return null }
            val j = org.json.JSONObject(c.inputStream.bufferedReader().use { it.readText() })
            val version = j.getString("version")
            val out = ArrayList<Release>()
            val rels = j.optJSONArray("releases") ?: JSONArray()
            for (i in 0 until rels.length()) {
                val r = rels.getJSONObject(i)
                val notes = r.optJSONArray("notes")?.let { a -> (0 until a.length()).joinToString("\n") { "- " + a.getString(it) } } ?: ""
                out += Release(r.getString("version"), "", 0, notes)
            }
            val top = Release(version, j.getString("downloadUrl"), j.optLong("size"), out.firstOrNull { it.version == version }?.notes ?: "", j.optString("sha256"))
            out.removeAll { it.version == version }; out.add(0, top)
            return out
        } finally { c.disconnect() }
    }
    private val all = ArrayList<Release>()

    /** One page: the best release on it and how many releases the page held; null on a network error. */
    private fun latestOn(page: Int): Pair<Release?, Int>? {
        val c = URL("https://api.github.com/repos/KL4PINGUINSKL/ALapps/releases?per_page=100&page=$page").openConnection() as HttpURLConnection
        c.connectTimeout = 10_000; c.readTimeout = 15_000
        c.setRequestProperty("Accept", "application/vnd.github+json"); c.setRequestProperty("User-Agent", APP)
        try {
            if (c.responseCode != 200) { Log.i(TAG, "releases http=${c.responseCode}"); return null }
            val list = JSONArray(c.inputStream.bufferedReader().use { it.readText() })
            val pattern = Regex("^${Regex.escape(APP)}-(\\d+(?:\\.\\d+)*)-Android\\.apk$", RegexOption.IGNORE_CASE)
            var best: Release? = null
            for (i in 0 until list.length()) {
                val r = list.getJSONObject(i)
                if (r.optBoolean("draft") || r.optBoolean("prerelease")) continue
                val assets = r.optJSONArray("assets") ?: continue
                for (j in 0 until assets.length()) {
                    val a = assets.getJSONObject(j)
                    val v = pattern.find(a.optString("name"))?.groupValues?.get(1) ?: continue
                    val rel = Release(v, a.optString("browser_download_url"), a.optLong("size"), r.optString("body").trim().take(1500))
                    all += rel
                    if (best == null || compare(v, best.version) > 0) best = rel
                }
            }
            return best to list.length()
        } finally { c.disconnect() }
    }

    private fun compare(a: String, b: String): Int {
        val x = a.split('.', '-', '+').map { it.toIntOrNull() ?: 0 }
        val y = b.split('.', '-', '+').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(x.size, y.size)) {
            val c = x.getOrElse(i) { 0 }.compareTo(y.getOrElse(i) { 0 })
            if (c != 0) return c
        }
        return 0
    }

    /** Puts the offer on screen; false when the activity is already gone (then it is asked again next time). */
    private fun ask(activity: Activity, current: String, release: Release): Boolean {
        if (activity.isFinishing || activity.isDestroyed) return false
        pending = release
        val news = seen.filter { compare(it.version, current) > 0 }.distinctBy { it.version }.take(8).map { it.version to lines(it.notes) }.filter { it.second.isNotEmpty() }
        val must = seen.any { compare(it.version, current) > 0 && required(it) } || required(release)
        offer = Offer(current, release.version, news.ifEmpty { listOf(release.version to lines(release.notes)) }, must)
        return true
    }

    private fun start(activity: Activity, release: Release) {
        // Android needs a one-time "install unknown apps" permission for this app.
        if (Build.VERSION.SDK_INT >= 26 && !activity.packageManager.canRequestPackageInstalls()) {
            Toast.makeText(activity, "اسمح لـ $APP بتثبيت التحديثات، ثم ارجع واضغط تحديث", Toast.LENGTH_LONG).show()
            activity.getSharedPreferences("app_update", Context.MODE_PRIVATE).edit().remove("checked_at").apply()
            runCatching { activity.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}"))) }
            return
        }
        stage = "جارٍ تنزيل ${release.version}…"; progress = -1f
        val app = activity.applicationContext
        Thread {
            try {
                val file = try { download(app, release) { f -> main.post { progress = f } } }
                    catch (e: Exception) {
                        if (release.mirror.isEmpty()) throw e
                        Log.i(TAG, "main download failed ($e), trying the mirror")
                        download(app, release.copy(url = release.mirror, sha256 = "")) { f -> main.post { progress = f } }
                    }
                main.post { stage = "جارٍ التثبيت…"; progress = -1f }
                commit(app, file)
                main.post { progress = null }
            } catch (e: Exception) {
                Log.w(TAG, "update failed", e)
                main.post {
                    progress = null
                    Toast.makeText(app, "تعذّر التحديث: ${e.message ?: e.javaClass.simpleName}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun download(context: Context, release: Release, progress: (Float) -> Unit): File {
        val dir = File(context.cacheDir, "update").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val out = File(dir, "$APP-${release.version}.apk")
        var url = URL(release.url)
        var c: HttpURLConnection
        var hops = 0
        while (true) {   // GitHub redirects to its download host
            c = url.openConnection() as HttpURLConnection
            c.instanceFollowRedirects = false; c.connectTimeout = 15_000; c.readTimeout = 30_000; c.setRequestProperty("User-Agent", APP)
            val code = c.responseCode
            if (code in 300..399 && hops++ < 5) { url = URL(url, c.getHeaderField("Location")); c.disconnect(); continue }
            if (code != 200) { c.disconnect(); throw IllegalStateException("التنزيل فشل ($code)") }
            break
        }
        val total = c.contentLengthLong.takeIf { it > 0 } ?: release.size.takeIf { it > 0 }
        var done = 0L; var last = 0L
        try {
            c.inputStream.use { input -> out.outputStream().use { output ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(buf); if (n < 0) break
                    output.write(buf, 0, n); done += n
                    if (total != null && done - last > 256 * 1024) { last = done; progress(done.toFloat() / total) }
                }
            } }
        } finally { c.disconnect() }
        if (total != null && done != total) throw IllegalStateException("التنزيل لم يكتمل")
        if (release.sha256.isNotEmpty()) {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            out.inputStream().use { i -> val b = ByteArray(64 * 1024); while (true) { val n = i.read(b); if (n < 0) break; md.update(b, 0, n) } }
            if (md.digest().joinToString("") { "%02x".format(it) } != release.sha256.lowercase()) { out.delete(); throw IllegalStateException("الملف المنزَّل تالف، حاول مرة أخرى") }
        }
        return out
    }

    private fun commit(context: Context, file: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            if (Build.VERSION.SDK_INT >= 31) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        val id = installer.createSession(params)
        installer.openSession(id).use { session ->
            file.inputStream().use { input -> session.openWrite("base.apk", 0, file.length()).use { out -> input.copyTo(out); session.fsync(out) } }
            val intent = Intent(context, AppUpdateReceiver::class.java)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
            session.commit(PendingIntent.getBroadcast(context, id, intent, flags).intentSender)
        }
    }
}

/** PackageInstaller's answer for a self-update: show Android's confirm screen, or report a failure. */
class AppUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION") val confirm = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java) else intent.getParcelableExtra(Intent.EXTRA_INTENT)
                confirm?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
            PackageInstaller.STATUS_SUCCESS, PackageInstaller.STATUS_FAILURE_ABORTED -> {}
            else -> Toast.makeText(context, "تعذّر التحديث: " + (intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "خطأ في التثبيت"), Toast.LENGTH_LONG).show()
        }
    }
}
