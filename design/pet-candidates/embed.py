import re, os, subprocess, json, zipfile, io, base64
os.chdir(os.path.dirname(os.path.abspath(__file__)))
rows = {l.split('\t')[1].split('/')[1][:-4]: l.rstrip('\n').split('\t')[2] for l in open('chosen.tsv', encoding='utf-8') if l.strip()}
for slug in ['penguin-5', 'penguin-7', 'penguin-12', 'chicken-1']:
    html = subprocess.run(['curl', '-sL', '-A', 'Mozilla/5.0', rows[slug]], capture_output=True).stdout.decode('utf-8', 'ignore').replace('\\"', '"')
    gifm = re.search(r'"contentUrl":"https://assets-v2\.lottiefiles\.com/a/([0-9a-f-]+)/', html)
    own = gifm.group(1)
    urls = [u for u in dict.fromkeys(re.findall(r'https://assets-v2\.lottiefiles\.com/a/[0-9a-f-]+/[A-Za-z0-9]+\.lottie', html)) if own in u]
    data = subprocess.run(['curl', '-sL', '-A', 'Mozilla/5.0', urls[0]], capture_output=True).stdout
    z = zipfile.ZipFile(io.BytesIO(data))
    names = z.namelist()
    js = json.loads(z.read([n for n in names if n.startswith('animations/') and n.endswith('.json')][0]))
    n = 0
    for a in js.get('assets', []):
        if 'p' in a and not str(a['p']).startswith('data:'):
            cand = [x for x in names if x.endswith('/' + a['p']) or x == a['p']]
            if not cand: print(slug, 'missing', a['p'], names); continue
            raw = z.read(cand[0]); ext = 'png' if raw[:4] == b'\x89PNG' else ('webp' if raw[8:12] == b'WEBP' else 'jpeg')
            a['p'] = f'data:image/{ext};base64,' + base64.b64encode(raw).decode(); a['u'] = ''; a['e'] = 1; n += 1
    json.dump(js, open(f'anim/{slug}.json', 'w', encoding='utf-8'), separators=(',', ':'))
    print(slug, 'embedded', n, os.path.getsize(f'anim/{slug}.json'))
