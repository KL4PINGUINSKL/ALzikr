"""Puts design/fiqh/madhahib.py into Fiqh.kt: each school's evidence, reasoning and the preferred view."""
import os, re, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from madhahib import ROWS

KT = r'D:\ALzikr\android\app\src\main\java\com\alzikr\app\Fiqh.kt'
QURAN = r'D:\ALzikr\android\app\src\main\assets\quran.txt'
s = open(KT, encoding='utf-8').read()

verses = {}
for line in open(QURAN, encoding='utf-8'):
    if line.strip():
        p = line.rstrip('\n').split('\t'); verses[(int(p[0]), int(p[1]))] = p[6]
def norm(t):
    t = re.sub('[\u0610-\u061A\u064B-\u065F\u0670\u06D6-\u06ED\u0640…]', '', t); t = t.replace('ٱ', 'ا'); t = re.sub('[آأإ]', 'ا', t)
    return re.sub('[ءؤئ]', '', t.replace('ى', 'ي').replace('ة', 'ه')).replace('وا', 'وا')

def kq(x): return '"' + x.replace('"', '\\"') + '"'
def ev(e):
    if e[0] == 'q':
        words = norm(e[1]).split()[:3]
        if not all(w in norm(verses[(e[2], e[3])]) for w in words): raise SystemExit(f'verse mismatch {e}')
        return f'q({kq(e[1])}, {e[2]}, {e[3]})'
    return f'{"h" if e[0] == "h" else "at"}({kq(e[1])}, {kq(e[2])}, {kq(e[3])})'

calls = list(re.finditer(r'mz = mz\(("[^"]*"),\s*("[^"]*"),\s*("[^"]*"),\s*("[^"]*")\)', s))
assert len(calls) == len(ROWS) == 84, (len(calls), len(ROWS))
out, last = [], 0
for k, (m, row) in enumerate(zip(calls, ROWS)):
    start = s.rfind('\n                i("', 0, m.start())
    n_ev = len(re.findall(r'\b(?:h|q|at|ij)\("', s[start:m.start()]))
    sides = []
    for j, side in enumerate(row[:4]):
        for u in side['use']:
            if u >= n_ev: raise SystemExit(f'row {k} side {j}: evidence {u} but item has {n_ev}')
        use = 'listOf(' + ', '.join(map(str, side['use'])) + ')'
        evs = 'listOf(' + ', '.join(ev(e) for e in side['ev']) + ')'
        sides.append(f'sd({m.group(j + 1)}, {use}, {evs}, {kq(side["wajh"])})')
    rajih = row[4]
    out.append(s[last:m.start()])
    out.append('mz = Mz(' + ', '.join(sides) + (f', rajih = {kq(rajih)}' if rajih else '') + ')')
    last = m.end()
out.append(s[last:])
s = ''.join(out)

# The model: a side per school, and the helper.
s = s.replace('''    /** The four schools: Hanafi, Maliki, Shafi'i, Hanbali. */
    data class Mz(val hanafi: String, val maliki: String, val shafii: String, val hanbali: String)''',
'''    /**
     * One school's answer: its position [q], the item's own evidence it relies on (indices in [use]),
     * its further evidence [ev], and how it reasons from them [wajh].
     */
    data class Side(val q: String, val use: List<Int> = emptyList(), val ev: List<Ev> = emptyList(), val wajh: String = "")
    /** The four schools: Hanafi, Maliki, Shafi'i, Hanbali, and the view preferred by the verifying scholars, if stated. */
    data class Mz(val hanafi: Side, val maliki: Side, val shafii: Side, val hanbali: Side, val rajih: String = "") {
        val sides get() = listOf(hanafi, maliki, shafii, hanbali)
    }''')
s = s.replace('''    private fun mz(h: String, m: String, s: String, hb: String) = Mz(h, m, s, hb)''',
'''    private fun sd(q: String, use: List<Int>, ev: List<Ev>, wajh: String) = Side(q, use, ev, wajh)''')
s = s.replace('''(h.item.mz?.let { "${it.hanafi} ${it.maliki} ${it.shafii} ${it.hanbali}" } ?: "")''',
              '''(h.item.mz?.sides?.joinToString(" ") { it.q } ?: "")''')
open(KT, 'w', encoding='utf-8', newline='').write(s)
print('merged', len(ROWS))
