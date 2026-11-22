import json, html, os
os.chdir(os.path.dirname(os.path.abspath(__file__)))
g = json.load(open('gallery.json', encoding='utf-8'))
order = [('cow', 'البقرة'), ('hippo', 'فرس النهر'), ('elephant', 'الفيل'), ('giraffe', 'الزرافة')]


def card(label, src, name, by, note=''):
    return (f'<button class="card" type="button" data-l="{html.escape(label)}" aria-pressed="false">'
            f'<span class="pic"><img src="{src}" alt="{html.escape(name)}" loading="lazy" width="170" height="170"></span>'
            f'<span class="lab">{html.escape(label)}</span><span class="by">{html.escape(by or "")}{note}</span>'
            f'<span class="tick" aria-hidden="true">✓</span></button>')


sec = []
for k, ar in order:
    items = [x for x in g['new'] if x['species'] == k]
    note = ''
    if k == 'hippo':
        note = '<p class="note">لم أجد في LottieFiles إلا فرس نهر واحدًا، وهو خارج قسم الرسوم المجانية، فسأتحقق من رخصته قبل إضافته.</p>'
    sec.append(f'<section><h2>{ar} <small>{len(items)}</small></h2>{note}<div class="grid">'
               + ''.join(card(x['label'], x['small'], x['name'], x['by']) for x in items) + '</div></section>')
old = {}
for x in g['existing']:
    old.setdefault(x['label'].rsplit(' ', 1)[0], []).append(x)
olds = ''.join(f'<h3>{sp}</h3><div class="grid">'
               + ''.join(card(x['label'], x['small'], x['label'], x['by']) for x in sorted(v, key=lambda x: int(x['label'].rsplit(' ', 1)[1])))
               + '</div>' for sp, v in old.items())
page = (open('template.html', encoding='utf-8').read()
        .replace('%%NEW%%', ''.join(sec)).replace('%%OLD%%', olds).replace('%%NOLD%%', str(len(g['existing']))))
open('index.html', 'w', encoding='utf-8').write(page)
print(len(page))
