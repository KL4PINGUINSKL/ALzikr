package com.alzikr.app

import android.app.Activity
import android.app.AlertDialog
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
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * The app updates itself, without needing ALhub: on start it reads the public
 * KL4PINGUINSKL/ALapps releases (asset "<App>-<ver>-Android.apk"), and when a newer build exists it
 * asks the user. "Update" downloads the APK and hands it to Android's PackageInstaller (Android asks
 * once more to confirm). "Later" stays quiet about that version for a day.
 * Shared file: the same code lives in ALmouse, ALspeednet, ALtvcontrol and ALphonectrl.
 */
object AppUpdate {
    private const val APP = "ALzikr"
    private const val TAG = "AppUpdate"
    private const val CHECK_EVERY_MS = 60 * 60_000L        // GitHub allows 60 anonymous requests per hour
    private const val LATER_MS = 24 * 60 * 60_000L
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var busy = false

    private data class Release(val version: String, val url: String, val size: Long, val notes: String)

    /** Call from the main activity's onCreate. Quiet when offline, up to date, or checked recently. */
    fun check(activity: Activity) = doCheck(activity, force = false)

    /** "Check for updates" in Settings › About: always asks GitHub, and says when there is nothing new. */
    fun checkNow(activity: Activity) {
        Toast.makeText(activity, "جارٍ البحث عن تحديث…", Toast.LENGTH_SHORT).show()
        doCheck(activity, force = true)
    }

    private fun doCheck(activity: Activity, force: Boolean) {
        val prefs = activity.getSharedPreferences("app_update", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (busy || !force && now - prefs.getLong("checked_at", 0) < CHECK_EVERY_MS) return
        busy = true
        fun say(text: String) { if (force) main.post { Toast.makeText(activity.applicationContext, text, Toast.LENGTH_LONG).show() } }
        Thread {
            try {
                val latest = latest() ?: run { say("تعذّر الوصول إلى GitHub الآن"); return@Thread }
                prefs.edit().putLong("checked_at", now).apply()
                val current = installedVersion(activity) ?: return@Thread
                if (compare(latest.version, current) <= 0) { say("لديك أحدث إصدار ($current)"); return@Thread }
                if (!force && prefs.getString("later_version", null) == latest.version && now - prefs.getLong("later_at", 0) < LATER_MS) return@Thread
                main.post { ask(activity, current, latest) }
            } catch (e: Exception) {
                Log.i(TAG, "check failed: $e"); say("تعذّر البحث عن تحديث")
            } finally { busy = false }
        }.start()
    }

    private fun installedVersion(context: Context): String? = try {
        val pm = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= 33) pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        else @Suppress("DEPRECATION") pm.getPackageInfo(context.packageName, 0)
        info.versionName
    } catch (_: Exception) { null }

    private fun latest(): Release? {
        val c = URL("https://api.github.com/repos/KL4PINGUINSKL/ALapps/releases?per_page=100").openConnection() as HttpURLConnection
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
                    if (best == null || compare(v, best.version) > 0)
                        best = Release(v, a.optString("browser_download_url"), a.optLong("size"), r.optString("body").trim().take(600))
                }
            }
            return best
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

    private fun ask(activity: Activity, current: String, release: Release) {
        if (activity.isFinishing || activity.isDestroyed) return
        val notes = if (release.notes.isNotEmpty()) "\n\n${release.notes}" else ""
        AlertDialog.Builder(activity)
            .setTitle("تحديث متوفر")
            .setMessage("$APP ${release.version} متوفر (لديك $current).$notes")
            .setPositiveButton("تحديث") { _, _ -> start(activity, release) }
            .setNegativeButton("لاحقاً") { _, _ ->
                activity.getSharedPreferences("app_update", Context.MODE_PRIVATE).edit()
                    .putString("later_version", release.version).putLong("later_at", System.currentTimeMillis()).apply()
            }
            .show()
    }

    private fun start(activity: Activity, release: Release) {
        // Android needs a one-time "install unknown apps" permission for this app.
        if (Build.VERSION.SDK_INT >= 26 && !activity.packageManager.canRequestPackageInstalls()) {
            Toast.makeText(activity, "اسمح لـ $APP بتثبيت التحديثات، ثم ارجع واضغط تحديث", Toast.LENGTH_LONG).show()
            activity.getSharedPreferences("app_update", Context.MODE_PRIVATE).edit().remove("checked_at").apply()
            runCatching { activity.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}"))) }
            return
        }
        val pad = (20 * activity.resources.displayMetrics.density).toInt()
        val bar = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100; isIndeterminate = true }
        val label = TextView(activity).apply { text = "جارٍ تنزيل ${release.version}…" }
        val box = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad, pad, pad / 2); addView(label); addView(bar) }
        val dialog = AlertDialog.Builder(activity).setTitle("تحديث $APP").setView(box).setCancelable(false).show()
        val app = activity.applicationContext
        Thread {
            try {
                val file = download(app, release) { f -> main.post { bar.isIndeterminate = false; bar.progress = (f * 100).toInt() } }
                main.post { label.text = "جارٍ التثبيت…" }
                commit(app, file)
                main.post { runCatching { dialog.dismiss() } }
            } catch (e: Exception) {
                Log.w(TAG, "update failed", e)
                main.post {
                    runCatching { dialog.dismiss() }
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
