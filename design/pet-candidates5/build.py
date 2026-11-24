"""Pick gallery for the new looks (advanced levels): shrunken GIF previews, labels «القطة م1» etc. (م = مستويات)."""
import json, os, re, io, subprocess, html
from concurrent.futures import ThreadPoolExecutor
from PIL import Image, ImageSequence
os.chdir(os.path.dirname(os.path.abspath(__file__)))
os.makedirs('small', exist_ok=True)
d = json.load(open('search.json', encoding='utf-8'))
ar = {'cat': 'القطة', 'penguin': 'البطريق', 'sheep': 'الغنمة', 'rabbit': 'الأرنب', 'chicken': 'الدجاجة', 'bird': 'العصفور',
      'swallow': 'السنونو', 'bear': 'الدب', 'panda': 'الباندا', 'cow': 'البقرة', 'giraffe': 'الزرافة', 'elephant': 'الفيل', 'seabird': 'طائر البحر'}
def shrink(url, out):
    if os.path.exists(out): return True
    raw = subprocess.run(['curl', '-sL', '-A', 'Mozilla/5.0', url], capture_output=True).stdout
    im = Image.open(io.BytesIO(raw)); frames, durs = [], []
    for i, f in enumerate(ImageSequence.Iterator(im)):
        if i % 2: continue
        f = f.convert('RGBA'); f.thumbnail((170, 170))
        bg = Image.new('RGBA', f.size, (255, 255, 255, 255)); bg.alpha_composite(f)
        frames.append(bg.convert('P', palette=Image.ADAPTIVE, colors=64)); durs.append((f.info.get('duration') or 40) * 2)
        if len(frames) >= 40: break
    frames[0].save(out, save_all=True, append_images=frames[1:], loop=0, duration=durs, optimize=True, disposal=2)
    return True
items = []
for sp in ar:
    for n, it in enumerate(d.get(sp, []), 1):
        items.append({**it, 'species': sp, 'label': f'{ar[sp]} م{n}', 'code': f'{sp}-m{n}'})
def job(it):
    try: shrink(it['gif'], f'small/{it["code"]}.gif'); return it
    except Exception as e: print('fail', it['code'], e); return None
with ThreadPoolExecutor(8) as ex: items = [x for x in ex.map(job, items) if x]
json.dump(items, open('gallery.json', 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
def card(x):
    return (f'<button class="card" type="button" data-l="{html.escape(x["label"])}" aria-pressed="false">'
            f'<span class="pic"><img src="small/{x["code"]}.gif" alt="{html.escape(x["name"])}" loading="lazy" width="170" height="170"></span>'
            f'<span class="lab">{html.escape(x["label"])}</span><span class="by">{html.escape(x["name"])}</span>'
            f'<span class="tick" aria-hidden="true">✓</span></button>')
sec = ''
for sp in ar:
    xs = [x for x in items if x['species'] == sp]
    if xs: sec += f'<section><h2>{ar[sp]} <small>{len(xs)}</small></h2><div class="grid">' + ''.join(map(card, xs)) + '</div></section>'
t = open('template.html', encoding='utf-8').read()
t = t.replace('<title>أصدقاء ALzikr الجدد</title>', '<title>أشكال المستويات المتقدمة</title>')
t = t.replace('<h1>أصدقاء جدد لـ ALzikr</h1>', '<h1>أشكال جديدة للمستويات المتقدمة</h1>')
t = re.sub(r'<p>رسوم متحركة مجانية.*?</p>', '<p>تصاميم من LottieFiles لم تظهر لك في أي معرض سابق، لتُفتح لصديقك في المستويات المتقدمة. الأرقام بحرف «م» حتى لا تختلط بالقوائم السابقة. للزرافة وطائر البحر لم أجد إلا تصميمًا واحدًا جديدًا. اختر ما يعجبك، ثم انسخ القائمة وأرسلها لي.</p>', t, flags=re.S)
t = re.sub(r'%%NEW%%.*?</section>\s*</div>\s*<div class="tray">', lambda m: sec + '\n</div>\n<div class="tray">', t, flags=re.S)
t = t.replace('%%NEW%%', sec)
t = t.replace("alzikr-pets-pick-2", "alzikr-pets-pick-5")
open('index.html', 'w', encoding='utf-8').write(t)
print(len(items), {sp: sum(1 for x in items if x['species'] == sp) for sp in ar}, sum(os.path.getsize('small/' + f) for f in os.listdir('small')))
