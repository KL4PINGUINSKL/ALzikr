"""New designs for every animal (2026-11-24), for the looks opened at advanced levels.
LottieFiles GraphQL search (paged), leaving out every design shown in earlier galleries (matched by the GIF asset id)."""
import json, os, re, subprocess, glob
from concurrent.futures import ThreadPoolExecutor
os.chdir(os.path.dirname(os.path.abspath(__file__)))
WANT = 20
terms = {
    'cat': ['cute cat', 'kitten', 'cat character', 'cat playing', 'cat cartoon'],
    'penguin': ['penguin', 'cute penguin', 'penguin character'],
    'seabird': ['seagull', 'albatross', 'seabird'],
    'swallow': ['swallow bird', 'flying bird cartoon', 'cute bird flying'],
    'rabbit': ['bunny', 'cute rabbit', 'rabbit character'],
    'sheep': ['sheep', 'cute sheep', 'lamb'],
    'chicken': ['chicken', 'cute chick', 'hen cartoon'],
    'cow': ['cow', 'cute cow', 'cow cartoon'],
    'elephant': ['elephant', 'cute elephant', 'baby elephant'],
    'giraffe': ['giraffe', 'cute giraffe'],
    'bird': ['cute bird', 'bird character', 'little bird'],
    'bear': ['cute bear', 'teddy bear', 'bear character'],
    'panda': ['panda', 'cute panda', 'panda character'],
}
must = {'cat': r'cat|kitt|kitty|meow', 'penguin': r'pengu', 'seabird': r'gull|albatros|sea ?bird', 'swallow': r'swallow|bird',
        'rabbit': r'rabbit|bunny|hare', 'sheep': r'sheep|lamb', 'chicken': r'chick|hen|rooster', 'cow': r'\bcow|calf|cattle',
        'elephant': r'elephant', 'giraffe': r'giraf', 'bird': r'bird|sparrow|robin|owl', 'bear': r'bear|teddy', 'panda': r'panda'}
bad = re.compile(r'404|loading|loader|load|icon|logo|valentine|halloween|christmas|xmas|button|emoji|sticker|text|banner|splash|menu|ui\b|app\b|game over|error|success|fail|spinner|progress|scroll|cursor|arrow|music', re.I)

def aid(url):
    m = re.search(r'/a/([0-9a-f-]+)/', url or ''); return m.group(1) if m else url
seen = set()
for f in glob.glob('../pet-candidates*/gallery.json') + glob.glob('../pet-candidates*/search.json'):
    if 'candidates5' in f: continue
    d = json.load(open(f, encoding='utf-8'))
    xs = d if isinstance(d, list) else [y for v in (d.values() if isinstance(d, dict) else []) for y in (v if isinstance(v, list) else v.get('items', []))]
    if isinstance(d, list):
        xs = [y for x in d for y in (x.get('items') or [x])]
    for it in xs:
        if isinstance(it, dict):
            seen.add(aid(it.get('gif'))); seen.add(it.get('page'))
print('seen', len(seen))

Q = 'query($q:String!,$f:Int,$a:String){searchPublicAnimations(query:$q,first:$f,after:$a){pageInfo{hasNextPage endCursor} edges{node{id name gifUrl url createdBy{firstName lastName username}}}}}'
def search(q, pages=3):
    out, after = [], None
    for _ in range(pages):
        body = json.dumps({'query': Q, 'variables': {'q': q, 'f': 50, 'a': after}})
        raw = subprocess.run(['curl', '-s', '-X', 'POST', 'https://graphql.lottiefiles.com/2022-08', '-H', 'content-type: application/json', '-A', 'Mozilla/5.0', '--data-binary', '@-'],
                             input=body.encode(), capture_output=True).stdout
        try: r = json.loads(raw)['data']['searchPublicAnimations']
        except Exception: break
        out += [e['node'] for e in r['edges']]
        if not r['pageInfo']['hasNextPage']: break
        after = r['pageInfo']['endCursor']
    return q, out

jobs = [t for ts in terms.values() for t in ts]
with ThreadPoolExecutor(6) as ex: res = dict(ex.map(search, jobs))
out = {}
for sp, ts in terms.items():
    items = []
    for t in ts:
        for n in res.get(t, []):
            name = n['name'] or ''
            if not n.get('gifUrl') or bad.search(name) or not re.search(must[sp], name, re.I): continue
            a = aid(n['gifUrl'])
            if a in seen or n['url'] in seen: continue
            seen.add(a)
            by = ' '.join(x for x in [(n['createdBy'] or {}).get('firstName'), (n['createdBy'] or {}).get('lastName')] if x) or (n['createdBy'] or {}).get('username', '')
            items.append({'name': name, 'gif': n['gifUrl'], 'page': n['url'], 'by': by.strip(), 'term': t, 'id': n['id']})
            if len(items) >= WANT: break
        if len(items) >= WANT: break
    out[sp] = items
    print(sp, len(items))
json.dump(out, open('search.json', 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
