package com.alzikr.app

import java.time.LocalDate

/**
 * Picks for the day, random for each person, and nothing comes back before everything else has come once.
 *
 * - [pick] (small lists by id: tasks, challenges, questions): a shuffle bag. Every item once in a random order, then
 *   the bag is refilled and shuffled anew. New items added later simply join the next bag.
 * - [index] (large lists: thousands of verses or hadiths): a full-cycle random order. Each person gets their own random
 *   start and stride (coprime with the list size), and the sequence moves one step a day (and one more for each
 *   «another»), so every item comes once before any repeats.
 *
 * Today's choice stays the same for the whole day.
 */
object DailyBag {
    private val sp get() = Store.prefs
    private const val SEP = "\u0001"
    private fun today() = LocalDate.now().toString()

    fun pick(kind: String, ids: List<String>, n: Int): List<String> {
        if (ids.isEmpty()) return emptyList()
        val d = today()
        if (sp.getString("bag_${kind}_day", "") == d) sp.getString("bag_${kind}_picks", "")!!.split(SEP).filter { it in ids }.takeIf { it.size == minOf(n, ids.size) }?.let { return it }
        val left = sp.getString("bag_${kind}_left", null)?.split(SEP)?.filter { it in ids }?.toMutableList() ?: mutableListOf()
        val picks = mutableListOf<String>()
        while (picks.size < minOf(n, ids.size)) {
            if (left.isEmpty()) left += ids.shuffled().filter { it !in picks }
            picks += left.removeAt(0)
        }
        sp.edit().putString("bag_${kind}_day", d).putString("bag_${kind}_picks", picks.joinToString(SEP)).putString("bag_${kind}_left", left.joinToString(SEP)).apply()
        return picks
    }

    /** The day's place in [kind]'s sequence: one step each new day. */
    private fun seq(kind: String): Long {
        val d = today()
        var s = sp.getLong("seq_$kind", -1L)
        if (sp.getString("seq_${kind}_day", "") != d) { s += 1; sp.edit().putLong("seq_$kind", s).putString("seq_${kind}_day", d).apply() }
        return s
    }
    /** «Another one»: the next in the sequence (it counts as seen). */
    fun next(kind: String) { seq(kind); sp.edit().putLong("seq_$kind", sp.getLong("seq_$kind", 0L) + 1).apply() }

    private fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)
    /** Today's index in a list of [size] items. */
    fun index(kind: String, size: Int): Int {
        if (size <= 1) return 0
        val n = size.toLong()
        var seed = sp.getLong("seed_$kind", 0L)
        if (seed == 0L) { seed = (1L..Long.MAX_VALUE / 4).random(); sp.edit().putLong("seed_$kind", seed).apply() }
        val start = Math.floorMod(seed, n)
        // A stride coprime with the size (from the seed), so the walk visits every item once per cycle.
        var stride = Math.floorMod(seed / 7919, n).coerceAtLeast(1L)
        while (gcd(stride, n) != 1L) stride = if (stride + 1 >= n) 1L else stride + 1
        return Math.floorMod(start + seq(kind) * stride, n).toInt()
    }
}
