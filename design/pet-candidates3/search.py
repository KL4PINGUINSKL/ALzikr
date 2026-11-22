"""More designs for the app's animals, leaving out everything shown before (2026-11-22)."""
import json, os, re, subprocess
from concurrent.futures import ThreadPoolExecutor
os.chdir(os.path.dirname(os.path.abspath(__file__)))
terms = {
    'cat': ['cat', 'kitten', 'cute-cat', 'cat-walking', 'cat-sleeping', 'kitty'],
    'chicken': ['chicken', 'hen', 'chick', 'baby-chick', 'rooster'],
    'penguin': ['penguin', 'cute-penguin', 'baby-penguin'],
    'rabbit': ['rabbit', 'bunny', 'cute-bunny'],
    'sheep': ['sheep', 'lamb', 'cute-sheep'],
    'bird': ['bird', 'cute-bird', 'flying-bird', 'swallow', 'seagull'],
    'cow': ['cute-cow', 'cartoon-cow', 'cow-animation', 'dairy-cow', 'calf'],
    'elephant': ['cute-elephant', 'baby-elephant', 'elephant-animation', 'cartoon-elephant'],
    'giraffe': ['cute-giraffe', 'baby-giraffe', 'giraffe-animation', 'cartoon-giraffe'],
}
pat = re.compile(r'"contentUrl":"(https://assets-v2\.lottiefiles\.com/a/[^"]+\.gif)","name":"([^"]*)".*?"acquireLicensePage":"([^"]+)".*?"creator":\{"@type":"Person","name":"([^"]*)"')
seen = set()
for x in json.load(open('../pet-candidates/gallery.json', encoding='utf-8')):
    for it in x['items']: seen.add(it['page'])
for k, v in json.load(open('../pet-candidates2/search.json', encoding='utf-8')).items():
    for it in v: seen.add(it['page'])


def fetch(slug):
    html = subprocess.run(['curl', '-sL', '-A', 'Mozilla/5.0', f'https://lottiefiles.com/free-animations/{slug}'], capture_output=True).stdout.decode('utf-8', 'ignore')
    return slug, pat.findall(html.replace('\\"', '"').replace('\\u002F', '/'))


jobs = [(sp, t) for sp, ts in terms.items() for t in ts]
with ThreadPoolExecutor(8) as ex:
    res = dict(ex.map(lambda j: fetch(j[1]), jobs))
out = {}
for sp, ts in terms.items():
    items = []
    for t in ts:
        for g, n, p, by in res[t]:
            if p in seen: continue
            seen.add(p); items.append({'name': n, 'gif': g, 'page': p, 'by': by, 'term': t})
    out[sp] = items
    print(sp, len(items), ' | '.join(i['name'] for i in items))
json.dump(out, open('search.json', 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
