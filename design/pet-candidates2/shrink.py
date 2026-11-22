import json, os, subprocess, io, sys
from PIL import Image, ImageSequence
os.makedirs('small', exist_ok=True)
d = json.load(open('search.json', encoding='utf-8'))
d['hippo'] = [{'name': 'bb hp', 'gif': 'https://assets-v2.lottiefiles.com/a/02bf574e-1188-11ee-b791-f7956531de6f/oALl3uCprb.gif', 'page': 'https://lottiefiles.com/animations/bb-hp-gfLAMaYMfl', 'by': 'Jan Gemerle', 'nofree': True}]
drop = {'Meat chop falls on the plate', 'cat', 'Crocodile'}
old = json.load(open('../pet-candidates/gallery.json', encoding='utf-8'))
codes = {it['code']: it for sp in old for it in sp['items']}
existing = []
for l in open('../pet-candidates/chosen.tsv', encoding='utf-8'):
    if not l.strip(): continue
    label, g, page = l.rstrip('\n').split('\t'); code = g.split('/')[1][:-4]
    if os.path.exists(f'../../android/app/src/main/assets/pets/{code}.json') and code not in [e['code'] for e in existing]:
        existing.append({'label': label, 'code': code, 'gif': codes[code]['gif'], 'page': page, 'by': codes[code]['by']})
def shrink(url, out):
    if os.path.exists(out): return os.path.getsize(out)
    raw = subprocess.run(['curl', '-sL', '-A', 'Mozilla/5.0', url], capture_output=True).stdout
    im = Image.open(io.BytesIO(raw)); frames, durs = [], []
    for i, f in enumerate(ImageSequence.Iterator(im)):
        if i % 2: continue
        f = f.convert('RGBA'); f.thumbnail((170, 170))
        bg = Image.new('RGBA', f.size, (255, 255, 255, 255)); bg.alpha_composite(f)
        frames.append(bg.convert('P', palette=Image.ADAPTIVE, colors=64)); durs.append(f.info.get('duration', 40) * 2 if f.info.get('duration') else 80)
        if len(frames) >= 45: break
    frames[0].save(out, save_all=True, append_images=frames[1:], loop=0, duration=durs, optimize=True, disposal=2)
    return os.path.getsize(out)
ar = {'cow': 'البقرة', 'hippo': 'فرس النهر', 'elephant': 'الفيل', 'giraffe': 'الزرافة'}
gal = {'new': [], 'existing': []}
for k, items in d.items():
    n = 0
    for it in items:
        if it['name'] in drop: continue
        n += 1; code = f'{k}-{n}'
        try: s = shrink(it['gif'], f'small/{code}.gif')
        except Exception as e: print('fail', code, e); continue
        gal['new'].append({**it, 'species': k, 'ar': ar[k], 'label': f'{ar[k]} {n}', 'code': code, 'small': f'small/{code}.gif', 'size': s})
for e in existing:
    try: s = shrink(e['gif'], f'small/old-{e["code"]}.gif')
    except Exception as ex: print('fail', e['code'], ex); continue
    gal['existing'].append({**e, 'small': f'small/old-{e["code"]}.gif', 'size': s})
json.dump(gal, open('gallery.json', 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
print(len(gal['new']), len(gal['existing']), sum(x['size'] for x in gal['new'] + gal['existing']))
