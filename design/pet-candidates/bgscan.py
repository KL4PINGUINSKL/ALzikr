import json, glob, os
os.chdir(r'D:\ALzikr\android\app\src\main\assets\pets')

def shapes_info(items, out):
    for it in items or []:
        ty = it.get('ty')
        if ty == 'gr': shapes_info(it.get('it'), out)
        elif ty in ('rc', 'el'):
            s = it.get('s', {}).get('k')
            if isinstance(s, list) and s and isinstance(s[0], (int, float)): out.append((ty, round(s[0]), round(s[1])))
        elif ty == 'fl':
            c = it.get('c', {}).get('k')
            if isinstance(c, list) and c and isinstance(c[0], (int, float)): out.append(('fill', [round(x, 2) for x in c[:3]]))

for f in ['seabird-1.json', 'sheep-5.json', 'sheep-2.json', 'cat-5.json', 'chicken-6.json', 'rabbit-9.json', 'sheep-8.json', 'penguin-7.json', 'bird-3.json']:
    d = json.load(open(f, encoding='utf-8'))
    print('==', f, d['w'], d['h'])
    for l in d['layers'][-4:]:
        info = []
        shapes_info(l.get('shapes'), info)
        print('  ', l.get('nm'), 'ty', l.get('ty'), 'sw/sh', l.get('sw'), l.get('sh'), 'sc', l.get('sc'), info[:6])
