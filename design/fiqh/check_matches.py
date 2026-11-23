"""Runs every hadith in Fiqh.kt through the app's matching against the HadeethEnc index (pulled from a device)
and lists what it finds, so a wrong or missing match can be fixed with an explicit key."""
import json, os, re
os.chdir(os.path.dirname(os.path.abspath(__file__)))
s = open(r'D:\ALzikr\android\app\src\main\java\com\alzikr\app\Fiqh.kt', encoding='utf-8').read()
index = json.load(open('hadeethenc_index.json', encoding='utf-8'))  # [[id, title], ...]

def norm(t):
    t = re.sub('[\u0610-\u061A\u064B-\u065F\u0670\u06D6-\u06ED\u08D3-\u08FF\u0640]', '', t)
    t = t.replace('ٱ', 'ا'); t = re.sub('[آأإ]', 'ا', t)
    t = t.replace('ى', 'ي').replace('ی', 'ي').replace('ة', 'ه').replace('ک', 'ك')
    return re.sub(r'\s+', ' ', t).strip()
FILL = {"حديث", "الحديث", "احاديث", "الاحاديث", "ابحث", "عن", "نص", "شرح", "رقم"}
def words(q):
    a = [w for w in re.sub('[«»"\'()،,.؟?!:]', ' ', norm(q)).split(' ') if len(w) >= 2]
    return [w for w in a if w not in FILL] or a
STOP = {"قال", "رسول", "الله", "صلى", "عليه", "وسلم", "عن", "في", "من", "ان", "ما", "لا", "اذا", "ثم", "او", "على"}
def key_of(text, key):
    if key: return key
    return ' '.join([w for w in re.sub('[^\u0621-\u064A\\s]', ' ', norm(text)).split() if len(w) >= 2 and w not in STOP][:4])
titles = [(i, norm(t)) for i, t in index]
def search(q):
    w = words(q)
    return [(i, t) for i, t in titles if w and all(x in t for x in w)][:8]

seen = set(); found = missing = 0
for m in re.finditer(r'\bh\("([^"]*)", "([^"]*)", "([^"]*)"(?:, "([^"]*)")?\)', s):
    text, by, src, key = m.group(1), m.group(2), m.group(3), m.group(4) or ''
    if text in seen: continue
    seen.add(text)
    k = key_of(text, key); ws = k.split()
    hits = []
    for n in range(len(ws), (len(ws) if key else min(3, len(ws))) - 1, -1):
        hits = search(' '.join(ws[:n]))
        if hits: break
    if hits: found += 1
    else: missing += 1
    print(('OK ' if hits else '-- ') + text[:50], '| key:', k, '|', (hits[0][1][:70] if hits else ''), f'(+{len(hits) - 1})' if len(hits) > 1 else '')
print('found', found, 'missing', missing)
