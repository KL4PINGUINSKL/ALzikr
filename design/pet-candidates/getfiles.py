import re, os, subprocess, json, zipfile, io
os.chdir(os.path.dirname(os.path.abspath(__file__)))
os.makedirs('anim', exist_ok=True)
rows = [l.rstrip('\n').split('\t') for l in open('chosen.tsv', encoding='utf-8') if l.strip()]
report = []
for label, gif, page in rows:
    slug = gif.split('/')[1][:-4]
    html = subprocess.run(['curl', '-sL', '-A', 'Mozilla/5.0', page], capture_output=True).stdout.decode('utf-8', 'ignore').replace('\\"', '"').replace('\\u002F', '/')
    lic = 'Lottie Simple License' if 'Lottie Simple License' in html else ('license page' if '/page/license' in html else '?')
    m = re.findall(r'https://assets-v2\.lottiefiles\.com/a/[0-9a-f-]+/[A-Za-z0-9]+\.(?:json|lottie)', html)
    # the animation's own folder is the one of its preview gif
    folder = re.search(r'/a/([0-9a-f-]+)/', html)
    gifm = re.search(r'"contentUrl":"(https://assets-v2\.lottiefiles\.com/a/([0-9a-f-]+)/[^"]+\.gif)"', html)
    own = gifm.group(2) if gifm else None
    cands = [u for u in dict.fromkeys(m) if own and own in u] or list(dict.fromkeys(m))[:1]
    ok = None
    for u in cands:
        data = subprocess.run(['curl', '-sL', '-A', 'Mozilla/5.0', u], capture_output=True).stdout
        try:
            if u.endswith('.lottie') or data[:2] == b'PK':
                z = zipfile.ZipFile(io.BytesIO(data))
                names = [n for n in z.namelist() if n.startswith('animations/') and n.endswith('.json')]
                imgs = [n for n in z.namelist() if n.startswith('images/') or n.startswith('i/')]
                js = json.loads(z.read(names[0]))
                if imgs: js['_has_images'] = len(imgs)
            else:
                js = json.loads(data)
            out = f'anim/{slug}.json'
            json.dump(js, open(out, 'w', encoding='utf-8'), separators=(',', ':'))
            ok = (out, os.path.getsize(out), js.get('w'), js.get('h'), js.get('fr'), js.get('op'), bool(js.get('assets') and any('p' in a and 'e' in a for a in js['assets'])))
            break
        except Exception as e:
            ok = ('fail ' + str(e)[:60],)
    report.append((label, slug, lic, ok))
    print(label, slug, lic, ok)
json.dump(report, open('report.json', 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
