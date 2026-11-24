"""Downloads the user's picks (2026-11-24): the JSON link from the GraphQL API, images embedded, the license read from each page."""
import base64, io, json, os, re, subprocess, zipfile
os.chdir(os.path.dirname(os.path.abspath(__file__)))
os.makedirs('anim', exist_ok=True)
items = json.load(open('gallery.json', encoding='utf-8'))
by_label = {x['label']: x for x in items}
picks = [by_label[l.strip()] for l in re.split('[،,]', open('picks.txt', encoding='utf-8').read()) if l.strip()]
def get(u): return subprocess.run(['curl', '-sL', '--retry', '3', '-A', 'Mozilla/5.0', u], capture_output=True).stdout
def urls(i):
    q = json.dumps({'query': 'query($i:Int!){publicAnimation(id:$i){jsonUrl lottieUrl}}', 'variables': {'i': int(i)}})
    raw = subprocess.run(['curl', '-s', '-X', 'POST', 'https://graphql.lottiefiles.com/2022-08', '-H', 'content-type: application/json', '-A', 'Mozilla/5.0', '--data-binary', '@-'], input=q.encode(), capture_output=True).stdout
    a = json.loads(raw)['data']['publicAnimation']; return [u for u in (a.get('jsonUrl'), a.get('lottieUrl')) if u]
report = []
for it in picks:
    code = it['code']
    html = get(it['page']).decode('utf-8', 'ignore')
    lic = 'Lottie Simple License' if 'Lottie Simple License' in html else '?'
    ok = None
    for u in urls(it['id']):
        data = get(u)
        try:
            if data[:2] == b'PK':
                z = zipfile.ZipFile(io.BytesIO(data)); names = z.namelist()
                js = json.loads(z.read([n for n in names if (n.startswith('animations/') or n.startswith('a/')) and n.endswith('.json')][0]))
                for a in js.get('assets', []):
                    if 'p' in a and not str(a['p']).startswith('data:'):
                        cand = [x for x in names if x.endswith('/' + a['p']) or x == a['p']]
                        if not cand: continue
                        raw = z.read(cand[0]); ext = 'png' if raw[:4] == b'\x89PNG' else ('webp' if raw[8:12] == b'WEBP' else 'jpeg')
                        a['p'] = f'data:image/{ext};base64,' + base64.b64encode(raw).decode(); a['u'] = ''; a['e'] = 1
            else:
                js = json.loads(data)
            # images referenced by URL (not embedded) would not load offline
            ext_img = [a for a in js.get('assets', []) if 'p' in a and not str(a['p']).startswith('data:')]
            out = f'anim/{code}.json'
            json.dump(js, open(out, 'w', encoding='utf-8'), separators=(',', ':'), ensure_ascii=False)
            ok = [os.path.getsize(out), js.get('w'), js.get('h'), js.get('fr'), js.get('op'), len(ext_img), [l.get('nm') for l in js.get('layers', [])][-4:]]
            break
        except Exception as e:
            ok = 'fail ' + str(e)[:80]
    report.append({'code': code, 'label': it['label'], 'name': it['name'], 'by': it['by'], 'page': it['page'], 'license': lic, 'file': ok})
    print(it['label'], code, lic, ok)
json.dump(report, open('report.json', 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
