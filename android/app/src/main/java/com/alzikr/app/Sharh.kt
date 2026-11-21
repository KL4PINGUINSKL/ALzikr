package com.alzikr.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Explained hadith from HadeethEnc.com (the Encyclopedia of Translated Prophetic Hadiths, reviewed by
 * scholars of Ahl as-Sunnah): each hadith with its grade, source, explanation, benefits and word
 * meanings. Categories and every opened hadith are kept on the phone, so they open again offline.
 */
object Sharh {
    data class Category(val id: String, val title: String, val count: Int, val parent: String?)
    data class Brief(val id: String, val title: String)
    data class Page(val items: List<Brief>, val page: Int, val last: Int)
    data class Hadith(
        val id: String, val title: String, val text: String, val attribution: String, val grade: String,
        val explanation: String, val hints: List<String>, val words: List<Pair<String, String>>, val reference: String,
    )

    private const val API = "https://hadeethenc.com/api/v1"
    private fun dir(c: Context) = File(c.filesDir, "sharh").apply { mkdirs() }

    private fun get(url: String): String? = runCatching {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000; conn.readTimeout = 30_000
        conn.setRequestProperty("Accept", "application/json")
        try { if (conn.responseCode == 200) conn.inputStream.bufferedReader().readText() else null } finally { conn.disconnect() }
    }.getOrNull()

    /** All categories (cached; [refresh] asks the site again). Blocking. */
    fun categories(c: Context, refresh: Boolean = false): List<Category> {
        val f = File(dir(c), "categories.json")
        val text = (if (!refresh && f.exists()) f.readText() else null) ?: get("$API/categories/list/?language=ar")?.also { f.writeText(it) } ?: f.takeIf { it.exists() }?.readText() ?: return emptyList()
        val a = JSONArray(text)
        return (0 until a.length()).map { a.getJSONObject(it).let { o ->
            Category(o.getString("id"), o.getString("title"), o.optString("hadeeths_count").toIntOrNull() ?: 0, o.optString("parent_id").takeIf { p -> p.isNotEmpty() && p != "null" })
        } }
    }

    /** One page of a category's hadiths (cached per page). Blocking. */
    fun list(c: Context, category: String, page: Int, refresh: Boolean = false): Page? {
        val f = File(dir(c), "list_${category}_$page.json")
        val text = (if (!refresh && f.exists()) f.readText() else null)
            ?: get("$API/hadeeths/list/?language=ar&category_id=${URLEncoder.encode(category, "UTF-8")}&page=$page&per_page=30")?.also { f.writeText(it) }
            ?: f.takeIf { it.exists() }?.readText() ?: return null
        val o = JSONObject(text)
        val a = o.getJSONArray("data")
        val meta = o.optJSONObject("meta")
        return Page((0 until a.length()).map { a.getJSONObject(it).let { h -> Brief(h.getString("id"), h.getString("title")) } },
            meta?.optString("current_page")?.toIntOrNull() ?: page, meta?.optInt("last_page") ?: page)
    }

    fun cached(c: Context, id: String) = File(dir(c), "h_$id.json").exists()

    /** One hadith with its explanation (cached forever once opened). Blocking. */
    fun hadith(c: Context, id: String): Hadith? {
        val f = File(dir(c), "h_$id.json")
        val text = (if (f.exists()) f.readText() else null) ?: get("$API/hadeeths/one/?language=ar&id=$id")?.also { f.writeText(it) } ?: return null
        val o = JSONObject(text)
        fun strings(key: String): List<String> = o.optJSONArray(key)?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList()
        val words = o.optJSONArray("words_meanings")?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it) }.map { w -> w.optString("word") to w.optString("meaning") } } ?: emptyList()
        return Hadith(o.getString("id"), o.optString("title"), o.optString("hadeeth"), o.optString("attribution"), o.optString("grade"),
            o.optString("explanation"), strings("hints").filter { it.isNotBlank() }, words, o.optString("reference"))
    }

    /** Everything already opened, for searching without the internet. */
    fun opened(c: Context): List<Brief> = dir(c).listFiles { f -> f.name.startsWith("h_") }?.mapNotNull { f ->
        runCatching { JSONObject(f.readText()).let { Brief(it.getString("id"), it.optString("title")) } }.getOrNull()
    } ?: emptyList()

    fun clearCache(c: Context) { dir(c).listFiles { f -> f.name.startsWith("list_") || f.name == "categories.json" }?.forEach { it.delete() } }

    fun isFavorite(id: String) = id in (Store.prefs.getStringSet("sharh_fav", emptySet()) ?: emptySet())
    fun toggleFavorite(id: String) {
        val set = (Store.prefs.getStringSet("sharh_fav", emptySet()) ?: emptySet()).toMutableSet()
        if (!set.remove(id)) set += id
        Store.prefs.edit().putStringSet("sharh_fav", set).apply(); Store.bump()
    }
    fun favorites(): Set<String> = Store.prefs.getStringSet("sharh_fav", emptySet()) ?: emptySet()
}
