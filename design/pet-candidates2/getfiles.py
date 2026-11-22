"""Downloads the user's picks (2026-11-22) as plain Lottie JSON with any images embedded, and checks the license."""
import base64, io, json, os, re, subprocess, zipfile
os.chdir(os.path.dirname(os.path.abspath(__file__)))
os.makedirs('anim', exist_ok=True)
picks = ['cow-7', 'cow-6', 'cow-5', 'cow-4', 'cow-2', 'cow-1', 'cow-13', 'elephant-2', 'elephant-3', 'elephant-5',
         'giraffe-1', 'giraffe-2', 'giraffe-3', 'giraffe-4', 'giraffe-7', 'giraffe-13', 'giraffe-9']
gal = {x['code']: x for x in json.load(open('gallery.json', encoding='utf-8'))['new']}
report = []


def get(u):
    return subprocess.run(['curl', '-sL', '-A', 'Mozilla/5.0', u], capture_output=True).stdout


for code in picks:
    it = gal[code]
    html = get(it['page']).decode('utf-8', 'ignore').replace('\\"', '"').replace('\\u002F', '/')
    lic = 'Lottie Simple License' if 'Lottie Simple License' in html else ('license page' if '/page/license' in html else '?')
    own = re.search(r'/a/([0-9a-f-]+)/', it['gif']).group(1)
    urls = [u for u in dict.fromkeys(re.findall(r'https://assets-v2\.lottiefiles\.com/a/[0-9a-f-]+/[A-Za-z0-9]+\.(?:json|lottie)', html)) if own in u]
    ok = None
    for u in urls:
        data = get(u)
        try:
            if data[:2] == b'PK':
                z = zipfile.ZipFile(io.BytesIO(data)); names = z.namelist()
                js = json.loads(z.read([n for n in names if n.startswith('animations/') and n.endswith('.json')][0]))
                for a in js.get('assets', []):
                    if 'p' in a and not str(a['p']).startswith('data:'):
                        cand = [x for x in names if x.endswith('/' + a['p']) or x == a['p']]
                        if not cand: continue
                        raw = z.read(cand[0]); ext = 'png' if raw[:4] == b'\x89PNG' else ('webp' if raw[8:12] == b'WEBP' else 'jpeg')
                        a['p'] = f'data:image/{ext};base64,' + base64.b64encode(raw).decode(); a['u'] = ''; a['e'] = 1
            else:
                js = json.loads(data)
            out = f'anim/{code}.json'
            json.dump(js, open(out, 'w', encoding='utf-8'), separators=(',', ':'), ensure_ascii=False)
            ok = [os.path.getsize(out), js.get('w'), js.get('h'), js.get('fr'), js.get('op'), [l.get('nm') for l in js.get('layers', [])][-4:]]
            break
        except Exception as e:
            ok = 'fail ' + str(e)[:80]
    report.append({'code': code, 'label': it['label'], 'name': it['name'], 'by': it['by'], 'page': it['page'], 'license': lic, 'file': ok})
    print(it['label'], code, lic, ok)
json.dump(report, open('report.json', 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
